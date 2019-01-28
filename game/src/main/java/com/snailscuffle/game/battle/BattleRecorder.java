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
	
	void recordEndOfRound(int time, Combatant player1, Combatant player2) {
		double player1Hp = 1.0 * player1.getHp() / Combatant.SCALE;
		double player1Ap = 1.0 * player1.getAp() / Combatant.SCALE;
		double player2Hp = 1.0 * player2.getHp() / Combatant.SCALE;
		double player2Ap = 1.0 * player2.getAp() / Combatant.SCALE;
		
		player1Hp = Math.max(player1Hp, 0);
		player2Hp = Math.max(player2Hp, 0);
		
		int ticksToEndOfRound = time - battle.currentTime();
		player1Ap += 1.0 * ticksToEndOfRound * player1.speedStat() / (Combatant.SCALE * Combatant.SCALE);
		player2Ap += 1.0 * ticksToEndOfRound * player2.speedStat() / (Combatant.SCALE * Combatant.SCALE);
		
		BattleSnapshot snapshot = new BattleSnapshot(time, player1Hp, player1Ap, player2Hp, player2Ap);
		snapshot.player1ActiveEffects.addAll(player1.getActiveEffects());
		snapshot.player2ActiveEffects.addAll(player2.getActiveEffects());
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
