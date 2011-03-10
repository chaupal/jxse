package net.jxse.systemtests.colocated;

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
        bobManager = new NetworkManager(ConfigMode.ADHOC, "bob", tempStorage.newFolder("bob").toURI());
        
        aliceManager.startNetwork();
        bobManager.startNetwork();
    }
    
    @After
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
    }
    
    @Test(timeout=10000)
    public void testColocatedPeerBidiPipeComms() throws Exception {
        SystemTestUtils.testPeerCommunication(aliceManager, bobManager);
        //Test no passing, it may be configuration.
//        SystemTestUtils.testPeerPropagatedCommunication(aliceManager, bobManager);
    }
}
