package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.impl.endpoint.AsynchronousMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.QueuedMessage;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * Netty channel based messenger implementation. Unfortunately, this extends BlockingMessenger despite
 * the elegant non-blocking, event driven architecture of Netty underneath. This was primarily done
 * for haste rather than for any overriding architectural reason, though there are some areas of the
 * endpoint service code that expect blocking messengers explicitly.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class AsynchronousNettyMessenger extends AsynchronousMessenger implements MessageArrivalListener {

    private static final Logger LOG = Logger.getLogger(NettyMessenger.class.getName());
    private static final int QUEUE_SIZE = Integer.getInteger("net.jxta.impl.endpoint.async.queuesize", 100);

    private Channel channel;
    private EndpointAddress logicalDestinationAddr;
    private EndpointService endpointService;
    private PeerID localPeerId;

    private EndpointAddress localAddress;

    public AsynchronousNettyMessenger(Channel channel, PeerGroupID homeGroupID, PeerID localPeerID, EndpointAddress localAddress, EndpointAddress logicalDestinationAddress, EndpointService endpointService) {
        super(homeGroupID, localAddress, QUEUE_SIZE);
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
    protected void requestClose() {
        LOG.log(Level.FINE, "Closing netty channel for messenger to {0}", logicalDestinationAddr);
        if(channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() || !channel.isOpen();
    }

    @Override
    protected boolean sendMessageImpl(final QueuedMessage message) {
        if (isClosed()) {
            IOException cause = new IOException("Messenger was closed, it cannot be used to send messages.");
            message.getWriteListener().writeFailure(cause);
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, cause.getMessage(), cause);
            }
            return false;
        }

        if(channel.isWritable()) {
            writeMessage(message);
            return true;
        } else {
            return false;
        }
    }

    private void writeMessage(final QueuedMessage message) {
        ChannelFuture future = channel.write(message.getMessage());
        message.getWriteListener().writeSubmitted();
        final ChannelFutureListener channelFutureListener = new ChannelFutureListener()
        {
            public void operationComplete(ChannelFuture future) throws Exception
            {
                if (!future.isSuccess())
                {
                    message.getWriteListener().writeFailure(future.getCause());
                }
                else
                {
                    message.getWriteListener().writeSuccess();
                }
            }
        };
        future.addListener(channelFutureListener);
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

        ExecutorService executorService = endpointService.getGroup().getTaskManager().getExecutorService();
        executorService.execute(new Runnable() {
            public void run() {
                endpointService.processIncomingMessage(msg, srcAddr, dstAddr);
            }
        });
    }

    public void connectionDied() {
        LOG.log(Level.INFO, "Underlying channel for messenger to {0} has died", logicalDestinationAddr);
        connectionFailed();
    }

    public final void connectionDisposed() {
        connectionCloseComplete();
    }

    public void channelSaturated(boolean saturated) {
        if(saturated == false) {
            pullMessages();
        }
    }

    @Override
    protected EndpointAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public EndpointAddress getLogicalDestinationAddress() {
        return logicalDestinationAddr;
    }

    private boolean isLoopback(EndpointAddress srcAddr) {
        if (localAddress.equals(srcAddr)) {
            if (Logging.SHOW_DEBUG && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Loopback message detected");
            }
            return true;
        }
        return false;
    }

    private EndpointAddress extractEndpointAddress(Message msg, String elementNamespace, String elementName, String addrType) {
        MessageElement element = msg.getMessageElement(elementNamespace, elementName);
        if(element == null) {
            if (Logging.SHOW_DEBUG && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Message with no " + addrType + " address detected: " + msg);
            }
            return null;
        } else {
            msg.removeMessageElement(element);
            return new EndpointAddress(element.toString());
        }

    }
}
