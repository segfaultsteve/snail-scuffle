package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Weapon {
	RIFLE (RIFLE_DISPLAY_NAME, RIFLE_ATTACK, 0, 0, RIFLE_AP_COST),
	ROCKET (ROCKET_DISPLAY_NAME, ROCKET_ATTACK, 0, 0, ROCKET_AP_COST),
	LASER (LASER_DISPLAY_NAME, LASER_ATTACK, 0, 0, LASER_AP_COST);
	
	public final String displayName;
	public final int attack;
	public final int defense;
	public final int speed;
	public final int apCost;
	
	private Weapon(String displayName, int attack, int defense, int speed, int apCost) {
		this.displayName = displayName;
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
		this.apCost = apCost;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
