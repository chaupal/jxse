/**
 * This class can be used to combine various tests
 */
package net.jxta.endpoint;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Ignore
@Suite.SuiteClasses({
	ByteArrayMessageElementTest.class,
	EndpointAddressTest.class,
	InputStreamMessageElementTest.class,
	MessageTest.class,
	MessengerStateBarrierTest.class,
	MessengerStateListenerListTest.class,
	SerializationPerformanceTest.class,
	StringMessageElementTest.class
})
public class EndpointTestSuite {}