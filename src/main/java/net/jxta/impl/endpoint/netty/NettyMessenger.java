package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * Netty channel based messenger implementation. Unfortunately, this extends BlockingMessenger despite
 * the elegant non-blocking, event driven architecture of Netty underneath. This was primarily done
 * for haste rather than for any overriding architectural reason, though there are some areas of the
 * endpoint service code that expect blocking messengers explicitly.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class NettyMessenger extends BlockingMessenger implements MessageArrivalListener {

    private static final Logger LOG = Logger.getLogger(NettyMessenger.class.getName());

    private Channel channel;
    private EndpointAddress logicalDestinationAddr;
    private EndpointService endpointService;
	private PeerID localPeerId;

	private EndpointAddress localAddress;
    
    public NettyMessenger(Channel channel, PeerGroupID homeGroupID, PeerID localPeerID, EndpointAddress localAddress, EndpointAddress logicalDestinationAddress, EndpointService endpointService) {
        super(homeGroupID, localAddress, endpointService.getGroup().getTaskManager(), true);
        this.channel = channel;
        this.localPeerId = localPeerID;
        this.localAddress = new EndpointAddress(localPeerId, null, null);
        this.endpointService = endpointService;
        this.logicalDestinationAddr = logicalDestinationAddress;
        
        attachMessageListener();
    }

    private void attachMessageListener() {
        MessageDispatchHandler handler = (MessageDispatchHandler)channel.getPipeline().get(MessageDispatchHandler.NAME);
        handler.setMessageArrivalListener(this);
    }

    @Override
    protected void closeImpl() {
        // TODO: do we need to wait for this?
    	LOG.log(Level.FINE, "Closing netty channel for messenger to {0}", logicalDestinationAddr);
    	if(channel.isOpen()) {
    		channel.close();
    	}
    }
    
    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    @Override
    protected EndpointAddress getLogicalDestinationImpl() {
        return logicalDestinationAddr;
    }

    @Override
    protected boolean isIdleImpl() {
        // netty connections are low overhead, so idle isn't a bad thing
        return false;
    }

    @Override
    protected void sendMessageBImpl(Message message, String service, String param) throws IOException {
        
        if (isClosed()) {

            IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
            Logging.logCheckedWarning(LOG, failure);
            throw failure;

        }
        
        ChannelFuture future = channel.write(retargetMessage(message, service, param));
        future.awaitUninterruptibly();
        if(!future.isSuccess()) {
            IOException failure;
            if(future.isCancelled()) {
                failure = new IOException("Message send failed for " + message + ": send was cancelled");
            } else {
            	failure = new IOException("Message send failed for " + message);
            	failure.initCause(future.getCause());
            }
            
            Logging.logCheckedWarning(LOG, "Failed to send message to ", logicalDestinationAddr,
                    "\n", failure);
            
            closeImpl();
            
            throw failure;

        }
    }

    private Message retargetMessage(Message message, String service, String param) {
        MessageElement srcAddrElem = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, localAddress.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddrElem);
        EndpointAddress destAddressToUse;
        destAddressToUse = getDestAddressToUse(service, param);

        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        
        return message;
    }

	public void messageArrived(final Message msg) {
		// Extract the source and destination
		final EndpointAddress srcAddr 
			= extractEndpointAddress(msg, 
									 EndpointServiceImpl.MESSAGE_SOURCE_NS, 
									 EndpointServiceImpl.MESSAGE_SOURCE_NAME,
									 "source");
        
		final EndpointAddress dstAddr 
			= extractEndpointAddress(msg,
									 EndpointServiceImpl.MESSAGE_DESTINATION_NS, 
									 EndpointServiceImpl.MESSAGE_DESTINATION_NAME,
									 "destination");
		
		if(srcAddr == null || isLoopback(srcAddr) || dstAddr == null) {
			return;
		}
		
		ExecutorService executorService = taskManager.getExecutorService();
		executorService.execute(new Runnable() {
		    public void run() {
		        endpointService.processIncomingMessage(msg, srcAddr, dstAddr);
		    }
		});
	}
	
	public void connectionDied() {
		LOG.log(Level.INFO, "Underlying channel for messenger to {0} has died - closing messenger", logicalDestinationAddr);
	    close();
	}
	
	public void connectionDisposed() {
	    // do nothing - this is only needed if we are
	    // responding to close asynchronously, which we are not
	    // in this case
	}
	
	public void channelSaturated(boolean saturated) {
	    // we do not do anything with channel saturation info in the blocking form
	    // of netty messenger
	}

	private boolean isLoopback(EndpointAddress srcAddr) {
            if (localAddress.equals(srcAddr)) {


                return true;
            }

            return false;

	}

	private EndpointAddress extractEndpointAddress(Message msg, String elementNamespace, String elementName, String addrType) {

            MessageElement element = msg.getMessageElement(elementNamespace, elementName);
	
            if(element == null) {


            } else {
	        msg.removeMessageElement(element);
            }
		
            return new EndpointAddress(element.toString());

	}
}
