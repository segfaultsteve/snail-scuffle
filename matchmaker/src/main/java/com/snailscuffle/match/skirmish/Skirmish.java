package com.snailscuffle.match.skirmish;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.players.PlayerData;

public class Skirmish {
	
	private static class BattlePlanPair {
		private BattlePlan player1Bp;
		private BattlePlan player2Bp;
	}
	
	public static final int ROUND_TIME_LIMIT_MILLIS = 30 * 1000;
	
	private final UUID id;
	private int round;
	private PlayerData player1;
	private PlayerData player2;
	private List<BattlePlanPair> battlePlans = new ArrayList<>();
	private long currentRoundStartTime;
	private int forfeit;	// 1 if player 1 has forfeited, 2 if player 2 has forfeited, 0 otherwise
	
	public Skirmish(PlayerData player) {
		id = UUID.randomUUID();
		player1 = player;
	}
	
	public UUID getId() { return id; }
	public PlayerData getPlayer1() { return player1; }
	public PlayerData getPlayer2() { return player2; }
	
	public void addOpponent(PlayerData player) {
		player2 = player;
		currentRoundStartTime = new Date().getTime();
	}
	
	public void addBattlePlan(BattlePlan bp, PlayerData player) {
		boolean isPlayer1 = player.id.equals(player1.id);
		boolean isPlayer2 = (player2 != null) && player.id.equals(player2.id);
		if (!isPlayer1 && !isPlayer2) {
			throw new RuntimeException("Submitting player is not part of this skirmish");
		}
		
		BattlePlanPair thisRoundBps = null;
		if (battlePlans.size() == round) {
			thisRoundBps = new BattlePlanPair();
			battlePlans.add(thisRoundBps);
		} else {
			thisRoundBps = battlePlans.get(round);
		}
		
		if (isPlayer1 && thisRoundBps.player1Bp == null) {
			thisRoundBps.player1Bp = bp;
		} else if (isPlayer2 && thisRoundBps.player2Bp == null) {
			thisRoundBps.player2Bp = bp;
		}
		
		if (thisRoundBps.player1Bp != null && thisRoundBps.player2Bp != null) {
			round++;
			currentRoundStartTime = new Date().getTime();
		}
	}
	
	public List<BattlePlan> getBattlePlans(String idOfRequestingPlayer) {
		List<BattlePlan> bps = new ArrayList<>();
		for (int i = 0; i < round; i++) {
			BattlePlanPair bpsForRound = battlePlans.get(i);
			if (idOfRequestingPlayer.equals(player1.id)) {
				bps.add(bpsForRound.player1Bp);
				bps.add(bpsForRound.player2Bp);
			} else {
				bps.add(bpsForRound.player2Bp);
				bps.add(bpsForRound.player1Bp);
			}
		}
		return bps;
	}
	
	public boolean playerHasSubmittedBattlePlan(PlayerData player) {
		boolean isPlayer1 = player.id.equals(player1.id);
		boolean isPlayer2 = (player2 != null) && player.id.equals(player2.id);
		if (!isPlayer1 && !isPlayer2) {
			throw new RuntimeException("Submitting player is not part of this skirmish");
		}
		
		boolean player1HasSubmitted = (battlePlans.size() > round && battlePlans.get(round).player1Bp != null);
		boolean player2HasSubmitted = (battlePlans.size() > round && battlePlans.get(round).player2Bp != null);
		
		return (isPlayer1 && player1HasSubmitted) || (isPlayer2 && player2HasSubmitted);
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
		if (player.id.equals(player1.id)) {
			forfeit = 1;
		} else if (player2 != null && player.id.equals(player2.id)) {
			forfeit = 2;
		}
	}
	
	public boolean opponentHasForfeited(PlayerData player) {
		if (player.id.equals(player1.id) && player2 != null) {
			return forfeit == 2 || (!playerHasSubmittedBattlePlan(player2) && millisecondsRemaining() == 0);
		} else if (player2 != null && player.id.equals(player2.id)) {
			return forfeit == 1 || (!playerHasSubmittedBattlePlan(player1) && millisecondsRemaining() == 0);
		} else {
			return false;
		}
	}
	
}
