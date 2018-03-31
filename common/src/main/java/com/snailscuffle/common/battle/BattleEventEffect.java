package com.snailscuffle.common.battle;

import java.io.Serializable;

public class BattleEventEffect implements Serializable {

	public int playerIndex;
	public Stat stat;
	public double change;
	
	public void validate() {
		if (playerIndex < 0 || playerIndex > 1) {
			throw new InvalidBattleException("Invalid player index; must be 0 (player 1) or 1 (player 2)");
		}
		
		if (stat == null) {
			throw new InvalidBattleException("Battle event effect is missing stat");
		}
		
		if (change == 0) {
			throw new InvalidBattleException("Battle event effect has zero magnitude");
		}
	}
	
}
