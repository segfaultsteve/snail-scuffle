package com.snailscuffle.game;

public class Constants {
	
	public static final int INITIAL_SYNC_HEIGHT = 809258;
	public static final long INITIAL_SYNC_BLOCK_ID = Long.parseUnsignedLong("18040855095686571912");
	public static final int SYNC_BACKTRACK = 30;
	public static final int RECENT_BATTLES_DEPTH = 14400;	// keep roughly 10 days' worth of battles in case of fork
	
	public static final int PROTOCOL_MAJOR_VERSION = 0;
	public static final int PROTOCOL_MINOR_VERSION = 1;
	
	public static final int MAX_BLOCKS_PER_ROUND = 4;
	public static final int MAX_BLOCKS_AFTER_HASHES_COMMITTED = 2;	// must broadcast battle plan within this many blocks of its hash
	
	public static final int INITIAL_RATING = 1000;
	public static final int MAX_RATING_CHANGE = 32;
	public static final int LOGISTIC_CURVE_DIVISOR = 100;
	
}
