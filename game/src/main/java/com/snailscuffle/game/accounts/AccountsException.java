package com.snailscuffle.game.accounts;

public class AccountsException extends Exception {
	
	public AccountsException(String message) {
		super(message);
	}
	
	public AccountsException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
