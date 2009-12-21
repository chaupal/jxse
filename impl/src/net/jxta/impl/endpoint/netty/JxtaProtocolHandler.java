package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.TimeoutException;
import org.jboss.netty.util.Timer;

/**
 * Implementation of the JXTA TCP/IP choreography protocol as specified in
 * {@link https://jxta-spec.dev.java.net/nonav/JXTAProtocols.html#trans-tcpipt Section 7.1} of the JXTA v2.0
 * Protocols Specification. This should be usable for any bidirectional, stream-based network transport.

 * @author iain.mcginniss@onedrum.com
 */
@ChannelPipelineCoverage("one")
public class JxtaProtocolHandler extends SimpleChannelHandler implements ChannelHandler, ChannelFutureListener {
    
    private static final Logger LOG = Logger.getLogger(JxtaProtocolHandler.class.getName());
	public static final String NAME = "jxtaProtocolHandler";
	private static final String WELCOME_TIMEOUT_HANDLER_NAME = "welcomeTimeoutHandler";
	
    /**
     * The maximum welcome message size is 4096 bytes, as stated in section 7.1.3 of the JXTA v2.0 protocols
     * specification.
     */
    public static final int MAX_WELCOME_MESSAGE_SIZE = 4096;
    
    private Timer timeoutTimer;
    private PeerID localPeerId;
    private JxtaProtocolState state;
    
    private ChannelBuffer receivedBytes;
    private MessagePackageHeader currentHeader;

    private AddressTranslator addrTranslator;
    private EndpointAddress connectToAddress;
    private EndpointAddress returnAddress;

    private ReentrantLock shutdownLock;
    private Set<ChannelFuture> pendingWrites;
    private boolean closing;
    
    public JxtaProtocolHandler(AddressTranslator addrTranslator, PeerID localPeerId, Timer timeoutTimer, EndpointAddress connectToAddress, EndpointAddress returnAddress) {
        this.addrTranslator = addrTranslator;
        this.localPeerId = localPeerId;
        this.state = JxtaProtocolState.AWAITING_WELCOME_MESSAGE;
        this.receivedBytes = ChannelBuffers.dynamicBuffer();
        this.timeoutTimer = timeoutTimer;
        this.connectToAddress = connectToAddress;
        this.returnAddress = returnAddress;
        this.pendingWrites = new HashSet<ChannelFuture>();
        this.closing = false;
        this.shutdownLock = new ReentrantLock();
    }
    
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        EndpointAddress dstAddr = getDestinationAddress(ctx);
        EndpointAddress srcAddr = getSourceAddress(ctx);
        WelcomeMessage welcome = new WelcomeMessage(dstAddr, srcAddr, localPeerId, false);
        ChannelBuffer welcomeBytes = ChannelBuffers.copiedBuffer(welcome.getByteBuffer());
        write(ctx, welcomeBytes, Channels.future(ctx.getChannel()));
        
        ctx.getPipeline().addBefore(NAME, WELCOME_TIMEOUT_HANDLER_NAME, new ReadTimeoutHandler(timeoutTimer, 5, TimeUnit.SECONDS));
        
        super.channelConnected(ctx, e);
    }

    private EndpointAddress getDestinationAddress(ChannelHandlerContext ctx) {
        if(connectToAddress != null) {
            return connectToAddress;
        }
        
        return addrTranslator.toEndpointAddress(ctx.getChannel().getRemoteAddress());
    }

    private EndpointAddress getSourceAddress(ChannelHandlerContext ctx) {
        EndpointAddress srcAddr;
        if(returnAddress != null) {
            srcAddr = returnAddress;
        } else if(ctx.getChannel().getParent() != null) {
            srcAddr = addrTranslator.toEndpointAddress(ctx.getChannel().getLocalAddress(), ctx.getChannel().getParent().getLocalAddress());
        } else {
            srcAddr = addrTranslator.toEndpointAddress(ctx.getChannel().getLocalAddress());
        }
        return srcAddr;
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        receivedBytes.readerIndex(0);
        receivedBytes.writeBytes((ChannelBuffer)e.getMessage());
        
        boolean makingProgress = true;
        while(makingProgress) {
            switch(state) {
            case AWAITING_WELCOME_MESSAGE:
                if(readWelcomeMessage(ctx)) {
                    state = JxtaProtocolState.READING_HEADER;
                    ctx.getPipeline().remove(WELCOME_TIMEOUT_HANDLER_NAME);
                } else {
                    makingProgress = false;
                }
                break;
            case READING_HEADER:
                if(readHeader(ctx)) {
                    state = JxtaProtocolState.READING_BODY;
                } else {
                    makingProgress = false;
                }
                break;
            case READING_BODY:
                if(readBody(ctx)) {
                    state = JxtaProtocolState.READING_HEADER; 
                    break;
                } else {
                    makingProgress = false;
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if(e.getCause() instanceof TimeoutException && state == JxtaProtocolState.AWAITING_WELCOME_MESSAGE) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Failed to receive welcome message from client " + ctx.getChannel().getRemoteAddress() + " in timely manner - disconnecting");
            }
            Channels.close(ctx, ctx.getChannel().getCloseFuture());
            return;
        }
        super.exceptionCaught(ctx, e);
    }
    
    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        SerializedMessage message = (SerializedMessage)e.getMessage();
        
        ChannelBuffer headerBuffer = ChannelBuffers.wrappedBuffer(message.getMessageHeader().getByteBuffer());
        ChannelBuffer fullFrame = ChannelBuffers.wrappedBuffer(headerBuffer, message.getMessageContents());
        
        write(ctx, fullFrame, e.getFuture());
    }
    
    @Override
    public void closeRequested(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        shutdownLock.lock();
        try {
            closing = true;
            
            if(pendingWrites.isEmpty()) {
                ctx.sendDownstream(e);
            }
        } finally {
            shutdownLock.unlock();
        }
    }
    
    public void operationComplete(ChannelFuture future) throws Exception {
        shutdownLock.lock();
        try {
            pendingWrites.remove(future);
            if(closing && pendingWrites.isEmpty()) {
                Channels.close(future.getChannel());
            }            
        } finally {
            shutdownLock.unlock();
        }
    }
    
    private void write(ChannelHandlerContext ctx, Object message, ChannelFuture future) {
        shutdownLock.lock();
        try {
            if(closing) {
                if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Attempt to write made after the channel shutdown process has started");
                }
                future.setFailure(new IllegalStateException("Attempt to write made after the channel shutdown process has started"));
                return;
            }
            pendingWrites.add(future);
            future.addListener(this);
            Channels.write(ctx, future, message);
        } finally {
            shutdownLock.unlock();
        }
    }

    private boolean readWelcomeMessage(ChannelHandlerContext ctx) {
        
        ByteBuffer buffer = createByteBuffer();
        
        try {
            WelcomeMessage receivedWelcomeMessage = new WelcomeMessage();
            if(receivedWelcomeMessage.read(buffer)) {
                Channels.fireMessageReceived(ctx, receivedWelcomeMessage);
                resetReadIndex(buffer);
                return true;
            } else {
                receivedBytes.readerIndex(0);
                if(receivedBytes.readableBytes() > MAX_WELCOME_MESSAGE_SIZE) {
                    // TODO: notify outside world?
                    if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Received a welcome message bigger than the maximum size (" + MAX_WELCOME_MESSAGE_SIZE + ") from client " + ctx.getChannel().getRemoteAddress() + "- disconnecting");
                    }
                    Channels.close(ctx, ctx.getChannel().getCloseFuture());
                }
                
                return false;
            }
        } catch(IOException ex) {
            // invalid / corrupt welcome message received, disconnect
            // TODO: flag this to controller
            Channels.close(ctx, ctx.getChannel().getCloseFuture());
            return false;
        }
    }

    private void resetReadIndex(ByteBuffer buffer) {
        receivedBytes.readerIndex(buffer.position());
        receivedBytes.discardReadBytes();
    }

    private ByteBuffer createByteBuffer() {
        ByteBuffer buffer = receivedBytes.toByteBuffer();
        return buffer;
    }
    
    private boolean readHeader(ChannelHandlerContext ctx) {
        ByteBuffer buffer = createByteBuffer();
        
        try {
            MessagePackageHeader header = new MessagePackageHeader();
            if(header.readHeader(buffer)) {
                currentHeader = header;
                resetReadIndex(buffer);
                return true;
            }
            
            return false;
        } catch(IOException e) {
            // invalid / corrupt welcome message received, disconnect
            // TODO: flag this to controller
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Corrupt / invalid message header received from client " + ctx.getChannel().getRemoteAddress() + " - disconnecting");
            }
            ctx.getChannel().close();
            return false;
        }
    }
    
    private boolean readBody(ChannelHandlerContext ctx) {
        if(receivedBytes.readableBytes() < currentHeader.getContentLengthHeader()) {
            return false;
        }
        
        int messageSize = (int)Math.min(Integer.MAX_VALUE, currentHeader.getContentLengthHeader());
        ChannelBuffer messageContents = ChannelBuffers.buffer(messageSize);
        receivedBytes.readBytes(messageContents, messageSize);
        
        SerializedMessage message = new SerializedMessage(currentHeader, messageContents);
        Channels.fireMessageReceived(ctx, message);
        receivedBytes.discardReadBytes();
        return true;
    }
    
    private enum JxtaProtocolState {

        AWAITING_WELCOME_MESSAGE,
        READING_HEADER,
        READING_BODY,
        CLOSED
        
    }
}
