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

package net.jxta.impl.rendezvous.server;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.PeerConnection;
import net.jxta.impl.rendezvous.RdvWalk;
import net.jxta.impl.rendezvous.RdvWalker;
import net.jxta.impl.rendezvous.RendezVousPropagateMessage;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.RendezVousService;
import net.jxta.impl.rendezvous.limited.LimitedRangeWalk;
import net.jxta.impl.rendezvous.rendezvousMeter.ClientConnectionMeter;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.rendezvous.rpv.RendezvousPeersView;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.jxta.impl.rendezvous.RendezVousServiceProvider;

/**
 * A JXTA {@link net.jxta.rendezvous.RendezVousService} implementation which
 * implements the rendezvous server portion of the standard JXTA Rendezvous
 * Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezVousService
 * @see <a href="https://jxta-spec.dev.java.net/nonav/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 */
public class RendezvouseServiceServer extends RendezVousService {

    private final static Logger LOG = Logging.getLogger(RendezvouseServiceServer.class.getName());

    public static final String RDV_WALK_SVC_NAME = "RdvWalkSvcName";
    public static final String RDV_WALK_SVC_PARAM = "RdvWalkSvcParam";

    public final static long GC_INTERVAL = 2 * TimeUtils.AMINUTE;
    public final static long DEFAULT_LEASE_DURATION = 20L * TimeUtils.AMINUTE;
    public final static int DEFAULT_MAX_CLIENTS = 200;

    /**
     * Duration of leases we offer measured in relative milliseconds.
     */
    private final long LEASE_DURATION;

    /**
     * The maximum number of simultaneous clients we will allow.
     */
    private final int MAX_CLIENTS;

    /**
     * The clients which currently have a lease with us.
     */
    private final Map<ID, ClientConnection> clients = Collections.synchronizedMap(new HashMap<ID, ClientConnection>());

    private RdvWalk walk = null;
    private RdvWalker walker = null;

    /**
     * The peer view for this rendezvous server.
     */
    public final RendezvousPeersView rendezvousPeersView;

    private ScheduledFuture<?> gcTaskHandle;

    /**
     * Constructor for the RdvPeerRdvService object
     *
     * @param group      the peer group
     * @param rdvService the rendezvous service object
     */
    public RendezvouseServiceServer(PeerGroup group, RendezVousServiceImpl rdvService) {

        super(group, rdvService);

        Advertisement adv = null;
        ConfigParams confAdv = group.getConfigAdvertisement();

        // Get the config. If we do not have a config, we're done; we just keep
        // the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(rdvService.getAssignedID());

                if (null != configDoc) {
                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (java.util.NoSuchElementException failed) {// ignored
            }
        }

        RdvConfigAdv rdvConfigAdv;

        if (!(adv instanceof RdvConfigAdv)) {
            Logging.logCheckedDebug(LOG, "Creating new RdvConfigAdv for defaults.");
            rdvConfigAdv = (RdvConfigAdv) AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType());
        } else {
            rdvConfigAdv = (RdvConfigAdv) adv;
        }

        if (rdvConfigAdv.getMaxTTL() > 0) {
            MAX_TTL = rdvConfigAdv.getMaxTTL();
        } else {
            MAX_TTL = RendezVousService.DEFAULT_MAX_TTL;
        }

        if (rdvConfigAdv.getMaxClients() > 0) {
            MAX_CLIENTS = rdvConfigAdv.getMaxClients();
        } else {
            MAX_CLIENTS = DEFAULT_MAX_CLIENTS;
        }

        if (rdvConfigAdv.getLeaseDuration() > 0) {
            LEASE_DURATION = rdvConfigAdv.getLeaseDuration();
        } else {
            LEASE_DURATION = DEFAULT_LEASE_DURATION;
        }

        // Update the peeradv with that information:
        // XXX 20050409 bondolo How does this interact with auto-rdv?
        try {
            XMLDocument params = (XMLDocument)StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            Element e = params.createElement("Rdv", Boolean.TRUE.toString());

            params.appendChild(e);
            group.getPeerAdvertisement().putServiceParam(rdvService.getAssignedID(), params);
        } catch (Exception ohwell) {
            // don't worry about it for now. It'll still work.
            Logging.logCheckedWarning(LOG, "Failed adding service params\n", ohwell);
        }

        PeerGroup advGroup = group.getParentGroup();

        if ((null == advGroup) || PeerGroupID.WORLD_PEER_GROUP_ID.equals(advGroup.getPeerGroupID())) {
            // For historical reasons, we publish in our own group rather than
            // the parent if our parent is the world group.
            advGroup = null;
        }

        rendezvousPeersView = new RendezvousPeersView(group, advGroup, rdvService, rdvService.getAssignedID().toString() + group.getPeerGroupID().getUniqueValue().toString());
        Logging.logCheckedInfo(LOG, "RendezVous Service is initialized for ", group.getPeerGroupID(), " as a Rendezvous peer.");
    }

    /**
     * Listener for
     * <p/>
     * &lt;assignedID>/&lt;group-unique>
     */
    private class RendezvousServiceServerMessageListener implements RendezVousService.RendezvousMessageListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void processIncomingMessage(Message msg, EndpointAddress sourceAddress, EndpointAddress destinationAddress) {
            Logging.logCheckedDebug(LOG, "[", peerGroup.getPeerGroupID(), "] processing ", msg);

            if (msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectRequest) != null) {
                processLeaseRequest(msg);
            }

            if (msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequest) != null) {
                processDisconnectRequest(msg);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param argv
     * @return 
     */
    @Override
    protected int startApp(String[] argv) {
        super.startApp(argv, new RendezvousServiceServerMessageListener());

        rendezvousPeersView.start();

        // The other services may not be fully functional but they're there
        // so we can start our subsystems.
        // As for us, it does not matter if our methods are called between init
        // and startApp().

        // Start the Walk protcol. Create a LimitedRange Walk
        walk = new LimitedRangeWalk(peerGroup, new WalkListener(), pName, pParam, rendezvousPeersView);

        // We need to use a Walker in order to propagate the request
        // when when have no answer.
        walker = walk.getWalker();

        ScheduledExecutorService scheduledExecutor = peerGroup.getTaskManager().getScheduledExecutorService();
        gcTaskHandle = scheduledExecutor.scheduleAtFixedRate(new GCTask(), GC_INTERVAL, GC_INTERVAL, TimeUnit.MILLISECONDS);

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.startRendezvous();
        }

        rendezvousServiceImplementation.generateEvent(RendezvousEvent.BECAMERDV, peerGroup.getPeerID());

        Logging.logCheckedInfo(LOG, "RdvPeerRdvService is started");

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopApp() {
        if (closed) {
            return;
        }

        closed = true;

        walk.stop();
        walk = null;

        rendezvousPeersView.stop();

        // Tell all our clientss that we are going down
        disconnectAllClients();

        clients.clear();

        gcTaskHandle.cancel(false);

        super.stopApp();

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.stopRendezvous();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectToRendezVous(EndpointAddress addr, Object hint) {
        Logging.logCheckedWarning(LOG, "Invalid call to connectToRendezVous() on rendezvous peer");
    }

    /**
     * By default a RendezVous is never connected to another RendezVous through
     * a lease. This method does nothing.
     */
    @Override
    public void challengeRendezVous(ID peer, long delay) {
        Logging.logCheckedWarning(LOG, "Invalid call to challengeRendezVous() on RDV peer");
    }

    /**
     * By default a RendezVous is never connected to another RendezVous through
     * a lease. This method does nothing.
     * @param peerId
     */
    @Override
    public void disconnectFromRendezVous(ID peerId) {
        Logging.logCheckedWarning(LOG, "Invalid call to disconnectFromRendezVous() on RDV peer");
    }

    /**
     * {@inheritDoc}
     * @return 
     */
    @Override
    public boolean isConnectedToRendezVous() {
        return this.rendezvousPeersView.getSize() > 0;
    }

    /**
     * {@inheritDoc}
     * </p>For Rendezvous peers, this is the list of EDGE peers connected to this Rendezvous.
     */
    @Override
    public Vector<ID> getConnectedRendezvousPeersIDs() {

        Vector<ID> result = new Vector<>();
        List allClients = Arrays.asList(clients.values().toArray());

        for (Object allClient : allClients) {
            PeerConnection aConnection = (PeerConnection) allClient;

            if (aConnection.isConnected()) {
                result.add(aConnection.getPeerID());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void propagate(Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        if (closed) {
            return;
        }

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Propagating ", msg, "(TTL=", useTTL, ") to :", "\n\tsvc name:", serviceName, "\tsvc params:", serviceParam);

        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, useTTL);

        if (null != propHdr) {
            walk(msg, PropSName, PropPName, useTTL);
            // hamada: this is a very expensive operation and therefore not a supported operation
            // sendToEachConnection(msg, propHdr);
            sendToNetwork(msg, propHdr);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param msg
     * @param serviceName
     * @param serviceParam
     * @param initialTTL
     * @throws java.io.IOException
     */
    @Override
    public void propagateInGroup(Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        if (closed) {
            return;
        }

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Propagating ", msg, "(TTL=", useTTL, ") in group to :", "\n\tsvc name:", serviceName, "\tsvc params:", serviceParam);

        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, useTTL);

        if (null != propHdr) {
            walk(msg, PropSName, PropPName, useTTL);
            sendToEachConnection(msg, propHdr);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        }
    }

    /**
     * @param peerId     
     * @return Peer connection     
     */    
    @Override
    public PeerConnection getPeerConnection(ID peerId) {
        return clients.get(peerId);
    }

    /**
     * @return Peer connections
     */
    @Override
    protected PeerConnection[] getPeerConnections() {
        return clients.values().toArray(new PeerConnection[0]);
    }

    /**
     * Add a client to our collection of clients.
     *
     * @param peerAdvertisement  The advertisement of the peer to be added.
     * @param lease The lease duration in relative milliseconds.
     * @return the ClientConnection
     */
    private ClientConnection addClient(PeerAdvertisement peerAdvertisement, long lease) {
        ClientConnectionMeter clientConnectionMeter = null;

        int eventType;
        ClientConnection clientPeerConnection;

        synchronized (clients) {
            clientPeerConnection = clients.get(peerAdvertisement.getPeerID());

            // Check if the peer is already registered.
            if (null != clientPeerConnection) {
                eventType = RendezvousEvent.CLIENTRECONNECT;
            } else {
                eventType = RendezvousEvent.CLIENTCONNECT;
                clientPeerConnection = new ClientConnection(peerGroup, rendezvousServiceImplementation, peerAdvertisement.getPeerID());
                clients.put(peerAdvertisement.getPeerID(), clientPeerConnection);
            }
        }

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            clientConnectionMeter = rendezvousServiceMonitor.getClientConnectionMeter(peerAdvertisement.getPeerID());
        }

        if (RendezvousEvent.CLIENTCONNECT == eventType) {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (clientConnectionMeter != null)) {
                clientConnectionMeter.clientConnectionEstablished(lease);
            }
        } else {
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (clientConnectionMeter != null)) {
                clientConnectionMeter.clientLeaseRenewed(lease);
            }
        }

        rendezvousServiceImplementation.generateEvent(eventType, peerAdvertisement.getPeerID());
        clientPeerConnection.connect(peerAdvertisement, lease);
        return clientPeerConnection;
    }

    /**
     * Removes the specified client from the clients collections.
     *
     * @param peerConnection     The connection object to remove.
     * @param requested If <code>true</code> then the disconnection was
     *                  requested by the remote peer.
     * @return the ClientConnection object of the client or <code>null</code>
     *         if the client was not known.
     */
    private ClientConnection removeClient(PeerConnection peerConnection, boolean requested) {
        Logging.logCheckedDebug(LOG, "Disconnecting client ", peerConnection);

        if (peerConnection.isConnected()) {            
            sendDisconnect(peerConnection);
            peerConnection.setConnected(false);
        }

        rendezvousServiceImplementation.generateEvent(requested ? RendezvousEvent.CLIENTDISCONNECT : RendezvousEvent.CLIENTFAILED, peerConnection.getPeerID());

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            ClientConnectionMeter clientConnectionMeter = rendezvousServiceMonitor.getClientConnectionMeter((PeerID) peerConnection.getPeerID());
            clientConnectionMeter.clientConnectionDisconnected(requested);
        }

        return clients.remove(peerConnection.getPeerID());
    }

    private void disconnectAllClients() {
        for (Object o : Arrays.asList(clients.values().toArray())) {

            ClientConnection clientPeerConnection = (ClientConnection) o;

            try {
                removeClient(clientPeerConnection, false);
            } catch (Exception ez1) {
                Logging.logCheckedWarning(LOG, "disconnectClient failed for", clientPeerConnection, "\n", ez1);
            }
        }
    }

    /**
     * Handle a disconnection request
     *
     * @param message Message containing the disconnection request.
     */
    private void processDisconnectRequest(Message message) {

        PeerAdvertisement peerAdvertisement;

        try {
            MessageElement elem = message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequest);
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(elem);
            peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
        } catch (IOException e) {
            Logging.logCheckedWarning(LOG, "Cannot retrieve advertisment from disconnect request\n", e);
            return;
        }

        ClientConnection clientPeerConnection = clients.get(peerAdvertisement.getPeerID());

        if (clientPeerConnection != null) {
            // Make sure we don't send a disconnect
            clientPeerConnection.setConnected(false); 
            removeClient(clientPeerConnection, true);
        }                
        
        //mindarchitect 27052015
        //Notify rendezvous clients that client disconnect request was processed
        message.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new TextDocumentMessageElement(DisconnectRequestNotification, (XMLDocument) peerAdvertisement.getSignedDocument(), null));
        propagateRequestMessageInPeerGroup(message);        
    }

    /**
     * Handles a lease request message
     *
     * @param message Message containing the lease request
     */
    private void processLeaseRequest(Message message) {
        PeerAdvertisement peerAdvertisement;

        try {
            MessageElement messageElement = message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectRequest);
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(messageElement);
            peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);            
        } catch (Exception e) {
            Logging.logCheckedWarning(LOG, "Cannot retrieve advertisment from lease request\n", e);
            return;
        }

        // Publish the client's peer advertisement
        try {
            // This is not our own peer adv so we must not keep it longer than
            // its expiration time.
            DiscoveryService discovery = peerGroup.getDiscoveryService();
            if (null != discovery) {
                discovery.publish(peerAdvertisement, LEASE_DURATION * 2, 0);
            }
        } catch (IOException e) {
            Logging.logCheckedWarning(LOG, "Client peer advertisement publish failed\n", e);
        }

        long leaseTime;

        ClientConnection clientConnection = clients.get(peerAdvertisement.getPeerID());

        if (null != clientConnection) {
            Logging.logCheckedDebug(LOG, "Renewing client lease to ", clientConnection);
            leaseTime = LEASE_DURATION;
        } else {
            if (clients.size() < MAX_CLIENTS) {
                leaseTime = LEASE_DURATION;
                Logging.logCheckedDebug(LOG, "Offering new client lease to ", peerAdvertisement.getName(), " [", peerAdvertisement.getPeerID(), "]");
            } else {
                leaseTime = 0;
                Logging.logCheckedWarning(LOG, "Max clients exceeded, declining lease request from: ", peerAdvertisement.getName(), " [", peerAdvertisement.getPeerID(), "]");
            }
        }

        if (leaseTime > 0) {
            clientConnection = addClient(peerAdvertisement, leaseTime);

            // FIXME 20041015 bondolo We're supposed to send a lease 0 if we can't accept new clients.
            sendLeaseReplyMessage(clientConnection, leaseTime);            
            
            //mindarchitect 27052015
            //Notify rendezvous clients that new client lease request was processed and rendezvous registered it                        
            message.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new TextDocumentMessageElement(ConnectRequestNotification, (XMLDocument) peerAdvertisement.getSignedDocument(), null));
            propagateRequestMessageInPeerGroup(message); 
        }
    }

    /**
     * Sends a Connected lease reply message to the specified peer
     *
     * @param clientConnection The client peer.
     * @param lease lease duration.
     * @return Description of the Returned Value
     */
    private boolean sendLeaseReplyMessage(ClientConnection clientConnection, long lease) {

        Logging.logCheckedDebug(LOG, "Sending lease (", lease, ") to ", clientConnection.getPeerName());

        Message msg = new Message();
        
        msg.addMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new TextDocumentMessageElement(ConnectedRendezvousAdvertisementReply, getPeerAdvertisementDoc(), null));
        msg.addMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(ConnectedPeerReply, peerGroup.getPeerID().toString(), null));
        msg.addMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(ConnectedLeaseReply, Long.toString(lease), null));

        return clientConnection.sendMessage(msg, pName, pParam);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void walk(Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        if (closed) {
            return;
        }

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Undirected walk of ", msg, "(TTL=", useTTL, ") to :", "\n\tsvc name:", serviceName, "\tsvc params:", serviceParam);

        msg.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(RDV_WALK_SVC_NAME, serviceName, null));
        msg.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(RDV_WALK_SVC_PARAM, serviceParam, null));

        try {
            walker.walkMessage(null, msg, pName, pParam, useTTL);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walk();
            }
        } catch (IOException failure) {

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.walkFailed();
            }

            Logging.logCheckedWarning(LOG, "Cannot send message with Walker\n", failure);
            throw failure;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void walk(Vector<? extends ID> destPeerIDs, Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        if (closed) {
            return;
        }

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Directed walk of ", msg, "(TTL=", useTTL, ") to :\n\tsvc name:",
            serviceName, "\tsvc params:", serviceParam);

        msg.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(RDV_WALK_SVC_NAME, serviceName, null));
        msg.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new StringMessageElement(RDV_WALK_SVC_PARAM, serviceParam, null));

        for (ID destPeerID : destPeerIDs) {
            try {
                walker.walkMessage((PeerID) destPeerID, msg.clone(), pName, pParam, useTTL);
            } catch (IOException failed) {

                if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                    rendezvousMeter.walkToPeersFailed();
                }

                Logging.logCheckedWarning(LOG, "Cannot send message with Walker to: ", destPeerID, "\n", failed);
                throw new IOException("Cannot send message with Walker to: " + destPeerID, failed);
            }
        }
        
        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.walkToPeers(destPeerIDs.size());
        }
    }
    
    private void propagateRequestMessageInPeerGroup(Message message) {
        //mindarchitect 27052015        
        try {                        
            propagateInGroup(message, pName, pParam, MAX_TTL);
        } catch (IOException exception) {
            Logging.logCheckedDebug(LOG, "Propagating in peer group ", pParam, " failed with error: \n", exception.getLocalizedMessage());
        }
    }

    /**
     * Periodic cleanup task
     */
    private class GCTask implements Runnable {
        /**
         * {@inheritDoc
         */
        @Override
        public void run() {
            try {
                long gcStart = TimeUtils.timeNow();
                int gcedClients = 0;

                List allClients = Arrays.asList(clients.values().toArray());

                for (Object allClient : allClients) {
                    ClientConnection pConn = (ClientConnection) allClient;

                    try {
                        long now = TimeUtils.timeNow();

                        if (!pConn.isConnected() || (pConn.getLeaseEnd() < now)) {

                            // This client has dropped out or the lease is over.
                            // remove it.
                            Logging.logCheckedDebug(LOG, "GC CLIENT: dropping ", pConn);

                            pConn.setConnected(false);
                            removeClient(pConn, false);
                            gcedClients++;
                        }
                    } catch (Exception e) {
                        Logging.logCheckedWarning(LOG, "GCTask failed for ", pConn, "\n", e);
                    }
                }
                Logging.logCheckedDebug(LOG, "Client GC ", gcedClients, " of ", allClients.size(), " clients completed in ", TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), gcStart), "ms.");
            } catch (Throwable all) {
                Logging.logCheckedError(LOG, "Uncaught Throwable in thread :", Thread.currentThread().getName(), "\n", all);
            }
        }
    }

    /**
     * @inheritDoc
     */
    private class WalkListener implements EndpointListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void processIncomingMessage(Message msg, EndpointAddress sourceAddress, EndpointAddress destinationAddress) {

            MessageElement serviceMessageElement = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, RDV_WALK_SVC_NAME);

            if (null == serviceMessageElement) {
                Logging.logCheckedDebug(LOG, "Discarding ", msg, " because its missing service name element");
                return;
            }

            msg.removeMessageElement(serviceMessageElement);
            String serviceName = serviceMessageElement.toString();

            MessageElement paramME = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, RDV_WALK_SVC_PARAM);

            String sParam;

            if (null == paramME) {
                sParam = null;
            } else {
                msg.removeMessageElement(paramME);
                sParam = paramME.toString();
            }

            EndpointAddress realDest = new EndpointAddress(destinationAddress, serviceName, sParam);

            Logging.logCheckedDebug(LOG, "Calling local listener for [", realDest.getServiceName(), " / ", realDest.getServiceParameter(), "] with ", msg);

            rendezvousServiceImplementation.endpoint.processIncomingMessage(msg, sourceAddress, realDest);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.receivedMessageProcessedLocally();
            }
        }
    }
}
