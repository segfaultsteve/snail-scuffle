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
	
	// A battle that runs to completion is considered finished at the height at which the last
	// battle plan of the last round is revealed. In this case, height is this height and blockId
	// is the ID of the block at this height.
	//
	// A battle that ends early because one player failed to submit a battle plan or its hash on
	// time is considered finished at the height of the "deadline" for this information: the height
	// of the last battle plan of the previous round plus Constants.MAX_BLOCKS_PER_ROUND for
	// committing a hash, or the height of the second committed hash for the round plus
	// Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED for revealing a battle plan. In both cases, the
	// ID of the corresponding block is unknown, so blockId is zero. See BattleInProgressResult for
	// more information.
	
	final public int height;
	final public long blockId;
	final public PlayerChange winner;
	final public PlayerChange loser;
	
	public StateChangeFromBattle(int finishHeight, long finishBlockId, PlayerChange winner, PlayerChange loser) {
		this.height = finishHeight;
		this.blockId = finishBlockId;
		this.winner = winner;
		this.loser = loser;
	}
	
}
