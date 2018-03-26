package com.snailscuffle.game;

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
		// TODO
	}
	
	@Test
	public void doPostWithEmptyBody() throws Exception {
		String postBody = "";
		try (BufferedReader reader = new BufferedReader(new StringReader(postBody))) {
			when(request.getReader()).thenReturn(reader);
			
			(new BattleServlet()).doPost(request, response);

			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	@Test
	public void doPostWithPlainTextBody() throws Exception {
		String postBody = "not json";
		try (BufferedReader reader = new BufferedReader(new StringReader(postBody))) {
			when(request.getReader()).thenReturn(reader);

			(new BattleServlet()).doPost(request, response);

			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	@Test
	public void doPostWithNullBattlePlans() throws Exception {
		String postBody = "{}";
		
		try (BufferedReader reader = new BufferedReader(new StringReader(postBody))) {
			when(request.getReader()).thenReturn(reader);

			(new BattleServlet()).doPost(request, response);

			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
