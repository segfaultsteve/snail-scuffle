package com.snailscuffle.game.battle;

import java.util.ArrayList;
import java.util.List;

import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattleEventEffect;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Stat;

class BattleRecorder {
	
	private Battle battle;
	private List<List<BattleEvent>> events = new ArrayList<>();
	private List<BattleEvent> currentRound;
	
	BattleRecorder(Battle toRecord) {
		battle = toRecord;
	}
	
	void recordAttack(Combatant attacker, double damage) {
		if (currentRound == null) {
			nextRound();
		}
		int time = battle.currentTime();
		int attackerIndex = battle.playerIndexOf(attacker);
		currentRound.add(BattleEvent.attack(time, attackerIndex, damage));
	}
	
	void recordUseItem(Combatant player, Item type, Stat stat, double change) {
		if (currentRound == null) {
			nextRound();
		}
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		currentRound.add(BattleEvent.useItem(time, playerIndex, type, stat, change));
	}
	
	void addEffectToLastEvent(Combatant player, Stat stat, double change) {
		int playerIndex = battle.playerIndexOf(player);
		BattleEvent lastEvent = currentRound.get(currentRound.size() - 1);
		lastEvent.effects.add(new BattleEventEffect(playerIndex, stat, change));
	}
	
	void nextRound() {
		currentRound = new ArrayList<>();
		events.add(currentRound);
	}
	
	List<List<BattleEvent>> eventsByRound() {
		return events;
	}
	
	List<BattleEvent> flatEvents() {
		List<BattleEvent> flat = new ArrayList<>();
		for (List<BattleEvent> round : events) {
			flat.addAll(round);
		}
		return flat;
	}

}
