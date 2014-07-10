/*
 * socket.io-java-client IOMessage.java
 *
 * Copyright (c) 2012, Enno Boland
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 * See LICENSE file for more information
 */
package io.socket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Class IOMessage.
 */
class IOMessage
{
	/** Debug logger */
	static final Logger logger = Logger.getLogger("io.message");
	
	public enum TypeMessage { TYPE_UNKNOWN,
								TYPE_DISCONNECT,
								TYPE_DISCONNECTED,
								TYPE_CONNECT,
								TYPE_CONNECTED,
								TYPE_HEARTBEAT,
								TYPE_PONG,
								TYPE_MESSAGE,
								TYPE_JSON_MESSAGE,
								TYPE_EVENT,
								TYPE_ACK,
								TYPE_ERROR,
								TYPE_NOOP,
								TYPE_BINARY_EVENT,
								TYPE_BINARY_ACK,
								TYPE_UPGRADE,
								};

	/** Index of the type field in a message */
	public static final int FIELD_CONTROL = 0;

	/** Index of the id field in a message */
	public static final int FIELD_ID = 1;

	/** Index of the end point field in a message */
	public static final int FIELD_ENDPOINT = 2;
	
	/** Index of the end point field in a message */
	public static final int FIELD_DATA = 3;

	/** Number of fields in a message. */
	public static final int NUM_FIELDS = 3;

	public static final int NUM_FIELDS_MESSAGE = 4;

	/** The field values */
	protected final String[] fields = new String[NUM_FIELDS];
	
	protected JSONArray args = new JSONArray();
	protected String data;
	
	protected String separator = ":";
	
	
	/** Type */
	protected TypeMessage type; // CAN BE TYPE_DISCONNECT,TYPE_CONNECT, TYPE_MESSAGE
	
	/** Event name*/
	protected String eventName = ""; // CAN BE "message","myevent","anything"
	
	/** Acknoledgement*/
	protected String ack = ""; // CAN BE "message","myevent","anything"
	
//	/**
//	 * Instantiates a new IOMessage by given data.
//	 * 
//	 * @param type
//	 *            the type
//	 * @param id
//	 *            the id
//	 * @param namespace
//	 *            the namespace
//	 * @param data
//	 *            the data
//	 */
//	protected IOMessage(TypeMessage type, String id, String namespace, String data)
//	{
//		this.type = type;
//		this.fields[FIELD_ID] = id;
//		this.fields[FIELD_CONTROL] = "" + type;
//		this.fields[FIELD_ENDPOINT] = namespace;
//		this.data = data;
//		args.put(data);
//	}
	
	protected final Map<Integer, IOMessage.TypeMessage> messages_codes = this.createMapMessages();

	protected Map<Integer, IOMessage.TypeMessage> createMapMessages() 
    {
		Map<Integer, IOMessage.TypeMessage> result= new HashMap<Integer, IOMessage.TypeMessage>() ;
		result.put(0, IOMessage.TypeMessage.TYPE_DISCONNECT);
		result.put(1, IOMessage.TypeMessage.TYPE_CONNECT);
		result.put(2, IOMessage.TypeMessage.TYPE_HEARTBEAT);
		result.put(3, IOMessage.TypeMessage.TYPE_MESSAGE);
		result.put(4, IOMessage.TypeMessage.TYPE_JSON_MESSAGE);
		result.put(5, IOMessage.TypeMessage.TYPE_EVENT);
		result.put(6, IOMessage.TypeMessage.TYPE_ACK);
		result.put(7, IOMessage.TypeMessage.TYPE_ERROR);
		result.put(8, IOMessage.TypeMessage.TYPE_NOOP);
		//return Collections.unmodifiableMap(result);
		return result;
    }
	
	/**
	 * Instantiates a new IOMessage by given data.
	 */
	protected IOMessage()
	{
		this.type = TypeMessage.TYPE_UNKNOWN;
		this.fields[FIELD_ID] = "";
		this.fields[FIELD_CONTROL] = "";
		this.fields[FIELD_ENDPOINT] = "";
		this.data = "";
	}
	
	/**
	 * Instantiates a new IOMessage without data.
	 * 
	 * @param type
	 *            the type
	 * @param id
	 *            the id
	 * @param namespace
	 *            the namespace
	 */
	protected IOMessage(TypeMessage type, String id, String namespace)
	{
		this.type = type;
		this.fields[FIELD_ID] = id;
		this.fields[FIELD_CONTROL] = "" + getControlNumber(type);
		this.fields[FIELD_ENDPOINT] = namespace;
	}
	
	protected int getControlNumber(TypeMessage type)
	{
		for (Entry<Integer, TypeMessage> entry : this.messages_codes.entrySet()) 
		{
	        if (type.equals(entry.getValue())) {
	            return entry.getKey();
	        }
	    }
		return 0;
	}
	
	public static IOMessage parseMessage(String message, IOConnection.VersionSocketIO version)
	{
		IOMessage messageOut;
		switch (version) 
		{
		case V09x:
			messageOut = new IOMessage(message);
			break;
		case V10x:
			messageOut = new IOMessageV10x(message);
			break;
		default:
			messageOut = new IOMessage(message);
			break;
		}
		return messageOut;
	}
	
	public static IOMessage createMessage(TypeMessage type, String id, String namespace, String data, IOConnection.VersionSocketIO version)
	{
		IOMessage messageOut;
		switch (version) 
		{
		case V09x:
			messageOut = new IOMessage(type,id,namespace);
			break;
		case V10x:
			messageOut = new IOMessageV10x(type,id,namespace);
			break;
		default:
			messageOut = new IOMessage(type,id,namespace);
			break;
		}
		if(data != "")
			messageOut.addData(data);
		return messageOut;
	}
	
	public static IOMessage createMessage(TypeMessage type, String id, String namespace,IOConnection.VersionSocketIO version)
	{
		return createMessage(type,id,namespace,"",version);
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
	protected IOMessage(String message)
	{
		String[] fields = message.split(":", NUM_FIELDS_MESSAGE);
		this.fields[FIELD_CONTROL] = fields[FIELD_CONTROL];
		this.fields[FIELD_ENDPOINT] = fields[FIELD_ENDPOINT];
		this.fields[FIELD_ID] = fields[FIELD_ID];
		this.type = messages_codes.get(Integer.parseInt(this.fields[FIELD_CONTROL]));
		if(this.type == TypeMessage.TYPE_MESSAGE)
		{
			args.put(data);
			data = fields[FIELD_DATA];
		}
		else if(this.type == TypeMessage.TYPE_EVENT)
		{
			data = fields[FIELD_DATA];
			try 
			{
				JSONObject event = new JSONObject(data);
				if (event.has("args"))
				{
					args = event.getJSONArray("args");
				}
				this.eventName = event.getString("name");
				
			} catch (JSONException e) {
				logger.warning("Malformated JSON received");
			}	
		}	
	}
	
	/**
	 * Generates a String representation of this object.
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();	
		builder.append(this.fields[FIELD_CONTROL]);
		builder.append(this.separator);

		String pIdL = this.fields[FIELD_ID];
		if (ack == "data")
		{
			pIdL += "+";
		}

		// Do not write pid for acknowledgements
		if (this.type != TypeMessage.TYPE_ACK)
		{
			builder.append(pIdL);
		}
		builder.append(this.separator);

		// Add the end point for the namespace to be used, as long as it is not
		// an ACK, heartbeat, or disconnect packet
		if (this.type != TypeMessage.TYPE_ACK 
				&& this.type != TypeMessage.TYPE_HEARTBEAT
				&& this.type != TypeMessage.TYPE_DISCONNECT)
			builder.append(this.fields[FIELD_ENDPOINT]);
		builder.append(this.separator);

		if (args.length() != 0)
		{
			String ackpId = "";
			// This is an acknowledgement packet, so, prepend the ack pid to the data
			if (this.type == TypeMessage.TYPE_ACK)
			{
				ackpId += pIdL+"+";
			}
			builder.append(ackpId);
			builder.append(this.stringify());
		}
		return builder.toString();
	}

	/**
	 * Returns the type of this IOMessage.
	 * 
	 * @return the type
	 */
	public TypeMessage getType() {
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
	 * Returns the eventName of this IOMessage.
	 * 
	 * @return the eventName
	 */
	public String getEvent() {
		return eventName;
	}
	
	/**
	 * Sets the event name of this IOMessage
	 * 
	 * @param event
	 */
	public void setEvent(String event) {
		eventName = event;
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
	 * Add data element
	 * Careful: one message type in V0.9.x can only have one element
	 * 
	 * @param the data
	 */
	public void addData(String data)
	{
		this.data += data;
		args.put(data);
	}
	
	/**
	 * Add Data element
	 * 
	 * @param the data
	 */
	public void addData(Object data)
	{
		args.put(data);
	}
	
	/**
	 * Returns the data of this IOMessage.
	 * 
	 * @return the data
	 */
	public String getData()
	{
		return data;
	}
	
	/**
	 * Returns the args.
	 * 
	 * @return the data received in objet array form
	 */
	public Object[] getArgs()
	{
		Object[] argsArray;
		argsArray = new Object[args.length()];
		try 
		{
			for (int i = 0; i < args.length(); i++) 
			{
				if (args.isNull(i) == false)					
						argsArray[i] = args.get(i);					
			}
		} catch (JSONException e) 
		{
			logger.warning("Error when exporting the data: "+e.toString());
		}
		return argsArray;
	}
	
	/**
	 * Stringify the data.
	 * 
	 * @return the data stringified
	 */
	public String stringify()
	{
		String res = "";
		try 
		{
			if(this.type == TypeMessage.TYPE_MESSAGE || this.type == TypeMessage.TYPE_JSON_MESSAGE )
				res = args.get(0).toString();
			else if(this.type == TypeMessage.TYPE_EVENT)
			{
				JSONObject event = new JSONObject();
				event.put("args", args);
				event.put("name", eventName);
				res = event.toString();
			}
			else
				res = this.getData();
		} catch (JSONException e) 
		{
			logger.warning("Error when stringify data: "+e.toString());
		}
		return res;
	}

}

		
class IOMessageV10x extends IOMessage
{	
	@Override
    protected Map<Integer, IOMessage.TypeMessage> createMapMessages() 
    {
		Map<Integer, IOMessage.TypeMessage> result= new HashMap<Integer, IOMessage.TypeMessage>() ;
		result.put(0, IOMessage.TypeMessage.TYPE_DISCONNECTED);
		result.put(1, IOMessage.TypeMessage.TYPE_CONNECTED);
		result.put(2, IOMessage.TypeMessage.TYPE_HEARTBEAT);
		result.put(3, IOMessage.TypeMessage.TYPE_PONG);
		result.put(4, IOMessage.TypeMessage.TYPE_MESSAGE);
		result.put(5, IOMessage.TypeMessage.TYPE_UPGRADE);
		result.put(6, IOMessage.TypeMessage.TYPE_NOOP);
		result.put(40, IOMessage.TypeMessage.TYPE_CONNECT);
		result.put(41, IOMessage.TypeMessage.TYPE_DISCONNECT);
		result.put(42, IOMessage.TypeMessage.TYPE_EVENT);
		result.put(43, IOMessage.TypeMessage.TYPE_ACK);
		result.put(44, IOMessage.TypeMessage.TYPE_ERROR);
		result.put(45, IOMessage.TypeMessage.TYPE_BINARY_EVENT);
		result.put(46, IOMessage.TypeMessage.TYPE_BINARY_ACK);
		//return Collections.unmodifiableMap(result);
		return result;
    }
    
        
    protected IOMessageV10x(TypeMessage type, String id, String namespace)
    {
    	super(type,id,namespace);
    	///OVERWRITE
    	separator = "";
		this.fields[FIELD_CONTROL] = "" + this.getControlNumber(type);
		///OVERWRITE END
    }
	
    protected IOMessageV10x(String message)
	{
    	///OVERWRITE
    	separator = "";
    	///OVERWRITE END
		//42["message","{\"type\":\"redirect\",\"url\":\"/logout\",\"rid\":\"test\",\"action\":\"reject\"}"]
		int control = message.charAt(0) - '0';
		data = message.substring(1);
		if(messages_codes.get(control) == IOMessage.TypeMessage.TYPE_MESSAGE)
		{
			control = 40;
			control += data.charAt(0) - '0';
			data = data.substring(1);
		}
		this.fields[FIELD_CONTROL] = String.valueOf(control);		
		this.type = messages_codes.get(control);
		
		String endpoint = "";
		int nendpoint = data.indexOf("[");
		if(nendpoint != -1)
		{
			endpoint = data.substring(0, nendpoint);
			data = data.substring(nendpoint);
		}
		this.fields[FIELD_ENDPOINT] = endpoint;		
		this.fields[FIELD_ID] = "";
		
		if(this.type == TypeMessage.TYPE_EVENT)
		{
			JSONArray arraydata;
			try 
			{
				arraydata = new JSONArray(data);
				eventName = arraydata.getString(0);
				for (int i = 1; i < arraydata.length(); ++i)
				{
					args.put(arraydata.get(i));
				}	
			} catch (JSONException e)
			{
				
			}
					
		}
	}    
	
    protected int getControlNumber(TypeMessage type)
	{
		return super.getControlNumber(type);
	}
    
	/**
	 * Returns the data of this IOMessage.
	 * 
	 * @return the data
	 */
    @Override
	public String stringify()
	{
		String res = "";
		if(this.type == TypeMessage.TYPE_EVENT)
		{
			JSONArray event = new JSONArray();
			event.put(eventName);
			try {
				for (int i=0; i<args.length();++i)
					event.put(args.get(i));
			} catch (JSONException e) {
				logger.warning("Error when stringify data: "+e.toString());
			}
			res = event.toString();
		}
		else
			res = this.getData();
		return res;
	}	
	
	/**
	 * Generates a String representation of this object.
	 */
	@Override
	public String toString()
	{
		return super.toString();
	}
	
}