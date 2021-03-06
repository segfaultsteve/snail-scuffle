package com.snailscuffle.game.battle;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattleResult;
import com.snailscuffle.common.battle.InvalidBattleException;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;

public class BattleServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(BattleServlet.class);
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		try {
			String json = HttpUtil.extractBody(request);
			BattleConfig battleConfig = JsonUtil.deserialize(BattleConfig.class, json);
			battleConfig.validate();
			BattleResult result = (new Battle(battleConfig)).getResult();
			JsonUtil.serialize(result, response.getWriter());
		} catch (IOException e) {
			logger.error("Failed to deserialize battle", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.battleConfigDeserializationError().because(e.getMessage()));
		} catch (InvalidBattleException e) {
			logger.error("Invalid battle config", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.invalidBattleConfig().because(e.getMessage()));
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError());
		}
		ServletUtil.markHandled(request);
	}

}
