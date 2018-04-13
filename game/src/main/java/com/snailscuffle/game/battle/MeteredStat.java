package com.snailscuffle.game.battle;

public class MeteredStat {
	
	private int value;
	private Combatant player;
	private Combatant opponent;
	
	public MeteredStat(Combatant player, int initialValue) {
		this.player = player;
		value = initialValue;
	}
	
	public void registerOpponentForCallbacks(Combatant opponent) {
		this.opponent = opponent;
	}
	
	public int get() {
		return value;
	}
	
	public void set(int value) {
		this.value = value;
		player.onStatChanged();
		opponent.onEnemyStatChanged();
	}
	
	public void add(int delta) {
		set(value + delta);
	}
	
	public void subtract(int delta) {
		set(value - delta);
	}
	
}
