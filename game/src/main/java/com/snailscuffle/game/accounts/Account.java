package com.snailscuffle.game.accounts;

import java.io.Serializable;

public class Account implements Serializable {
	
	public String id;	// stored as a string to avoid loss of precision in JavaScript
	public String username;
	public String publicKey;
	public int wins;
	public int losses;
	public int streak;
	public int rating;
	public int rank;
	public double balance;
	
	@SuppressWarnings("unused")
	private Account() {}	// needed for serialization via jackson
	
	// This constructor takes only the persisted properties.
	public Account(long id, String username, String publicKey, int wins, int losses,
			int streak, int rating) {
		this(id, username, publicKey, wins, losses, streak, rating, 0, 0);
	}
	
	public Account(long id, String username, String publicKey, int wins, int losses,
			int streak, int rating, int rank, double balance) {
		this.id = String.valueOf(id);
		this.username = username;
		this.publicKey = publicKey;
		this.wins = wins;
		this.losses = losses;
		this.streak = streak;
		this.rating = rating;
		this.rank = rank;
		this.balance = balance;
	}
	
	public long numericId() {
		return Long.valueOf(id);
	}
	
}
