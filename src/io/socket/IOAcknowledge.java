/*
 * socket.io-java-client IOAcknowledge.java
 *
 * Copyright (c) 2012, Enno Boland
 * PROJECT DESCRIPTION
 * 
 * See LICENSE file for more information
 */
package io.socket;

/**
 * The Interface IOAcknowledge.
 */
public interface IOAcknowledge {
	
	/**
	 * Acknowledges a package.
	 *
	 * @param args the args
	 */
	void ack(Object... args);
}
