package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests ad-hoc communication using the various physical transports supported by JXTA.
 * 
 * <p>
 * At present these tests do not pass reliably due to NetworkManager isolation issues.
 * Once these are fixed, this suite of tests can be enabled again.
 */
@Ignore("Peer isolation issues prevent tests from reliably passing")
public class AdHocCommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Test(timeout=60000)
	public void testTcpComms() throws Exception {
            String aliceInstanceName = "alice";
            String bobInstanceName = "bob";
            
            aliceManager = PeerConfigurator.createTcpAdhocPeer(aliceInstanceName, 58000, tempStorage);
            bobManager = PeerConfigurator.createTcpAdhocPeer(bobInstanceName, 58001, tempStorage);
            
            aliceManager.getConfigurator().setPrincipal(aliceInstanceName);
            bobManager.getConfigurator().setPrincipal(bobInstanceName);
                
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
