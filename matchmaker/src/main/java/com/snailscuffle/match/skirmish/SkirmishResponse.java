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
	public final boolean battlePlanAlreadySubmitted;
	
	public SkirmishResponse(PlayerData requestingPlayer, boolean battlePlanAlreadySubmitted) {
		Skirmish skirmish = requestingPlayer.skirmish;
		PlayerData player1 = skirmish.getPlayer1();
		PlayerData player2 = skirmish.getPlayer2();
		
		skirmishId = skirmish.getId().toString();
		
		if (requestingPlayer.id.equals(player1.id)) {
			playerName = player1.name;
			opponentName = (player2 == null) ? null : player2.name;
		} else if (player2 != null && requestingPlayer.id.equals(player2.id)) {
			playerName = player2.name;
			opponentName = player1.name;
		} else {
			throw new RuntimeException("Requesting player is not part of this battle");
		}
		
		battlePlans = skirmish.getBattlePlans(requestingPlayer.id);
		this.battlePlanAlreadySubmitted = battlePlanAlreadySubmitted;
	}
	
}
