package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.blockchain.IgnisArchivalNodeConnection;

public class AccountsServletTest {
	
	private static final Account ACCOUNT1 = new Account(1, "account1", "pubkey1", 3, 0, 3, 1500, 1, 100);
	private static final Account ACCOUNT2 = new Account(2, "account2", "pubkey2", 2, 2, 1, 1000, 2, 200);
	private static final Account ACCOUNT3 = new Account(3, "account3", "pubkey3", 0, 3, -3, 700, 3, 300);
	private static final double DELTA = 0.001;		// for comparing doubles
	
	@Mock private IgnisArchivalNodeConnection mockIgnisNode;
	private Accounts accounts;
	private BlockchainSubsystem blockchainSubsystem;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(mockIgnisNode.getBalance(ACCOUNT1.numericId())).thenReturn(ACCOUNT1.balance);
		when(mockIgnisNode.getBalance(ACCOUNT2.numericId())).thenReturn(ACCOUNT2.balance);
		when(mockIgnisNode.getBalance(ACCOUNT3.numericId())).thenReturn(ACCOUNT3.balance);
		
		accounts = new Accounts(":memory:", Constants.MAX_SNAPSHOT_COUNT);
		blockchainSubsystem = new BlockchainSubsystem(mockIgnisNode, accounts);
	}

	@Test
	public void getAccountById() throws Exception {
		accounts.insertOrUpdate(Arrays.asList(ACCOUNT1), Constants.INITIAL_SYNC_HEIGHT + 1, 0);
		
		String response = sendGetRequest("id=" + ACCOUNT1.id);
		Account retrievedAccount = JsonUtil.deserialize(Account.class, response);
		
		assertAccountsAreEqual(ACCOUNT1, retrievedAccount);
	}
	
	@Test
	public void getAccountByUsername() throws Exception {
		accounts.insertOrUpdate(Arrays.asList(ACCOUNT1), Constants.INITIAL_SYNC_HEIGHT + 1, 0);
		
		String response = sendGetRequest("player=" + ACCOUNT1.username);
		Account retrievedAccount = JsonUtil.deserialize(Account.class, response);
		
		assertAccountsAreEqual(ACCOUNT1, retrievedAccount);
	}
	
	@Test
	public void determineCorrectRanking() throws Exception {
		// insertion order shouldn't matter
		accounts.insertOrUpdate(Arrays.asList(ACCOUNT3, ACCOUNT1, ACCOUNT2), Constants.INITIAL_SYNC_HEIGHT + 1, 0);
		
		int rank1 = getRank(ACCOUNT1);
		int rank2 = getRank(ACCOUNT2);
		int rank3 = getRank(ACCOUNT3);
		
		assertEquals(1, rank1);
		assertEquals(2, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void reportAccountNotFound() throws Exception {
		int expectedErrorCode = ErrorResponse.failedToRetrieveAccount().errorCode;
		
		String response = sendGetRequest("id=123");
		ErrorResponse error = JsonUtil.deserialize(ErrorResponse.class, response);
		
		assertEquals(expectedErrorCode, error.errorCode);
		assertNotNull(error.errorDescription);
		assertNotNull(error.errorDetails);
	}
	
	private String sendGetRequest(String queryString) throws Exception {
		Request request = mock(Request.class);
		when(request.getQueryString()).thenReturn(queryString);
		when(request.getParameterMap()).thenReturn(parameterMapFor(queryString));
		
		Response response = mock(Response.class);
		StringWriter responseBuffer = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(responseBuffer));
		
		(new AccountsServlet(blockchainSubsystem)).doGet(request, response);
		return responseBuffer.toString();
	}
	
	private static Map<String, String[]> parameterMapFor(String queryString) {
		Map<String, String[]> parameterMap = new HashMap<>();
		String[] queryParams = queryString.split("&");
		for (String param : queryParams) {
			String[] kvp = param.split("=");
			parameterMap.put(kvp[0], new String[] { kvp[1] });
		}
		return parameterMap;
	}
	
	private int getRank(Account account) throws Exception {
		String response = sendGetRequest("id=" + account.id);
		return JsonUtil.deserialize(Account.class, response).rank;
	}
	
	private static void assertAccountsAreEqual(Account expected, Account actual) {
		assertEquals(expected.id, actual.id);
		assertEquals(expected.username, actual.username);
		assertEquals(expected.publicKey, actual.publicKey);
		assertEquals(expected.wins, actual.wins);
		assertEquals(expected.losses, actual.losses);
		assertEquals(expected.streak, actual.streak);
		assertEquals(expected.rating, actual.rating);
		assertEquals(expected.rank, actual.rank);
		assertEquals(expected.balance, actual.balance, DELTA);
	}
	
}
