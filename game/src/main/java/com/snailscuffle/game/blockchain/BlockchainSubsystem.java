package com.snailscuffle.game.blockchain;

import java.net.URL;

public class BlockchainSubsystem {
	
	private final IgnisArchivalNodeConnection ignisNode;
	
	public BlockchainSubsystem(URL ignisArchivalNodeUrl) {
		this(new IgnisArchivalNodeConnection(ignisArchivalNodeUrl));
	}
	
	BlockchainSubsystem(IgnisArchivalNodeConnection node) {
		ignisNode = node;
	}

}
