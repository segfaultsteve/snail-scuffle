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
		PlayerData player0 = skirmish.getPlayer0();
		PlayerData player1 = skirmish.getPlayer1();
		
		skirmishId = skirmish.getId().toString();
		
		if (requestingPlayer.id.equals(player0.id)) {
			playerName = player0.name;
			opponentName = (player1 == null) ? null : player1.name;
			firstMover = 0;
		} else if (player1 != null && requestingPlayer.id.equals(player1.id)) {
			playerName = player1.name;
			opponentName = player0.name;
			firstMover = 1;
		} else {
			throw new RuntimeException("Requesting player is not part of this battle");
		}
		
		battlePlans = skirmish.getBattlePlans(requestingPlayer.id);
		timeRemaining = skirmish.millisecondsRemaining();
		opponentHasForfeited = skirmish.opponentHasForfeited(requestingPlayer);
	}
	
}
