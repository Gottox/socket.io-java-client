/*
 * socket.io-java-client SocketIO.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.SSLContext;

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

	/** Used for setting header during handshaking. */
	private Properties headers = new Properties();

	private URL url;

	/**
	 * Instantiates a new socket.io connection. The object connects after
	 * calling {@link #connect(URL, IOCallback)} or
	 * {@link #connect(String, IOCallback)}
	 */
	public SocketIO() {

	}

	/**
	 * Instantiates a new socket.io connection. The object connects after
	 * calling {@link #connect(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 * @throws MalformedURLException
	 *             the malformed url exception
	 */
	public SocketIO(final String url) throws MalformedURLException {
		if (url == null)
			throw new RuntimeException("url may not be null.");
		setAndConnect(new URL(url), null);
	}

	/**
	 * Instantiates a new socket.io connection and sets the request headers used
	 * while connecting the first time for authorizing. The object connects
	 * after calling {@link #connect(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 * @param headers
	 *            the {@link Properties headers} used while handshaking
	 * @throws MalformedURLException
	 *             the malformed url exception
	 */
	public SocketIO(final String url, Properties headers)
			throws MalformedURLException {
		if (url == null)
			throw new RuntimeException("url may not be null.");

		if (headers != null)
			this.headers = headers;

		setAndConnect(new URL(url), null);
	}

	/**
	 * Instantiates a new socket.io object and connects to the given url. Do not
	 * call any of the connect() methods afterwards.
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
	 * Instantiates a new socket.io object and connects to the given url. Do not
	 * call any of the connect() methods afterwards.
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 */
	public SocketIO(final URL url, final IOCallback callback) {
		if (setAndConnect(url, callback) == false)
			throw new RuntimeException("url and callback may not be null.");
	}

	/**
	 * Instantiates a new socket.io connection. The object connects after
	 * calling {@link #connect(IOCallback)}
	 * 
	 * @param url
	 *            the url
	 */
	public SocketIO(final URL url) {
		setAndConnect(url, null);
	}
	
	/**
	 * Set the socket factory used for SSL connections.
	 * @param socketFactory
	 */
	public static void setDefaultSSLSocketFactory(SSLContext sslContext) {
		IOConnection.setSslContext(sslContext);
	}
	
	/**
	 * connects to supplied host using callback. Do only use this method if you
	 * instantiate {@link SocketIO} using {@link #SocketIO()}.
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 */
	public void connect(final String url, final IOCallback callback)
			throws MalformedURLException {
		if (setAndConnect(new URL(url), callback) == false) {
			if (url == null || callback == null)
				throw new RuntimeException("url and callback may not be null.");
			else
				throw new RuntimeException(
						"connect(String, IOCallback) can only be invoked after SocketIO()");
		}
	}

	/**
	 * connects to supplied host using callback. Do only use this method if you
	 * instantiate {@link SocketIO} using {@link #SocketIO()}.
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 */
	public void connect(URL url, IOCallback callback) {
		if (setAndConnect(url, callback) == false) {
			if (url == null || callback == null)
				throw new RuntimeException("url and callback may not be null.");
			else
				throw new RuntimeException(
						"connect(URL, IOCallback) can only be invoked after SocketIO()");
		}
	}

	/**
	 * connects to an already set host. Do only use this method if you
	 * instantiate {@link SocketIO} using {@link #SocketIO(String)} or
	 * {@link #SocketIO(URL)}.
	 * 
	 * @param callback
	 *            the callback
	 */
	public void connect(IOCallback callback) {
		if (setAndConnect(null, callback) == false) {
			if (callback == null)
				throw new RuntimeException("callback may not be null.");
			else if (this.url == null)
				throw new RuntimeException(
						"connect(IOCallback) can only be invoked after SocketIO(String) or SocketIO(URL)");
		}
	}

	/**
	 * Sets url and callback and initiates connecting if both are present
	 * 
	 * @param url
	 *            the url
	 * @param callback
	 *            the callback
	 * @return true if connecting has been initiated, false if not
	 */
	private boolean setAndConnect(URL url, IOCallback callback) {
		if(this.connection != null)
			throw new RuntimeException("You can connect your SocketIO instance only once. Use a fresh instance instead.");
		if ((this.url != null && url != null)
				|| (this.callback != null && callback != null))
			return false;
		if (url != null) {
			this.url = url;
		}
		if (callback != null) {
			this.callback = callback;
		}
		if (this.callback != null && this.url != null) {
			final String origin = this.url.getProtocol() + "://"
					+ this.url.getAuthority();
			this.namespace = this.url.getPath();
			if (this.namespace.equals("/")) {
				this.namespace = "";
			}
			this.connection = IOConnection.register(origin, this);
			return true;
		}
		return false;
	}

	/**
	 * Emits an event to the Socket.IO server. If the connection is not
	 * established, the call will be buffered and sent as soon as it is
	 * possible.
	 * 
	 * @param event
	 *            the event name
	 * @param args
	 *            arguments. can be any argument {@link org.json.JSONArray#put(Object)} can take. 
	 */
	public void emit(final String event, final Object... args) {
		this.connection.emit(this, event, null, args);
	}

	/**
	 * Emits an event to the Socket.IO server. If the connection is not
	 * established, the call will be buffered and sent as soon as it is
	 * possible.
	 * 
	 * @param event
	 *            the event name
	 * @param ack
	 *            an acknowledge implementation
	 * @param args
	 *            arguments. can be any argument {@link org.json.JSONArray#put(Object)} can take. 
	 */
	public void emit(final String event, IOAcknowledge ack,
			final Object... args) {
		this.connection.emit(this, event, ack, args);
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
		this.connection.send(this, null, json);
	}

	/**
	 * Send JSON data to the Socket.io server.
	 * 
	 * @param ack
	 *            an acknowledge implementation
	 * @param json
	 *            the JSON object
	 */
	public void send(IOAcknowledge ack, final JSONObject json) {
		this.connection.send(this, ack, json);
	}

	/**
	 * Send String data to the Socket.io server.
	 * 
	 * @param message
	 *            the message String
	 */
	public void send(final String message) {
		this.connection.send(this, null, message);
	}

	/**
	 * Send JSON data to the Socket.io server.
	 * 
	 * @param ack
	 *            an acknowledge implementation
	 * @param message
	 *            the message String
	 */
	public void send(IOAcknowledge ack, final String message) {
		this.connection.send(this, ack, message);
	}

	/**
	 * Disconnect the socket.
	 */
	public void disconnect() {
		this.connection.unregister(this);
	}

	/**
	 * Triggers the transport to reconnect.
	 * 
	 * This had become useful on some android devices which do not shut down
	 * tcp-connections when switching from HSDPA to Wifi
	 */
	public void reconnect() {
		this.connection.reconnect();
	}

	/**
	 * Returns, if a connection is established at the moment
	 * 
	 * @return true if a connection is established, false if the transport is
	 *         not connected or currently connecting
	 */
	public boolean isConnected() {
		return this.connection != null && this.connection.isConnected();
	}
	
	/**
	 * Returns the name of the used transport
	 * 
	 * @return the name of the currently used transport
	 */
	public String getTransport() {
		IOTransport transport = this.connection.getTransport();
		return transport != null ? transport.getName() : null;
	}

	/**
	 * Returns the headers used while handshaking. These Properties are not
	 * necessarily the ones set by {@link #addHeader(String, String)} or
	 * {@link #SocketIO(String, Properties)} but the ones used for the
	 * handshake.
	 * 
	 * @return the headers used while handshaking
	 */
	public Properties getHeaders() {
		return headers;
	}

	/**
	 * Sets the headers used while handshaking. Internally used. Use
	 * {@link #SocketIO(String, Properties)} or
	 * {@link #addHeader(String, String)} instead.
	 * 
	 * @param headers
	 *            the headers used while handshaking
	 */
	void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * Adds an header to the {@link #headers}
	 * @return SocketIO.this for daisy chaining.
	 */
	public SocketIO addHeader(String key, String value) {
		if (this.connection != null)
			throw new RuntimeException(
					"You may only set headers before connecting.\n"
							+ " Try to use new SocketIO().addHeader(key, value).connect(host, callback) "
							+ "instead of SocketIO(host, callback).addHeader(key, value)");
		this.headers.setProperty(key, value);
		return this;
	}

	/**
	 * Returns the header value
	 * 
	 * @return the header value or {@code null} if not present
	 */
	public String getHeader(String key) {
		if (this.headers.contains(key))
			return this.headers.getProperty(key);
		return null;
	}
}
