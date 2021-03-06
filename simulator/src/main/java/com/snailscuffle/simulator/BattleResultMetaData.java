package com.snailscuffle.simulator;

import java.util.List;

import com.snailscuffle.common.battle.*;

public class BattleResultMetaData {

	public int marginOfVictory;
	public int timeToVictory;
	public int wonGame;
	public List<BattleResult> battleResults;

	public BattleResultMetaData(List<BattleResult> battleResults) {
		this.battleResults = battleResults;
		marginOfVictory = determineMarginOfVictory();
		timeToVictory = determineTimeToVictory();
		wonGame = determineWinner();
	}
	
	private int determineWinner() {
		
		int winner = -1;
		
		for(BattleResult result : battleResults) {
			if (result.winnerIndex < 0)
				continue;
			else
				winner = result.winnerIndex;
		}
		
		return winner;
	}
	
	private int determineMarginOfVictory() {
		
		int player0Health = 100;
		int player1Health = 100;
		
		for(BattleResult result : battleResults) {
			for(BattleEvent event : result.flattenEvents()) {
				for(BattleEventEffect effect : event.effects) {
					if (effect.playerIndex == 0 && effect.stat == Stat.HP) {
						player0Health += effect.change;
					} else if (effect.playerIndex == 1 && effect.stat == Stat.HP) {
						player1Health += effect.change;
					}
				}
			}		
		}

		return Math.abs(player0Health - player1Health);
	}
	
	private int determineTimeToVictory() {

		int timeToVictory = -1;
		
		for(BattleResult result : battleResults) {
			int numberOfEventsInBattle = result.flattenEvents().size() - 1;
			
			if (result.winnerIndex < 0)
				continue;
			else
			{
				// We're guaranteed for the battle to end as soon as one player runs out of health
				// so we know that the time to win will be the time of either the last or second-to-last event
				// (depending on the winner).
				if (result.flattenEvents().get(numberOfEventsInBattle).playerIndex == result.winnerIndex ) {
					timeToVictory =  result.flattenEvents().get(numberOfEventsInBattle).time;
				} else {
					timeToVictory =  result.flattenEvents().get(numberOfEventsInBattle - 1).time;
				}
			}
		}
		return timeToVictory;
	}
}
