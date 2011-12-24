/*
 * socket.io-java-client IOCallback.java
 *
 * Copyright (c) 2011, ${author}
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import org.json.JSONObject;

/**
 * The Interface IOCallback. A callback interface to SocketIO
 */
public interface IOCallback {
	
	/**
	 * On disconnect. Called when the socket disconnects and there are no further attempts to reconnect
	 */
	void onDisconnect();
	
	/**
	 * On connect. Called when the socket becomes ready so it is now able to receive data
	 */
	void onConnect();
	
	/**
	 * On message. Called when theserver sends String data
	 *
	 * @param data the data
	 */
	void onMessage(String data);
	
	/**
	 * On message. Called when the server sends JSON data
	 *
	 * @param json JSON object sent by server
	 */
	void onMessage(JSONObject json);
	
	/**
	 * On [Event]. Called when server calls emit()
	 *
	 * @param event Name of the event
	 * @param args arguments of the event
	 */
	void on(String event, JSONObject... args);
	
	/**
	 * On error. Called when socket is in an undefined state. No reconnect attempts will be made.
	 *
	 * @param socketIOException the last exception describing the error
	 */
	void onError(SocketIOException socketIOException);
}
