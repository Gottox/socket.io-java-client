/*
 * socket.io-java-client Test.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import org.json.JSONException;
import org.json.JSONObject;


public class Test {
	private static SocketIO socket;
	static IOCallback callback = new IOCallback() {
		
		@Override
		public void onMessage(JSONObject json) {

		}
		
		@Override
		public void onMessage(String data) {
			
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
		public void on(String event, JSONObject... args) {
			try {
				socket.emit("bla", new JSONObject().put("Hello", "World"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(new IOMessage("0::/ABC").toString());
		//System.exit(0);
		try {
			socket = new SocketIO();
			socket.connect("http://127.0.0.1:3001/foobar", callback);
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
