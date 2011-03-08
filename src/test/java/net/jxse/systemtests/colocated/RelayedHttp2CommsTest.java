package net.jxse.systemtests.colocated;

import java.net.URI;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

@Ignore
public class RelayedHttp2CommsTest {


	@Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();
    
    private NetworkManager aliceManager;
    private NetworkManager bobManager;
    private NetworkManager relayManager;
	
    @Before
    public void initPeers() throws Exception {
    	URI relayURI = URI.create("http2://127.0.0.101:50000");
    	relayManager = configureRelay("relay", "127.0.0.101", 50000);
    	
        aliceManager = configurePeer("alice", relayURI);
        bobManager = configurePeer("bob", relayURI);
        
        relayManager.startNetwork();
        aliceManager.startNetwork();
        bobManager.startNetwork();
    }
    
    private NetworkManager configureRelay(String relayName, String interfaceAddr, int tcpPort) throws Exception {
		NetworkManager manager = new NetworkManager(ConfigMode.RENDEZVOUS_RELAY, relayName, tempStorage.newFolder(relayName).toURI());
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setUseMulticast(false);
		configurator.setUseOnlyRelaySeeds(true);
		configurator.setUseOnlyRendezvousSeeds(true);
		
		configurator.setTcpEnabled(false);
		configurator.setHttpEnabled(false);
		
		configurator.setHttp2Enabled(true);
		configurator.setHttp2Incoming(true);
		configurator.setHttp2Outgoing(true);
		configurator.setHttp2InterfaceAddress(interfaceAddr);
		configurator.setHttp2Port(tcpPort);
		
		return manager;
	}

	private NetworkManager configurePeer(String peerName, URI relayRdvURI) throws Exception {
    	NetworkManager manager = new NetworkManager(ConfigMode.EDGE, peerName, tempStorage.newFolder(peerName).toURI());
    	NetworkConfigurator configurator = manager.getConfigurator();
    	configurator.setUseMulticast(false);
    	configurator.setUseOnlyRelaySeeds(true);
    	configurator.setUseOnlyRendezvousSeeds(true);
    	
    	configurator.setTcpEnabled(false);
    	configurator.setHttpEnabled(false);
    	
    	configurator.setHttp2Enabled(true);
    	configurator.setHttp2Incoming(false);
    	configurator.setHttp2Outgoing(true);
    	
    	configurator.addSeedRelay(relayRdvURI);
    	configurator.addSeedRendezvous(relayRdvURI);
    	return manager;
	}

	@After
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
        relayManager.stopNetwork();
    }
	
	@Test(timeout=30000)
	public void testComms() throws Exception {
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
        SystemTestUtils.testPeerPropagatedCommunication(aliceManager, bobManager);
	}
}
