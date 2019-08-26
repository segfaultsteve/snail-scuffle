package com.snailscuffle.common.util;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
	
	public static String extractPath(HttpServletRequest request) {
		String path = request.getPathInfo();
		if (path == null) {
			path = "";
		}
		return path.replaceAll("^/|/$", "");
	}
	
	public static String getQueryParameterValue(String paramName, HttpServletRequest request) {
		String[] values = request.getParameterMap().get(paramName);
		if (values == null || values[0].isEmpty()) {
			return null;
		} else {
			return values[0];
		}
	}
	
	public static String extractBody(HttpServletRequest request) {
		String body = "";
		try {
			body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			logger.error("Failed to process request", e);
		}
		return body;
	}

}
