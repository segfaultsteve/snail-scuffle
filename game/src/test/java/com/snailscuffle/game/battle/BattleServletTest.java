package com.snailscuffle.game.battle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.BattleResult;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;
import com.snailscuffle.common.util.JsonUtil;

public class BattleServletTest {
	
	@Mock private Response response;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void doPost() throws Exception {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.ROCKET;
		bp.validate();
		BattleConfig config = new BattleConfig(bp, bp, bp, bp, bp, bp);		// three periods
		
		String response = sendPostRequest(JsonUtil.serialize(config));
		BattleResult result = JsonUtil.deserialize(BattleResult.class, response);
		
		assertEquals(0, result.winnerIndex);	// player 1 (index 0) wins when battle plans are identical
	}
	
	@Test
	public void doPostWithEmptyBody() throws Exception {
		sendPostRequest("");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void doPostWithPlainTextBody() throws Exception {
		sendPostRequest("not json");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void doPostWithNullBattlePlans() throws Exception {
		sendPostRequest("{}");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	private String sendPostRequest(String body) throws Exception {
		Request request = mock(Request.class);
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
		
		StringWriter responseBuffer = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(responseBuffer));
		
		(new BattleServlet()).doPost(request, response);
		
		return responseBuffer.toString();
	}

}
