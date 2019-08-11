package com.snailscuffle.game.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;
import com.snailscuffle.game.blockchain.BlockchainUtil;

public class Transaction {
	
	public final long sender;
	public final long recipient;
	public final int height;
	public final int index;
	public final long blockId;
	public final String message;
	public final String alias;		// Note: This is merely the aliasName (if present) in the attachments node. It does not necessarily belong to sender.
	
	public Transaction(long sender, long recipient, int height, int index, long blockId, String message, String alias) {
		this.sender = sender;
		this.recipient = recipient;
		this.height = height;
		this.index = index;
		this.blockId = blockId;
		this.message = message;
		this.alias = alias;
	}
	
	public Transaction(JsonNode txNode, String apiFunction) throws BlockchainSubsystemException {
		JsonNode senderNode = BlockchainUtil.getResponsePropertyOrThrow(txNode, "sender", apiFunction);
		JsonNode recipientNode = txNode.get("recipient");
		JsonNode heightNode = BlockchainUtil.getResponsePropertyOrThrow(txNode, "height", apiFunction);
		JsonNode indexNode = BlockchainUtil.getResponsePropertyOrThrow(txNode, "transactionIndex", apiFunction);
		JsonNode blockNode = BlockchainUtil.getResponsePropertyOrThrow(txNode, "block", apiFunction);
		
		JsonNode attachmentNode = txNode.get("attachment");
		JsonNode messageNode = null;
		JsonNode aliasNode = null;
		if (attachmentNode != null) {
			messageNode = attachmentNode.get("message");
			aliasNode = attachmentNode.get("alias");
		}
		
		sender = BlockchainUtil.parseUnsignedLong(senderNode, apiFunction + " returned invalid sender account ID '" + senderNode.textValue() + "'");
		recipient = (recipientNode == null) ? 0 : BlockchainUtil.parseUnsignedLong(recipientNode, apiFunction + " returned invalid recipient account ID '" + recipientNode.textValue() + "'");
		height = heightNode.asInt();
		index = indexNode.asInt();
		blockId = BlockchainUtil.parseUnsignedLong(blockNode, apiFunction + " returned invalid block ID '" + blockNode.textValue() + "'");
		message = (messageNode == null) ? "" : messageNode.asText();
		alias = (aliasNode == null || !aliasNode.asText().startsWith("snailscuffle")) ? "" : aliasNode.asText().replaceFirst("snailscuffle", "");
	}
	
	public static List<Transaction> parseAll(JsonNode txArray, String apiFunction) throws BlockchainSubsystemException {
		List<Transaction> txs = new ArrayList<>();
		for (JsonNode txNode : txArray) {
			txs.add(new Transaction(txNode, apiFunction));
		}
		return txs;
	}
	
}
