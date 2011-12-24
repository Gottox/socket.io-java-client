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

// TODO: Auto-generated Javadoc
/**
 * The Class SocketIO.
 */
public class SocketIO {

	/** The callback. */
	private IOCallback callback;

	/** The connection. */
	private IOConnection connection;

	/** The go called. */
	boolean goCalled = false;

	/** The namespace. */
	private String namespace;

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
		this(new URL(url));
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
	 * @throws MalformedURLException
	 *             the malformed url exception
	 */
	public SocketIO(final String url, final IOCallback callback)
			throws MalformedURLException {
		this(url);
		this.go(callback);
	}

	/**
	 * Instantiates a new socket.io connection. The object connects after
	 * calling {@link #go(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 */
	public SocketIO(final URL url) {
		final String origin = url.getProtocol() + "://" + url.getAuthority();
		this.namespace = url.getPath();
		if (this.namespace.equals("/")) {
			this.namespace = "";
		}
		this.connection = IOConnection.create(origin);
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
		this(url);
		this.go(callback);
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
	 * Gets the callback. Internally used
	 * 
	 * @return the callback
	 */
	public IOCallback getCallback() {
		return this.callback;
	}

	/**
	 * Gets the name space. Internally used
	 * 
	 * @return the namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * If you're using {@link #SocketIO(String)} or {@link #SocketIO(URL)}, this
	 * call will start connecting to the server. Make sure, you call this
	 * function only once. A second call on an object will cause a
	 * {@link RuntimeException}.
	 * 
	 * {@link #SocketIO(String, IOCallback)} an
	 * {@link #SocketIO(URL, IOCallback)} will call this function in the
	 * constructor. Don't use this function if your using one of these
	 * constructors
	 * 
	 * @param callback
	 *            the callback
	 */
	public void go(final IOCallback callback) {
		if (this.goCalled) {
			throw new RuntimeException(
					"go() may only be called when using SocketIO constructor with one argument.");
		}
		this.callback = callback;
		this.connection.connect(this);
		this.goCalled = true;
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
