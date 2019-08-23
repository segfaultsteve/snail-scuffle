package com.snailscuffle.game.tx;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class NewAccountTransactionQuery {
	
	final String player;
	final String publicKey;
	
	NewAccountTransactionQuery(HttpServletRequest request) throws InvalidQueryException {
		player = HttpUtil.getQueryParameterValue("player", request);
		publicKey = HttpUtil.getQueryParameterValue("publicKey", request);
		
		if (player == null || publicKey == null) {
			String missingParam = (player == null) ? "player=[username]" : "publicKey=[public key]";
			throw new InvalidQueryException("Requests to /transactions/newaccount must include " + missingParam + " in the query string");
		}
	}
	
}
