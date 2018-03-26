package com.snailscuffle.game;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		GameSettings settings = new GameSettings();
		Server server = configureJettyServer(settings);
		try {
			server.start();
			server.join();
		} catch(Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	private static Server configureJettyServer(GameSettings settings) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler handler = new ServletContextHandler();
		handler.addServlet(BattleServlet.class, "/battle");
		
		HandlerList handlers = new HandlerList();
		handlers.addHandler(handler);
		
		NCSARequestLog requestLog = new NCSARequestLog("yyyy_mm_dd.request.log");
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLog.setLogLatency(true);
		requestLog.setRetainDays(90);
		
		server.addConnector(connector);
		server.setHandler(handlers);
		server.setStopAtShutdown(true);
		server.setRequestLog(requestLog);
		return server;
	}
	
}
