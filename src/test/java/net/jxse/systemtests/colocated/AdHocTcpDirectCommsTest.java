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

public class AdHocTcpDirectCommsTest {

    @Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();

    private NetworkManager aliceManager;
    private NetworkManager bobManager;

    @Before
    public void initPeers() throws Exception {
        aliceManager = new NetworkManager(ConfigMode.ADHOC, "alice", tempStorage.newFolder("alice").toURI());
        configureForTcp(aliceManager, 59080);
        bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", tempStorage.newFolder("bob").toURI());
        configureForTcp(bobManager, 58081);

        aliceManager.startNetwork();
        bobManager.startNetwork();
    }

    private void configureForTcp(NetworkManager manager, int port) throws IOException {
		NetworkConfigurator configurator = manager.getConfigurator();
		configurator.setTcpEnabled(true);
		configurator.setHttpEnabled(false);
		configurator.setHttp2Enabled(false);

		configurator.setTcpIncoming(true);
		configurator.setTcpOutgoing(true);
		configurator.setTcpPort(port);
		configurator.setTcpStartPort(port);
		configurator.setTcpEndPort(port+100);
	}

    @After
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
    }

    @Test(timeout=10000)
    public void testColocatedPeerBidiPipeComms() throws Exception {
        SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
    }

        public static void main(String[] args) {
            try {
                AdHocTcpDirectCommsTest t = new AdHocTcpDirectCommsTest();
                t.initPeers();
                t.testColocatedPeerBidiPipeComms();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
}
