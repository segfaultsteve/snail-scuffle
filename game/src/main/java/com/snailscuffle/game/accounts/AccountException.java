package com.snailscuffle.game.accounts;

public class AccountException extends Exception {
	
	public AccountException(String message) {
		super(message);
	}
	
	public AccountException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
