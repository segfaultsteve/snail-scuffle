package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.List;

public class BattleResult implements Serializable {
	
	public List<BattleEvent> sequenceOfEvents;
	
	public void validate() {
		if (sequenceOfEvents == null || sequenceOfEvents.size() == 0) {
			throw new InvalidBattleException("Battle result not found");
		}
		sequenceOfEvents.forEach(e -> e.validate());
	}
	
}
