package net.jxse.systemtests.colocated;

import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.platform.NetworkManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests ad-hoc mode communication using the HTTP2 transport. This test exists within
 * {@link AdHocCommsTest} however due to peer isolation issues it must currently
 * be run separately - placing it in it's own class ensures it is run in a forked
 * VM.
 */
public class AdHocHttp2CommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Before
	public void createPeers() throws Exception {
                String instanceName = "alice";                
                
		aliceManager = PeerConfigurator.createHttp2AdhocPeer(instanceName, 58000, tempStorage);
                aliceManager.getConfigurator().setPrincipal(instanceName);
                aliceManager.startNetwork();
                
                // give the network managers time to stabilise
		Thread.sleep(5000L);
                
                instanceName = "bob";
		bobManager = PeerConfigurator.createHttp2AdhocPeer(instanceName, 58001, tempStorage);               
                bobManager.getConfigurator().setPrincipal(instanceName);
                bobManager.startNetwork();
		
		// give the network managers time to stabilise
		Thread.sleep(5000L);
	}
	
	@Test
	public void testComms() throws Exception {
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
	
    @After
    public void terminatePeers() throws Exception {        
        aliceManager.stopNetwork();
        if (!aliceManager.isStarted()) {
            bobManager.stopNetwork();
        }
    }	
}
