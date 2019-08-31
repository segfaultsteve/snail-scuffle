package com.snailscuffle.game.blockchain;

import static com.snailscuffle.game.testutil.SyncUtil.*;

import static com.snailscuffle.game.testutil.ServletUtil.sendHttpRequest;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snailscuffle.common.util.JsonUtil;
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
		
		String unsignedTxJson = sendHttpRequest(this::transactionsPUT, "/new-account", "", serialize(transactionRequestBody));
		UnsignedTransaction unsignedTx = JsonUtil.deserialize(UnsignedTransaction.class, unsignedTxJson);
		sendHttpRequest(this::transactionsPOST, "/", "", serialize(unsignedTx.asJson));
		
		Account account = waitForValue(TIMEOUT_MILLIS, () -> {
			try {
				String accountJson = sendHttpRequest(this::accountsGET, "/", "player=" + player);
				return JsonUtil.deserialize(Account.class, accountJson);
			} catch (IOException e) {
				return null;
			}
		});
		
		assertEquals(player, account.username);
		assertEquals(publicKey, account.publicKey);
	}
	
	private void transactionsPUT(Request request, Response response) {
		try {
			transactionsServlet.doPut(request, response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void transactionsPOST(Request request, Response response) {
		try {
			transactionsServlet.doPost(request, response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void accountsGET(Request request, Response response) {
		try {
			accountsServlet.doGet(request, response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String serialize(Object data) {
		try {
			return new ObjectMapper().writer().writeValueAsString(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
