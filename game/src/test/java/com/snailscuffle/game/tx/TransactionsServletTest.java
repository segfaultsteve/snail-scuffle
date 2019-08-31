package com.snailscuffle.game.tx;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;

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
		
		HashMap<String, Object> requestBody = new HashMap<>();
		requestBody.put("player", "newplayer");
		requestBody.put("publicKey", "pubkey");
		
		String response = sendPUTRequest("/new-account", requestBody);
		UnsignedTransaction tx = JsonUtil.deserialize(UnsignedTransaction.class, response);
		
		assertEquals(txJson, tx.asJson.toString());
		assertEquals(txBytes, tx.asHex);
	}
	
	@Test
	public void createNewAccountReturnsErrorIfMissingPlayer() throws Exception {
		HashMap<String, Object> requestBody = new HashMap<>();
		requestBody.put("publicKey", "pubkey");
		
		ErrorResponse error = createNewAccountWithInvalidRequest(requestBody);
		
		assertEquals(ErrorResponse.invalidQuery().errorCode, error.errorCode);
		assertEquals(ErrorResponse.invalidQuery().errorDescription, error.errorDescription);
	}
	
	@Test
	public void createNewAccountReturnsErrorIfMissingPublicKey() throws Exception {
		HashMap<String, Object> requestBody = new HashMap<>();
		requestBody.put("player", "newplayer");
		
		ErrorResponse error = createNewAccountWithInvalidRequest(requestBody);
		
		assertEquals(ErrorResponse.invalidQuery().errorCode, error.errorCode);
		assertEquals(ErrorResponse.invalidQuery().errorDescription, error.errorDescription);
	}
	
	private ErrorResponse createNewAccountWithInvalidRequest(HashMap<String, Object> body) throws Exception {
		when(mockIgnisNode.createNewAccountTransaction(anyString(), anyString()))
			.thenReturn(new UnsignedTransaction(null, "txBytes"));
		
		String response = sendPUTRequest("/new-account", body);
		return JsonUtil.deserialize(ErrorResponse.class, response);
	}
	
	private String sendPUTRequest(String path, HashMap<String, Object> body) throws Exception {
		Accounts accounts = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
		BlockchainSubsystem blockchainSubsystem = new BlockchainSubsystem(mockIgnisNode, accounts, Constants.RECENT_BATTLES_DEPTH);
		TransactionsServlet txServlet = new TransactionsServlet(blockchainSubsystem);
		
		return ServletUtil.sendHttpRequest((req, resp) -> txServlet.doPut(req, resp), path, "", JsonUtil.serialize(body));
	}
	
}
