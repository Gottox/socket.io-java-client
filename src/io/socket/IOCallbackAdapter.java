package io.socket;

import org.json.JSONObject;

public class IOCallbackAdapter implements IOCallback {

	@Override
	public void onDisconnect() {
	}

	@Override
	public void onConnect() {
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
	}

	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
	}

	@Override
	public void onError(SocketIOException socketIOException) {
	}

}
