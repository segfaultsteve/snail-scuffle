package com.snailscuffle.common.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

public class JsonUtil {
	
	public static String serialize(Serializable data) throws IOException {
		return JSON.std
				.with(Feature.FAIL_ON_UNKNOWN_TYPE_WRITE)
				.with(Feature.USE_FIELDS)
				.asString(data);
	}
	
	public static void serialize(Serializable data, PrintWriter writer) throws IOException {
		writer.print(serialize(data));
	}
	
	public static <T> T deserialize(Class<T> type, String json) throws IOException {
		return JSON.std
				.with(Feature.FAIL_ON_UNKNOWN_BEAN_PROPERTY)
				.with(Feature.USE_FIELDS)
				.beanFrom(type, json);
	}

}
