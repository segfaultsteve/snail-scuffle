package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class AccountsQuery {
	
	final String player;
	final String id;
	
	AccountsQuery(HttpServletRequest request) throws InvalidQueryException {
		if (request.getParameterMap().isEmpty()) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or account=[account ID] as query parameters");
		}
		
		player = HttpUtil.getQueryParameterValue("player", request);
		id = HttpUtil.getQueryParameterValue("id", request);
		
		if (id == null && player == null) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or account=[account ID] as query parameters");
		}
	}
	
}
