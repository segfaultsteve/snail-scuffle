package com.snailscuffle.game;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.util.InvalidConfigurationException;

public class GameSettings {
	
	public final int port;
	public final URI matchmakerUrl;
	public final URI apiUrl;
	
	private static final String CONFIG_FILE = "/config.properties";
	private static final Logger logger = LoggerFactory.getLogger(GameSettings.class);
	
	public GameSettings() throws InvalidConfigurationException {
		Properties config = new Properties();
		try (InputStream configFile = this.getClass().getResourceAsStream(CONFIG_FILE)) {
			config.load(configFile);
		} catch (Exception e) {
			String error = "Could not load config file";
			logger.error(error, e);
			throw new InvalidConfigurationException(error);
		}
		
		port = Integer.parseInt(config.getProperty("port", "80"));
		logger.debug("port = {}", port);
		
		matchmakerUrl = getUri(config, "matchmakerUrl");
		logger.debug("matchmakerUrl = {}", matchmakerUrl);
		
		apiUrl = getUri(config, "apiUrl");
		logger.debug("apiUrl = {}", apiUrl);
	}
	
	private static URI getUri(Properties properties, String propertyName) throws InvalidConfigurationException {
		String uriString = properties.getProperty(propertyName);
		try {
			if (uriString.endsWith("/")) {
				uriString = uriString.substring(0, uriString.length() - 1);
			}
			return new URI(uriString);
		} catch (Exception e) {
			String error = "Invalid URL specified for " + propertyName;
			logger.error(error);
			throw new InvalidConfigurationException(error);
		}
	}

}
