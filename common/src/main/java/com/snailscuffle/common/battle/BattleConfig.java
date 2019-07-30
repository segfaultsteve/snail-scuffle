package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleConfig implements Serializable {
	
	public List<BattlePlan> battlePlans;
	public int firstMover;			// 0 refers to the player who submitted the first battle plan in battlePlans; 1 refers to the other player
	
	@SuppressWarnings("unused")
	private BattleConfig() {}		// needed for serialization via jackson
	
	public BattleConfig(List<BattlePlan> battlePlans, int firstMover) {
		this.battlePlans = new ArrayList<>(battlePlans);
		this.firstMover = firstMover;
	}
	
	public void validate() throws InvalidBattleException {
		if (battlePlans == null || battlePlans.isEmpty()) {
			throw new InvalidBattleException("Battle plans not found");
		}
		
		if (battlePlans.size() % 2 > 0) {
			throw new InvalidBattleException("Expected even number of battle plans; found " + battlePlans.size());
		}
		
		for (BattlePlan bp : battlePlans) {
			if (bp == null) {
				throw new InvalidBattleException("Missing battle plan");
			}
			bp.validate();
		}
	}

}
