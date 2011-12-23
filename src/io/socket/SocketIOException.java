package io.socket;



public class SocketIOException extends Exception {

	private static final long serialVersionUID = 4965561569568761814L;

	public SocketIOException(String message) {
		super(message);
	}

	public SocketIOException(Exception ex) {
		super(ex);
	}
}
