package net.jxta.impl.endpoint.netty;

import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.TransportUtils;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.TransportAdvertisement;

import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * JBoss Netty based endpoint transport. Utilizes netty client and server channel
 * factories to implement the standard JXTA messaging protocol described in
 * Section 7.1 "TCP/IP Message Transport" of the JXTA v2.0 protocols specification.
 * 
 * By default, this implementation uses the Netty NIO socket factories, and so
 * requires an unfettered TCP/IP connection to communicate. Subclasses may use
 * alternative factories to work around this restriction, for instance by negotiating
 * the opening of a port using UPnP or STUN/TURN, or utilizing HTTP to tunnel the
 * data.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class NettyTransport implements Module {
    
    private static final Logger LOG = Logger.getLogger(NettyTransport.class.getName());

    public static final int MODULE_STARTUP_FAILED = -1;
    
    private PeerGroup group;

    private String protocolName;
    
    private boolean serverEnabled = true;
    private TransportServerComponent server;

    private boolean clientEnabled = true;
    private TransportClientComponent client;
    
    
    private boolean started = false;
    
    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) throws PeerGroupException {
        this.group = group;
        this.protocolName = getDefaultProtocolName();
        processStaticConfiguration(implAdv);
        
        TCPAdv instanceConfiguration = extractInstanceConfiguration(assignedID);
        initServer(instanceConfiguration);
        initClient(instanceConfiguration, getPreferredReturnAddress(instanceConfiguration));
        
        if(Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            LOG.config(buildConfigurationState(assignedID, (ModuleImplAdvertisement)implAdv));
        }
    }

    private void initServer(TCPAdv instanceConfiguration) throws PeerGroupException {
        this.serverEnabled = instanceConfiguration.isServerEnabled();
        if(!serverEnabled) {
            this.server = new NullTransportServerComponent();
            return;
        }
        
        String interfaceAddress = instanceConfiguration.getInterfaceAddress();
        
        InetAddress bindAddr;
        if(interfaceAddress != null) {
            bindAddr = TransportUtils.resolveInterfaceAddress(interfaceAddress);
        } else {
            bindAddr = IPUtils.ANYADDRESS;
        }
        
        String publicName = instanceConfiguration.getServer();
        EndpointAddress publicAddress = null;
        if (publicName != null) {
            publicAddress = new EndpointAddress(protocolName, publicName, null, null);
        }
        
        NettyTransportServer server = new NettyTransportServer(createServerSocketChannelFactory(), new InetSocketAddressTranslator(protocolName), group);
        
        int preferredPort = correctPort(instanceConfiguration.getPort(), 1, 65535, getDefaultPort(), getDefaultPort(), "Preferred");
        int startPort = correctPort(instanceConfiguration.getStartPort(), 1, preferredPort, getDefaultPortRangeLowerBound(), 1, "Range start");
        int endPort = correctPort(instanceConfiguration.getEndPort(), startPort, 65535, getDefaultPortRangeUpperBound(), 65535, "Range end");
        List<InetSocketAddress> potentialBindpoints = IPUtils.createRandomSocketAddressList(bindAddr, preferredPort, startPort, endPort);
        
        server.init(potentialBindpoints, publicAddress, instanceConfiguration.getPublicAddressOnly());
        this.server = server;
    }
    
    private void initClient(TCPAdv instanceConfiguration, EndpointAddress returnAddress) {
        this.clientEnabled = instanceConfiguration.isClientEnabled();
        if(!clientEnabled) {
            client = new NullTransportClientComponent();
            return;
        }
        
        client = new NettyTransportClient(createClientSocketChannelFactory(), new InetSocketAddressTranslator(protocolName), group, returnAddress);
    }

    private EndpointAddress getPreferredReturnAddress(TCPAdv instanceConfiguration) {
        if(server == null || !server.getPublicAddresses().hasNext()) {
            InetAddress addr = TransportUtils.resolveInterfaceAddress(instanceConfiguration.getInterfaceAddress());
            InetSocketAddress socketAddress = new InetSocketAddress(addr, 0);
            InetSocketAddressTranslator translator = new InetSocketAddressTranslator(protocolName);
            return translator.toEndpointAddress(socketAddress);
        } else {
            // the preferred address is assumed to be the first one
            return server.getPublicAddresses().next();
        }
    }

    private String buildConfigurationState(ID assignedID, ModuleImplAdvertisement implAdv) {
        StringWriter writer = new StringWriter();
        writer.append("Configuring ").append(getTransportDescriptiveName()).append(" Transport : ").append(assignedID.toString());
        
        if(implAdv != null) {
            writer.append("\n\tImplementation: ");
            writer.append("\n\t\tModule Spec ID: ").append(implAdv.getModuleSpecID().toString());
            writer.append("\n\t\tImpl Description: ").append(implAdv.getDescription());
            writer.append("\n\t\tImpl URI: ").append(implAdv.getUri());
            writer.append("\n\t\tImpl Code: ").append(implAdv.getCode());
        }
        
        writer.append("\n\tGroup Params:");
        writer.append("\n\t\tGroup: ").append(group.toString());
        writer.append("\n\t\tPeer ID: ").append(group.getPeerID().toString());
        
        writer.append("\n\tConfiguration:");
        writer.append("\n\t\tProtocol: ").append(protocolName);
        
        writer.append("\n\tServer enabled: ").append(Boolean.toString(serverEnabled));
        if(serverEnabled) {
            writer.append("\n\t\tServer physical addresses:");
            for(EndpointAddress addr : server.getBoundAddresses()) {
                writer.append("\n\t\t\t" + addr);
            }
            writer.append("\n\t\tServer public addresses:");
            Iterator<EndpointAddress> publicAddrs = server.getPublicAddresses();
            while(publicAddrs.hasNext()) {
                writer.append("\n\t\t\t" + publicAddrs.next());
            }
        }
        writer.append("\n\tClient enabled: ").append(Boolean.toString(clientEnabled));
        if(clientEnabled) {
            writer.append("\n\t\tClient return address: ").append(client.getPublicAddress().toString());
        }
        
        return writer.toString();
    }

    private void processStaticConfiguration(Advertisement implAdv) {
        if(implAdv == null || !(implAdv instanceof ModuleImplAdvertisement)) {
            return;
        }
        ModuleImplAdvertisement moduleImplAdv = (ModuleImplAdvertisement) implAdv;
        StructuredDocument<?> parameters = moduleImplAdv.getParam();
        
        if(parameters != null) {
            Enumeration<?> protoChildren = parameters.getChildren("Proto");
            if(protoChildren.hasMoreElements()) {
                this.protocolName = ((XMLElement<?>) protoChildren.nextElement()).getTextValue();
            }
        }
    }

    // Type conversion warnings are disabled due to the unhelpful generics structure of StructuredDocument and Element.
    @SuppressWarnings("unchecked")
    private TCPAdv extractInstanceConfiguration(ID assignedID) {
        ConfigParams configAdvertisement = group.getConfigAdvertisement();
        
        XMLElement instanceParameters = (XMLDocument) configAdvertisement.getServiceParam(assignedID);
        
        if(instanceParameters == null) {
            return null;
        }
        
        Enumeration<XMLElement> adverts = instanceParameters.getChildren(TransportAdvertisement.getAdvertisementType());
        
        if(!adverts.hasMoreElements()) {
            return null;
        }
        
        try {
            XMLElement adv = adverts.nextElement();
            Advertisement advertisement = AdvertisementFactory.newAdvertisement(adv);
            if(!(advertisement instanceof TCPAdv)) {
                throw new IllegalArgumentException("Service parameter for " + assignedID + " should be a " + TCPAdv.getAdvertisementType() + ", but is a " + advertisement.getAdvType());
            }
            
            return (TCPAdv) advertisement;
        } catch(NoSuchElementException e) {
            throw new IllegalArgumentException("Service parameter for " + assignedID + " is not a valid advertisement");
        }
    }

    /**
     * @return the socket channel factory to be used for outgoing connections by the client. It is intended
     * that this be overridden, if a child implementation wishes to change the mechanism used to establish an
     * outbound connection.
     */
    protected ClientSocketChannelFactory createClientSocketChannelFactory() {
        return new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }
    
    /**
     * @return the server socket channel factory to be used for binding and accepting connections from
     * remote peers. It is intended that this be overridden, if a child implementation wishes to change 
     * the mechanism used to accept an inbound connection.
     */
    protected ServerSocketChannelFactory createServerSocketChannelFactory() {
        return new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }
    
    /**
     * Used to take a port from the configuration, and "correct" it to a useable port number.
     * <ol>
     *     <li>If the port is -1 (meaning "default"), then the default port is returned.</li>
     *     <li>If the port is 0 (meaning "any legal port"), then the anyPort parameter is returned.</li>
     *     <li>Otherwise, the port is constrained to the bounds defined by the min and max parameters.</li>
     * </ol>
     */
    private int correctPort(int port, int min, int max, int defaultPort, int anyPort, String portName) {
        if(port == -1) {
            return defaultPort;
        } else if(port == 0) {
            return anyPort;
        } else if(port < min || port > max) {
            port = Math.max(min, Math.min(port, max));
            if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "{0} port was outside legal range ({1}-{2}), changed to {3}", new Object[] { portName, min, max, port });
            }
        }
        
        return port;
    }

    public int startApp(String[] args) {
        if(!serverEnabled && !clientEnabled) {
            return Module.START_DISABLED;
        }
        
        EndpointService endpointService = group.getEndpointService();
        
        if(endpointService == null) {
            return Module.START_AGAIN_STALLED;
        }
        
        if(!client.start(endpointService) || !server.start(endpointService)) {
            return MODULE_STARTUP_FAILED;
        }
        
        started = true;
        return Module.START_OK;
    }

    public void stopApp() {
        if(!started) {
            return;
        }
        started = false;
        
        client.beginStop();
        server.beginStop();
        client.stop();
        server.stop();
        
        group = null;
    }

    public String getProtocolName() {
        return protocolName;
    }
    
    /* Methods we expect child classes to override, typically to distinguish
     * themselves from this default TCP-based implementation
     */
    
    /**
     * Returns the protocol name which will be used, if not specified in the "Proto"
     * element of the module implementation advertisement.
     */
    protected String getDefaultProtocolName() {
        return "tcp";
    }
    
    /**
     * The default port for this transport, if not specified by the instance configuration
     * for this transport.
     */
    protected int getDefaultPort() {
        return 7901;
    }
    
    /**
     * If binding to the preferred or default port fails, we will typically attempt to bind
     * to any port within a range specified by the configuration or within a default range.
     * This method should specify the <em>lower</em> end of the default range, used if the
     * configuration does not specify anything else.
     */
    protected int getDefaultPortRangeLowerBound() {
        return 7901;
    }
    
    /**
     * If binding to the preferred or default port fails, we will typically attempt to bind
     * to any port within a range specified by the configuration or within a default range.
     * This method should specify the <em>upper</em> end of the default range, used if the
     * configuration does not specify anything else.
     */
    protected int getDefaultPortRangeUpperBound() {
        return 7999;
    }
    
    /**
     * A short human-readable name for this transport, that will be displayed in configuration
     * logging.
     */
    protected String getTransportDescriptiveName() {
        return "Netty TCP";
    }
}
