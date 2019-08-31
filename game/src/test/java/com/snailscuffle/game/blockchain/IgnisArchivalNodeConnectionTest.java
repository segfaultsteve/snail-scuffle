package com.snailscuffle.game.blockchain;

import static com.snailscuffle.game.testutil.BlockchainJson.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.data.AccountMetadata;
import com.snailscuffle.game.blockchain.data.Alias;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.Transaction;

public class IgnisArchivalNodeConnectionTest {
	
	private static final String BASE_URL = "https://game.snailscuffle.com";
	
	@Mock private HttpClient mockHttpClient;
	private IgnisArchivalNodeConnection ignisNode;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ignisNode = new IgnisArchivalNodeConnection(BASE_URL, mockHttpClient);
	}
	
	@Test
	public void isReady() throws BlockchainSubsystemException, InterruptedException {
		String getBlockchainStatusUrl = BASE_URL + "/nxt?requestType=getBlockchainStatus";
		String getBlockchainStatusResponse = getBlockchainStatusResponse(true, Constants.INITIAL_SYNC_BLOCK_ID, Constants.INITIAL_SYNC_HEIGHT);
		
		setGETResponse(getBlockchainStatusUrl, getBlockchainStatusResponse);
		
		boolean isReady = ignisNode.isReady();
		
		assertTrue(isReady);
	}
	
	@Test
	public void isNotReady() throws BlockchainSubsystemException, InterruptedException {
		String getBlockchainStatusUrl = BASE_URL + "/nxt?requestType=getBlockchainStatus";
		String getBlockchainStatusResponse = getBlockchainStatusResponse(false, Constants.INITIAL_SYNC_BLOCK_ID, Constants.INITIAL_SYNC_HEIGHT);
		
		setGETResponse(getBlockchainStatusUrl, getBlockchainStatusResponse);
		
		boolean isReady = ignisNode.isReady();
		
		assertFalse(isReady);
	}
	
	@Test
	public void getRecentBlocks() throws BlockchainSubsystemException, InterruptedException {
		final long SENDER = 123;
		final long RECIPIENT = 456;
		final String MESSAGE = "hello world";
		final String ALIAS = "player1";
		
		List<Block> expectedBlocks = Arrays.asList(
			new Block(1, Constants.INITIAL_SYNC_HEIGHT + 1, 1, new ArrayList<Transaction>()),
			new Block(2, Constants.INITIAL_SYNC_HEIGHT + 2, 2, Arrays.asList(
				new Transaction(SENDER, RECIPIENT, Constants.INITIAL_SYNC_HEIGHT + 2, 0, 2, MESSAGE, ""),
				new Transaction(SENDER, 0, Constants.INITIAL_SYNC_HEIGHT + 2, 1, 2, "", ALIAS),
				new Transaction(SENDER, RECIPIENT, Constants.INITIAL_SYNC_HEIGHT + 2, 2, 2, "", "")
			))
		);
		
		String getBlocksUrl = BASE_URL + "/nxt?requestType=getBlocks&includeTransactions=true&lastIndex=1";
		String getBlocksResponse = "{"
				+	"\"blocks\": " + blocksToJson(expectedBlocks) + ", "
				+	"\"requestProcessingTime\": 2"
				+ "}";
		
		setGETResponse(getBlocksUrl, getBlocksResponse);
		
		List<Block> actualBlocks = ignisNode.getRecentBlocks(2);
		
		assertEquals(expectedBlocks.size(), actualBlocks.size());
		for (int i = 0; i < expectedBlocks.size(); i++) {
			Block expected = expectedBlocks.get(i);
			Block actual = actualBlocks.get(i);
			
			assertEquals(expected.id, actual.id);
			assertEquals(expected.height, actual.height);
			assertEquals(expected.timestamp, actual.timestamp);
			assertEquals(expected.transactions.size(), actual.transactions.size());
			
			for (int j = 0; j < expected.transactions.size(); j++) {
				Transaction expectedTx = expected.transactions.get(j);
				Transaction actualTx = actual.transactions.get(j);
				
				assertEquals(expectedTx.sender, actualTx.sender);
				assertEquals(expectedTx.recipient, actualTx.recipient);
				assertEquals(expectedTx.height, actualTx.height);
				assertEquals(expectedTx.index, actualTx.index);
				assertEquals(expectedTx.blockId, actualTx.blockId);
				assertEquals(expectedTx.message, actualTx.message);
				assertEquals(expectedTx.alias, actualTx.alias);
			}
		}
	}

	@Test
	public void getPlayerAccount() throws BlockchainSubsystemException, InterruptedException {
		AccountMetadata account = new AccountMetadata(1, "player1", "pubkey1");
		String getAliasesUrl = BASE_URL + "/nxt?requestType=getAliases&chain=2&account=" + account.id;
		String getAliasesResponse = "{"
				+	"\"aliases\": " + aliasesToJson(Arrays.asList(
						new Alias("irrelevantAlias", account.id),				// lacks "snailscuffle" prefix
						new Alias("snailscuffleplayer1", account.id),
						new Alias("snailscuffleplayer2", account.id))) + ","
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		String getAccountPublicKeyUrl = BASE_URL + "/nxt?requestType=getAccountPublicKey&account=" + account.id;
		String getAccountPublicKeyResponse = "{"
				+	"\"publicKey\": \"" + account.publicKey + "\", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		setGETResponse(getAliasesUrl, getAliasesResponse);
		setGETResponse(getAccountPublicKeyUrl, getAccountPublicKeyResponse);
		
		AccountMetadata retrieved = ignisNode.getPlayerAccount(Long.toUnsignedString(account.id));
		
		assertEquals(account.id, retrieved.id);
		assertEquals(account.username, retrieved.username);
		assertEquals(account.publicKey, retrieved.publicKey);
	}
	
	@Test
	public void getAllPlayerAccounts() throws BlockchainSubsystemException, InterruptedException {
		AccountMetadata account1 = new AccountMetadata(1, "snailscuffle1", "pubkey1");
		AccountMetadata account2 = new AccountMetadata(2, "fun username", "pubkey2");
		
		String getAliasesLikeUrl = BASE_URL + "/nxt?requestType=getAliasesLike&chain=2&aliasPrefix=snailscuffle";
		String getAliasesLikeResponse = "{"
				+	"\"aliases\": " + aliasesToJson(Arrays.asList(
						new Alias("snailscuffle" + account1.username, account1.id),
						new Alias("snailscuffle" + account2.username, account2.id))) + ","
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		String getAccount1PublicKeyUrl = BASE_URL + "/nxt?requestType=getAccountPublicKey&account=" + account1.id;
		String getAccount1PublicKeyResponse = "{"
				+	"\"publicKey\": \"" + account1.publicKey + "\", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		String getAccount2PublicKeyUrl = BASE_URL + "/nxt?requestType=getAccountPublicKey&account=" + account2.id;
		String getAccount2PublicKeyResponse = "{"
				+	"\"publicKey\": \"" + account2.publicKey + "\", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		setGETResponse(getAliasesLikeUrl, getAliasesLikeResponse);
		setGETResponse(getAccount1PublicKeyUrl, getAccount1PublicKeyResponse);
		setGETResponse(getAccount2PublicKeyUrl, getAccount2PublicKeyResponse);
		
		List<AccountMetadata> accounts = ignisNode.getAllPlayerAccounts();
		
		AccountMetadata retrieved1 = accounts.stream().filter(a -> a.id == account1.id).findFirst().get();
		AccountMetadata retrieved2 = accounts.stream().filter(a -> a.id == account2.id).findFirst().get();
		
		assertEquals(account1.id, retrieved1.id);
		assertEquals(account1.username, retrieved1.username);
		assertEquals(account1.publicKey, retrieved1.publicKey);
		
		assertEquals(account2.id, retrieved2.id);
		assertEquals(account2.username, retrieved2.username);
		assertEquals(account2.publicKey, retrieved2.publicKey);
	}
	
	@Test
	public void getAccountBalance() throws BlockchainSubsystemException, InterruptedException {
		final BigDecimal NQT_PER_IGNIS = new BigDecimal(100_000_000);
		
		int accountId = 123;
		double expectedBalance = 1234.5678;
		String expectedBalanceNQT = new BigDecimal(expectedBalance).multiply(NQT_PER_IGNIS).toString();
		String requestUrl = BASE_URL + "/nxt?requestType=getBalance&chain=2&account=" + accountId;
		String responseJson = "{"
				+	"\"unconfirmedBalanceNQT\": \"" + expectedBalanceNQT + "\", "
				+	"\"balanceNQT\": \"" + expectedBalanceNQT + "\", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		setGETResponse(requestUrl, responseJson);
		
		double returnedBalance = ignisNode.getBalance(String.valueOf(accountId));
		
		assertEquals(expectedBalance, returnedBalance, 0);
	}
	
	@Test
	public void getMessages() throws BlockchainSubsystemException, InterruptedException {
		final long SENDER = 123;
		final long RECIPIENT = 456;
		final int INITIAL_HEIGHT = Constants.INITIAL_SYNC_HEIGHT + 1;
		final int FINAL_HEIGHT = Constants.INITIAL_SYNC_HEIGHT + 2;
		
		List<Block> blocks = Arrays.asList(
			new Block(1, INITIAL_HEIGHT, 1, Arrays.asList(
				new Transaction(SENDER, RECIPIENT, INITIAL_HEIGHT, 1, 1, "first match", ""),
				new Transaction(RECIPIENT, SENDER, INITIAL_HEIGHT, 2, 1, "wrong sender", "")
			)),
			new Block(2, FINAL_HEIGHT, 2, Arrays.asList(
				new Transaction(SENDER, RECIPIENT, FINAL_HEIGHT, 3, 2, "second match", "")
			)),
			new Block(3, FINAL_HEIGHT + 1, 3, Arrays.asList(
				new Transaction(SENDER, RECIPIENT, FINAL_HEIGHT + 1, 4, 3, "too late", "")
			))
		);
		List<Transaction> transactions = blocks.stream()
				.flatMap(b -> b.transactions.stream())
				.collect(Collectors.toList());
		
		String getBlockUrl = BASE_URL + "/nxt?requestType=getBlock&includeTransactions=true&height=" + INITIAL_HEIGHT;
		String getBlockResponse = blockToJson(blocks.get(0), 0);
		
		String getBlockchainTransactionsUrl = BASE_URL + "/nxt?requestType=getBlockchainTransactions"
				+ "&chain=2"
				+ "&account=" + SENDER
				+ "&timestamp=" + blocks.get(0).timestamp
				+ "&type=1"
				+ "&subtype=0"
				+ "&includeExpiredPrunable=true";
		String getBlockchainTransactionsResponse = "{"
				+	"\"requestProcessingTime\": 0, "
				+	"\"transactions\": " + transactionsToJson(transactions)
				+ "}";
		
		setGETResponse(getBlockUrl, getBlockResponse);
		setGETResponse(getBlockchainTransactionsUrl, getBlockchainTransactionsResponse);
		
		List<Transaction> retrieved = ignisNode.getMessagesFrom(Long.toUnsignedString(SENDER), INITIAL_HEIGHT, FINAL_HEIGHT);
		Transaction tx0 = retrieved.get(0);
		Transaction tx1 = retrieved.get(1);
		
		assertEquals(2, retrieved.size());
		
		assertEquals(SENDER, tx0.sender);
		assertEquals(RECIPIENT, tx0.recipient);
		assertEquals(INITIAL_HEIGHT, tx0.height);
		assertEquals(1, tx0.index);
		assertEquals(1, tx0.blockId);
		assertEquals("first match", tx0.message);
		assertEquals("", tx0.alias);
		
		assertEquals(SENDER, tx1.sender);
		assertEquals(RECIPIENT, tx1.recipient);
		assertEquals(INITIAL_HEIGHT + 1, tx1.height);
		assertEquals(3, tx1.index);
		assertEquals(2, tx1.blockId);
		assertEquals("second match", tx1.message);
		assertEquals("", tx1.alias);
	}
	
	private void setGETResponse(String requestUrl, String response) {
		ContentResponse mockContentResponse = mock(ContentResponse.class);
		try {
			when(mockHttpClient.GET(requestUrl)).thenReturn(mockContentResponse);
			when(mockContentResponse.getContentAsString()).thenReturn(response);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			assert(false);	// this is impossible (it's just a mock)
		}
	}
	
}
