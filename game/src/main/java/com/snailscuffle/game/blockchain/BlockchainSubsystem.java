package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.net.URL;

import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.AccountNotFoundException;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsQuery;

public class BlockchainSubsystem implements Closeable {
	
	private final IgnisArchivalNodeConnection ignisNode;
	private final Accounts accounts;
	
	public BlockchainSubsystem(URL ignisArchivalNodeUrl, Accounts accounts) throws BlockchainSubsystemException {
		this(new IgnisArchivalNodeConnection(ignisArchivalNodeUrl), accounts);
	}
	
	public BlockchainSubsystem(IgnisArchivalNodeConnection node, Accounts accounts) {
		ignisNode = node;
		this.accounts = accounts;
	}
	
	public Account getAccount(AccountsQuery query) throws AccountNotFoundException, BlockchainSubsystemException {
		Account account = accounts.get(query);
		account.balance = ignisNode.getBalanceOf(account.numericId());
		return account;
	}

	@Override
	public void close() {
		ignisNode.close();
	}

}
