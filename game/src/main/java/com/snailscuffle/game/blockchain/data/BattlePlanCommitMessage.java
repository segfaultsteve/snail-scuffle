package com.snailscuffle.game.blockchain.data;

public class BattlePlanCommitMessage extends BattlePlanMessage {
	
	public String battlePlanHash;
	
	@SuppressWarnings("unused")
	private BattlePlanCommitMessage() {		// needed for deserialization via jackson
		super();
	}
	
	public BattlePlanCommitMessage(String battleId, int round, String battlePlanHash) {
		super(battleId, round);
		this.battlePlanHash = battlePlanHash;
	}
	
}
