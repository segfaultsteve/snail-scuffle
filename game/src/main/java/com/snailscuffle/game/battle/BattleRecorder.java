package com.snailscuffle.game.battle;

import java.util.ArrayList;
import java.util.List;

import com.snailscuffle.common.battle.BattleEvent;

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
	
	public List<BattleEvent> battleEvents() {
		return events;
	}

}
