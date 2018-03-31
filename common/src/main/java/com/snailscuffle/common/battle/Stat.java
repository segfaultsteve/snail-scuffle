package com.snailscuffle.common.battle;

public enum Stat {
	HP,
	AP,
	ATTACK,
	DEFENSE,
	SPEED;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
