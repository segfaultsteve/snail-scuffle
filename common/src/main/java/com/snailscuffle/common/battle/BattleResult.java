package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleResult implements Serializable {
	
	public List<List<BattleEvent>> eventsByRound;
	public List<BattleSnapshot> endOfRoundStats;
	public int winnerIndex;		// 0 or 1; see BattleEvent for convention
	
	@SuppressWarnings("unused")
	private BattleResult() {}	// needed for serialization via jackson
	
	public BattleResult(List<List<BattleEvent>> eventsByRound, List<BattleSnapshot> endOfRoundStats, int winnerIndex) {
		this.eventsByRound = eventsByRound;
		this.endOfRoundStats = endOfRoundStats;
		this.winnerIndex = winnerIndex;
	}
	
	public void validate() {
		if (eventsByRound == null || eventsByRound.size() == 0) {
			throw new InvalidBattleException("Battle result not found");
		}
		
		for (List<BattleEvent> events : eventsByRound) {
			events.forEach(e -> e.validate());
		}
		
		if (endOfRoundStats == null || endOfRoundStats.size() == 0) {
			throw new InvalidBattleException("End-of-round stats not found");
		}
		
		endOfRoundStats.forEach(s -> s.validate());
		
		if (eventsByRound.size() != endOfRoundStats.size()) {
			throw new InvalidBattleException("Expected exactly one end-of-round summary for each round");
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
