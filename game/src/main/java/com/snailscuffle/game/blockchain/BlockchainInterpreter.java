package com.snailscuffle.game.blockchain;

import java.net.URL;

import com.snailscuffle.game.accounts.Accounts;

public class BlockchainInterpreter {
	
	private final IgnisArchivalNodeConnection ignisNode;
	private final Accounts accounts;
	
	public BlockchainInterpreter(URL ignisArchivalNodeUrl, Accounts accounts) {
		this(new IgnisArchivalNodeConnection(ignisArchivalNodeUrl), accounts);
	}
	
	BlockchainInterpreter(IgnisArchivalNodeConnection node, Accounts accounts) {
		ignisNode = node;
		this.accounts = accounts;
	}

}
