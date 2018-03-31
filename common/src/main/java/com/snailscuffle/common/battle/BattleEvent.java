package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.List;

public class BattleEvent implements Serializable {

	public int time;
	public int playerIndex;		// 0 refers to the player who submitted the first battle plan in BattleConfig.battlePlans; 1 refers to the other player
	public Action action;
	public Item itemUsed;		// null unless action is Action.USE_ITEM
	public List<BattleEventEffect> effects;
	
	public void validate() {
		if (time < 0) {
			throw new InvalidBattleException("Timestamp must be non-negative");
		}
		
		if (playerIndex < 0 || playerIndex > 1) {
			throw new InvalidBattleException("Invalid player index; must be 0 (player 1) or 1 (player 2)");
		}
		
		if (action == null) {
			throw new InvalidBattleException("Action not found");
		}
		
		if (action == Action.USE_ITEM && itemUsed == null) {
			throw new InvalidBattleException("Event of type USE_ITEM must include a non-null item to use");
		}
		
		if (effects == null || effects.size() == 0) {
			throw new InvalidBattleException("Event has no effects");
		}
		
		effects.forEach(e -> e.validate());
	}
	
}
