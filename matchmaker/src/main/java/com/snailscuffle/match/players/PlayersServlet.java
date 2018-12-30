package com.snailscuffle.match.players;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.util.HttpUtil;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;
import com.snailscuffle.match.InvalidPlayerException;

public class PlayersServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(PlayersServlet.class);
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.addHeader("Cache-Control","no-cache");
		response.addHeader("Cache-Control","no-store");
		
		try {
			String json = HttpUtil.extractBody(request);
			PlayerConfig player = JsonUtil.deserialize(PlayerConfig.class, json);
			player.validate();
			// TODO: log in
		} catch (IOException e) {
			logger.error("Failed to deserialize player", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.playerDeserializationError().because(e.getMessage()));
		} catch (InvalidPlayerException e) {
			logger.error("Invalid player", e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().print(ErrorResponse.invalidPlayer().because(e.getMessage()));
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}

}
