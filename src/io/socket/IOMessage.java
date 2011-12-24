/*
 * socket.io-java-client IOMessage.java
 *
 * Copyright (c) 2011, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.util.regex.Pattern;

public class IOMessage {
	public static final int TYPE_DISCONNECT = 0;
	public static final int TYPE_CONNECT = 1;
	public static final int TYPE_HEARTBEAT = 2;
	public static final int TYPE_MESSAGE = 3;
	public static final int TYPE_JSON_MESSAGE = 4;
	public static final int TYPE_EVENT = 5;
	public static final int TYPE_ACK = 6;
	public static final int TYPE_ERROR = 7;
	public static final int TYPE_NOOP = 8;

	public static final int FIELD_TYPE = 0;
	public static final int FIELD_ID = 1;
	public static final int FIELD_ENDPOINT = 2;
	public static final int FIELD_DATA = 3;
	public static final int NUM_FIELDS = 4;
	
	public static final Pattern TRIM_PATTERN = Pattern.compile(":*$");

	String[] fields = new String[NUM_FIELDS];

	public IOMessage(int type, String id, String namespace, String data) {
		this(type, namespace, data);
		this.fields[FIELD_ID] = id;
	}

	public IOMessage(int type, String namespace, String data) {
		this.fields[FIELD_TYPE] = ""+type;
		this.fields[FIELD_ENDPOINT] = namespace;
		this.fields[FIELD_DATA] = data;
	}

	public IOMessage(String message) {
		String[] fields = message.split(":", NUM_FIELDS);
		for (int i = 0; i < fields.length; i++) {
			this.fields[i] = fields[i];
		}
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(String field : fields) {
			if(field != null)
				builder.append(field + ":");
			else
				builder.append(':');
		}
		return TRIM_PATTERN.matcher(builder.toString()).replaceFirst("");
	}

	public int getType() {
		return Integer.parseInt(fields[FIELD_TYPE]);
	}

	public String getId() {
		return fields[FIELD_ID];
	}

	public String getEndpoint() {
		return fields[FIELD_ENDPOINT];
	}

	public String getData() {
		return fields[FIELD_DATA];
	}

}
