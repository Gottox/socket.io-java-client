/*
 * socket.io-java-client IOMessage.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.IOConnection.VersionSocketIO;

/**
 * The Class IOMessage.
 */
class IOMessage {

	/** Message type Unknown*/
	public static final int TYPE_UNKNOWN= -1;
	
	/** Message type disconnect */
	public static final int TYPE_DISCONNECT = 0;

	/** Message type connect */
	public static final int TYPE_CONNECT = 1;

	/** Message type heartbeat/ping */
	public static final int TYPE_HEARTBEAT = 2;
	
	/** Message type pong */
	public static final int TYPE_PONG = 9;

	/** Message type message */
	public static final int TYPE_MESSAGE = 3;

	/** Message type JSON message */
	public static final int TYPE_JSON_MESSAGE = 4;

	/** Message type event */
	public static final int TYPE_EVENT = 5;

	/** Message type acknowledge */
	public static final int TYPE_ACK = 6;

	/** Message type error */
	public static final int TYPE_ERROR = 7;

	/** Message type noop */
	public static final int TYPE_NOOP = 8;
	
	/** Message type Binary Event */
	public static final int TYPE_BINARY_EVENT= 9;
	
	/** Message type Binary ACK*/
	public static final int TYPE_BINARY_ACK = 10;
	
	/** Message type Upgrade required */
	public static final int TYPE_UPGRADE = 11;

	/** Index of the type field in a message */
	public static final int FIELD_TYPE = 0;

	/** Index of the id field in a message */
	public static final int FIELD_ID = 1;

	/** Index of the end point field in a message */
	public static final int FIELD_ENDPOINT = 2;

	/** Index of the data field in a message */
	public static final int FIELD_DATA = 3;

	/** Number of fields in a message. */
	public static final int NUM_FIELDS = 4;

	/** The field values */
	private final String[] fields = new String[NUM_FIELDS];

	/** Type */
	private int type;
	
	private IOConnection.VersionSocketIO version;
	
	/**
	 * Instantiates a new IOMessage by given data.
	 * 
	 * @param type
	 *            the type
	 * @param id
	 *            the id
	 * @param namespace
	 *            the namespace
	 * @param data
	 *            the data
	 */
	public IOMessage(int type, String id, String namespace, String data,IOConnection.VersionSocketIO version)
	{
		this.version = version;
		this.type = type;
		this.fields[FIELD_ID] = id;
		this.fields[FIELD_TYPE] = "" + type;
		this.fields[FIELD_ENDPOINT] = namespace;
		this.fields[FIELD_DATA] = data;
	}

	/**
	 * Instantiates a new IOMessage by given data.
	 * 
	 * @param type
	 *            the type
	 * @param namespace
	 *            the name space
	 * @param data
	 *            the data
	 */
	public IOMessage(int type, String namespace, String data,IOConnection.VersionSocketIO version)
	{
		this(type, null, namespace, data,version);
	}

	/**
	 * Instantiates a new IOMessage from a String representation. If the String
	 * is not well formated, the result is undefined.
	 * 
	 * @param message
	 *            the message
	 * @param version
	 *            The parsing for a socket io 0.9.x and 1.0.x are different
	 */
	public IOMessage(String message,IOConnection.VersionSocketIO version)
	{
		this.version = version;
		switch (this.version)
		{
			case V09x:
				String[] fields = message.split(":", NUM_FIELDS);
				for (int i = 0; i < fields.length; i++) {
					this.fields[i] = fields[i];
					if(i == FIELD_TYPE)
						this.type = Integer.parseInt(fields[i]);
				}
				break;
			case V10x:
			{
				//42["message","{\"type\":\"redirect\",\"url\":\"/logout\",\"rid\":\"test\",\"info\":\"Internal error: could not get csInfo.\",\"action\":\"reject\"}"]
				int control = message.charAt(0) - '0';
				String data = message.substring(1);
				String endpoint = "";
				
				this.fields[FIELD_ID] = "";				
				switch (control)
				{
					case 0:
						this.type = TYPE_UNKNOWN;
						break;
					case 1:
						this.type = TYPE_UNKNOWN;
						break;
					case 2:
						this.type = TYPE_HEARTBEAT;
						this.setData(data);
						break;
					case 3:
						this.type = TYPE_PONG;
						this.setData(data);
						break;
					case 4:
					{
						control = data.charAt(0) - '0';						
						data = data.substring(1);
						int iendpoint = data.indexOf('[');
						if(iendpoint != -1)
							endpoint = data.substring(0,iendpoint);
						switch (control)
						{
							case 0:
								this.type = TYPE_CONNECT;								
								break;
							case 1:
								this.type = TYPE_DISCONNECT;
								break;
							case 2:
							{
								//event
								//5:::
								//message
								// ["message","{\"type\":\"message sendWith backslash\"}"]
//								System.out.println("To Parse: "+data);
//								int extraQuote = data.lastIndexOf("}\"");
//								if(extraQuote != -1)
//								{									
//									StringBuilder b = new StringBuilder(data);
//									b.replace(extraQuote,extraQuote+2, "}" );
//									data = b.toString();
//									data = data.replace("\"{", "{");
////									data = data.replaceAll("}\"", "}");
//								}
								//
								while(data.indexOf("}\",\"{") != -1)
									data = data.replace("}\",\"{", "},{");
								data = data.replace(",\"{", ",{");
								data = data.replace("}\"]", "}]");								
								// ["message",{\"type\":\"message sendWith backslash\"}]
								data = data.replaceAll("\\\\\\\\\\\\\"", "\"");
								data = data.replaceAll("\\\\\"", "\"");
								// ["message",{"type":"message sendWith backslash"}]
//								System.out.println("To Parse: "+data);
								try 
								{									
									JSONArray datain = null;
									datain = new JSONArray(data);
									String event = datain.getString(0);
									String dataout = "";
									StringBuilder builder = new StringBuilder();
									for(int i = 1; i < datain.length(); i++)
									{
										builder.append(',');
										if (datain.isNull(i) == false)
											builder.append(datain.getString(i));
									}
									dataout = builder.substring(1);
//									System.out.println("Event: "+event);
//									System.out.println("Data Out: "+dataout);
									if("message".equals(event))
									{
										this.type = TYPE_JSON_MESSAGE;
										this.setData(dataout);
									}
									else
									{
										this.type = TYPE_EVENT;
										JSONObject jdataout = new JSONObject()
											.put("name",event)
											.put("args",new JSONArray("["+dataout+"]"));
										this.setData(jdataout.toString());
									}
																		
								}catch (JSONException e) 
								{
									System.err.println("Malformed Message received");
								}
							}	break;
							case 3:
								this.type = TYPE_ACK;
								break;
							case 4:
								this.type = TYPE_ERROR;
								break;
							case 5:
								this.type = TYPE_BINARY_EVENT;
								
								break;
							case 6:
								this.type = TYPE_BINARY_ACK;
								break;	
							default:
								this.type = TYPE_UNKNOWN;
								break;
						}
					}	break;
					case 5:
						this.type = TYPE_UPGRADE;						
						break;
					case 6:
						this.type = TYPE_NOOP;
						break;	
					default:
						this.type = TYPE_UNKNOWN;
						break;
				}
				this.setEndpoint(endpoint);
			}	break;
		}
		
		
	}

	/**
	 * Generates a String representation of this object.
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		switch (version)
		{		
			case V09x:
			{				
				for(int i = 0; i < fields.length; i++)
				{
					builder.append(':');
					if (fields[i] != null)
						builder.append(fields[i]);
				}
			}	break;
			case V10x:
			{
				//"42<endpoint>["message","<s>"]
				builder.append(":42");
				builder.append(fields[FIELD_ENDPOINT]);
				
				switch (Integer.parseInt(fields[FIELD_TYPE]))
				{
				case TYPE_MESSAGE:
				case TYPE_JSON_MESSAGE:
					builder.append("[\"message\",\"");
					builder.append(fields[FIELD_DATA]);
					builder.append("\"]");
					break;
				case TYPE_EVENT:
					try 
					{
						JSONObject event = new JSONObject(this.getData());
						String eventName = event.getString("name");
						JSONArray dataOut = new JSONArray();
						dataOut.put(0, eventName);
						if (event.has("args"))
						{
							JSONArray args = event.getJSONArray("args");
							for (int i = 0; i < args.length(); i++)
							{
								if (args.isNull(i) == false)
									dataOut.put(i+1,args.get(i));
							}
						}
						builder.append(dataOut.toString());
						
					} catch (JSONException e) {
						System.err.println("Malformated JSON To Send");
					}	
				default:
					break;
				}
								
			}	break;
		}
		
		return builder.substring(1);
	}

	/**
	 * Returns the type of this IOMessage.
	 * 
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the id of this IOMessage.
	 * 
	 * @return the id
	 */
	public String getId() {
		return fields[FIELD_ID];
	}

	/**
	 * Sets the id of this IOMessage
	 * 
	 * @param id
	 */
	public void setId(String id) {
		fields[FIELD_ID] = id;
	}

	/**
	 * Sets the endpoint of this IOMessage.
	 * 
	 * @param the endpoint
	 */
	private void setEndpoint(String endPoint) {
		fields[FIELD_ENDPOINT] = endPoint;
	}
	
	/**
	 * Returns the endpoint of this IOMessage.
	 * 
	 * @return the endpoint
	 */
	public String getEndpoint() {
		return fields[FIELD_ENDPOINT];
	}

	/**
	 * Returns the data of this IOMessage.
	 * 
	 * @param the data
	 */
	private void setData(String data) {
		fields[FIELD_DATA] = data;
	}
	
	/**
	 * Returns the data of this IOMessage.
	 * 
	 * @return the data
	 */
	public String getData() {
		return fields[FIELD_DATA];
	}

}
