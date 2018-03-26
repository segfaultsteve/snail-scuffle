package com.snailscuffle.common;

import java.io.IOException;
import java.io.Serializable;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSON;

public class ErrorResponse implements Serializable {
	
	private int errorCode;
	private String errorDescription;
	private String errorDetails;
	
	public static ErrorResponse battleConfigSerializationError() { return new ErrorResponse(101, "Error serializing battle config"); }
	public static ErrorResponse battleConfigDeserializationError() { return new ErrorResponse(102, "Error deserializing battle config"); }
	public static ErrorResponse invalidBattleConfig() { return new ErrorResponse(103, "Battle config is invalid"); }
	
	private ErrorResponse(int code, String description) {
		errorCode = code;
		errorDescription = description;
	}
	
	public ErrorResponse because(String details) {
		this.errorDetails = details;
		return this;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public String getErrorDescription() {
		return errorDescription;
	}
	
	public String getErrorDetails() {
		return errorDetails;
	}
	
	@Override
	public String toString() {
		String json = "{}";
		try {
			json = JSON.std.asString(this);
		} catch (IOException e) {
			assert(false);
			LoggerFactory.getLogger(ErrorResponse.class).error("Failed to serialize error message", e);
		}
		return json;
	}
}
