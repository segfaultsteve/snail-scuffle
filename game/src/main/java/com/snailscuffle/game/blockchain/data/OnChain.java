package com.snailscuffle.game.blockchain.data;

public class OnChain<T> {
	
	public final long blockId;
	public final int height;
	public final int transactionIndex;
	public final long sendingAccount;
	public final long receivingAccount;
	public final T data;
	
	public OnChain(long blockId, int height, int transactionIndex, long sendingAccount, long receivingAccount, T data) {
		this.blockId = blockId;
		this.height = height;
		this.transactionIndex = transactionIndex;
		this.sendingAccount = sendingAccount;
		this.receivingAccount = receivingAccount;
		this.data = data;
	}
	
}
