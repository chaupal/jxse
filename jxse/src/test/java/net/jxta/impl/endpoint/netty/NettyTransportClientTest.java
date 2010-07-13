package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.exception.PeerGroupException;

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.junit.Before;
import org.junit.Test;

public class NettyTransportClientTest {
    
    private NettyTransportClient client;
    private NioClientSocketChannelFactory factory;
    private FakePeerGroup group;
    private EndpointAddress returnAddress;
    
    @Before
    public void setUp() throws UnknownHostException {
        factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        
        group = new FakePeerGroup();
        returnAddress = new EndpointAddress("test", "10.1.1.1:12345", null, null);
        
        client = new NettyTransportClient(factory, new InetSocketAddressTranslator("test"), group, returnAddress);
    }
    
    @Test
    public void testAllowsRouting() throws PeerGroupException {
        assertTrue(client.allowsRouting());
    }
    
    @Test
    public void testIsConnectionOriented() throws PeerGroupException {
        assertTrue(client.isConnectionOriented());
    }
    
    @Test
    public void testGetProtocolName() throws PeerGroupException {
        assertEquals("test", client.getProtocolName());
    }
    
    @Test
    public void testGetEndpointService_returnsInstanceFromGroup() {
        client.start(group.endpointService);
        assertSame(group.endpointService, client.getEndpointService());
    }
    
    @Test
    public void testGetProtocolName_matchesValueFromAddressTranslator() {
        assertEquals("test", client.getProtocolName());
        
        NettyTransportClient secondClient = new NettyTransportClient(factory, new InetSocketAddressTranslator("test2"), group, returnAddress);
        assertEquals("test2", secondClient.getProtocolName());
    }
    
    @Test
    public void testStart_returnsFalseIfEndpointServiceRefusesRegistration() throws PeerGroupException {
        group.endpointService.refuseRegistration();
        assertFalse(client.start(group.endpointService));
    }
    
}
