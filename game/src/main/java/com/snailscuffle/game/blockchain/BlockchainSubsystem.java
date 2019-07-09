package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.net.URL;

import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.accounts.Accounts;

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
	
	public Account getAccountById(String id) throws AccountsException, BlockchainSubsystemException {
		Account account = accounts.getById(id);
		account.balance = ignisNode.getBalanceOf(account.numericId());
		return account;
	}
	
	public Account getAccountByUsername(String username) throws AccountsException, BlockchainSubsystemException {
		Account account = accounts.getByUsername(username);
		account.balance = ignisNode.getBalanceOf(account.numericId());
		return account;
	}

	@Override
	public void close() {
		ignisNode.close();
	}

}
