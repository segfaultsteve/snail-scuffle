package com.snailscuffle.common.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import com.snailscuffle.common.InvalidConfigurationException;

public class ConfigUtil {
	
	public static URL getUrl(Properties properties, String propertyName) throws InvalidConfigurationException {
		String urlString = properties.getProperty(propertyName);
		try {
			if (urlString == null) {
				return null;
			} else if (urlString.endsWith("/")) {
				urlString = urlString.substring(0, urlString.length() - 1);
			}
			return new URL(urlString);
		} catch (MalformedURLException e) {
			String error = "Invalid URL specified for " + propertyName;
			throw new InvalidConfigurationException(error);
		}
	}
	
}
