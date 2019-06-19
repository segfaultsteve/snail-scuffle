package com.snailscuffle.game;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.InvalidConfigurationException;
import com.snailscuffle.common.util.ConfigUtil;
import com.snailscuffle.common.util.ResourceUtil;

public class GameSettings {
	
	public final int port;
	public final URL matchmakerUrl;
	public final URL delegateGameServerUrl;		// must be specified if ignisArchivalNodeUrl is not
	public final URL ignisArchivalNodeUrl;		// must be specified if delegateGameServerUrl is not
	
	private static final Logger logger = LoggerFactory.getLogger(GameSettings.class);
	
	public GameSettings(String filename) throws InvalidConfigurationException {
		Properties config = new Properties();
		try (InputStream configFile = ResourceUtil.getConfigFile(filename, this.getClass())) {
			config.load(configFile);
		} catch (IOException e) {
			String error = "Could not load config file";
			logger.error(error, e);
			throw new InvalidConfigurationException(error);
		}
		
		port = Integer.parseInt(config.getProperty("port", "80"));
		matchmakerUrl = ConfigUtil.getUrl(config, "matchmakerUrl");
		delegateGameServerUrl = ConfigUtil.getUrl(config, "delegateGameServerUrl");
		ignisArchivalNodeUrl = ConfigUtil.getUrl(config, "ignisArchivalNodeUrl");
		
		if (matchmakerUrl == null) {
			throw new InvalidConfigurationException("matchmakerUrl must be specified in " + filename);
		} else if (delegateGameServerUrl == null && ignisArchivalNodeUrl == null) {
			throw new InvalidConfigurationException("either delegateGameServerUrl or ignisArchivalNodeUrl must be specified in " + filename);
		}
		
		logger.debug("port = {}", port);
		logger.debug("matchmakerUrl = {}", matchmakerUrl);
		logger.debug("delegateGameServerUrl = {}", delegateGameServerUrl);
		logger.debug("ignisArchivalNodeUrl = {}", ignisArchivalNodeUrl);
	}

}
