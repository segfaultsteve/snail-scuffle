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
	private HasCondition() {}		// needed for serialization via jackson
	
	public HasCondition(HasCondition other) {
		player = other.player;
		stat = other.stat;
		inequality = other.inequality;
		threshold = other.threshold;
	}
	
	public HasCondition(Player player, Stat stat, Inequality inequality, int threshold) {
		this.player = player;
		this.stat = stat;
		this.inequality = inequality;
		this.threshold = threshold;
	}

	public void validate() {
		if (stat == null || inequality == null) {	// note: player can be null when this condition is used in a wait instruction
			String missingField = "stat";
			if (inequality == null) {
				missingField = "inequality";
			}
			
			throw new InvalidBattleException("Condition is missing " + missingField);
		}
		
		if (stat != Stat.HP && stat != Stat.AP) {
			throw new InvalidBattleException("Invalid trigger condition; must trigger on HP or AP");
		}
	}

}
