package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

public class AccountsQuery {
	
	public final String player;
	public final String id;
	
	public AccountsQuery(HttpServletRequest request) throws InvalidQueryException {
		if (request.getParameterMap().isEmpty()) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
		
		player = HttpUtil.getQueryParameterValue("player", request);
		id = HttpUtil.getQueryParameterValue("id", request);
		
		if (id == null && player == null) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
	}
	
}
