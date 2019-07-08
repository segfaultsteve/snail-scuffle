package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class AccountQuery {
	
	final String player;
	final String id;
	
	AccountQuery(HttpServletRequest request) throws InvalidQueryException {
		if (request.getParameterMap().isEmpty()) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
		
		player = HttpUtil.getQueryParameterValue("player", request);
		id = HttpUtil.getQueryParameterValue("id", request);
		
		if (id == null && player == null) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
	}
	
	boolean byId() {
		return id != null;
	}
	
	boolean byPlayer() {
		return id == null;
	}
	
}
