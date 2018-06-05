package com.snailscuffle.game;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			GameSettings settings = new GameSettings();
			Server server = configureJettyServer(settings);
			server.start();
			server.join();
		} catch(Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	private static Server configureJettyServer(GameSettings settings) throws MalformedURLException, URISyntaxException {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler context = new ServletContextHandler();
		context.setBaseResource(Resource.newResource(findWebRoot()));
		context.addServlet(DefaultServlet.class, "/");
		context.addServlet(BattleServlet.class, "/battle");
		
		server.addConnector(connector);
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.setRequestLog(createRequestLog());
		return server;
	}
	
	private static URI findWebRoot() throws URISyntaxException {
		URL webRootLocation = Main.class.getClass().getResource("/webroot/index.html");
		return URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
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
