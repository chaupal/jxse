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

package net.jxta.test.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;

import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.util.LimitInputStream;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.impl.endpoint.IPUtils;

/**
 *  Low-level TcpMessenger
 */
public class TcpConnection implements Runnable {

    private transient volatile boolean closed = false;
    private volatile boolean closingDueToFailure = false;

    private EndpointAddress dstAddress = null;
    private EndpointAddress fullDstAddress = null;
    private transient InetAddress inetAddress = null;
    private boolean initiator;
    private transient WatchedInputStream<Object> inputStream = null;
    private transient WelcomeMessage itsWelcome = null;

    private transient long lastUsed = System.currentTimeMillis();

    private transient WelcomeMessage myWelcome = null;
    private transient WatchedOutputStream<Object> outputStream = null;
    private transient int port = 0;
    static final int SendBufferSize = 64 * 1024; // 64 KBytes
    static final int RecvBufferSize = 64 * 1024; // 64 KBytes
    static final int LingerDelay = 2 * 60 * 1000;
    static final int LongTimeout = 30 * 60 * 1000;
    static final int ShortTimeout = 10 * 1000;

    private transient Thread recvThread = null;
    private transient Socket sharedSocket = null;
    private int connectionTimeOut = 10 * 1000;

    // Connections that are watched often - io in progress
    List<Object> ShortCycle = Collections.synchronizedList(new ArrayList<Object>());

    // Connections that are watched rarely - idle or waiting for input
    List<Object> LongCycle = Collections.synchronizedList(new ArrayList<Object>());

    /**
     *  only one outgoing message per connection.
     */
    private transient Object writeLock = "Write Lock";
    private final static Logger LOG = Logging.getLogger(TcpConnection.class.getName());
    private MessageListener listener = null;
    private final static MimeMediaType appMsg = new MimeMediaType("application/x-jxta-msg");

    /**
     *  Creates a new TcpConnection for the specified destination address.
     *
     *@param  destaddr         the destination address of this connection.
     *@param  p                the transport which this connection is part of.
     *@exception  IOException  Description of the Exception
     *@throws  IOException     for failures in creating the connection.
     */
    public TcpConnection(EndpointAddress destaddr, InetAddress from, PeerID id, MessageListener listener) throws IOException {

        initiator = true;

        this.listener = listener;
        this.fullDstAddress = destaddr;
        this.dstAddress = new EndpointAddress(destaddr, null, null);

        Logging.logCheckedInfo(LOG, "New TCP Connection to : " + dstAddress);

        String tmp = destaddr.getProtocolAddress();
        int portIndex = tmp.lastIndexOf(":");

        if (portIndex == -1) {
            throw new IllegalArgumentException("Invalid EndpointAddress (port # missing) ");
        }

        try {
            port = Integer.valueOf(tmp.substring(portIndex + 1)).intValue();
        } catch (NumberFormatException caught) {
            throw new IllegalArgumentException("Invalid EndpointAddress (port # invalid) ");
        }

        // Check for bad port number.
        if ((port <= 0) || (port > 65535)) {
            throw new IllegalArgumentException("Invalid port number in EndpointAddress: " + port);
        }
        inetAddress = InetAddress.getByName(tmp.substring(0, portIndex));
        try {
            sharedSocket = IPUtils.connectToFrom(inetAddress, port, from, 0, connectionTimeOut);
            startSocket(id);
        } catch (IOException e) {
            // If we failed for any reason, make sure the socket is closed.
            // We're the only one to know about it.
            if (sharedSocket != null) {
                sharedSocket.close();
            }
            throw e;
        }
    }

    /**
     *  Creates a new connection from an incoming socket
     *
     *@param  incSocket        the incoming socket.
     *@param  p                Description of the Parameter
     *@exception  IOException  Description of the Exception
     *@throws  IOException     for failures in creating the connection.
     */
    public TcpConnection(Socket incSocket, PeerID id, MessageListener listener) throws IOException {

        try {

            Logging.logCheckedInfo(LOG, "Connection from " + incSocket.getInetAddress().getHostAddress() + ":" + incSocket.getPort());

            initiator = false;
            this.listener = listener;
            inetAddress = incSocket.getInetAddress();
            port = incSocket.getPort();

            // Temporarily, our address for inclusion in the welcome message
            // response.
            dstAddress = new EndpointAddress("tcp", inetAddress.getHostAddress() + ":" + port, null, null);
            fullDstAddress = dstAddress;

            sharedSocket = incSocket;
            startSocket(id);

            // The correct value for dstAddr: that of the other party.
            dstAddress = itsWelcome.getPublicAddress();
            fullDstAddress = dstAddress;

            // Reset the thread name now that we have a meaningfull
            // destination address and remote welcome msg.
            setThreadName();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     *  Set the last used time for this connection in absolute milliseconds.
     *
     *@param  time  absolute time in milliseconds.
     */
    private void setLastUsed(long time) {
        lastUsed = time;
    }

    public WelcomeMessage getWM() {

        return myWelcome;
    }

    /**
     *  Sets the threadName attribute of the TcpConnection object
     */
    private synchronized void setThreadName() {

        if (recvThread != null) {

            try {

                recvThread.setName("TCP receive : " + itsWelcome.getPeerID() + " on address " + dstAddress);

            } catch (Exception ez1) {

                Logging.logCheckedWarning(LOG, "Cannot change thread name", ez1);

            }

        }
    }

    /**
     *  Gets the connectionAddress attribute of the TcpConnection object
     *
     *@return    The connectionAddress value
     */

    public EndpointAddress getConnectionAddress() {
        // Somewhat confusing but destinationAddress is the name of that thing
        // for the welcome message.
        return itsWelcome.getDestinationAddress();
    }

    /**
     *  Gets the destinationAddress attribute of the TcpConnection object
     *
     *@return    The destinationAddress value
     */
    public EndpointAddress getDestinationAddress() {
        return dstAddress;
    }

    /**
     *  Gets the destinationPeerID attribute of the TcpConnection object
     *
     *@return    The destinationPeerID value
     */
    public ID getDestinationPeerID() {
        return itsWelcome.getPeerID();
    }

    /**
     *  Return the absolute time in milliseconds at which this Connection was
     *  last used.
     *
     *@return    absolute time in milliseconds.
     */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
     *  return the current connection status.
     *
     *@return       The connected value
     */
    public boolean isConnected() {
        return ((recvThread != null) && (!closed));
    }

    /**
     *  Soft close of the connection. Messages can no longer be sent, but any in
     *  the queue will be flushed.
     */
    public synchronized void close() {

        Logging.logCheckedInfo(LOG, (closingDueToFailure ? "Failure" : "Normal") + " close of socket to : "
            + dstAddress + " / " + inetAddress.getHostAddress() + ":" + port);

        if (closingDueToFailure) {
            Logging.logCheckedInfo(LOG, "Failure stack trace\n", new Throwable("stack trace"));
        }

        if (!closed) {
            setLastUsed(0);
            // we idle now. Way idle.
            closeIOs();
            closed = true;
            if (recvThread != null) {
                recvThread.interrupt();
            }
        }
    }

    /**
     *  Description of the Method
     */
    private void closeIOs() {

        if (inputStream != null) {

            try {
                inputStream.close();
                inputStream = null;
            } catch (Exception ez1) {
                Logging.logCheckedWarning(LOG, "could not close inputStream ", ez1);
            }

        }

        if (outputStream != null) {

            try {
                outputStream.close();
                outputStream = null;
            } catch (Exception ez1) {
                Logging.logCheckedWarning(LOG, "Error : could not close outputStream ", ez1);
            }

        }
        if (sharedSocket != null) {

            try {
                sharedSocket.close();
                sharedSocket = null;
            } catch (Exception ez1) {
                Logging.logCheckedWarning(LOG, "Error : could not close socket ", ez1);
            }

        }
    }

    /**
     *  {@inheritDoc}
     *
     *@param  target  Description of the Parameter
     *@return         Description of the Return Value
     */
    @Override
    public boolean equals(Object target) {
        if (this == target) {
            return true;
        }

        if (null == target) {
            return false;
        }

        if (target instanceof TcpConnection) {
            TcpConnection likeMe = (TcpConnection) target;

            return getDestinationAddress().equals(likeMe.getDestinationAddress())
                    && getDestinationPeerID().equals(likeMe.getDestinationPeerID());
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected void finalize() {
        close();
    }

    /**
     *  {@inheritDoc}
     *
     *@return    Description of the Return Value
     */
    @Override
    public int hashCode() {
        return getDestinationPeerID().hashCode() + getDestinationAddress().hashCode();
    }

    /**
     *  This is called with "true" when the invoker is about to read some input
     *  and is not willing to wait for it to come. This is called with "false"
     *  when the invoker is about to wait for a long time for input to become
     *  available with a potentialy very long blocking read.
     *
     *@param  active  Description of the Parameter
     */
    private void inputActive(boolean active) {
        if (active) {
            inputStream.setWatchList(ShortCycle);
        } else {
            inputStream.setWatchList(LongCycle);
        }
    }

    /**
     *  {@inheritDoc}
     *
     * <p/>This is the background Thread. While the connection is active, takes
     * messages from the queue and send it.
     */
    public void run() {

        try {

            Logging.logCheckedInfo(LOG, "tcp receive - starts for " + inetAddress.getHostAddress() + ":" + port);

            try {

                while (isConnected()) {

                    if (closed) break;

                    Logging.logCheckedDebug(LOG, "tcp receive - message starts for " + inetAddress.getHostAddress() + ":" + port);

                    // We can stay blocked here for a long time, it's ok.
                    MessagePackageHeader header = new MessagePackageHeader(inputStream);
                    MimeMediaType msgMime = header.getContentTypeHeader();
                    long msglength = header.getContentLengthHeader();

                    // FIXME 20020730 bondolo@jxta.org Do something with content-coding here.

                    Logging.logCheckedDebug(LOG, "Message body (" + msglength + ") starts for " + inetAddress.getHostAddress() + ":" + port);

                    // read the message!
                    // We have received the header, so, the rest had better
                    // come. Turn the short timeout on.
                    inputActive(true);

                    Message msg = null;

                    try {

                        InputStream msgStream = new LimitInputStream(inputStream, msglength, true);
                        msg = WireFormatMessageFactory.fromWire(msgStream, msgMime, (MimeMediaType) null);

                    } catch (IOException failed) {

                        Logging.logCheckedWarning(LOG, "Failed reading msg from " + inetAddress.getHostAddress() + ":" + port);
                        throw failed;

                    } finally {
                        // We can relax again.
                        inputActive(false);
                    }

                    Logging.logCheckedDebug(LOG, "Handing incoming message from "
                        + inetAddress.getHostAddress() + ":" + port + " to EndpointService");

                    try {

                        // Demux the message for the upper layers
                        if (listener != null) {
                            listener.demux(msg);
                        }

                    } catch (Throwable t) {

                        Logging.logCheckedWarning(LOG, "Failure while endpoint demuxing " + msg, t);

                    }

                    setLastUsed(System.currentTimeMillis());
                }
            } catch (InterruptedIOException woken) {
                // We have to treat this as fatal since we don't know where
                // in the framing the input stream was at.

                closingDueToFailure = true;

                Logging.logCheckedWarning(LOG, "Error : read() timeout after " + woken.bytesTransferred + " on connection "
                            + inetAddress.getHostAddress() + ":" + port);

            } catch (EOFException finished) {

                // The other side has closed the connection
                Logging.logCheckedInfo(LOG, "Connection was closed by " + inetAddress.getHostAddress() + ":" + port);

            } catch (Throwable e) {

                closingDueToFailure = true;
                Logging.logCheckedWarning(LOG, "Error on connection " + inetAddress.getHostAddress() + ":" + port, e);

            } finally {
                synchronized (this) {
                    if (!closed) {
                        // We need to close the connection down.
                        close();
                    }

                    recvThread = null;
                }
            }

        } catch (Throwable all) {

            Logging.logCheckedError(LOG, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);

        }
    }

    /**
     *  Send message to the remote peer.
     *
     *@param  msg              the message to send.
     *@exception  IOException  Description of the Exception
     */
    public void sendMessage(Message msg) throws IOException {

        // socket is a stream, only one writer at a time...
        synchronized (writeLock) {

            if (closed) {
                Logging.logCheckedInfo(LOG, "Connection was closed to : " + dstAddress);
                throw new IOException("Connection was closed to : " + dstAddress);
            }

            boolean success = false;
            long size = 0;

            try {
                // 20020730 bondolo@jxta.org Do something with content-coding here
                // serialize the message.
                WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, appMsg, (MimeMediaType[]) null);

                // Build the protocol header
                // Allocate a buffer to contain the message and the header
                MessagePackageHeader header = new MessagePackageHeader();

                header.setContentTypeHeader(serialed.getMimeType());
                size = serialed.getByteLength();
                header.setContentLengthHeader(size);

                Logging.logCheckedDebug(LOG, "sendMessage (" + serialed.getByteLength() + ") to " + dstAddress + " via "
                    + inetAddress.getHostAddress() + ":" + port);

                // Write the header and the message.
                header.sendToStream(outputStream);
                outputStream.flush();

                serialed.sendToStream(outputStream);
                outputStream.flush();

                // all done!
                success = true;
                setLastUsed(System.currentTimeMillis());

            } catch (Throwable failure) {

                Logging.logCheckedInfo(LOG, "tcp send - message send failed for ",
                    inetAddress.getHostAddress(), ':', port, '\n', failure);
                closingDueToFailure = true;
                close();

            }
        }
    }

    /**
     *  Description of the Method
     */
    protected void start() {
        recvThread.start();
    }

    /**
     *  Description of the Method
     *
     *@exception  IOException  Description of the Exception
     */
    private void startSocket(PeerID id) throws IOException {

        sharedSocket.setKeepAlive(true);
        int useBufferSize = Math.max(SendBufferSize, sharedSocket.getSendBufferSize());

        sharedSocket.setSendBufferSize(useBufferSize);
        useBufferSize = Math.max(RecvBufferSize, sharedSocket.getReceiveBufferSize());
        sharedSocket.setReceiveBufferSize(useBufferSize);

        sharedSocket.setSoLinger(true, LingerDelay);
        // socket.setTcpNoDelay(true);

        outputStream = new WatchedOutputStream<Object>(sharedSocket.getOutputStream());
        outputStream.setWatchList(ShortCycle);
        inputStream = new WatchedInputStream<Object>(sharedSocket.getInputStream());
        outputStream.setWatchList(LongCycle);

        if ((inputStream == null) || (outputStream == null)) {
            Logging.logCheckedError(LOG, "   failed getting streams.");
            throw new IOException("Could not get streams");
        }

        myWelcome = new WelcomeMessage(fullDstAddress, fullDstAddress, id, false);
        myWelcome.sendToStream(outputStream);
        outputStream.flush();
        // The response should arrive shortly or we bail out.
        inputActive(true);
        itsWelcome = new WelcomeMessage(inputStream);

        // Ok, we can wait for messages now.
        inputActive(false);

        Logging.logCheckedDebug(LOG, "Hello from " + itsWelcome.getPublicAddress() + " [" + itsWelcome.getPeerID() + "]");

        recvThread = new Thread(this);
        setThreadName();
        recvThread.setDaemon(true);
    }

    /**
     *  {@inheritDoc} <p/>
     *
     *  Implementation for debugging.
     *
     *@return    Description of the Return Value
     */
    @Override
    public String toString() {
        return super.toString() + ":" + ((null != itsWelcome) ? itsWelcome.getPeerID().toString() : "unknown") + " on address "
                + ((null != dstAddress) ? dstAddress.toString() : "unknown");
    }
}

