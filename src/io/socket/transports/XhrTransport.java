/*
 * socket.io-java-client XhrTransport.java
 *
 * Copyright (c) 2012, Enno Boland
 * PROJECT DESCRIPTION
 * 
 * See LICENSE file for more information
 */
package io.socket.transports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.socket.IOConnection;
import io.socket.IOTransport;

/**
 * The Class XhrTransport.
 */
public class XhrTransport implements IOTransport {
	
	/** The String to identify this Transport. */
	public static final String TRANSPORT_NAME = "xhr-polling";
	
	/** The connection. */
	private IOConnection connection;
	
	/** The url. */
	private URL url;
	
	/** The queue holding elements to send. */
	ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();

	/** background thread for managing the server connection. */
	PollThread pollThread = null;
	
	/** Indicates whether the {@link IOConnection} wants us to be connected. */
	private boolean connect;
	
	/** Indicates whether {@link PollThread} is blocked. */
	private boolean blocked;
	
	/**
	 * The Class ReceiverThread.
	 */
	private class PollThread extends Thread {
		
		/**
		 * Instantiates a new receiver thread.
		 */
		public PollThread() {
			super(TRANSPORT_NAME);
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			connection.transportConnected();
			while (isConnect()) {
				try {
					String line;
					URLConnection urlConnection = url.openConnection();
					if (!queue.isEmpty()) {
						urlConnection.setDoOutput(true);
						BufferedWriter output = new BufferedWriter(
								new OutputStreamWriter(
										urlConnection.getOutputStream()));
						while((line = queue.peek()) != null) {
							output.write(line);
							queue.remove();
						}
					}
					else {
						setBlocked(true);
						BufferedReader input = new BufferedReader(
								new InputStreamReader(
										urlConnection.getInputStream()));
						while ((line = input.readLine()) != null) {
							if (connection != null)
								connection.transportMessage(line);
						}
						setBlocked(false);
					}

				} catch (IOException e) {
					if (connection != null && interrupted() == false) {
						connection.transportError(e);
						return;
					}
				}
			}
			connection.transportDisconnected();
		}
	}

	/**
	 * Creates a new Transport for the given url an {@link IOConnection}.
	 *
	 * @param url the url
	 * @param connection the connection
	 * @return the iO transport
	 */
	public static IOTransport create(URL url, IOConnection connection) {
		try {
			URL xhrUrl = new URL(url.toString() + IOConnection.SOCKET_IO_1
					+ TRANSPORT_NAME + "/" + connection.getSessionId());
			return new XhrTransport(xhrUrl, connection);
		} catch (MalformedURLException e) {
			throw new RuntimeException(
					"Malformed Internal url. This should never happen. Please report a bug.",
					e);
		}

	}

	/**
	 * Instantiates a new xhr transport.
	 *
	 * @param url the url
	 * @param connection the connection
	 */
	public XhrTransport(URL url, IOConnection connection) {
		this.connection = connection;
		this.url = url;
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#connect()
	 */
	@Override
	public void connect() {
		this.setConnect(true);
		pollThread = new PollThread();
		pollThread.start();
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#disconnect()
	 */
	@Override
	public void disconnect() {
		this.setConnect(false);
		pollThread.interrupt();
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#send(java.lang.String)
	 */
	@Override
	public void send(String text) throws IOException {
		sendBulk(new String[] { text });
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#canSendBulk()
	 */
	@Override
	public boolean canSendBulk() {
		return true;
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#sendBulk(java.lang.String[])
	 */
	@Override
	public void sendBulk(String[] texts) throws IOException {
		for (String text : texts) {
			queue.add(text);
		}
		if(isBlocked())
			pollThread.interrupt();
	}

	/* (non-Javadoc)
	 * @see io.socket.IOTransport#invalidate()
	 */
	@Override
	public void invalidate() {
		this.connection = null;
	}

	/**
	 * Checks if is connect.
	 *
	 * @return true, if is connect
	 */
	private synchronized boolean isConnect() {
		return connect;
	}

	/**
	 * Sets the connect.
	 *
	 * @param connect the new connect
	 */
	private synchronized void setConnect(boolean connect) {
		this.connect = connect;
	}

	/**
	 * Checks if is blocked.
	 *
	 * @return true, if is blocked
	 */
	private synchronized boolean isBlocked() {
		return blocked;
	}

	/**
	 * Sets the blocked.
	 *
	 * @param blocked the new blocked
	 */
	private synchronized void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
}
