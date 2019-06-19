package com.snailscuffle.game.accounts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;

public class AccountsServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AccountsServlet.class);
	
	private final Accounts accounts;
	
	public AccountsServlet(Accounts accounts) {
		this.accounts = accounts;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			AccountsQuery query = new AccountsQuery(request);
			Account account = accounts.get(query);
			JsonUtil.serialize(account, response.getWriter());
		} catch (InvalidQueryException e) {
			logger.error("Invalid query to /accounts", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.invalidQuery().because(e.getMessage()));
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}

}
