package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.logging.Logging;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * The terminal upstream listener, which takes decoded messages (either WelcomeMessage or Message)
 * and passes this information up to the MessageArrivalListener - typically, the NettyMessenger
 * for this connection.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class MessageDispatchHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = Logger.getLogger(MessageDispatchHandler.class.getName());
    
	public static final String NAME = "messageDispatch";
	
    private NettyChannelRegistry registry;
    private MessageArrivalListener listener;
    private Queue<Message> messages;

    public MessageDispatchHandler(NettyChannelRegistry registry) {
        this.registry = registry;
        messages = new LinkedList<Message>();
    }
    
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        listener.connectionDied();
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if(e.getMessage() instanceof WelcomeMessage) {
            WelcomeMessage message = (WelcomeMessage) e.getMessage();
            EndpointAddress logicalDestinationAddr = new EndpointAddress("jxta", message.getPeerID().getUniqueValue().toString(), null, null);
            registry.newConnection(ctx.getChannel(), message.getDestinationAddress(), logicalDestinationAddr);
            return;
        }
        
    	synchronized(messages) {
		    if(listener == null) {
		        messages.offer((Message)e.getMessage());
		        return;
		    }
    	}
        
        sendToListener((Message)e.getMessage());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable cause = e.getCause();
		if(cause instanceof ConnectException) {
			LOG.log(Level.FINE, "Unable to connect to remote host");
        } else if(cause instanceof ClosedChannelException) {
    		LOG.log(Level.FINE, "Channel to {0} has been closed", new Object[] { ctx.getChannel().getRemoteAddress() });
        } else if(cause instanceof IOException) {
        	LOG.log(Level.FINE, "Channel to {0} has failed - {1}", new Object[] { ctx.getChannel().getRemoteAddress(), cause.getMessage() });
        } else if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, "Unhandled exception in netty channel pipeline - closing connection", cause);
        }
        
        if(listener != null) {
            listener.connectionDied();
        }
        
        Channels.close(ctx, ctx.getChannel().getCloseFuture());
    }
    
    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        listener.channelSaturated(ctx.getChannel().getInterestOps() == Channel.OP_READ_WRITE);
    }

    private void sendToListener(Message message) {
        if(message instanceof Message) {
            listener.messageArrived(message);
        }
    }

    public synchronized void setMessageArrivalListener(MessageArrivalListener listener) {
    	synchronized(messages) {
	        this.listener = listener;
	        
	        Object message;
	        while((message = messages.poll()) != null) {
	            sendToListener((Message)message);
	        }
    	}
    }
}
