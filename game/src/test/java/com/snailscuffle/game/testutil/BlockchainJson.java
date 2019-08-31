package com.snailscuffle.game.testutil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snailscuffle.game.blockchain.data.Alias;
import com.snailscuffle.game.blockchain.data.Block;
import com.snailscuffle.game.blockchain.data.Transaction;

public class BlockchainJson {
	
	public static String getBlockchainStatusResponse(boolean ready, long lastBlockId, int height) {
		Map<String, Object> data = new HashMap<>();
		
		data.put("apiProxy", false);
		data.put("application", "Ardor");
		data.put("blockchainState", ready ? "UP_TO_DATE" : "DOWNLOADING");
		data.put("correctInvalidFees", false);
		data.put("cumulativeDifficulty", "80162036691095023");
		data.put("currentMinRollbackHeight", "850200");
		data.put("includeExpiredPrunable", true);
		data.put("isDownloading", String.valueOf(!ready));
		data.put("isLightClient", false);
		data.put("isScanning", false);
		data.put("isTestnet", false);
		data.put("lastBlock", Long.toUnsignedString(lastBlockId));
		data.put("lastBlockchainFeeder", "111.222.333.444");
		data.put("lastBlockchainFeederHeight", "851937");
		data.put("ledgerTrimKeep", 30000);
		data.put("maxAPIRecords", 100);
		data.put("maxPrunableLifetime", 7776000);
		data.put("maxRollback", 800);
		data.put("numberOfBlocks", height + 1);
		data.put("requestProcessingTime", 1);
		data.put("services", Arrays.asList("CORS"));
		data.put("time", 50335296);
		data.put("version", "2.2.5");
		
		return serialize(data);
	}
	
	public static String getBalanceResponse(int balanceNQT) {
		Map<String, Object> data = new HashMap<>();
		
		data.put("unconfirmedBalanceNQT", String.valueOf(balanceNQT));
		data.put("balanceNQT", String.valueOf(balanceNQT));
		data.put("requestProcessingTime", 1);
		
		return serialize(data);
	}
	
	public static String aliasesToJson(List<Alias> aliases) {
		return serialize(aliasData(aliases));
	}
	
	public static String blocksToJson(List<Block> blocks) {
		return serialize(blockData(blocks));
	}
	
	public static String blockToJson(Block block, long previousBlockId) {
		Map<String, Object> data = blockData(block, Long.toUnsignedString(previousBlockId));
		return serialize(data);
	}
	
	public static String transactionJsonToBlockJson(long blockId, long previousBlockId, int height, List<String> transactions) {
		Block emptyBlock = new Block(blockId, height, height, new ArrayList<Transaction>());
		String emptyBlockJson = BlockchainJson.blockToJson(emptyBlock, previousBlockId);
		String txArray = "\"transactions\": [" + String.join(", ", transactions) + "]";
		return emptyBlockJson.replaceAll("\"transactions\":\\s*\\[\\s*\\]", txArray);
	}
	
	public static String transactionsToJson(List<Transaction> transactions) {
		return serialize(transactionData(transactions));
	}
	
	public static String serialize(Object data) {
		try {
			return new ObjectMapper().writer().writeValueAsString(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static List<Map<String, Object>> aliasData(List<Alias> aliases) {
		return aliases.stream().map(a -> aliasData(a)).collect(Collectors.toList());
	}
	
	private static Map<String, Object> aliasData(Alias alias) {
		Map<String, Object> data = new HashMap<>();
		
		data.put("aliasURI", "");
		data.put("aliasName", alias.name);
		data.put("accountRS", "ARDOR-0000-0000-0000-00000");
		data.put("alias", "123456");
		data.put("account", Long.toUnsignedString(alias.account));
		data.put("timestamp", 0);
		
		return data;
	}
	
	private static List<Map<String, Object>> blockData(List<Block> blocks) {
		List<Map<String, Object>> data = new ArrayList<>();
		for (int i = 0; i < blocks.size(); i++) {
			Block block = blocks.get(i);
			long previousBlockId = (i > 0) ? blocks.get(i - 1).id : 0;
			data.add(blockData(block, Long.toUnsignedString(previousBlockId)));
		}
		return data;
	}
	
	private static Map<String, Object> blockData(Block block, String previousBlockId) {
		Map<String, Object> data = new HashMap<>();
		
		data.put("baseTarget", "123456");
		data.put("block", Long.toUnsignedString(block.id));
		data.put("blockSignature", "John Hancock");
		data.put("cumulativeDifficulty", "123456");
		data.put("generatorSignature", "Yours Truly");
		data.put("generator", "123456");
		data.put("generatorPublicKey", "my public key");
		data.put("generatorRS", "ARDOR-0000-0000-0000-00000");
		data.put("height", block.height);
		data.put("numberOfTransactions", block.transactions.size());
		data.put("payloadHash", "123456");
		data.put("timestamp", block.timestamp);
		data.put("totalFeeFQT", block.transactions.isEmpty() ? "0" : "1000000");
		data.put("transactions", transactionData(block.transactions));
		data.put("version", 3);
		
		return data;
	}
	
	private static List<Map<String, Object>> transactionData(List<Transaction> txs) {
		return txs.stream().map(tx -> transactionData(tx)).collect(Collectors.toList());
	}
	
	private static Map<String, Object> transactionData(Transaction tx) {
		int type = 0;
		Map<String, Object> attachment = new HashMap<>();
		
		if (tx.message.length() > 0) {
			type = 1;
			attachment.put("message", tx.message);
			attachment.put("messageHash", "123456");
			attachment.put("messageIsText", true);
			attachment.put("version.PrunablePlainMessage", 1);
		} else if (tx.alias.length() > 0) {
			type = 8;
			attachment.put("alias", "snailscuffle" + tx.alias);
			attachment.put("uri", "acct:ARDOR-0000-0000-0000-00000");
			attachment.put("version.AliasAssignment", 1);
		}
		
		Map<String, Object> data = new HashMap<>();
		data.put("amountNQT", "0");
		if (!attachment.isEmpty()) {
			data.put("attachment", attachment);
		}
		data.put("block", Long.toUnsignedString(tx.blockId));
		data.put("blockTimestamp", 123456);
		data.put("chain", 2);
		data.put("confirmations", 1);
		data.put("deadline", 10);
		data.put("ecBlockHeight", 123456);
		data.put("ecBlockId", "123456");
		data.put("feeNQT", "1000000");
		data.put("fullHash", "123456");
		data.put("height", tx.height);
		data.put("phased", false);
		if (type != 8) {
			data.put("recipient", Long.toUnsignedString(tx.recipient));
			data.put("recipientRS", "ARDOR-1111-1111-1111-11111");
		}
		data.put("sender", Long.toUnsignedString(tx.sender));
		data.put("senderPublicKey", "123456");
		data.put("senderRS", "ARDOR-0000-0000-0000-00000");
		data.put("signature", "123456");
		data.put("signatureHash", "123456");
		data.put("subtype", 0);
		data.put("timestamp", 123456);
		data.put("transaction", 123456);
		data.put("transactionIndex", tx.index);
		data.put("type", type);
		data.put("version", 1);
		
		return data;
	}
	
}
