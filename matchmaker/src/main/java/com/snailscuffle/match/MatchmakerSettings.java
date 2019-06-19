package com.snailscuffle.match;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.InvalidConfigurationException;
import com.snailscuffle.common.util.ResourceUtil;

public class MatchmakerSettings {
	
	public final int port;
	
	private static final Logger logger = LoggerFactory.getLogger(MatchmakerSettings.class);
	
	public MatchmakerSettings(String filename) throws InvalidConfigurationException {
		Properties config = new Properties();
		try (InputStream configFile = ResourceUtil.getConfigFile(filename, this.getClass())) {
			config.load(configFile);
		} catch (IOException e) {
			String error = "Could not load config file";
			logger.error(error, e);
			throw new InvalidConfigurationException(error);
		}
		
		port = Integer.parseInt(config.getProperty("port", "80"));
		logger.debug("port = {}", port);
	}

}
