package net.jxse.systemtests.colocated;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;

public class AdHocHttpDirectCommsTest {
	
	@Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();
	
	private NetworkManager aliceManager;
	private NetworkManager bobManager;
	
	@Before
	public void initPeers() throws Exception {
		aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", tempStorage.newFolder("alice").toURI());
		configureForHttp(aliceManager, 59901);
		bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", tempStorage.newFolder("bob").toURI());
		configureForHttp(bobManager, 59902);
		
		aliceManager.startNetwork();
		bobManager.startNetwork();
	}
	
	@After
	public void killPeers() {
		aliceManager.stopNetwork();
		bobManager.stopNetwork();
	}

	private void configureForHttp(NetworkManager manager, int port) throws IOException {
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setTcpEnabled(false);
		configurator.setHttp2Enabled(false);
		
		configurator.setHttpEnabled(true);
		configurator.setHttpIncoming(true);
		configurator.setHttpOutgoing(true);
		configurator.setHttpPort(port);
	}
	
	@Test
	public void testSimpleComms() throws Exception {
		SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
        SystemTestUtils.testPeerPropagatedCommunication(aliceManager, bobManager);
	}
}
