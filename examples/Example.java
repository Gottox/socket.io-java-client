/*
 * socket.io-java-client Test.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import org.json.JSONException;
import org.json.JSONObject;


public class Example {
	private static SocketIO socket;
	static IOCallback callback = new IOCallback() {
		
		@Override
		public void onMessage(JSONObject json, IOAcknowledge ack) {

		}
		
		@Override
		public void onMessage(String data, IOAcknowledge ack) {
			
		}
		
		@Override
		public void onError(SocketIOException socketIOException) {
			
		}
		
		@Override
		public void onDisconnect() {
			
		}
		
		@Override
		public void onConnect() {
			
		}
		
		@Override
		public void on(String event, IOAcknowledge ack, Object... args) {
			try {
				socket.emit("bla", new IOAcknowledge() {

					@Override
					public void ack(Object... args) {
						System.out.println("Fooo");
					}} ,new JSONObject().put("Hello", "World"));
				ack.ack("Hello");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			socket = new SocketIO();
			socket.connect("http://127.0.0.1:3001/", callback);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
