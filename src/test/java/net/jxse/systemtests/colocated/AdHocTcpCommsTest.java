package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests ad-hoc mode communication using the TCP transport. This test exists within
 * {@link AdHocCommsTest} however due to peer isolation issues it must currently
 * be run separately - placing it in it's own class ensures it is run in a forked
 * VM.
 */
public class AdHocTcpCommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Before
	public void createPeers() throws Exception {
		String aliceInstanceName = "alice";
                String bobInstanceName = "bob";

                aliceManager = PeerConfigurator.createTcpAdhocPeer(aliceInstanceName, 58000, tempStorage);
                bobManager = PeerConfigurator.createTcpAdhocPeer(bobInstanceName, 58001, tempStorage);

                aliceManager.getConfigurator().setPrincipal(aliceInstanceName);
                bobManager.getConfigurator().setPrincipal(bobInstanceName);
		
		aliceManager.startNetwork();
		bobManager.startNetwork();
		
		// XXX: give the network managers time to stabilise
		Thread.sleep(5000L);
	}
	
	@Test
	public void testComms() throws Exception {
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
	@After
	public void killAlice() throws Exception {
		aliceManager.stopNetwork();
	}
	
	@After
	public void killBob() throws Exception {
		bobManager.stopNetwork();
	}
	
}
