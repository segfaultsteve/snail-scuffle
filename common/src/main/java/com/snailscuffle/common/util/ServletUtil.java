package com.snailscuffle.common.util;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;

public class ServletUtil {
	
	public static void markHandled(HttpServletRequest request) {
		Request baseRequest = (request instanceof Request) ? ((Request)request) : HttpConnection.getCurrentConnection().getHttpChannel().getRequest();
		baseRequest.setHandled(true);
	}
	
}
