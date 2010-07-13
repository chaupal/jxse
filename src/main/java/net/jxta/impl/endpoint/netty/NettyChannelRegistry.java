package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.EndpointAddress;

import org.jboss.netty.channel.Channel;

/**
 * Simple notification interface for when a new channel is created and fully established (i.e.
 * the JXTAHELLO handshake has completed). This interface is of particular importance to the server
 * side of a transport, as it will be used to signal new accepted connections.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public interface NettyChannelRegistry {

    public void newConnection(Channel channel, EndpointAddress directedAt, EndpointAddress logicalEndpointAddress);
    
}
