package com.snailscuffle.common.battle;

public enum Accessory {
	NONE;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
