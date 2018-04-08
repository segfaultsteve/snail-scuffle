package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Shell {
	ALUMINUM (0, ALUMINUM_DEFENSE, ALUMINUM_SPEED),
	STEEL (0, STEEL_DEFENSE, STEEL_SPEED),
	NONE (0, 0, 0);
	
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Shell(int attack, int defense, int speed) {
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
