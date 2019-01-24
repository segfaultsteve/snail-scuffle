package com.snailscuffle.common.battle;

import java.io.Serializable;

public class BattleConfig implements Serializable {
	
	public BattlePlan[] battlePlans;
	public int firstMover;			// 0 refers to the player who submitted the first battle plan in battlePlans; 1 refers to the other player
	
	@SuppressWarnings("unused")
	private BattleConfig() {}		// needed for serialization via jackson
	
	public BattleConfig(BattlePlan... battlePlans) {
		this.battlePlans = battlePlans;
	}
	
	public void validate() throws InvalidBattleException {
		if (battlePlans == null) {
			throw new InvalidBattleException("Battle plans not found");
		}
		
		if (battlePlans.length % 2 > 0) {
			throw new InvalidBattleException("Expected even number of battle plans; found " + battlePlans.length);
		}
		
		for (BattlePlan bp : battlePlans) {
			if (bp == null) {
				throw new InvalidBattleException("Missing battle plan");
			}
			bp.validate();
		}
	}

}
