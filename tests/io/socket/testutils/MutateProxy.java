package io.socket.testutils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;

public class MutateProxy extends Thread {
	int listenPort;
	int socketPort;
	private Forwarder serverToClient;
	private Forwarder clientToServer;

	public MutateProxy(int listenPort, int socketPort) {
		super("MutateProxy");
		this.listenPort = listenPort;
		this.socketPort = socketPort;
	}

	@Override
	public void run() {
		try {
			server();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void server() throws IOException {
		ServerSocket server = new ServerSocket(listenPort);
		Socket connection;
		while ((connection = server.accept()) != null) {
			InputStreamReader reader = new InputStreamReader(
					connection.getInputStream());
			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream());
			client(reader, writer);
		}
	}

	private void client(final InputStreamReader clientReader,
			final OutputStreamWriter clientWriter) throws UnknownHostException,
			IOException {
		Socket socket = new Socket("127.0.0.1", socketPort);
		final InputStreamReader serverReader = new InputStreamReader(
				socket.getInputStream());
		final OutputStreamWriter serverWriter = new OutputStreamWriter(
				socket.getOutputStream());
		serverToClient = new Forwarder("serverToClient", serverReader,
				clientWriter);
		clientToServer = new Forwarder("clientToServer", clientReader,
				serverWriter);
		serverToClient.start();
		clientToServer.start();
	}

	private static class Forwarder extends Thread {
		private OutputStreamWriter output;
		private InputStreamReader input;

		public Forwarder(String name, InputStreamReader input,
				OutputStreamWriter output) {
			super(name);
			this.output = output;
			this.input = input;
		}

		@Override
		public void run() {
			CharBuffer buffer = CharBuffer.allocate(1024);
			try {
				while(input.read(buffer) != 0) {
					output.append(buffer.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
