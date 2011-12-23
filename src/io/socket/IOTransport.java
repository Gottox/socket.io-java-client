package io.socket;

import java.io.IOException;

public interface IOTransport {
	void connect();
	void disconnect();
	void send(String text) throws IOException;
	boolean canSendBulk();
	void sendBulk(String[] texts) throws IOException;
}
