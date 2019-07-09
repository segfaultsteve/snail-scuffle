package com.snailscuffle.game;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

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
import com.snailscuffle.game.blockchain.BlockchainSubsystem;
import com.snailscuffle.game.info.InfoServlet;
import com.snailscuffle.game.tx.TransactionsServlet;

public class Main {
	
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		BlockchainSubsystem blockchainSubsystem = null;
		try {
			LoggingUtil.initLogback(Main.class);
			GameSettings settings = new GameSettings("config.properties");
			
			if (settings.ignisArchivalNodeUrl != null) {
				createAccountsDatabaseDirectory(settings.databasePath);
				Accounts accounts = new Accounts(settings.databasePath.toString(), Constants.MAX_SNAPSHOT_COUNT);
				blockchainSubsystem = new BlockchainSubsystem(settings.ignisArchivalNodeUrl, accounts);
			}
			
			Server server = configureJettyServer(settings, blockchainSubsystem);
			server.start();
			server.join();
		} catch(Exception e) {
			logger.error("Fatal error", e);
		} finally {
			if (blockchainSubsystem != null) {
				blockchainSubsystem.close();
			}
		}
	}
	
	private static Server configureJettyServer(GameSettings settings, BlockchainSubsystem blockchainSubsystem) throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(settings.port);
		connector.setReuseAddress(true);
		
		ServletContextHandler context = new ServletContextHandler();
		context.setBaseResource(Resource.newResource(findWebRoot()));
		context.addServlet(DefaultServlet.class, "/").setInitParameter("precompressed", "true");
		context.addServlet(BattleServlet.class, "/battle");
		context.addServlet(new ServletHolder(new InfoServlet(settings)), "/info/*");
		
		if (blockchainSubsystem == null) {
			String redirectBaseUrl = settings.delegateGameServerUrl.toString();
			context.addServlet(new ServletHolder(new RedirectServlet(redirectBaseUrl + "/accounts")), "/accounts/*");
			context.addServlet(new ServletHolder(new RedirectServlet(redirectBaseUrl + "/transactions")), "/transactions/*");
		} else {
			context.addServlet(new ServletHolder(new AccountsServlet(blockchainSubsystem)), "/accounts/*");
			context.addServlet(new ServletHolder(new TransactionsServlet(blockchainSubsystem)), "/transactions/*");
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
	
	private static void createAccountsDatabaseDirectory(Path dbPath) throws IOException {
		File dbDir = new File(dbPath.getParent().toString());
		dbDir.mkdirs();
		if (!dbDir.exists()) {
			throw new IOException("Could not create database directory '" + dbDir.getAbsolutePath() + "'");
		}
	}
	
}
