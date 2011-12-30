/*
 * socket.io-java-client WebsocketTransport.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket.transports;

import io.socket.IOConnection;
import io.socket.IOTransport;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import net.tootallnate.websocket.WebSocketClient;

/**
 * The Class WebsocketTransport.
 */
public class WebsocketTransport extends WebSocketClient implements IOTransport {
	
	/** Pattern used to replace http:// by ws:// respectively https:// by wss:// */
	private final static Pattern PATTERN_HTTP = Pattern.compile("^http");
	
	/** The String which identifies this Transport */
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
	 */
	public WebsocketTransport(URI uri, IOConnection connection) {
		super(uri);
		this.connection = connection;
	}

	/* (non-Javadoc)
	 * @see net.tootallnate.websocket.WebSocketClient#onClose()
	 */
	@Override
	public void onClose() {
		if (connection != null)
			connection.transportDisconnected();
	}

	/* (non-Javadoc)
	 * @see net.tootallnate.websocket.WebSocketClient#onIOError(java.io.IOException)
	 */
	@Override
	public void onIOError(IOException error) {
		if (connection != null)
			connection.transportError(error);
	}

	/* (non-Javadoc)
	 * @see net.tootallnate.websocket.WebSocketClient#onMessage(java.lang.String)
	 */
	@Override
	public void onMessage(String message) {
		if (connection != null)
			connection.transportMessage(message);
	}

	/* (non-Javadoc)
	 * @see net.tootallnate.websocket.WebSocketClient#onOpen()
	 */
	@Override
	public void onOpen() {
		if (connection != null)
			connection.transportConnected();
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#disconnect()
	 */
	@Override
	public void disconnect() {
		try {
			this.close();
		} catch (IOException e) {
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

}
