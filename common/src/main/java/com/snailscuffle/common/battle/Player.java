package com.snailscuffle.common.battle;

public enum Player {
	ME,
	ENEMY;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
