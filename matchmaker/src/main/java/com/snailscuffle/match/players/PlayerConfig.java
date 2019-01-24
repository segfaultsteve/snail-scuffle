package com.snailscuffle.match.players;

import java.io.Serializable;

import com.snailscuffle.match.InvalidPlayerException;

public class PlayerConfig implements Serializable {
	
	public String name;
	
	public void validate() {
		if (name == null || name.isEmpty()) {
			throw new InvalidPlayerException("Missing player name");
		}
	}
	
}
