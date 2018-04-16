package com.snailscuffle.game.battle;

import java.util.ArrayList;
import java.util.List;

import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattleEventEffect;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Stat;

class BattleRecorder {
	
	private Battle battle;
	private List<BattleEvent> events = new ArrayList<>();
	
	BattleRecorder(Battle toRecord) {
		battle = toRecord;
	}
	
	void recordAttack(Combatant attacker, double damage) {
		int time = battle.currentTime();
		int attackerIndex = battle.playerIndexOf(attacker);
		events.add(BattleEvent.attack(time, attackerIndex, damage));
	}
	
	void recordUseItem(Combatant player, Item type, Stat stat, double change) {
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		events.add(BattleEvent.useItem(time, playerIndex, type, stat, change));
	}
	
	void addEffectToLastEvent(Combatant player, Stat stat, double change) {
		int playerIndex = battle.playerIndexOf(player);
		BattleEvent lastEvent = events.get(events.size() - 1);
		lastEvent.effects.add(new BattleEventEffect(playerIndex, stat, change));
	}
	
	List<BattleEvent> battleEvents() {
		return events;
	}

}
