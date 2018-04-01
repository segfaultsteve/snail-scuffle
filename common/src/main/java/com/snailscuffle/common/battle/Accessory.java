package com.snailscuffle.common.battle;

public enum Accessory {
	STEROIDS,			// attack boost
	SNAIL_MAIL,			// defense boost
	CAFFEINE,			// speed boost
	CHARGED_ATTACK,		// attack increases as AP increases
	ADRENALINE,			// attack increases as HP decreases
	THORNS,				// enemy takes damage every time he attacks
	SALTED_SHELL,		// reduces defense in first half; large attack boost in second half
	DEFIBRILLATOR;		// mortal blow leaves 1 HP remaining
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
