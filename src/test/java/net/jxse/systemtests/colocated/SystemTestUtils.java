package net.jxse.systemtests.colocated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.*;
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

    public static void testPeerPropagatedCommunication(NetworkManager aliceManager, NetworkManager bobManager) throws IOException, InterruptedException
    {
        final CountDownLatch bobReceived = new CountDownLatch(1);
        final String name = "testPropagate";
        final InputPipe bobInput = createPropagatedInputPipe(bobManager, name, new PipeMsgListener()
        {
            public void pipeMsgEvent(PipeMsgEvent event)
            {
                bobReceived.countDown();
            }
        });
        final OutputPipe aliceOut = createPropagatedOutputPipe(aliceManager, name);
        assertEquals(aliceOut.getPipeID(), bobInput.getPipeID());
        Thread.sleep(5000); //Pipe would not have been set up quickly
        final Message testPayload = createMessage("TestPayload");
        aliceOut.send(testPayload);
        assertTrue(bobReceived.await(5, TimeUnit.SECONDS));
        bobInput.close();
        aliceOut.close();
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

    private static PipeID getPipeID(PeerGroup pg, String name)
    {
        return IDFactory.newPipeID(pg.getPeerGroupID(), hash("collocatedTest"));
    }

    private static PipeAdvertisement createMulticastSocketAdvertisement(NetworkManager manager, String name)
    {
        PipeID socketID = getPipeID(manager.getNetPeerGroup(), name);

        PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement
                .getAdvertisementType());
        advertisement.setPipeID(socketID);
        // set to type to propagate
        advertisement.setType(PipeService.PropagateType);
        advertisement.setName(name);
        return advertisement;
    }

    public static InputPipe createPropagatedInputPipe(NetworkManager manager, String name, PipeMsgListener listener) throws IOException
    {
        PipeAdvertisement adv = createMulticastSocketAdvertisement(manager, name);
        return manager.getNetPeerGroup().getPipeService().createInputPipe(adv, listener);
    }

    public static OutputPipe createPropagatedOutputPipe(NetworkManager manager, String name) throws IOException
    {
        PipeAdvertisement adv = createMulticastSocketAdvertisement(manager, name);
        return manager.getNetPeerGroup().getPipeService().createOutputPipe(adv, 50000);
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
    private static byte[] hash(final String expression)
	{
		byte[] result;
		MessageDigest digest;

		if (expression == null)
		{
			throw new IllegalArgumentException("Invalid null expression");
		}

		try
		{
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException failed)
		{
			failed.printStackTrace(System.err);
			RuntimeException failure = new IllegalStateException("Could not get SHA-1 Message");
			failure.initCause(failed);
			throw failure;
		}

		try
		{
			byte[] expressionBytes = expression.getBytes("UTF-8");
			result = digest.digest(expressionBytes);
		} catch (UnsupportedEncodingException impossible)
		{
			RuntimeException failure = new IllegalStateException("Could not encode expression as UTF8");

			failure.initCause(impossible);
			throw failure;
		}
		return result;
	}
}
