package com.snailscuffle.common;

import java.io.Serializable;

import com.snailscuffle.common.util.JsonUtil;

public class ErrorResponse implements Serializable {
	
	public int errorCode;
	public String errorDescription;
	public String errorDetails;
	
	// game server
	public static ErrorResponse battleConfigDeserializationError() { return new ErrorResponse(101, "Error deserializing battle config"); }
	public static ErrorResponse invalidBattleConfig() { return new ErrorResponse(102, "Battle config is invalid"); }
	public static ErrorResponse failedToRetrieveAccount() { return new ErrorResponse(111, "Failed to retrieve account"); }
	
	// matchmaker server
	public static ErrorResponse playerDeserializationError() { return new ErrorResponse(201, "Error deserializing player"); }
	public static ErrorResponse invalidPlayer() { return new ErrorResponse(202, "Player is invalid"); }
	public static ErrorResponse notAuthorized() { return new ErrorResponse(211, "Not authorized"); }
	public static ErrorResponse battlePlanDeserializationError() { return new ErrorResponse(212, "Error deserializing battle plan"); }
	public static ErrorResponse invalidBattlePlan() { return new ErrorResponse(213, "Battle plan is invalid"); }
	
	// general
	public static ErrorResponse invalidQuery() { return new ErrorResponse(900, "Query is invalid"); }
	public static ErrorResponse unexpectedError() { return new ErrorResponse(999, "Unexpected error"); }
	
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
		return JsonUtil.serialize(this);
	}
}
