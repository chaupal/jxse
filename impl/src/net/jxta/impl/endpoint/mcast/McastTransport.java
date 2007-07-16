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
package net.jxta.impl.endpoint.mcast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attribute;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessagePropagater;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.meter.MonitorResources;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.TransportAdvertisement;

import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.endpoint.transportMeter.TransportServiceMonitor;
import net.jxta.impl.meter.MonitorManager;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;

/**
 * This class implements the IP Multicast Message Transport
 *
 * @see net.jxta.endpoint.MessageTransport
 * @see net.jxta.endpoint.MessagePropagater
 * @see net.jxta.endpoint.EndpointService
 * @see <a href="http://spec.jxta.org/v1.0/docbook/JXTAProtocols.html#trans-tcpipt">JXTA Protocols Specification : Standard JXTA Transport Bindings</a>
 */
public class McastTransport implements Runnable, Module, MessagePropagater {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(McastTransport.class.getName());
    
    /**
     * Well known service class identifier: mcast message transport
     */
    public final static ModuleClassID MCAST_TRANSPORT_CLASSID = 
            ModuleClassID.create(URI.create("urn:jxta:uuid-0C801F65D38F421C9884D706B337B81105"));
    
    /**
     * Well known service spec identifier: mcast message transport
     */
    public final static ModuleSpecID MCAST_TRANSPORT_SPECID = 
            ModuleSpecID.create(URI.create("urn:jxta:uuid-0C801F65D38F421C9884D706B337B8110106"));
    
    /**
     *  The Protocol name we will use for our endpoint addresses.
     */
    private String protocolName = "mcast";
    
    /**
     *  The Source Address Element we attach to all of the messages we send.
     */
    private EndpointAddress msgSrcAddr = null;
    
    /**
     *  The Source Address Element we attach to all of the messages we send.
     */
    private MessageElement msgSrcAddrElement = null;
    
    /**
     *  The name of the  local interface that we bind to.
     */
    private String interfaceAddressStr;
    
    /**
     *  The address of the local interface address that be bind to.
     */
    private InetAddress usingInterface;

    /**
     *  If {@code true} then we are closed otherwise {@code false}
     */
    private boolean isClosed = false;

    /**
     *  The name of multicast address we will send/receive upon.
     */
    private String multicastAddress = "224.0.1.85";
    
    /**
     *  The multicast address we will send/receive upon.
     */
    private InetAddress multicastInetAddress;
    
    /**
     *  The "return address" we will advertise.
     */
    private EndpointAddress publicAddress = null;
    
    /**
     *  The port number will send/receive upon.
     */
    private int multicastPort = 1234;
    
    /**
     *  The maximum size of multicast messages we will send and the size of the 
     *  {@code DatagramPacket}s we will allocate.
     */
    private int multicastPacketSize = 16384;
    
    /**
     *  The socket we use to send and receive.
     */
    private MulticastSocket multicastSocket = null;

    /**
     *  Daemon thread which services the multicast socket and receives datagrams.
     */
    private Thread multicastThread = null;

    /**
     *  Thread pooling/queing multicast datagram processor.
     */
    private MulticastProcessor multicastProcessor;

    /**
     *  The peer group we are working for.
     */
    private PeerGroup group = null;
    
    /**
     *  The endpoint service we are working for.
     */
    private EndpointService endpoint = null;

    private TransportMeter multicastTransportMeter;
    private TransportBindingMeter multicastTransportBindingMeter;

    /**
     * This is the thread group into which we will place all of the threads
     * we create. THIS HAS NO EFFECT ON SCHEDULING. Java thread groups are
     * only for organization and naming.
     */
    ThreadGroup myThreadGroup = null;

    /**
     * Construct a new McastTransport instance
     */
    public McastTransport() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object target) {
        if (this == target) {
            return true;
        }

        if (target instanceof McastTransport) {
            McastTransport likeMe = (McastTransport) target;

            if (!getProtocolName().equals(likeMe.getProtocolName())) {
                return false;
            }

            return getPublicAddress().equals(likeMe.getPublicAddress());
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
     * {@inheritDoc}
     */
    public void init(PeerGroup group, ID assignedID, Advertisement impl) throws PeerGroupException {

        this.group = group;
        ModuleImplAdvertisement implAdvertisement = (ModuleImplAdvertisement) impl;

        try {
            ConfigParams configAdv = group.getConfigAdvertisement();

            // Get out invariable parameters from the implAdv
            XMLElement param = (XMLElement) implAdvertisement.getParam();

            if (param != null) {
                Enumeration<XMLElement> list = param.getChildren("Proto");

                if (list.hasMoreElements()) {
                    XMLElement pname = list.nextElement();
                    protocolName = pname.getTextValue();
                }
            }

            // Get our peer-defined parameters in the configAdv
            param = (XMLElement) configAdv.getServiceParam(PeerGroup.tcpProtoClassID);
            Enumeration<XMLElement> tcpChilds = param.getChildren(TransportAdvertisement.getAdvertisementType());

            // get the TransportAdv
            if (tcpChilds.hasMoreElements()) {
                param = tcpChilds.nextElement();
                Attribute typeAttr = param.getAttribute("type");

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
                paramsAdv = AdvertisementFactory.newAdvertisement(param);
            } catch (NoSuchElementException notThere) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Could not find parameter document", notThere);
                }
            }

            if (!(paramsAdv instanceof TCPAdv)) {
                throw new IllegalArgumentException("Provided Advertisement was not a " + TCPAdv.getAdvertisementType());
            }

            TCPAdv adv = (TCPAdv) paramsAdv;
            
            if(!adv.getMulticastState()) {
                throw new PeerGroupException( "IP Multicast Message Transport is disabled.");
            }

            // determine the local interface to use. If the user specifies one,
            // use that. Otherwise, use the all the available interfaces.
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

            // Start the servers
            myThreadGroup = new ThreadGroup(group.getHomeThreadGroup(), "MCastTransport " + usingInterface.getHostAddress());

            // Only the outgoing interface matters.
            // Verify that ANY interface does not in fact mean LOOPBACK only.
            // If that's the case, we want to make that explicit, so that 
            // consistency checks regarding the allowed use of that 
            // interface work properly.
            if (usingInterface.equals(IPUtils.ANYADDRESS)) {
                boolean localOnly = true;
                Iterator<InetAddress> eachLocal = IPUtils.getAllLocalAddresses();

                while (eachLocal.hasNext()) {
                    InetAddress anAddress = eachLocal.next();

                    if (!anAddress.isLoopbackAddress()) {
                        localOnly = false;
                        break;
                    }
                }

                if (localOnly) {
                    usingInterface = IPUtils.LOOPBACK;
                }
            }

            msgSrcAddr = new EndpointAddress(group.getPeerID(), null, null);
            msgSrcAddrElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, msgSrcAddr.toString(), null);

            // Get the multicast configuration.
            multicastAddress = adv.getMulticastAddr();
            multicastPort = adv.getMulticastPort();
            
            // XXX 20070711 bondolo We resolve the address only once. Perhaps we should do this dynamically?
            multicastInetAddress = InetAddress.getByName(multicastAddress);
            
            assert multicastInetAddress.isMulticastAddress();

            publicAddress = new EndpointAddress(protocolName, multicastAddress + ":" + Integer.toString(multicastPort), null, null);

            multicastPacketSize = adv.getMulticastSize();

            // Create the multicast input socket
            multicastSocket = new MulticastSocket(new InetSocketAddress(usingInterface, multicastPort));
            try {
                multicastSocket.setLoopbackMode(false);
            } catch (SocketException ignored) {// We may not be able to set loopback mode. It is
                // inconsistent whether an error will occur if the set fails.
            }
        } catch (Exception e) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Initialization exception", e);
            }

            throw new PeerGroupException("Initialization exception", e);
        }

        // Tell tell the world about our configuration.
        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            StringBuilder configInfo = new StringBuilder("Configuring IP Multicast Message Transport : " + assignedID);

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
            configInfo.append("\n\t\tInterface address: ").append(
                    interfaceAddressStr == null ? "(unspecified)" : interfaceAddressStr);
            configInfo.append("\n\t\tMulticast Addr: ").append(multicastAddress);
            configInfo.append("\n\t\tMulticast Port: ").append(multicastPort);
            configInfo.append("\n\t\tMulticast Packet Size: ").append(multicastPacketSize);

            configInfo.append("\n\tBound To :");
            configInfo.append("\n\t\tUsing Interface: ").append(usingInterface.getHostAddress());

            configInfo.append("\n\t\tMulticast Server Bind Addr: ").append(multicastSocket.getLocalSocketAddress());
            configInfo.append("\n\t\tPublic Address: ").append(publicAddress);

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

        isClosed = false;

        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            TransportServiceMonitor transportServiceMonitor = (TransportServiceMonitor) MonitorManager.getServiceMonitor(group,
                    MonitorResources.transportServiceMonitorClassID);

            if (transportServiceMonitor != null) {
                multicastTransportMeter = transportServiceMonitor.createTransportMeter("Multicast", publicAddress);
                multicastTransportBindingMeter = getMulticastTransportBindingMeter(publicAddress);
                multicastTransportBindingMeter.connectionEstablished(true, 0); // Since multicast is connectionless, force it to appear outbound connected
                multicastTransportBindingMeter.connectionEstablished(false, 0); // Since multicast is connectionless, force it to appear inbound connected
            }
        }

        // We're fully ready to function.
        MessengerEventListener messengerEventListener = endpoint.addMessageTransport(this);

        if (messengerEventListener == null) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Transport registration refused");
            }
            return -1;
        }

        // Cannot start before registration, we could be announcing new messengers while we
        // do not exist yet ! (And get an NPE because we do not have the messenger listener set).

         try {
            multicastSocket.joinGroup(multicastInetAddress);
            multicastThread = new Thread(myThreadGroup, this, "TCP Multicast Server Listener");
            multicastProcessor = new MulticastProcessor();
            multicastThread.setDaemon(true);
            multicastThread.start();
        } catch (IOException soe) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Could not join multicast group, setting Multicast off");
            }
            return -1;
        }

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("IP Multicast Message Transport started.");
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
        
        if (multicastSocket != null) {
            multicastSocket.close();
            multicastSocket = null;
            multicastThread = null;
        }

        endpoint.removeMessageTransport(this);

        endpoint = null;
        group = null;
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
     * <p/>
     * Handles incoming multicasts.
     */
    public void run() {

        try {
            while(!isClosed) {
                byte[] buffer = new byte[multicastPacketSize];
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
                } catch( InterruptedIOException woken ) {
                    Thread.interrupted();
                }  catch (Exception e) {
                    if (isClosed) {
                        return;
                    }
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE) && (!isClosed)) {
                        LOG.log(Level.SEVERE, "failure during multicast receive", e);
                    }
                    break;
                }               
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
     * Synchronized to not allow concurrent IP multicast: this naturally bounds
     * the usage of ip-multicast boolean be linear and not exponential.
     */
    public synchronized boolean propagate(Message message, String pName, String pParams, int initalTTL) {
        long sendStartTime = System.currentTimeMillis();
        int numBytesInPacket = 0;

        try {
            message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, msgSrcAddrElement);

            // First build the destination and source addresses
            EndpointAddress destAddr = new EndpointAddress(publicAddress, pName, pParams);
            MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddr.toString(), null);

            message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);

            WireFormatMessage serialed = WireFormatMessageFactory.toWire(message, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null);
            MessagePackageHeader header = new MessagePackageHeader();

            header.setContentTypeHeader(serialed.getMimeType());
            header.setContentLengthHeader(serialed.getByteLength());
            header.replaceHeader("srcEA", msgSrcAddr.toString());
            
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
            
            if ((buffer.size() > multicastPacketSize) && Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Multicast datagram exceeds multicast size." );
            }

            DatagramPacket packet = new DatagramPacket(buffer.toByteArray(), numBytesInPacket, multicastInetAddress, multicastPort);

            multicastSocket.send(packet);
            
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sent Multicast message to :" + pName + "/" + pParams);
            }
            
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                multicastTransportBindingMeter.messageSent(true, message, System.currentTimeMillis() - sendStartTime, numBytesInPacket);
            }
            return true;
        } catch (IOException e) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                multicastTransportBindingMeter.sendFailure(true, message, System.currentTimeMillis() - sendStartTime, numBytesInPacket);
            }
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Multicast socket send failed", e);
            }
            return false;
        }
    }

    TransportBindingMeter getMulticastTransportBindingMeter(EndpointAddress destinationAddress) {
        if (multicastTransportMeter != null) {
            return multicastTransportMeter.getTransportBindingMeter(group.getPeerID(), destinationAddress);
        } else {
            return null;
        }
    }

    /**
     * a class with it's own threadpool dedicated to processing datagrams
     * Note: could use the group threadpool instead, chose not to as it may introduce unfairness in a storm
     *
     */
    private class MulticastProcessor implements Runnable {

        private final ThreadPoolExecutor threadPool;
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

                    throw new IOException("damaged multicast discarded : too short");
                }

                if (('J' != buffer[0]) || ('X' != buffer[1]) || ('T' != buffer[2]) || ('A' != buffer[3])) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("damaged multicast discarded");
                    }

                    throw new IOException("damaged multicast discarded : incorrect signature");
                }

                ByteBuffer bbuffer = ByteBuffer.wrap(buffer, 4, size - 4);
                MessagePackageHeader header = new MessagePackageHeader();

                if (!header.readHeader(bbuffer)) {
                    throw new IOException("Failed to read framing header");
                }

                Iterator<MessagePackageHeader.Header> eachSrcEA = header.getHeader("srcEA");

                if (!eachSrcEA.hasNext()) {
                    throw new IOException("No Source Address");
                }

                EndpointAddress srcAddr = new EndpointAddress(eachSrcEA.next().getValueString());

                if (srcAddr.equals(msgSrcAddr)) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Discarding loopback ");
                    }
                    return;
                }

                MimeMediaType msgMime = header.getContentTypeHeader();
                // TODO 20020730 bondolo@jxta.org Do something with content-coding here.

                // read the message!
                Message msg = WireFormatMessageFactory.fromBuffer(bbuffer, msgMime, null);

                MessageElement dstAddrElem = msg.getMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, EndpointServiceImpl.MESSAGE_DESTINATION_NAME);
                if (null == dstAddrElem) {
                    throw new IOException("No Destination Address in " + msg);
                }

                EndpointAddress dstAddr = new EndpointAddress(dstAddrElem.toString());

                // Handoff the message to the EndpointService Manager
                endpoint.processIncomingMessage(msg, srcAddr, dstAddr);

                if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                    multicastTransportBindingMeter.messageReceived(false, msg, messageReceiveBeginTime - System.currentTimeMillis(), size);
                }
            } catch (Throwable e) {
                if (TransportMeterBuildSettings.TRANSPORT_METERING && (multicastTransportBindingMeter != null)) {
                    multicastTransportBindingMeter.receiveFailure(false, messageReceiveBeginTime - System.currentTimeMillis(), size);
                }

                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "discard incoming multicast message - exception ", e);
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
        
        /**
         *  The target maximum pool size. We will only exceed this amount if we
         *  are failing to make progress.
         */
        private final int MAXPOOLSIZE;
        
        private CallerBlocksPolicy(int maxPoolSize) {
            MAXPOOLSIZE = maxPoolSize;
        }
        
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            BlockingQueue<Runnable> queue = executor.getQueue();

            while (!executor.isShutdown()) {
                executor.purge();

                try {
                    boolean pushed = queue.offer(runnable, 500, TimeUnit.MILLISECONDS);
                    
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
                    int newMax = Math.min(currentMax + 1, MAXPOOLSIZE * 2);
                    
                    if (newMax != currentMax) {
                        executor.setMaximumPoolSize(newMax);
                    }
                    
                    // If we are already at the max, increase the core size
                    if (newMax == (MAXPOOLSIZE * 2)) {
                        int currentCore = executor.getCorePoolSize();
                        
                        int newCore = Math.min(currentCore + 1, MAXPOOLSIZE * 2);
                        
                        if (currentCore != newCore) {
                            executor.setCorePoolSize(newCore);
                        } else {
                            // Core size is at the max too. We just have to wait.
                            continue;
                        }
                    }
                }
                
                // Should work now.
                executor.execute(runnable);
                break;
            }
        }
    }
}
