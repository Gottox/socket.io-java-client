/*
 * socket.io-java-client AbstractTestSocketIO.java
 *
 * Copyright (c) 2012, Enno Boland
 * PROJECT DESCRIPTION
 * 
 * See LICENSE file for more information
 */
package io.socket;

import static org.junit.Assert.*;

import io.socket.testutils.MutateProxy;

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

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractTestSocketIO.
 */
@RunWith(io.socket.testutils.RandomBlockJUnit4ClassRunner.class)
public abstract class AbstractTestSocketIO implements IOCallback {

	private static final String REQUEST_ACKNOWLEDGE = "requestAcknowledge";

	/** The Constant to the node executable */
	private final static String NODE = "/usr/local/bin/node";

	/** The port of this test, randomly choosed */
	private int port = -1;

	/** Timeout for the tests */
	private static final int TIMEOUT = 3000;

	/** Received queues. */
	LinkedBlockingQueue<String> events;

	/** stdout of the node executable */
	LinkedBlockingQueue<String> outputs;

	/** Received arguments of events */
	LinkedBlockingQueue<Object> args;

	/** Thread for processing stdout */
	Thread stdoutThread;

	/** Thread for processing stderr */
	Thread stderrThread;

	/** The node process. */
	private Process node;

	/** The socket to test. */
	private SocketIO socket;

	private MutateProxy proxy = null;

	/** The transport of this test */
	static protected String transport = null;

	/**
	 * Tear down after class.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets up the test. Starts the node testserver on a randomly choosed port,
	 * starts backgroundthreads for processing stdin/stdout. Adds shutdown-hook
	 * for clean kill of the node server.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		assertNotNull("Transport is set correctly", transport);
		events = new LinkedBlockingQueue<String>();
		outputs = new LinkedBlockingQueue<String>();
		args = new LinkedBlockingQueue<Object>();
		System.out.println("Connect with " + transport);
		node = Runtime.getRuntime().exec(
				new String[] { NODE, "./tests/io/socket/testutils/socketio.js",
						"" + getPort(), transport });
		proxy = new MutateProxy(getPort() + 1, getPort());
		proxy.start();

		stdoutThread = new Thread("stdoutThread") {
			@Override
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
			@Override
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

	/**
	 * Tears down this test. Assures queues are empty.
	 * 
	 * @throws Exception
	 *             the exception
	 */
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

	/**
	 * Sets up a {@link SocketIO} connection.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	void doConnect() throws Exception {
		// Setting up socket connection
		socket = new SocketIO("http://127.0.0.1:" + getProxyPort() + "/main",
				this);
		assertEquals("onConnect", takeEvent());
		assertEquals(transport, socket.getTransport());
	}

	/**
	 * Closes a {@link SocketIO} connection.
	 * 
	 * @throws Exception
	 *             the exception
	 */
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

	/**
	 * Tests sending of a message to the server. Assures result by stdout.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
	public void send() throws Exception {
		doConnect();
		String str = "TESTSTRING";
		socket.send(str);
		assertEquals("MESSAGE:" + str, takeLine());
		doClose();
	}

	/**
	 * Emit and on.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
	public void emitAndOn() throws Exception {
		doConnect();

		socket.emit("echo");
		assertEquals("Test String", "on", takeEvent());

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

	/**
	 * Emit and message.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
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

	/**
	 * Namespaces.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
	public void namespaces() throws Exception {
		SocketIO ns1 = new SocketIO("http://127.0.0.1:" + getProxyPort()
				+ "/ns1", this);
		assertEquals("onConnect", takeEvent());

		doConnect();
		ns1.disconnect();
		assertEquals("onDisconnect", takeEvent());

		SocketIO ns2 = new SocketIO("http://127.0.0.1:" + getProxyPort()
				+ "/ns2", this);
		assertEquals("onConnect", takeEvent());
		assertEquals("onMessage_string", takeEvent());
		assertEquals("ns2", takeArg());

		socket.emit("defaultns", "TESTSTRING");
		assertEquals("onMessage_string", takeEvent());
		assertEquals("TESTSTRING", takeArg());
		assertEquals("onMessage_string", takeEvent());
		assertEquals("TESTSTRING", takeArg());

		SocketIO ns2_2 = new SocketIO("http://127.0.0.1:" + getProxyPort()
				+ "/ns2", this);
		assertEquals("onConnect", takeEvent());

		assertEquals("onMessage_string", takeEvent());
		assertEquals("ns2", takeArg());

		ns2_2.disconnect();
		ns2.disconnect();
		assertEquals("onDisconnect", takeEvent());
		assertEquals("onDisconnect", takeEvent());
		doClose();
	}

	/**
	 * Error.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
	public void error() throws Exception {
		doConnect();
		new SocketIO("http://127.0.0.1:1024/", this);
		assertEquals("onError", takeEvent());
		doClose();
	}

	/**
	 * Acknowledge.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test(timeout = TIMEOUT)
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

		socket.emit(REQUEST_ACKNOWLEDGE, "TESTSTRING");
		assertEquals("on", takeEvent());
		assertEquals("TESTSTRING", takeArg());
		assertEquals("ACKNOWLEDGE:TESTSTRING", takeLine());
		doClose();
	}

	@Test(timeout = TIMEOUT)
	public void reconnectInvalidated() throws Exception {
		doConnect();
		socket.disconnect();
		try {
			socket.connect(this);
			fail("reconnecting an invalidated socket should fail");
		} catch (RuntimeException ex) {
		}
	}

	@Test(timeout = TIMEOUT)
	public void sendUtf8() throws Exception {
		doConnect();
		socket.emit("fooo", "\uD83C\uDF84");
		socket.emit("fooo", "🎄");
		assertEquals("on", takeEvent());
		doClose();
	}
	
	// END TESTS

	/**
	 * Take event.
	 * 
	 * @return the string
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	String takeEvent() throws InterruptedException {
		String event = events.poll(TIMEOUT, TimeUnit.SECONDS);
		if (event == null) {
			fail("takeEvent Timeout!");
		}
		System.out.println("Event Taken: " + event);
		return event;
	}

	/**
	 * Take line.
	 * 
	 * @return the string
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	String takeLine() throws InterruptedException {
		String line = outputs.poll(TIMEOUT, TimeUnit.SECONDS);
		if (line == null) {
			fail("takeLine Timeout!");
		}
		System.out.println("Line Taken: " + line);
		return line;
	}

	/**
	 * Take arg.
	 * 
	 * @return the object
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	Object takeArg() throws InterruptedException {
		Object arg = args.poll(TIMEOUT, TimeUnit.MILLISECONDS);
		if (arg == null) {
			fail("takeArg Timeout!");
		}
		System.out.println("Argument Taken: " + arg);
		return arg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#onDisconnect()
	 */
	@Override
	public void onDisconnect() {
		events.add("onDisconnect");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#onConnect()
	 */
	@Override
	public void onConnect() {
		System.out.println("onConnect");
		events.add("onConnect");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#onMessage(java.lang.String,
	 * io.socket.IOAcknowledge)
	 */
	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		events.add("onMessage_string");

		this.args.add(data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#onMessage(org.json.JSONObject,
	 * io.socket.IOAcknowledge)
	 */
	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		events.add("onMessage_json");
		this.args.add(json);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#on(java.lang.String, io.socket.IOAcknowledge,
	 * java.lang.Object[])
	 */
	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
		events.add("on");
		if (event.equals(REQUEST_ACKNOWLEDGE)) {
			ack.ack(args);
		}
		this.args.addAll(Arrays.asList(args));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.socket.IOCallback#onError(io.socket.SocketIOException)
	 */
	@Override
	public void onError(SocketIOException socketIOException) {
		socketIOException.printStackTrace();
		events.add("onError");
	}

	/**
	 * Gets the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		if (port == -1)
			port = 2048 + (int) (Math.random() * 10000) * 2;
		return port;
	}

	public int getProxyPort() {
		return getPort() + (proxy == null ? 0 : 1);
	}
}
