package com.snailscuffle.common.battle;

public enum Stat {
	HP,
	AP;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
