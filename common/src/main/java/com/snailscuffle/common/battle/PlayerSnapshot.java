package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerSnapshot implements Serializable {
	
	public double hp;
	public double ap;
	public final List<Item> activeEffects = new ArrayList<>();
	
	@SuppressWarnings("unused")
	private PlayerSnapshot() {}		// needed for serialization via jackson
	
	public PlayerSnapshot(double hp, double ap) {
		this.hp = hp;
		this.ap = ap;
	}
	
	public void validate() {
		if (hp < 0) {
			throw new InvalidBattleException("Invalid HP");
		}
		
		if (ap < 0) {
			throw new InvalidBattleException("Invalid AP");
		}
	}
	
}
