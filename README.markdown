[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=Gottox&url=https://github.com/Gottox/socket.io-java-client&title=socket.io-java-client&language=&tags=github&category=software)

# Socket.IO-Client for Java

socket.io-java-client is a simple implementation of [socket.io](http://socket.io) for Java.

It uses [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) as transport backend, but it's easy
to write your own transport. See description below.

The API is inspired by [java-socket.io.client](https://github.com/benkay/java-socket.io.client) but as the license
of this project was unclear and it had some nasty bugs, I decided to write socket.io-java-client from the scratch.

Features:

 * __transparent reconnecting__ - The API cares about re-establishing the connection to the server
   when the transport is interrupted.
 * __easy to use API__ - implement an interface, instantiate a class - you're done.
 * __output buffer__ - send data while the transport is still connecting. No problem, socket.io-java-client handles that.
 * __meaningful exceptions__ - if something goes wrong, SocketIO tries to throw meaningful exceptions with hints for fixing.

## How to use

Using socket.io-java-client is quite simple. But lets see:

``` java
	// Initialise a socket:
	SocketIO socket = new IOSocket("http://127.0.0.1:3001")
	socket.connect(new IOCallback() {
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
				System.out.println("Something went wrong. Lets exit");
				System.exit(0);
			}
			
			@Override
			public void onDisconnect() {
				System.out.println("Disconnected");
				System.exit(0);
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
	
	// This will be cached until the server is connected.
	socket.emit("hello", new JSONObject().put("msg", "Hello Socket.io! :D"));
```

For further informations, read the [Javadoc](http://s01.de/~tox/hgexport/socket.io-java-client/).

 * [Class SocketIO](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/SocketIO.html)
 * [Interface IOCallback](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/IOCallback.html)

## What is the architecture?
Read this if you want to investigate in socket.io-java-client.

![Schema](https://github.com/Gottox/socket.io-java-client/raw/master/doc/schema.png)

### What is the SocketIO class?

SocketIO is the API frontend. You can use this to connect to multiple hosts. If an
*IOConnection* object exists for a certian host, it will be reused as the
socket.io specs state.

[Javadoc](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/SocketIO.html)

### What is the IOConnection class?

This class is used to hold a connection to a socket.io server. It handles calling
callback functions of the corresponding *SocketIO* and reconnecting if the connection
is shut down ungracefully.

[Javadoc](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/IOConnection.html)

### What is the IOTransport interface?

This interface describes a connection to a host. The implementation can be fairly minimal,
as *IOConnection* does most of the work for you. Reconnecting, errorhandling, etc... is
handled by *IOConnection*.

[Javadoc](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/IOTransport.html)

## How to implement a transport?

An example can be found in [WebsocketTransport.java](http://github.com/Gottox/socket.io-java-client/blob/master/src/io/socket/transports/WebsocketTransport.java)

Create a class implementing the IOTransport interface.

### IOTransport

#### public static final String TRANSPORT_NAME
This constant should contain the name of the transport.

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

#### void invalidate();

After this call, the transport should not call any methods of IOConnection. It must not disconnect from the server.
This is the case when we're forcing a reconnect. If we disconnect gracefully from the server, it will terminate our
session.

### IOConnection

Ok, now we know when our functions are called. But how do we tell socket.io-java-client to process messages we get?
The provided IOConnection does the trick.

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

### Changes to IOConnection

Now IOConnection needs to instantiate the transpost look at the sourcecode of IOConnection and search for the connectTransport() method.
It's part of the ConnectThread inner class.

add a new else if branch to the section. I.e.:

``` java
	...
	else if (protocols.contains(MyTransport.TRANSPORT_NAME))
		transport = MyTransport.create(url, IOConnection.this);
	...
```
## GWT?

I haven't tried it. But it would be great to get it working on GWT. Please let me know, if you've got it working.

## License - the boring stuff...

This library is distributed under MIT Licence.