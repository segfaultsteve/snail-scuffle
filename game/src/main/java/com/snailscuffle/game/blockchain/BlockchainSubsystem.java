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
		(new BlockchainSyncThread(ignisNode, accounts)).start();
	}
	
	public Account getAccountById(String id) throws AccountsException, BlockchainSubsystemException, InterruptedException {
		long numericId = Long.parseUnsignedLong(id);	// for now, assume 64-bit integer form
		Account account = accounts.getById(numericId);
		account.balance = ignisNode.getBalance(account.numericId());
		return account;
	}
	
	public Account getAccountByUsername(String username) throws AccountsException, BlockchainSubsystemException, InterruptedException {
		Account account = accounts.getByUsername(username);
		account.balance = ignisNode.getBalance(account.numericId());
		return account;
	}

	@Override
	public void close() {
		ignisNode.close();
	}
	
}
