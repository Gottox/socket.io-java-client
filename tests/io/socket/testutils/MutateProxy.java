package io.socket.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class MutateProxy extends Thread {
	int listenPort;
	int socketPort;
	private Forwarder serverToClient;
	private Forwarder clientToServer;
	private ServerSocket server;

	public MutateProxy(int listenPort, int socketPort) {
		super("MutateProxy");
		this.listenPort = listenPort;
		this.socketPort = socketPort;
		try {
			server = new ServerSocket(listenPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		Socket clientSocket;
		while ((clientSocket = server.accept()) != null) {
			client(clientSocket);
		}
	}

	private void client(Socket clientSocket) throws UnknownHostException,
			IOException {
		Socket serverSocket = new Socket("127.0.0.1", this.socketPort);
		serverToClient = new Forwarder("serverToClient", serverSocket,
				clientSocket);
		clientToServer = new Forwarder("clientToServer", clientSocket,
				serverSocket);
		serverToClient.start();
		clientToServer.start();
	}

	private class Forwarder extends Thread {
		private Socket from;
		private Socket to;

		public Forwarder(String name, Socket from, Socket to) {
			super(name);
			this.from = from;
			this.to = to;
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[1024];
				int length;
				InputStream fromStream = from.getInputStream();
				OutputStream toStream = to.getOutputStream();
				while ((length = fromStream.read(buffer)) >= 0) {
					toStream.write(buffer, 0, length);
					toStream.flush();
				}
			} catch (IOException e) {
				synchronized (MutateProxy.this) {
					System.err.println("Thread " + this.getName());
					e.printStackTrace();
				}
			} finally {
				try {
					to.close();
				} catch (IOException e) {
				}
				try {
					from.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
