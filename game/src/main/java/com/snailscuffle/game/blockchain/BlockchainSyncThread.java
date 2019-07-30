package com.snailscuffle.game.blockchain;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
import com.snailscuffle.game.accounts.AccountsSnapshot;
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
	private static final Logger logger = LoggerFactory.getLogger(BlockchainSyncThread.class);
	
	private final SyncAction CONNECT;
	private final SyncAction VALIDATE_SNAPSHOTS;
	private final SyncAction SYNC_FROM_LAST_SNAPSHOT;
	private final SyncAction CONTINUOUS_SYNC_LOOP;
	private final SyncAction ABORT;
	private final SyncAction EXIT_WITH_ERROR;
	
	private final BattlesInProgress battlesInProgress = new BattlesInProgress();

	BlockchainSyncThread(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		CONNECT = new SyncAction(() -> connect(ignisNode));
		VALIDATE_SNAPSHOTS = new SyncAction(() -> validateSnapshots(ignisNode, accounts));
		SYNC_FROM_LAST_SNAPSHOT = new SyncAction(() -> syncFromLastSnapshot(ignisNode, accounts));
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
				return VALIDATE_SNAPSHOTS;
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
	
	private SyncAction validateSnapshots(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		try {
			List<AccountsSnapshot> snapshotsByHeight = accounts.getAllSnapshots();
			int lastValidHeight = Constants.INITIAL_SYNC_HEIGHT;
			for (AccountsSnapshot snapshot : snapshotsByHeight) {
				Block observedBlock = ignisNode.getBlockAtHeight(snapshot.height);
				if (snapshot.blockId == observedBlock.id) {
					lastValidHeight = snapshot.height;
					break;
				}
			}
			
			accounts.rollBackTo(lastValidHeight);
			
			return SYNC_FROM_LAST_SNAPSHOT;
		} catch (IgnisNodeCommunicationException e) {
			return CONNECT;
		} catch (AccountsException | BlockchainSubsystemException e) {
			logger.error("Unexpected error while trying to validate snapshots", e);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			return ABORT;
		}
	}
	
	private SyncAction syncFromLastSnapshot(IgnisArchivalNodeConnection ignisNode, Accounts accounts) {
		try {
			Block lastBlock = ignisNode.getCurrentBlock();
			List<Account> playerAccounts = ignisNode.getAllPlayerAccounts();
			
			// Battles that conclude after lastSyncHeight will be used to update accounts. In
			// order to catch battles that were in progress at lastSyncHeight, we must backtrack.
			int lastSyncHeight = accounts.getSyncHeight();
			int backtrackHeight = Math.min(lastSyncHeight - Constants.SYNC_BACKTRACK, Constants.INITIAL_SYNC_HEIGHT);
			
			for (Account account : playerAccounts) {
				List<Transaction> transactions = ignisNode.getTransactionsToAndFrom(account.numericId(), backtrackHeight, lastBlock.height);
				List<OnChain<? extends BattlePlanMessage>> battlePlanMessages = parseBattlePlanMessages(transactions, account.numericId());
				battlesInProgress.update(battlePlanMessages);
			}
			Map<Long, Account> accountsToUpdate = accounts.getById(battlesInProgress.getAllAccounts())
					.stream()
					.collect(Collectors.toMap(a -> a.numericId(), a -> a));
			
			Map<Long, Account> updatedAccounts = battlesInProgress.runAll(accountsToUpdate, lastSyncHeight, lastBlock.height);
			
			accounts.insertOrUpdate(updatedAccounts.values(), lastBlock.height, lastBlock.id);
			accounts.takeSnapshot(Instant.now().toString());
			
			return CONTINUOUS_SYNC_LOOP;
		} catch (IgnisNodeCommunicationException e) {
			return CONNECT;
		} catch (AccountsException | BlockchainSubsystemException e) {
			logger.error("Unexpected error while doing full sync to blockchain", e);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			return ABORT;
		}
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
		return new OnChain<>(tx.height, tx.index, tx.sender, tx.recipient, message);
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
	
}
