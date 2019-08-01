package com.snailscuffle.game.blockchain;

import java.io.Serializable;

public class StatChangesFromBattle implements Serializable {
	
	public static class PlayerResult {
		final public long id;
		final public Stats previous;
		final public Stats updated;
		
		public PlayerResult (long id, int previousRating, int currentRating, int previousStreak, int currentStreak) {
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
	final public PlayerResult winner;
	final public PlayerResult loser;
	
	public StatChangesFromBattle(int finishHeight, long finishBlockId, PlayerResult winner, PlayerResult loser) {
		this.finishHeight = finishHeight;
		this.finishBlockId = finishBlockId;
		this.winner = winner;
		this.loser = loser;
	}
	
}
