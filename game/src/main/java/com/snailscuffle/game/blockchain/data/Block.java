package com.snailscuffle.game.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;
import com.snailscuffle.game.blockchain.BlockchainUtil;

public class Block {
	
	public final long id;
	public final int height;
	public final int timestamp;
	public final List<Transaction> transactions = new ArrayList<>();
	
	public Block(JsonNode blockJson, String apiFunction) throws BlockchainSubsystemException {
		JsonNode blockIdNode = BlockchainUtil.getResponsePropertyOrThrow(blockJson, "block", apiFunction);
		JsonNode heightNode = BlockchainUtil.getResponsePropertyOrThrow(blockJson, "height", apiFunction);
		JsonNode timestampNode = BlockchainUtil.getResponsePropertyOrThrow(blockJson, "timestamp", apiFunction);
		JsonNode txNode = blockJson.get("transactions");
		
		id = BlockchainUtil.parseUnsignedLong(blockIdNode, apiFunction + " returned invalid block ID '" + blockIdNode.textValue() + "'");
		height = heightNode.asInt();
		timestamp = timestampNode.asInt();
		if (txNode != null) {
			transactions.addAll(Transaction.parseAll(txNode, apiFunction));
		}
	}
	
	public static List<Block> parseAll(JsonNode blockArray, String apiFunction) throws BlockchainSubsystemException {
		List<Block> blocks = new ArrayList<>();
		for (JsonNode blockNode : blockArray) {
			blocks.add(new Block(blockNode, apiFunction));
		}
		return blocks;
	}
	
}
