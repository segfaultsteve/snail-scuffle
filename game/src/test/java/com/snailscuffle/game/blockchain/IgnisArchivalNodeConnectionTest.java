package com.snailscuffle.game.blockchain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IgnisArchivalNodeConnectionTest {
	
	private static final String BASE_URL = "https://game.snailscuffle.com";
	private static final BigDecimal NQT_PER_IGNIS = new BigDecimal(100_000_000);
	
	@Mock private HttpClient mockHttpClient;
	private IgnisArchivalNodeConnection ignisNode;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ignisNode = new IgnisArchivalNodeConnection(BASE_URL, mockHttpClient);
	}
	
	@Test
	public void getAccountBalance() throws BlockchainSubsystemException, InterruptedException {
		int accountId = 123;
		double expectedBalance = 1234.5678;
		String expectedBalanceNQT = new BigDecimal(expectedBalance).multiply(NQT_PER_IGNIS).toString();
		String requestUrl = BASE_URL + "/nxt?requestType=getBalance&chain=2&account=" + accountId;
		String responseJson = "{"
				+ 	"\"unconfirmedBalanceNQT\" : \"" + expectedBalanceNQT + "\","
				+ 	"\"balanceNQT\" : \"" + expectedBalanceNQT + "\","
				+ 	"\"requestProcessingTime\" : 1"
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
