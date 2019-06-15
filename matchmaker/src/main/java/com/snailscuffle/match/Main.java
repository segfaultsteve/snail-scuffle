package com.snailscuffle.match;

import static com.snailscuffle.common.util.LoggingUtil.createRequestLog;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.match.players.PlayersServlet;
import com.snailscuffle.match.skirmish.SkirmishServlet;
import com.snailscuffle.match.skirmish.SkirmishSessionListener;

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
	
	private static Server configureJettyServer(MatchmakerSettings settings) throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.getSessionHandler().setMaxInactiveInterval(15*60);		// invalidate idle sessions after 15 min
		context.getSessionHandler().addEventListener(new SkirmishSessionListener());
		context.getServletContext().getSessionCookieConfig().setHttpOnly(true);
		context.addServlet(PlayersServlet.class, "/players");
		context.addServlet(SkirmishServlet.class, "/skirmishes/*");
		
		DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(server);
		HouseKeeper houseKeeper = new HouseKeeper();
		houseKeeper.setIntervalSec(60);
		sessionIdManager.setSessionHouseKeeper(houseKeeper);
		server.setSessionIdManager(sessionIdManager);
		
		FilterHolder cors = context.addFilter(CrossOriginFilter.class, "/*", null);
		cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,HEAD,OPTIONS");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
		
		server.addConnector(connector);
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.setRequestLog(createRequestLog());
		return server;
	}
	
}
