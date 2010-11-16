package net.jxta.impl.endpoint.netty;

import java.net.SocketAddress;

import org.jboss.netty.channel.AbstractChannel;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.DefaultChannelConfig;

public class FakeChannel extends AbstractChannel {

    public SocketAddress localAddress;
    public SocketAddress remoteAddress;
    public boolean bound;
    public boolean connected;
    
    public FakeChannel(Channel parent, ChannelFactory factory, ChannelPipeline pipeline, ChannelSink sink) {
        super(parent, factory, pipeline, sink);
    }
    
    public ChannelConfig getConfig() {
        return new DefaultChannelConfig();
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public boolean isBound() {
        return bound;
    }

    public boolean isConnected() {
        return connected;
    }
    
}