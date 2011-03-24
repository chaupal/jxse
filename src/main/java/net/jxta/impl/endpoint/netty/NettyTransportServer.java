package net.jxta.impl.endpoint.netty;

import java.net.BindException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.exception.PeerGroupException;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.HashedWheelTimer;

/**
 * The server side of a netty based transport. Responsible for accepting remote connections and making
 * the endpoint service aware of these asynchronously.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class NettyTransportServer implements NettyChannelRegistry, MessageReceiver, TransportServerComponent {

    private static final Logger LOG = Logger.getLogger(NettyTransportServer.class.getName());
    
    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;
    private EndpointService endpointService;
    
    private AtomicBoolean started = new AtomicBoolean(false);
    private PeerGroupID homeGroupID;
    private PeerID localPeerID;
    
    private MessengerEventListener listener;
    private List<EndpointAddress> publicAddresses;

    private AddressTranslator addrTranslator;
    
    private ChannelGroup channels;

    private HashedWheelTimer timeoutTimer;

    private ChannelGroupFuture closeChannelsFuture;

    private List<EndpointAddress> boundAddresses;
    
    public NettyTransportServer(ServerChannelFactory factory, AddressTranslator addrTranslator, final PeerGroup group) {
        this.channels = new DefaultChannelGroup();
        this.homeGroupID = group.getPeerGroupID();
        this.localPeerID = group.getPeerID();
        this.addrTranslator = addrTranslator;
        serverBootstrap = new ServerBootstrap(factory);
        serverBootstrap.setParentHandler(new ConnectionGroupAddHandler());
        timeoutTimer = new HashedWheelTimer();
    }
 
    public void init(List<? extends SocketAddress> potentialBindpoints, EndpointAddress publicAddress, boolean usePublicOnly) throws PeerGroupException {
        serverBootstrap.setPipelineFactory(new NettyTransportChannelPipelineFactory(localPeerID, timeoutTimer, this, addrTranslator, started, null, publicAddress));
        SocketAddress chosenAddress = bindServerChannel(potentialBindpoints);
        boundAddresses = Collections.unmodifiableList(addrTranslator.translateToExternalAddresses(chosenAddress));
        
        if(serverChannel == null) {

            Logging.logCheckedWarning(LOG, "Failed to bind to any of the addresses in the configured range");
            throw new PeerGroupException("Failed to bind to any address in the configured range");

        }
        
        if(usePublicOnly) {

            if(publicAddress == null) {

                Logging.logCheckedWarning(LOG, "Instructed to use public address only, but no public address specified! Using all bound addresses instead");
                publicAddresses = new ArrayList<EndpointAddress>(boundAddresses);

            } else {

                publicAddresses = new ArrayList<EndpointAddress>(1);
                publicAddresses.add(publicAddress);

            }

        } else {
            int size = boundAddresses.size() + ((publicAddress != null) ? 1 : 0);
            publicAddresses = new ArrayList<EndpointAddress>(size);
            if(publicAddress != null) {
                publicAddresses.add(publicAddress);
            }
            publicAddresses.addAll(boundAddresses);
        }
    }

    private SocketAddress bindServerChannel(List<? extends SocketAddress> potentialBindpoints) {
        
        for(SocketAddress nextBP : potentialBindpoints) {

            try {
                serverChannel = serverBootstrap.bind(nextBP);
                channels.add(serverChannel);
                return nextBP;
            } catch(ChannelException e) {
                String failReason = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                Logging.logCheckedInfo(LOG, "Attempt to bind to ", nextBP, " failed (", failReason, "), trying another address");
            }
            
        }
        
        return null;
    }
    
    public boolean start(EndpointService endpointService) throws IllegalStateException {
        if(started.get()) {
            throw new IllegalStateException("already started");
        }
        
        this.endpointService = endpointService;
        listener = endpointService.addMessageTransport(this);
        
        if(listener == null) {

            Logging.logCheckedSevere(LOG, "Transport registration failed for netty transport server, protocol=", addrTranslator.getProtocolName());
            return false;

        }
        
        started.set(true);
        return true;
    }
    
    public void beginStop() {

        if(!started.compareAndSet(true, false)) {

            Logging.logCheckedWarning(LOG, "Netty transport server for protocol ", addrTranslator.getProtocolName(), " already stopped or never started!");
            return;

        }
        
        closeChannelsFuture = channels.close();
    }
    
    public void stop() throws IllegalStateException {
        if(closeChannelsFuture != null) {
            closeChannelsFuture.awaitUninterruptibly();
        }
        
        serverChannel = null;
        serverBootstrap.releaseExternalResources();
        timeoutTimer.stop();
    }

    public void newConnection(Channel channel, EndpointAddress directedAt, EndpointAddress logicalEndpointAddress) {
        // EndpointAddress localAddr = addrTranslator.toEndpointAddress(channel.getLocalAddress(), serverChannel.getLocalAddress());
        // NettyMessenger messenger = new NettyMessenger(channel, homeGroupID, localPeerID, directedAt, logicalEndpointAddress, endpointService);
	    AsynchronousNettyMessenger messenger = new AsynchronousNettyMessenger(channel, homeGroupID, localPeerID, directedAt, logicalEndpointAddress, endpointService);
	    listener.messengerReady(new MessengerEvent(this, messenger, messenger.getDestinationAddress()));
	}
	
    public Iterator<EndpointAddress> getPublicAddresses() {
        return publicAddresses.iterator();
    }

    public EndpointService getEndpointService() {
        return endpointService;
    }

    public String getProtocolName() {
        return addrTranslator.getProtocolName();
    }

    @Sharable
    private final class ConnectionGroupAddHandler extends SimpleChannelUpstreamHandler {

        @Override
        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {


            channels.add(e.getChildChannel());
            super.childChannelOpen(ctx, e);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            // BindExceptions are transformed into ChannelBindExceptions by the netty
            // ServerBootstrap, and handled in in bindServerChannel()
            if(!(e.getCause() instanceof BindException)) {
                LOG.log(Level.WARNING, "Unexpected exception on server channel for {0} protocol:\n{1}", 
                        new Object[] { getProtocolName(), e.getCause() });
            }
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    /**
     * @return the physically bound addresses for this transport, as opposed to those which are
     * broadcasted to external peers.
     */
    public List<EndpointAddress> getBoundAddresses() {
        return boundAddresses;
    }
}
