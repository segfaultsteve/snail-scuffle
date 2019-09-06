package com.snailscuffle.game.blockchain;

import static com.snailscuffle.common.util.JsonUtil.deserialize;
import static com.snailscuffle.common.util.JsonUtil.serialize;
import static com.snailscuffle.game.testutil.ServletUtil.sendHttpRequest;
import static com.snailscuffle.game.testutil.SyncUtil.waitForValue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.accounts.AccountsServlet;
import com.snailscuffle.game.tx.TransactionsServlet;
import com.snailscuffle.game.tx.UnsignedTransaction;

public class BlockchainSubsystemIntegrationTests {
	
	private static final String BASE_URL = "https://game.snailscuffle.com";
	private static final int SYNC_LOOP_PERIOD_MILLIS = 100;
	private static final int TIMEOUT_MILLIS = 2000;
	private static final long PLAYER_0_ID = 1;
	private static final long PLAYER_1_ID = 2;
	private static final String PLAYER_0_USERNAME = "player0";
	private static final String PLAYER_1_USERNAME = "player1";
	private static final String PLAYER_0_PUBLIC_KEY = "player0PublicKey";
	private static final String PLAYER_1_PUBLIC_KEY = "player1PublicKey";
	private static final int ROUNDS = 3;
	private static final String BATTLE_ID = "test battle";
	private static final double EXPECTED_BALANCE = IgnisArchivalNodeConnection.nqtToDouble(BlockchainStub.IGNIS_BALANCE_NQT);
	private static final double DELTA = 0.001;
	
	private BlockchainStub blockchainStub;
	private BlockchainSubsystem blockchainSubsystem;
	private AccountsServlet accountsServlet;
	private TransactionsServlet transactionsServlet;
	
	@Before
	public void setUp() throws AccountsException {
		blockchainStub = new BlockchainStub(BASE_URL);
		IgnisArchivalNodeConnection ignisNode = new IgnisArchivalNodeConnection(BASE_URL, blockchainStub.mockHttpClient);
		Accounts accountsDb = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
		blockchainSubsystem = new BlockchainSubsystem(ignisNode, accountsDb, Constants.RECENT_BATTLES_DEPTH, SYNC_LOOP_PERIOD_MILLIS);
		
		blockchainStub.addPublicKey(PLAYER_0_ID, PLAYER_0_PUBLIC_KEY);
		blockchainStub.addPublicKey(PLAYER_1_ID, PLAYER_1_PUBLIC_KEY);
		
		accountsServlet = new AccountsServlet(blockchainSubsystem);
		transactionsServlet = new TransactionsServlet(blockchainSubsystem);
	}
	
	@After
	public void tearDown() {
		blockchainSubsystem.close();
	}
	
	@Test
	public void createNewAccount() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		
		Account account = waitForAccount(PLAYER_0_USERNAME);
		
		assertEquals(PLAYER_0_ID, account.numericId());
		assertEquals(PLAYER_0_USERNAME, account.username);
		assertEquals(PLAYER_0_PUBLIC_KEY, account.publicKey);
	}
	
	@Test
	public void playABattle() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		runBattle(PLAYER_0_ID, PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, PLAYER_1_PUBLIC_KEY);
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		assertEquals(1, player0.streak);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, player0.rating);
		assertEquals(1, player0.rank);
		assertEquals(EXPECTED_BALANCE, player0.balance, DELTA);
		
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
		assertEquals(-1, player1.streak);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, player1.rating);
		assertEquals(2, player1.rank);
		assertEquals(EXPECTED_BALANCE, player1.balance, DELTA);
	}
	
	@Test
	public void ignoreUnrecognizedAccount() {
		final long PLAYER_2_ID = 3;
		final String PLAYER_2_PUBLIC_KEY = "player2PublicKey";
		blockchainStub.addPublicKey(PLAYER_2_ID, PLAYER_2_PUBLIC_KEY);
		
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		runBattle("invalid battle", PLAYER_0_ID, PLAYER_0_PUBLIC_KEY, PLAYER_2_ID, PLAYER_2_PUBLIC_KEY);	// invalid because player 2 does not have an account
		runBattle("valid battle", PLAYER_0_ID, PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, PLAYER_1_PUBLIC_KEY);		// run a valid battle to make sure the sync thread hasn't crashed
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
	}
	
	@Test
	public void resolveAFork() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		runBattle(PLAYER_0_ID, PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, PLAYER_1_PUBLIC_KEY);
		
		Account player0BeforeRollback = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1BeforeRollback = waitForAccount(PLAYER_1_USERNAME);
		
		blockchainStub.rollBackAllBlocks();
		
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		runBattle(PLAYER_1_ID, PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, PLAYER_0_PUBLIC_KEY);	// let player 1 win this time
		
		Account player0AfterRollback = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.losses > 0);
		Account player1AfterRollback = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0BeforeRollback.wins);
		assertEquals(1, player1BeforeRollback.losses);
		
		assertEquals(1, player0AfterRollback.losses);
		assertEquals(1, player1AfterRollback.wins);
	}
	
	@Test
	public void ignoreBattleThatOpponentNeverAcknowledges() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, winningBp());
		
		for (int i = 0; i < 10; i++) {
			blockchainStub.addBlock();
		}
		
		// Run a complete battle, too, so that we can assert that it has been synced.
		runBattle(PLAYER_1_ID, PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, PLAYER_0_PUBLIC_KEY);
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.losses > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(0, player0.wins);
		assertEquals(1, player0.losses);
		
		assertEquals(1, player1.wins);
		assertEquals(0, player1.losses);
	}
	
	@Test
	public void ignoreUnfinishedBattle() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, winningBp());
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, winningBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, losingBp());
		
		for (int i = 0; i < 10; i++) {
			blockchainStub.addBlock();
		}
		
		// Run a complete battle, too, so that we can assert that it has been synced.
		runBattle(PLAYER_1_ID, PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, PLAYER_0_PUBLIC_KEY);
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.losses > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(0, player0.wins);
		assertEquals(1, player0.losses);
		
		assertEquals(1, player1.wins);
		assertEquals(0, player1.losses);
	}
	
	@Test
	public void forfeitBattleByNotRevealingBattlePlan() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, winningBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		
		for (int i = 0; i < 10; i++) {
			blockchainStub.addBlock();
		}
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
	}
	
	@Test
	public void forfeitBattleByNotCommittingBattlePlan() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		// round 0
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, winningBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, winningBp());
		
		// round 1
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 1, losingBp());
		
		for (int i = 0; i < 10; i++) {
			blockchainStub.addBlock();
		}
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
	}
	
	@Test
	public void forfeitBattleIfBattlePlanDoesNotMatchHash() {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-commit", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, winningBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, "ignored battle", 0, losingBp());
		submitBattlePlanTransaction("/battle-plan-reveal", PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, "ignored battle", 0, losingBp());		// doesn't match hash
		
		for (int i = 0; i < 10; i++) {
			blockchainStub.addBlock();
		}
		
		Account player0 = waitForAccountWith(PLAYER_0_USERNAME, p0 -> p0.wins > 0);
		Account player1 = waitForAccount(PLAYER_1_USERNAME);
		
		assertEquals(1, player0.wins);
		assertEquals(0, player0.losses);
		
		assertEquals(0, player1.wins);
		assertEquals(1, player1.losses);
	}
	
	private void submitNewAccountTransaction(String username, String publicKey) {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("player", username);
		requestBody.put("publicKey", publicKey);
		
		String newAccountTxJson = sendHttpRequest(transactionsServlet::doPut, "/new-account", "", serialize(requestBody));
		
		try {
			UnsignedTransaction newAccountTx = deserialize(UnsignedTransaction.class, newAccountTxJson);
			sendHttpRequest(transactionsServlet::doPost, "/", "", serialize(newAccountTx.asJson));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void runBattle(long winnerId, String winnerPublicKey, long loserId, String loserPublicKey) {
		runBattle(BATTLE_ID, winnerId, winnerPublicKey, loserId, loserPublicKey);
	}
	
	private void runBattle(String battleId, long winnerId, String winnerPublicKey, long loserId, String loserPublicKey) {
		for (int i = 0; i < ROUNDS; i++) {
			submitBattlePlanTransaction("/battle-plan-commit", winnerPublicKey, loserId, battleId, i, winningBp());
			submitBattlePlanTransaction("/battle-plan-commit", loserPublicKey, winnerId, battleId, i, losingBp());
			
			submitBattlePlanTransaction("/battle-plan-reveal", winnerPublicKey, loserId, battleId, i, winningBp());
			submitBattlePlanTransaction("/battle-plan-reveal", loserPublicKey, winnerId, battleId, i, losingBp());
		}
	}
	
	private void submitBattlePlanTransaction(String path, String publicKey, long recipient, String battleId, int round, BattlePlan battlePlan) {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("publicKey", publicKey);
		requestBody.put("recipient", Long.toUnsignedString(recipient));
		requestBody.put("battleId", battleId);
		requestBody.put("round", round);
		requestBody.put("battlePlan", battlePlan);
		
		String bpTxJson = sendHttpRequest(transactionsServlet::doPut, path, "", serialize(requestBody));
		try {
			UnsignedTransaction bpTx = deserialize(UnsignedTransaction.class, bpTxJson);
			sendHttpRequest(transactionsServlet::doPost, "/", "", serialize(bpTx.asJson));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static BattlePlan winningBp() {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		bp.shell = Shell.ALUMINUM;
		bp.accessory = Accessory.DEFIBRILLATOR;
		bp.validate();
		return bp;
	}
	
	private static BattlePlan losingBp() {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.RIFLE;
		bp.shell = Shell.ALUMINUM;
		// no accessory - this should guarantee a loss
		bp.validate();
		return bp;
	}
	
	private Account waitForAccount(String username) {
		return waitForAccountWith(username, a -> true);
	}
	
	private Account waitForAccountWith(String username, Predicate<Account> selector) {
		return waitForValue(TIMEOUT_MILLIS, () -> {
			String accountJson = sendHttpRequest(accountsServlet::doGet, "/", "player=" + username);
			Account account = deserialize(Account.class, accountJson);
			return selector.test(account) ? account : null;
		});
	}
	
}
