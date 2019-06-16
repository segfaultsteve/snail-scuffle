package com.snailscuffle.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class ResourceUtil {
	
	public static final String EXTERNAL_DIR_PREFIX = "/../config/";
	public static final String JAR_PREFIX = "/config/";
	
	public static <T> InputStream getConfigFile(String filename, Class<T> classInJar) {
		InputStream configFile = getFileRelativeToJar(EXTERNAL_DIR_PREFIX + filename, classInJar);
		if (configFile == null) {
			configFile = getFileFromWithinJar(JAR_PREFIX + filename, classInJar);
		}
		return configFile;
	}
	
	public static <T> InputStream getFileRelativeToJar(String relativePath, Class<T> classInJar) {
		try {
			File jarFolder = new File(classInJar.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			return new FileInputStream(jarFolder.getPath() + relativePath);
		} catch (URISyntaxException | FileNotFoundException e) {
			return null;
		}
	}
	
	public static <T> InputStream getFileFromWithinJar(String pathFromJarRoot, Class<T> classInJar) {
		return classInJar.getResourceAsStream(pathFromJarRoot);
	}
	
}
