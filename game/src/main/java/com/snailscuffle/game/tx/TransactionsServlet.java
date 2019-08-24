package com.snailscuffle.game.tx;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.InvalidQueryException;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;

public class TransactionsServlet extends HttpServlet {
	
	private static final String NEW_ACCOUNT_PATH = "newaccount";
	private static final Logger logger = LoggerFactory.getLogger(TransactionsServlet.class);
	
	private final BlockchainSubsystem blockchain;
	
	public TransactionsServlet(BlockchainSubsystem blockchain) {
		this.blockchain = blockchain;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			String path = extractPath(request);
			if (path.equalsIgnoreCase(NEW_ACCOUNT_PATH)) {
				response.getWriter().write(newAccountResponse(request));
			} else if (path.length() > 0) {
				TransactionStatus status = blockchain.getTransactionStatus(path);
				response.getWriter().write(JsonUtil.serialize(status));
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (InvalidQueryException e) {
			logger.error("Invalid query", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.invalidQuery().because(e.getMessage()));
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			if (extractPath(request).length() > 0) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} else {
				String txJson = HttpUtil.extractBody(request);
				TransactionStatus status = blockchain.broadcastTransaction(txJson);
				response.getWriter().write(JsonUtil.serialize(status));
			}
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}
	
	private static String extractPath(HttpServletRequest request) {
		String path = request.getPathInfo();
		if (path == null) {
			path = "";
		}
		return path.replaceAll("^/|/$", "");
	}
	
	private String newAccountResponse(HttpServletRequest request) throws InvalidQueryException, AccountsException, BlockchainSubsystemException, InterruptedException {
		NewAccountTransactionQuery query = new NewAccountTransactionQuery(request);
		UnsignedTransaction tx = blockchain.createNewAccountTransaction(query.player, query.publicKey);
		return JsonUtil.serialize(tx);
	}
	
}
