package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Set of tests which send messages as quickly as possible on a single pipe, to ensure that
 * overall system stability is not compromised under load.
 */
@Ignore("slow test")
public class AdHocPipeLoadTest {

	public static final int NUM_MESSAGES = 1000;
	public static final int MESSAGE_SIZE = 1024 * 32;
	
	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Test(timeout=60000)
	public void testTcp() throws Exception {
		aliceManager = PeerConfigurator.createTcpAdhocPeer("alice", 50000, tempStorage);
		bobManager = PeerConfigurator.createTcpAdhocPeer("bob", 50001, tempStorage);
		startPeers();
		LoadTester.loadTestSinglePipe(aliceManager, bobManager, NUM_MESSAGES, MESSAGE_SIZE);
	}
	
	@Test(timeout=60000)
	public void testHttp() throws Exception {
		aliceManager = PeerConfigurator.createHttpAdhocPeer("alice", 50000, tempStorage);
		bobManager = PeerConfigurator.createHttpAdhocPeer("bob", 50001, tempStorage);
		startPeers();
		LoadTester.loadTestSinglePipe(aliceManager, bobManager, NUM_MESSAGES, MESSAGE_SIZE);
	}
	
	@Test(timeout=60000)
	public void testHttp2() throws Exception {
		aliceManager = PeerConfigurator.createHttp2AdhocPeer("alice", 50000, tempStorage);
		bobManager = PeerConfigurator.createHttp2AdhocPeer("bob", 50001, tempStorage);
		startPeers();
		LoadTester.loadTestSinglePipe(aliceManager, bobManager, NUM_MESSAGES, MESSAGE_SIZE);
	}
	
	
	private void startPeers() throws Exception {
		aliceManager.startNetwork();
		bobManager.startNetwork();
		
		Thread.sleep(5000);
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
