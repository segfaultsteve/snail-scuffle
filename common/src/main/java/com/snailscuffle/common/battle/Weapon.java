package com.snailscuffle.common.battle;

public enum Weapon {
	RIFLE,
	ROCKET,
	LASER;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
