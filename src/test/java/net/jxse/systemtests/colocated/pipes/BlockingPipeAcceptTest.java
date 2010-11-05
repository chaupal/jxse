package net.jxse.systemtests.colocated.pipes;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.jxse.systemtests.colocated.SystemTestUtils;
import net.jxse.systemtests.colocated.configs.PeerConfigurator;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkManager;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BlockingPipeAcceptTest {

    @Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();

    private NetworkManager aliceManager;
    private NetworkManager bobManager;
    private NetworkManager relayManager;

    @Before
    public void initPeers() throws Exception {
    	relayManager = PeerConfigurator.createTcpRdvRelayPeer("relay", 50000, tempStorage);
        aliceManager = PeerConfigurator.createTcpClientPeer("alice", relayManager, tempStorage);
        bobManager = PeerConfigurator.createTcpClientPeer("bob", relayManager, tempStorage);

        relayManager.startNetwork();
        aliceManager.startNetwork();
        bobManager.startNetwork();
        
        // XXX: frustratingly, this test does not pass reliably unless time is given to 
        // the peers to connect to the Rdv/Relay peer and settle down. Otherwise, the 
        // pipe accept request is never received by alice unless bob explicitly retries.
        Thread.sleep(5000);
    }

    @After
    public void killPeers() throws Exception {
        aliceManager.stopNetwork();
        bobManager.stopNetwork();
        relayManager.stopNetwork();
    }

    @Test(timeout=60000)
    public void testBlockingAccept() throws Exception {
    	
    	// these latches are used to control the concurrency that is necessary to test the blocking
    	// API, and waiting for asynchronous delivery of messages.
    	final CountDownLatch connectionAcceptLatch = new CountDownLatch(1);
    	final CountDownLatch aliceReceivedRequest = new CountDownLatch(1);
    	final CountDownLatch bobReceivedResponse = new CountDownLatch(1);
    	AtomicReference<JxtaBiDiPipe> acceptedPipe = new AtomicReference<JxtaBiDiPipe>(null);
    	
    	// create the server pipe, and have alice wait for an incoming connection (in another thread)
    	JxtaServerPipe aliceServerPipe = SystemTestUtils.createServerPipe(aliceManager);
    	aliceServerPipe.setPipeTimeout(0);
    	aliceWait(aliceServerPipe, connectionAcceptLatch, acceptedPipe);
    	
    	// have bob attempt to connect to alice's server pipe
    	JxtaBiDiPipe bobClientPipe = new JxtaBiDiPipe(bobManager.getNetPeerGroup(), aliceServerPipe.getPipeAdv(), 2000, new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				bobReceivedResponse.countDown();
			}
		});
    	
    	assertTrue("Timeout while waiting for pipe accept", connectionAcceptLatch.await(15, TimeUnit.SECONDS));
    	
    	// configure alice's end of the newly created BidiPipe
    	final JxtaBiDiPipe alicePipeEnd = acceptedPipe.get();
    	alicePipeEnd.setMessageListener(new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				aliceReceivedRequest.countDown();
			}
		});
    	
    	// send the messages in each direction to test the pipe is functioning
    	bobClientPipe.sendMessage(SystemTestUtils.createMessage("hello alice"));
    	assertTrue("Timeout while waiting for bob -> alice message", aliceReceivedRequest.await(15, TimeUnit.SECONDS));
    	
    	alicePipeEnd.sendMessage(SystemTestUtils.createMessage("hello bob"));
    	assertTrue("Timeout while waiting for alice -> bob message", bobReceivedResponse.await(15, TimeUnit.SECONDS));
    }

	private void aliceWait(final JxtaServerPipe serverPipe, final CountDownLatch connectionAcceptLatch, final AtomicReference<JxtaBiDiPipe> accepted) {
		new Thread(new Runnable() {

			public void run() {
				try {
					accepted.set(serverPipe.accept());
					connectionAcceptLatch.countDown();
				} catch (IOException e) {
					System.err.println("Failed to accept");
					e.printStackTrace();
				}
			}
		}).start();
	}
}
