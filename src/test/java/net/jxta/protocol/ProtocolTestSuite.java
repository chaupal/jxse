/**
 * This class can be used to combine various tests
 */
package net.jxta.protocol;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Ignore
@Suite.SuiteClasses({
	DiscoveryQueryMsgTest.class,
	DiscoveryResponseMsgTest.class,
	GroupConfigTest.class,
	PeerAdvertisementTest.class,
	PipeAdvertisementTest.class,
	RdvAdvertisementTest.class,
	ResolverMsgTest.class,
	ResolverQueryMsgTest.class,
	ResolverResponseMsgTest.class,
	ResolverSrdiMsgTest.class,
	SignedAdvertisementTest.class,
	SrdiMessageTest.class,
	TestRouteAdv.class

})
public class ProtocolTestSuite {}