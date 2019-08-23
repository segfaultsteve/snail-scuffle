package com.snailscuffle.game.tx;

import java.io.Serializable;

public class UnsignedTransaction implements Serializable {
	
	public String asJson;
	public String asHex;
	
	@SuppressWarnings("unused")
	private UnsignedTransaction() {}	// needed for deserialization via jackson
	
	public UnsignedTransaction(String txJson, String txBytes) {
		asJson = txJson;
		asHex = txBytes;
	}
	
}
