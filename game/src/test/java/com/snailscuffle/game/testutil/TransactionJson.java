package com.snailscuffle.game.testutil;

import java.util.HashMap;
import java.util.Map;

import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.game.blockchain.BlockchainUtil;

public class TransactionJson {
	
	public static String newAccountTransaction(int height, long blockId, int index, long sender, String publicKey, String username) {
		Map<String, Object> attachment = new HashMap<>();
		attachment.put("alias", username);
		attachment.put("uri", "");
		attachment.put("version.AliasAssignment", 1);
		
		Map<String, Object> tx = commonTxProperties(height, blockId, index, sender, publicKey);
		tx.put("attachment", attachment);
		
		return JsonUtil.serialize(tx);
	}
	
	public static String messageTransaction(int height, long blockId, int index, long sender, String publicKey, long recipient, String message) {
		Map<String, Object> attachment = new HashMap<>();
		attachment.put("message", message);
		attachment.put("messageHash", BlockchainUtil.sha256Hash(message));
		attachment.put("messageIsText", true);
		attachment.put("version.ArbitraryMessage", 0);
		attachment.put("version.PrunablePlainMessage", 1);
		
		Map<String, Object> tx = commonTxProperties(height, blockId, index, sender, publicKey);
		tx.put("attachment", attachment);
		tx.put("recipient", Long.toUnsignedString(recipient));
		
		return JsonUtil.serialize(tx);
	}
	
	private static Map<String, Object> commonTxProperties(int height, long blockId, int index, long sender, String publicKey) {
		Map<String, Object> tx = new HashMap<>();
		tx.put("block", Long.toUnsignedString(blockId));
		tx.put("blockTimestamp", height);
		tx.put("feeNQT", "199000000");
		tx.put("height", height);
		tx.put("sender", Long.toUnsignedString(sender));
		tx.put("senderPublicKey", publicKey);
		tx.put("subtype", 0);
		tx.put("timestamp", height);
		tx.put("transactionIndex", index);
		tx.put("type", 8);
		tx.put("amountNQT", "0");
		tx.put("chain", 2);
		tx.put("confirmations", 1);
		tx.put("deadline", 15);
		tx.put("ecBlockId", "abc123");
		tx.put("fullHash", "abc123");
		tx.put("fxtTransaction", 123);
		tx.put("phased", false);
		tx.put("senderRS", "ARDOR-IGNORE-THIS");
		tx.put("signature", "abc123");
		tx.put("signatureHash", "abc123");
		tx.put("version", 1);
		return tx;
	}
	
}
