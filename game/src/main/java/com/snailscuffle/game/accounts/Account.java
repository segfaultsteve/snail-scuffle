package com.snailscuffle.game.accounts;

import java.io.Serializable;

public class Account implements Serializable {
	
	public long id;
	public String username;
	public String publicKey;
	public int wins;
	public int losses;
	public int rating;
	public int rank;
	public double balance;
	
}
