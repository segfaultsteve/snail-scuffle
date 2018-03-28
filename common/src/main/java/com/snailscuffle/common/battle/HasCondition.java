package com.snailscuffle.common.battle;

import java.io.Serializable;

// describes a rule of the form: "use item when [I have/enemy has] [HP/AP] [>=/<=] [threshold]"
// e.g., "use attack boost when I have HP <= 40" or "use defense boost when enemy has AP > 30"
public class HasCondition implements Serializable {
	
	public Player player;
	public Stat stat;
	public Inequality inequality;
	public int threshold;
	
	public void validate() {
		if (player == null || stat == null || inequality == null) {
			String missingField = "inequality";
			if (player == null) missingField = "player";
			else if (stat == null) missingField = "stat";
			
			throw new InvalidBattleException("condition is missing " + missingField);
		}
	}

}
