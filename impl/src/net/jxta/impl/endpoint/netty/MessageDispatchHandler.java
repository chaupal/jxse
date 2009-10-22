package net.jxta.impl.endpoint.netty;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.logging.Logging;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
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
@ChannelPipelineCoverage("one")
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
        if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, "Unhandled exception in netty channel pipeline - closing connection", e.getCause());
        }
        
        if(listener != null) {
            listener.connectionDied();
        }
        
        Channels.close(ctx, ctx.getChannel().getCloseFuture());
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
