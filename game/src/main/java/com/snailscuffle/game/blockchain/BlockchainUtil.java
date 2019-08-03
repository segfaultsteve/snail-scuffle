package com.snailscuffle.game.blockchain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.util.JsonUtil;

public class BlockchainUtil {
	
	public static JsonNode parseJson(String json, String errorString) throws BlockchainSubsystemException {
		try {
			return JsonUtil.deserialize(json);
		} catch (IOException e) {
			throw new BlockchainSubsystemException(errorString + ": " + e.getMessage());
		}
	}
	
	public static JsonNode getResponsePropertyOrThrow(JsonNode responseJson, String property, String apiFunction) throws BlockchainDataNotFoundException {
		JsonNode value = responseJson.get(property);
		if (value == null) {
			String error = "Response from " + apiFunction + " did not contain property '" + property + "'";
			JsonNode errorDescription = responseJson.get("errorDescription");
			if (errorDescription != null) {
				error += ": " + errorDescription.textValue();
			}
			throw new BlockchainDataNotFoundException(error);
		}
		return value;
	}
	
	public static long parseUnsignedLong(JsonNode value, String errorString) throws BlockchainSubsystemException {
		try {
			return Long.parseUnsignedLong(value.textValue());
		} catch (NumberFormatException e) {
			throw new BlockchainSubsystemException(errorString);
		}
	}
	
	public static String sha256Hash(String input) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Java installation is broken! All JVMs must support SHA-256!!", e);
		}
	}
	
	public static String sha256Hash(BattlePlan battlePlan) {
		String bpJson;
		try {
			bpJson = JsonUtil.serialize(battlePlan);
			return sha256Hash(bpJson);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error serializing battle plan", e);		// this should never happen
		}
	}
	
}
