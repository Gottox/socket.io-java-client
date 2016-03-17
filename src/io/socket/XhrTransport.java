/*
 * socket.io-java-client XhrTransport.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * The Class XhrTransport.
 */
class XhrTransport implements IOTransport {

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

	HttpURLConnection urlConnection;

	/**
	 * The Class ReceiverThread.
	 */
	private class PollThread extends Thread {

		private static final String CHARSET = "UTF-8";

		/**
		 * Instantiates a new receiver thread.
		 */
		public PollThread() {
			super(TRANSPORT_NAME);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			connection.transportConnected();
			while (isConnect()) {
				try {
					String line;
					URL url = new URL(XhrTransport.this.url.toString() + "?t="
							+ System.currentTimeMillis());
					urlConnection = (HttpURLConnection) url.openConnection();
					SSLContext context = IOConnection.getSslContext();
					if(urlConnection instanceof HttpsURLConnection && context != null) {
						((HttpsURLConnection)urlConnection).setSSLSocketFactory(context.getSocketFactory());
					}
					if (!queue.isEmpty()) {
						urlConnection.setDoOutput(true);
						OutputStream output = urlConnection.getOutputStream();
						if (queue.size() == 1) {
							line = queue.poll();
							output.write(line.getBytes(CHARSET));
						} else {
							Iterator<String> iter = queue.iterator();
							while (iter.hasNext()) {
								String junk = iter.next();
								line = IOConnection.FRAME_DELIMITER + junk.length()
										+ IOConnection.FRAME_DELIMITER + junk;
								output.write(line.getBytes(CHARSET));
								iter.remove();
							}
						}
						output.close();
						InputStream input = urlConnection.getInputStream();
						byte[] buffer = new byte[1024];
						while(input.read(buffer) > 0) {
						}
						input.close();
					} else {
						setBlocked(true);
						InputStream plainInput = urlConnection.getInputStream();
						BufferedReader input = new BufferedReader(
								new InputStreamReader(plainInput, CHARSET));
						while ((line = input.readLine()) != null) {
							if (connection != null)
								connection.transportData(line);
						}
						setBlocked(false);
					}

				} catch (IOException e) {
					if (connection != null && interrupted() == false) {
						connection.transportError(e);
						return;
					}
				}
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
			}
			connection.transportDisconnected();
		}
	}

	/**
	 * Creates a new Transport for the given url an {@link IOConnection}.
	 * 
	 * @param url
	 *            the url
	 * @param connection
	 *            the connection
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
	 * @param url
	 *            the url
	 * @param connection
	 *            the connection
	 */
	public XhrTransport(URL url, IOConnection connection) {
		this.connection = connection;
		this.url = url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOTransport#connect()
	 */
	@Override
	public void connect() {
		this.setConnect(true);
		pollThread = new PollThread();
		pollThread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOTransport#disconnect()
	 */
	@Override
	public void disconnect() {
		this.setConnect(false);
		pollThread.interrupt();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOTransport#send(java.lang.String)
	 */
	@Override
	public void send(String text) throws IOException {
		sendBulk(new String[] { text });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOTransport#canSendBulk()
	 */
	@Override
	public boolean canSendBulk() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOTransport#sendBulk(java.lang.String[])
	 */
	@Override
	public void sendBulk(String[] texts) throws IOException {
		queue.addAll(Arrays.asList(texts));
		if (isBlocked()) {
			pollThread.interrupt();
			urlConnection.disconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
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
	 * @param connect
	 *            the new connect
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
	 * @param blocked
	 *            the new blocked
	 */
	private synchronized void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	@Override
	public String getName() {
		return TRANSPORT_NAME;
	}
}
