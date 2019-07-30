package com.snailscuffle.game.blockchain.data;

public class BattlePlanMessage extends SnailScuffleMessage {
	
	public String battleId;
	public int round;
	
	public BattlePlanMessage(String battleId, int round) {
		this.battleId = battleId;
		this.round = round;
	}
	
}
