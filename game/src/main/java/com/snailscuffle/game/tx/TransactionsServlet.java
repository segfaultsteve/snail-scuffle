package com.snailscuffle.game.tx;

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
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.accounts.AccountsServlet;
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.blockchain.BlockchainSubsystemException;
import com.snailscuffle.game.blockchain.IgnisNodeCommunicationException;

public class TransactionsServlet extends HttpServlet {
	
	private static final String NEW_ACCOUNT_PATH = "newaccount";
	private static final Logger logger = LoggerFactory.getLogger(AccountsServlet.class);
	
	private final BlockchainSubsystem blockchain;
	
	public TransactionsServlet(BlockchainSubsystem blockchain) {
		this.blockchain = blockchain;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			String path = extractPath(request);
			if (path.equalsIgnoreCase(NEW_ACCOUNT_PATH)) {
				response.getWriter().write(newAccountResponse(request));
			} else if (path.length() > 0) {
				response.getWriter().write(getTransactionStatus(path));
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
		try {
			return JsonUtil.serialize(tx);
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize unsigned transaction: " + tx.asJson);	// this should never happen
		}
	}
	
	private String getTransactionStatus(String txid) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		TransactionStatus status = blockchain.getTransactionStatus(txid);
		try {
			return JsonUtil.serialize(status);
		} catch (IOException e) {
			throw new RuntimeException("Failed to serialize transaction status");	// this should never happen
		}
	}
	
}
