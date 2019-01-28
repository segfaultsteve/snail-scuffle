package com.snailscuffle.common.battle;

public enum Action {
	ATTACK,
	USE_ITEM,
	ITEM_DONE,
	RESUSCITATE;
	
	@Override
	public String toString() {
		return this.name().toLowerCase();	// serialize as lowercase
	}
}
