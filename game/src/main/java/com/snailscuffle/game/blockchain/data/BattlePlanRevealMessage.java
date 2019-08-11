package com.snailscuffle.game.blockchain.data;

import com.snailscuffle.common.battle.BattlePlan;

public class BattlePlanRevealMessage extends BattlePlanMessage {
	
	public BattlePlan battlePlan;
	
	@SuppressWarnings("unused")
	private BattlePlanRevealMessage() {		// needed for deserialization via jackson
		super();
	}
	
	public BattlePlanRevealMessage(String battleId, int round, BattlePlan battlePlan) {
		super(battleId, round);
		this.battlePlan = battlePlan;
	}
	
}
