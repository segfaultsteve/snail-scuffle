package com.snailscuffle.match.players;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.match.NotAuthorizedException;
import com.snailscuffle.match.skirmish.Skirmish;

public class PlayerData {
	
	public enum PlayerType {
		LOGGED_IN,
		GUEST
	}
	
	public static final String ATTRIBUTE_KEY = "player-data";
	
	public static PlayerData createGuestPlayer(String id) {
		return new PlayerData("Guest", id, PlayerType.GUEST);
	}
	
	public final String name;
	public final String id;
	public final PlayerType type;
	public Skirmish skirmish;
	
	private PlayerData(String name, String id, PlayerType type) {
		this.name = name;
		this.id = id;
		this.type = type;
	}
	
	public void tryAddBattlePlan(BattlePlan bp) throws NotAuthorizedException {
		skirmish.addBattlePlan(bp, this);
	}
	
	public boolean hasSubmittedBattlePlan() throws NotAuthorizedException {
		return skirmish.playerHasSubmittedBattlePlan(this);
	}
	
	public void forfeit() {
		if (skirmish != null) {
			skirmish.forfeit(this);
		}
	}
	
}
