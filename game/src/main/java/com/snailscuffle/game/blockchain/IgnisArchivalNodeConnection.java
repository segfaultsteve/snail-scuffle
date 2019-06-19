package com.snailscuffle.game.blockchain;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

//import org.eclipse.jetty.client.HttpClient;

class IgnisArchivalNodeConnection implements Closeable {
	
	//private final HttpClient connection;
	
	IgnisArchivalNodeConnection(URL ignisArchivalNodeUrl) {
		
	}

	@Override
	public void close() throws IOException {
		//connection.close();
	}

}
