package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleEvent implements Serializable {

	public int time;
	public int playerIndex;		// 0 refers to the player who submitted the first battle plan in BattleConfig.battlePlans; 1 refers to the other player
	public Action action;
	public Item itemUsed;		// null unless action is Action.USE_ITEM
	public List<BattleEventEffect> effects;
	
	private BattleEvent() {}
	
	public static BattleEvent newAttackEvent(int timestamp, int attackerIndex, double damage) {
		BattleEvent event = new BattleEvent();
		event.time = timestamp;
		event.playerIndex = attackerIndex;
		event.action = Action.ATTACK;
		event.effects = new ArrayList<>();
		
		int targetIndex = (attackerIndex == 0) ? 1 : 0;
		event.effects.add(new BattleEventEffect(targetIndex, Stat.HP, -1.0 * damage));
		
		return event;
	}
	
	public static BattleEvent newUseItemEvent(int timestamp, int playerIndex, Item item, Stat stat, double change) {
		BattleEvent event = new BattleEvent();
		event.time = timestamp;
		event.playerIndex = playerIndex;
		event.action = Action.USE_ITEM;
		event.effects = new ArrayList<>();
		event.effects.add(new BattleEventEffect(playerIndex, stat, change));
		return event;
	}
	
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
