package com.snailscuffle.game.tx;

import java.io.Serializable;

public class TransactionStatus implements Serializable {
	
	public String fullHash;
	public boolean confirmed;
	public int confirmations;
	
	@SuppressWarnings("unused")
	private TransactionStatus() {}		// needed for deserialization via jackson
	
	public TransactionStatus(String fullHash, boolean confirmed, int confirmations) {
		this.fullHash = fullHash;
		this.confirmed = confirmed;
		this.confirmations = confirmations;
	}
	
}
