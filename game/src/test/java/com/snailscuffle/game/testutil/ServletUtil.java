package com.snailscuffle.game.testutil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public class ServletUtil {
	
	public static String sendHttpRequest(ThrowingBiConsumer<Request, Response> doRequest, String path, String queryString) {
		return sendHttpRequest(doRequest, path, queryString, "");
	}
	
	public static String sendHttpRequest(ThrowingBiConsumer<Request, Response> doRequest, String path, String queryString, String body) {
		try {
			Request request = mock(Request.class);
			when(request.getPathInfo()).thenReturn(path);
			when(request.getQueryString()).thenReturn(queryString);
			when(request.getParameterMap()).thenReturn(parameterMapFor(queryString));
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
			
			Response response = mock(Response.class);
			StringWriter responseBuffer = new StringWriter();
			when(response.getWriter()).thenReturn(new PrintWriter(responseBuffer));
			
			doRequest.accept(request, response);
			return responseBuffer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Map<String, String[]> parameterMapFor(String queryString) {
		Map<String, String[]> parameterMap = new HashMap<>();
		String[] queryParams = queryString.split("&");
		for (String param : queryParams) {
			String[] kvp = param.split("=");
			if (kvp.length > 1) {
				parameterMap.put(kvp[0], new String[] { kvp[1] });
			}
		}
		return parameterMap;
	}
	
}
