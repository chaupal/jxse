package net.jxta.impl.endpoint.netty;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.logging.Logging;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Simple channel handler which will immediately close all accepted connections until an atomic
 * boolean is flipped from false to true. Useful to ensure that connections are not accepted
 * before the environment is fully initialised.
 * 
 * @author iain.mcginniss@onedrum.com
 */
@ChannelPipelineCoverage("all")
public class ConnectionRejector extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = Logger.getLogger(ConnectionRejector.class.getName());
    
	public static final String NAME = "rejector";
    private AtomicBoolean acceptConnections;
	
	public ConnectionRejector(AtomicBoolean acceptConnections) {
		this.acceptConnections = acceptConnections;
	}
	
	public void setAcceptConnections(boolean acceptConnections) {
		this.acceptConnections.set(acceptConnections);
	}
	
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
	    
            if(!acceptConnections.get()) {

	        if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
	            SocketAddress localAddr = ctx.getChannel().getParent().getLocalAddress();
	            SocketAddress remoteAddr = ctx.getChannel().getRemoteAddress();
	            LOG.log(Level.WARNING, "Transport is not ready yet - rejecting connection from " + remoteAddr + " to local bound address " + localAddr);
	        }
	        ctx.getChannel().close();
	        return;

	    }
	    
	    super.channelConnected(ctx, e);
     }
	
}
