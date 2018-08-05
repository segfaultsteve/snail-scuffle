package com.snailscuffle.match.skirmish;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.NotAuthorizedException;
import com.snailscuffle.match.players.PlayerData;

public class Skirmish {
	
	private static class BattlePlanPair {
		private BattlePlan firstMoverBp;
		private BattlePlan opponentBp;
	}
	
	private final UUID id;
	private int round;
	private PlayerData player1;		// player1 has first-mover advantage: his battle plan comes first in each round's BattleConfig
	private PlayerData player2;
	private List<BattlePlanPair> battlePlans = new ArrayList<>();
	
	public Skirmish(PlayerData player) {
		id = UUID.randomUUID();
		player1 = player;
	}
	
	public UUID getId() { return id; }
	public PlayerData getPlayer1() { return player1; }
	public PlayerData getPlayer2() { return player2; }
	
	public void addOpponent(PlayerData player) {
		player2 = player;
	}
	
	public void addBattlePlan(BattlePlan bp, PlayerData player) throws NotAuthorizedException {
		boolean isPlayer1 = player.id.equals(player1.id);
		boolean isPlayer2 = (player2 != null) && player.id.equals(player2.id);
		if (!isPlayer1 && !isPlayer2) {
			throw new NotAuthorizedException("Submitting player is not part of this match");
		}
		
		boolean player2IsFirstMover = battlePlans.isEmpty() && isPlayer2;
		if (player2IsFirstMover) {
			PlayerData temp = player1;
			player1 = player2;
			player2 = temp;
			isPlayer1 = !isPlayer1;
			isPlayer2 = !isPlayer2;
		}
		
		BattlePlanPair thisRoundBps = null;
		if (battlePlans.size() == round) {
			thisRoundBps = new BattlePlanPair();
			battlePlans.add(thisRoundBps);
		} else {
			thisRoundBps = battlePlans.get(round);
		}
		
		if (isPlayer1 && thisRoundBps.firstMoverBp == null) {
			thisRoundBps.firstMoverBp = bp;
		} else if (isPlayer2 && thisRoundBps.opponentBp == null) {
			thisRoundBps.opponentBp = bp;
		}
		
		if (thisRoundBps.firstMoverBp != null && thisRoundBps.opponentBp != null) {
			round++;
		}
	}
	
	public List<BattlePlan> getBattlePlans() {
		List<BattlePlan> bps = new ArrayList<>();
		for (int i = 0; i < round; i++) {
			BattlePlanPair bpsForRound = battlePlans.get(i);
			bps.add(bpsForRound.firstMoverBp);
			bps.add(bpsForRound.opponentBp);
		}
		return bps;
	}
	
}
