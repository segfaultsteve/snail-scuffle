package com.snailscuffle.common.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.server.NCSARequestLog;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LoggingUtil {
	
	public static <T> void initLogback(Class<T> classInJar) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		try (InputStream configStream = ResourceUtil.getConfigFile("logback.xml", classInJar)) {
			configurator.setContext(loggerContext);
			configurator.doConfigure(configStream);
		} catch (IOException | JoranException e) {
			e.printStackTrace();
		}
	}
	
	public static NCSARequestLog createRequestLog() {
		NCSARequestLog requestLog = new NCSARequestLog("yyyy_mm_dd.request.log");
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLog.setLogLatency(true);
		requestLog.setRetainDays(90);
		return requestLog;
	}
	
}
