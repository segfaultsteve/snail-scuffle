package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Snail {
	DALE (DALE_ATTACK, DALE_DEFENSE, DALE_SPEED),
	GAIL (GAIL_ATTACK, GAIL_DEFENSE, GAIL_SPEED),
	TODD (TODD_ATTACK, TODD_DEFENSE, TODD_SPEED),
	DOUG (DOUG_ATTACK, DOUG_DEFENSE, DOUG_SPEED);
	
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Snail(int attack, int defense, int speed) {
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
