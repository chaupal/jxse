package net.jxta.impl.endpoint.netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.HashedWheelTimer;

/**
 * Client side of a netty based transport. Responsible for initiating outgoing connections.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class NettyTransportClient implements MessageSender, TransportClientComponent {

    private final class ClientConnectionRegistrationHandler implements NettyChannelRegistry {
        public EndpointAddress directedAt;
        public EndpointAddress logicalEndpointAddress;
        public CountDownLatch latch = new CountDownLatch(1);

        public void newConnection(Channel channel, EndpointAddress directedAt, EndpointAddress logicalEndpointAddress) {
            this.directedAt = directedAt;
            this.logicalEndpointAddress = logicalEndpointAddress;
            latch.countDown();
        }
    }

    private static final Logger LOG = Logger.getLogger(NettyTransportClient.class.getName());
    
    private EndpointAddress localAddress;
    
    private PeerGroupID homeGroupID;
    private PeerID localPeerID;
    private EndpointService endpointService;
    private MessengerEventListener messageEventListener;
    private AddressTranslator addrTranslator;
    
    private ChannelGroup channels;
    private HashedWheelTimer timeoutTimer;
    private ChannelGroupFuture closeChannelsFuture;
    private AtomicBoolean started;
    private AtomicBoolean stopping;
    
    private ChannelFactory clientFactory;

    private EndpointAddress returnAddress;
    
    public NettyTransportClient(ChannelFactory clientFactory, AddressTranslator addrTranslator, PeerGroup group, EndpointAddress returnAddress) {
        this.started = new AtomicBoolean(false);
        this.stopping = new AtomicBoolean(false);
        this.channels = new DefaultChannelGroup();
        this.addrTranslator = addrTranslator;
        this.clientFactory = clientFactory;
        this.returnAddress = returnAddress;
        
        localAddress = returnAddress;
        
        this.homeGroupID = group.getPeerGroupID();
        this.localPeerID = group.getPeerID();
        
        timeoutTimer = new HashedWheelTimer();
    }

    public boolean start(EndpointService endpointService) {
        this.endpointService = endpointService;
        messageEventListener = endpointService.addMessageTransport(this);
        
        if(messageEventListener == null) {
            return false;
        }
        
        started.set(true);
        return true;
    }
    
    public void beginStop() {
        if(!started.get()) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Netty transport server for protocol " + addrTranslator.getProtocolName() + " already stopped or never started!");
            }
            return;
        }
        closeChannelsFuture = channels.close();
        stopping.set(true);
    }
    
    public void stop() {
        if(!stopping.get()) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Netty transport server for protocol " + addrTranslator.getProtocolName() + " already stopped or never started!");
            }
            return;
        }
        closeChannelsFuture.awaitUninterruptibly();
        clientFactory.releaseExternalResources();
        timeoutTimer.stop();
        
        endpointService.removeMessageTransport(this);
        endpointService = null;
    }

    public Messenger getMessenger(EndpointAddress dest, Object hint) {
        if(!started.get()) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Request to get messenger for " + dest.toString() + " when netty transport client stopped or never started");
            }
            return null;
        }
        
        if(Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "processing request to open connection to {0}", dest);
        }
        
        ClientConnectionRegistrationHandler clientRegistry = new ClientConnectionRegistrationHandler();
        
        ClientBootstrap bootstrap = new ClientBootstrap(clientFactory);
        bootstrap.setPipelineFactory(new NettyTransportChannelPipelineFactory(localPeerID, timeoutTimer, clientRegistry, addrTranslator, dest, returnAddress));
        
        ChannelFuture connectFuture = bootstrap.connect(addrTranslator.toSocketAddress(dest));
        try {
            if(!connectFuture.await(5000L, TimeUnit.MILLISECONDS)) {
                if(Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "Netty transport for protocol {0} failed to connect to {1} within acceptable time", 
                            new Object[] { addrTranslator.getProtocolName(), dest });
                }
                return null;
            }
        } catch(InterruptedException e) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Interrupted while waiting for connection to {0} to be established", dest);
            }
            connectFuture.cancel();
            return null;
        }
        
        if(!connectFuture.isSuccess()) {
            if(Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                Throwable cause = connectFuture.getCause();
                String causeString = (cause != null) ? cause.getMessage() : "cause unknown";
				String message = String.format("Netty transport for protocol %s failed to connect to %s - %s", addrTranslator.getProtocolName(), dest, causeString);
                LOG.log(Level.INFO, message);
            }
            return null;
        }
        
        boolean established = false;
        try {
            established = clientRegistry.latch.await(15L, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Interrupted while waiting for connection handover", e);
            }
        }
        
        if(!established) {
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Connection handover timed out - either remote host was not a valid JXTA peer or did not respond on time");
            }
            connectFuture.getChannel().close();
            return null;
        }
        
        if(Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "succeeded in connecting to {0}, remote peer has logical address {1}", new Object[] { dest, clientRegistry.logicalEndpointAddress });
        }
        
        channels.add(connectFuture.getChannel());
        // return new NettyMessenger(connectFuture.getChannel(), homeGroupID, localPeerID, clientRegistry.directedAt, clientRegistry.logicalEndpointAddress, endpointService);
        return new AsynchronousNettyMessenger(connectFuture.getChannel(), homeGroupID, localPeerID, clientRegistry.directedAt, clientRegistry.logicalEndpointAddress, endpointService);
    }
    
    public boolean allowsRouting() {
        return true;
    }

    public EndpointAddress getPublicAddress() {
        return localAddress;
    }

    public boolean isConnectionOriented() {
        return true;
    }

    @Deprecated
    public boolean ping(EndpointAddress addr) {
        throw new RuntimeException("ping is deprecated, do not use!");
    }

    public EndpointService getEndpointService() {
        return endpointService;
    }

    public String getProtocolName() {
        return addrTranslator.getProtocolName();
    }

    @Deprecated
    public Object transportControl(Object operation, Object value) {
        throw new RuntimeException("transportControl is deprecated, do not use!");
    }
}
