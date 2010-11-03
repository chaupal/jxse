package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.jxta.endpoint.router.EndpointRoutingTransport;
import net.jxta.peer.PeerID;
import net.jxta.document.Advertisement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageFilterListener;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.service.Service;

public class FakeEndpointService implements EndpointService {

    public Queue<ReceivedMessage> received;
    public PeerGroup group;
    private List<MessageTransport> transports;
    public List<Messenger> messengers;
    private boolean refuseRegistration = false;
    
    public FakeEndpointService(PeerGroup group) {
        this.group = group;
        received = new ConcurrentLinkedQueue<ReceivedMessage>();
        transports = new LinkedList<MessageTransport>();
        messengers = Collections.synchronizedList(new LinkedList<Messenger>());
    }
    
    public PeerGroup getGroup() {
        return group;
    }
    
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        received.offer(new ReceivedMessage(message, srcAddr, dstAddr));
    }

    public EndpointRoutingTransport getEndpointRouter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public class ReceivedMessage {
        public Message msg;
        public EndpointAddress srcAddr;
        public EndpointAddress dstAddr;
        
        public ReceivedMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {
            this.msg = msg;
            this.srcAddr = srcAddr;
            this.dstAddr = dstAddr;
        }
    }
    
    /* UNIMPLEMENTED, IRRELEVANT METHODS BEYOND THIS POINT */
    
    public boolean isConnectedToRelayPeer() {
        throw new RuntimeException("not implemented");
    }

    public Collection<PeerID> getConnectedRelayPeers() {
        throw new RuntimeException("not implemented");
    }

    public void addIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        throw new RuntimeException("not implemented");
    }

    public boolean addIncomingMessageListener(EndpointListener listener, String serviceName, String serviceParam) {
        throw new RuntimeException("not implemented");
    }

    public MessengerEventListener addMessageTransport(MessageTransport transport) {
        if(refuseRegistration) {
            return null;
        }
        
        this.transports.add(transport);
        return new FakeMessengerEventListener(this);
    }

    public boolean addMessengerEventListener(MessengerEventListener listener, int priority) {
        throw new RuntimeException("not implemented");
    }

    public void addOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        throw new RuntimeException("not implemented");
    }

    public void demux(Message msg) {
        throw new RuntimeException("not implemented");
    }

    public Iterator<MessageTransport> getAllMessageTransports() {
        throw new RuntimeException("not implemented");
    }

    public Messenger getCanonicalMessenger(EndpointAddress addr, Object hint) {
        throw new RuntimeException("not implemented");
    }

    public Messenger getDirectMessenger(EndpointAddress addr, Object hint, boolean exclusive) {
        throw new RuntimeException("not implemented");
    }

    

    public EndpointListener getIncomingMessageListener(String serviceName, String serviceParam) {
        throw new RuntimeException("not implemented");
    }

    public MessageTransport getMessageTransport(String name) {
        throw new RuntimeException("not implemented");
    }

    public Messenger getMessenger(EndpointAddress addr, Object hint) {
        throw new RuntimeException("not implemented");
    }

    public Messenger getMessenger(EndpointAddress addr) {
        throw new RuntimeException("not implemented");
    }

    public boolean getMessenger(MessengerEventListener listener, EndpointAddress addr, Object hint) {
        throw new RuntimeException("not implemented");
    }

    public Messenger getMessengerImmediate(EndpointAddress addr, Object hint) {
        throw new RuntimeException("not implemented");
    }

    public boolean ping(EndpointAddress addr) {
        throw new RuntimeException("not implemented");
    }

    public void propagate(Message message, String serviceName, String serviceParam) throws IOException {
        throw new RuntimeException("not implemented");
    }

    public void propagate(Message message, String serviceName, String serviceParam, int initialTTL) {
        throw new RuntimeException("not implemented");
    }

    public MessageFilterListener removeIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        throw new RuntimeException("not implemented");
    }

    public EndpointListener removeIncomingMessageListener(String serviceName, String serviceParam) {
        throw new RuntimeException("not implemented");
    }

    public boolean removeMessageTransport(MessageTransport transpt) {
        throw new RuntimeException("not implemented");
    }

    public boolean removeMessengerEventListener(MessengerEventListener listener, int priority) {
        throw new RuntimeException("not implemented");
    }

    public MessageFilterListener removeOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        throw new RuntimeException("not implemented");
    }

    public Advertisement getImplAdvertisement() {
        throw new RuntimeException("not implemented");
    }

//    public Service getInterface() {
//        throw new RuntimeException("not implemented");
//    }

    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public int startApp(String[] args) {
        throw new RuntimeException("not implemented");
    }

    public void stopApp() {
        throw new RuntimeException("not implemented");
    }

    public void refuseRegistration() {
        refuseRegistration = true;
    }

	public void processIncomingMessage(Message msg) {
		// do nothing
	}

    public boolean isReachable(PeerID pid, boolean tryToConnect) {
        throw new RuntimeException("not implemented");
    }

}
