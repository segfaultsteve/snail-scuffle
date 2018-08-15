package com.snailscuffle.match;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.match.players.PlayersServlet;
import com.snailscuffle.match.skirmish.SkirmishServlet;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			MatchmakerSettings settings = new MatchmakerSettings();
			Server server = configureJettyServer(settings);
			server.start();
			server.join();
		} catch(Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	private static Server configureJettyServer(MatchmakerSettings settings) throws MalformedURLException, URISyntaxException {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.getServletContext().getSessionCookieConfig().setHttpOnly(true);
		context.addServlet(PlayersServlet.class, "/players");
		context.addServlet(SkirmishServlet.class, "/skirmishes/*");
		
		FilterHolder cors = context.addFilter(CrossOriginFilter.class, "/*", null);
		cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,HEAD,OPTIONS");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

		
		server.addConnector(connector);
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.setRequestLog(createRequestLog());
		return server;
	}
	
	private static NCSARequestLog createRequestLog() {
		NCSARequestLog requestLog = new NCSARequestLog("yyyy_mm_dd.request.log");
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLog.setLogLatency(true);
		requestLog.setRetainDays(90);
		return requestLog;
	}
	
}
