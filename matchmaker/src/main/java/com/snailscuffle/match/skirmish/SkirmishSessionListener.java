package com.snailscuffle.match.skirmish;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.snailscuffle.match.players.PlayerData;

public class SkirmishSessionListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent se) {}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		PlayerData player = (PlayerData) session.getAttribute(PlayerData.ATTRIBUTE_KEY);
		if (player != null) {
			player.forfeit();
		}
	}

}
