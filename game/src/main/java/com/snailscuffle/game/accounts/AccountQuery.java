package com.snailscuffle.game.accounts;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;

class AccountQuery {
	
	private static final int DEFAULT_COUNT = 10;
	private static final int DEFAULT_OFFSET = 0;
	
	final String player;
	final String id;
	final int count;
	final int offset;
	
	AccountQuery(HttpServletRequest request) throws InvalidQueryException {
		Optional<String> unrecognizedParam = request.getParameterMap().keySet().stream()
				.filter(p -> !p.equals("player") && !p.equals("id") && !p.equals("count") && !p.equals("offset"))
				.findFirst();
		
		if (unrecognizedParam.isPresent()) {
			throw new InvalidQueryException("Unrecognized query parameter: '" + unrecognizedParam.get() + "'");
		}
		
		String accountIdInPath = HttpUtil.extractPath(request);
		player = accountIdInPath.isEmpty() ? HttpUtil.getQueryParameterValue("player", request) : null;
		id = accountIdInPath.isEmpty() ? HttpUtil.getQueryParameterValue("id", request) : accountIdInPath;
		count = accountIdInPath.isEmpty() ? getNonnegativeIntParam("count", request, DEFAULT_COUNT) : DEFAULT_COUNT;
		offset = accountIdInPath.isEmpty() ? getNonnegativeIntParam("offset", request, DEFAULT_OFFSET) : DEFAULT_OFFSET;
	}
	
	boolean byId() {
		return id != null;
	}
	
	boolean byPlayer() {
		return player != null;
	}
	
	private static int getNonnegativeIntParam(String param, HttpServletRequest request, int defaultValue) {
		String valueString = HttpUtil.getQueryParameterValue(param, request);
		if (valueString == null) {
			return defaultValue;
		}
		
		try {
			int result = Integer.parseInt(valueString);
			if (result < 0) {
				result = defaultValue;
			}
			return result;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
}
