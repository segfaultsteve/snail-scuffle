package com.snailscuffle.simulator;

import java.io.FileInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorSettings {
	
	public final String databaseFilePath;
	public final String tableName;
	
	private static final String CONFIG_FILE = "src/main/resources/config.properties";
	private static final Logger logger = LoggerFactory.getLogger(SimulatorSettings.class);
	
	public SimulatorSettings() {
		Properties config = new Properties();
		try (FileInputStream configFile = new FileInputStream(CONFIG_FILE)) {
			config.load(configFile);
		} catch (Exception e) {
			logger.error("Could not load config file", e);
		}
		
		databaseFilePath = config.getProperty("databaseFilePath");		
		logger.debug("databaseFilePath = {}", databaseFilePath);
		
		tableName = config.getProperty("tableName");
		logger.debug("tableName = {}", tableName);
	}

}
