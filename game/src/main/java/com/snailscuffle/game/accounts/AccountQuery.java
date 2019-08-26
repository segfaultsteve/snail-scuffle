package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class AccountQuery {
	
	final String player;
	final String id;
	
	AccountQuery(HttpServletRequest request) throws InvalidQueryException {
		player = HttpUtil.getQueryParameterValue("player", request);
		
		String accountIdInPath = HttpUtil.extractPath(request);
		id = (accountIdInPath.length() > 0) ? accountIdInPath : HttpUtil.getQueryParameterValue("id", request);
		
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
