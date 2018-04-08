package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Weapon {
	RIFLE (RIFLE_ATTACK, 0, 0, RIFLE_AP_COST),
	ROCKET (ROCKET_ATTACK, 0, 0, ROCKET_AP_COST),
	LASER (LASER_ATTACK, 0, 0, LASER_AP_COST);
	
	public int attack;
	public int defense;
	public int speed;
	public int apCost;
	
	private Weapon(int attack, int defense, int speed, int apCost) {
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
