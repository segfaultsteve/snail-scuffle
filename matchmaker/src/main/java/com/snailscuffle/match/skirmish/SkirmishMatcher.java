package com.snailscuffle.match.skirmish;

import java.util.ArrayList;
import java.util.List;
import com.snailscuffle.match.players.PlayerData;
import com.snailscuffle.match.players.PlayerData.PlayerType;

public class SkirmishMatcher {
	
	private List<PlayerData> guestsAwaitingMatches = new ArrayList<>();
	private List<PlayerData> loggedInPlayersAwaitingMatches = new ArrayList<>();
	
	public void tryMatchPlayer(PlayerData player) {
		List<PlayerData> potentialOpponents = (player.type == PlayerType.GUEST) ? guestsAwaitingMatches : loggedInPlayersAwaitingMatches;
		tryMatchPlayer(player, potentialOpponents);
	}
	
	private static void tryMatchPlayer(PlayerData player, List<PlayerData> waitingPlayers) {
		synchronized(waitingPlayers) {
			if (waitingPlayers.isEmpty()) {
				player.skirmish = new Skirmish(player);
				waitingPlayers.add(player);
			} else {
				PlayerData opponent = waitingPlayers.remove(0);
				opponent.skirmish.addOpponent(player);
				player.skirmish = opponent.skirmish;
			}
		}
	}

}
