package com.snailscuffle.game.blockchain.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;
import com.snailscuffle.game.blockchain.BlockchainUtil;

public class Block {
	
	public final long id;
	public final int height;
	public final int timestamp;
	
	public Block(long id, int height, int timestamp) {
		this.id = id;
		this.height = height;
		this.timestamp = timestamp;
	}
	
	public Block(JsonNode getBlockResponse) throws BlockchainSubsystemException {
		JsonNode blockIdNode = BlockchainUtil.getResponsePropertyOrThrow(getBlockResponse, "block", "getBlock");
		JsonNode heightNode = BlockchainUtil.getResponsePropertyOrThrow(getBlockResponse, "height", "getBlock");
		JsonNode timestampNode = BlockchainUtil.getResponsePropertyOrThrow(getBlockResponse, "timestamp", "getBlock");
		
		id = BlockchainUtil.parseUnsignedLong(blockIdNode, "getBlock returned invalid block ID '" + blockIdNode.textValue() + "'");
		height = heightNode.asInt();
		timestamp = timestampNode.asInt();
	}
	
}
