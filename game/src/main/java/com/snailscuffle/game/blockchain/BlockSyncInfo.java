package com.snailscuffle.game.blockchain;

public class BlockSyncInfo {
	
	public final int height;
	public final long blockId;
	
	public BlockSyncInfo(int height, long blockId) {
		this.height = height;
		this.blockId = blockId;
	}
	
}
