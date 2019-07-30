package com.snailscuffle.game.blockchain.data;

import com.snailscuffle.common.battle.BattlePlan;

public class BattlePlanRevealMessage extends BattlePlanMessage {
	
	public BattlePlan battlePlan;
	
	public BattlePlanRevealMessage(String battleId, int round, BattlePlan battlePlan) {
		super(battleId, round);
		this.battlePlan = battlePlan;
	}
	
}
