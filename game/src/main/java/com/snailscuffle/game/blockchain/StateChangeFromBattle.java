package com.snailscuffle.game.blockchain;

import java.io.Serializable;

public class StateChangeFromBattle implements Serializable {
	
	public static class PlayerChange {
		final public long id;
		final public Stats previous;
		final public Stats updated;
		
		public PlayerChange (long id, int previousRating, int currentRating, int previousStreak, int currentStreak) {
			this.id = id;
			this.previous = new Stats(previousRating, previousStreak);
			this.updated = new Stats(currentRating, currentStreak);
		}
	}
	
	public static class Stats {
		final public int rating;
		final public int streak;
		
		public Stats(int rating, int streak) {
			this.rating = rating;
			this.streak = streak;
		}
	}
	
	final public int finishHeight;
	final public long finishBlockId;
	final public PlayerChange winner;
	final public PlayerChange loser;
	
	public StateChangeFromBattle(int finishHeight, long finishBlockId, PlayerChange winner, PlayerChange loser) {
		this.finishHeight = finishHeight;
		this.finishBlockId = finishBlockId;
		this.winner = winner;
		this.loser = loser;
	}
	
}
