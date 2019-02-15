package com.snailscuffle.common.battle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BattleSnapshot implements Serializable {
	
	public int time;
	public final List<PlayerSnapshot> players = new ArrayList<>();
	
	@SuppressWarnings("unused")
	private BattleSnapshot() {}		// needed for serialization via jackson
	
	public BattleSnapshot(int time, double player0Hp, double player0Ap, double player1Hp, double player1Ap) {
		this.time = time;
		players.add(new PlayerSnapshot(player0Hp, player0Ap));
		players.add(new PlayerSnapshot(player1Hp, player1Ap));
	}
	
	public void validate() {
		if (time < 0) {
			throw new InvalidBattleException("Invalid timestamp");
		}
		
		players.forEach(p -> p.validate());
	}
	
}
