package com.snailscuffle.common.battle;

import java.io.Serializable;

// describes a rule of the form: "use item when [I have/enemy has] [HP/AP] [>=/<=] [threshold]"
// e.g., "use attack boost when I have HP <= 40" or "use defense boost when enemy has AP > 30"
public class HasCondition implements Serializable {
	
	public Player player;
	public Stat stat;
	public Inequality inequality;
	public int threshold;
	
	@SuppressWarnings("unused")
	private HasCondition() {}		// needed for serialization via jackson-jr
	
	public HasCondition(Player player, Stat stat, Inequality inequality, int threshold) {
		this.player = player;
		this.stat = stat;
		this.inequality = inequality;
		this.threshold = threshold;
	}

	public void validate() {
		if (player == null || stat == null || inequality == null) {
			String missingField = "inequality";
			if (player == null) missingField = "player";
			else if (stat == null) missingField = "stat";
			
			throw new InvalidBattleException("condition is missing " + missingField);
		}
	}

}
