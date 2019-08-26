package com.snailscuffle.game.testutil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public class ServletUtil {
	
	public static String sendGETRequest(BiConsumer<Request, Response> doGet, String path, String queryString) {
		Request request = mock(Request.class);
		when(request.getPathInfo()).thenReturn(path);
		when(request.getQueryString()).thenReturn(queryString);
		when(request.getParameterMap()).thenReturn(parameterMapFor(queryString));
		
		Response response = mock(Response.class);
		StringWriter responseBuffer = new StringWriter();
		
		try {
			when(response.getWriter()).thenReturn(new PrintWriter(responseBuffer));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		doGet.accept(request, response);
		return responseBuffer.toString();
	}
	
	public static String sendPUTRequest(BiConsumer<Request, Response> doPut, String path, String body) {
		try {
			Request request = mock(Request.class);
			when(request.getPathInfo()).thenReturn(path);
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
			
			Response response = mock(Response.class);
			StringWriter responseBuffer = new StringWriter();
			when(response.getWriter()).thenReturn(new PrintWriter(responseBuffer));
			
			doPut.accept(request, response);
			return responseBuffer.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Map<String, String[]> parameterMapFor(String queryString) {
		Map<String, String[]> parameterMap = new HashMap<>();
		String[] queryParams = queryString.split("&");
		for (String param : queryParams) {
			String[] kvp = param.split("=");
			parameterMap.put(kvp[0], new String[] { kvp[1] });
		}
		return parameterMap;
	}
	
}
