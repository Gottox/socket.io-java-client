package io.socket;

import org.junit.BeforeClass;

public class XHRTestSocketIO extends AbstractTestSocketIO {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		transport = "xhr-polling";
	}
}
