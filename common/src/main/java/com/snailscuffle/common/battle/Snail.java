package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Snail {
	DALE (DALE_DISPLAY_NAME, DALE_DESCRIPTION, DALE_ATTACK, DALE_DEFENSE, DALE_SPEED),
	GAIL (GAIL_DISPLAY_NAME, GAIL_DESCRIPTION, GAIL_ATTACK, GAIL_DEFENSE, GAIL_SPEED),
	TODD (TODD_DISPLAY_NAME, TODD_DESCRIPTION, TODD_ATTACK, TODD_DEFENSE, TODD_SPEED),
	DOUG (DOUG_DISPLAY_NAME, DOUG_DESCRIPTION, DOUG_ATTACK, DOUG_DEFENSE, DOUG_SPEED);
	
	public final String displayName;
	public final String description;
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Snail(String displayName, String description, int attack, int defense, int speed) {
		this.displayName = displayName;
		this.description = description;
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
