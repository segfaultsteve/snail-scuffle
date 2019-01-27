package com.snailscuffle.common.battle;

import static com.snailscuffle.common.battle.Constants.*;

public enum Item {
	ATTACK (ATTACK_BOOST_DISPLAY_NAME, ATTACK_BOOST_DESCRIPTION),
	DEFENSE (DEFENSE_BOOST_DISPLAY_NAME, DEFENSE_BOOST_DESCRIPTION),
	SPEED (SPEED_BOOST_DISPLAY_NAME, SPEED_BOOST_DESCRIPTION),
	HEAL (HEAL_DISPLAY_NAME, HEAL_DESCRIPTION),
	STUN (STUN_DISPLAY_NAME, STUN_DESCRIPTION),
	NONE (NO_ITEM_DISPLAY_NAME, "");
	
	public final String displayName;
	public final String description;
	
	private Item(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
} 
