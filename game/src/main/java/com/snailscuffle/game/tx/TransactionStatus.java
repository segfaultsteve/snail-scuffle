package com.snailscuffle.game.tx;

import java.io.Serializable;

public class TransactionStatus implements Serializable {
	
	public String txid;
	public boolean confirmed;
	public int confirmations;
	
	@SuppressWarnings("unused")
	private TransactionStatus() {}		// needed for deserialization via jackson
	
	public TransactionStatus(String txid, boolean confirmed, int confirmations) {
		this.txid = txid;
		this.confirmed = confirmed;
		this.confirmations = confirmations;
	}
	
}
