package com.snailscuffle.common.battle;

public enum Stat {
	HP,
	AP,
	ATTACK,
	DEFENSE,
	SPEED,
	NONE;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
