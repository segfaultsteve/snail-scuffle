package com.snailscuffle.common.battle;

import java.io.Serializable;

public class BattleSnapshot implements Serializable {
	
	public int time;
	public double player1Hp;
	public double player1Ap;
	public double player2Hp;
	public double player2Ap;
	
	@SuppressWarnings("unused")
	private BattleSnapshot() {}		// needed for serialization via jackson
	
	public BattleSnapshot(int time, double player1Hp, double player1Ap, double player2Hp, double player2Ap) {
		this.time = time;
		this.player1Hp = player1Hp;
		this.player1Ap = player1Ap;
		this.player2Hp = player2Hp;
		this.player2Ap = player2Ap;
	}
	
	public void validate() {
		if (time < 0) {
			throw new InvalidBattleException("Invalid timestamp");
		}
		
		if (player1Hp < 0) {
			throw new InvalidBattleException("Invalid HP for player 1");
		}
		
		if (player1Ap < 0) {
			throw new InvalidBattleException("Invalid AP for player 1");
		}
		
		if (player2Hp < 0) {
			throw new InvalidBattleException("Invalid HP for player 2");
		}
		
		if (player2Ap < 0) {
			throw new InvalidBattleException("Invalid AP for player 2");
		}
	}
	
}
