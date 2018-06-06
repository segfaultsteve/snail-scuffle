package com.snailscuffle.game.battle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EquipmentInfo implements Serializable {
	
	public String name;
	public String displayName;
	public String description;
	public int attackModifier;
	public int defenseModifier;
	public int speedModifier;
	public Map<String, Object> other = new HashMap<>();
	
	public EquipmentInfo(String name, String displayName, String description, int attackModifier, int defenseModifier, int speedModifier) {
		this.name = name;
		this.displayName = displayName;
		this.description = description;
		this.attackModifier = attackModifier;
		this.defenseModifier = defenseModifier;
		this.speedModifier = speedModifier;
	}
	
}
