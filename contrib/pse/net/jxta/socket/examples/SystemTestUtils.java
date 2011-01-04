package net.jxta.socket.examples;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import net.jxta.util.ServerPipeAcceptListener;
import net.jxta.impl.endpoint.EndpointServiceImpl;

public class SystemTestUtils {

    private static final String TEST_NAMESPACE = "SystemTest";
    private static final String STRING_PAYLOAD_ELEMENT = "strPayload";

    public static JxtaServerPipe createServerPipe(NetworkManager manager, ServerPipeAcceptListener listener, boolean secure) throws IOException {
        PipeID pipeId = IDFactory.newPipeID(manager.getNetPeerGroup().getPeerGroupID());
        PipeAdvertisement pipeAd = createUnicastPipeAd(pipeId, secure);
        if(listener == null) {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd);
        } else {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd, listener);
        }
    }

    public static JxtaServerSocket createServerSocket(NetworkManager manager, boolean secure) throws IOException {
        PipeID pipeId = IDFactory.newPipeID(manager.getNetPeerGroup().getPeerGroupID());
        PipeAdvertisement pipeAd = createUnicastPipeAd(pipeId, false);
//        return new JxtaServerSocket(manager.getNetPeerGroup(), pipeAd, secure);
        return new JxtaServerSocket(manager.getNetPeerGroup(), pipeAd, secure);
    }

    public static JxtaServerSocket createServerSocket(NetworkManager manager, boolean secure, net.jxta.pipe.PipeID pipeID) throws IOException {
        PipeAdvertisement pipeAd = createUnicastPipeAd(pipeID, false);
//        return new JxtaServerSocket(manager.getNetPeerGroup(), pipeAd, secure);
        return new JxtaServerSocket(manager.getNetPeerGroup(), pipeAd);
    }
    
    public static PipeAdvertisement createUnicastPipeAd(PipeID pipeID, boolean secure) {
        PipeAdvertisement advertisement = (PipeAdvertisement)
        AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        
        advertisement.setPipeID(pipeID);
        if (secure)
            advertisement.setType(PipeService.UnicastSecureType);
        else
            advertisement.setType(PipeService.UnicastType);
        
        return advertisement;
    }
    
    public static Message createMessage(String payload) {
        Message msg = new Message();
        msg.addMessageElement(TEST_NAMESPACE, new StringMessageElement(STRING_PAYLOAD_ELEMENT, payload, null));
        return msg;
    }
    
    public static void testPeerCommunication(NetworkManager aliceManager, NetworkManager bobManager, boolean secure) throws IOException, InterruptedException {
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
        
        JxtaServerPipe aliceServerPipe = createServerPipe(aliceManager, listener, secure);
        
        PipeMsgListener aliceListener = new PipeMsgListener() {
            public void pipeMsgEvent(PipeMsgEvent event) {
System.out.println("Alice received message: " + event.getMessage().getMessageElement(STRING_PAYLOAD_ELEMENT));
                Message bobsMessage = event.getMessage();
                Set<EndpointAddress> tempSetEA = (Set)bobsMessage.getMessageProperty(EndpointServiceImpl.VERIFIED_ADDRESS_SET);
                Iterator i = tempSetEA.iterator();
                while(i.hasNext())
System.out.println("    -- verified address: " + i.next());
                aliceRequestReceived.countDown();

            }
        };
        
        PipeMsgListener bobListener = new PipeMsgListener() {
            public void pipeMsgEvent(PipeMsgEvent event) {
System.out.println("Bob received message: " + event.getMessage().getMessageElement(STRING_PAYLOAD_ELEMENT));
                Message alicesMessage = event.getMessage();
                Set<EndpointAddress> tempSetEA = (Set)alicesMessage.getMessageProperty(EndpointServiceImpl.VERIFIED_ADDRESS_SET);
                Iterator i = tempSetEA.iterator();
                while(i.hasNext())
System.out.println("    -- verified address: " + i.next());
                bobResponseReceived.countDown();
            }
        };
        JxtaBiDiPipe bobPipe = connectNonBlocking(bobManager, aliceServerPipe.getPipeAdv(), bobListener);

        pipeEstablished.await(5, TimeUnit.SECONDS);
        aliceAcceptedPipe.get().setMessageListener(aliceListener);

System.out.println("Bob: sending message to alice - 'hello alice'");
        bobPipe.sendMessage(SystemTestUtils.createMessage("hello alice"));
        aliceRequestReceived.await(5, TimeUnit.SECONDS);

System.out.println("Alice: sending message to bob - 'hello bob'");
        aliceAcceptedPipe.get().sendMessage(SystemTestUtils.createMessage("hello bob"));
        bobResponseReceived.await(5, TimeUnit.SECONDS);
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

    public static void rmdir(File dir) throws IOException {
        if (null == dir)
            return;
        if (!dir.exists())
            return;
        File[] children = dir.listFiles();
        if (children!=null)
            for (int i=0; i<children.length; i++) {
                if(children[i].isFile())
                    children[i].delete();
                else if (children[i].isDirectory())
                    rmdir(children[i]);
            }
        dir.delete();
    }
}
