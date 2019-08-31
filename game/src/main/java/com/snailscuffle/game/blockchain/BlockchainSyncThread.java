package com.snailscuffle.game.blockchain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.blockchain.data.AccountMetadata;
import com.snailscuffle.game.blockchain.data.BattlePlanCommitMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanRevealMessage;
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
	
	private static final int SYNC_LOOP_EXTRA_BLOCKS = 10;
	private static final Logger logger = LoggerFactory.getLogger(BlockchainSyncThread.class);
	
	private final SyncAction CONNECT;
	private final SyncAction VALIDATE_RECENT_BLOCKS;
	private final SyncAction SYNC_FROM_LAST_HEIGHT;
	private final SyncAction CONTINUOUS_SYNC_LOOP;
	private final SyncAction ABORT;
	private final SyncAction EXIT_WITH_ERROR;
	
	private final BattlesInProgress battlesInProgress;
	private final int syncLoopPeriodMillis;
	
	private boolean caughtUp;
	
	BlockchainSyncThread(IgnisArchivalNodeConnection ignisNode, Accounts accounts, int recentBattlesDepth, int syncLoopPeriodMillis) {
		CONNECT = new SyncAction(() -> connect(ignisNode));
		VALIDATE_RECENT_BLOCKS = new SyncAction(() -> validateRecentBlocks(ignisNode, accounts));
		SYNC_FROM_LAST_HEIGHT = new SyncAction(() -> syncFromLastHeight(ignisNode, accounts));
		CONTINUOUS_SYNC_LOOP = new SyncAction(() -> continuousSyncLoop(ignisNode, accounts));
		ABORT = new SyncAction(() -> abort());
		EXIT_WITH_ERROR = new SyncAction(() -> exitWithError());
		
		battlesInProgress = new BattlesInProgress(recentBattlesDepth);
		this.syncLoopPeriodMillis = syncLoopPeriodMillis;
	}
	
	@Override
	public void run() {
		SyncAction nextAction = CONNECT;
		while (nextAction != null) {
			nextAction = nextAction.run();
		}
	}
	
	public synchronized boolean isCaughtUp() {
		return caughtUp;
	}
	
	private synchronized void setCaughtUp(boolean value) {
		caughtUp = value;
	}
	
	private SyncAction connect(IgnisArchivalNodeConnection ignisNode) {
		try {
			if (ignisNode.isReady()) {
				return VALIDATE_RECENT_BLOCKS;
			} else {
				Thread.sleep(syncLoopPeriodMillis);
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
			Block currentBlock = ignisNode.getCurrentBlock();
			List<Account> playerAccounts = ignisNode.getAllPlayerAccounts().stream()
					.map(a -> new Account(a.id, a.username, a.publicKey))
					.collect(Collectors.toList());
			
			accounts.addIfNotPresent(playerAccounts);
			accounts.updateUsernames(playerAccounts);
			
			// Battles that conclude after lastSyncHeight will be used to update accounts. In
			// order to catch battles that were in progress at lastSyncHeight, we must backtrack.
			int previousSyncHeight = accounts.getSyncState().height;
			int backtrackHeight = Math.min(previousSyncHeight - Constants.SYNC_BACKTRACK, Constants.INITIAL_SYNC_HEIGHT);
			
			battlesInProgress.clear();
			for (Account account : playerAccounts) {
				List<Transaction> messageTxs = ignisNode.getMessagesFrom(account.id, backtrackHeight, currentBlock.height);
				List<OnChain<? extends BattlePlanMessage>> battlePlanMessages = parseBattlePlanMessages(messageTxs);
				battlesInProgress.update(battlePlanMessages);
			}
			Map<Long, Account> accountsToUpdate = accounts.getById(battlesInProgress.getAllAccounts());
			
			Collection<StateChangeFromBattle> results = battlesInProgress.runAll(accountsToUpdate, previousSyncHeight, currentBlock.height);
			
			Block forkCheck = ignisNode.getBlockAtHeight(currentBlock.height);
			if (forkCheck.id == currentBlock.id) {		// make sure there wasn't a fork while we were syncing
				accounts.update(results);
				accounts.updateSyncState(currentBlock.height, currentBlock.id);
				return CONTINUOUS_SYNC_LOOP;
			} else {
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
		setCaughtUp(true);
		try {
			BlockSyncInfo currentSyncState = accounts.getSyncState();
			Block currentBlock = ignisNode.getCurrentBlock();
			
			if (currentBlock.height == currentSyncState.height) {
				Thread.sleep(syncLoopPeriodMillis);
				return CONTINUOUS_SYNC_LOOP;
			}
			
			int blockCount = currentBlock.height - currentSyncState.height + SYNC_LOOP_EXTRA_BLOCKS;
			List<Block> recentBlocks = ignisNode.getRecentBlocks(blockCount);
			Block blockAtLastSyncedHeight = recentBlocks.stream().filter(b -> b.height == currentSyncState.height).findFirst().orElse(null);
			boolean forkDetected = (blockAtLastSyncedHeight == null) || blockAtLastSyncedHeight.id != currentSyncState.blockId;
			
			if (forkDetected) {
				List<BlockSyncInfo> recentSyncInfo = accounts.getSyncInfoFromRecentStateChanges();
				BlockSyncInfo lastMatchingBlock = findBeginningOfFork(recentSyncInfo, ignisNode);
				if (lastMatchingBlock == null) {
					setCaughtUp(false);
					return VALIDATE_RECENT_BLOCKS;
				} else {
					accounts.rollBackTo(lastMatchingBlock.height, lastMatchingBlock.blockId);
					battlesInProgress.rollBackTo(lastMatchingBlock.height);
					return CONTINUOUS_SYNC_LOOP;
				}
			} else {
				syncRecentBlocks(recentBlocks, accounts, ignisNode);
				return CONTINUOUS_SYNC_LOOP;
			}
		} catch (IgnisNodeCommunicationException e) {
			setCaughtUp(false);
			return CONNECT;
		} catch (AccountsException | BlockchainSubsystemException e) {
			logger.error("Unexpected error while doing full sync to blockchain", e);
			setCaughtUp(false);
			return EXIT_WITH_ERROR;
		} catch (InterruptedException e) {
			setCaughtUp(false);
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
	
	private static BlockSyncInfo findBeginningOfFork(List<BlockSyncInfo> recentSyncInfo, IgnisArchivalNodeConnection ignisNode) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		Block currentBlock = ignisNode.getCurrentBlock();
		int blockCount = currentBlock.height - recentSyncInfo.get(recentSyncInfo.size() - 1).height + SYNC_LOOP_EXTRA_BLOCKS;
		Map<Integer, Block> blocksByHeight = ignisNode.getRecentBlocks(blockCount).stream()
				.collect(Collectors.toMap(b -> b.height, b -> b));
		
		for (BlockSyncInfo syncedBlock : recentSyncInfo) {
			Block observedBlock = blocksByHeight.get(syncedBlock.height);
			if (observedBlock.id == syncedBlock.blockId) {
				return syncedBlock;
			}
		}
		
		return null;
	}
	
	private void syncRecentBlocks(List<Block> recentBlocks, Accounts accounts, IgnisArchivalNodeConnection ignisNode) throws AccountsException, IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		BlockSyncInfo currentSyncState = accounts.getSyncState();
		Block currentBlock = recentBlocks.get(0);
		
		List<Transaction> txsToSync = recentBlocks.stream()
				.filter(b -> b.height > currentSyncState.height)
				.flatMap(b -> b.transactions.stream())
				.collect(Collectors.toList());
		List<Account> accountsWithNewAliases = parseAccountsWithChangedAliases(txsToSync, ignisNode);
		List<OnChain<? extends BattlePlanMessage>> battleMessages = parseBattlePlanMessages(txsToSync);
		
		accounts.addIfNotPresent(accountsWithNewAliases);
		accounts.updateUsernames(accountsWithNewAliases);
		
		battlesInProgress.update(battleMessages);
		Map<Long, Account> accountsToUpdate = accounts.getById(battlesInProgress.getAllAccounts());
		Collection<StateChangeFromBattle> results = battlesInProgress.runAll(accountsToUpdate, currentSyncState.height, currentBlock.height);
		accounts.update(results);
		accounts.updateSyncState(currentBlock.height, currentBlock.id);
	}
	
	private static List<Account> parseAccountsWithChangedAliases(List<Transaction> transactions, IgnisArchivalNodeConnection ignisNode) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		List<Long> accountsWithChangedAliases = transactions.stream()
				.filter(tx -> tx.alias.length() > 0)
				.flatMap(tx -> Stream.of(tx.sender, tx.recipient))
				.filter(id -> id != 0)		// recipient is zero for txs with no recipient
				.collect(Collectors.toList());
		
		List<Account> accounts = new ArrayList<>();
		for (Long accountId : accountsWithChangedAliases) {
			AccountMetadata account = ignisNode.getPlayerAccount(Long.toUnsignedString(accountId));
			accounts.add(new Account(account.id, account.username, account.publicKey));
		}
		return accounts;
	}
	
	private static List<OnChain<? extends BattlePlanMessage>> parseBattlePlanMessages(List<Transaction> transactions) {
		List<OnChain<? extends BattlePlanMessage>> battlePlanMessages = new ArrayList<>();
		
		for (Transaction tx : transactions) {
			try {
				JsonNode messageNode = JsonUtil.deserialize(tx.message);
				if (messageNode == null) {
					continue;
				}
				
				JsonNode battlePlanHashNode = messageNode.get("battlePlanHash");
				JsonNode battlePlanNode = messageNode.get("battlePlan");
				
				if (battlePlanHashNode != null) {
					battlePlanMessages.add(parseMessage(BattlePlanCommitMessage.class, tx));
				} else if (battlePlanNode != null) {
					battlePlanMessages.add(parseMessage(BattlePlanRevealMessage.class, tx));
				}
			} catch (IOException e) {
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
