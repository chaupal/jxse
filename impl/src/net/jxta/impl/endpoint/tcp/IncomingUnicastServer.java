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

import net.jxta.logging.Logging;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.impl.endpoint.IPUtils;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;

/**
 * This server handles incoming unicast TCP connections
 */
public class IncomingUnicastServer implements Runnable {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(IncomingUnicastServer.class.getName());
    /**
     * The transport which owns this server.
     */
    private final TcpTransport transport;
    /**
     * The interface address the serverSocket will try to bind to.
     */
    private final InetAddress serverBindLocalInterface;
    /**
     * The beginning of the port range the serverSocket will try to bind to.
     */
    private final int serverBindStartLocalPort;
    /**
     * The port the serverSocket will try to bind to.
     */
    private int serverBindPreferredLocalPort;
    /**
     * The end of the port range the serverSocket will try to bind to.
     */
    private final int serverBindEndLocalPort;
    /**
     * The thread on which connection accepts will take place.
     */
    private Thread acceptThread = null;
    /**
     * Channel Selector
     */
    private final Selector acceptSelector;
    /**
     *  The Server Socket Channel we have opened to receive connections.
     */
    private ServerSocketChannel serverSocChannel = null;
    /**
     *  The server socket associated with the channel.
     */
    private ServerSocket serverSocket = null;

    /**
     * Constructor for the TCP server
     *
     * @param owner The message transport we are working for.
     * @param serverInterface The network interface to use.
     * @param preferredPort The port we will be listening on, 0 or -1 if we have no current preference.
     * @param startPort The lowest port # in the range we will try or -1 if range is disabled.
     * @param endPort The highest port # in the range we will try or -1 if range is disabled.
     * @throws IOException Thrown if the server socket cannot be opened.
     * @throws SecurityException Thrown if the server socket cannot be oppened due to a security restriction.
     */
    public IncomingUnicastServer(TcpTransport owner, InetAddress serverInterface, int preferredPort, int startPort, int endPort) throws IOException, SecurityException {
        this.transport = owner;
        serverBindLocalInterface = serverInterface;
        serverBindPreferredLocalPort = preferredPort;
        serverBindStartLocalPort = startPort;
        serverBindEndLocalPort = endPort;

        acceptSelector = SelectorProvider.provider().openSelector();
        serverSocChannel = openServerSocket(acceptSelector);
        serverSocket = serverSocChannel.socket();
        serverBindPreferredLocalPort = serverSocket.getLocalPort();
    }

    /**
     * Start this server.
     *
     * @return {@code true} if successfully started otherwise {@code false} if 
     * this server cannot be started.
     */
    public synchronized boolean start() {
        if (!acceptSelector.isOpen()) {
            return false;
        }

        if (acceptThread != null) {
            return false;
        }

        // Start daemon thread
        acceptThread = new Thread(transport.group.getHomeThreadGroup(), this, "ServerSocketChannel acceptor for " + getLocalSocketAddress());
        acceptThread.setDaemon(true);
        acceptThread.start();

        return true;
    }

    /**
     * Stop this server. The server cannot be restarted after it has been
     * stopped.
     */
    public synchronized void stop() {
        Thread temp = acceptThread;

        if (null != temp) {
            // interrupt does not seem to have an effect on threads blocked in accept.
            temp.interrupt();
        }

        try {
            acceptSelector.close();
        } catch (IOException io) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "IO error occured while closing Selectors", io);
            }
        }
    }

    /**
     * Get the address of the network interface being used.
     *
     * @return the local socket address
     */
    InetSocketAddress getLocalSocketAddress() {
        ServerSocket localSocket = serverSocket;

        if (null != localSocket) {
            return (InetSocketAddress) localSocket.getLocalSocketAddress();
        } else {
            return null;
        }
    }

    /**
     * Get the start port range we are using
     *
     * @return starting port range
     */
    int getStartPort() {
        return serverBindStartLocalPort;
    }

    /**
     * Get the end port range we are using
     *
     * @return the ending port range
     */
    int getEndPort() {
        return serverBindEndLocalPort;
    }

    /**
     * Daemon where we wait for incoming connections.
     */
    public void run() {

        try {
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Server is ready to accept connections. " + transport.getPublicAddress());
            }

            while (acceptSelector.isOpen()) {
                try {
                    // Open the channel if not already open.
                    if ((null == serverSocChannel) || !serverSocChannel.isOpen()) {
                        serverSocChannel = null;
                        serverSocket = null;

                        if (null == (serverSocChannel = openServerSocket(acceptSelector))) {
                            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                                LOG.warning("Failed to open Server Channel");
                            }
                            break;
                        }
                        serverSocket = serverSocChannel.socket();
                        serverBindPreferredLocalPort = serverSocket.getLocalPort();
                    }

                    // select() waiting for connections.
                    acceptSelector.select();
                    Iterator<SelectionKey> it = acceptSelector.selectedKeys().iterator();

                    while (it.hasNext()) {
                        SelectionKey key = it.next();

                        // remove it
                        it.remove();
                        if (key.isAcceptable()) {
                            ServerSocketChannel nextReady = (ServerSocketChannel) key.channel();
                            SocketChannel inputSocket = nextReady.accept();

                            if (inputSocket == null) {
                                continue;
                            }

                            MessengerBuilder builder = new MessengerBuilder(transport, inputSocket);

                            try {
                                transport.executor.execute(builder);
                                transport.incrementConnectionsAccepted();
                            } catch (RejectedExecutionException re) {
                                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                    LOG.log(Level.FINE, MessageFormat.format("Executor rejected task : {0}", builder.toString()), re);
                                }
                            }
                        }
                    }
                } catch (ClosedSelectorException cse) {
                    break;
                } catch (InterruptedIOException woken) {
                    Thread.interrupted();
                } catch (IOException e1) {
                    if (!acceptSelector.isOpen()) {
                        break;
                    }
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "[1] ServerSocket.accept() failed on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort(), e1);
                    }
                } catch (SecurityException e2) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "[2] ServerSocket.accept() failed on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort(), e2);
                    }

                    break;
                }
            }
        } catch (Throwable all) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        } finally {
            synchronized (this) {
                ServerSocketChannel temp = serverSocChannel;
                serverSocChannel = null;

                if (null != temp) {
                    try {
                        temp.close();
                    } catch (IOException ignored) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Exception occurred while closing server socket", ignored);
                        }
                    }
                }
                acceptThread = null;
            }

            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Server has been shut down. " + transport.getPublicAddress());
            }
        }
    }

    private synchronized ServerSocketChannel openServerSocket(Selector registerSelector) throws IOException, SecurityException {
        ServerSocketChannel newChannel = ServerSocketChannel.open();

        while (true) {
            InetSocketAddress bindAddress;

            if ((-1 != serverBindPreferredLocalPort) && (0 != serverBindPreferredLocalPort)) {
                // Try to bind to our preferred port if we have one.
                try {
                    bindAddress = new InetSocketAddress(serverBindLocalInterface, serverBindPreferredLocalPort);
                    ServerSocket newSocket = newChannel.socket();
                    int useBufferSize = Math.max(TcpTransport.RecvBufferSize, newSocket.getReceiveBufferSize());

                    newSocket.setReceiveBufferSize(useBufferSize);
                    newSocket.bind(bindAddress, TcpTransport.MaxAcceptCnxBacklog);
                } catch (SocketException failed) {
                    if (-1 != serverBindStartLocalPort) {
                        // If there is a port range then forget our preferred port and rest
                        serverBindPreferredLocalPort = (0 == serverBindStartLocalPort) ? 0 : -1;
                        continue;
                    }

                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "Cannot bind ServerSocket on " + serverBindLocalInterface + ":" + serverBindPreferredLocalPort, failed);
                    }

                    return null;
                }
            } else {
                // No preference or we already tried and failed to bind the preferred port.
                ServerSocket newSocket = newChannel.socket();
                int useBufferSize = Math.max(TcpTransport.RecvBufferSize, newSocket.getReceiveBufferSize());

                newSocket.setReceiveBufferSize(useBufferSize);

                newSocket = IPUtils.bindServerSocketInRange(newSocket, serverBindStartLocalPort, serverBindEndLocalPort, TcpTransport.MaxAcceptCnxBacklog, serverBindLocalInterface);
            }

            try {
                newChannel.configureBlocking(false);
                newChannel.register(registerSelector, SelectionKey.OP_ACCEPT);
            } catch (ClosedChannelException cce) {
                // Odd.... try again.
                continue;
            }
            
            break;
        }

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Server will accept connections at " + newChannel.socket().getLocalSocketAddress());
        }

        return newChannel;
    }

    /**
     * An Executor task that creates a messenger from an incoming SocketChannel
     * object.
     */
    private static class MessengerBuilder implements Runnable {

        private final SocketChannel socketChannel;
        private final TcpTransport transport;
        TcpMessenger newMessenger;

        MessengerBuilder(TcpTransport transport, SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            this.transport = transport;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                if (socketChannel.isConnected() && (null != socketChannel.socket())) {
                    newMessenger = new TcpMessenger(socketChannel, transport);
                    if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                        TransportBindingMeter transportBindingMeter = transport.getUnicastTransportBindingMeter(null, newMessenger.getDestinationAddress());

                        if (transportBindingMeter != null) {
                            transportBindingMeter.connectionEstablished(false, 0);
                        }
                    }
                } else {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, socketChannel + " not connected.");
                    }
                }
            } catch (IOException io) {
                // protect against invalid connections
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Messenger creation failure", io);
                }
            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable", all);
                }
            }
        }
    }
}
