package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.netty.FakeEndpointService.ReceivedMessage;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class EndToEndTest {

    public static final String TEST_PROTO_NAME = "test";
    
    private static final PeerID LOCAL_PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503304F8E1DEBB4942C0BF16DD923DEC949803"));
    private static final PeerID REMOTE_PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E50472050325033E7E1335996F44E38BD66B16349BB1F1E03"));
    
    private Mockery mockContext = new JUnit4Mockery();
    private FakeEndpointService clientEndpoint;
    private FakeEndpointService serverEndpoint;
    
    private PeerGroup serverGroup;
    private PeerGroup clientGroup;
    
    @Before
    public void setUp() {
        
        serverGroup = mockContext.mock(PeerGroup.class, "serverGroup");
        clientGroup = mockContext.mock(PeerGroup.class, "clientGroup");
        
        clientEndpoint = new FakeEndpointService(clientGroup);
        serverEndpoint = new FakeEndpointService(serverGroup);
        
        
        mockContext.checking(new Expectations() {{
            ignoring(clientGroup).getEndpointService(); will(returnValue(clientEndpoint));
            ignoring(serverGroup).getEndpointService(); will(returnValue(serverEndpoint));
            
            ignoring(clientGroup).getPeerID(); will(returnValue(LOCAL_PEER_ID));
            ignoring(serverGroup).getPeerID(); will(returnValue(REMOTE_PEER_ID));
            
            ignoring(clientGroup).getPeerGroupID(); will(returnValue(PeerGroupID.defaultNetPeerGroupID));
            ignoring(serverGroup).getPeerGroupID(); will(returnValue(PeerGroupID.defaultNetPeerGroupID));
        }});
    }

    
    @Test
    @Ignore("Investigate - Priority, works on own, but not in all tests")
    public void testConnectClientAndSendMessages() throws Exception {
        ServerChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

        InetSocketAddressTranslator addrTranslator = new InetSocketAddressTranslator(TEST_PROTO_NAME);
        SocketAddress serverAddress = new InetSocketAddress(IPUtils.ANYADDRESS, 12344);
        
        List<SocketAddress> addresses = new LinkedList<SocketAddress>();
        addresses.add(serverAddress);
        NettyTransportServer server = new NettyTransportServer(factory, addrTranslator, serverGroup);
        server.init(addresses, null, false);
        server.start(serverEndpoint);
        
        ChannelFactory clientFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        EndpointAddress clientReturnAddress = addrTranslator.toEndpointAddress(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        NettyTransportClient client = new NettyTransportClient(clientFactory, addrTranslator, clientGroup, clientReturnAddress);
        client.start(clientEndpoint);
        
        Messenger messenger = client.getMessenger(server.getPublicAddresses().next());
//        Messenger messenger = client.getMessenger(server.getPublicAddresses().next(), null);
        
        messenger.sendMessage(createTestMessage("a", "b"));
        messenger.sendMessage(createTestMessage("c", "d"));
        messenger.sendMessage(createTestMessage("e", "f"));
        
        Thread.sleep(200L);
        
        assertEquals(3, serverEndpoint.received.size());
        checkReceivedMessage(serverEndpoint.received.poll(), createTestMessage("a", "b"));
        checkReceivedMessage(serverEndpoint.received.poll(), createTestMessage("c", "d"));
        checkReceivedMessage(serverEndpoint.received.poll(), createTestMessage("e", "f"));
        
        assertEquals(1, serverEndpoint.messengers.size());
        Messenger serverMessenger = serverEndpoint.messengers.get(0);
        serverMessenger.sendMessage(createTestMessage("x", "y"));
        serverMessenger.sendMessage(createTestMessage("m", "n"));
        
        Thread.sleep(200L);
        
        assertEquals(2, clientEndpoint.received.size());
        checkReceivedMessage(clientEndpoint.received.poll(), createTestMessage("x", "y"));
        checkReceivedMessage(clientEndpoint.received.poll(), createTestMessage("m", "n"));
    }

    private void checkReceivedMessage(ReceivedMessage received, Message expected) {
        assertEquals(expected, received.msg);
    }

    private Message createTestMessage(String key, String value) {
        Message msg = new Message();
        msg.addMessageElement(new StringMessageElement(key, value, null));
        
        return msg;
    }
}
