package com.snailscuffle.game.blockchain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.game.blockchain.data.AccountMetadata;

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
	public void getPlayerAccount() throws BlockchainSubsystemException, InterruptedException {
		AccountMetadata account = new AccountMetadata(1, "player1", "pubkey1");
		String getAliasesUrl = BASE_URL + "/nxt?requestType=getAliases&chain=2&account=" + account.id;
		String getAliasesResponse = "{"
				+	"\"aliases\": ["
				+		"{"
				+			"\"aliasURI\": \"\", "
				+			"\"aliasName\": \"irrelevantAlias\", "		// lacks "snailscuffle" prefix
				+			"\"accountRS\": \"ARDOR-0000-0000-0000-00000\", "
				+			"\"alias\": \"12345\", "
				+			"\"account\": \"" + account.id + "\", "
				+			"\"timestamp\": 0"
				+		"},"
				+		"{"
				+			"\"aliasURI\": \"\", "
				+			"\"aliasName\": \"snailscuffleplayer1\", "
				+			"\"accountRS\": \"ARDOR-0000-0000-0000-00000\", "
				+			"\"alias\": \"23456\", "
				+			"\"account\": \"" + account.id + "\", "
				+			"\"timestamp\": 0"
				+		"},"
				+		"{"
				+			"\"aliasURI\": \"\", "
				+			"\"aliasName\": \"snailscuffleplayer2\", "
				+			"\"accountRS\": \"ARDOR-0000-0000-0000-00000\", "
				+			"\"alias\": \"34567\", "
				+			"\"account\": \"" + account.id + "\", "
				+			"\"timestamp\": 0"
				+		"}"
				+	"],"
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		String getAccountPublicKeyUrl = BASE_URL + "/nxt?requestType=getAccountPublicKey&account=" + account.id;
		String getAccountPublicKeyResponse = "{"
				+	"\"publicKey\": \"" + account.publicKey + "\", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		setGETResponse(getAliasesUrl, getAliasesResponse);
		setGETResponse(getAccountPublicKeyUrl, getAccountPublicKeyResponse);
		
		AccountMetadata retrieved = ignisNode.getPlayerAccount(account.id);
		
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
				+	"\"aliases\": ["
				+		"{"
				+			"\"aliasURI\": \"\", "
				+			"\"aliasName\": \"snailscuffle" + account1.username + "\", "
				+			"\"accountRS\": \"ARDOR-0000-0000-0000-00000\", "
				+			"\"alias\": \"12345\", "
				+			"\"account\": \"" + account1.id + "\", "
				+			"\"timestamp\": 0"
				+		"},"
				+		"{"
				+			"\"aliasURI\": \"\", "
				+			"\"aliasName\": \"snailscuffle" + account2.username + "\", "
				+			"\"accountRS\": \"ARDOR-1111-1111-1111-11111\", "
				+			"\"alias\": \"23456\", "
				+			"\"account\": \"" + account2.id + "\", "
				+			"\"timestamp\": 0"
				+		"}"
				+	"],"
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
		
		double returnedBalance = ignisNode.getBalance(accountId);
		
		assertEquals(expectedBalance, returnedBalance, 0);
	}
	
	private void setGETResponse(String requestUrl, String response) {
		ContentResponse mockContentResponse = mock(ContentResponse.class);
		try {
			when(mockHttpClient.GET(requestUrl)).thenReturn(mockContentResponse);
			when(mockContentResponse.getContentAsString()).thenReturn(response);
		} catch (Exception e) {
			assert(false);	// this is impossible (it's just a mock)
		}
	}
	
}
