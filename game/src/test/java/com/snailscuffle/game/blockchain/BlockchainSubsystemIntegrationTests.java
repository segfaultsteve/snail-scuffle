package com.snailscuffle.game.blockchain;

import static com.snailscuffle.common.util.JsonUtil.deserialize;
import static com.snailscuffle.game.testutil.BlockchainJson.serialize;
import static com.snailscuffle.game.testutil.ServletUtil.sendHttpRequest;
import static com.snailscuffle.game.testutil.SyncUtil.waitForValue;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.accounts.AccountsServlet;
import com.snailscuffle.game.tx.TransactionsServlet;
import com.snailscuffle.game.tx.UnsignedTransaction;

public class BlockchainSubsystemIntegrationTests {
	
	private static final int SYNC_LOOP_PERIOD_MILLIS = 100;
	private static final int TIMEOUT_MILLIS = 1000;
	
	private AccountsServlet accountsServlet;
	private TransactionsServlet transactionsServlet;
	private BlockchainStub blockchainStub;
	
	@Before
	public void setUp() throws AccountsException {
		blockchainStub = new BlockchainStub();
		Accounts accountsDb = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
		BlockchainSubsystem blockchainSubsystem = new BlockchainSubsystem(blockchainStub.ignisNode, accountsDb, Constants.RECENT_BATTLES_DEPTH, SYNC_LOOP_PERIOD_MILLIS);
		
		accountsServlet = new AccountsServlet(blockchainSubsystem);
		transactionsServlet = new TransactionsServlet(blockchainSubsystem);
	}
	
	@Test
	public void createNewAccount() throws Exception {
		String player = "player1";
		String publicKey = "player1PublicKey";
		blockchainStub.addPublicKey(1, publicKey);
		
		Map<String, Object> transactionRequestBody = new HashMap<>();
		transactionRequestBody.put("player", player);
		transactionRequestBody.put("publicKey", publicKey);
		
		String unsignedTxJson = sendHttpRequest(transactionsServlet::doPut, "/new-account", "", serialize(transactionRequestBody));
		UnsignedTransaction unsignedTx = deserialize(UnsignedTransaction.class, unsignedTxJson);
		
		sendHttpRequest(transactionsServlet::doPost, "/", "", serialize(unsignedTx.asJson));
		
		Account account = waitForValue(TIMEOUT_MILLIS, () -> {
			String accountJson = sendHttpRequest(accountsServlet::doGet, "/", "player=" + player);
			return deserialize(Account.class, accountJson);
		});
		
		assertEquals(player, account.username);
		assertEquals(publicKey, account.publicKey);
	}
	
}
