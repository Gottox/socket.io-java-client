/*
 * socket.io-java-client WebsocketTransport.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;


import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import de.roderick.weberknecht.WebSocketConnection;
import de.roderick.weberknecht.WebSocketEventHandler;
import de.roderick.weberknecht.WebSocketException;
import de.roderick.weberknecht.WebSocketMessage;

/**
 * The Class WebsocketTransport.
 */
class WebsocketTransport implements IOTransport, WebSocketEventHandler {
	
	WebSocketConnection websocket;
	
	/** Pattern used to replace http:// by ws:// respectively https:// by wss:// */
	private final static Pattern PATTERN_HTTP = Pattern.compile("^http");
	
	/** The String to identify this Transport */
	public static final String TRANSPORT_NAME = "websocket";
	
	/** The IOConnection of this transport. */
	private IOConnection connection;
	
	/**
	 * Creates a new Transport for the given url an {@link IOConnection}.
	 *
	 * @param url the url
	 * @param connection the connection
	 * @return the iO transport
	 */
	public static IOTransport create(URL url, IOConnection connection) {
		URI uri = URI.create(
				PATTERN_HTTP.matcher(url.toString()).replaceFirst("ws")
				+ IOConnection.SOCKET_IO_1 + TRANSPORT_NAME
				+ "/" + connection.getSessionId());

		return new WebsocketTransport(uri, connection);
	}
	
	/**
	 * Instantiates a new websocket transport.
	 *
	 * @param uri the uri
	 * @param connection the connection
	 * @throws WebSocketException 
	 */
	public WebsocketTransport(URI uri, IOConnection connection) {
		try {
			websocket = new WebSocketConnection(uri);
		} catch (WebSocketException e) {
			connection.transportError(e);
			return;
		}
		this.connection = connection;
		websocket.setEventHandler(this);
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#disconnect()
	 */
	@Override
	public void disconnect() {
		try {
			websocket.close();
		} catch (Exception e) {
			connection.transportError(e);
		}
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#canSendBulk()
	 */
	@Override
	public boolean canSendBulk() {
		return false;
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#sendBulk(java.lang.String[])
	 */
	@Override
	public void sendBulk(String[] texts) throws IOException {
		throw new RuntimeException("Cannot send Bulk!");
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#invalidate()
	 */
	@Override
	public void invalidate() {
		connection = null;
	}

	@Override
	public void onClose() {
		if(connection != null)
			connection.transportDisconnected();
	}

	@Override
	public void onMessage(WebSocketMessage arg0) {
		if(connection != null)
			connection.transportMessage(arg0.getText());
	}

	@Override
	public void onOpen() {
		if(connection != null)
			connection.transportConnected();
	}

	@Override
	public void connect() {
		try {
			websocket.connect();
		} catch (WebSocketException e) {
			connection.transportError(e);
		}
	}

	@Override
	public void send(String text) throws Exception {
		websocket.send(text);
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}
}
