package com.snailscuffle.game.battle;

import java.util.ArrayList;
import java.util.List;

import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Stat;

public class BattleRecorder {
	
	private Battle battle;
	private List<BattleEvent> events;
	
	public BattleRecorder(Battle toRecord) {
		battle = toRecord;
		events = new ArrayList<>();
	}
	
	public void recordAttack(Combatant attacker, double damage) {
		int time = battle.currentTime();
		int attackerIndex = battle.playerIndexOf(attacker);
		events.add(BattleEvent.newAttackEvent(time, attackerIndex, damage));
	}
	
	public void recordUseItem(Combatant player, Item type, Stat stat, double change) {
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		events.add(BattleEvent.newUseItemEvent(time, playerIndex, type, stat, change));
	}
	
	public List<BattleEvent> battleEvents() {
		return events;
	}

}
