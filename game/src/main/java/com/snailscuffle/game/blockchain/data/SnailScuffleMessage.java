package com.snailscuffle.game.blockchain.data;

import java.io.Serializable;

import com.snailscuffle.game.Constants;

public class SnailScuffleMessage implements Serializable {
	
	public int protocolMajorVersion = Constants.PROTOCOL_MAJOR_VERSION;
	public int protocolMinorVersion = Constants.PROTOCOL_MINOR_VERSION;
	
}
