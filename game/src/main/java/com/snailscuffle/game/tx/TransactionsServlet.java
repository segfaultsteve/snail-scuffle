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
import com.snailscuffle.game.blockchain.BlockchainUtil;
import com.snailscuffle.game.blockchain.IgnisNodeCommunicationException;
import com.snailscuffle.game.blockchain.data.BattlePlanCommitMessage;
import com.snailscuffle.game.blockchain.data.BattlePlanRevealMessage;

public class TransactionsServlet extends HttpServlet {
	
	private static final String NEW_ACCOUNT_PATH = "new-account";
	private static final String BP_COMMIT_PATH = "battle-plan-commit";
	private static final String BP_REVEAL_PATH = "battle-plan-reveal";
	private static final Logger logger = LoggerFactory.getLogger(TransactionsServlet.class);
	
	private final BlockchainSubsystem blockchain;
	
	public TransactionsServlet(BlockchainSubsystem blockchain) {
		this.blockchain = blockchain;
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			String path = HttpUtil.extractPath(request);
			if (path.equalsIgnoreCase(NEW_ACCOUNT_PATH)) {
				response.getWriter().write(newAccountResponse(request));
			} else if (path.equalsIgnoreCase(BP_COMMIT_PATH)) {
				response.getWriter().write(bpCommitResponse(request));
			} else if (path.equalsIgnoreCase(BP_REVEAL_PATH)) {
				response.getWriter().write(bpRevealResponse(request));
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
			if (HttpUtil.extractPath(request).length() > 0) {
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
	
	private String newAccountResponse(HttpServletRequest request) throws InvalidQueryException, AccountsException, BlockchainSubsystemException, InterruptedException {
		NewAccountTransactionQuery query = new NewAccountTransactionQuery(request);
		UnsignedTransaction tx = blockchain.createNewAccountTransaction(query.publicKey, query.player);
		return JsonUtil.serialize(tx);
	}
	
	private String bpCommitResponse(HttpServletRequest request) throws InvalidQueryException, IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		BattlePlanTransactionQuery query = new BattlePlanTransactionQuery(request);
		BattlePlanCommitMessage bpCommitMessage = new BattlePlanCommitMessage(query.battleId, query.round, BlockchainUtil.sha256Hash(query.battlePlan));
		UnsignedTransaction tx = blockchain.createArbitraryMessageTransaction(query.publicKey, query.recipient, bpCommitMessage);
		return JsonUtil.serialize(tx);
	}
	
	private String bpRevealResponse(HttpServletRequest request) throws InvalidQueryException, IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		BattlePlanTransactionQuery query = new BattlePlanTransactionQuery(request);
		BattlePlanRevealMessage bpCommitMessage = new BattlePlanRevealMessage(query.battleId, query.round, query.battlePlan);
		UnsignedTransaction tx = blockchain.createArbitraryMessageTransaction(query.publicKey, query.recipient, bpCommitMessage);
		return JsonUtil.serialize(tx);
	}
	
}
