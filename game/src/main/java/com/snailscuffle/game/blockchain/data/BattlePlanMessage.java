package com.snailscuffle.game.blockchain.data;

public class BattlePlanMessage extends SnailScuffleMessage {
	
	public String battleId;
	public int round;
	
	protected BattlePlanMessage() {}		// needed for deserialization via jackson
	
	public BattlePlanMessage(String battleId, int round) {
		this.battleId = battleId;
		this.round = round;
	}
	
}
