/*
 * socket.io-java-client SocketIO.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

/**
 * The Class SocketIO.
 */
public class SocketIO {

	/** callback of this Socket. */
	private IOCallback callback;

	/** connection of this Socket. */
	private IOConnection connection;

	/** namespace. */
	private String namespace;

	private URL url;
	
	public SocketIO() {
		
	}
	
	/**
	 * Instantiates a new socket.io object. The object connects after calling
	 * {@link #go(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 * @throws MalformedURLException
	 *             the malformed url exception
	 */
	public SocketIO(final String url) throws MalformedURLException {
		connect(url, null);
	}

	/**
	 * Instantiates a new socket.io object and connects to the given url.
	 * Do not call any of the {@link #connect(URL, IOCallback)} methods afterwards.
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 * @throws MalformedURLException
	 *             the malformed url exception
	 */
	public SocketIO(final String url, final IOCallback callback)
			throws MalformedURLException {
		connect(url, callback);
	}
	
	/**
	 * Connects to a given host with a given callback.
	 */
	public void connect(final String url, final IOCallback callback) throws MalformedURLException {
		connect(new URL(url), callback);
	}

	/**
	 * Instantiates a new socket.io connection. The object connects after
	 * calling {@link #go(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 */
	public SocketIO(final URL url) {
		connect(url, null);
	}
	
	/**
	 * Instantiates a new socket.io object and connects to the given url.
	 * calling {@link #go(IOCallback)} afterwards results in a
	 * {@link RuntimeException}
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 */
	public SocketIO(final URL url, final IOCallback callback) {
		connect(url, callback);
	}
	
	public void connect(URL url, IOCallback callback) {
		if(url != null) {
			this.url = url;
			final String origin = url.getProtocol() + "://" + url.getAuthority();
			this.namespace = url.getPath();
			if (this.namespace.equals("/")) {
				this.namespace = "";
			}
			this.connection = IOConnection.create(origin);
		}
		if(callback != null) {
			this.callback = callback;
		}
		if(this.callback != null && this.url != null) {
			this.connection.connect(this);
		}
	}
	
	public void connect(IOCallback callback) {
		connect((URL)null, callback);
	}

	/**
	 * Emits an event to the Socket.IO server. If the connection is not
	 * established, the call will be buffered and sent as soon as it is
	 * possible.
	 * 
	 * @param event
	 *            the event name
	 * @param args
	 *            the arguments
	 */
	public void emit(final String event, final JSONObject... args) {
		this.connection.emit(this, event, args);
	}

	/**
	 * Gets the callback. Internally used.
	 * 
	 * @return the callback
	 */
	public IOCallback getCallback() {
		return this.callback;
	}

	/**
	 * Gets the namespace. Internally used.
	 * 
	 * @return the namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Send JSON data to the Socket.io server.
	 * 
	 * @param json
	 *            the JSON object
	 */
	public void send(final JSONObject json) {
		this.connection.send(this, json);
	}

	/**
	 * Send String data to the Socket.io server.
	 * 
	 * @param message
	 *            the message String
	 */
	public void send(final String message) {
		this.connection.send(this, message);
	}

	/**
	 * Disconnect the socket.
	 */
	public void disconnect() {
		this.connection.disconnect(this);
	}
	
	/**
	 * Triggers the transport to reconnect.
	 * 
	 * Works only if IOConnection thinks, we are connected.
	 */
	public void reconnect() {
		this.connection.reconnect();
	}
	
	public boolean isConnected() {
		return this.connection.isConnected();
	}
}
