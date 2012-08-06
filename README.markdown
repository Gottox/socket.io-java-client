[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=Gottox&url=https://github.com/Gottox/socket.io-java-client&title=socket.io-java-client&language=&tags=github&category=software)

# Socket.IO-Client for Java

socket.io-java-client is an easy to use implementation of [socket.io](http://socket.io) for Java.

It uses [Weberknecht](http://code.google.com/p/weberknecht/) as transport backend, but it's easy
to write your own transport. See description below. An XHR-Transport is included, too. But it's
not functional in its current state.

The API is inspired by [java-socket.io.client](https://github.com/benkay/java-socket.io.client).

Features:

 * __transparent reconnecting__ - The API cares about re-establishing the connection to the server
   when the transport is interrupted.
 * __easy to use API__ - implement an interface, instantiate a class - you're done.
 * __output buffer__ - send data while the transport is still connecting. No problem, socket.io-java-client handles that.
 * __meaningful exceptions__ - If something goes wrong, SocketIO tries to throw meaningful exceptions with hints for fixing.

__Status:__ Connecting with Websocket is production ready. XHR is in beta.


## How to use

Using socket.io-java-client is quite simple. But lets see:

Checkout and compile the project:

``` bash
git clone git://github.com/Gottox/socket.io-java-client.git
cd socket.io-java-client
ant jar
mv jar/socketio.jar /path/to/your/libs/project
```

If you're using ant, change your build.xml to include socketio.jar. If you're eclipse, add the jar to your project buildpath.

Afterwards, you'll be able to use this library: 

``` java

		SocketIO socket = new SocketIO("http://127.0.0.1:3001/");
		socket.connect(new IOCallback() {
			@Override
			public void onMessage(JSONObject json, IOAcknowledge ack) {
				try {
					System.out.println("Server said:" + json.toString(2));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onMessage(String data, IOAcknowledge ack) {
				System.out.println("Server said: " + data);
			}

			@Override
			public void onError(SocketIOException socketIOException) {
				System.out.println("an Error occured");
				socketIOException.printStackTrace();
			}

			@Override
			public void onDisconnect() {
				System.out.println("Connection terminated.");
			}

			@Override
			public void onConnect() {
				System.out.println("Connection established");
			}

			@Override
			public void on(String event, IOAcknowledge ack, Object... args) {
				System.out.println("Server triggered event '" + event + "'");
			}
		});
		
		// This line is cached until the connection is establisched.
		socket.send("Hello Server!");

```

For further informations, read the [Javadoc](http://s01.de/~tox/hgexport/socket.io-java-client/).

 * [Class SocketIO](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/SocketIO.html)
 * [Interface IOCallback](http://s01.de/~tox/hgexport/socket.io-java-client/io/socket/IOCallback.html)
 
## Checkout

 * with git
 
		git clone git://github.com/Gottox/socket.io-java-client.git

 * with mercurial
 
 		hg clone https://bitbucket.org/Gottox/socket.io-java-client 
 
Both repositories are synchronized and up to date.

## Building

to build a jar-file:

	cd $PATH_TO_SOCKETIO_JAVA
	ant jar
	ls jar/socketio.jar

You'll find the socket.io-jar in jar/socketio.jar 

## Bugs

Please report any bugs feature requests to [the Github issue tracker](https://github.com/Gottox/socket.io-java-client/issues)

## Frameworks

This Library was designed with portability in mind.

* __Android__ is fully supported.
* __JRE__ is fully supported.
* __GWT__ does not work at the moment, but a port would be possible.
* __JavaME__ untested.
* ... is there anything else out there?

## Testing

There comes a JUnit test suite with socket.io-java-client. Currently it's tested with Eclipse.

You need node installed in PATH.

 * open the project with eclipse
 * open tests/io.socket/AllTests.java
 * run it as JUnit4 test.

## TODO

* Socket.io needs more unit-tests.
* XhrTransport needs to pass all tests.
* If websockets are failing (due to proxy servers e.g.), use XHR automaticly instead.

## License - the boring stuff...

This library is distributed under MIT Licence.

## Sounds so interesting...

You'll find further documentation at the [Socket.io-java-client Github Wiki](https://github.com/Gottox/socket.io-java-client/wiki)