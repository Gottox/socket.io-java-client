/*
 * socket.io-java-client IOConnection.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import io.socket.transports.WebsocketTransport;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.jws.Oneway;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class IOConnection.
 */
public class IOConnection {

	/** Socket.io path. */
	public static final String SOCKET_IO_1 = "/socket.io/1/";

	/** All available connections. */
	private static HashMap<String, IOConnection> connections = new HashMap<String, IOConnection>();

	/** The url for this connection. */
	private URL url;

	/** The transport for this connection. */
	private IOTransport transport;

	/** The connection timeout. */
	private int connectTimeout = 10000;

	/** The session id of this connection. */
	private String sessionId;

	/** The heartbeat timeout. Set by the server */
	private long heartbeatTimeout;

	/** The closing timeout. Set By the server */
	private long closingTimeout;

	/** The protocols supported by the server. */
	private List<String> protocols;

	/** The output buffer used to cache messages while (re-)connecting. */
	private LinkedList<String> outputBuffer = new LinkedList<String>();

	/** The sockets of this connection. */
	private HashMap<String, SocketIO> sockets = new HashMap<String, SocketIO>();

	/** The connect thread handling authentication. */
	private Thread connectThread = null;

	/** Indicates if a connection is currently available. */
	private boolean connected = false;

	/** true if the connection should be shut down immediately. */
	private boolean wantToDisconnect = false;

	/**
	 * The first socket to be connected. the socket.io server does not send a
	 * connected response to this one.
	 */
	private SocketIO firstSocket = null;

	/** The reconnect timer. IOConnect waits a second before trying to reconnect */
	private Timer reconnectTimer;

	/**
	 * The reconnect timeout timer. Aborts all reconnect attempts after this
	 * timeout.
	 */
	private Timer reconnectTimeoutTimer;

	/** A String representation of {@link #url}. */
	private String urlStr;

	/**
	 * The last occurred exception, which will be given to the user if
	 * IOConnection gives up.
	 */
	private Exception lastException;

	/**
	 * The next ID to use
	 */
	private int nextId = 1;

	/**
	 * Acknowledges
	 */
	HashMap<Integer, IOAcknowledge> acknowledge = new HashMap<Integer, IOAcknowledge>();

	/** true if there's already a keepalive in {@link #outputBuffer}. */
	private boolean keepAliveInQueue;

	/**
	 * The Class ReconnectTimeoutTask. Handles the abortion of reconnect
	 * attempts
	 */
	private final class ReconnectTimeoutTask extends TimerTask {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			wantToDisconnect = true;
			if (reconnectTimer != null) {
				reconnectTimer.cancel();
				reconnectTimer = null;
			}
			error(new SocketIOException(
					"Error while reconnecting. Either the server is down or your Network connection is broken.",
					lastException));
			cleanup();
		}
	}

	/**
	 * The Class ReconnectTask. Handles reconnect attempts
	 */
	private final class ReconnectTask extends TimerTask {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			connect();
			if (!keepAliveInQueue) {
				sendPlain("2::");
				keepAliveInQueue = true;
			}
		}
	}

	/**
	 * The Class ConnectThread. Handles connecting to the server with an
	 * {@link IOTransport}
	 */
	private class ConnectThread extends Thread {

		/**
		 * Tries handshaking if necessary and connects with corresponding
		 * transport afterwards.
		 */
		public void run() {
			if (transport != null)
				return;

			try {
				if (sessionId == null) {
					System.out.println("Handshaking");
					handshake();
				} else
					System.out.println("Try to reconnect");
				connectTransport();
				System.out.println("Transport connected.");

			} catch (IOException e) {
				error(new SocketIOException(
						"Error while handshaking. This could be caused by insufficient rights or network problems.",
						e));
			}
			connectThread = null;
		}

		/**
		 * Handshake.
		 * 
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		private void handshake() throws IOException {
			URL url = new URL(IOConnection.this.url.toString() + SOCKET_IO_1);
			String response;
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(connectTimeout);

			InputStream stream = connection.getInputStream();
			Scanner in = new Scanner(stream);
			response = in.nextLine();
			if (response.contains(":")) {
				String[] data = response.split(":");
				heartbeatTimeout = Long.parseLong(data[1]) * 1000;
				closingTimeout = Long.parseLong(data[2]) * 1000;
				protocols = Arrays.asList(data[3].split(","));
				sessionId = data[0];
			}
		}

		/**
		 * Connect transport.
		 */
		private void connectTransport() {
			if (protocols.contains(WebsocketTransport.TRANSPORT_NAME))
				transport = WebsocketTransport.create(url, IOConnection.this);
			else
				error(new SocketIOException(
						"Server supports no available transports. You should reconfigure the server to support a available transport"));

			transport.connect();
		}

	};

	/**
	 * Creates a new connection or returns the corresponding one.
	 * 
	 * @param origin
	 *            the origin
	 * @return a IOConnection object
	 */
	static public IOConnection create(String origin) {
		IOConnection connection = connections.get(origin);
		if (connection == null) {
			connection = new IOConnection(origin);
			connections.put(origin, connection);
		}
		return connection;
	}

	/**
	 * Creates a new {@link IOAcknowledge} instance which sends its arguments
	 * back to the server
	 * 
	 * @param message
	 * @param socket
	 * @return a ne {@link IOAcknowledge} instance
	 */
	private IOAcknowledge remoteAcknowledge(IOMessage message) {
		if (message.getId().endsWith("+") == false)
			return null;
		final String id = message.getId();
		final String endPount = message.getEndpoint();
		return new IOAcknowledge() {
			@Override
			public void ack(Object... args) {
				JSONArray array = new JSONArray();
				for (Object o : args) {
					try {
						array.put(o == null ? JSONObject.NULL : o);
					} catch (Exception e) {
						error(new SocketIOException(
								"You can only put values in IOAcknowledge.ack() which can be handled by JSONArray.put()",
								e));
					}

				}
				IOMessage ackMsg = new IOMessage(IOMessage.TYPE_ACK, endPount, id
						+ array.toString());
				sendPlain(ackMsg.toString());
			}
		};
	}

	private void synthesizeAck(IOMessage message, IOAcknowledge ack) {
		if (ack != null) {
			int id = nextId++;
			acknowledge.put(id, ack);
			message.setId(id + "+");
		}
	}

	/**
	 * Instantiates a new IOConnection.
	 * 
	 * @param url
	 *            the URL
	 */
	private IOConnection(String url) {
		try {
			this.url = new URL(url);
			this.urlStr = url;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		connect();
	}

	/**
	 * Populates an error to the connected {@link IOCallback}s and shuts down
	 * 
	 * @param e
	 *            an exception
	 */
	protected void error(SocketIOException e) {
		if (sockets.size() == 0) {
			System.err
					.println("This is a fallback handling. This error was thrown when no SocketIO was connected which could handle this Exception.");
			e.printStackTrace();
		}
		for (SocketIO socket : sockets.values()) {
			socket.getCallback().onError(e);
		}
		cleanup();
	}

	/**
	 * Connect to the server.
	 */
	private void connect() {
		if (connectThread == null) {
			connectThread = new ConnectThread();
			connectThread.start();
		}
	}

	/**
	 * Connects a socket to the IOConnection
	 * 
	 * @param socket
	 *            the socket to be connected
	 */
	public void connect(SocketIO socket) {
		if (sockets.size() == 0)
			firstSocket = socket;
		String namespace = socket.getNamespace();
		if (sockets.containsKey(namespace))
			socket.getCallback()
					.onError(
							new SocketIOException(
									"Namespace '"
											+ namespace
											+ "' is already registered. Do not try to connect twice to the same url."));
		else
			sockets.put(namespace, socket);
		if (socket.getNamespace() != "") {
			IOMessage message = new IOMessage(IOMessage.TYPE_CONNECT,
					namespace, "");
			sendPlain(message.toString());
		}
	}

	/**
	 * Cleanup. IOConnection is not usable after this calling this.
	 */
	private void cleanup() {
		System.out.println("Clean up");
		wantToDisconnect = true;
		if (transport != null)
			transport.disconnect();
		sockets.clear();
		connections.remove(urlStr);
		if (reconnectTimer != null) {
			reconnectTimer.cancel();
			reconnectTimer = null;
		}
		if (reconnectTimeoutTimer != null) {
			reconnectTimeoutTimer.cancel();
			reconnectTimeoutTimer = null;
		}
	}

	/**
	 * Disconnect a socket from the IOConnection. Shuts down this IOConnection
	 * if no further connections are available for this IOConnection.
	 * 
	 * @param socket
	 *            the socket to be shut down
	 */
	public void disconnect(SocketIO socket) {
		sendPlain("0::" + socket.getNamespace());
		sockets.remove(socket.getNamespace());
		socket.getCallback().onDisconnect();

		if (sockets.size() == 0) {
			cleanup();
		}
	}

	/**
	 * Sends a plain message to the {@link IOTransport}.
	 * 
	 * @param text
	 *            the Text to be send.
	 */
	private void sendPlain(String text) {
		synchronized (outputBuffer) {
			if (connected)
				try {
					System.out.println("> " + text);
					transport.send(text);
				} catch (IOException e) {
					System.out.println("IOEx: saving");
					outputBuffer.add(text);
				}
			else {
				System.out.println("Not online: saving");
				outputBuffer.add(text);
			}
		}
	}

	/**
	 * Invalidates an {@link IOTransport}, used for forced reconnecting.
	 */
	private void invalidateTransport() {
		transport.invalidate();
		transport = null;
	}

	/**
	 * {@link IOTransport} calls this when a connection is established.
	 */
	public void transportConnected() {
		connected = true;
		if (reconnectTimeoutTimer != null) {
			reconnectTimeoutTimer.cancel();
		}
		if (firstSocket != null) {
			firstSocket.getCallback().onConnect();
			firstSocket = null;
		}
		synchronized (outputBuffer) {
			LinkedList<String> outputBuffer = this.outputBuffer;
			this.outputBuffer = new LinkedList<String>();
			if (transport.canSendBulk())
				try {
					transport.sendBulk(outputBuffer
							.toArray(new String[outputBuffer.size()]));
				} catch (IOException e) {
					this.outputBuffer = outputBuffer;
				}
			this.keepAliveInQueue = false;
			for (String text : outputBuffer) {
				sendPlain(text);
			}
		}
	}

	/**
	 * {@link IOTransport} calls this when a connection has been shut down.
	 */
	public void transportDisconnected() {
		System.out.println("Disconnect");
		this.lastException = null;
		connected = false;
		reconnect();
	}

	/**
	 * {@link IOTransport} calls this, when an exception has occured and the
	 * transport is not usable anymore.
	 * 
	 * @param error
	 *            the error
	 */
	public void transportError(Exception error) {
		System.out.println("Error");
		this.lastException = error;
		connected = false;
		reconnect();
	}

	/**
	 * {@link IOTransport} calls this, when a message has been received.
	 * 
	 * @param text
	 *            the text
	 */
	public void transportMessage(String text) {
		System.out.println("< " + text);
		IOMessage message = new IOMessage(text);
		switch (message.getType()) {
		case IOMessage.TYPE_DISCONNECT:
			if (message.getEndpoint().equals("")) {
				for (SocketIO socket : sockets.values()) {
					socket.getCallback().onDisconnect();
				}
			} else
				try {
					findCallback(message).onDisconnect();
				} catch (Exception e) {
					error(new SocketIOException(
							"Exception was thrown in onDisconnect()", e));
				}
			break;
		case IOMessage.TYPE_CONNECT:
			try {
				findCallback(message).onConnect();
			} catch (Exception e) {
				error(new SocketIOException(
						"Exception was thrown in onConnect()", e));
			}
			break;
		case IOMessage.TYPE_HEARTBEAT:
			sendPlain("2::");
			break;
		case IOMessage.TYPE_MESSAGE:
			try {
				findCallback(message).onMessage(message.getData(),
						remoteAcknowledge(message));
			} catch (Exception e) {
				error(new SocketIOException(
						"Exception was thrown in onMessage(String).\n"
								+ "Message was: " + message.toString(), e));
			}
			break;
		case IOMessage.TYPE_JSON_MESSAGE:
			try {
				JSONObject obj = null;
				String data = message.getData();
				if (data.trim().equals("null") == false)
					obj = new JSONObject(data);
				try {
					findCallback(message).onMessage(obj,
							remoteAcknowledge(message));
				} catch (Exception e) {
					error(new SocketIOException(
							"Exception was thrown in onMessage(JSONObject)\n"
									+ "Message was: " + message.toString(), e));
				}
			} catch (JSONException e) {
				warning("Malformated JSON received");
			}
			break;
		case IOMessage.TYPE_EVENT:
			try {
				JSONObject event = new JSONObject(message.getData());
				JSONArray args = event.getJSONArray("args");
				JSONObject[] argsArray = new JSONObject[args.length()];
				for (int i = 0; i < args.length(); i++) {
					if (args.isNull(i) == false)
						argsArray[i] = args.getJSONObject(i);
				}
				String eventName = event.getString("name");
				try {
					findCallback(message).on(eventName,
							remoteAcknowledge(message), argsArray);
				} catch (Exception e) {
					error(new SocketIOException(
							"Exception was thrown in on(String, JSONObject[])"
									+ "Message was: " + message.toString(), e));
				}
			} catch (JSONException e) {
				warning("Malformated JSON received");
			}
			break;

		case IOMessage.TYPE_ACK:
			String[] data = message.getData().split("\\+", 2);
			if (data.length == 2) {
				try {
					int id = Integer.parseInt(data[0]);
					IOAcknowledge ack = acknowledge.get(id);
					if (ack == null)
						warning("Received unknown ack packet");
					else {
						JSONArray array = new JSONArray(data[1]);
						Object[] args = new Object[array.length()];
						for (int i = 0; i < args.length; i++) {
							args[i] = array.get(i);
						}
						ack.ack(args);
					}
				} catch (NumberFormatException e) {
					warning("Received malformated Acknowledge! This is potentially filling up the acknowledges!");
				} catch (JSONException e) {
					warning("Received malformated Acknowledge data!");
				}
			}
			else if(data.length == 1){
				sendPlain("6:::"+data[0]);
			}
			break;
		case IOMessage.TYPE_ERROR:
			if (message.getEndpoint().equals(""))
				for (SocketIO socket : sockets.values()) {
					socket.getCallback().onError(
							new SocketIOException(message.getData()));
				}
			else
				findCallback(message).onError(
						new SocketIOException(message.getData()));
			if (message.getData().endsWith("+0")) {
				// We are adviced to disconnect
				cleanup();
			}
			break;
		case IOMessage.TYPE_NOOP:
			break;
		default:
			warning("Unkown type received" + message.getType());
			break;
		}
	}

	/**
	 * forces a reconnect. This had become useful on some android devices which
	 * do not shut down TCP-connections when switching from HSDPA to Wifi
	 */
	public void reconnect() {
		if (wantToDisconnect == false) {
			if (transport != null)
				invalidateTransport();
			transport = null;
			connected = false;
			if (reconnectTimeoutTimer == null) {
				reconnectTimeoutTimer = new Timer("reconnectTimeoutTimer");
				reconnectTimeoutTimer.schedule(new ReconnectTimeoutTask(),
						closingTimeout);
			}
			if (reconnectTimer != null)
				reconnectTimer.cancel();
			reconnectTimer = new Timer("reconnectTimer");
			reconnectTimer.schedule(new ReconnectTask(), 1000);
		}
	}

	/**
	 * finds the corresponding callback object to an incoming message. Returns a
	 * dummy callback if no corresponding callback can be found
	 * 
	 * @param message
	 *            the message
	 * @return the iO callback
	 */
	private IOCallback findCallback(IOMessage message) {
		SocketIO socket = sockets.get(message.getEndpoint());
		if (socket == null) {
			warning("Cannot find socket for '" + message.getEndpoint() + "'");
			return DUMMY_CALLBACK;
		}
		return socket.getCallback();
	}

	/**
	 * Handles a non-fatal error.
	 * 
	 * @param message
	 *            the message
	 */
	private void warning(String message) {
		System.out.println(message);
	}

	/**
	 * Returns the session id. This should be called from the
	 * {@link IOTransport} to connect to the right Session.
	 * 
	 * @return the session id
	 */
	public String getSessionId() {
		return sessionId;
	}

	/** A dummy callback used when IOConnection receives a unexpected message */
	final static public IOCallback DUMMY_CALLBACK = new IOCallback() {
		private void out(String msg) {
			System.out.println("DUMMY CALLBACK: " + msg);
		}

		@Override
		public void onDisconnect() {
			out("Disconnect");
		}

		@Override
		public void onConnect() {
			out("Connect");
		}

		@Override
		public void onMessage(String data, IOAcknowledge ack) {
			out("Message:\n" + data + "\n-------------");
		}

		@Override
		public void onMessage(JSONObject json, IOAcknowledge ack) {
			out("Message:\n" + json.toString() + "\n-------------");
		}

		@Override
		public void on(String event, IOAcknowledge ack, Object... args) {
			out("Event '" + event + "':\n");
			for (Object arg : args)
				out(arg.toString());
			out("\n-------------");
		}

		@Override
		public void onError(SocketIOException socketIOException) {
			out("Error");
			throw new RuntimeException(socketIOException);
		}

	};

	/**
	 * sends a String message from {@link SocketIO} to the {@link IOTransport}
	 * 
	 * @param socket
	 *            the socket
	 * @param text
	 *            the text
	 */
	public void send(SocketIO socket, IOAcknowledge ack, String text) {
		IOMessage message = new IOMessage(IOMessage.TYPE_MESSAGE,
				socket.getNamespace(), text);
		synthesizeAck(message, ack);
		sendPlain(message.toString());
	}

	/**
	 * sends a JSON message from {@link SocketIO} to the {@link IOTransport}
	 * 
	 * @param socket
	 *            the socket
	 * @param json
	 *            the json
	 */
	public void send(SocketIO socket, IOAcknowledge ack, JSONObject json) {
		IOMessage message = new IOMessage(IOMessage.TYPE_JSON_MESSAGE,
				socket.getNamespace(), json.toString());
		synthesizeAck(message, ack);
		sendPlain(message.toString());
	}

	/**
	 * emits an event from {@link SocketIO} to the {@link IOTransport}
	 * 
	 * @param socket
	 *            the socket
	 * @param event
	 *            the event
	 * @param args
	 *            the arguments to be send
	 */
	public void emit(SocketIO socket, String event, IOAcknowledge ack,
			Object... args) {
		try {
			JSONObject json = new JSONObject().put("name", event).put("args",
					new JSONArray(Arrays.asList(args)));
			IOMessage message = new IOMessage(IOMessage.TYPE_EVENT,
					socket.getNamespace(), json.toString());
			synthesizeAck(message, ack);
			sendPlain(message.toString());
		} catch (JSONException e) {
			error(new SocketIOException(
					"Error while emitting an event. Make sure you only try to send arguments, which can be serialized into JSON."));
		}

	}

	/**
	 * Checks if IOConnection is currently connected.
	 * 
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		return connected;
	}
}
