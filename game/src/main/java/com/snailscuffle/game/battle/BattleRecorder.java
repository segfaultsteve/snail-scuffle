package com.snailscuffle.game.battle;

import java.util.ArrayList;
import java.util.List;

import com.snailscuffle.common.battle.BattleEvent;
import com.snailscuffle.common.battle.BattleEventEffect;
import com.snailscuffle.common.battle.BattleSnapshot;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Stat;

class BattleRecorder {
	
	private Battle battle;
	private List<List<BattleEvent>> events = new ArrayList<>();
	private List<BattleEvent> currentRound = new ArrayList<>();
	private List<BattleSnapshot> endOfRoundStats = new ArrayList<>();
	private boolean newRound;
	
	BattleRecorder(Battle toRecord) {
		battle = toRecord;
		events.add(currentRound);
	}
	
	void recordAttack(Combatant attacker, double damage) {
		checkForNewRound();
		int time = battle.currentTime();
		int attackerIndex = battle.playerIndexOf(attacker);
		currentRound.add(BattleEvent.attack(time, attackerIndex, damage));
	}
	
	void recordUseItem(Combatant player, Item type, Stat stat, double change) {
		checkForNewRound();
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		currentRound.add(BattleEvent.useItem(time, playerIndex, type, stat, change));
	}
	
	void recordItemDone(Combatant player, Item type) {
		checkForNewRound();
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		currentRound.add(BattleEvent.itemDone(time, playerIndex, type));
	}
	
	void recordUseDefibrillator(Combatant player) {
		checkForNewRound();
		int time = battle.currentTime();
		int playerIndex = battle.playerIndexOf(player);
		currentRound.add(BattleEvent.resuscitate(time, playerIndex));
	}
	
	private void checkForNewRound() {
		if (newRound) {
			currentRound = new ArrayList<>();
			events.add(currentRound);
			newRound = false;
		}
	}
	
	void addEffectToLastEvent(Combatant player, Stat stat, double change) {
		int playerIndex = battle.playerIndexOf(player);
		BattleEvent lastEvent = currentRound.get(currentRound.size() - 1);
		lastEvent.effects.add(new BattleEventEffect(playerIndex, stat, change));
	}
	
	void recordEndOfRound(int time, Combatant player0, Combatant player1) {
		double player0Hp = 1.0 * player0.getHp() / Combatant.SCALE;
		double player0Ap = 1.0 * player0.getAp() / Combatant.SCALE;
		double player1Hp = 1.0 * player1.getHp() / Combatant.SCALE;
		double player1Ap = 1.0 * player1.getAp() / Combatant.SCALE;
		
		player0Hp = Math.max(player0Hp, 0);
		player1Hp = Math.max(player1Hp, 0);
		
		BattleSnapshot snapshot = new BattleSnapshot(time, player0Hp, player0Ap, player1Hp, player1Ap);
		snapshot.players.get(0).activeEffects.addAll(player0.getActiveEffects());
		snapshot.players.get(1).activeEffects.addAll(player1.getActiveEffects());
		endOfRoundStats.add(snapshot);
		
		newRound = true;
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
	
	List<BattleSnapshot> endOfRoundStats() {
		return endOfRoundStats;
	}

}
