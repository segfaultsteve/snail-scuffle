package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.net.URL;

import com.snailscuffle.game.accounts.Account;
import com.snailscuffle.game.accounts.AccountNotFoundException;
import com.snailscuffle.game.accounts.Accounts;
import com.snailscuffle.game.accounts.AccountsException;
import com.snailscuffle.game.blockchain.data.SnailScuffleMessage;
import com.snailscuffle.game.tx.TransactionStatus;
import com.snailscuffle.game.tx.UnsignedTransaction;

public class BlockchainSubsystem implements Closeable {
	
	private final IgnisArchivalNodeConnection ignisNode;
	private final Accounts accounts;
	private final BlockchainSyncThread blockchainSyncThread;
	
	public BlockchainSubsystem(URL ignisArchivalNodeUrl, Accounts accounts, int recentBattlesDepth) throws BlockchainSubsystemException {
		this(new IgnisArchivalNodeConnection(ignisArchivalNodeUrl), accounts, recentBattlesDepth);
	}
	
	public BlockchainSubsystem(IgnisArchivalNodeConnection node, Accounts accounts, int recentBattlesDepth) {
		ignisNode = node;
		this.accounts = accounts;
		blockchainSyncThread = new BlockchainSyncThread(ignisNode, accounts, recentBattlesDepth);
		
		blockchainSyncThread.start();
	}
	
	public Account getAccountById(String id) throws AccountsException, BlockchainSubsystemException, InterruptedException {
		try {
			long numericId = Long.parseUnsignedLong(id);	// for now, assume 64-bit integer form
			
			Account account = null;
			try {
				account = accounts.getById(numericId);
			} catch (AccountNotFoundException e) {
				account = new Account(numericId, "", "", 0, 0, 0, 0, 0, 0);
			}
			
			account.balance = ignisNode.getBalance(id);
			return account;
		} catch (NumberFormatException e) {
			throw new AccountNotFoundException("'" + id + "' is not a valid account ID");
		} catch (BlockchainDataNotFoundException e) {
			throw new AccountNotFoundException("Account " + id + " not found: " + e.getMessage());
		}
	}
	
	public Account getAccountByUsername(String username) throws AccountsException, BlockchainSubsystemException, InterruptedException {
		Account account = accounts.getByUsername(username);
		account.balance = ignisNode.getBalance(account.id);
		return account;
	}
	
	public UnsignedTransaction createNewAccountTransaction(String publicKey, String username) throws AccountsException, BlockchainSubsystemException, InterruptedException {
		try {
			Account account = accounts.getByUsername(username);
			if (!account.publicKey.equalsIgnoreCase(publicKey)) {
				throw new AccountsException("Another account has already registered the username '" + username + "'");
			}
		} catch (AccountNotFoundException e) { }
		
		return ignisNode.createNewAccountTransaction(publicKey, username);
	}
	
	public UnsignedTransaction createArbitraryMessageTransaction(String publicKey, String recipient, SnailScuffleMessage message) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		return ignisNode.createArbitraryMessageTransaction(publicKey, recipient, message);
	}
	
	public TransactionStatus broadcastTransaction(String txJson) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		return ignisNode.broadcastTransaction(txJson);
	}
	
	public TransactionStatus getTransactionStatus(String fullHash) throws IgnisNodeCommunicationException, BlockchainSubsystemException, InterruptedException {
		return ignisNode.getTransactionStatus(fullHash);
	}
	
	@Override
	public void close() {
		blockchainSyncThread.interrupt();
		ignisNode.close();
	}
	
}
