package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
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
	
    private final NettyChannelRegistry registry;
    private final ReentrantLock listenerLock;
    // guarded by listenerLock
    private volatile MessageArrivalListener listener;
    // guarded by listenerLock
    private final Queue<Runnable> events;

    public MessageDispatchHandler(NettyChannelRegistry registry) {
        this.registry = registry;
        listenerLock = new ReentrantLock();
        events = new LinkedList<Runnable>();
    }
    
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	dispatchImportantListenerEvent(new Runnable() {
			public void run() {
				listener.connectionDied();
			}
    	});
    }
    
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final MessageArrivalListener l = listener;
        if (l != null)
        {
            l.connectionDisposed();
        }
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if(e.getMessage() instanceof WelcomeMessage) {
            WelcomeMessage message = (WelcomeMessage) e.getMessage();
            EndpointAddress logicalDestinationAddr = new EndpointAddress("jxta", message.getPeerID().getUniqueValue().toString(), null, null);
            registry.newConnection(ctx.getChannel(), message.getDestinationAddress(), logicalDestinationAddr);
            return;
        }
        
        final Message message = (Message) e.getMessage();
        
        dispatchImportantListenerEvent(new Runnable() {
        	public void run() {
        		listener.messageArrived(message);
        	}
        });
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
        
		dispatchImportantListenerEvent(new Runnable() {
            public void run() {
                listener.connectionDied();
            }
        });
        
        if (ctx.getChannel().isOpen()) {
            Channels.close(ctx, ctx.getChannel().getCloseFuture());
        }
    }
    
    @Override
    public void channelInterestChanged(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
    	dispatchListenerEvent(new Runnable() {
    		public void run() {
    			listener.channelSaturated(ctx.getChannel().getInterestOps() == Channel.OP_READ_WRITE);
    		}
    	});
    }

    public synchronized void setMessageArrivalListener(MessageArrivalListener listener) {
    	listenerLock.lock();
    	try {
	        this.listener = listener;
	        
	        Runnable eventDispatcher;
	        while((eventDispatcher = events.poll()) != null) {
	        	eventDispatcher.run();
	        }
    	} finally {
    		listenerLock.unlock();
    	}
    }
    
    /**
     * Runs the provided runnable if the listener has been set, otherwise
     * queues the runnable until the listener has been set. Use this for
     * critical events that the listener must know about.
     */
    private void dispatchImportantListenerEvent(Runnable r) {
    	listenerLock.lock();
    	try {
    		if(listener == null) {
    			events.add(r);
    		} else {
    			r.run();
    		}
    	} finally {
    		listenerLock.unlock();
    	}
    }
    
    /**
     * Runs the provided runnable if the listener has been set. Use this
     * instead of {@link #dispatchImportantListenerEvent(Runnable)} if the
     * event is a transient hint rather than critical information.
     */
    private void dispatchListenerEvent(Runnable r) {
    	listenerLock.lock();
    	try {
    		if(listener != null) {
    			r.run();
    		}
    	} finally {
    		listenerLock.unlock();
    	}
    }
}
