package com.snailscuffle.game.blockchain;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.mockito.invocation.InvocationOnMock;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.Constants;
import com.snailscuffle.game.blockchain.data.Alias;
import com.snailscuffle.game.testutil.BlockchainJson;
import com.snailscuffle.game.testutil.TransactionJson;

class BlockchainStub {
	
	private static class Block {
		private final long id;
		private final int height;
		private final List<String> txsJson;
		
		private Block(long id, int height, List<String> txsJson) {
			this.id = id;
			this.height = height;
			this.txsJson = txsJson;
		}
	}
	
	final static long IGNIS_BALANCE_NQT = 10_000_000_000l;
	
	final HttpClient mockHttpClient;
	
	private final Map<String, Long> publicKeyToAccountId = new HashMap<>();
	private final List<Block> blockchain = new ArrayList<>();
	private long currentBlockId = Constants.INITIAL_SYNC_BLOCK_ID;
	private int currentHeight = Constants.INITIAL_SYNC_HEIGHT;
	
	BlockchainStub(String baseUrl) {
		mockHttpClient = mock(HttpClient.class);
		
		try {
			when(mockHttpClient.GET(contains("getBlockchainStatus"))).then(this::returnBlockchainStatus);
			when(mockHttpClient.GET(contains("getBalance"))).then(this::returnBalance);
			when(mockHttpClient.GET(contains("getBlocks"))).then(this::returnBlocks);
			when(mockHttpClient.GET(contains("getBlock&"))).then(this::returnBlock);
			when(mockHttpClient.GET(contains("getAliasesLike"))).then(this::returnAliasesLike);
			when(mockHttpClient.GET(contains("getAliases&"))).then(this::returnAliases);
			when(mockHttpClient.GET(contains("getAccountPublicKey"))).then(this::returnPublicKey);
			when(mockHttpClient.GET(contains("getBlockchainTransactions"))).then(this::returnTransactions);
			when(mockHttpClient.GET(contains("getTransaction"))).then(this::returnMostRecentTransaction);
			when(mockHttpClient.POST(baseUrl + "/nxt")).then(this::respondToPOST);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		blockchain.add(new Block(Constants.INITIAL_SYNC_BLOCK_ID, Constants.INITIAL_SYNC_HEIGHT, new ArrayList<>()));
	}
	
	void addPublicKey(long accountId, String publicKey) {
		publicKeyToAccountId.put(publicKey, accountId);
	}
	
	void addBlock(List<String> txsJson) {
		synchronized (blockchain) {
			++currentHeight;
			++currentBlockId;
			
			String escapedHeight = Matcher.quoteReplacement("\"height\": " + currentHeight);
			String escapedTimestamp = Matcher.quoteReplacement("\"timestamp\": " + currentHeight);
			String escapedBlockTimestamp = Matcher.quoteReplacement("\"blockTimestamp\": " + currentHeight);
			String escapedBlockId = Matcher.quoteReplacement("\"block\": \"" + Long.toUnsignedString(currentBlockId) + "\"");
			
			List<String> correctedTxsJson = txsJson.stream()
					.map(tx -> tx.replaceAll("\"height\":\\s*\\d*\\s*", escapedHeight))
					.map(tx -> tx.replaceAll("\"timestamp\":\\s*\\d*\\s*", escapedTimestamp))
					.map(tx -> tx.replaceAll("\"blockTimestamp\":\\s*\\d*\\s*", escapedBlockTimestamp))
					.map(tx -> tx.replaceAll("\"block\":\\s*\"\\d*\"\\s*", escapedBlockId))
					.collect(Collectors.toList());
			
			blockchain.add(new Block(currentBlockId, currentHeight, correctedTxsJson));
		}
	}
	
	void rollBackAllBlocks() {
		synchronized (blockchain) {
			blockchain.removeIf(b -> b.height > Constants.INITIAL_SYNC_HEIGHT);
			currentHeight = Constants.INITIAL_SYNC_HEIGHT;
			// do not reset block ID--the purpose of rolling back is to create a fork
		}
	}
	
	private ContentResponse returnBlockchainStatus(InvocationOnMock args) {
		synchronized (blockchain) {
			Block lastBlock = blockchain.get(blockchain.size() - 1);
			return responseStub(BlockchainJson.getBlockchainStatusResponse(true, lastBlock.id, lastBlock.height));
		}
	}
	
	private ContentResponse returnBalance(InvocationOnMock args) {
		return responseStub(BlockchainJson.getBalanceResponse(IGNIS_BALANCE_NQT));
	}
	
	private ContentResponse returnBlocks(InvocationOnMock args) {
		synchronized (blockchain) {
			String url = args.getArgument(0);
			String lastIndex = extractQueryParameter("lastIndex", url);
			int count = Integer.parseInt(lastIndex) + 1;
			
			List<Block> blocks = new ArrayList<>(blockchain);
			Collections.reverse(blocks);
			blocks.subList(0, Math.min(count, blocks.size()));
			
			String response = "{"
					+	"\"blocks\": " + toJson(blocks) + ", "
					+	"\"requestProcessingTime\": 1"
					+ "}";
			
			return responseStub(response);
		}
	}
	
	private ContentResponse returnBlock(InvocationOnMock args) {
		synchronized (blockchain) {
			String url = args.getArgument(0);
			String heightString = extractQueryParameter("height", url);
			int height = Math.max(Integer.parseInt(heightString), Constants.INITIAL_SYNC_HEIGHT);
			
			Block match = blockchain.stream()
					.filter(b -> b.height == height)
					.findFirst().get();
			
			return responseStub(toJson(match));
		}
	}
	
	private static String toJson(List<Block> blocks) {
		List<String> blocksJson = blocks.stream().map(b -> toJson(b)).collect(toList());
		return "[" + String.join(",", blocksJson) + "]";
	}
	
	private static String toJson(Block block) {
		return BlockchainJson.transactionJsonToBlockJson(block.id, block.id - 1, block.height, block.txsJson);
	}
	
	private ContentResponse returnAliasesLike(InvocationOnMock args) {
		String url = args.getArgument(0);
		String prefix = extractQueryParameter("aliasPrefix", url);
		
		List<Alias> aliases = getAliases(a -> a.name.startsWith(prefix));
		
		String response = "{"
				+	"\"aliases\": " + BlockchainJson.aliasesToJson(aliases) + ", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		return responseStub(response);
	}
	
	private ContentResponse returnAliases(InvocationOnMock args) {
		String url = args.getArgument(0);
		String accountId = extractQueryParameter("account", url);
		
		List<Alias> aliases = getAliases(a -> a.account == Long.parseUnsignedLong(accountId));
		
		String response = "{"
				+	"\"aliases\": " + BlockchainJson.aliasesToJson(aliases) + ", "
				+	"\"requestProcessingTime\": 1"
				+ "}";
		
		return responseStub(response);
	}
	
	private ContentResponse returnPublicKey(InvocationOnMock args) {
		synchronized (blockchain) {
			String url = args.getArgument(0);
			String accountId = extractQueryParameter("account", url);
			
			String publicKey = blockchain.stream()
					.flatMap(b -> b.txsJson.stream())
					.map(tx -> deserializeNothrow(tx))
					.filter(tx -> tx.get("sender").asText().equals(accountId))
					.findFirst().get()
					.get("senderPublicKey").asText();
			
			String response = "{"
					+	"\"publicKey\": \"" + publicKey + "\", "
					+	"\"requestProcessingTime\": 1"
					+ "}";
			
			return responseStub(response);
		}
	}
	
	private ContentResponse returnTransactions(InvocationOnMock args) {
		synchronized (blockchain) {
			String url = args.getArgument(0);
			long account = Long.parseUnsignedLong(extractQueryParameter("account", url));
			String senderOrRecipientRegex = ".*\"(?:sender|recipient)\":\\w*\"" + Long.toUnsignedString(account) + "\".*";
			
			List<String> matches = blockchain.stream()
					.flatMap(b -> b.txsJson.stream())
					.filter(tx -> tx.matches(senderOrRecipientRegex))
					.collect(toList());
			
			String response = "{"
					+	"\"transactions\": [" + String.join(",", matches) + "], "
					+	"\"requestProcessingTime\": 1"
					+ "}";
			
			return responseStub(response);
		}
	}
	
	private ContentResponse returnMostRecentTransaction(InvocationOnMock args) {
		synchronized (blockchain) {
			List<String> lastBlockTxs = blockchain.get(blockchain.size() - 1).txsJson;
			return responseStub(lastBlockTxs.get(lastBlockTxs.size() - 1));
		}
	}
	
	private static String extractQueryParameter(String param, String url) {
		int index = url.indexOf('?');
		String queryString = (index > 0) ? url.substring(index + 1) : "";
		return Stream.of(queryString.split("&"))
			.filter(p -> p.split("=")[0].equalsIgnoreCase(param))
			.map(p -> p.split("=")[1])
			.findFirst().orElse("");
	}
	
	private List<Alias> getAliases(Predicate<Alias> selectionFilter) {
		synchronized (blockchain) {
			return blockchain.stream()
					.flatMap(b -> b.txsJson.stream())
					.map(tx -> deserializeNothrow(tx))
					.filter(tx -> tx.has("attachment") && tx.get("attachment").has("alias"))
					.map(tx -> txToAlias(tx))
					.filter(a -> selectionFilter.test(a))
					.collect(toList());
		}
	}
	
	private static JsonNode deserializeNothrow(String json) {
		try {
			return JsonUtil.deserialize(json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Alias txToAlias(JsonNode tx) {
		String alias = tx.get("attachment").get("alias").asText();
		long sender = Long.parseUnsignedLong(tx.get("sender").asText());
		return new Alias(alias, sender);
	}
	
	private Request respondToPOST(InvocationOnMock args) {
		try {
			Map<String, String> params = new HashMap<>();

			Request request = mock(Request.class);
			when(request.param(anyString(), anyString())).then(i -> params.put(i.getArgument(0), i.getArgument(1)));
			when(request.send()).then(i -> returnPOSTResponseFor(params));

			return request;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private ContentResponse returnPOSTResponseFor(Map<String, String> params) {
		String publicKey = params.get("publicKey");
		Long sender = publicKeyToAccountId.get(publicKey);
		String response = "error";
		
		switch (params.get("requestType")) {
		case "setAlias":
			String newAccountTx = TransactionJson.newAccountTransaction(currentHeight, currentBlockId, 0, sender, publicKey, params.get("aliasName"));
			response = txResponse(newAccountTx);
			break;
		case "sendMessage":
			long recipient = Long.parseUnsignedLong(params.get("recipient"));
			String messageTx = TransactionJson.messageTransaction(currentHeight, currentBlockId, 0, sender, publicKey, recipient, params.get("message"));
			response = txResponse(messageTx);
			break;
		case "broadcastTransaction":
			String txJson = params.get("transactionJSON");
			addBlock(Arrays.asList(txJson));
			response = txJson;
			break;
		}
		
		return responseStub(response);
	}
	
	private static String txResponse(String txJson) {
		return "{"
				+	"\"transactionJSON\": " + txJson + ", "
				+	"\"unsignedTransactionBytes\": \"deadbeef\""
				+ "}";
	}
	
	private static ContentResponse responseStub(String responseBody) {
		ContentResponse response = mock(ContentResponse.class);
		when(response.getContentAsString()).thenReturn(responseBody);
		return response;
	}
	
}
