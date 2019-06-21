package com.snailscuffle.game;

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
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsServlet;
import com.snailscuffle.game.battle.BattleServlet;
import com.snailscuffle.game.blockchain.BlockchainInterpreter;
import com.snailscuffle.game.info.InfoServlet;
import com.snailscuffle.game.tx.TransactionsServlet;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			LoggingUtil.initLogback(Main.class);
			GameSettings settings = new GameSettings("config.properties");
			Server server = configureJettyServer(settings);
			server.start();
			server.join();
		} catch(Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	private static Server configureJettyServer(GameSettings settings) throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler context = new ServletContextHandler();
		context.setBaseResource(Resource.newResource(findWebRoot()));
		context.addServlet(DefaultServlet.class, "/").setInitParameter("precompressed", "true");
		context.addServlet(BattleServlet.class, "/battle");
		context.addServlet(new ServletHolder(new InfoServlet(settings)), "/info/*");
		
		if (settings.ignisArchivalNodeUrl == null) {
			String baseUrl = settings.delegateGameServerUrl.toString();
			context.addServlet(new ServletHolder(new RedirectServlet(baseUrl + "/accounts")), "/accounts/*");
			context.addServlet(new ServletHolder(new RedirectServlet(baseUrl + "/transactions")), "/transactions/*");
		} else {
			Accounts accounts = new Accounts();
			BlockchainInterpreter blockchainInterpreter = new BlockchainInterpreter(settings.ignisArchivalNodeUrl, accounts);
			context.addServlet(new ServletHolder(new AccountsServlet(accounts)), "/accounts/*");
			context.addServlet(new ServletHolder(new TransactionsServlet(blockchainInterpreter)), "/transactions/*");
		}
		
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
