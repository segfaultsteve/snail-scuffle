package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class AccountQuery {
	
	final String player;
	final String id;
	
	AccountQuery(HttpServletRequest request) throws InvalidQueryException {
		String accountIdInPath = checkPathForAccountId(request.getPathInfo());
		
		player = HttpUtil.getQueryParameterValue("player", request);
		id = (accountIdInPath == null) ? HttpUtil.getQueryParameterValue("id", request) : accountIdInPath;
		
		if (id == null && player == null) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
	}
	
	private static String checkPathForAccountId(String path) throws InvalidQueryException {
		if (path == null || path.length() <= 1) {
			return null;
		}
		
		String pathTrimmed = path.replaceAll("^/|/$", "");	// remove leading and trailing slashes
		if (pathTrimmed.contains("/")) {
			throw new InvalidQueryException("/accounts/" + pathTrimmed + " is not a valid path");
		}
		
		return (pathTrimmed.length() > 0) ? pathTrimmed : null;
	}
	
	boolean byId() {
		return id != null;
	}
	
	boolean byPlayer() {
		return id == null;
	}
	
}
