package com.snailscuffle.common.battle;

public enum Shell {
	ALUMINUM,
	STEEL,
	NONE;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
