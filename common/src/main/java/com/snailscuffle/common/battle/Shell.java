package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Shell {
	ALUMINUM (ALUMINUM_DISPLAY_NAME, 0, ALUMINUM_DEFENSE, ALUMINUM_SPEED),
	STEEL (STEEL_DISPLAY_NAME, 0, STEEL_DEFENSE, STEEL_SPEED),
	NONE (NO_SHELL_DISPLAY_NAME, 0, 0, 0);
	
	public final String displayName;
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Shell(String displayName, int attack, int defense, int speed) {
		this.displayName = displayName;
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
