package com.snailscuffle.game.battle;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattleResult;

public class Battle {
	
	private static final int PERIOD = 4 * Combatant.SCALE;
	
	private int time = 0;
	private Combatant player1;
	private Combatant player2;
	private BattleRecorder recorder;
	
	public Battle(BattleConfig config) {
		recorder = new BattleRecorder(this);
		player1 = new Combatant(0, recorder);
		player2 = new Combatant(1, recorder);
		
		player1.setOpponent(player2);
		player2.setOpponent(player1);
		
		int periodEnd = PERIOD;
		for (int bpIndex = 0; bpIndex < config.battlePlans.length && player1.isAlive() && player2.isAlive(); bpIndex += 2) {
			player1.setBattlePlan(config.battlePlans[bpIndex]);
			player2.setBattlePlan(config.battlePlans[bpIndex + 1]);
			
			int increment = 0;
			for (time = 0; time < periodEnd && player1.isAlive() && player2.isAlive(); time += increment) {
				player1.update(increment);
				player2.update(increment);
				increment = nextIncrement(player1, player2);
			}
			periodEnd += PERIOD;
		}
	}
	
	private static int nextIncrement(Combatant p1, Combatant p2) {
		return Math.min(p1.ticksToNextAp(), p2.ticksToNextAp());
	}
	
	public int currentTime() {
		return time;
	}
	
	public int playerIndexOf(Combatant combatant) {
		if (combatant == player1) {
			return 0;
		} else if (combatant == player2) {
			return 1;
		} else {
			throw new RuntimeException("Unknown player");
		}
	}

	public BattleResult getResult() {
		int winnerIndex = 0;
		if (!player2.isAlive()) {
			winnerIndex = 0;
		} else if (!player1.isAlive()) {
			winnerIndex = 1;
		} else {
			winnerIndex = -1;	// both players are still alive
		}
		
		return new BattleResult(recorder.battleEvents(), winnerIndex);
	}

}
