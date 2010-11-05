package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AdHocCommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Test(timeout=60000)
	public void testTcpComms() throws Exception {
		aliceManager = PeerConfigurator.createTcpAdhocPeer("alice", 58000, tempStorage);
		bobManager = PeerConfigurator.createTcpAdhocPeer("bob", 58001, tempStorage);
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
	@Test(timeout=60000)
	public void testHttpComms() throws Exception {
		aliceManager = PeerConfigurator.createHttpAdhocPeer("alice", 58000, tempStorage);
		bobManager = PeerConfigurator.createHttpAdhocPeer("bob", 58001, tempStorage);
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
	@Test(timeout=60000)
	public void testHttp2Comms() throws Exception {
		aliceManager = PeerConfigurator.createHttp2AdhocPeer("alice", 58000, tempStorage);
		bobManager = PeerConfigurator.createHttp2AdhocPeer("bob", 58001, tempStorage);
		startPeers();
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}

	private void startPeers() throws Exception {
		aliceManager.startNetwork();
		bobManager.startNetwork();
	}
	
	@After
	public void killAlice() {
		if(aliceManager != null) {
			aliceManager.stopNetwork();
			aliceManager = null;
		}
	}
	
	@After
	public void killBob() {
		if(bobManager != null) {
			bobManager.stopNetwork();
			bobManager = null;
		}
	}
	
}
