package com.snailscuffle.game.accounts;

public class AccountNotFoundException extends AccountsException {
	
	public AccountNotFoundException(String message) {
		super(message);
	}
	
	public AccountNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
