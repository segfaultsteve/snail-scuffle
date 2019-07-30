package com.snailscuffle.game.blockchain;

class DisqualifyingMessageException extends IllegalMessageException {
	
	final long accountId;
	
	DisqualifyingMessageException(long accountId, String message) {
		super(message);
		this.accountId = accountId;
	}
	
}
