package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RelayedHttp2CommsTest {

	@Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();

    private NetworkManager aliceManager;
    private NetworkManager bobManager;
    private NetworkManager relayManager;
	
    @Before
    public void initPeers() throws Exception {
    	relayManager = PeerConfigurator.createHttp2RdvRelayPeer("relay", 50000, tempStorage);
        aliceManager = PeerConfigurator.createHttp2ClientPeer("alice", relayManager, tempStorage);
        bobManager = PeerConfigurator.createHttp2ClientPeer("bob", relayManager, tempStorage);

        relayManager.startNetwork();
        aliceManager.startNetwork();
        bobManager.startNetwork();
        
        Thread.sleep(5000L);
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
