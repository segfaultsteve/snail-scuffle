package com.snailscuffle.game.accounts;

public class AccountsSnapshot {
	
	public final String name;
	public final long height;
	public final long blockId;
	
	public AccountsSnapshot(String name, long height, long blockId) {
		this.name = name;
		this.height = height;
		this.blockId = blockId;
	}
	
}
