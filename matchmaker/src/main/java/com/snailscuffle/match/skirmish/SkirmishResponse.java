package com.snailscuffle.match.skirmish;

import java.io.Serializable;
import java.util.List;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.players.PlayerData;

public class SkirmishResponse implements Serializable {
	
	public final String skirmishId;
	public final String player1Name;
	public final String player2Name;
	public final int indexOfRequestingPlayer;
	public final List<BattlePlan> battlePlans;
	
	private SkirmishResponse(Skirmish skirmish, String idOfRequestingPlayer) {
		PlayerData player1 = skirmish.getPlayer1();
		PlayerData player2 = skirmish.getPlayer2();
		
		skirmishId = skirmish.getId().toString();
		player1Name = player1.name;
		player2Name = (player2 == null) ? null : player2.name;
		
		if (idOfRequestingPlayer.equals(player1.id)) {
			indexOfRequestingPlayer = 1;
		} else if (player2 != null && idOfRequestingPlayer.equals(player2.id)) {
			indexOfRequestingPlayer = 2;
		} else {
			indexOfRequestingPlayer = 0;
		}
		
		battlePlans = skirmish.getBattlePlans();
	}
	
	public static SkirmishResponse forPlayer(PlayerData player) {
		return new SkirmishResponse(player.skirmish, player.id);
	}
	
	public static SkirmishResponse forSkirmish(Skirmish skirmish, String idOfRequestingPlayer) {
		return new SkirmishResponse(skirmish, idOfRequestingPlayer);
	}
	
}
