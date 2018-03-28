package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleConfig implements Serializable {
	
	public List<BattlePlan> battlePlans;
	
	@SuppressWarnings("unused")
	private BattleConfig() {}		// needed for serialization via jackson-jr
	
	public BattleConfig(BattlePlan p1firstHalf, BattlePlan p2firstHalf) {
		battlePlans = new ArrayList<>();
		battlePlans.add(p1firstHalf);
		battlePlans.add(p2firstHalf);
	}
	
	public BattleConfig(BattlePlan p1firstHalf, BattlePlan p2firstHalf, BattlePlan p1secondHalf, BattlePlan p2secondHalf) {
		battlePlans = new ArrayList<>();
		battlePlans.add(p1firstHalf);
		battlePlans.add(p2firstHalf);
		battlePlans.add(p1secondHalf);
		battlePlans.add(p2secondHalf);
	}
	
	public void validate() throws InvalidBattleException {
		if (battlePlans == null) {
			throw new InvalidBattleException("Battle plans not found");
		}
		
		if (battlePlans.size() != 2 && battlePlans.size() != 4) {
			throw new InvalidBattleException("Expected 2 or 4 battle plans; found " + battlePlans.size());
		}
		
		battlePlans.forEach(bp -> bp.validate());
	}

}
