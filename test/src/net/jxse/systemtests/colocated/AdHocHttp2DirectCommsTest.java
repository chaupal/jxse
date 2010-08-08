package net.jxse.systemtests.colocated;

import java.io.IOException;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AdHocHttp2DirectCommsTest {

	@Rule
	public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Before
	public void initPeers() throws Exception {
        aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", tempStorage.newFolder("alice").toURI());
        configureForHttp2(aliceManager, 58080);
        bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", tempStorage.newFolder("bob").toURI());
        configureForHttp2(bobManager, 58081);
        
        aliceManager.startNetwork();
        bobManager.startNetwork();
    }
    
    private void configureForHttp2(NetworkManager manager, int port) throws IOException {
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setTcpEnabled(false);
		configurator.setHttpEnabled(false);
		
		configurator.setHttp2Enabled(true);
		configurator.setHttp2Incoming(true);
		configurator.setHttp2Outgoing(true);
		configurator.setHttp2Port(port);
	}

	@After
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
    }
	
	@Test
	public void testSimpleComms() throws Exception {
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
	}
}
