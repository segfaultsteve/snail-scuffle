package com.snailscuffle.game.tx;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;

class NewAccountTransactionQuery {
	
	final String player;
	final String publicKey;
	
	NewAccountTransactionQuery(HttpServletRequest request) throws InvalidQueryException {
		try {
			JsonNode body = JsonUtil.deserialize(HttpUtil.extractBody(request));
			JsonNode playerNode = body.get("player");
			JsonNode publicKeyNode = body.get("publicKey");
			
			if (playerNode == null || publicKeyNode == null) {
				String missingParam = (playerNode == null) ? "player=[username]" : "publicKey=[public key]";
				throw new InvalidQueryException("Requests to /transactions/new-account must include " + missingParam + " in the query string");
			}
			
			player = playerNode.asText();
			publicKey = publicKeyNode.asText();
		} catch (IOException e) {
			throw new InvalidQueryException("Failed to deserialize request to /transactions/new-account");
		}
	}
	
}
