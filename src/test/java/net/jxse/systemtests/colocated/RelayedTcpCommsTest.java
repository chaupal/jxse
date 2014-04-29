package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RelayedTcpCommsTest {

	@Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();

    private NetworkManager aliceManager;
    private NetworkManager bobManager;
    private NetworkManager relayManager;
	
    @Before
    public void initPeers() throws Exception {
    	    
        String instanceName = "relay";
    	relayManager = PeerConfigurator.createTcpRdvRelayPeer(instanceName, 50000, tempStorage);
        relayManager.getConfigurator().setPrincipal(instanceName);
        relayManager.startNetwork();
        
        instanceName = "alice";
        aliceManager = PeerConfigurator.createTcpClientPeer(instanceName, relayManager, tempStorage);
        aliceManager.getConfigurator().setPrincipal(instanceName);
        aliceManager.startNetwork();
        
        instanceName = "bob";
        bobManager = PeerConfigurator.createTcpClientPeer(instanceName, relayManager, tempStorage);
        bobManager.getConfigurator().setPrincipal(instanceName);
        bobManager.startNetwork();
        
        Thread.sleep(5000);
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
	}
}
