package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.accounts.AccountNotFoundException;

public class IgnisArchivalNodeConnection implements Closeable {
	
	private static final long NQT_PER_IGNIS = 100_000_000;
	
	private final String baseUrl;
	private final HttpClient connection;
	
	public IgnisArchivalNodeConnection(URL ignisArchivalNodeUrl) throws BlockchainSubsystemException {
		baseUrl = ignisArchivalNodeUrl.toString();
		connection = new HttpClient(new SslContextFactory());
		try {
			connection.start();
		} catch (Exception e){
			throw new BlockchainSubsystemException("Failed to start http(s) client: " + e.getMessage());
		}
	}
	
	public double getBalanceOf(long accountId) throws AccountNotFoundException, BlockchainSubsystemException {
		String url = baseUrl + "/nxt?requestType=getBalance&chain=2&account=" + accountId;
		try {
			String response = connection.GET(url).getContentAsString();
			JsonNode balanceNode = JsonUtil.deserialize(response).get("unconfirmedBalanceNQT");
			if (balanceNode == null) {
				throw new AccountNotFoundException("Failed to get balance of account " + accountId + "; query returned: " + response);
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
		//connection.close();
	}

}
