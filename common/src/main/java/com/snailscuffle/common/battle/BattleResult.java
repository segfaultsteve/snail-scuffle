package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.List;

public class BattleResult implements Serializable {
	
	public List<BattleEvent> sequenceOfEvents;
	public int winnerIndex;		// 0 or 1; see BattleEvent for convention
	
	public void validate() {
		if (sequenceOfEvents == null || sequenceOfEvents.size() == 0) {
			throw new InvalidBattleException("Battle result not found");
		}
		sequenceOfEvents.forEach(e -> e.validate());
		
		if (winnerIndex < 0 || winnerIndex > 1) {
			throw new InvalidBattleException("Invalid winner index; must be 0 (player 1) or 1 (player 2)");
		}
	}
	
}
