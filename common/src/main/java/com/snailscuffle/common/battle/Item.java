package com.snailscuffle.common.battle;

public enum Item {
	ATTACK,
	DEFENSE,
	SPEED;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
