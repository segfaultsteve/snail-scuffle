package com.snailscuffle.common.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {
	
	public static String serialize(Serializable data) {
		try {
			return new ObjectMapper()
					.setSerializationInclusion(Include.NON_NULL)
					.writer()
					.with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
					.writeValueAsString(data);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected serialization error");	// this generally shouldn't happen
		}
	}
	
	public static void serialize(Serializable data, PrintWriter writer) {
		writer.print(serialize(data));
	}
	
	public static <T> T deserialize(Class<T> type, String json) throws IOException {
		return new ObjectMapper()
				.readerFor(type)
				.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
				.readValue(json);
	}
	
	public static JsonNode deserialize(String json) throws IOException {
		return new ObjectMapper().readTree(json);
	}

}
