/**
 * This class can be used to combine various tests
 */
package net.jxse.suite;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import net.jxse.systemtests.colocated.AdHocHttp2CommsTest;
import net.jxse.systemtests.colocated.RelayedHttp2CommsTest;
import net.jxse.systemtests.colocated.RelayedHttpCommsTest;
import net.jxse.systemtests.colocated.pipes.BlockingPipeAcceptTest;
import net.jxta.socket.SocketServerTest;

@RunWith(Suite.class)
@Ignore
@Suite.SuiteClasses({
	AdHocHttp2CommsTest.class,
	RelayedHttp2CommsTest.class,
	SocketServerTest.class,
	RelayedHttpCommsTest.class,
	BlockingPipeAcceptTest.class

})
public class TestSuite {}