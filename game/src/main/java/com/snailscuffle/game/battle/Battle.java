package com.snailscuffle.game.battle;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattleResult;

public class Battle {
	
	private static final int PERIOD = 6 * Combatant.SCALE;
	
	private int time;
	private Combatant player1;
	private Combatant player2;
	private BattleRecorder recorder;
	
	public Battle(BattleConfig config) {
		recorder = new BattleRecorder(this);
		player1 = new Combatant(recorder);
		player2 = new Combatant(recorder);
		
		player1.setOpponent(player2);
		player2.setOpponent(player1);
		
		int periodEnd = PERIOD;
		for (int bpIndex = 0; bpIndex < config.battlePlans.length && player1.isAlive() && player2.isAlive(); bpIndex += 2) {
			recorder.nextRound();
			player1.setBattlePlan(config.battlePlans[bpIndex]);
			player2.setBattlePlan(config.battlePlans[bpIndex + 1]);
			
			int increment = 0;
			while (time + increment < periodEnd && player1.isAlive() && player2.isAlive()) {
				time += increment;
				player1.update(increment);
				if (player2.isAlive()) {
					player2.update(increment);
				}
				increment = nextIncrement(player1, player2);
			}
			recorder.recordEndOfRound(periodEnd, player1, player2);
			periodEnd += PERIOD;
		}
	}
	
	private static int nextIncrement(Combatant p1, Combatant p2) {
		return Math.min(p1.ticksToNextAp(), p2.ticksToNextAp());
	}
	
	int currentTime() {
		return time;
	}
	
	int playerIndexOf(Combatant combatant) {
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
		
		return new BattleResult(recorder.eventsByRound(), recorder.endOfRoundStats(), winnerIndex);
	}

}
