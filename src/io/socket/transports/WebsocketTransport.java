package io.socket.transports;

import io.socket.IOConnection;
import io.socket.SocketIO;
import io.socket.IOTransport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import net.tootallnate.websocket.WebSocketClient;

public class WebsocketTransport extends WebSocketClient implements IOTransport {
	private final static Pattern PATTERN_HTTP = Pattern.compile("^http");
	public static final Object TRANSPORT_NAME = "websocket";

	public static IOTransport create(URL url, IOConnection connection) {
		URI uri = URI.create(
				PATTERN_HTTP.matcher(url.toString()).replaceFirst("ws")
				+ IOConnection.SOCKET_IO_1 + TRANSPORT_NAME
				+ "/" + connection.getSessionId());

		return new WebsocketTransport(uri, connection);
	}

	private IOConnection connection;

	public WebsocketTransport(URI uri, IOConnection connection) {
		super(uri);
		this.connection = connection;
	}

	@Override
	public void onClose() {
		connection.transportDisconnected();
	}

	@Override
	public void onIOError(IOException error) {
		connection.transportError(error);
	}

	@Override
	public void onMessage(String message) {
		connection.transportMessage(message);
	}

	@Override
	public void onOpen() {
		connection.transportConnected();
	}

	@Override
	public void disconnect() {
		try {
			this.close();
		} catch (IOException e) {
			connection.transportError(e);
		}
	}

	@Override
	public boolean canSendBulk() {
		return false;
	}
	
	@Override
	public void sendBulk(String[] texts) throws IOException {
		throw new RuntimeException("Cannot send Bulk!");
	}

}
