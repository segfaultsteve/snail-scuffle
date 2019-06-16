package com.snailscuffle.game;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.util.LoggingUtil;
import com.snailscuffle.game.battle.BattleServlet;
import com.snailscuffle.game.info.InfoServlet;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			LoggingUtil.initLogback(Main.class);
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
		context.addServlet(DefaultServlet.class, "/").setInitParameter("precompressed", "true");
		context.addServlet(BattleServlet.class, "/battle");
		context.addServlet(new ServletHolder(new InfoServlet(settings)), "/info/*");
		
		server.addConnector(connector);
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.setRequestLog(LoggingUtil.createRequestLog());
		return server;
	}
	
	private static URI findWebRoot() throws URISyntaxException {
		URL webRootLocation = Main.class.getResource("/webroot/index.html");
		return URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
	}
	
}
