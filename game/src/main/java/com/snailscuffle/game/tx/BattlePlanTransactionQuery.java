package com.snailscuffle.game.tx;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;

class BattlePlanTransactionQuery {
	
	final String publicKey;
	final String recipient;
	final String battleId;
	final int round;
	final BattlePlan battlePlan;
	
	BattlePlanTransactionQuery(HttpServletRequest request) throws InvalidQueryException {
		String path = "/transactions/" + HttpUtil.extractPath(request);
		
		try {
			JsonNode body = JsonUtil.deserialize(HttpUtil.extractBody(request));
			publicKey = getPropertyOrThrow(body, "publicKey", path);
			recipient = getPropertyOrThrow(body, "recipient", path);
			battleId = getPropertyOrThrow(body, "battleId", path);
			round = Integer.parseInt(getPropertyOrThrow(body, "round", path));
			
			JsonNode battlePlanNode = body.get("battlePlan");
			if (battlePlanNode == null) {
				throw new InvalidQueryException("Requests to " + path + " must include battlePlan in the body");
			}
			battlePlan = JsonUtil.deserialize(BattlePlan.class, battlePlanNode);
		} catch (IOException e) {
			throw new InvalidQueryException("Failed to deserialize request to " + path);
		} catch (NumberFormatException e) {
			throw new InvalidQueryException("Invalid round");
		}
	}
	
	private static String getPropertyOrThrow(JsonNode requestBody, String property, String path) throws InvalidQueryException {
		JsonNode propertyNode = requestBody.get(property);
		if (propertyNode == null) {
			throw new InvalidQueryException("Requests to " + path + " must include " + property + " in the body");
		}
		return propertyNode.asText();
	}
	
}
