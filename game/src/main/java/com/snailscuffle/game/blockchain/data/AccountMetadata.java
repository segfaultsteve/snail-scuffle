package com.snailscuffle.game.blockchain.data;

public class AccountMetadata {
	
	public final long id;
	public final String username;
	public final String publicKey;
	
	public AccountMetadata(long id, String username, String publicKey) {
		this.id = id;
		this.username = username;
		this.publicKey = publicKey;
	}
	
}
