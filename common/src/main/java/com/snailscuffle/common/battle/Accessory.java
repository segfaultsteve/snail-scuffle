package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Accessory {
	STEROIDS (STEROIDS_ATTACK, 0, 0),		// attack boost
	SNAIL_MAIL (0, SNAIL_MAIL_DEFENSE, 0),	// defense boost
	CAFFEINE (0, 0, CAFFEINE_SPEED),		// speed boost
	CHARGED_ATTACK (0, 0, 0),				// attack increases as AP increases
	ADRENALINE (0, 0, 0),					// attack increases as HP decreases
	SALTED_SHELL (0, 0, 0),					// reduces defense in first half; large attack boost in second half
	THORNS (0, 0, 0),						// enemy takes light damage every time he attacks
	DEFIBRILLATOR (0, 0, 0),				// mortal blow leaves 1 HP remaining
	NONE (0, 0, 0);
	
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Accessory(int attack, int defense, int speed) {
		this.attack = attack;
		this.defense = defense;
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
