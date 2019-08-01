package com.snailscuffle.game.blockchain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.blockchain.data.BattlePlanCommitMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanMessage;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.OnChain;
import com.snailscuffle.game.blockchain.data.Transaction;

class BlockchainSyncThread extends Thread {
	
	private static class SyncAction {
		private Supplier<SyncAction> theAction;
		
		SyncAction(Supplier<SyncAction> theAction) {
			this.theAction = theAction;
		}
		
		SyncAction run() {
			return theAction.get();
		}
	}
	
	private static final int SYNC_LOOP_PERIOD_MILLIS = 10000;
	private static final int SYNC_LOOP_BLOCK_COUNT = 10;
	private static final Logger logger = LoggerFactory.getLogger(BlockchainSyncThread.class);
	
	private final SyncAction CONNECT;
	private final SyncAction VALIDATE_RECENT_BLOCKS;
	private final SyncAction SYNC_FROM_LAST_HEIGHT;
	private final SyncAction CONTINUOUS_SYNC_LOOP;
	private final SyncAction ABORT;
	private final SyncAction EXIT_WITH_ERROR;
	
	private final BattlesInProgress battlesInProgress = new BattlesInProgress();

	BlockchainSyncThread(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		CONNECT = new SyncAction(() -> connect(ignisNode));
		VALIDATE_RECENT_BLOCKS = new SyncAction(() -> validateRecentBlocks(ignisNode, accounts));
		SYNC_FROM_LAST_HEIGHT = new SyncAction(() -> syncFromLastHeight(ignisNode, accounts));
		CONTINUOUS_SYNC_LOOP = new SyncAction(() -> continuousSyncLoop(ignisNode, accounts));
		ABORT = new SyncAction(() -> abort());
		EXIT_WITH_ERROR = new SyncAction(() -> exitWithError());
	}
	
	@Override
	public void run() {
		SyncAction nextAction = CONNECT;
		while (nextAction != null) {
			nextAction = nextAction.run();
		}
	}
	
	private SyncAction connect(IgnisArchivalNodeConnection ignisNode) {
		try {
			if (ignisNode.isReady()) {
				return VALIDATE_RECENT_BLOCKS;
			} else {
				Thread.sleep(10000);
				return CONNECT;
			}
		} catch (BlockchainSubsystemException e) {
			logger.error("Unable to connect to Ignis archival node", e);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			return ABORT;
		}
	}
	
	private SyncAction validateRecentBlocks(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		try {
			List<BlockSyncInfo> recentBlocks = accounts.getSyncInfoFromRecentStateChanges();
			BlockSyncInfo lastValid = new BlockSyncInfo(Constants.INITIAL_SYNC_HEIGHT, Constants.INITIAL_SYNC_BLOCK_ID);
			for (BlockSyncInfo block : recentBlocks) {
				Block observedBlock = ignisNode.getBlockAtHeight(block.height);
				if (observedBlock.id == block.blockId) {
					lastValid = block;
					break;
				}
			}
			
			accounts.rollBackTo(lastValid.height, lastValid.blockId);
			
			return SYNC_FROM_LAST_HEIGHT;
		} catch (IgnisNodeCommunicationException e) {
			return CONNECT;
		} catch (AccountsException | BlockchainSubsystemException e) {
			logger.error("Unexpected error while trying to validate recent blocks", e);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			return ABORT;
		}
	}
	
	private SyncAction syncFromLastHeight(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		try {
			Block lastBlock = ignisNode.getCurrentBlock();
			List<Account> playerAccounts = ignisNode.getAllPlayerAccounts();
			
			accounts.addIfNotPresent(playerAccounts);
			
			// Battles that conclude after lastSyncHeight will be used to update accounts. In
			// order to catch battles that were in progress at lastSyncHeight, we must backtrack.
			int lastSyncHeight = accounts.getSyncState().height;
			int backtrackHeight = Math.min(lastSyncHeight - Constants.SYNC_BACKTRACK, Constants.INITIAL_SYNC_HEIGHT);
			
			for (Account account : playerAccounts) {
				List<Transaction> transactions = ignisNode.getTransactionsToAndFrom(account.numericId(), backtrackHeight, lastBlock.height);
				List<OnChain<? extends BattlePlanMessage>> battlePlanMessages = parseBattlePlanMessages(transactions, account.numericId());
				battlesInProgress.update(battlePlanMessages);
			}
			Map<Long, Account> accountsToUpdate = accounts.getById(battlesInProgress.getAllAccounts())
					.stream()
					.collect(Collectors.toMap(a -> a.numericId(), a -> a));
			
			Collection<StateChangeFromBattle> results = battlesInProgress.runAll(accountsToUpdate, lastSyncHeight, lastBlock.height);
			accounts.update(results);
			
			Block forkCheck = ignisNode.getBlockAtHeight(lastBlock.height);
			if (forkCheck.id == lastBlock.id) {		// make sure there wasn't a fork while we were syncing
				return CONTINUOUS_SYNC_LOOP;
			} else {
				accounts.rollBackTo(lastBlock.height, lastBlock.id);
				battlesInProgress.clear();
				return SYNC_FROM_LAST_HEIGHT;
			}
		} catch (IgnisNodeCommunicationException e) {
			return CONNECT;
		} catch (AccountsException | BlockchainSubsystemException e) {
			logger.error("Unexpected error while doing full sync to blockchain", e);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			return ABORT;
		}
	}
	
	private SyncAction continuousSyncLoop(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		try {
			Thread.sleep(SYNC_LOOP_PERIOD_MILLIS);
			return CONTINUOUS_SYNC_LOOP;
		} catch (InterruptedException e) {
			return ABORT;
		}
	}
	
	private SyncAction abort() {
		logger.error("Sync thread interrupted");
		return EXIT_WITH_ERROR;
	}
	
	private static SyncAction exitWithError() {
		logger.error("Fatal error; exiting sync thread");
		return null;
	}
	
	private static List<OnChain<? extends BattlePlanMessage>> parseBattlePlanMessages(List<Transaction> transactions, long sender) {
		List<OnChain<? extends BattlePlanMessage>> battlePlanMessages = new ArrayList<>();
		
		for (Transaction tx : transactions) {
			if (tx.sender != sender) {
				continue;
			}
			
			try {
				JsonNode messageNode = BlockchainUtil.parseJson(tx.message, "");
				JsonNode battlePlanHashNode = messageNode.get("battlePlanHash");
				JsonNode battlePlanNode = messageNode.get("battlePlan");
				
				if (battlePlanHashNode != null) {
					battlePlanMessages.add(parseMessage(BattlePlanCommitMessage.class, tx));
				} else if (battlePlanNode != null) {
					battlePlanMessages.add(parseMessage(BattlePlanMessage.class, tx));
				}
			} catch (BlockchainSubsystemException | IOException e) {
				// not a relevant message, so skip it
			}
		}
		
		return battlePlanMessages;
	}
	
	private static <T> OnChain<T> parseMessage(Class<T> type, Transaction tx) throws IOException {
		T message = JsonUtil.deserialize(type, tx.message);
		return new OnChain<>(tx.blockId, tx.height, tx.index, tx.sender, tx.recipient, message);
	}
	
}
