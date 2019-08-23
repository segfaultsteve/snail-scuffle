package com.snailscuffle.game.tx;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.function.BiConsumer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.blockchain.IgnisArchivalNodeConnection;
import com.snailscuffle.game.testutil.ServletUtil;

public class TransactionsServletTest {
	
	@Mock private IgnisArchivalNodeConnection mockIgnisNode;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(mockIgnisNode.isReady()).thenReturn(false);	// keep sync loop happy
	}
	
	@Test
	public void createNewAccount() throws Exception {
		String txJson = "{\"txJson\":\"value\"}";
		String txBytes = "txBytes";
		
		when(mockIgnisNode.createNewAccountTransaction(anyString(), anyString()))
			.thenReturn(new UnsignedTransaction(JsonUtil.deserialize(txJson), txBytes));
		
		String response = sendGETRequest("/newaccount", "player=newplayer&publicKey=pubkey");
		UnsignedTransaction tx = JsonUtil.deserialize(UnsignedTransaction.class, response);
		
		assertEquals(txJson, tx.asJson.toString());
		assertEquals(txBytes, tx.asHex);
	}
	
	@Test
	public void createNewAccountReturnsErrorIfMissingPlayer() throws Exception {
		ErrorResponse error = createNewAccountWithInvalidQuery("publicKey=pubkey");
		
		assertEquals(ErrorResponse.invalidQuery().errorCode, error.errorCode);
		assertEquals(ErrorResponse.invalidQuery().errorDescription, error.errorDescription);
	}
	
	@Test
	public void createNewAccountReturnsErrorIfMissingPublicKey() throws Exception {
		ErrorResponse error = createNewAccountWithInvalidQuery("player=newplayer");
		
		assertEquals(ErrorResponse.invalidQuery().errorCode, error.errorCode);
		assertEquals(ErrorResponse.invalidQuery().errorDescription, error.errorDescription);
	}
	
	private ErrorResponse createNewAccountWithInvalidQuery(String queryString) throws Exception {
		when(mockIgnisNode.createNewAccountTransaction(anyString(), anyString()))
			.thenReturn(new UnsignedTransaction(null, "txBytes"));
	
		String response = sendGETRequest("/newaccount", queryString);
		return JsonUtil.deserialize(ErrorResponse.class, response);
	}
	
	private String sendGETRequest(String path, String queryString) throws Exception {
		BiConsumer<Request, Response> doGetNothrow = (req, resp) -> {
			try {
				Accounts accounts = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
				BlockchainSubsystem blockchainSubsystem = new BlockchainSubsystem(mockIgnisNode, accounts, Constants.RECENT_BATTLES_DEPTH);
				(new TransactionsServlet(blockchainSubsystem)).doGet(req, resp);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		
		return ServletUtil.sendGETRequest(doGetNothrow, path, queryString);
	}
	
}
