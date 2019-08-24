package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.game.blockchain.data.AccountMetadata;
import com.snailscuffle.game.blockchain.data.Alias;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.Transaction;
import com.snailscuffle.game.tx.TransactionStatus;
import com.snailscuffle.game.tx.UnsignedTransaction;

public class IgnisArchivalNodeConnection implements Closeable {
	
	private static final String PARAM_IGNIS_CHAIN = "2";
	private static final String PARAM_CALCULATE_FEE = "-1";
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
	
	public boolean isReady() throws BlockchainSubsystemException, InterruptedException {
		String url = baseUrl + "/nxt?requestType=getBlockchainStatus";
		try {
			String response = sendGETRequest(url, "Failed to get blockchain status");
			JsonNode parsedResponse = BlockchainUtil.parseJson(response, "Failed to deserialize response to getBlockchainStatus");
			
			JsonNode includeExpiredPrunable = BlockchainUtil.getResponsePropertyOrThrow(parsedResponse, "includeExpiredPrunable", "getBlockchainStatus");
			if (!includeExpiredPrunable.asBoolean()) {
				throw new BlockchainSubsystemException("Ignis node is not an archival node (includeExpiredPrunable = false)");
			}
			
			JsonNode blockchainState = BlockchainUtil.getResponsePropertyOrThrow(parsedResponse, "blockchainState", "getBlockchainStatus");
			return blockchainState.textValue().equals("UP_TO_DATE");
		} catch (IgnisNodeCommunicationException e) {
			return false;
		}
	}
	
	public Block getCurrentBlock() throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		List<Block> blocks = getRecentBlocks(1);
		if (blocks.size() > 0) {
			return blocks.get(0);
		} else {
			throw new BlockchainDataNotFoundException("Response from blockchain node contained no blocks");
		}
	}
	
	public List<Block> getRecentBlocks(int count) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String url = baseUrl + "/nxt?requestType=getBlocks&includeTransactions=true&lastIndex=" + (count - 1);
		String response = sendGETRequest(url, "Failed to get current block");
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from getBlocks");
		JsonNode blockArray = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "blocks", "getBlocks");
		return Block.parseAll(blockArray, "getBlocks");
	}
	
	public Block getBlockAtHeight(int height) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String url = baseUrl + "/nxt?requestType=getBlock&includeTransactions=true&height=" + height;
		String response = sendGETRequest(url, "Failed to get block at height " + height);
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from getBlock for height=" + height);
		return new Block(responseJson, "getBlock");
	}
	
	public AccountMetadata getPlayerAccount(long accountId) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String accountIdString = Long.toUnsignedString(accountId);
		String url = baseUrl + "/nxt?requestType=getAliases&chain=2&account=" + accountIdString;
		String response = sendGETRequest(url, "Failed to get aliases for account " + accountIdString);
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from getAliases");
		JsonNode aliasArray = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "aliases", "getAliases");
		List<Alias> aliases = Alias.parseAll(aliasArray, "getAliases");
		
		for (Alias alias : aliases) {
			if (alias.name.startsWith("snailscuffle")) {
				String publicKey = getPublicKey(alias.account);
				String username = alias.name.replaceFirst("snailscuffle", "");
				return new AccountMetadata(alias.account, username, publicKey);		// use the first valid username (getAliases returns them in alphabetical order)
			}
		}
		
		throw new BlockchainDataNotFoundException("Account " + accountIdString + " does not have an alias with the prefix 'snailscuffle'");
	}
	
	public List<AccountMetadata> getAllPlayerAccounts() throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String url = baseUrl + "/nxt?requestType=getAliasesLike&chain=2&aliasPrefix=snailscuffle";
		String response = sendGETRequest(url, "Failed to get aliases with the prefix 'snailscuffle'");
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from getAliasesLike");
		JsonNode aliasArray = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "aliases", "getAliasesLike");
		List<Alias> aliases = Alias.parseAll(aliasArray, "getAliasesLike");
		
		Set<Long> accountIds = new HashSet<>();
		List<AccountMetadata> accounts = new ArrayList<>();
		for (Alias alias : aliases) {
			// If an account has multiple snailscuffle aliases, use only the first. Note that
			// getAliasesLike returns them in alphabetical order.
			if (alias.name.startsWith("snailscuffle") && accountIds.add(alias.account)) {
				String publicKey = getPublicKey(alias.account);
				String username = alias.name.replaceFirst("snailscuffle", "");
				accounts.add(new AccountMetadata(alias.account, username, publicKey));
			}
		}
		return accounts;
	}
	
	private String getPublicKey(long accountId) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String accountIdString = Long.toUnsignedString(accountId);
		String url = baseUrl + "/nxt?requestType=getAccountPublicKey&account=" + accountIdString;
		String response = sendGETRequest(url, "Failed to get public key for account " + accountIdString);
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response to getAccountPublicKey inquiry for account " + accountIdString);
		JsonNode publicKey = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "publicKey", "getAccountPublicKey");
		return publicKey.asText();
	}
	
	public double getBalance(long accountId) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String accountIdString = Long.toUnsignedString(accountId);
		String url = baseUrl + "/nxt?requestType=getBalance&chain=2&account=" + accountIdString;
		String response = sendGETRequest(url, "Failed to get balance of account " + accountIdString);
		JsonNode parsedResponse = BlockchainUtil.parseJson(response, "Failed to deserialize response to getBalance inquiry for account " + accountIdString);
		JsonNode balance = BlockchainUtil.getResponsePropertyOrThrow(parsedResponse, "unconfirmedBalanceNQT", "getBalance");
		return nqtToDouble(balance.asLong());
	}
	
	private static double nqtToDouble(long nqt) {
		return new BigDecimal(nqt).divide(new BigDecimal(NQT_PER_IGNIS)).doubleValue();
	}
	
	public List<Transaction> getMessagesFrom(long accountId, int initialHeight, int finalHeight) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String accountIdString = Long.toUnsignedString(accountId);
		int startingTimestamp = getBlockAtHeight(initialHeight).timestamp;
		String url = baseUrl + "/nxt?requestType=getBlockchainTransactions"
				+ "&chain=2"
				+ "&account=" + accountIdString
				+ "&timestamp=" + startingTimestamp
				+ "&type=1"
				+ "&subtype=0"
				+ "&includeExpiredPrunable=true";
		String response = sendGETRequest(url, "Failed to get transactions to and from account " + accountIdString);
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response to getBlockchainTransactions inquiry for account " + accountIdString);
		JsonNode txArray = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "transactions", "getBlockchainTransactions");
		return Transaction.parseAll(txArray, "getBlockchainTransactions").stream()
				.filter(t -> t.sender == accountId && t.height <= finalHeight)
				.collect(Collectors.toList());
	}
	
	public UnsignedTransaction createNewAccountTransaction(String username, String publicKey) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("requestType", "setAlias");
		parameters.put("chain", PARAM_IGNIS_CHAIN);
		parameters.put("feeNQT", PARAM_CALCULATE_FEE);
		parameters.put("aliasName", username);
		parameters.put("publicKey", publicKey);
		
		String response = sendPOSTRequest(parameters, "Failed to create alias 'snailscuffle" + username + "'");
		
		JsonNode parsedResponse = BlockchainUtil.parseJson(response, "Failed to deserialize response to setAlias");
		JsonNode txJson = BlockchainUtil.getResponsePropertyOrThrow(parsedResponse, "transactionJSON", "setAlias");
		JsonNode txBytes = BlockchainUtil.getResponsePropertyOrThrow(parsedResponse, "unsignedTransactionBytes", "setAlias");
		
		return new UnsignedTransaction(txJson, txBytes.asText());
	}
	
	public TransactionStatus broadcastTransaction(String txJson) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("requestType", "broadcastTransaction");
		parameters.put("transactionJSON", txJson);
		
		String response = sendPOSTRequest(parameters, "Failed to broadcast transaction");
		
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from broadcastTransaction");
		JsonNode fullHash = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "fullHash", "broadcastTransaction");
		return getTransactionStatus(fullHash.asText());
	}
	
	public TransactionStatus getTransactionStatus(String fullHash) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		String url = baseUrl + "/nxt?requestType=getTransaction&chain=2&fullHash=" + fullHash;
		String response = sendGETRequest(url, "Failed to get status of transaction '" + fullHash + "'");
		JsonNode responseJson = BlockchainUtil.parseJson(response, "Failed to deserialize response from getTransaction for transaction '" + fullHash + "'");
		JsonNode block = responseJson.get("block");
		JsonNode confirmationsNode = BlockchainUtil.getResponsePropertyOrThrow(responseJson, "confirmations", "getTransaction");
		
		boolean confirmed = block != null;
		int confirmations = (confirmed && confirmationsNode != null) ? confirmationsNode.asInt() : 0;
		
		return new TransactionStatus(fullHash, confirmed, confirmations);
	}
	
	private String sendGETRequest(String url, String errorString) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		try {
			return httpClient.GET(url).getContentAsString();
		} catch (TimeoutException e) {
			throw new IgnisNodeCommunicationException(errorString + ": " + e.getMessage());
		} catch (ExecutionException e) {
			throw new BlockchainSubsystemException(errorString + ": " + e.getMessage());
		}
	}
	
	private String sendPOSTRequest(Map<String, String> parameters, String errorString) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		try {
			Request request = httpClient.POST(baseUrl + "/nxt");
			for (Entry<String, String> kvp : parameters.entrySet()) {
				request.param(kvp.getKey(), kvp.getValue());
			}
			return request.send().getContentAsString();
		} catch (TimeoutException e) {
			throw new IgnisNodeCommunicationException(errorString + ": " + e.getMessage());
		} catch (ExecutionException e) {
			throw new BlockchainSubsystemException(errorString + ": " + e.getMessage());
		}
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
