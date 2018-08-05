package com.snailscuffle.match.skirmish;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.snailscuffle.match.players.PlayerData;
import com.snailscuffle.match.players.PlayerData.PlayerType;

public class SkirmishMatcher {
	
	private List<PlayerData> guestsAwaitingMatches = new ArrayList<>();
	private List<PlayerData> loggedInPlayersAwaitingMatches = new ArrayList<>();
	private ConcurrentMap<String, Skirmish> skirmishes = new ConcurrentHashMap<>();
	
	public void tryMatchPlayer(PlayerData player) {
		if (player.skirmish == null) {
			List<PlayerData> potentialOpponents = (player.type == PlayerType.GUEST) ? guestsAwaitingMatches : loggedInPlayersAwaitingMatches;
			Skirmish skirmish = tryMatchPlayer(player, potentialOpponents);
			skirmishes.put(skirmish.getId().toString(), skirmish);
		}
	}
	
	private static Skirmish tryMatchPlayer(PlayerData player, List<PlayerData> waitingPlayers) {
		Skirmish skirmish = null;
		synchronized(waitingPlayers) {
			if (waitingPlayers.isEmpty()) {
				skirmish = new Skirmish(player);
				player.skirmish = skirmish;
				waitingPlayers.add(player);
			} else {
				PlayerData opponent = waitingPlayers.remove(0);
				skirmish = opponent.skirmish;
				
				// these operations do not need to be atomic, so no synchronized block here
				skirmish.addOpponent(player);
				player.skirmish = skirmish;
			}
		}
		return skirmish;
	}
	
	public Skirmish getSkirmish(String skirmishId) {
		return skirmishes.get(skirmishId);
	}

}
