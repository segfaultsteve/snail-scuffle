package com.snailscuffle.match.skirmish;

import java.io.Serializable;
import java.util.List;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.players.PlayerData;

public class SkirmishResponse implements Serializable {
	
	public final String skirmishId;
	public final String playerName;
	public final String opponentName;
	public final List<BattlePlan> battlePlans;
	public final int firstMover;
	public final int timeRemaining;
	public final boolean opponentHasForfeited;
	
	public SkirmishResponse(PlayerData requestingPlayer) {
		Skirmish skirmish = requestingPlayer.skirmish;
		PlayerData player1 = skirmish.getPlayer1();
		PlayerData player2 = skirmish.getPlayer2();
		
		skirmishId = skirmish.getId().toString();
		
		if (requestingPlayer.id.equals(player1.id)) {
			playerName = player1.name;
			opponentName = (player2 == null) ? null : player2.name;
			firstMover = 0;
		} else if (player2 != null && requestingPlayer.id.equals(player2.id)) {
			playerName = player2.name;
			opponentName = player1.name;
			firstMover = 1;
		} else {
			throw new RuntimeException("Requesting player is not part of this battle");
		}
		
		battlePlans = skirmish.getBattlePlans(requestingPlayer.id);
		timeRemaining = skirmish.millisecondsRemaining();
		opponentHasForfeited = skirmish.opponentHasForfeited(requestingPlayer);
	}
	
}
