package net.jxse.systemtests.colocated.pipes;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import net.jxse.systemtests.colocated.SystemTestUtils;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;
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
    
    @Test
    public void testBlockingAccept() throws Exception {
    	JxtaServerPipe aliceServerPipe = SystemTestUtils.createServerPipe(aliceManager);
    	aliceServerPipe.setPipeTimeout(0);
    	CountDownLatch connectionAcceptLatch = new CountDownLatch(1);
    	AtomicReference<JxtaBiDiPipe> acceptedPipe = new AtomicReference<JxtaBiDiPipe>(null);
    	aliceWait(aliceServerPipe, connectionAcceptLatch, acceptedPipe);
    	
    	final CountDownLatch aliceReceivedRequest = new CountDownLatch(1);
    	final CountDownLatch bobReceivedResponse = new CountDownLatch(1);
    	
    	JxtaBiDiPipe bobClientPipe = new JxtaBiDiPipe(bobManager.getNetPeerGroup(), aliceServerPipe.getPipeAdv(), 2000, new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				bobReceivedResponse.countDown();
			}
		});
    	
    	connectionAcceptLatch.await();
    	final JxtaBiDiPipe alicePipeEnd = acceptedPipe.get();
    	alicePipeEnd.setMessageListener(new PipeMsgListener() {
			public void pipeMsgEvent(PipeMsgEvent event) {
				aliceReceivedRequest.countDown();
			}
		});
    	
    	bobClientPipe.sendMessage(SystemTestUtils.createMessage("hello alice"));
    	aliceReceivedRequest.await();
    	
    	
    	alicePipeEnd.sendMessage(SystemTestUtils.createMessage("hello bob"));
    	bobReceivedResponse.await();
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
