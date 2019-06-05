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

	@Mock private Request request;
	@Mock private Response response;
	private StringWriter responseBuffer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		responseBuffer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(responseBuffer);
		when(response.getWriter()).thenReturn(printWriter);
	}

	@Test
	public void doPost() throws Exception {
		BattlePlan bp = new BattlePlan();
		bp.snail = Snail.DALE;
		bp.weapon = Weapon.ROCKET;
		bp.validate();
		BattleConfig config = new BattleConfig(bp, bp, bp, bp, bp, bp);		// three periods
		
		postRequest(JsonUtil.serialize(config));
		BattleResult result = JsonUtil.deserialize(BattleResult.class, responseBuffer.toString());
		
		assertEquals(0, result.winnerIndex);	// player 1 (index 0) wins when battle plans are identical
	}
	
	@Test
	public void doPostWithEmptyBody() throws Exception {
		postRequest("");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void doPostWithPlainTextBody() throws Exception {
		postRequest("not json");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	@Test
	public void doPostWithNullBattlePlans() throws Exception {
		postRequest("{}");
		verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	private void postRequest(String body) throws Exception {
		try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
			when(request.getReader()).thenReturn(reader);
			(new BattleServlet()).doPost(request, response);
		}
	}

}
