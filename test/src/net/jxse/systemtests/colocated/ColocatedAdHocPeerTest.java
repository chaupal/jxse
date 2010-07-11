package net.jxse.systemtests.colocated;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import net.jxta.util.ServerPipeAcceptListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ColocatedAdHocPeerTest {

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
    
    @Test(timeout=120000)
    public void testColocatedPeerBidiPipeComms() throws Exception {
        final CountDownLatch pipeEstablished = new CountDownLatch(1);
        final CountDownLatch aliceRequestReceived = new CountDownLatch(1);
        final CountDownLatch bobResponseReceived = new CountDownLatch(1);
        
        final AtomicReference<JxtaBiDiPipe> aliceAcceptedPipe = new AtomicReference<JxtaBiDiPipe>();
        
        ServerPipeAcceptListener listener = new ServerPipeAcceptListener() {
            public void pipeAccepted(JxtaBiDiPipe pipe) {
                aliceAcceptedPipe.set(pipe);
                pipeEstablished.countDown();
            }
            
            public void serverPipeClosed() {}
        };
        
        JxtaServerPipe aliceServerPipe = SystemTestUtils.createServerPipe(aliceManager, listener);
        
        
        PipeMsgListener aliceListener = new PipeMsgListener() {
            public void pipeMsgEvent(PipeMsgEvent event) {
                aliceRequestReceived.countDown();
            }
        };
        
        PipeMsgListener bobListener = new PipeMsgListener() {
            public void pipeMsgEvent(PipeMsgEvent event) {
                bobResponseReceived.countDown();
            }
        };
        
        JxtaBiDiPipe bobPipe = new JxtaBiDiPipe(bobManager.getNetPeerGroup(), aliceServerPipe.getPipeAdv(), 5000, bobListener);
        
        assertTrue(pipeEstablished.await(5, TimeUnit.SECONDS));
        aliceAcceptedPipe.get().setMessageListener(aliceListener);
        
        bobPipe.sendMessage(SystemTestUtils.createMessage("hello alice"));
        assertTrue(aliceRequestReceived.await(5, TimeUnit.SECONDS));
        
        aliceAcceptedPipe.get().sendMessage(SystemTestUtils.createMessage("hello bob"));
        assertTrue(bobResponseReceived.await(5, TimeUnit.SECONDS));
    }
}
