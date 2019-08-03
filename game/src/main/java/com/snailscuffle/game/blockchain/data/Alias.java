package com.snailscuffle.game.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;
import com.snailscuffle.game.blockchain.BlockchainUtil;

public class Alias {
	
	public final String name;
	public final long account;
	
	public Alias(String name, long account) {
		this.name = name;
		this.account = account;
	}
	
	public Alias(JsonNode aliasNode, String apiFunction) throws BlockchainSubsystemException {
		name = BlockchainUtil.getResponsePropertyOrThrow(aliasNode, "aliasName", apiFunction).textValue();
		String accountString = BlockchainUtil.getResponsePropertyOrThrow(aliasNode, "account", apiFunction).textValue();
		account = Long.parseUnsignedLong(accountString);
	}
	
	public static List<Alias> parseAll(JsonNode aliasArray, String apiFunction) throws BlockchainSubsystemException {
		List<Alias> aliases = new ArrayList<>();
		for (JsonNode aliasNode : aliasArray) {
			aliases.add(new Alias(aliasNode, apiFunction));
		}
		return aliases;
	}
	
}
