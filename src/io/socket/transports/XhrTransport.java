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

public class XhrTransport implements IOTransport {
	/** The String to identify this Transport */
	public static final String TRANSPORT_NAME = "xhr-polling";
	private IOConnection connection;
	private URL url;
	ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();

	ReceiverThread receiver = null;
	private boolean connect;
	private boolean blocked;
	
	private class ReceiverThread extends Thread {
		public ReceiverThread() {
			super(TRANSPORT_NAME);
		}

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

	public XhrTransport(URL url, IOConnection connection) {
		this.connection = connection;
		this.url = url;
	}

	@Override
	public void connect() {
		this.setConnect(true);
		if (receiver == null) {
			receiver = new ReceiverThread();
			receiver.start();
		} else
			new RuntimeException(
					"connecting on an allready connected transport");
	}

	@Override
	public void disconnect() {
		this.setConnect(false);
		receiver.interrupt();
	}

	@Override
	public void send(String text) throws IOException {
		sendBulk(new String[] { text });
	}

	@Override
	public boolean canSendBulk() {
		return true;
	}

	@Override
	public void sendBulk(String[] texts) throws IOException {
		for (String text : texts) {
			queue.add(text);
		}
		if(isBlocked())
			receiver.interrupt();
	}

	@Override
	public void invalidate() {
		this.connection = null;
	}

	public synchronized boolean isConnect() {
		return connect;
	}

	public synchronized void setConnect(boolean connect) {
		this.connect = connect;
	}

	public synchronized boolean isBlocked() {
		return blocked;
	}

	public synchronized void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
}
