# Socket.IO-Client for Java

io.socket is a simple implementation of [socket.io](http://socket.io) for Java.

It uses [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) as transport backend, but it's easy
to write your own transport. See description below.

The API is inspired by [java-socket.io.client](https://github.com/benkay/java-socket.io.client) but as the license
of this project was unclear and it had some nasty bugs, I decided to write io.socket from the scratch.

## How to use

Using io.socket is quite simple. But lets see:

	// Initialise a socket:
	SocketIO socket = new IOSocket("http://127.0.0.1:3001")
	socket.go(new IOCallback() {
			@Override
			public void onMessage(JSONObject json) {
				System.out.println("We received a message: " + json.toString(2));
			}
			
			@Override
			public void onMessage(String data) {
				System.out.println("We received a message:" + data);
			}
			
			@Override
			public void onError(SocketIOException socketIOException) {
				System.out.println("Something went wrong");
			}
			
			@Override
			public void onDisconnect() {
				System.out.println("Disconnected");
			}
			
			@Override
			public void onConnect() {
				System.out.println("Connected");
			}
			
			@Override
			public void on(String event, JSONObject... args) {
				try {
					socket.emit("answer", new JSONObject().put("msg", "Hello again Socket.io!"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	
	socket.emit("hello", new JSONObject().put("msg", "Hello Socket.io! :D"));
	
For further informations, read the [Javadoc](http://s01.de/~tox/hgexport/io.socket/).

 * [Class SocketIO](http://s01.de/~tox/hgexport/io.socket/io/socket/SocketIO.html)
 * [Interface IOCallback](http://s01.de/~tox/hgexport/io.socket/io/socket/IOCallback.html)

## What is the architecture?
![Schema](https://github.com/Gottox/io.socket/raw/master/doc/schema.png)

Yea, I know, this is a stub...

## How to implement a transport?

An example can be found in [WebsocketTransport.java](http://github.com/Gottox/io.socket/blob/master/src/io/socket/transports/WebsocketTransport.java)

Create a class implementing the IOTransport interface.

### IOTransport

#### static IOTransport create(URL url, IOConnection connection)
 
Called by IOConnector to create a new Instance of the transport. The URL is the one you should connect to. Here you can rewrite the
url if needed, i.e. WebsocketTransport rewrites the incoming "http://" address to "ws://"
 
#### void connect();

Called by IOConnection. Here you should set up the connection.

#### void disconnect();

Called by IOConnection. This should shut down the connection. I'm currently not sure if this function is called multiple times.
So make sure, it doesn't crash if it's called more than once.

#### void send(String text) throws IOException;

Called by IOConnection. This call request you to send data to the server.

#### boolean canSendBulk();
 
If you can send more than one message at a time, return true. If not return false.

#### void sendBulk(String[] texts) throws IOException;

Basicly the same as send() but for multiple messages at a time. This is only called when canSendBulk returns true.

### IOConnection

Ok, now we know when our functions are called. But how do we tell io.socket to process messages we get? IOConnection which the
create() method gets provides methods do to this.

#### IOConnection.transportConnect()
 
Call this method when the connection is established an the socket is ready to send and receive data.
   
#### IOConnection.transportDisconnected()
   
Call this method when the connection is shot down. IOConnection will care about reconnecting, if it's feasibility.
   
#### IOConnection.transportError(Exception error)
 
Call this method when the connection is experiencing an error. IOConnection will take care about reconnecting or throwing an
error to the callbacks. Whatever makes more sense ;)
   
#### IOConnection.transportMessage(String message)
 
This should be called as soon as the transport has received data. IOConnection will take care about parsing the information and
calling the callbacks of the sockets.

## License - the boring stuff...

This library is distributed under MIT Licence.