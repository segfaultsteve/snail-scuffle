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
		
		Combatant firstMover = (config.firstMover == 0) ? player1 : player2;
		Combatant secondMover = (config.firstMover == 0) ? player2 : player1;
		
		int periodEnd = PERIOD;
		for (int bpIndex = 0; bpIndex < config.battlePlans.length && player1.isAlive() && player2.isAlive(); bpIndex += 2) {
			player1.setBattlePlan(config.battlePlans[bpIndex]);
			player2.setBattlePlan(config.battlePlans[bpIndex + 1]);
			
			int increment = 0;
			while (time + increment < periodEnd && player1.isAlive() && player2.isAlive()) {
				time += increment;
				firstMover.update(increment);
				if (secondMover.isAlive()) {
					secondMover.update(increment);
				}
				increment = nextIncrement(player1, player2);
			}
			
			if (player1.isAlive() && player2.isAlive()) {
				int ticksToEndOfRound = periodEnd - time; 
				time = periodEnd;
				firstMover.update(ticksToEndOfRound);
				secondMover.update(ticksToEndOfRound);
			}
			
			recorder.recordEndOfRound(time, player1, player2);
			periodEnd += PERIOD;
		}
	}
	
	private static int nextIncrement(Combatant p1, Combatant p2) {
		return Math.min(p1.ticksToNextEvent(), p2.ticksToNextEvent());
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
		int winnerIndex = -1;
		if (!player2.isAlive()) {
			winnerIndex = 0;
		} else if (!player1.isAlive()) {
			winnerIndex = 1;
		}
		return new BattleResult(recorder.eventsByRound(), recorder.endOfRoundStats(), winnerIndex);
	}

}
