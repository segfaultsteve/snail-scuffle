package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleResult implements Serializable {
	
	public List<List<BattleEvent>> eventsByRound;
	public int winnerIndex;		// 0 or 1; see BattleEvent for convention
	
	@SuppressWarnings("unused")
	private BattleResult() {}	// needed for serialization via jackson-jr
	
	public BattleResult(List<List<BattleEvent>> eventsByRound, int winnerIndex) {
		this.eventsByRound = eventsByRound;
		this.winnerIndex = winnerIndex;
	}
	
	public void validate() {
		if (eventsByRound == null || eventsByRound.size() == 0) {
			throw new InvalidBattleException("Battle result not found");
		}
		
		for (List<BattleEvent> events : eventsByRound) {
			events.forEach(e -> e.validate());
		}
		
		if (winnerIndex < 0 || winnerIndex > 1) {
			throw new InvalidBattleException("Invalid winner index; must be 0 (player 1) or 1 (player 2)");
		}
	}
	
	public List<BattleEvent> flattenEvents() {
		List<BattleEvent> flat = new ArrayList<>();
		for (List<BattleEvent> round : eventsByRound) {
			flat.addAll(round);
		}
		return flat;
	}
	
}
