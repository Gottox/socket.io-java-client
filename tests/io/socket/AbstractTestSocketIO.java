package io.socket;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(io.socket.RandomBlockJUnit4ClassRunner.class)
public abstract class AbstractTestSocketIO implements IOCallback {
	private final static String NODE = "node";
	private int port = -1;
	private static final int TIMEOUT = 1000;
	LinkedBlockingQueue<String> events;
	LinkedBlockingQueue<String> outputs;
	LinkedBlockingQueue<Object> args;

	Thread stdoutThread;
	Thread stderrThread;
	private Process node;
	private SocketIO socket;
	static protected String transport = null;

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		assertNotNull("Transport is set correctly", transport);
		events = new LinkedBlockingQueue<String>();
		outputs = new LinkedBlockingQueue<String>();
		args = new LinkedBlockingQueue<Object>();
		System.out.println("Connect with " + transport);
		node = Runtime.getRuntime().exec(
				new String[] { NODE, "./tests/io/socket/socketio.js",
						"" + getPort(), transport });

		stdoutThread = new Thread("stdoutThread") {
			public void run() {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(node.getInputStream()));
				String line;
				try {
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("__:")) {
							System.out.println("Out: " + line);
							outputs.add(line.substring("__:".length()));
						} else
							System.out.println("Node: " + line);
					}
				} catch (IOException e) {
					if (!interrupted()) {
						e.printStackTrace();
						System.err.println("Node read error");
					}
				}
				System.err.println("Node output end");
			}
		};
		stderrThread = new Thread("stderrThread") {
			public void run() {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(node.getErrorStream()));
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						System.err.println("Node: " + line);
					}
				} catch (IOException e) {
					if (!interrupted()) {
						e.printStackTrace();
						System.err.println("Node read error");
					}
				}
				System.err.println("Node output end");
			};
		};
		stderrThread.start();
		stdoutThread.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					node.destroy();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		assertEquals("OK", takeLine());
	}

	@After
	public void tearDown() throws Exception {

		node.destroy();
		stderrThread.interrupt();
		stdoutThread.interrupt();
		node.waitFor();
		for (String s : events) {
			System.out.println("Event in Queue: " + s);
		}
		for (String line : outputs) {
			System.out.println("Line in Queue: " + line);
		}
		for (Object o : args) {
			System.out.println("Argument in Queue: " + o.toString());
		}
	}

	void doConnect() throws Exception {
		// Setting up socket connection
		socket = new SocketIO("http://127.0.0.1:" + getPort(), this);
		assertEquals("onConnect", takeEvent());
		assertEquals(transport, socket.getTransport());
	}

	void doClose() throws Exception {
		socket.disconnect();
		assertEquals("onDisconnect", takeEvent());

		while (outputs.size() != 0) {
			fail("Line in queue: " + outputs.poll());
		}
		while (events.size() != 0) {
			fail("Event in queue: " + events.poll());
		}
		while (args.size() != 0) {
			fail("Arguments in queue: " + args.poll());
		}
	}

	// BEGIN TESTS

	@Test(timeout=TIMEOUT)
	public void send() throws Exception {
		doConnect();
		String str = "TESTSTRING";
		socket.send(str);
		assertEquals("MESSAGE:" + str, takeLine());
		doClose();
	}

	@Test(timeout=TIMEOUT)
	public void emitAndOn() throws Exception {
		doConnect();

		String str = "TESTSTRING";
		socket.emit("echo", str);
		assertEquals("Test String", "on", takeEvent());
		assertEquals(str, takeArg());

		JSONObject obj = new JSONObject("{'foo':'bar'}");
		socket.emit("echo", obj);
		assertEquals("Test JSON", "on", takeEvent());
		assertEquals(obj.toString(), takeArg().toString());

		doClose();
	}

	@Test(timeout=TIMEOUT)
	public void emitAndMessage() throws Exception {
		doConnect();
		String str = "TESTSTRING";
		socket.emit("echoSend", str);
		assertEquals("onMessage_string", events.take());
		assertEquals(str, takeArg());

		/*
		 * // Server sends us a string instead of a JSONObject, strange thing
		 * JSONObject obj = new JSONObject("{'foo':'bar'}");
		 * socket.emit("echoSend", obj); assertEquals("Test JSON",
		 * "onMessage_json", takeEvent()); assertEquals(obj.toString(),
		 * takeArg().toString());
		 */
		doClose();
	}

	@Test(timeout=TIMEOUT)
	public void namespaces() throws Exception {
		SocketIO ns1 = new SocketIO("http://127.0.0.1:" + getPort() + "/ns1", this);
		assertEquals("onConnect", takeEvent());

		// In some very rare cases, it is possible to receive data on an socket
		// which isn't connected yet, this sleep assures that these events
		// aren't submitted. This is a server side problem. Maybe socket.io-java
		// could cache these events until the server drops the connect event.
		Thread.sleep(100);
		doConnect();

		ns1.disconnect();
		assertEquals("onDisconnect", takeEvent());

		SocketIO ns2 = new SocketIO("http://127.0.0.1:" + getPort() + "/ns2", this);
		assertEquals("onConnect", takeEvent());

		assertEquals("onMessage_string", takeEvent());
		assertEquals("ns2", takeArg());

		SocketIO ns2_2 = new SocketIO("http://127.0.0.1:" + getPort() + "/ns2", this);
		assertEquals("onConnect", takeEvent());

		assertEquals("onMessage_string", takeEvent());
		assertEquals("ns2", takeArg());

		ns2_2.disconnect();
		ns2.disconnect();
		assertEquals("onDisconnect", takeEvent());
		assertEquals("onDisconnect", takeEvent());
		doClose();
	}

	@Test(timeout=TIMEOUT)
	public void error() throws Exception {
		doConnect();
		new SocketIO("http://127.0.0.1:1024/", this);
		assertEquals("onError", takeEvent());
		doClose();
	}

	@Test(timeout=TIMEOUT)
	public void acknowledge() throws Exception {
		doConnect();
		socket.emit("echoAck", new IOAcknowledge() {
			@Override
			public void ack(Object... args) {
				events.add("ack");
				AbstractTestSocketIO.this.args.addAll(Arrays.asList(args));
			}
		}, "TESTSTRING");
		assertEquals("ack", takeEvent());
		assertEquals("TESTSTRING", takeArg());
		doClose();
	}

	// END TESTS

	String takeEvent() throws InterruptedException {
		String event = events.poll(TIMEOUT, TimeUnit.SECONDS);
		if (event == null) {
			fail("takeEvent Timeout!");
		}
		System.out.println("Event Taken: " + event);
		return event;
	}

	String takeLine() throws InterruptedException {
		String line = outputs.poll(TIMEOUT, TimeUnit.SECONDS);
		if (line == null) {
			fail("takeLine Timeout!");
		}
		System.out.println("Line Taken: " + line);
		return line;
	}

	Object takeArg() throws InterruptedException {
		Object arg = args.poll(TIMEOUT, TimeUnit.MILLISECONDS);
		if (arg == null) {
			fail("takeArg Timeout!");
		}
		System.out.println("Argument Taken: " + arg);
		return arg;
	}

	@Override
	public void onDisconnect() {
		events.add("onDisconnect");
	}

	@Override
	public void onConnect() {
		events.add("onConnect");
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		events.add("onMessage_string");
		this.args.add(data);
	}

	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		events.add("onMessage_json");
		this.args.add(json);
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
		events.add("on");
		this.args.addAll(Arrays.asList(args));
	}

	@Override
	public void onError(SocketIOException socketIOException) {
		events.add("onError");
	}

	public int getPort() {
		if(port == -1)
			port = 2048 + (int)(Math.random() * 10000);
		return port;
	}

}
