package com.snailscuffle.game.battle;

import com.snailscuffle.common.battle.BattleConfig;
import com.snailscuffle.common.battle.BattleResult;

public class Battle {
	
	private static final int HALFTIME = 100 * Combatant.SCALE;	// TODO: set to a reasonable number; for now, all battles finish before halftime
	
	private int time = 0;
	private Combatant player1;
	private Combatant player2;
	private BattleRecorder recorder;
	
	public Battle(BattleConfig config) {
		recorder = new BattleRecorder(this);
		player1 = new Combatant(config.battlePlans.get(0), 0, recorder);
		player2 = new Combatant(config.battlePlans.get(1), 1, recorder);
		
		player1.setOpponent(player2);
		player2.setOpponent(player1);
		
		int increment = 0;
		for (time = 0; time < HALFTIME && player1.isAlive() && player2.isAlive(); time += increment) {
			player1.update(increment);
			player2.update(increment);
			increment = nextIncrement(player1, player2);
		}
		
		if (config.battlePlans.size() == 4) {
			player1.setSecondHalfBattlePlan(config.battlePlans.get(2));
			player2.setSecondHalfBattlePlan(config.battlePlans.get(3));
			
			while (player1.isAlive() && player2.isAlive()) {
				increment = nextIncrement(player1, player2);
				time += increment;
				player1.update(increment);
				player2.update(increment);
			}
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
			winnerIndex = -1;	// both players are still alive (halftime result)
		}
		
		return new BattleResult(recorder.battleEvents(), winnerIndex);
	}

}
