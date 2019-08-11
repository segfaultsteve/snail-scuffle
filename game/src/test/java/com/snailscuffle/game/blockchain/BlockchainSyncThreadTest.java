package com.snailscuffle.game.blockchain;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
import com.snailscuffle.game.blockchain.data.AccountMetadata;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.Transaction;

@FunctionalInterface
interface ThrowingSupplier<T> {
	T get() throws Exception;
}

public class BlockchainSyncThreadTest {
	
	private static final int TIMEOUT_IN_MILLISECONDS = 1000;
	private static final long PLAYER_0_ID = 1;
	private static final long PLAYER_1_ID = 2;
	
	@Mock private IgnisArchivalNodeConnection ignisNode;
	private List<AccountMetadata> accountsOnBlockchain;
	private List<Block> recentBlocks;
	private Accounts accountsDb;
	
	private BattlePlan winningBp;
	private String winningBpHash;
	private BattlePlan losingBp;
	private String losingBpHash;
	
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
			int count = invocation.getArgument(0);
			synchronized (recentBlocks) {
				return new ArrayList<>(recentBlocks.subList(recentBlocks.size() - count, recentBlocks.size()));
			}
		});
		when(ignisNode.getBlockAtHeight(anyInt())).thenAnswer(invocation -> {
			int height = invocation.getArgument(0);
			synchronized (recentBlocks) {
				return recentBlocks.stream().filter(b -> b.height == height).findFirst().get();
			}
		});
		when(ignisNode.getAllPlayerAccounts()).thenReturn(accountsOnBlockchain);
		when(ignisNode.getPlayerAccount(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			return accountsOnBlockchain.stream().filter(a -> a.id == id).findFirst().get();
		});
		when(ignisNode.getMessagesFrom(anyLong(), anyInt(), anyInt())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			int initialHeight = invocation.getArgument(1);
			int finalHeight = invocation.getArgument(2);
			synchronized (recentBlocks) {
				return recentBlocks.stream()
						.flatMap(b -> b.transactions.stream())
						.filter(t -> t.sender == id && t.height >= initialHeight && t.height <= finalHeight)
						.collect(Collectors.toList());
			}
		});
		
		addBlocks(new Block(Constants.INITIAL_SYNC_BLOCK_ID, Constants.INITIAL_SYNC_HEIGHT, 0, new ArrayList<>()));
		
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
	}
	
	@Test
	public void syncPreexistingAccounts() throws Exception {
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(PLAYER_0_ID, "player1", "pubkey1"),
			new AccountMetadata(PLAYER_1_ID, "player2", "pubkey2")
		));
		
		startBlockchainSyncThread();
		Account player1 = waitForValue(() -> accountsDb.getById(1));
		Account player2 = waitForValue(() -> accountsDb.getById(2));
		
		assertEquals("player1", player1.username);
		assertEquals("pubkey1", player1.publicKey);
		assertEquals("player2", player2.username);
		assertEquals("pubkey2", player2.publicKey);
	}
	
	@Test
	public void updateUsername() throws Exception {
		int accountId = 1;
		accountsDb.addIfNotPresent(Arrays.asList(
			new Account(accountId, "oldname", "pubkey")
		));
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(accountId, "newname", "pubkey")
		));
		
		startBlockchainSyncThread();
		Account player = waitForValue(() -> accountsDb.getByUsername("newname"));
		
		assertNotNull(player);
	}
	
	@Test
	public void backtrackPastLastSyncHeightToFindBattleInProgress() throws Exception {
		accountsOnBlockchain.addAll(Arrays.asList(
			new AccountMetadata(PLAYER_0_ID, "player1", "pubkey1"),
			new AccountMetadata(PLAYER_1_ID, "player2", "pubkey2")
		));
		
		String battleId = "battle1";
		int height = Constants.INITIAL_SYNC_HEIGHT;
		long blockId = Constants.INITIAL_SYNC_BLOCK_ID;
		
		addBlocks(
			newCommitBlock(battleId, 0, ++blockId, ++height, winningBpHash, null),
			newCommitBlock(battleId, 0, ++blockId, ++height, null, losingBpHash),
			newRevealBlock(battleId, 0, ++blockId, ++height, winningBp, losingBp),
			newCommitBlock(battleId, 1, ++blockId, ++height, winningBpHash, losingBpHash),
			newRevealBlock(battleId, 1, ++blockId, ++height, null, losingBp),
			newRevealBlock(battleId, 1, ++blockId, ++height, winningBp, null)
		);
		
		accountsDb.updateSyncState(height, blockId);	// battle still in progress at this height
		
		addBlocks(
			newCommitBlock(battleId, 2, ++blockId, ++height, winningBpHash, losingBpHash),
			newRevealBlock(battleId, 2, ++blockId, ++height, winningBp, losingBp)
		);
		
		startBlockchainSyncThread();
		
		Account player0 = waitForValue(() -> {
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
	
	private void addBlocks(Block... blocks) {
		synchronized (recentBlocks) {
			for (Block block : blocks) {
				recentBlocks.add(block);
			}
		}
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
		Function<BattlePlan, String> serializeNoThrow = (bp) -> {
			try {
				return JsonUtil.serialize(bp);
			} catch (IOException e) {
				return null;
			}
		};
		
		Function<BattlePlan, String> toRevealMessage = (bp) -> "{"
			+	"\"battleId\": \"" + battleId + "\", "
			+	"\"round\": " + round + ", "
			+	"\"battlePlan\": " + serializeNoThrow.apply(bp)
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
	
	private void startBlockchainSyncThread() {
		(new BlockchainSyncThread(ignisNode, accountsDb, Constants.RECENT_BATTLES_DEPTH)).start();
	}
	
	private static <T> T waitForValue(ThrowingSupplier<T> func) throws InterruptedException {
		T value = null;
		long startTime = currentTimeMillis();
		while (value == null && currentTimeMillis() - startTime < TIMEOUT_IN_MILLISECONDS) {
			Thread.sleep(10);
			try {
				value = func.get();
			} catch (Exception e) {
				value = null;
			}
		}
		return value;
	}
	
}
