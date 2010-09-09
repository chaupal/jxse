package net.jxse.systemtests.colocated;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import net.jxta.util.ServerPipeAcceptListener;

public class SystemTestUtils {

    private static final String TEST_NAMESPACE = "SystemTest";
    private static final String STRING_PAYLOAD_ELEMENT = "strPayload";

    public static JxtaServerPipe createServerPipe(NetworkManager manager, ServerPipeAcceptListener listener) throws IOException {
        PipeID pipeId = IDFactory.newPipeID(manager.getNetPeerGroup().getPeerGroupID());
        PipeAdvertisement pipeAd = createUnicastPipeAd(pipeId);
        if(listener == null) {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd);
        } else {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd, listener);
        }
    }
    
    public static PipeAdvertisement createUnicastPipeAd(PipeID pipeID) {
        PipeAdvertisement advertisement = (PipeAdvertisement)
        AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        
        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.UnicastType);
        
        return advertisement;
    }
    
    public static Message createMessage(String payload) {
        Message msg = new Message();
        msg.addMessageElement(TEST_NAMESPACE, new StringMessageElement(STRING_PAYLOAD_ELEMENT, payload, null));
        return msg;
    }
    
    public static void testPeerCommunication(NetworkManager aliceManager, NetworkManager bobManager) throws IOException, InterruptedException {
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
        
        JxtaServerPipe aliceServerPipe = createServerPipe(aliceManager, listener);
        
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
        //TODO We have an issue on initialisation which means need to wait for services to be running before executing test. Only effects relay tests.
        Thread.sleep(5000);
        JxtaBiDiPipe bobPipe = connectNonBlocking(bobManager, aliceServerPipe.getPipeAdv(), bobListener);
        
        assertTrue(pipeEstablished.await(5, TimeUnit.SECONDS));
        aliceAcceptedPipe.get().setMessageListener(aliceListener);
        
        bobPipe.sendMessage(SystemTestUtils.createMessage("hello alice"));
        assertTrue(aliceRequestReceived.await(5, TimeUnit.SECONDS));
        
        aliceAcceptedPipe.get().sendMessage(SystemTestUtils.createMessage("hello bob"));
        assertTrue(bobResponseReceived.await(5, TimeUnit.SECONDS));
    }

    private static JxtaBiDiPipe connectNonBlocking(NetworkManager clientManager,
												 PipeAdvertisement pipeAdv,
												 PipeMsgListener clientListener) throws IOException
    {
        final JxtaBiDiPipe biDiPipe = new JxtaBiDiPipe();
        biDiPipe.setWindowSize(20);
        biDiPipe.connect(clientManager.getNetPeerGroup(),null, pipeAdv, 5000, clientListener, true);
        return biDiPipe;
    }

	private static JxtaBiDiPipe connectWithRetry(NetworkManager clientManager, 
												 PipeAdvertisement pipeAdv, 
												 PipeMsgListener clientListener) throws IOException {
		int tryCount = 0;
		while(true) {
			try {
				return new JxtaBiDiPipe(clientManager.getNetPeerGroup(), pipeAdv, 5000, clientListener);
			} catch (SocketTimeoutException e) {
				tryCount++;
				if(tryCount >= 3) {
					throw e;
				}
			}
		}
	}
}
