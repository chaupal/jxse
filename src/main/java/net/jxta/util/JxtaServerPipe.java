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
package net.jxta.util;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.endpoint.tcp.TcpMessenger;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;
import java.util.Properties;

/**
 * The server side of a JxtaBiDiPipe. The intent of this object is accept connection requests.
 * JxtaServerPipe follows the same pattern as java.net.ServerSocket, without it no connection can be
 * established.
 */
public class JxtaServerPipe implements PipeMsgListener {

    private static final Logger LOG = Logger.getLogger(JxtaServerPipe.class.getName());
    protected static final String nameSpace = "JXTABIP";
    protected static final String credTag = "Cred";
    protected static final String reqPipeTag = "reqPipe";
    protected static final String remPeerTag = "remPeer";
    protected static final String remPipeTag = "remPipe";
    protected static final String closeTag = "close";
    protected static final String reliableTag = "reliable";
    protected static final String directSupportedTag = "direct";
    protected static final String connectionPropertiesTag = "connectionproperties";

    public static final int DEFAULT_TIMEOUT = 30 * 1000; // 30 seconds
    public static final int DEFAULT_BACKLOG = 50;

    private PeerGroup group;
    private InputPipe serverPipe;
    private PipeAdvertisement pipeadv;
    private final Object closeLock = new Object();
    private boolean bound = false;
    private boolean closed = false;
    protected StructuredDocument myCredentialDoc = null;

    private volatile ServerPipeAcceptListener listener;
    private final QueuingServerPipeAcceptor defaultListener;

    /**
     * Default constructor for the JxtaServerPipe
     * <p/>
     * backlog is set to {@link #DEFAULT_BACKLOG}.
     * accept timeout is set to {@link #DEFAULT_TIMEOUT}.
     * <p> call to accept() for this ServerPipe will
     * block for only this amount of time. If the timeout expires,
     * a java.net.SocketTimeoutException is raised, though the ServerPipe is still valid.
     * <p/>
     *
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @throws IOException if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv) throws IOException {
        this(group, pipeadv, DEFAULT_BACKLOG, DEFAULT_TIMEOUT);
    }

    /**
     * Constructor for the JxtaServerPipe object
     *
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @param backlog the maximum length of the queue.
     * @throws IOException if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv, int backlog) throws IOException {
        this(group, pipeadv, backlog, DEFAULT_TIMEOUT);
    }

    /**
     * Constructor for the JxtaServerPipe
     *
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @param backlog the maximum length of the queue.
     * @param timeout call to accept() for this ServerPipe will
     *                block for only this amount of time. If the timeout expires,
     *                a java.net.SocketTimeoutException is raised, though the ServerPipe is still valid.
     * @throws IOException if an I/O error occurs
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv, int backlog, int timeout) throws IOException {
        this.defaultListener = new QueuingServerPipeAcceptor(backlog, timeout);
        this.listener = defaultListener;
        bind(group, pipeadv);
    }

    /**
     * Creates a server pipe for the specified group, configured using the properties of the specified pipe
     * advertisement. Additionally, accepting incoming pipe connections will be sent asynchronously to the
     * provided {@link ServerPipeAcceptListener}. This form of the constructor is intended for those clients
     * who wish to immediately handle incoming connections rather than using the blocking {@link #accept()} method
     * synchronously. Please note that if the object is constructed this way, the accept method will no longer
     * function, and will instead immediately return null.
     * 
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @param listener the listener to which all incoming connections will be sent immediately and asynchronously.
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the listener is null
     */
    public JxtaServerPipe(PeerGroup group, PipeAdvertisement pipeadv, ServerPipeAcceptListener listener) throws IOException {
        // we still set a valid default listener even though it is not used. This is used later as a quick
        // way of checking whether we are using the default or not.
        if(listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.defaultListener = new QueuingServerPipeAcceptor(1, 0);
        this.listener = listener;
        bind(group, pipeadv);
    }

    /**
     * Binds the <code>JxtaServerPipe</code> to a specific pipe advertisement
     *
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @throws IOException if an I/O error occurs
     */
    public void bind(PeerGroup group, PipeAdvertisement pipeadv) throws IOException {
        this.group = group;
        this.pipeadv = pipeadv;
        PipeService pipeSvc = group.getPipeService();
        serverPipe = pipeSvc.createInputPipe(pipeadv, this);
        setBound();
    }

    /**
     * Binds the <code>JxtaServerPipe</code> to a specific pipe advertisement
     *
     * @param group   JXTA PeerGroup
     * @param pipeadv PipeAdvertisement on which pipe requests are accepted
     * @param backlog the maximum length of the queue.
     * @throws IOException if an I/O error occurs
     * @deprecated as of version 2.7, backlog must be specified to the constructor of the server pipe
     * only.
     */
    @Deprecated
    public void bind(PeerGroup group, PipeAdvertisement pipeadv, int backlog) throws IOException {
        bind(group, pipeadv);
    }

    /**
     * Listens for a connection to be made to this socket and accepts
     * it. The method blocks until a connection is made.
     *
     * @return the connection accepted, null otherwise
     * @throws IOException if an I/O error occurs
     */
    public JxtaBiDiPipe accept() throws IOException {
    	checkNotClosed();
        checkBound();

        if(usingBlockingAccept()) {
            return defaultListener.acceptBackwardsCompatible();
        } else {
            throw new IllegalStateException("cannot call accept() if a custom ServerPipeAcceptListener is in use");
        }
    }

    public boolean usingBlockingAccept() {
        return listener == defaultListener;
    }

    /**
     * Gets the group associated with this JxtaServerPipe
     *
     * @return The group value
     */
    public PeerGroup getGroup() {
        return group;
    }

    /**
     * Gets the PipeAdvertisement associated with this JxtaServerPipe
     *
     * @return The pipeAdv value
     */
    public PipeAdvertisement getPipeAdv() {
        return pipeadv;
    }

    /**
     * Closes this JxtaServerPipe (closes the underlying input pipe).
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        synchronized (closeLock) {
            if (isClosed()) {
                return;
            }
            if (bound) {
                // close all the pipe
                serverPipe.close();
                listener.serverPipeClosed();
                bound = false;
            }
            closed = true;
        }
    }

    /**
     * Sets the bound attribute of the JxtaServerPipe
     */
    void setBound() {
        bound = true;
    }

    private void checkNotClosed() throws SocketException {
        if (isClosed()) {
            throw new SocketException("Server Pipe is closed");
        }
    }

    private void checkBound() throws SocketException {
        if (!isBound()) {
            throw new SocketException("JxtaServerPipe is not bound yet");
        }
    }

    /**
     * Gets the Timeout attribute of the JxtaServerPipe.
     *
     * @return The timeout value in use for the {@link #accept()} method.
     * @throws IOException if an I/O error occurs.
     * @throws IllegalStateException if a custom {@link ServerPipeAcceptListener} is in use.
     */
    public synchronized int getPipeTimeout() throws IOException {
        checkNotClosed();
        if(usingBlockingAccept()) {
            return defaultListener.getTimeoutBackwardsCompatible();
        } else {
            throw new IllegalStateException("Custom ServerPipeAcceptListener is in use, timeout does not apply");
        }
    }

    /**
     * Sets the Timeout attribute of the JxtaServerPipe. A timeout of 0 blocks forever, and
     * a timeout value less than zero is illegal.
     *
     * @throws SocketException if an I/O error occurs
     * @throws IllegalStateException if a custom {@link ServerPipeAcceptListener} is in use.
     */
    public synchronized void setPipeTimeout(int timeout) throws SocketException {
        checkNotClosed();
        if(usingBlockingAccept()) {
            defaultListener.setTimeoutBackwardsCompatible(timeout);
        } else {
            throw new IllegalStateException("Custom ServerPipeAcceptListener is in use, timeout does not apply");
        }

    }

    /**
     * Returns the closed state of the JxtaServerPipe.
     *
     * @return true if the socket has been closed
     */
    public boolean isClosed() {
        synchronized (closeLock) {
            return closed;
        }
    }

    /**
     * Returns the binding state of the JxtaServerPipe.
     *
     * @return true if the ServerSocket successfully bound to an address
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {
        Message message = event.getMessage();
        if (message == null) {
            return;
        }

        JxtaBiDiPipe bidi = processMessage(message);
        // make sure we have a socket returning
        if (bidi == null) {
            return;
        }

        listener.pipeAccepted(bidi);
    }

    /**
     * Method processMessage is the heart of this class.
     * <p/>
     * This takes new incoming connect messages and constructs the JxtaBiDiPipe
     * to talk to the new client.
     * <p/>
     * The ResponseMessage is created and sent.
     *
     * @param msg The client connection request (assumed not null)
     * @return JxtaBiDiPipe Which may be null if an error occurs.
     */
    private JxtaBiDiPipe processMessage(Message msg) {

        PipeAdvertisement outputPipeAdv = null;
        PeerAdvertisement peerAdv = null;
        StructuredDocument credDoc = null;
        Properties connectionProperties = null;
        try {
            MessageElement el = msg.getMessageElement(nameSpace, credTag);

            if (el != null) {
                credDoc = StructuredDocumentFactory.newStructuredDocument(el);
            }

            el = msg.getMessageElement(nameSpace, reqPipeTag);
            if (el != null) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(el);
                outputPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
            }

            el = msg.getMessageElement(nameSpace, remPeerTag);
            if (el != null) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(el);
                peerAdv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
            }

            el = msg.getMessageElement(nameSpace, reliableTag);
            boolean isReliable = false;

            if (el != null) {

                isReliable = Boolean.valueOf((el.toString()));
                Logging.logCheckedFine(LOG, "Connection request [isReliable] :", isReliable);

            }

            el = msg.getMessageElement(nameSpace, directSupportedTag);
            boolean directSupported = false;

            if (el != null) {

                directSupported = Boolean.valueOf((el.toString()));
                Logging.logCheckedFine(LOG, "Connection request [directSupported] :", directSupported);

            }

            el = msg.getMessageElement(nameSpace, connectionPropertiesTag);
            byte[] connectionPropertiesBytes = null;

            if (el != null) {

                connectionPropertiesBytes = el.getBytes(false);
                Logging.logCheckedFine(LOG, "Connection request [connectionPropertiesBytes] :", connectionPropertiesBytes);

                if (connectionPropertiesBytes != null) 
                    connectionProperties = bytesToProperties(connectionPropertiesBytes);

            }

            Messenger msgr;
            boolean direct = false;
            if (directSupported) {
                msgr = JxtaBiDiPipe.getDirectMessenger(group, outputPipeAdv, peerAdv);
                if (msgr == null) {
                    msgr = JxtaBiDiPipe.lightweightOutputPipe(group, outputPipeAdv, peerAdv);
                } else {
                    direct = true;
                }
            } else {
                msgr = JxtaBiDiPipe.lightweightOutputPipe(group, outputPipeAdv, peerAdv);
            }

            if (msgr != null) {

                Logging.logCheckedFine(LOG, "Reliability set to :", isReliable);

                PipeAdvertisement newpipe = newInputPipe(group, outputPipeAdv);
                JxtaBiDiPipe pipe = null;

                if (connectionProperties != null) {
                    pipe = new JxtaBiDiPipe(group, msgr, newpipe, credDoc, isReliable, direct, connectionProperties);
                } else {
                    pipe = new JxtaBiDiPipe(group, msgr, newpipe, credDoc, isReliable, direct);
                }

                pipe.setRemotePeerAdvertisement(peerAdv);
                pipe.setRemotePipeAdvertisement(outputPipeAdv);
                sendResponseMessage(group, msgr, newpipe);

                return pipe;
            }

        } catch (IOException e) {

            // deal with the error
            Logging.logCheckedFine(LOG, "IOException occured\n", e);

        }

        return null;
    }

    private Properties bytesToProperties(byte[] propsBytes) {
        Properties properties = new Properties();
        ByteArrayInputStream bis = new ByteArrayInputStream(propsBytes);
        try {
            properties.load(bis);
        } catch (IOException e) {
        }
        return properties;
    }

    /**
     * Method sendResponseMessage get the createResponseMessage and sends it.
     *
     * @param group  the peer group
     * @param msgr   the remote node messenger
     * @param pipeAd the pipe advertisement
     * @throws IOException for failures sending the response message.
     */
    protected void sendResponseMessage(PeerGroup group, Messenger msgr, PipeAdvertisement pipeAd) throws IOException {

        Message msg = new Message();
        PeerAdvertisement peerAdv = group.getPeerAdvertisement();

        if (myCredentialDoc == null) {
            myCredentialDoc = JxtaBiDiPipe.getCredDoc(group);
        }

        if (myCredentialDoc != null) {
            msg.addMessageElement(JxtaServerPipe.nameSpace,
                    new TextDocumentMessageElement(credTag, (XMLDocument) myCredentialDoc, null));
        }

        final String neverAllowDirectBreaksRelay = Boolean.toString(false);
        msg.addMessageElement(JxtaServerPipe.nameSpace,
                new StringMessageElement(JxtaServerPipe.directSupportedTag, neverAllowDirectBreaksRelay, null));

        msg.addMessageElement(JxtaServerPipe.nameSpace,
                new TextDocumentMessageElement(remPipeTag, (XMLDocument) pipeAd.getDocument(MimeMediaType.XMLUTF8), null));

        msg.addMessageElement(nameSpace,
                new TextDocumentMessageElement(remPeerTag, (XMLDocument) peerAdv.getDocument(MimeMediaType.XMLUTF8), null));
        if (msgr instanceof TcpMessenger) {
            ((TcpMessenger) msgr).sendMessageDirect(msg, null, null, true);
        } else {
            msgr.sendMessage(msg);
        }
    }

    /**
     * Utility method newInputPipe is used to get new pipe advertisement (w/random pipe ID) from old one.
     * <p/>
     * Called by JxtaSocket to make pipe (name -> name.remote) for open message
     * <p/>
     * Called by JxtaServerSocket to make pipe (name.remote -> name.remote.remote) for response message
     *
     * @param group   the peer group
     * @param pipeadv to get the basename and type from
     * @return PipeAdvertisement a new pipe advertisement
     */
    protected static PipeAdvertisement newInputPipe(PeerGroup group, PipeAdvertisement pipeadv) {
        PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        adv.setPipeID(IDFactory.newPipeID(group.getPeerGroupID()));
        adv.setName(pipeadv.getName());
        adv.setType(pipeadv.getType());
        return adv;
    }

    /**
     * get the credential doc
     *
     * @return Credential StructuredDocument
     */
    public StructuredDocument getCredentialDoc() {
        return myCredentialDoc;
    }

    /**
     * Sets the connection credential doc
     * If no credentials are set, the default group credential will be used
     *
     * @param doc Credential StructuredDocument
     */
    public void setCredentialDoc(StructuredDocument doc) {
        this.myCredentialDoc = doc;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Closes the JxtaServerPipe.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                Logging.logCheckedWarning(LOG, "JxtaServerPipe is being finalized without being previously closed. This is likely a user's bug.");
            }
            close();
        } finally {
            super.finalize();
        }
    }
}
