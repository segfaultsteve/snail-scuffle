package com.snailscuffle.match;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchmakerSettings {
	
	public final int port;
	
	private static final String CONFIG_FILE = "/config.properties";
	private static final Logger logger = LoggerFactory.getLogger(MatchmakerSettings.class);
	
	public MatchmakerSettings() {
		Properties config = new Properties();
		try (InputStream configFile = this.getClass().getResourceAsStream(CONFIG_FILE)) {
			config.load(configFile);
		} catch (Exception e) {
			logger.error("Could not load config file", e);
		}
		
		port = Integer.parseInt(config.getProperty("port", "80"));
		logger.debug("port = {}", port);
	}

}
