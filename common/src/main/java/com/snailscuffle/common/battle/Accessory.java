package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Accessory {
	STEROIDS (STEROIDS_DISPLAY_NAME, STEROIDS_DESCRIPTION, STEROIDS_ATTACK, 0, 0),			// attack boost
	SNAIL_MAIL (SNAIL_MAIL_DISPLAY_NAME, SNAIL_MAIL_DESCRIPTION, 0, SNAIL_MAIL_DEFENSE, 0),	// defense boost
	CAFFEINE (CAFFEINE_DISPLAY_NAME, CAFFEINE_DESCRIPTION, 0, 0, CAFFEINE_SPEED),			// speed boost
	CHARGED_ATTACK (CHARGED_ATTACK_DISPLAY_NAME, CHARGED_ATTACK_DESCRIPTION, 0, 0, 0),		// attack increases as AP increases
	ADRENALINE (ADRENALINE_DISPLAY_NAME, ADRENALINE_DESCRIPTION, 0, 0, 0),					// attack increases as HP decreases
	SALTED_SHELL (SALTED_SHELL_DISPLAY_NAME, SALTED_SHELL_DESCRIPTION, 0, 0, 0),			// reduces defense in first period; large attack boost in subsequent period(s)
	THORNS (THORNS_DISPLAY_NAME, THORNS_DESCRIPTION, 0, 0, 0),								// enemy takes light damage every time he attacks
	DEFIBRILLATOR (DEFIBRILLATOR_DISPLAY_NAME, DEFIBRILLATOR_DESCRIPTION, 0, 0, 0),			// mortal blow leaves 1 HP remaining
	NONE (NO_ACCESSORY_DISPLAY_NAME, "", 0, 0, 0);
	
	public final String displayName;
	public final String description;
	public final int attack;
	public final int defense;
	public final int speed;
	
	private Accessory(String displayName, String description, int attack, int defense, int speed) {
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
