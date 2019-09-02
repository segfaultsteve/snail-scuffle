package com.snailscuffle.game.blockchain;

import static com.snailscuffle.common.util.JsonUtil.deserialize;
import static com.snailscuffle.game.testutil.BlockchainJson.serialize;
import static com.snailscuffle.game.testutil.ServletUtil.sendHttpRequest;
import static com.snailscuffle.game.testutil.SyncUtil.waitForValue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import com.snailscuffle.game.testutil.ThrowingSupplier;
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
	public void createNewAccount() throws Exception {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		
		Account account = waitForValue(TIMEOUT_MILLIS, getAccount(PLAYER_0_USERNAME));
		
		assertEquals(PLAYER_0_ID, account.numericId());
		assertEquals(PLAYER_0_USERNAME, account.username);
		assertEquals(PLAYER_0_PUBLIC_KEY, account.publicKey);
	}
	
	@Test
	public void playABattle() throws Exception {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		
		waitForValue(TIMEOUT_MILLIS, getAccount(PLAYER_0_USERNAME));
		waitForValue(TIMEOUT_MILLIS, getAccount(PLAYER_1_USERNAME));
		
		runBattle(losingBp(), winningBp());
		
		Account player0 = waitForValue(TIMEOUT_MILLIS, () -> {
			String player0Json = sendHttpRequest(accountsServlet::doGet, "/", "player=" + PLAYER_0_USERNAME);
			Account p0 = deserialize(Account.class, player0Json);
			return (p0.losses > 0) ? p0 : null;
		});
		Account player1 = getAccount(PLAYER_1_USERNAME).get();
		
		assertEquals(0, player0.wins);
		assertEquals(1, player0.losses);
		assertEquals(-1, player0.streak);
		assertEquals(Constants.INITIAL_RATING - Constants.MAX_RATING_CHANGE / 2, player0.rating);
		assertEquals(2, player0.rank);
		assertEquals(EXPECTED_BALANCE, player0.balance, DELTA);
		
		assertEquals(1, player1.wins);
		assertEquals(0, player1.losses);
		assertEquals(1, player1.streak);
		assertEquals(Constants.INITIAL_RATING + Constants.MAX_RATING_CHANGE / 2, player1.rating);
		assertEquals(1, player1.rank);
		assertEquals(EXPECTED_BALANCE, player1.balance, DELTA);
	}
	
	@Test
	public void resolveAFork() throws Exception {
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		runBattle(losingBp(), winningBp());
		
		Account player0BeforeRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			String player0Json = sendHttpRequest(accountsServlet::doGet, "/", "player=" + PLAYER_0_USERNAME);
			Account p0 = deserialize(Account.class, player0Json);
			return (p0.losses > 0) ? p0 : null;
		});
		Account player1BeforeRollback = getAccount(PLAYER_1_USERNAME).get();
		
		blockchainStub.rollBackAllBlocks();
		
		submitNewAccountTransaction(PLAYER_0_USERNAME, PLAYER_0_PUBLIC_KEY);
		submitNewAccountTransaction(PLAYER_1_USERNAME, PLAYER_1_PUBLIC_KEY);
		runBattle(winningBp(), losingBp());		// let player 0 win this time
		
		Account player0AfterRollback = waitForValue(TIMEOUT_MILLIS, () -> {
			String player0Json = sendHttpRequest(accountsServlet::doGet, "/", "player=" + PLAYER_0_USERNAME);
			Account p0 = deserialize(Account.class, player0Json);
			return (p0.wins > 0) ? p0 : null;
		});
		Account player1AfterRollback = getAccount(PLAYER_1_USERNAME).get();
		
		assertEquals(1, player0BeforeRollback.losses);
		assertEquals(1, player1BeforeRollback.wins);
		
		assertEquals(1, player0AfterRollback.wins);
		assertEquals(1, player1AfterRollback.losses);
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
	
	private void runBattle(BattlePlan player0Bp, BattlePlan player1Bp) {
		for (int i = 0; i < ROUNDS; i++) {
			submitBpCommitTransaction(PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, BATTLE_ID, i, player0Bp);
			submitBpCommitTransaction(PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, BATTLE_ID, i, player1Bp);
			
			submitBpRevealTransaction(PLAYER_0_PUBLIC_KEY, PLAYER_1_ID, BATTLE_ID, i, player0Bp);
			submitBpRevealTransaction(PLAYER_1_PUBLIC_KEY, PLAYER_0_ID, BATTLE_ID, i, player1Bp);
		}
	}
	
	private void submitBpCommitTransaction(String publicKey, long recipient, String battleId, int round, BattlePlan battlePlan) {
		submitBattlePlanTransaction("/battle-plan-commit", publicKey, recipient, battleId, round, battlePlan);
	}
	
	private void submitBpRevealTransaction(String publicKey, long recipient, String battleId, int round, BattlePlan battlePlan) {
		submitBattlePlanTransaction("/battle-plan-reveal", publicKey, recipient, battleId, round, battlePlan);
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
	
	private ThrowingSupplier<Account> getAccount(String player) {
		return () -> {
			String accountJson = sendHttpRequest(accountsServlet::doGet, "/", "player=" + player);
			return deserialize(Account.class, accountJson);
		};
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
	
}
