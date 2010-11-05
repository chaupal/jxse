package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * FIXME: This is the way I would *like* the relay tests to be done, but it does not work at
 * present as there are still isolation issues between the peers. It seems this issue is
 * particularly apparent when a relay is being used, as it is not possible to start a new
 * relay peer after the first relay is shut down. This warrants further exploration, but
 * for now this test set will be marked as ignored and each test will be done in a separate
 * class, which will ensure they are run in separate VMs and therefore no contamination
 * between runs is possible.
 */
@Ignore
public class RelayedCommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager relayManager;
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Test(timeout=60000)
	public void testTcpComms() throws Exception {
		relayManager = PeerConfigurator.createTcpRdvRelayPeer("relay", 58000, tempStorage);
		aliceManager = PeerConfigurator.createTcpClientPeer("alice", relayManager, tempStorage);
		bobManager = PeerConfigurator.createTcpClientPeer("bob", relayManager, tempStorage);
		
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
	@Test(timeout=60000)
	public void testHttpComms() throws Exception {
		relayManager = PeerConfigurator.createHttpRdvRelayPeer("relay", 58000, tempStorage);
		aliceManager = PeerConfigurator.createHttpClientPeer("alice", relayManager, tempStorage);
		bobManager = PeerConfigurator.createHttpClientPeer("bob", relayManager, tempStorage);
		
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
	@Test(timeout=60000)
	public void testHttp2Comms() throws Exception {
		relayManager = PeerConfigurator.createHttp2RdvRelayPeer("relay", 58000, tempStorage);
		aliceManager = PeerConfigurator.createHttp2ClientPeer("alice", relayManager, tempStorage);
		bobManager = PeerConfigurator.createHttp2ClientPeer("bob", relayManager, tempStorage);
		
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}

	private void startPeers() throws Exception {
		relayManager.startNetwork();
		aliceManager.startNetwork();
		bobManager.startNetwork();
		
		/* XXX: frustratingly, the tests do not pass reliably unless time is given to 
         * the peers to connect to the Rdv/Relay peer and settle down. Otherwise, the 
         * pipe accept request is never received by alice unless bob explicitly retries.
		 */
		Thread.sleep(5000L);
	}
	
	@After
	public void killRelayPeer() throws Exception {
		if(relayManager != null) {
			relayManager.stopNetwork();
			relayManager = null;
		}
	}
		
	@After
	public void killAlicePeer() throws Exception {
		if(aliceManager != null) {
			aliceManager.stopNetwork();
			aliceManager = null;
		}
	}
	
	@After
	public void killBobPeer() throws Exception {
		if(bobManager != null) {
			bobManager.stopNetwork();
			bobManager = null;
		}
	}
}
