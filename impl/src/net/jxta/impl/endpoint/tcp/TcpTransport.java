/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
package net.jxta.impl.endpoint.tcp;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.TextElement;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessagePropagater;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.LoopbackMessenger;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.endpoint.transportMeter.TransportServiceMonitor;
import net.jxta.impl.meter.MonitorManager;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;
import net.jxta.meter.MonitorResources;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.TransportAdvertisement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the TCP Message Transport
 *
 * @see net.jxta.endpoint.MessageTransport
 * @see net.jxta.endpoint.MessagePropagater
 * @see net.jxta.endpoint.MessageReceiver
 * @see net.jxta.endpoint.MessageSender
 * @see net.jxta.endpoint.EndpointService
 * @see <a href="http://spec.jxta.org/v1.0/docbook/JXTAProtocols.html#trans-tcpipt">JXTA Protocols Specification : Standard JXTA Transport Bindings</a>
 */
public class TcpTransport implements Runnable, Module, MessageSender, MessageReceiver, MessagePropagater {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(TcpTransport.class.getName());

    /**
     * The TCP send buffer size.
     * The size of the buffer used to store outgoing messages
     * This should be set to the maximum message size (smaller is allowed).
     */
    static final int SendBufferSize = 64 * 1024; // 64 KBytes

    /**
     * The TCP receive buffer size
     */
    static final int RecvBufferSize = 64 * 1024; // 64 KBytes

    /**
     * The amount of time the socket "lingers" after we close it locally.
     * Linger enables the remote socket to finish receiving any pending data
     * at its own rate.
     * Note: LingerDelay time unit is seconds
     */
    static final int LingerDelay = 2 * 60;

    /**
     * Connection  timeout
     * use the same system property defined by URLconnection, otherwise default to 20 seconds.
     */
    static int connectionTimeOut = 20 * (int) TimeUtils.ASECOND;

    // Java's default is 50
    static final int MaxAcceptCnxBacklog = 50;

    private String serverName = null;
    private List<EndpointAddress> publicAddresses = new ArrayList<EndpointAddress>();
    private EndpointAddress publicAddress = null;
    private MessageElement msgSrcAddrElement = null;

    private String interfaceAddressStr;
    InetAddress usingInterface;
    private int serverSocketPort;
    private int restrictionPort = -1;
    private IncomingUnicastServer unicastServer = null;

    private boolean isClosed = false;

    private boolean allowMulticast = true;
    private String multicastAddress = "224.0.1.85";
    private int multicastPortNb = 1234;
    private int multicastPacketSize = 16384;
    private EndpointAddress mAddress = null;
    private InetAddress propagateInetAddress;
    private int propagatePort;
    private int propagateSize;
    private Thread multicastThread = null;
    private MulticastSocket multicastSocket = null;

    private long messagesSent = 0;
    private long messagesReceived = 0;
    private long bytesSent = 0;
    private long bytesReceived = 0;
    private long connectionsAccepted = 0;

    PeerGroup group = null;
    EndpointService endpoint = null;
    ThreadPoolExecutor executor;

    private String protocolName = "tcp";
    private TransportMeter unicastTransportMeter;
    private TransportMeter multicastTransportMeter;
    private TransportBindingMeter multicastTransportBindingMeter;

    private boolean publicAddressOnly = false;

    private MessengerEventListener messengerEventListener = null;

    private Thread messengerSelectorThread;
    Selector messengerSelector = null;

    private final Map<TcpMessenger, SocketChannel> regisMap = new ConcurrentHashMap<TcpMessenger, SocketChannel>();
    private final Set<SocketChannel> unregisMap = Collections.synchronizedSet(new HashSet<SocketChannel>());

    /**
     * This is the thread group into which we will place all of the threads
     * we create. THIS HAS NO EFFECT ON SCHEDULING. Java thread groups are
     * only for organization and naming.
     */
    ThreadGroup myThreadGroup = null;

    /**
     * The maximum number of write selectors we will maintain in our cache.
     */
    protected final static int MAX_WRITE_SELECTORS = 20;

    /**
     * A cache we maintain for selectors writing messages to the socket.
     */
    private final static Stack<Selector> writeSelectorCache = new Stack<Selector>();
    private MulticastProcessor multicastProcessor;

    /**
     * Construct a new TcpTransport instance
     */
    public TcpTransport() {
        try {
            try {
                for (int i = 0; i < MAX_WRITE_SELECTORS; i++) {
                    writeSelectorCache.add(Selector.open());
                }
            } catch (IOException ex) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Could not create the write selector pool");
                }
                throw ex;
            }
            String connectTOStr = System.getProperty("sun.net.client.defaultConnectTimeout");

            if (connectTOStr != null) {
                connectionTimeOut = Integer.parseInt(connectTOStr);
            }
        } catch (Exception e) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Could not parse system property: sun.net.client.defaultConnectTimeout");
            }
        }
    }

    /**
     * Getter for property 'allowMulticast'.
     *
     * @return Value for property 'allowMulticast'.
     */
    public boolean isAllowMulticast() {
        return allowMulticast;
    }

    /**
     * Setter for property 'allowMulticast'.
     *
     * @param allowMulticast Value to set for property 'allowMulticast'.
     */
    public void setAllowMulticast(boolean allowMulticast) {
        this.allowMulticast = allowMulticast;
    }

    /**
     * Gets the number of 'connectionsAccepted'.
     *
     * @return the number of 'connectionsAccepted'.
     */
    public long getConnectionsAccepted() {
        return connectionsAccepted;
    }

    /**
     * increment the number of connectionsAccepted sent by 1
     */
    public void incrementConnectionsAccepted() {
        connectionsAccepted++;
    }

    /**
     * increment the number of messages sent by 1
     */
    public void incrementMessagesSent() {
        messagesSent++;
    }

    /**
     * increment the number of messages received by 1
     */
    public void incrementMessagesReceived() {
        messagesReceived++;
    }

    /**
     * increment the number of bytes sent
     *
     * @param bytes the number of bytes to be added
     */
    public void incrementBytesSent(long bytes) {
        bytesSent += bytes;
    }

    /**
     * increment the number of bytes received
     *
     * @param bytes the number of bytes to be added
     */
    public void incrementBytesReceived(long bytes) {
        bytesReceived += bytes;
    }

    /**
     * Gets the number of 'messagesSent'.
     *
     * @return the number of 'messagesSent'.
     */
    public long getMessagesSent() {
        return messagesSent;
    }

    /**
     * Gets the number of 'messagesReceived'.
     *
     * @return the number of 'messagesReceived'.
     */
    public long getMessagesReceived() {
        return messagesReceived;
    }

    /**
     * Gets the number of 'bytesSent'.
     *
     * @return the number of 'bytesSent'.
     */
    public long getBytesSent() {
        return bytesSent;
    }

    /**
     * Gets the number of 'bytesReceived'.
     *
     * @return the number of 'bytesReceived'.
     */
    public long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object target) {
        if (this == target) {
            return true;
        }

        if (null == target) {
            return false;
        }

        if (target instanceof TcpTransport) {
            TcpTransport likeMe = (TcpTransport) target;

            if (!getProtocolName().equals(likeMe.getProtocolName())) {
                return false;
            }

            // todo 20020630 bondolo@jxta.org Compare the multicasts.
            Iterator<EndpointAddress> itsAddrs = likeMe.publicAddresses.iterator();

            for (EndpointAddress publicAddress1 : publicAddresses) {
                if (!itsAddrs.hasNext()) {
                    return false;
                } // it has fewer than i do.

                EndpointAddress mine = publicAddress1;
                EndpointAddress its = itsAddrs.next();

                if (!mine.equals(its)) {
                    // content didnt match
                    return false;
                }
            }
            // ran out at the same time?
            return (!itsAddrs.hasNext());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getPublicAddress().hashCode();
    }

    /**
     * Initialization of the TcpTransport (called by Platform)
     */
    public void init(PeerGroup group, ID assignedID, Advertisement impl) throws PeerGroupException {

        this.group = group;
        ModuleImplAdvertisement implAdvertisement = (ModuleImplAdvertisement) impl;

        this.executor = ((StdPeerGroup) group).getExecutor();

        try {
            ConfigParams configAdv = group.getConfigAdvertisement();

            // Get out invariable parameters from the implAdv
            Element param = implAdvertisement.getParam();

            if (param != null) {
                Enumeration list = param.getChildren("Proto");

                if (list.hasMoreElements()) {
                    TextElement pname = (TextElement) list.nextElement();
                    protocolName = pname.getTextValue();
                }
            }

            // Get our peer-defined parameters in the configAdv
            param = configAdv.getServiceParam(assignedID);
            Enumeration tcpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

            // get the TransportAdv
            if (tcpChilds.hasMoreElements()) {
                param = (Element) tcpChilds.nextElement();
                Attribute typeAttr = ((Attributable) param).getAttribute("type");

                if (!TCPAdv.getAdvertisementType().equals(typeAttr.getValue())) {
                    throw new IllegalArgumentException("transport adv is not a " + TCPAdv.getAdvertisementType());
                }

                if (tcpChilds.hasMoreElements()) {
                    throw new IllegalArgumentException("Multiple transport advs detected for " + assignedID);
                }
            } else {
                throw new IllegalArgumentException(TransportAdvertisement.getAdvertisementType() + " could not be located.");
            }

            Advertisement paramsAdv = null;

            try {
                paramsAdv = AdvertisementFactory.newAdvertisement((XMLElement) param);
            } catch (NoSuchElementException notThere) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Could not find parameter document", notThere);
                }
            }

            if (!(paramsAdv instanceof TCPAdv)) {
                throw new IllegalArgumentException("Provided Advertisement was not a " + TCPAdv.getAdvertisementType());
            }

            try {
                messengerSelector = SelectorProvider.provider().openSelector();
            } catch (IOException e) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Could not create a messenger selector", e);
                }
            }

            TCPAdv adv = (TCPAdv) paramsAdv;

            // determine the local interface to use. If the user specifies
            // one, use that. Otherwise, use the all the available interfaces.
            interfaceAddressStr = adv.getInterfaceAddress();
            if (interfaceAddressStr != null) {
                try {
                    usingInterface = InetAddress.getByName(interfaceAddressStr);
                } catch (UnknownHostException failed) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.warning("Invalid address for local interface address, using default");
                    }
                    usingInterface = IPUtils.ANYADDRESS;
                }
            } else {
                usingInterface = IPUtils.ANYADDRESS;
            }

            serverName = adv.getServer();

            // Even when server is not enabled, we use the serverSocketPort
            // as a discriminant for the simulated network partitioning,
            // human readable messages, and a few things of that sort.
            serverSocketPort = adv.getPort();

            // should we expose other than a public address if one was
            // specified ?
            publicAddressOnly = adv.getPublicAddressOnly();

            // Start the servers
            myThreadGroup = new ThreadGroup(group.getHomeThreadGroup(), "TcpTransport " + usingInterface.getHostAddress());

            if (adv.isServerEnabled()) {
                unicastServer = new IncomingUnicastServer(this, usingInterface, serverSocketPort, adv.getStartPort(), adv.getEndPort());
                InetSocketAddress boundAddresss = unicastServer.getLocalSocketAddress();

                // TODO bondolo 20040628 Save the port back as a preference to TCPAdv

                // Build the publicAddresses :
                // first in the list is the "public server name". We don't try to
                // resolve this since it might not be resolvable in the context
                // we are running in, we just assume it's good.
                if (serverName != null) {
                    // use speced server name.
                    EndpointAddress newAddr = new EndpointAddress(protocolName, serverName, null, null);
                    publicAddresses.add(newAddr);
                }

                // then add the rest of the local interfaces as appropriate
                // Unless we find an non-loopback interface, we're in local
                // only mode.
                boolean localOnly = true;

                if (usingInterface.equals(IPUtils.ANYADDRESS)) {
                    // its wildcarded
                    Iterator eachLocal = IPUtils.getAllLocalAddresses();
                    List<EndpointAddress> wildAddrs = new ArrayList<EndpointAddress>();

                    while (eachLocal.hasNext()) {
                        InetAddress anAddress = (InetAddress) eachLocal.next();
                        String hostAddress = IPUtils.getHostAddress(anAddress);
                        EndpointAddress newAddr = new EndpointAddress(protocolName,
                                hostAddress + ":" + Integer.toString(boundAddresss.getPort()), null, null);

                        // don't add it if its already in the list
                        if (!anAddress.isLoopbackAddress()) {
                            localOnly = false;
                        }

                        if (!publicAddresses.contains(newAddr)) {
                            wildAddrs.add(newAddr);
                        }
                    }

                    // we sort them so that later equals() will be deterministic.
                    // the result of IPUtils.getAllLocalAddresses() is not known
                    // to be sorted.
                    Collections.sort(wildAddrs, new Comparator<EndpointAddress>() {
                        public int compare(EndpointAddress one, EndpointAddress two) {
                            return one.toString().compareTo(two.toString());
                        }

                        public boolean equals(Object that) {
                            return (this == that);
                        }
                    });

                    // Add public addresses:
                    // don't add them if we have a hand-set public address
                    // and the publicAddressOnly property is set.
                    if (!(serverName != null && publicAddressOnly)) {
                        publicAddresses.addAll(wildAddrs);
                    }
                } else {
                    // use specified interface
                    if (!usingInterface.isLoopbackAddress()) {
                        localOnly = false;
                    }

                    String hostAddress = IPUtils.getHostAddress(usingInterface);
                    EndpointAddress newAddr = new EndpointAddress(protocolName,
                            hostAddress + ":" + Integer.toString(boundAddresss.getPort()), null, null);

                    // Add public address:
                    // don't add it if its already in the list
                    // don't add it if specified as public address and publicAddressOnly
                    if (!(serverName != null && publicAddressOnly)) {
                        if (!publicAddresses.contains(newAddr)) {
                            publicAddresses.add(newAddr);
                        }
                    }
                }

                // If the only available interface is LOOPBACK,
                // then make sure we use only that (that includes
                // resetting the outgoing/listening interface
                // from ANYADDRESS to LOOPBACK).

                if (localOnly) {
                    usingInterface = IPUtils.LOOPBACK;
                    publicAddresses.clear();
                    String hostAddress = IPUtils.getHostAddress(usingInterface);
                    EndpointAddress pubAddr = new EndpointAddress(protocolName
                            ,
                            hostAddress + ":" + Integer.toString(boundAddresss.getPort()), null, null);

                    publicAddresses.add(pubAddr);
                }

                // Set the "prefered" public address. This is the address we
                // will use for identifying outgoing requests.
                publicAddress = publicAddresses.get(0);
            } else {
                // Only the outgoing interface matters.
                // Verify that ANY interface does not in fact mean
                // LOOPBACK only. If that's the case, we want to make
                // that explicit, so that consistency checks regarding
                // the allowed use of that interface work properly.
                if (usingInterface.equals(IPUtils.ANYADDRESS)) {
                    boolean localOnly = true;
                    Iterator eachLocal = IPUtils.getAllLocalAddresses();

                    while (eachLocal.hasNext()) {
                        InetAddress anAddress = (InetAddress) eachLocal.next();

                        if (!anAddress.isLoopbackAddress()) {
                            localOnly = false;
                            break;
                        }
                    }

                    if (localOnly) {
                        usingInterface = IPUtils.LOOPBACK;
                    }
                }

                // The "public" address is just an internal label
                // it is not usefull to anyone outside.
                // IMPORTANT: we set the port to zero, to signify that this
                // address is not realy usable. This means that the
                // TCP restriction port HACK will NOT be consistent in stopping
                // multicasts if you do not enable incoming connections.
                String hostAddress = IPUtils.getHostAddress(usingInterface);

                publicAddress = new EndpointAddress(protocolName, hostAddress + ":0", null, null);
            }

            msgSrcAddrElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, publicAddress.toString(), null);

            // Get the multicast configuration.
            allowMulticast = adv.getMulticastState();
            if (allowMulticast) {
                multicastAddress = adv.getMulticastAddr();
                multicastPortNb = adv.getMulticastPort();
                multicastPacketSize = adv.getMulticastSize();

                mAddress = new EndpointAddress(protocolName, multicastAddress + ":" + Integer.toString(multicastPortNb), null
                        ,
                        null);

                // Create the multicast input socket
                propagatePort = multicastPortNb;
                propagateSize = multicastPacketSize;
                propagateInetAddress = InetAddress.getByName(multicastAddress);
                multicastSocket = new MulticastSocket(new InetSocketAddress(usingInterface, propagatePort));
                try {
                    multicastSocket.setLoopbackMode(false);
                } catch (SocketException ignored) {// We may not be able to set loopback mode. It is
                    // inconsistent whether an error will occur if the set fails.
                }
            }
        } catch (Exception e) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Initialization exception", e);
            }

            throw new PeerGroupException("Initialization exception", e);
        }

        // Tell tell the world about our configuration.
        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            StringBuilder configInfo = new StringBuilder("Configuring TCP Message Transport : " + assignedID);

            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: ").append(implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
            }

            configInfo.append("\n\tGroup Params:");
            configInfo.append("\n\t\tGroup : ").append(group);
            configInfo.append("\n\t\tPeer ID: ").append(group.getPeerID());

            configInfo.append("\n\tConfiguration:");
            configInfo.append("\n\t\tProtocol: ").append(protocolName);
            configInfo.append("\n\t\tPublic address: ").append(serverName == null ? "(unspecified)" : serverName);
            configInfo.append("\n\t\tInterface address: ").append(
                    interfaceAddressStr == null ? "(unspecified)" : interfaceAddressStr);
            configInfo.append("\n\t\tMulticast State: ").append(allowMulticast ? "Enabled" : "Disabled");

            if (allowMulticast) {
                configInfo.append("\n\t\t\tMulticastAddr: ").append(multicastAddress);
                configInfo.append("\n\t\t\tMulticastPort: ").append(multicastPortNb);
                configInfo.append("\n\t\t\tMulticastPacketSize: ").append(multicastPacketSize);
            }

            configInfo.append("\n\tConfiguration :");
            if (null != unicastServer) {
                if (-1 == unicastServer.getStartPort()) {
                    configInfo.append("\n\t\tUnicast Server Bind Addr: ").append(usingInterface.getHostAddress()).append(":").append(
                            serverSocketPort);
                } else {
                    configInfo.append("\n\t\tUnicast Server Bind Addr: ").append(usingInterface.getHostAddress()).append(":").append(serverSocketPort).append(" [").append(unicastServer.getStartPort()).append("-").append(unicastServer.getEndPort()).append(
                            "]");
                }
                configInfo.append("\n\t\tUnicast Server Bound Addr: ").append(unicastServer.getLocalSocketAddress());
            } else {
                configInfo.append("\n\t\tUnicast Server : disabled");
            }

            if (allowMulticast) {
                configInfo.append("\n\t\tMulticast Server Bind Addr: ").append(multicastSocket.getLocalSocketAddress());
            }
            configInfo.append("\n\t\tPublic Addresses: ");
            configInfo.append("\n\t\t\tDefault Endpoint Addr : ").append(publicAddress);

            for (Object publicAddress1 : publicAddresses) {
                EndpointAddress anAddr = (EndpointAddress) publicAddress1;

                configInfo.append("\n\t\t\tEndpoint Addr : ").append(anAddr);
            }
            LOG.config(configInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int startApp(String[] arg) {
        endpoint = group.getEndpointService();

        if (null == endpoint) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Stalled until there is an endpoint service");
            }
            return Module.START_AGAIN_STALLED;
        }

        messengerSelectorThread = new Thread(myThreadGroup, new MessengerSelectorThread(), "MessengerSelectorThread for " + this);
        messengerSelectorThread.setDaemon(true);
        messengerSelectorThread.start();

        // We're fully ready to function.
        messengerEventListener = endpoint.addMessageTransport(this);

        if (messengerEventListener == null) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Transport registration refused");
            }
            return -1;
        }

        // Cannot start before registration, we could be announcing new messengers while we
        // do not exist yet ! (And get an NPE because we do not have the messenger listener set).

        if (unicastServer != null) {
            if (!unicastServer.start(myThreadGroup)) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Unable to start TCP Unicast Server");
                }
                return -1;
            }
        }

        if (allowMulticast) {
            try {
                multicastSocket.joinGroup(propagateInetAddress);
                multicastThread = new Thread(myThreadGroup, this, "TCP Multicast Server Listener");
                multicastProcessor = new MulticastProcessor();
                multicastThread.setDaemon(true);
                multicastThread.start();
            } catch (IOException soe) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Could not join multicast group, setting Multicast off");
                }
                allowMulticast = false;
                return -1;
            }
        }

        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            TransportServiceMonitor transportServiceMonitor = (TransportServiceMonitor) MonitorManager.getServiceMonitor(group,
                    MonitorResources.transportServiceMonitorClassID);

            if (transportServiceMonitor != null) {
                unicastTransportMeter = transportServiceMonitor.createTransportMeter("TCP", publicAddress);
            }
        }

        if (allowMulticast) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                TransportServiceMonitor transportServiceMonitor = (TransportServiceMonitor) MonitorManager.getServiceMonitor(group,
                        MonitorResources.transportServiceMonitorClassID);

                if (transportServiceMonitor != null) {
                    multicastTransportMeter = transportServiceMonitor.createTransportMeter("Multicast", mAddress);
                    multicastTransportBindingMeter = getMulticastTransportBindingMeter(mAddress);
                    multicastTransportBindingMeter.connectionEstablished(true, 0); // Since multicast is connectionless, force it to appear outbound connected
                    multicastTransportBindingMeter.connectionEstablished(false, 0); // Since multicast is connectionless, force it to appear inbound connected
                }
            }
        }

        isClosed = false;

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("TCP Message Transport started.");
        }
        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stopApp() {
        if (isClosed) {
            return;
        }

        isClosed = true;
        
        if (null != multicastProcessor) {
            multicastProcessor.stop();
            multicastProcessor = null;
        }
        
        if (unicastServer != null) {
            unicastServer.stop();
            unicastServer = null;
        }

        Thread temp = messengerSelectorThread;

        if (null != temp) {
            temp.interrupt();
            try {
                messengerSelector.close();
            } catch (IOException failed) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "IO error occured while closing server socket", failed);
                }
            }
        }

        if (multicastSocket != null) {
            multicastSocket.close();
            multicastSocket = null;
            multicastThread = null;
        }

        endpoint.removeMessageTransport(this);

        endpoint = null;
        group = null;

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info(MessageFormat.format("Total bytes sent : {0}", getBytesSent()));
            LOG.info(MessageFormat.format("Total Messages sent : {0}", getMessagesSent()));
            LOG.info(MessageFormat.format("Total bytes received : {0}", getBytesReceived()));
            LOG.info(MessageFormat.format("Total Messages received : {0}", getMessagesReceived()));
            LOG.info(MessageFormat.format("Total connections accepted : {0}", getConnectionsAccepted()));

            LOG.info("TCP Message Transport shut down.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getProtocolName() {
        return protocolName;
    }

    /**
     * {@inheritDoc}
     */
    public EndpointAddress getPublicAddress() {
        return publicAddress;
    }

    /**
     * {@inheritDoc}
     */
    public EndpointService getEndpointService() {
        return (EndpointService) endpoint.getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public Object transportControl(Object operation, Object Value) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<EndpointAddress> getPublicAddresses() {
        return Collections.unmodifiableList(publicAddresses).iterator();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isConnectionOriented() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsRouting() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Messenger getMessenger(EndpointAddress dst, Object hintIgnored) {

        EndpointAddress plainAddr = new EndpointAddress(dst, null, null);

        if (!plainAddr.getProtocolName().equals(getProtocolName())) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Cannot make messenger for protocol: " + plainAddr.getProtocolName());
            }
            return null;
        }

        // If the destination is one of our addresses including loopback, we 
        // return a loopback messenger.
        if (publicAddresses.contains(plainAddr)) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("return LoopbackMessenger for addr : " + dst);
            }
            return new LoopbackMessenger(endpoint, getPublicAddress(), dst,
                    new EndpointAddress("jxta", group.getPeerID().getUniqueValue().toString(), null, null));
        }
        
        try {
            // Right now we do not want to "announce" outgoing messengers because they get pooled and so must
            // not be grabbed by a listener. If "announcing" is to be done, that should be by the endpoint
            // and probably with a subtely different interface.
            return new TcpMessenger(dst, this);
        } catch (Exception caught) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Could not get messenger for " + dst, caught);
                } else {
                    LOG.warning("Could not get messenger for " + dst + " : " + caught.getMessage());
                }
            }

            if (caught instanceof RuntimeException) {
                throw (RuntimeException) caught;
            }

            return null;
        }
    }

    /**
     * Handles incoming multicasts.
     */
    public void run() {

        if (!allowMulticast) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Multicast disabled");
            }
            return;
        }

        try {

            byte[] buffer;

            while (true) {
                if (isClosed) {
                    return;
                }

                buffer = new byte[propagateSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    multicastSocket.receive(packet);
                    if (isClosed) {
                        return;
                    }
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("multicast message received from :" + packet.getAddress().getHostAddress());
                    }
                    multicastProcessor.put(packet);
                } catch (Exception e) {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE) && (!isClosed)) {
                        LOG.log(Level.SEVERE, "failure during multicast receive", e);
                    }
                    if (isClosed) {
                        return;
                    }
                    break;
                }
                packet = null;
            }
        } catch (Throwable all) {
            if (isClosed) {
                return;
            }
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Synchronized to not allow concurrent IP multicast: this
     * naturally bounds the usage of ip-multicast boolean be linear and not
     * exponential.
     */
    public synchronized boolean propagate(Message message, String pName, String pParams, int initalTTL) {
        if (!allowMulticast) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Multicast disabled, returning");
            }
            return false;
        }

        long sendStartTime = System.currentTimeMillis();
        int numBytesInPacket = 0;

        try {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                sendStartTime = System.currentTimeMillis();
            }

            message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, msgSrcAddrElement);

            // First build the destination and source addresses
            EndpointAddress destAddr = new EndpointAddress(mAddress, pName, pParams);
            MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME
                    ,
                    destAddr.toString(), null);

            message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);

            WireFormatMessage serialed = WireFormatMessageFactory.toWire(message, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null);
            MessagePackageHeader header = new MessagePackageHeader();

            header.setContentTypeHeader(serialed.getMimeType());
            header.setContentLengthHeader(serialed.getByteLength());

            try {
                header.replaceHeader("srcEA", getPublicAddress().toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException never) {
                // utf-8 is a required encoding.
                throw new IllegalStateException("utf-8 encoding support missing!");
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream(multicastPacketSize);

            buffer.write('J');
            buffer.write('X');
            buffer.write('T');
            buffer.write('A');
            header.sendToStream(buffer);
            serialed.sendToStream(buffer);
            buffer.flush();
            buffer.close();
            numBytesInPacket = buffer.size();

            DatagramPacket packet = new DatagramPacket(buffer.toByteArray(), numBytesInPacket, propagateInetAddress, propagatePort);

            multicastSocket.send(packet);
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sent Multicast message to :" + pName + "/" + pParams);
            }
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                multicastTransportBindingMeter.messageSent(true, message, System.currentTimeMillis() - sendStartTime
                        ,
                        numBytesInPacket);
            }
            return true;
        } catch (IOException e) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                multicastTransportBindingMeter.sendFailure(true, message, System.currentTimeMillis() - sendStartTime,
                        numBytesInPacket);
            }
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Multicast socket send failed", e);
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>This implementation tries to open a connection, and after tests the
     * result.
     */
    public boolean ping(EndpointAddress addr) {
        boolean result = false;
        EndpointAddress endpointAddress;
        long pingStartTime = 0;

        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            pingStartTime = System.currentTimeMillis();
        }

        endpointAddress = new EndpointAddress(addr, null, null);

        try {
            // Too bad that this one will not get pooled. On the other hand ping is
            // not here too stay.
            TcpMessenger tcpMessenger = new TcpMessenger(endpointAddress, this);

            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                TransportBindingMeter transportBindingMeter = tcpMessenger.getTransportBindingMeter();

                if (transportBindingMeter != null) {
                    transportBindingMeter.ping(System.currentTimeMillis() - pingStartTime);
                }
            }
            result = true;
        } catch (Throwable e) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "failure pinging " + addr.toString(), e);
            }
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                TransportBindingMeter transportBindingMeter = getUnicastTransportBindingMeter(null, endpointAddress);

                if (transportBindingMeter != null) {
                    transportBindingMeter.pingFailed(System.currentTimeMillis() - pingStartTime);
                }
            }
        }

        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("ping to " + addr.toString() + " == " + result);
        }
        return result;
    }

    /**
     * Getter for property 'restrictionPort'.
     *
     * @return Value for property 'restrictionPort'.
     */
    int getRestrictionPort() {
        return restrictionPort;
    }

    TransportBindingMeter getUnicastTransportBindingMeter(PeerID peerID, EndpointAddress destinationAddress) {
        if (unicastTransportMeter != null) {
            return unicastTransportMeter.getTransportBindingMeter(
                    (peerID != null) ? peerID.toString() : TransportMeter.UNKNOWN_PEER, destinationAddress);
        } else {
            return null;
        }
    }

    TransportBindingMeter getMulticastTransportBindingMeter(EndpointAddress destinationAddress) {
        if (multicastTransportMeter != null) {
            return multicastTransportMeter.getTransportBindingMeter(group.getPeerID(), destinationAddress);
        } else {
            return null;
        }
    }

    void messengerReadyEvent(Messenger newMessenger, EndpointAddress connAddr) {
        messengerEventListener.messengerReady(new MessengerEvent(this, newMessenger, connAddr));
    }

    /**
     * Getter for property 'server'.
     *
     * @return Value for property 'server'.
     */
    IncomingUnicastServer getServer() {
        return unicastServer;

    }

    /**
     * Getter for property 'selector'.
     *
     * @return Value for property 'selector'.
     * @throws InterruptedException if interrupted
     */
    public Selector getSelector() throws InterruptedException {
        synchronized (writeSelectorCache) {
            Selector selector = null;

            try {
                if (!writeSelectorCache.isEmpty()) {
                    selector = writeSelectorCache.pop();
                }
            } catch (EmptyStackException ese) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("No write selector available, waiting for one");
                }
            }

            int attempts = 0;

            while (selector == null && attempts < 2) {
                writeSelectorCache.wait(connectionTimeOut);
                try {
                    if (!writeSelectorCache.isEmpty()) {
                        selector = writeSelectorCache.pop();
                    }
                } catch (EmptyStackException ese) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Failed to get a write selector available, waiting for one", ese);
                    }
                }
                attempts++;
            }
            return selector;
        }
    }

    /**
     * Return the <code>Selector</code> to the cache
     *
     * @param selector the selector to put back into the pool
     */
    public void returnSelector(Selector selector) {
        synchronized (writeSelectorCache) {
            writeSelectorCache.push(selector);
            // it does not hurt to notify, even if there are no waiters
            writeSelectorCache.notify();
        }
    }

    /**
     * Waits for incoming data on channels and sends it to the appropriate
     * messenger object.
     */
    private class MessengerSelectorThread implements Runnable {

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                    LOG.info("MessengerSelectorThread polling started");
                }

                while (!isClosed) {
                    try {
                        int selectedKeys = 0;

                        // Update channel registerations.
                        updateChannelRegisterations();

                        try {
                            // this can be interrupted through wakeup
                            selectedKeys = messengerSelector.select();
                        } catch (CancelledKeyException cke) {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.log(Level.FINE, "Key was cancelled", cke);
                            }
                        }

                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine(MessageFormat.format("MessengerSelector has {0} selected keys", selectedKeys));
                        }

                        if (selectedKeys == 0 && messengerSelector.selectNow() == 0) {
                            // We were probably just woken.
                            continue;
                        }

                        Set<SelectionKey> keySet = messengerSelector.selectedKeys();

                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine(MessageFormat.format("KeySet has {0} selected keys", keySet.size()));
                        }

                        Iterator<SelectionKey> it = keySet.iterator();

                        while (it.hasNext()) {
                            SelectionKey key = it.next();

                            // remove it from the SelectedKeys Set
                            it.remove();

                            if (key.isValid()) {
                                if (key.isReadable() && key.channel().isOpen()) {
                                    // ensure this channel is not selected again until the thread is done with it
                                    // TcpMessenger is expected to reset the interestOps back to OP_READ
                                    // Without this, expect multiple threads to execute on the same event, until
                                    // the first thread completes reading all data available
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);

                                    // get the messenger
                                    TcpMessenger msgr = (TcpMessenger) key.attachment();

                                    // process the data
                                    try {
                                        executor.execute(msgr);
                                    } catch (RejectedExecutionException re) {
                                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                            LOG.log(Level.FINE,
                                                    MessageFormat.format("Executor rejected task for messenger :{0}",
                                                    msgr.toString()), re);
                                        }
                                    }
                                }
                            } else {
                                // unregister it, no need to keep invalid/closed channels around
                                key.channel().close();
                                key.cancel();
                                key = null;
                            }
                        }
                    } catch (ClosedSelectorException cse) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("IO Selector closed");
                        }
                    } catch (InterruptedIOException woken) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Thread inturrupted", woken);
                        }
                    } catch (IOException e1) {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "An exception occurred while selecting keys", e1);
                        }
                    } catch (SecurityException e2) {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "A security exception occurred while selecting keys", e2);
                        }
                    }
                }

                // XXX 20070205 bondolo What should we do about the channels 
                // that are still registered with the selector and any pending 
                // updates?

            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE && Logging.SHOW_SEVERE) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable", all);
                }
            } finally {
                messengerSelectorThread = null;
            }
        }
    }

    /**
     * Registers the channel with the Read selector and attaches the messenger to the channel
     *
     * @param channel   the socket channel.
     * @param messenger the messenger to attach to the channel.
     */
    public void register(SocketChannel channel, TcpMessenger messenger) {
        regisMap.put(messenger, channel);
        messengerSelector.wakeup();
    }

    /**
     * Unregisters the channel with the Read selector
     *
     * @param channel the socket channel.
     */
    public void unregister(SocketChannel channel) {
        unregisMap.add(channel);
        messengerSelector.wakeup();
    }

    /**
     * Registers all newly accepted and returned (by TcpMessenger) channels.
     * Removes all closing TcpMessengers.
     */
    private synchronized void updateChannelRegisterations() {

        if (!regisMap.isEmpty() && Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine(MessageFormat.format("Registering {0} channels with MessengerSelectorThread", regisMap.size()));
        }

        if (!regisMap.isEmpty()) {
            Iterator<Map.Entry<TcpMessenger, SocketChannel>> eachMsgr = regisMap.entrySet().iterator();

            while (eachMsgr.hasNext()) {
                Map.Entry<TcpMessenger, SocketChannel> anEntry = eachMsgr.next();
                TcpMessenger msgr = anEntry.getKey();
                SocketChannel channel = anEntry.getValue();

                SelectionKey key = channel.keyFor(messengerSelector);

                try {
                    if (key == null) {
                        key = channel.register(messengerSelector, SelectionKey.OP_READ, msgr);
                    }
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                    if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                        LOG.finer(MessageFormat.format("Key interestOps on channel {0}, bit set :{1}", channel, key.interestOps()));
                    }
                } catch (ClosedChannelException e) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Failed to register Channel with messenger selector", e);
                    }
                    // it's best a new messenger is created when a new messenger is requested
                    msgr.close();
                } catch (CancelledKeyException e) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Key is already cancelled, removing key from registeration map", e);
                    }
                } catch (IllegalBlockingModeException e) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Invalid blocking channel mode, closing messenger", e);
                    }
                    // messenger state is unknown
                    msgr.close();
                }
                // remove it from the table
                eachMsgr.remove();
            }
        }

        // Unregister and close channels.
        if (!unregisMap.isEmpty() && Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine(MessageFormat.format("Unregistering {0} channels with MessengerSelectorThread", unregisMap.size()));
        }
        if (!unregisMap.isEmpty()) {
            Iterator<SocketChannel> eachChannel;

            synchronized (unregisMap) {
                List<SocketChannel> allChannels = new ArrayList<SocketChannel>(unregisMap);
                unregisMap.clear();
                eachChannel = allChannels.iterator();
            }

            while (eachChannel.hasNext()) {
                SocketChannel aChannel = eachChannel.next();
                SelectionKey key = aChannel.keyFor(messengerSelector);

                if (null != key) {
                    try {
                        key.cancel();
                    } catch (CancelledKeyException e) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Key is already cancelled, removing key from registeration map", e);
                        }
                    }
                }
            }
        }
        executor.purge();
    }

    /**
     * a class with it's own threadpool dedicated to processing datagrams
     * Note: could use the group threadpool instead, chose not to as it may introduce unfairness in a storm
     *
     */
    private class MulticastProcessor implements Runnable {

        private ThreadPoolExecutor threadPool;
        private final static int MAXPOOLSIZE = 5;
        private final ArrayBlockingQueue<DatagramPacket> queue = new ArrayBlockingQueue<DatagramPacket>(MAXPOOLSIZE * 2);

        /**
         * Default constructor
         */
        MulticastProcessor() {
            this.threadPool = new ThreadPoolExecutor(2, MAXPOOLSIZE,
                                                     20, TimeUnit.SECONDS,
                                                     new ArrayBlockingQueue<Runnable>(MAXPOOLSIZE * 2));
            threadPool.setRejectedExecutionHandler(new CallerBlocksPolicy(MAXPOOLSIZE));
        }

        /**
         * Stops this thread
         */
        public void stop() {
            threadPool.shutdownNow();
            queue.clear();
        }

        /**
         * Puts a datagram on the queue
         *
         * @param packet the datagram
         */
        void put(DatagramPacket packet) {
            try {
                queue.put(packet);
                threadPool.execute(this);
            } catch (InterruptedException e) {
                if (Logging.SHOW_SEVERE) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable", e);
                }
            }
        }

        /**
         * Handle a byte buffer from a multi-cast. This assumes that processing of
         * the buffer is lightweight. Formerly there used to be a delegation to
         * worker threads. The way queuing works has changed though and it should
         * be ok to do the receiver right on the server thread.
         *
         * @param packet the message packet.
         */
        public void processMulticast(DatagramPacket packet) {
            if (!allowMulticast) {
                return;
            }
            int size = packet.getLength();
            byte[] buffer = packet.getData();

            long messageReceiveBeginTime = System.currentTimeMillis();

            try {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                    messageReceiveBeginTime = System.currentTimeMillis();
                }
                if (size < 4) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("damaged multicast discarded");
                    }
                    return;
                }

                if (('J' != buffer[0]) || ('X' != buffer[1]) || ('T' != buffer[2]) || ('A' != buffer[3])) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("damaged multicast discarded");
                    }
                    return;
                }

                ByteBuffer bbuffer = ByteBuffer.wrap(buffer, 4, size - 4);
                MessagePackageHeader header = new MessagePackageHeader();

                if (header.readHeader(bbuffer)) {
                    MimeMediaType msgMime = header.getContentTypeHeader();
                    // TODO 20020730 bondolo@jxta.org Do something with content-coding here.

                    // read the message!
                    Message msg = WireFormatMessageFactory.fromBuffer(bbuffer, msgMime, null);

                    // Give the message to the EndpointService Manager
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("handing multicast message to EndpointService");
                    }

                    if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                        multicastTransportBindingMeter.messageReceived(false, msg,
                                messageReceiveBeginTime - System.currentTimeMillis(), size);
                    }
                    // Demux the message for the upper layers.
                    endpoint.demux(msg);
                }
            } catch (Throwable e) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                    multicastTransportBindingMeter.receiveFailure(false, messageReceiveBeginTime - System.currentTimeMillis(), size);
                }
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "processMulticast : discard incoming multicast message - exception ", e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                DatagramPacket packet = queue.take();
                processMulticast(packet);
            } catch (InterruptedException ie) {
                // we are being interrupted to stop
            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable", all);
                }
            }
        }
    }


    /**
     * This is copied from StdPeerGroup.  it should be factored out in a utility class and used by both StdPeerGroup and
     * this class.
     */
    private static class CallerBlocksPolicy implements RejectedExecutionHandler {
        int max;

        CallerBlocksPolicy(int max) {
            this.max = max;
        }

        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            BlockingQueue<Runnable> queue = executor.getQueue();

            while (!executor.isShutdown()) {
                boolean pushed;

                try {
                    executor.purge();
                    pushed = queue.offer(runnable, 3, TimeUnit.SECONDS);
                    if (pushed) {
                        break;
                    }
                } catch (InterruptedException woken) {
                    // This is our entire handling of interruption. If the
                    // interruption signaled a state change of the executor our
                    // while() loop condition will handle termination.
                    Thread.interrupted();
                    continue;
                }

                // Couldn't push? Add a thread!
                synchronized (executor) {
                    int currentMax = executor.getMaximumPoolSize();
                    int newMax = Math.min(currentMax + 1, max * 2);

                    if (newMax != currentMax) {
                        executor.setMaximumPoolSize(newMax);
                    }

                    // If we are already at the max, increase the core size
                    if (newMax == (max * 2)) {
                        int currentCore = executor.getCorePoolSize();
                        int newCore = Math.min(currentCore + 1, max * 2);

                        if (currentCore != newCore) {
                            executor.setCorePoolSize(newCore);
                        } else {
                            // Core size is at the max too. We just have to wait.
                            continue;
                        }
                    }
                    // Should work now.
                    executor.execute(runnable);
                    break;
                }
            }
        }
    }
}
