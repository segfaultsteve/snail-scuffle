package com.snailscuffle.game;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServlet extends HttpServlet {
	
	private final String redirectBaseUrl;
	
	public RedirectServlet(String redirectBaseUrl) {
		this.redirectBaseUrl = redirectBaseUrl;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SendRedirect(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SendRedirect(request, response);
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SendRedirect(request, response);
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SendRedirect(request, response);
	}
	
	private void SendRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String redirectUrl = redirectBaseUrl
				+ (request.getPathInfo() == null ? "" : request.getPathInfo())
				+ (request.getQueryString() == null ? "" : "?" + request.getQueryString());
		
		response.addHeader("Location", redirectUrl);
		response.sendError(HttpServletResponse.SC_TEMPORARY_REDIRECT);
	}
	
}
