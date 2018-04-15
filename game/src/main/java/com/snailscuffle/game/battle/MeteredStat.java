package com.snailscuffle.game.battle;

class MeteredStat {
	
	private int value;
	private Combatant player;
	private Combatant opponent;
	
	MeteredStat(Combatant player, int initialValue) {
		this.player = player;
		value = initialValue;
	}
	
	void registerOpponentForCallbacks(Combatant opponent) {
		this.opponent = opponent;
	}
	
	int get() {
		return value;
	}
	
	void set(int value) {
		this.value = value;
		player.onStatChanged();
		opponent.onEnemyStatChanged();
	}
	
	void add(int delta) {
		set(value + delta);
	}
	
	void subtract(int delta) {
		set(value - delta);
	}
	
}
