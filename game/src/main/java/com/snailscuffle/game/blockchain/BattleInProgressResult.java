package com.snailscuffle.game.blockchain;

class BattleInProgressResult {
	
	static final BattleInProgressResult ONGOING = new BattleInProgressResult(false);
	static final BattleInProgressResult ABORTED = new BattleInProgressResult(true);
	
	final long winner;
	final long loser;
	final int finishHeight;
	private boolean aborted;
	
	BattleInProgressResult(long winner, long loser, int finishHeight) {
		this.winner = winner;
		this.loser = loser;
		this.finishHeight = finishHeight;
	}
	
	private BattleInProgressResult(boolean aborted) {
		this.winner = 0;
		this.loser = 0;
		this.finishHeight = 0;
		this.aborted = aborted;
	}
	
	boolean isFinished() {
		return finishHeight > 0;
	}
	
	boolean wasAborted() {
		return aborted;
	}
	
}
