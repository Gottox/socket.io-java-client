/*
 * socket.io-java-client SocketIOException.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;



public class SocketIOException extends Exception {

	private static final long serialVersionUID = 4965561569568761814L;

	public SocketIOException(String message) {
		super(message);
	}

	public SocketIOException(Exception ex) {
		super(ex);
	}
}
