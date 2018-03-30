package com.snailscuffle.common;

import java.io.IOException;
import java.io.Serializable;

import org.slf4j.LoggerFactory;

import com.snailscuffle.common.util.JsonUtil;

public class ErrorResponse implements Serializable {
	
	public int errorCode;
	public String errorDescription;
	public String errorDetails;
	
	public static ErrorResponse battleConfigSerializationError() { return new ErrorResponse(101, "Error serializing battle config"); }
	public static ErrorResponse battleConfigDeserializationError() { return new ErrorResponse(102, "Error deserializing battle config"); }
	public static ErrorResponse invalidBattleConfig() { return new ErrorResponse(103, "Battle config is invalid"); }
	
	private ErrorResponse() {}
	
	private ErrorResponse(int code, String description) {
		errorCode = code;
		errorDescription = description;
	}
	
	public ErrorResponse because(String details) {
		this.errorDetails = details;
		return this;
	}
	
	@Override
	public String toString() {
		String json = "{}";
		try {
			json = JsonUtil.serialize(this);
		} catch (IOException e) {
			assert(false);
			LoggerFactory.getLogger(ErrorResponse.class).error("Failed to serialize error message", e);
		}
		return json;
	}
}
