package com.snailscuffle.game.accounts;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

public class AccountQuery {
	
	public final String player;
	public final String id;
	
	public static AccountQuery forPlayer(String player) throws InvalidQueryException {
		if (player == null || player.length() == 0) {
			throw new InvalidQueryException("Invalid username");
		}
		return new AccountQuery(player, null);
	}
	
	public static AccountQuery forId(String id) throws InvalidQueryException {
		if (id == null || id.length() == 0) {
			throw new InvalidQueryException("Invalid account ID");
		}
		return new AccountQuery(null, id);
	}
	
	public AccountQuery(HttpServletRequest request) throws InvalidQueryException {
		if (request.getParameterMap().isEmpty()) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
		
		player = HttpUtil.getQueryParameterValue("player", request);
		id = HttpUtil.getQueryParameterValue("id", request);
		
		if (id == null && player == null) {
			throw new InvalidQueryException("Requests to /accounts must include either player=[username] or id=[account ID] as query parameters");
		}
	}
	
	private AccountQuery(String player, String id) {
		this.player = player;
		this.id = id;
	}
	
}
