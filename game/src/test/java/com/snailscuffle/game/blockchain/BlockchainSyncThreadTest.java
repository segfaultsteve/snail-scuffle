package com.snailscuffle.game.blockchain;

import static com.snailscuffle.game.testutil.AccountsTestUtil.changesFromBattle;
import static com.snailscuffle.game.testutil.SyncUtil.waitForValue;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.blockchain.data.AccountMetadata;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.Transaction;

public class BlockchainSyncThreadTest {
	
	private static final long PLAYER_0_ID = 1;
	private static final long PLAYER_1_ID = 2;
	private static final int ROUNDS_TO_FINISH_BATTLE = 3;
	private static final int SYNC_LOOP_PERIOD_MILLIS = 100;
	private static final int TIMEOUT_MILLIS = 2000;
	
	@Mock private IgnisArchivalNodeConnection ignisNode;
	private List<AccountMetadata> accountsOnBlockchain;
	private List<Block> recentBlocks;
	private Accounts accountsDb;
	
	private BattlePlan winningBp;
	private String winningBpHash;
	private BattlePlan losingBp;
	private String losingBpHash;
	
	private BlockchainSyncThread blockchainSyncThread;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		accountsOnBlockchain = new ArrayList<>();
		recentBlocks = new ArrayList<>();
		accountsDb = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
		
		when(ignisNode.isReady()).thenReturn(true);
		when(ignisNode.getCurrentBlock()).thenAnswer(i -> {
			synchronized (recentBlocks) {
				return recentBlocks.get(recentBlocks.size() - 1);
			}
		});
		when(ignisNode.getRecentBlocks(anyInt())).thenAnswer(invocation -> {
			synchronized (recentBlocks) {
				int count = Math.min(invocation.getArgument(0), recentBlocks.size());
				List<Block> blocks = new ArrayList<>(recentBlocks.subList(recentBlocks.size() - count, recentBlocks.size()));
				Collections.reverse(blocks);
				return blocks;
			}
		});
		when(ignisNode.getBlockAtHeight(anyInt())).thenAnswer(invocation -> {
			int height = invocation.getArgument(0);
			synchronized (recentBlocks) {
				return recentBlocks.stream().filter(b -> b.height == height).findFirst().get();
			}
		});
		when(ignisNode.getAllPlayerAccounts()).thenReturn(accountsOnBlockchain);
		when(ignisNode.getPlayerAccount(anyString())).thenAnswer(invocation -> {
			long id = Long.parseUnsignedLong(invocation.getArgument(0));
			return accountsOnBlockchain.stream().filter(a -> a.id == id).findFirst().get();
		});
		when(ignisNode.getMessagesFrom(anyString(), anyInt(), anyInt())).thenAnswer(invocation -> {
			long id = Long.parseUnsignedLong(invocation.getArgument(0));
			int initialHeight = invocation.getArgument(1);
			int finalHeight = invocation.getArgument(2);
			synchronized (recentBlocks) {
				return recentBlocks.stream()
						.flatMap(b -> b.transactions.stream())
						.filter(t -> t.sender == id && t.height >= initialHeight && t.height <= finalHeight)
						.collect(Collectors.toList());
			}
		});
		
		recentBlocks.add(new Block(Constants.INITIAL_SYNC_BLOCK_ID, Constants.INITIAL_SYNC_HEIGHT, 0, new ArrayList<>()));
		
		winningBp = new BattlePlan();
		winningBp.snail = Snail.DALE;
		winningBp.weapon = Weapon.RIFLE;
		winningBp.shell = Shell.ALUMINUM;
		winningBp.accessory = Accessory.DEFIBRILLATOR;
		winningBp.validate();
		
		winningBpHash = BlockchainUtil.sha256Hash(winningBp);
		
		losingBp = new BattlePlan();
		losingBp.snail = Snail.DALE;
		losingBp.weapon = Weapon.RIFLE;
		losingBp.shell = Shell.ALUMINUM;
		// no accessory - this should guarantee a loss
		losingBp.validate();
		
		losingBpHash = BlockchainUtil.sha256Hash(losingBp);
		
		blockchainSyncThread = new BlockchainSyncThread(ignisNode, accountsDb, Constants.RECENT_BATTLES_DEPTH, SYNC_LOOP_PERIOD_MILLIS);
	}
	
	@After
	public void tearDown() {
		blockchainSyncThread.interrupt();
	}
	
	@Test
	public void syncPreexistingAccounts() throws Exception {
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(PLAYER_0_ID, "player0", "pubkey0"),
			new AccountMetadata(PLAYER_1_ID, "player1", "pubkey1")
		));
		
		blockchainSyncThread.start();
		Account player0 = waitForValue(TIMEOUT_MILLIS, () -> accountsDb.getById(PLAYER_0_ID));
		Account player1 = accountsDb.getById(PLAYER_1_ID);
		
		assertEquals("player0", player0.username);
		assertEquals("pubkey0", player0.publicKey);
		assertEquals("player1", player1.username);
		assertEquals("pubkey1", player1.publicKey);
	}
	
	@Test
	public void updateUsername() throws Exception {
		accountsDb.addIfNotPresent(Arrays.asList(
			new Account(PLAYER_0_ID, "oldname", "pubkey")
		));
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(PLAYER_0_ID, "newname", "pubkey")
		));
		
		blockchainSyncThread.start();
		Account player = waitForValue(TIMEOUT_MILLIS, () -> accountsDb.getByUsername("newname"));
		
		assertNotNull(player);
	}
	
	@Test
	public void backtrackPastLastSyncHeightToFindBattleInProgress() throws Exception {
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(PLAYER_0_ID, "player0", "pubkey0"),
			new AccountMetadata(PLAYER_1_ID, "player1", "pubkey1")
		));
		
		String battleId = "battle1";
		int height = Constants.INITIAL_SYNC_HEIGHT;
		long blockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		for (int round = 0; round < ROUNDS_TO_FINISH_BATTLE - 1; round++) {
			recentBlocks.addAll(Arrays.asList(
				newCommitBlock(battleId, round, ++blockId, ++height, winningBpHash, null),
				newCommitBlock(battleId, round, ++blockId, ++height, null, losingBpHash),
				newRevealBlock(battleId, round, ++blockId, ++height, winningBp, losingBp)
			));
		}
		
		accountsDb.updateSyncState(height, blockId);	// battle still in progress at this height
		
		recentBlocks.addAll(Arrays.asList(
			newCommitBlock(battleId, ROUNDS_TO_FINISH_BATTLE - 1, ++blockId, ++height, winningBpHash, losingBpHash),
			newRevealBlock(battleId, ROUNDS_TO_FINISH_BATTLE - 1, ++blockId, ++height, winningBp, losingBp)
		));
		
		blockchainSyncThread.start();
		
		Account player0 = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 1) ? p0 : null;
		});
		Account player1 = accountsDb.getById(PLAYER_1_ID);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, player0.rating);
		assertEquals(1, player0.streak);
		
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, player1.rating);
		assertEquals(-1, player1.streak);
	}
	
	@Test
	public void rollBackAllBlocksDuringInitialSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		String battleId = "battle1";
		
		startWithAccounts(Arrays.asList(player0, player1));
		int finalHeight = startWithBattle(player0, player1, battleId, Constants.INITIAL_SYNC_HEIGHT + 1, 1);
		
		long mismatchedBlockId = 999;
		recentBlocks.remove(recentBlocks.size() - 1);
		recentBlocks.add(newRevealBlock(battleId, ROUNDS_TO_FINISH_BATTLE - 1, mismatchedBlockId, finalHeight, winningBp, losingBp));
		
		Account player0BeforeRollback = accountsDb.getById(player0.numericId());
		Account player1BeforeRollback = accountsDb.getById(player1.numericId());
		
		blockchainSyncThread.start();
		
		Account player0AfterRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 0) ? p0 : null;
		});
		Account player1AfterRollback = accountsDb.getById(player1.numericId());
		
		assertEquals(1, player0BeforeRollback.wins);
		assertEquals(0, player0BeforeRollback.losses);
		assertEquals(1, player0BeforeRollback.streak);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, player0BeforeRollback.rating);
		assertEquals(1, player0BeforeRollback.rank);
		
		assertEquals(0, player1BeforeRollback.wins);
		assertEquals(1, player1BeforeRollback.losses);
		assertEquals(-1, player1BeforeRollback.streak);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, player1BeforeRollback.rating);
		assertEquals(2, player1BeforeRollback.rank);
		
		for (Account player : Arrays.asList(player0AfterRollback, player1AfterRollback)) {
			assertEquals(0, player.wins);
			assertEquals(0, player.losses);
			assertEquals(0, player.streak);
			assertEquals(Constants.INITIAL_RATING, player.rating);
			assertEquals(1, player.rank);
		}
	}
	
	@Test
	public void rollBackSomeBlocksDuringInitialSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		
		startWithAccounts(Arrays.asList(player0, player1));
		int firstBattleFinishHeight = startWithBattle(player0, player1, "battle1", Constants.INITIAL_SYNC_HEIGHT + 1, 1);
		int secondBattleFinishHeight = startWithBattle(accountsDb.getById(player0.numericId()), accountsDb.getById(player1.numericId()), "battle2", firstBattleFinishHeight + 1, firstBattleFinishHeight - Constants.INITIAL_SYNC_HEIGHT + 1);
		
		long mismatchedBlockId = 999;
		recentBlocks.remove(recentBlocks.size() - 1);
		recentBlocks.add(newRevealBlock("battle2", ROUNDS_TO_FINISH_BATTLE - 1, mismatchedBlockId, secondBattleFinishHeight, winningBp, losingBp));
		
		Account player0BeforeRollback = accountsDb.getById(player0.numericId());
		Account player1BeforeRollback = accountsDb.getById(player1.numericId());
		
		blockchainSyncThread.start();
		
		Account player0AfterRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 1) ? p0 : null;
		});
		Account player1AfterRollback = accountsDb.getById(player1.numericId());
		
		assertEquals(2, player0BeforeRollback.wins);
		assertEquals(0, player0BeforeRollback.losses);
		assertEquals(2, player0BeforeRollback.streak);
		assertEquals(1, player0BeforeRollback.rank);
		
		assertEquals(0, player1BeforeRollback.wins);
		assertEquals(2, player1BeforeRollback.losses);
		assertEquals(-2, player1BeforeRollback.streak);
		assertEquals(2, player1BeforeRollback.rank);
		
		assertEquals(1, player0AfterRollback.wins);
		assertEquals(0, player0AfterRollback.losses);
		assertEquals(1, player0AfterRollback.streak);
		assertEquals(1, player0AfterRollback.rank);
		
		assertEquals(0, player1AfterRollback.wins);
		assertEquals(1, player1AfterRollback.losses);
		assertEquals(-1, player1AfterRollback.streak);
		assertEquals(2, player1AfterRollback.rank);
	}
	
	@Test
	public void runBattleDuringContinuousSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		int height = Constants.INITIAL_SYNC_HEIGHT;
		long blockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		startWithAccounts(Arrays.asList(player0, player1));
		
		blockchainSyncThread.start();
		waitForContinuousSyncLoop();
		
		synchronized (recentBlocks) {
			for (int round = 0; round < ROUNDS_TO_FINISH_BATTLE; round++) {
				recentBlocks.addAll(Arrays.asList(
					newCommitBlock("battle1", round, ++blockId, ++height, winningBpHash, null),
					newCommitBlock("battle1", round, ++blockId, ++height, null, losingBpHash),
					newRevealBlock("battle1", round, ++blockId, ++height, winningBp, losingBp)
				));
			}
		}
		
		Account player0AfterSync = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 1) ? p0 : null;
		});
		Account player1AfterSync = accountsDb.getById(player1.numericId());
		
		assertEquals(1, player0AfterSync.wins);
		assertEquals(0, player0AfterSync.losses);
		assertEquals(1, player0AfterSync.streak);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, player0AfterSync.rating);
		assertEquals(1, player0AfterSync.rank);
		
		assertEquals(0, player1AfterSync.wins);
		assertEquals(1, player1AfterSync.losses);
		assertEquals(-1, player1AfterSync.streak);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, player1AfterSync.rating);
		assertEquals(2, player1AfterSync.rank);
	}
	
	@Test
	public void rollBackAllBlocksDuringContinuousSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		String battleId = "battle1";
		
		startWithAccounts(Arrays.asList(player0, player1));
		startWithBattle(player0, player1, battleId, Constants.INITIAL_SYNC_HEIGHT + 1, 1);
		
		Account player0BeforeRollback = accountsDb.getById(player0.numericId());
		Account player1BeforeRollback = accountsDb.getById(player1.numericId());
		
		blockchainSyncThread.start();
		waitForContinuousSyncLoop();
		
		synchronized (recentBlocks) {
			long mismatchedBlockId = 999;
			int height = recentBlocks.remove(recentBlocks.size() - 1).height;
			recentBlocks.add(new Block(mismatchedBlockId, height, height, new ArrayList<Transaction>()));
			recentBlocks.add(new Block(mismatchedBlockId + 1, height + 1, height + 1, new ArrayList<Transaction>()));
		}
		
		Account player0AfterRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 0) ? p0 : null;
		});
		Account player1AfterRollback = accountsDb.getById(player1.numericId());
		Account[] playersAfterRollback = new Account[] { player0AfterRollback, player1AfterRollback };
		
		assertEquals(1, player0BeforeRollback.wins);
		assertEquals(1, player1BeforeRollback.losses);
		
		for (Account player : playersAfterRollback) {
			assertEquals(0, player.wins);
			assertEquals(0, player.losses);
			assertEquals(0, player.streak);
			assertEquals(Constants.INITIAL_RATING, player.rating);
			assertEquals(1, player.rank);
		}
	}
	
	@Test
	public void rollBackSomeBlocksDuringContinuousSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		
		startWithAccounts(Arrays.asList(player0, player1));
		int firstBattleFinishHeight = startWithBattle(player0, player1, "battle1", Constants.INITIAL_SYNC_HEIGHT + 1, 1);
		
		player0 = accountsDb.getById(player0.numericId());
		player1 = accountsDb.getById(player1.numericId());
		startWithBattle(player0, player1, "battle2", firstBattleFinishHeight + 1, firstBattleFinishHeight - Constants.INITIAL_SYNC_HEIGHT + 1);
		
		Account player0BeforeRollback = accountsDb.getById(player0.numericId());
		Account player1BeforeRollback = accountsDb.getById(player1.numericId());
		
		blockchainSyncThread.start();
		waitForContinuousSyncLoop();
		
		synchronized (recentBlocks) {
			long mismatchedBlockId = 999;
			int height = recentBlocks.remove(recentBlocks.size() - 1).height;
			recentBlocks.add(new Block(mismatchedBlockId, height, height, new ArrayList<Transaction>()));
			recentBlocks.add(new Block(mismatchedBlockId + 1, height + 1, height + 1, new ArrayList<Transaction>()));
		}
		
		Account player0AfterRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 1) ? p0 : null;
		});
		Account player1AfterRollback = accountsDb.getById(player1.numericId());
		
		assertEquals(2, player0BeforeRollback.wins);
		assertEquals(2, player1BeforeRollback.losses);
		
		assertEquals(1, player0AfterRollback.wins);
		assertEquals(0, player0AfterRollback.losses);
		assertEquals(1, player0AfterRollback.streak);
		assertEquals(1, player0AfterRollback.rank);
		
		assertEquals(0, player1AfterRollback.wins);
		assertEquals(1, player1AfterRollback.losses);
		assertEquals(-1, player1AfterRollback.streak);
		assertEquals(2, player1AfterRollback.rank);
	}
	
	@Test
	public void reconnectAfterLosingConnectionDuringContinuousSync() throws Exception {
		Account player0 = new Account(PLAYER_0_ID, "player0", "pubkey0");
		Account player1 = new Account(PLAYER_1_ID, "player1", "pubkey1");
		startWithAccounts(Arrays.asList(player0, player1));
		
		blockchainSyncThread.start();
		waitForContinuousSyncLoop();
		
		String battleId = "battle1";
		int height = Constants.INITIAL_SYNC_HEIGHT;
		long blockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		synchronized (recentBlocks) {
			for (int round = 0; round < ROUNDS_TO_FINISH_BATTLE - 1; round++) {
				recentBlocks.addAll(Arrays.asList(
					newCommitBlock(battleId, round, ++blockId, ++height, winningBpHash, losingBpHash),
					newRevealBlock(battleId, round, ++blockId, ++height, winningBp, losingBp)
				));
			}
		}
		
		Mockito
			.doThrow(IgnisNodeCommunicationException.class)
			.doAnswer(i -> {
				synchronized (recentBlocks) {
					return recentBlocks.get(recentBlocks.size() - 1);
				}
			})
			.when(ignisNode).getCurrentBlock();
		
		synchronized (recentBlocks) {
			recentBlocks.addAll(Arrays.asList(
				newCommitBlock(battleId, ROUNDS_TO_FINISH_BATTLE - 1, ++blockId, ++height, winningBpHash, losingBpHash),
				newRevealBlock(battleId, ROUNDS_TO_FINISH_BATTLE - 1, ++blockId, ++height, winningBp, losingBp)
			));
		}
		
		Account player0AfterReconnecting = waitForValue(TIMEOUT_MILLIS, () -> {
			Account p0 = accountsDb.getById(PLAYER_0_ID);
			return (p0.wins == 1) ? p0 : null;
		});
		Account player1AfterReconnecting = accountsDb.getById(PLAYER_1_ID);
		
		assertEquals(1, player0AfterReconnecting.wins);
		assertEquals(1, player1AfterReconnecting.losses);
	}
	
	private void startWithAccounts(Collection<Account> accounts) throws AccountsException {
		accountsDb.addIfNotPresent(accounts);
		
		accountsOnBlockchain.addAll(
			accounts.stream()
				.map(a -> new AccountMetadata(a.numericId(), a.username, a.publicKey))
				.collect(Collectors.toList())
		);
	}
	
	private int startWithBattle(Account winner, Account loser, String battleId, int startingHeight, long startingBlockId) throws AccountsException {
		int height = startingHeight;
		long blockId = startingBlockId;
		
		for (int round = 0; round < ROUNDS_TO_FINISH_BATTLE; round++) {
			recentBlocks.addAll(Arrays.asList(
				newCommitBlock(battleId, round, blockId++, height++, winningBpHash, null),
				newCommitBlock(battleId, round, blockId++, height++, null, losingBpHash),
				newRevealBlock(battleId, round, blockId++, height++, winningBp, losingBp)
			));
		}
		
		accountsDb.update(Arrays.asList(changesFromBattle(winner, loser, --height, --blockId)));
		
		return height;
	}
	
	private static Block newCommitBlock(String battleId, int round, long blockId, int height, String player0Hash, String player1Hash) {
		Function<String, String> toCommitMessage = (hash) -> "{"
			+	"\"battleId\": \"" + battleId + "\", "
			+	"\"round\": " + round + ", "
			+	"\"battlePlanHash\": \"" + hash + "\""
			+ "}";
		
		return newMessageBlock(blockId, height, player0Hash, player1Hash, toCommitMessage);
	}
	
	private static Block newRevealBlock(String battleId, int round, long blockId, int height, BattlePlan player0Bp, BattlePlan player1Bp) {
		Function<BattlePlan, String> toRevealMessage = (bp) -> "{"
			+	"\"battleId\": \"" + battleId + "\", "
			+	"\"round\": " + round + ", "
			+	"\"battlePlan\": " + JsonUtil.serialize(bp)
			+ "}";
		
		return newMessageBlock(blockId, height, player0Bp, player1Bp, toRevealMessage);
	}
	
	private static <T> Block newMessageBlock(long blockId, int height, T player0Data, T player1Data, Function<T, String> toMessage) {
		List<T> data = Arrays.asList(player0Data, player1Data);
		long[] sender = new long[] { PLAYER_0_ID, PLAYER_1_ID };
		long[] recipient = new long[] { PLAYER_1_ID, PLAYER_0_ID };
		
		List<Transaction> txs = IntStream.range(0, 2)
				.filter(i -> data.get(i) != null)
				.mapToObj(i -> new Transaction(sender[i], recipient[i], height, i, blockId, toMessage.apply(data.get(i)), ""))
				.collect(Collectors.toList());
		
		return new Block(blockId, height, height, txs);
	}
	
	private void waitForContinuousSyncLoop() throws InterruptedException {
		long startTime = currentTimeMillis();
		while (!blockchainSyncThread.isCaughtUp() && currentTimeMillis() - startTime < TIMEOUT_MILLIS) {
			Thread.sleep(10);
		}
	}
	
}
