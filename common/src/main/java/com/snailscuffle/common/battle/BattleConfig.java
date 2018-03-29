package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleConfig implements Serializable {
	
	public List<BattlePlan> battlePlans;
	public long firstHalfRngSeed;
	public long secondHalfRngSeed;
	
	@SuppressWarnings("unused")
	private BattleConfig() {}		// needed for serialization via jackson-jr
	
	public BattleConfig(BattlePlan p1FirstHalf, BattlePlan p2FirstHalf, long seed) {
		battlePlans = new ArrayList<>();
		battlePlans.add(p1FirstHalf);
		battlePlans.add(p2FirstHalf);
		firstHalfRngSeed = seed;
	}
	
	public BattleConfig(BattlePlan p1FirstHalf, BattlePlan p2FirstHalf, BattlePlan p1SecondHalf, BattlePlan p2SecondHalf, long seed1, long seed2) {
		battlePlans = new ArrayList<>();
		battlePlans.add(p1FirstHalf);
		battlePlans.add(p2FirstHalf);
		battlePlans.add(p1SecondHalf);
		battlePlans.add(p2SecondHalf);
		firstHalfRngSeed = seed1;
		secondHalfRngSeed = seed2;
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
