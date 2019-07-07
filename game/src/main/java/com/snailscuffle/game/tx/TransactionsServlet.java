package com.snailscuffle.game.tx;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.util.ServletUtil;
import com.snailscuffle.game.accounts.AccountsServlet;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;

public class TransactionsServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(AccountsServlet.class);
	
	public TransactionsServlet(BlockchainSubsystem blockchain) {
		
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			// TODO
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}
	
}
