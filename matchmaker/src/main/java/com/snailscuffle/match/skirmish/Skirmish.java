package com.snailscuffle.match.skirmish;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.players.PlayerData;

public class Skirmish {
	
	public static final int ROUND_TIME_LIMIT_MILLIS = 3 * 60 * 1000;
	
	private final UUID id;
	private int round;
	private PlayerData[] players = new PlayerData[2];
	private List<BattlePlan[]> battlePlans = new ArrayList<>();
	private long currentRoundStartTime;
	private int forfeit = -1;	// 0 if player 0 has forfeited, 1 if player 1 has forfeited, -1 otherwise
	
	public Skirmish(PlayerData player) {
		id = UUID.randomUUID();
		players[0] = player;
	}
	
	public UUID getId() { return id; }
	public PlayerData getPlayer0() { return players[0]; }
	public PlayerData getPlayer1() { return players[1]; }
	
	public void addOpponent(PlayerData player) {
		players[1] = player;
		currentRoundStartTime = new Date().getTime();
	}
	
	public void addBattlePlan(BattlePlan bp, PlayerData player) {
		boolean isPlayer0 = player.id.equals(players[0].id);
		boolean isPlayer1 = (players[1] != null) && player.id.equals(players[1].id);
		if (!isPlayer0 && !isPlayer1) {
			throw new RuntimeException("Submitting player is not part of this skirmish");
		}
		
		BattlePlan[] thisRoundBps = null;
		if (battlePlans.size() == round) {
			thisRoundBps = new BattlePlan[2];
			battlePlans.add(thisRoundBps);
		} else {
			thisRoundBps = battlePlans.get(round);
		}
		
		if (isPlayer0 && thisRoundBps[0] == null) {
			thisRoundBps[0] = bp;
		} else if (isPlayer1 && thisRoundBps[1] == null) {
			thisRoundBps[1] = bp;
		}
		
		if (thisRoundBps[0] != null && thisRoundBps[1] != null) {
			round++;
			currentRoundStartTime = new Date().getTime();
		}
	}
	
	public List<BattlePlan> getBattlePlans(String idOfRequestingPlayer) {
		List<BattlePlan> bps = new ArrayList<>();
		for (int i = 0; i < round; i++) {
			BattlePlan[] bpsForRound = battlePlans.get(i);
			if (idOfRequestingPlayer.equals(players[0].id)) {
				bps.add(bpsForRound[0]);
				bps.add(bpsForRound[1]);
			} else {
				bps.add(bpsForRound[1]);
				bps.add(bpsForRound[0]);
			}
		}
		return bps;
	}
	
	public boolean playerHasSubmittedBattlePlan(PlayerData player) {
		boolean isPlayer0 = player.id.equals(players[0].id);
		boolean isPlayer1 = (players[1] != null) && player.id.equals(players[1].id);
		if (!isPlayer0 && !isPlayer1) {
			throw new RuntimeException("Submitting player is not part of this skirmish");
		}
		
		boolean player0HasSubmitted = (battlePlans.size() > round && battlePlans.get(round)[0] != null);
		boolean player1HasSubmitted = (battlePlans.size() > round && battlePlans.get(round)[1] != null);
		
		return (isPlayer0 && player0HasSubmitted) || (isPlayer1 && player1HasSubmitted);
	}
	
	public int millisecondsRemaining() {
		if (currentRoundStartTime == 0) {
			return 0;
		} else {
			int elapsed = (int) (new Date().getTime() - currentRoundStartTime);
			return Math.max(ROUND_TIME_LIMIT_MILLIS - elapsed, 0);
		}
	}
	
	public void forfeit(PlayerData player) {
		if (player.id.equals(players[0].id)) {
			forfeit = 0;
		} else if (players[1] != null && player.id.equals(players[1].id)) {
			forfeit = 1;
		}
	}
	
	public boolean opponentHasForfeited(PlayerData player) {
		if (player.id.equals(players[0].id) && players[1] != null) {
			return forfeit == 1 || (!playerHasSubmittedBattlePlan(players[1]) && millisecondsRemaining() == 0);
		} else if (players[1] != null && player.id.equals(players[1].id)) {
			return forfeit == 0 || (!playerHasSubmittedBattlePlan(players[0]) && millisecondsRemaining() == 0);
		} else {
			return false;
		}
	}
	
}
