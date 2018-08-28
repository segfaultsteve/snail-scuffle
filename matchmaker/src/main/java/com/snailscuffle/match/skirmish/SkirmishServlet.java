package com.snailscuffle.match.skirmish;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.InvalidBattleException;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;
import com.snailscuffle.match.NotAuthorizedException;
import com.snailscuffle.match.players.PlayerData;

public class SkirmishServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(SkirmishServlet.class);
	
	private SkirmishMatcher skirmishEngine = new SkirmishMatcher();
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.addHeader("Cache-Control","no-cache");
		response.addHeader("Cache-Control","no-store");
		
		try {
			String skirmishId = tryExtractSkirmishId(request.getPathInfo());
			Skirmish skirmish = (skirmishId == null) ? null : skirmishEngine.getSkirmish(skirmishId);
			if (skirmishId == null) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			} else if (skirmish == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			} else {
				PlayerData player = (PlayerData) request.getSession().getAttribute(PlayerData.ATTRIBUTE_KEY);
				String playerId = (player == null) ? "" : player.id;
				JsonUtil.serialize(SkirmishResponse.forSkirmish(skirmish, playerId), response.getWriter());
			}
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError());
		}
		
		ServletUtil.markHandled(request);
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		try {
			HttpSession session = request.getSession();
			PlayerData player = (PlayerData) session.getAttribute(PlayerData.ATTRIBUTE_KEY);
			if (player == null) {
				// TODO: client sends screen name in body of PUT if signed in; check for it here to
				// make sure client doesn't think it's signed in (i.e., used to be, but timed out)
				player = PlayerData.createGuestPlayer(session.getId());
				session.setAttribute(PlayerData.ATTRIBUTE_KEY, player);
			}
			
			String skirmishId = tryExtractSkirmishId(request.getPathInfo());
			if (skirmishId == null) {
				skirmishEngine.tryMatchPlayer(player);
			} else {
				throwIfNotAuthorized(player, skirmishId);
				String battlePlanJson = HttpUtil.extractBody(request);
				BattlePlan battlePlan = JsonUtil.deserialize(BattlePlan.class, battlePlanJson);
				battlePlan.validate();
				player.tryAddBattlePlan(battlePlan);
			}
			JsonUtil.serialize(SkirmishResponse.forPlayer(player), response.getWriter());
		} catch (NotAuthorizedException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().print(ErrorResponse.notAuthorized().because(e.getMessage()));
		} catch (IOException e) {
			logger.error("Failed to deserialize battle plan", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.battlePlanDeserializationError().because(e.getMessage()));
		} catch (InvalidBattleException e) {
			logger.error("Invalid request", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.invalidBattlePlan().because(e.getMessage()));
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError());
		}
		
		ServletUtil.markHandled(request);
	}
	
	private static String tryExtractSkirmishId(String urlPathInfo) {
		if (urlPathInfo == null || urlPathInfo.length() <= 1) {
			return null;
		} else {
			return urlPathInfo.substring(1);
		}
	}
	
	private static void throwIfNotAuthorized(PlayerData player, String skirmishId) throws NotAuthorizedException {
		if (player.skirmish == null || !player.skirmish.getId().toString().equals(skirmishId)) {
			throw new NotAuthorizedException("Submitting player is not part of this skirmish");
		}
	}
	
}
