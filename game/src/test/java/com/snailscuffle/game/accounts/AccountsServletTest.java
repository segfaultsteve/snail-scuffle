package com.snailscuffle.game.accounts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.BlockchainDataNotFoundException;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.blockchain.IgnisArchivalNodeConnection;
import com.snailscuffle.game.testutil.ServletUtil;

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
		when(mockIgnisNode.getBalance(ACCOUNT1.id)).thenReturn(ACCOUNT1.balance);
		when(mockIgnisNode.getBalance(ACCOUNT2.id)).thenReturn(ACCOUNT2.balance);
		when(mockIgnisNode.getBalance(ACCOUNT3.id)).thenReturn(ACCOUNT3.balance);
		
		accounts = new Accounts(":memory:", Constants.RECENT_BATTLES_DEPTH);
		blockchainSubsystem = new BlockchainSubsystem(mockIgnisNode, accounts, Constants.RECENT_BATTLES_DEPTH);
	}

	@Test
	public void getAccountById() throws Exception {
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT1));
		
		String response = sendGETRequest("id=" + ACCOUNT1.id);
		Account retrievedAccount = JsonUtil.deserialize(Account.class, response);
		
		assertAccountsAreEqual(ACCOUNT1, retrievedAccount);
	}
	
	@Test
	public void getAccountByUsername() throws Exception {
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT1));
		
		String response = sendGETRequest("player=" + ACCOUNT1.username);
		Account retrievedAccount = JsonUtil.deserialize(Account.class, response);
		
		assertAccountsAreEqual(ACCOUNT1, retrievedAccount);
	}
	
	@Test
	public void returnCorrectRanksForIndividualAccounts() throws Exception {
		// insertion order shouldn't matter
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT3, ACCOUNT1, ACCOUNT2));
		
		int rank1 = getRank(ACCOUNT1);
		int rank2 = getRank(ACCOUNT2);
		int rank3 = getRank(ACCOUNT3);
		
		assertEquals(1, rank1);
		assertEquals(2, rank2);
		assertEquals(3, rank3);
	}
	
	@Test
	public void getOverallRanking() throws Exception {
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT3, ACCOUNT1, ACCOUNT2));
		
		Account[] ranked = JsonUtil.deserialize(Account[].class, sendGETRequest(""));
		
		assertEquals(3, ranked.length);
		assertEquals(ACCOUNT1.id, ranked[0].id);
		assertEquals(ACCOUNT2.id, ranked[1].id);
		assertEquals(ACCOUNT3.id, ranked[2].id);
	}
	
	@Test
	public void getOverallRankingWithExplicitCount() throws Exception {
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT3, ACCOUNT1, ACCOUNT2));
		
		Account[] ranked = JsonUtil.deserialize(Account[].class, sendGETRequest("count=2"));
		
		assertEquals(2, ranked.length);
		assertEquals(ACCOUNT1.id, ranked[0].id);
		assertEquals(ACCOUNT2.id, ranked[1].id);
	}
	
	@Test
	public void getOverallRankingWithOffset() throws Exception {
		accounts.addIfNotPresent(Arrays.asList(ACCOUNT3, ACCOUNT1, ACCOUNT2));
		
		Account[] ranked = JsonUtil.deserialize(Account[].class, sendGETRequest("count=2&offset=1"));
		
		assertEquals(2, ranked.length);
		assertEquals(ACCOUNT2.id, ranked[0].id);
		assertEquals(ACCOUNT3.id, ranked[1].id);
	}
	
	@Test
	public void getBalanceOfUntrackedAccount() throws Exception {
		String response = sendGETRequest("id=" + ACCOUNT1.id);
		Account retrievedAccount = JsonUtil.deserialize(Account.class, response);
		
		assertEquals(ACCOUNT1.id, retrievedAccount.id);
		assertEquals(ACCOUNT1.balance, retrievedAccount.balance, DELTA);
		assertEquals("", retrievedAccount.username);
		assertEquals("", retrievedAccount.publicKey);
	}
	
	@Test
	public void reportAccountNotFound() throws Exception {
		Mockito.doThrow(BlockchainDataNotFoundException.class).when(mockIgnisNode).getBalance(ACCOUNT1.id);
		int expectedErrorCode = ErrorResponse.failedToRetrieveAccount().errorCode;
		
		String response = sendGETRequest("id=" + ACCOUNT1.id);
		ErrorResponse error = JsonUtil.deserialize(ErrorResponse.class, response);
		
		assertEquals(expectedErrorCode, error.errorCode);
		assertNotNull(error.errorDescription);
		assertNotNull(error.errorDetails);
	}
	
	private int getRank(Account account) throws Exception {
		String response = sendGETRequest("id=" + account.id);
		return JsonUtil.deserialize(Account.class, response).rank;
	}
	
	private String sendGETRequest(String queryString) {
		AccountsServlet accountsServlet = new AccountsServlet(blockchainSubsystem);
		return ServletUtil.sendHttpRequest((req, resp) -> accountsServlet.doGet(req, resp), "", queryString);
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
