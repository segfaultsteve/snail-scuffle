package com.snailscuffle.common.battle;

public enum Snail {
	DALE,
	GAIL,
	TODD,
	DOUG;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
