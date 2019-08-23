package com.snailscuffle.game.tx;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

public class UnsignedTransaction implements Serializable {
	
	public JsonNode asJson;
	public String asHex;
	
	@SuppressWarnings("unused")
	private UnsignedTransaction() {}	// needed for deserialization via jackson
	
	public UnsignedTransaction(JsonNode txJson, String txBytes) {
		asJson = txJson;
		asHex = txBytes;
	}
	
}
