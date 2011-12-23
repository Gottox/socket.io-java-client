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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IOConnection {
	public static final String SOCKET_IO_1 = "/socket.io/1/";
	private static HashMap<String, IOConnection> connections = new HashMap<String, IOConnection>();
	private URL url;
	private IOTransport transport;
	private int connectTimeout = 10000;
	private String sessionId;
	private long heartbeatTimeout;
	private long closingTimout;
	private List<String> protocols;
	private LinkedList<String> outputBuffer = new LinkedList<String>();
	private HashMap<String, SocketIO> sockets = new HashMap<String, SocketIO>();
	private Thread connectThread = null;
	private boolean connected = false;
	private boolean wantToDisconnect = false;
	private SocketIO firstSocket = null;
	private Timer reconnectTimer;
	private Timer reconnectTimeoutTimer;
	private String urlStr;

	private final class ReconnectTimeoutTask extends TimerTask {
		@Override
		public void run() {
			wantToDisconnect = true;
			if (reconnectTimer != null) {
				reconnectTimer.cancel();
				reconnectTimer = null;
			}
			cleanup();
		}
	}

	private final class ReconnectTask extends TimerTask {
		@Override
		public void run() {
			connect();
		}
	}

	private class ConnectThread extends Thread {
		public void run() {
			if (transport != null)
				return;

			try {
				if (sessionId == null)
					handshake();
				else
					System.out.println("Try to reconnect");
				connectTransport();

			} catch (IOException e) {
				error(new SocketIOException(e));
			}
			connectThread = null;
		}

		private void handshake() throws IOException {
			URL url = new URL(IOConnection.this.url.toString() + SOCKET_IO_1);

			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(connectTimeout);

			String response;
			try {
				InputStream stream = connection.getInputStream();
				Scanner in = new Scanner(stream);
				response = in.nextLine();
				// process handshake response
				// example: 4d4f185e96a7b:15:10:websocket,xhr-polling
				if (response.contains(":")) {
					String[] data = response.split(":");
					sessionId = data[0];
					heartbeatTimeout = Long.parseLong(data[1]) * 1000;
					closingTimout = Long.parseLong(data[2]) * 1000;
					protocols = Arrays.asList(data[3].split(","));
				}
			} catch (SocketTimeoutException ex) {
				error(new SocketIOException(ex));
				return;
			}
		}

		private void connectTransport() {
			if (protocols.contains(WebsocketTransport.TRANSPORT_NAME))
				transport = WebsocketTransport.create(url, IOConnection.this);
			else
				error(new SocketIOException(
						"Server supports no availible transports"));

			transport.connect();
		}

	};

	static public IOConnection create(String origin) {
		IOConnection connection = connections.get(origin);
		if (connection == null) {
			connection = new IOConnection(origin);
			connections.put(origin, connection);
		}
		return connection;
	}

	private IOConnection(String url) {
		try {
			this.url = new URL(url);
			this.urlStr = url;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		connect();
	}

	protected void error(SocketIOException e) {
		for (SocketIO socket : sockets.values()) {
			socket.getCallback().onError(e);
		}
	}

	private void connect() {
		if (connectThread == null) {
			connectThread = new ConnectThread();
			connectThread.start();
		}
	}

	public void connect(SocketIO socket) {
		if (sockets.size() == 0)
			firstSocket = socket;
		String namespace = socket.getNamespace();
		if (sockets.containsKey(namespace))
			socket.getCallback().onError(
					new SocketIOException("Namespace '" + namespace
							+ "' is already registered."));
		else
			sockets.put(namespace, socket);
		if (socket.getNamespace() != "") {
			IOMessage message = new IOMessage(IOMessage.TYPE_CONNECT,
					namespace, "");
			sendPlain(message.toString());
		}
	}

	private void cleanup() {
		System.out.println("Clean up");
		wantToDisconnect = true;
		if (transport != null)
			transport.disconnect();
		sockets.clear();
		connections.remove(urlStr);
		if(reconnectTimer != null) {
			reconnectTimer.cancel();
			reconnectTimer = null;
		}
		if(reconnectTimeoutTimer != null) {
			reconnectTimeoutTimer.cancel();
			reconnectTimeoutTimer = null;
		}
	}

	public void disconnect(SocketIO socket) {
		sendPlain("0::" + socket.getNamespace());
		sockets.remove(socket.getNamespace());
		socket.getCallback().onDisconnect();

		if (sockets.size() == 0) {
			cleanup();
		}
	}

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
			for (String text : outputBuffer) {
				sendPlain(text);
			}
		}
	}

	public void transportDisconnected() {
		connected = false;
		reconnect();
	}

	public void transportError(Exception error) {
		if (wantToDisconnect) {
			for (SocketIO socket : sockets.values()) {
				socket.getCallback().onError(new SocketIOException(error));
			}
		}
		connected = false;
		reconnect();
	}

	public void reconnect() {
		if (wantToDisconnect == false) {
			transport = null;
			if (reconnectTimeoutTimer == null) {
				reconnectTimeoutTimer = new Timer("reconnectTimeoutTimer");
				reconnectTimeoutTimer.schedule(new ReconnectTimeoutTask(),
						closingTimout);
			}
			if (reconnectTimer != null)
				reconnectTimer.cancel();
			reconnectTimer = new Timer("reconnectTimer");
			reconnectTimer.schedule(new ReconnectTask(), 1000);
		}
	}

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
				findCallback(message).onDisconnect();
			break;
		case IOMessage.TYPE_CONNECT:
			findCallback(message).onConnect();
			break;
		case IOMessage.TYPE_HEARTBEAT:
			sendPlain("2::");
			break;
		case IOMessage.TYPE_MESSAGE:
			findCallback(message).onMessage(message.getData());
			break;
		case IOMessage.TYPE_JSON_MESSAGE:
			try {
				findCallback(message).onMessage(
						new JSONObject(message.getData()));
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
					argsArray[i] = args.getJSONObject(i);
				}
				String eventName = event.getString("name");
				findCallback(message).on(eventName, argsArray);
			} catch (JSONException e) {
				warning("Malformated JSON received");
			}
			break;

		case IOMessage.TYPE_ACK:

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

	private IOCallback findCallback(IOMessage message) {
		SocketIO socket = sockets.get(message.getEndpoint());
		if (socket == null) {
			warning("Cannot find socket for '" + message.getEndpoint() + "'");
			return DUMMY_CALLBACK;
		}
		return socket.getCallback();
	}

	private void warning(String message) {
		System.out.println(message);
	}

	public String getSessionId() {
		return sessionId;
	}

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
		public void onMessage(String data) {
			out("Message:\n" + data + "\n-------------");
		}

		@Override
		public void onMessage(JSONObject json) {
			out("Message:\n" + json.toString() + "\n-------------");
		}

		@Override
		public void on(String event, JSONObject... args) {
			out("Event '" + event + "':\n");
			for (JSONObject arg : args)
				try {
					out(arg.toString(2));
				} catch (JSONException e) {
					out("???");
				}
			out("\n-------------");
		}

		@Override
		public void onError(SocketIOException socketIOException) {
			out("Error");
			throw new RuntimeException(socketIOException);
		}

	};

	public void send(SocketIO socket, String text) {
		IOMessage message = new IOMessage(IOMessage.TYPE_MESSAGE,
				socket.getNamespace(), text);
		sendPlain(message.toString());
	}

	public void send(SocketIO socket, JSONObject json) {
		IOMessage message = new IOMessage(IOMessage.TYPE_JSON_MESSAGE,
				socket.getNamespace(), json.toString());
		sendPlain(message.toString());
	}

	public void emit(SocketIO socket, String event, JSONObject[] args) {
		try {
			JSONObject json = new JSONObject().put("name", event).put("args",
					new JSONArray(Arrays.asList(args)));
			IOMessage message = new IOMessage(IOMessage.TYPE_EVENT,
					socket.getNamespace(), json.toString());
			sendPlain(message.toString());
		} catch (JSONException e) {
		}

	}

	public long getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public void setHeartbeatTimeout(long heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}
}
