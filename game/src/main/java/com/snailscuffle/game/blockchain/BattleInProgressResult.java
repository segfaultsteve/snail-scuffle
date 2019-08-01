package com.snailscuffle.game.blockchain;

class BattleInProgressResult {
	
	static final BattleInProgressResult ONGOING = new BattleInProgressResult(false);
	static final BattleInProgressResult ABORTED = new BattleInProgressResult(true);
	
	final long winner;
	final long loser;
	final int finishHeight;
	// Important!! In cases where one player forfeits (i.e., by submitting a hash or battle plan
	// too late) or is disqualified (i.e., by revealing a battle plan that doesn't match a
	// previously committed hash), the ID of the finishing block is not necessarily known. For
	// example, suppose both players committed hashes of their battle plans in block n. They
	// then have until block (n + Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED) to reveal their
	// battle plans. If one player does so and the other doesn't, then the latter player forfeits
	// and the height of the forfeit is (n + Constants.MAX_BLOCKS_AFTER_HASHES_COMMITTED).
	// There is no message at this height, though, so we don't have an OnChain<BattlePlan> from
	// which to read the block ID. In such cases, we simply set finishBlockId to zero and filter
	// these results out of the blocks returned by Accounts.getAllBlocksInCache(). Worst-case
	// scenario, we roll back an extra block or two while resolving a fork.
	final long finishBlockId;
	private boolean aborted;
	
	BattleInProgressResult(long winner, long loser, int finishHeight, long finishBlockId) {
		this.winner = winner;
		this.loser = loser;
		this.finishHeight = finishHeight;
		this.finishBlockId = finishBlockId;
	}
	
	private BattleInProgressResult(boolean aborted) {
		this.winner = 0;
		this.loser = 0;
		this.finishHeight = 0;
		this.finishBlockId = 0;
		this.aborted = aborted;
	}
	
	boolean isFinished() {
		return finishHeight > 0;
	}
	
	boolean wasAborted() {
		return aborted;
	}
	
}
