package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class NettyTransportServerTest {

    protected static final PeerGroupID PEER_GROUP_ID = PeerGroupID.defaultNetPeerGroupID;
	Mockery mockContext = new JUnit4Mockery();
    private NettyTransportServer server;
    private FakeEndpointService endpointService;
    private ServerChannelFactory factory;
    private FakePeerGroup group;
    private List<InetSocketAddress> bindpoints;
    
    @Before
    public void setUp() throws UnknownHostException {
        factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        
        group = new FakePeerGroup();
        endpointService = group.endpointService;
        
        bindpoints = new LinkedList<InetSocketAddress>();
        bindpoints.add(new InetSocketAddress(InetAddress.getLocalHost(), 12345));
        
        List<EndpointAddress> publicAddresses = new ArrayList<EndpointAddress>();
        publicAddresses.add(new EndpointAddress("test", "[::1]:12345", null, null));
        server = new NettyTransportServer(factory, new InetSocketAddressTranslator("tcp2"), group);
    }
    
    @After
    public void tearDown() {
        if(server.isStarted()) {
            server.beginStop();
            server.stop();
        }
    }
    
    @Test
    public void testCannotStartTwice() throws UnknownHostException, PeerGroupException {
        
        server.init(bindpoints, null, false);
        server.start(group.getEndpointService());
        
        try {
            server.start(group.getEndpointService());
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            assertEquals("already started", e.getMessage());
        }
    }

    @Test
    public void testStart_returnsFalseIfEndpointServiceRefusesRegistration() throws PeerGroupException {
        server.init(bindpoints, null, false);
        endpointService.refuseRegistration();
        assertFalse(server.start(endpointService));
    }
}
