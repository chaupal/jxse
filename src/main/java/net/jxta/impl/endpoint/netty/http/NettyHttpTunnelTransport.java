package net.jxta.impl.endpoint.netty.http;

import java.util.concurrent.Executors;

import net.jxta.impl.endpoint.netty.NettyTransport;

import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.httptunnel.HttpTunnelClientChannelFactory;
import org.jboss.netty.channel.socket.httptunnel.HttpTunnelServerChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Netty based transport which uses a full duplex HTTP tunnel rather than a raw TCP
 * connection to send messages between client and server. This is intended to allow
 * negotiation of restrictive transparent firewalls and proxies, typically in corporate
 * environments.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class NettyHttpTunnelTransport extends NettyTransport {

    @Override
    protected ClientSocketChannelFactory createClientSocketChannelFactory() {
        NioClientSocketChannelFactory nioFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        return new HttpTunnelClientChannelFactory(nioFactory);
    }
    
    @Override
    protected ServerSocketChannelFactory createServerSocketChannelFactory() {
        NioServerSocketChannelFactory nioFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        return new HttpTunnelServerChannelFactory(nioFactory);
    }
    
    @Override
    protected String getDefaultProtocolName() {
        return "http2";
    }
    
    @Override
    protected int getDefaultPort() {
        return 8080;
    }
    
    @Override
    protected int getDefaultPortRangeLowerBound() {
    	return 8081;
    }
    
    @Override
    protected int getDefaultPortRangeUpperBound() {
    	return 8099;
    }
    
    @Override
    protected String getTransportDescriptiveName() {
        return "HTTP Tunnel";
    }
    
}
