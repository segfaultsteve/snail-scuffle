package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.accounts.AccountException;

public class IgnisArchivalNodeConnection implements Closeable {
	
	private static final long NQT_PER_IGNIS = 100_000_000;
	private static final Logger logger = LoggerFactory.getLogger(IgnisArchivalNodeConnection.class);
	
	private final String baseUrl;
	private final HttpClient httpClient;
	
	public IgnisArchivalNodeConnection(URL ignisArchivalNodeUrl) throws BlockchainSubsystemException {
		baseUrl = ignisArchivalNodeUrl.toString();
		httpClient = new HttpClient(new SslContextFactory());
		try {
			httpClient.start();
		} catch (Exception e){
			throw new BlockchainSubsystemException("Failed to start http(s) client: " + e.getMessage());
		}
	}
	
	// This constructor is for unit tests.
	IgnisArchivalNodeConnection(String baseUrl, HttpClient httpClient) {
		this.baseUrl = baseUrl;
		this.httpClient = httpClient;
	}
	
	public double getBalanceOf(long accountId) throws AccountException, BlockchainSubsystemException {
		String url = baseUrl + "/nxt?requestType=getBalance&chain=2&account=" + accountId;
		try {
			String response = httpClient.GET(url).getContentAsString();
			JsonNode balanceNode = JsonUtil.deserialize(response).get("unconfirmedBalanceNQT");
			if (balanceNode == null) {
				throw new AccountException("Failed to get balance of account " + accountId + "; query returned: " + response);
			} else {
				return nqtToDouble(balanceNode.asLong());
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new BlockchainSubsystemException("Failed to get balance of account " + accountId + ": " + e.getMessage());
		} catch (IOException e) {
			throw new BlockchainSubsystemException("Failed to deserialize response to getBalance inquiry for account " + accountId + ": " + e.getMessage());
		}
	}
	
	private static double nqtToDouble(long nqt) {
		return new BigDecimal(nqt).divide(new BigDecimal(NQT_PER_IGNIS)).doubleValue();
	}
	
	@Override
	public void close() {
		try {
			httpClient.stop();
		} catch (Exception e) {
			logger.error("Failed to close connection to Ignis archival node");
		}
	}

}
