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

package net.jxta.impl.rendezvous.client.edge;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.endpoint.relay.RelayReferralSeedingManager;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.PeerConnection;
import net.jxta.impl.rendezvous.RendezVousPropagateMessage;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;
import net.jxta.impl.rendezvous.RendezVousServiceProvider;
import net.jxta.impl.rendezvous.RendezVousService;
import static net.jxta.impl.rendezvous.RendezVousService.ConnectRequest;
import static net.jxta.impl.rendezvous.RendezVousService.ConnectedPeerReply;
import static net.jxta.impl.rendezvous.RendezVousService.DisconnectRequestNotification;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousConnectionMeter;
import net.jxta.impl.rendezvous.rendezvousMeter.RendezvousMeterBuildSettings;
import net.jxta.impl.rendezvous.rpv.PeerviewSeedingManager;
import net.jxta.impl.util.SeedingManager;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.URISeedingManager;
import net.jxta.impl.util.threads.SelfCancellingTask;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.Module;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;

/**
 * A JXTA {@link net.jxta.rendezvous.RendezVousService} implementation which
 * implements the client portion of the standard JXTA Rendezvous Protocol (RVP).
 *
 * @see net.jxta.rendezvous.RendezVousService
 * @see <a href="https://jxta-spec.dev.java.net/nonav/JXTAProtocols.html#proto-rvp" target="_blank">JXTA Protocols Specification : Rendezvous Protocol</a>
 */
public class EdgePeerRendezvousServiceClient extends RendezVousService {

    private final static transient Logger LOG = Logging.getLogger(EdgePeerRendezvousServiceClient.class.getName());

    /**
     * Interval in milliseconds at which we will check our rendezvous connection.
     */
    private final static long MONITOR_INTERVAL = 15 * TimeUtils.ASECOND;

    /**
     * Number of rendezvous we will try to connect to.
     */
    private final int MAX_RDV_CONNECTIONS = 1;

    /**
     * The default amount of time we will attempt to renew a lease before it
     * expires.
     */
    private long LEASE_MARGIN = 5 * TimeUtils.AMINUTE;

    /**
     * Our source for rendezvous server seeds.
     */
    private final SeedingManager seedingManager;

    /**
     * Our current seeds.
     */
    private final List<RouteAdvertisement> seeds = new ArrayList<>();

    /**
     * Our current connections with RendezVous peers.
     */
    private final Map<ID, RendezvousConnection> connectedRendezVousPeers = Collections.synchronizedMap(new HashMap<ID, RendezvousConnection>());

    private MonitorTask monitorTask;

    /**
     * Standard Constructor
     *
     * @param peerGroup      Description of Parameter
     * @param rendezvousServiceImplementation Description of Parameter
     */
    public EdgePeerRendezvousServiceClient(PeerGroup peerGroup, RendezVousServiceImpl rendezvousServiceImplementation) {
        super(peerGroup, rendezvousServiceImplementation);

        Advertisement adv = null;
        ConfigParams confAdv = peerGroup.getConfigAdvertisement();

        // Get the config. If we do not have a config, we're done; we just keep the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            adv = confAdv.getSvcConfigAdvertisement(rendezvousServiceImplementation.getAssignedID());
        }

        RdvConfigAdv rendezvoudConfigurationAdvertisement;

        if (!(adv instanceof RdvConfigAdv)) {
            Logging.logCheckedDebug(LOG, "Creating new RdvConfigAdv for defaults.");
            rendezvoudConfigurationAdvertisement = (RdvConfigAdv) AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType());
        } else {
            rendezvoudConfigurationAdvertisement = (RdvConfigAdv) adv;
        }

        if (rendezvoudConfigurationAdvertisement.getMaxTTL() != -1) {
            MAX_TTL = rendezvoudConfigurationAdvertisement.getMaxTTL();
        }

        if (rendezvoudConfigurationAdvertisement.getLeaseMargin() != 0) {
            LEASE_MARGIN = rendezvoudConfigurationAdvertisement.getLeaseMargin();
        }

        String serviceName = rendezvousServiceImplementation.getAssignedID().toString() + peerGroup.getPeerGroupID().getUniqueValue().toString();
        //For debugging purposes only
        //System.out.println("Rendezvous service started for peer group: " + peerGroup.getPeerGroupID().toString());

        URISeedingManager uriSeedingManager;

        if (rendezvoudConfigurationAdvertisement.getProbeRelays()) {
            uriSeedingManager = new RelayReferralSeedingManager(rendezvoudConfigurationAdvertisement.getAclUri(), rendezvoudConfigurationAdvertisement.getUseOnlySeeds(), peerGroup, serviceName);
        } else {
            uriSeedingManager = new URISeedingManager(rendezvoudConfigurationAdvertisement.getAclUri(), rendezvoudConfigurationAdvertisement.getUseOnlySeeds(), peerGroup, serviceName);
        }

        for (URI aSeeder : Arrays.asList(rendezvoudConfigurationAdvertisement.getSeedingURIs())) {
            Logging.logCheckedConfig(LOG, "EdgePeerRdvService adding seeding: ", aSeeder);
            uriSeedingManager.addSeedingURI(aSeeder);
        }

        for (URI aSeed : Arrays.asList(rendezvoudConfigurationAdvertisement.getSeedRendezvous())) {
            Logging.logCheckedConfig(LOG, "EdgePeerRdvService adding seed   : ", aSeed);
            uriSeedingManager.addSeed(aSeed);
        }

        this.seedingManager = uriSeedingManager;
        Logging.logCheckedInfo(LOG, "RendezVous Service is initialized for ", peerGroup.getPeerGroupID(), " as an EDGE peer.");
    }

    /**
     * Listener for
     * <p/>
     * &lt;assignedID>
     */
    private class EdgePeerRendezvousServiceClientMessageListener implements RendezVousService.RendezvousMessageListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void processIncomingMessage(Message message, EndpointAddress sourceAddress, EndpointAddress destinationAddress) {
            Logging.logCheckedDebug(LOG, "[", peerGroup.getPeerGroupID(), "] processing ", message);                        

            if ((message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectedPeerReply) != null) || (message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectedRendezvousAdvertisementReply) != null)) {
                processConnectedReply(message);                
            }                        

            if (message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequest) != null) {
                processDisconnectRequest(message);                
            }
            
            PeerID peerId = peerGroup.getPeerID();                        

            //mindarchitect 27052015
            //This message is propagated by rendezvous server service to notify all rendezvous clients that peer lease request was processed and it connected to rendezvous in peer group
            //We need to know when peer connects or disconnects from the rendezvous to notify all peers about that
            if ((message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectRequestNotification) != null)) {                
                //Just generate the corresponding event
                PeerAdvertisement peerAdvertisement;

                try {
                    MessageElement messageElement = message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectRequestNotification);
                    XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(messageElement);
                    peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
                } catch (IOException e) {
                    Logging.logCheckedWarning(LOG, "Could not process connect request notification\n", e);
                    return;
                }                
                
                //Make sure we are not processing own request                
                if (!peerAdvertisement.getPeerID().equals(peerId)) {
                    rendezvousServiceImplementation.generateEvent(RendezvousEvent.CLIENTCONNECT, peerAdvertisement.getPeerID());
                }
            }  
            
            //mindarchitect 27052015
            //This message is propagated by rendezvous server service to notify all rendezvous clients that peer disconnect request was processed and it is disconnected from rendezvous in peer group
            //We need to know when peer connects or disconnects from the rendezvous to notify all peers about that
            if ((message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequestNotification) != null)) {                
                //Just generate the corresponding event
                PeerAdvertisement peerAdvertisement;

                try {
                    MessageElement messageElement = message.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequestNotification);
                    XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(messageElement);
                    peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
                } catch (IOException e) {
                    Logging.logCheckedWarning(LOG, "Could not process disconnect request notification\n", e);
                    return;
                }                
                
                //Make sure we are not processing own request                
                if (!peerAdvertisement.getPeerID().equals(peerId)) {
                    rendezvousServiceImplementation.generateEvent(RendezvousEvent.CLIENTDISCONNECT, peerAdvertisement.getPeerID());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @return 
     */
    @Override
    protected int startApp(String[] arg) {

        super.startApp(arg, new EdgePeerRendezvousServiceClientMessageListener());

        // The other services may not be fully functional but they're there
        // so we can start our subsystems.
        // As for us, it does not matter if our methods are called between init
        // and startApp().

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.startEdge();
        }

        rendezvousServiceImplementation.generateEvent(RendezvousEvent.BECAMEEDGE, peerGroup.getPeerID());

        scheduleMonitor(0);
        return Module.START_OK;
    }

    private void scheduleMonitor(long delayInMs) {
        stopMonitor();
        
        ScheduledExecutorService scheduledExecutor = peerGroup.getTaskManager().getScheduledExecutorService();
        MonitorTask monitorTask = new MonitorTask();
        monitorTask.setHandle(scheduledExecutor.scheduleAtFixedRate(monitorTask, delayInMs, MONITOR_INTERVAL, TimeUnit.MILLISECONDS));
    }

    private void stopMonitor() {
        if(monitorTask != null) {
            monitorTask.cancel();
        }
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
        seedingManager.stop();
        disconnectFromAllRendezVous();
        stopMonitor();
        super.stopApp();

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
            rendezvousMeter.stopEdge();
        }
    }

    /**
     * {@inheritDoc}
     * @return 
     */
    @Override
    public Vector<ID> getConnectedRendezvousPeersIDs() {
        return new Vector<ID>(connectedRendezVousPeers.keySet());
    }

    /**
     * {@inheritDoc}
     * @return 
     */
    @Override
    public boolean isConnectedToRendezVous() {
        return !connectedRendezVousPeers.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectToRendezVous(EndpointAddress addr, Object hint) {
        if (seedingManager instanceof URISeedingManager) {
            URISeedingManager uriseed = (URISeedingManager) seedingManager;

            if (hint instanceof RouteAdvertisement) {
                uriseed.addSeed((RouteAdvertisement) hint);
            } else {
                uriseed.addSeed(addr.toURI());
            }
        } else if (seedingManager instanceof PeerviewSeedingManager) {
            PeerviewSeedingManager pvseed = (PeerviewSeedingManager) seedingManager;

            if (hint instanceof RouteAdvertisement) {
                pvseed.addSeed((RouteAdvertisement) hint);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param peerid
     */
    @Override
    public void challengeRendezVous(ID peerid, long delay) {

        // If immediate failure is requested, just do it.
        // {@code disconnectFromRendezVous()} will at least get the peer
        // removed from the peerView, even if it is not currently a rendezvous
        // of ours. That permits to purge from the peerview rdvs that we try
        // and fail to connect to, faster than the background keep alive done
        // by PeerView itself.
        if (delay <= 0) {
            removeRendezvousPeer(peerid, false);
            return;
        }

        RendezvousConnection pConn = connectedRendezVousPeers.get(peerid);

        if (null != pConn) {
            long adjusted_delay = Math.max(0, Math.min(TimeUtils.toRelativeTimeMillis(pConn.getLeaseEnd()), delay));

            pConn.setLease(adjusted_delay, adjusted_delay);
        }
    }

    /**
     * {@inheritDoc}
     * @param peerId
     */
    @Override
    public void disconnectFromRendezVous(ID peerId) {
        removeRendezvousPeer(peerId, false);
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void propagate(Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Propagating ", msg, "(TTL=", useTTL, ") to :\n\tsvc name:", serviceName, "\tsvc params:", serviceParam);

        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, useTTL);

        if (null != propHdr) {
            sendToEachConnection(msg, propHdr);
            sendToNetwork(msg, propHdr);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }

        } else {
            Logging.logCheckedDebug(LOG, "Declining to propagate ", msg, " (No prop header)");
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

        msg = msg.clone();
        int useTTL = Math.min(initialTTL, MAX_TTL);

        Logging.logCheckedDebug(LOG, "Propagating ", msg, "(TTL=", useTTL, ") in group to :\n\tsvc name:", serviceName, "\tsvc params:", serviceParam);

        RendezVousPropagateMessage propHdr = updatePropHeader(msg, getPropHeader(msg), serviceName, serviceParam, useTTL);

        if (null != propHdr) {
            sendToEachConnection(msg, propHdr);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousMeter != null)) {
                rendezvousMeter.propagateToGroup();
            }
        } else {
            Logging.logCheckedDebug(LOG, "Declining to propagate ", msg, " (No prop header)");
        }
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void walk(Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        propagateInGroup(msg, serviceName, serviceParam, initialTTL);
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void walk(Vector<? extends ID> destPeerIDs, Message msg, String serviceName, String serviceParam, int initialTTL) throws IOException {
        propagate(Collections.enumeration(destPeerIDs), msg, serviceName, serviceParam, initialTTL);
    }

    /**
    * 
    * @param peer
    * @return PeerConnection
    * */
    @Override
    public PeerConnection getPeerConnection(ID peer) {
        return connectedRendezVousPeers.get(peer);
    }

    /**
     * @return PeerConnection[]
     * */
    @Override
    protected PeerConnection[] getPeerConnections() {
        return connectedRendezVousPeers.values().toArray(new PeerConnection[0]);
    }

    private void disconnectFromAllRendezVous() {
        for (RendezvousConnection rendezvousConnection : new ArrayList<>(connectedRendezVousPeers.values())) {
            try {
                disconnectFromRendezVous(rendezvousConnection.getPeerID());
            } catch (Exception failed) {
                Logging.logCheckedWarning(LOG, "disconnectFromRendezVous failed for ", rendezvousConnection, "\n", failed);
            }
        }
    }

    /**
     * Handle a disconnection request from a remote peer.
     *
     * @param msg Description of Parameter
     */
    private void processDisconnectRequest(Message msg) {
        try {
            MessageElement messageElement = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, DisconnectRequest);

            if (null != messageElement) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(messageElement);
                PeerAdvertisement peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
                RendezvousConnection rendezvousPeerConnection = connectedRendezVousPeers.get(peerAdvertisement.getPeerID());

                if (null != rendezvousPeerConnection) {                    
                    removeRendezvousPeer(peerAdvertisement.getPeerID(), true);                    
                } else {
                    Logging.logCheckedDebug(LOG, "Ignoring disconnect request from ", peerAdvertisement.getPeerID());
                }
            }
        } catch (IOException failure) {
            Logging.logCheckedWarning(LOG, "Failure processing disconnect request\n", failure);
        }
    }

    /**
     * Add a rendezvous to our collection of rendezvous peers.
     *
     * @param peerAdvertisement  PeerAdvertisement for the rendezvous peer.
     * @param lease The duration of the lease in relative milliseconds.
     */
    private void addRendezVousPeer(PeerAdvertisement peerAdvertisement, long lease) {

        int eventType;

        RendezvousConnection rendezvousConnection;

        synchronized (connectedRendezVousPeers) {
            rendezvousConnection = connectedRendezVousPeers.get(peerAdvertisement.getPeerID());

            if (null == rendezvousConnection) {
                rendezvousConnection = new RendezvousConnection(peerGroup, rendezvousServiceImplementation, peerAdvertisement);
                connectedRendezVousPeers.put(peerAdvertisement.getPeerID(), rendezvousConnection);
                eventType = RendezvousEvent.RDVCONNECT;
            } else {
                eventType = RendezvousEvent.RDVRECONNECT;
            }
        }

        // Check if the peer is already registered.
        if (RendezvousEvent.RDVRECONNECT == eventType) {
            Logging.logCheckedInfo(LOG, "Renewed RDV lease from ", rendezvousConnection);

            // Already connected, just upgrade the lease
            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
                RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter(peerAdvertisement.getPeerID());
                rendezvousConnectionMeter.leaseRenewed(lease);
            }
        } else {
            Logging.logCheckedInfo(LOG, "New RDV lease from ", rendezvousConnection);

            if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
                RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter(peerAdvertisement.getPeerID());
                
                rendezvousConnectionMeter.connectionEstablished(lease);
            }
        }

        rendezvousConnection.connect(peerAdvertisement, lease, Math.min(LEASE_MARGIN, (lease / 2)));
        rendezvousServiceImplementation.generateEvent(eventType, peerAdvertisement.getPeerID());
    }

    /**
     * Remove the specified rendezvous from our collection of rendezvous.
     *
     * @param rdvid the id of the rendezvous to remove.
     * @param requested if true, indicates a requested operation
     */
    private void removeRendezvousPeer(ID rendezvousPeerId, boolean requested) {

        Logging.logCheckedInfo(LOG, "Disconnecting from rendezvous peer ", rendezvousPeerId);

        RendezvousConnection rendezvousPeerConnection;

        synchronized (connectedRendezVousPeers) {
            rendezvousPeerConnection = connectedRendezVousPeers.remove(rendezvousPeerId);
        }

        if (null != rendezvousPeerConnection) {
            if (rendezvousPeerConnection.isConnected()) {
                //TODO
                //Investigate the problem of not sending the message using this method
                
                //sendDisconnect(rendezvousConnection);                                
                //sendDisconnect(rendezvousPeerId, rendezvousConnection.getRendezvousPeerAdvertisement());                                                
                sendBlockingDisconnect(rendezvousPeerConnection);                                                        
                rendezvousPeerConnection.setConnected(false);                
            }
        }                

        rendezvousServiceImplementation.generateEvent(requested ? RendezvousEvent.RDVDISCONNECT : RendezvousEvent.RDVFAILED, rendezvousPeerId);

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            RendezvousConnectionMeter rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter((PeerID) rendezvousPeerId);
            rendezvousConnectionMeter.connectionDisconnected();
        }
    }

    /**
     *  Send lease request to the specified rendezvous peer.
     *
     *  @param rendezvousPeerConnection The peer to which the message should be sent.
     *  @throws IOException Thrown for errors sending the lease request.
     */
    private void sendLeaseRequest(RendezvousConnection rendezvousPeerConnection) throws IOException {

        Logging.logCheckedDebug(LOG, "Sending lease request to ", rendezvousPeerConnection);

        RendezvousConnectionMeter rendezvousConnectionMeter = null;

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousServiceMonitor != null)) {
            rendezvousConnectionMeter = rendezvousServiceMonitor.getRendezvousConnectionMeter(rendezvousPeerConnection.getPeerID().toString());
        }

        Message message = new Message();

        // The request simply includes the local peer advertisement.
        message.replaceMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new TextDocumentMessageElement(ConnectRequest, getPeerAdvertisementDoc(), null));
        rendezvousPeerConnection.sendMessage(message, pName, pParam);

        if (RendezvousMeterBuildSettings.RENDEZVOUS_METERING && (rendezvousConnectionMeter != null)) {
            rendezvousConnectionMeter.beginConnection();
        }
    }

    /**
     * Processes peer connection reply from the rendezvous peer service
     *
     * @param msg Description of Parameter
     */
    private void processConnectedReply(Message msg) {
        // Get the Peer Advertisement of the RDV.
        MessageElement rendezvousPeerAdvertisementElement = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectedRendezvousAdvertisementReply);

        if (null == rendezvousPeerAdvertisementElement) {
            Logging.logCheckedDebug(LOG, "Missing rendezvous peer advertisement");
            return;
        }

        long leaseTime;

        try {
            MessageElement leaseTimeElement = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectedLeaseReply);

            if (leaseTimeElement == null) {
                Logging.logCheckedDebug(LOG, "Missing lease time information");
                return;
            }
            leaseTime = Long.parseLong(leaseTimeElement.toString());
        } catch (Exception e) {
            Logging.logCheckedDebug(LOG, "Parse lease failed with\n", e);
            return;
        }

        ID peerId;
        MessageElement rendezvousPeerIdElement = msg.getMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, ConnectedPeerReply);

        if (rendezvousPeerIdElement == null) {
            Logging.logCheckedDebug(LOG, "Missing rendezvous peer ID");
            return;
        }

        try {
            peerId = IDFactory.fromURI(new URI(rendezvousPeerIdElement.toString()));
        } catch (URISyntaxException exception) {
            Logging.logCheckedDebug(LOG, "Bad rendezvous peer ID");
            return;
        }

        if (leaseTime <= 0) {
            removeRendezvousPeer(peerId, false);
        } else {
            if (connectedRendezVousPeers.containsKey(peerId) || (connectedRendezVousPeers.size() < MAX_RDV_CONNECTIONS)) {
                PeerAdvertisement peerAdvertisement = null;

                try {
                    XMLDocument rendezvousPeerAdvertisementElementDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(rendezvousPeerAdvertisementElement);
                    peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(rendezvousPeerAdvertisementElementDocument);
                } catch (Exception failed) {
                    Logging.logCheckedWarning(LOG, "Failed processing peer advertisement");
                }

                if (peerAdvertisement == null) {
                    Logging.logCheckedDebug(LOG, "Missing rendezvous peer advertisement");
                    return;
                }

                if (!seedingManager.isAcceptablePeer(peerAdvertisement)) {
                    Logging.logCheckedDebug(LOG, "Rejecting lease offer from unacceptable peer : ", peerAdvertisement.getPeerID());

                    // XXX bondolo 20061123 perhaps we should send a disconnect here.
                    return;
                }

                addRendezVousPeer(peerAdvertisement, leaseTime);

                try {
                    DiscoveryService discoveryService = peerGroup.getDiscoveryService();

                    if (discoveryService != null) {
                        // This is not our own peer advertisement so we choose not to share it and keep it for only a short time.
                        discoveryService.publish(peerAdvertisement, leaseTime * 2, 0);
                    }
                } catch (IOException e) {
                    Logging.logCheckedDebug(LOG, "Failed to publish rendezvous peer advertisement\n", e);
                }

                String rendezvousPeerName = peerAdvertisement.getName();

                if (peerAdvertisement.getName() == null) {
                    rendezvousPeerName = peerId.toString();
                }

                Logging.logCheckedDebug(LOG, "Rendezvous connect response : peer = ", rendezvousPeerName, " lease time = ", leaseTime, "ms");
            } else {
                Logging.logCheckedDebug(LOG, "Ignoring lease offer from ", peerId);
                // XXX bondolo 20040423 perhaps we should send a disconnect here.
            }
        }
    }

    /**
     * A timer task for monitoring our active rendezvous connections.
     * <p/>
     * Checks leases, initiates lease renewals, starts new lease requests.
     */
    private class MonitorTask extends SelfCancellingTask {

        /**
         * @inheritDoc
         */
        @Override
        public void execute() {
            try {

                Logging.logCheckedDebug(LOG, "[", peerGroup, "] Periodic rendezvous check");

                if (closed) {
                    return;
                }

                if (!PeerGroupID.WORLD_PEER_GROUP_ID.equals(peerGroup.getPeerGroupID())) {
                    MessageTransport router = rendezvousServiceImplementation.endpoint.getEndpointRouter();

                    if (null == router) {

                        Logging.logCheckedWarning(LOG, "Rendezvous connection stalled until router is started!");

                        // Reschedule another run very soon.
                        this.cancel();
                        scheduleMonitor(2 * TimeUtils.ASECOND);
                        return;
                    }
                }

                List<RendezvousConnection> currentRdvs = new ArrayList<>(connectedRendezVousPeers.values());

                for (RendezvousConnection pConn : currentRdvs) {
                    try {

                        if (!pConn.isConnected()) {
                            Logging.logCheckedInfo(LOG, "[", peerGroup.getPeerGroupID(), "] Lease expired. Disconnected from ", pConn);
                            removeRendezvousPeer(pConn.getPeerID(), false);
                            continue;
                        }

                        if (TimeUtils.toRelativeTimeMillis(pConn.getRenewal()) <= 0) {
                            Logging.logCheckedDebug(LOG, "[", peerGroup.getPeerGroupID(), "] Attempting lease renewal for ", pConn);
                            sendLeaseRequest(pConn);
                        }

                    } catch (Exception e) {
                        Logging.logCheckedWarning(LOG, "[", peerGroup.getPeerGroupID(), "] Failure while checking ", pConn, e);
                    }
                }

                // Not enough Rdvs? Try finding more.
                if (connectedRendezVousPeers.size() < MAX_RDV_CONNECTIONS) {
                    if (seeds.isEmpty()) {
                        seeds.addAll(Arrays.asList(EdgePeerRendezvousServiceClient.this.seedingManager.getActiveSeedRoutes()));
                    }

                    int sentLeaseRequests = 0;

                    while (!seeds.isEmpty() && (sentLeaseRequests < 3)) {
                        RouteAdvertisement aSeed = seeds.remove(0);

                        Message msg = new Message();

                        // The lease request simply includes the local peer advertisement.
                        msg.addMessageElement(RendezVousServiceProvider.RENDEZVOUS_MESSAGE_NAMESPACE_NAME, new TextDocumentMessageElement(ConnectRequest, getPeerAdvertisementDoc(), null));

                        Messenger msgr = null;

                        if (null == aSeed.getDestPeerID()) {
                            // It is an incomplete route advertisement. We are going to assume that it is only a wrapper for a single ea.
                            List<String> seed_eas = aSeed.getDest().getVectorEndpointAddresses();

                            if (!seed_eas.isEmpty()) {
                                EndpointAddress aSeedHost = new EndpointAddress(seed_eas.get(0));

                                msgr = rendezvousServiceImplementation.endpoint.getMessengerImmediate(aSeedHost, null);
                            }
                        } else {
                            // We have a full route, send it to the virtual address of the route!
                            EndpointAddress aSeedHost = new EndpointAddress(aSeed.getDestPeerID(), null, null);

                            msgr = rendezvousServiceImplementation.endpoint.getMessengerImmediate(aSeedHost, aSeed);
                        }

                        if (null != msgr) {
                            try {
                                msgr.sendMessageN(msg, pName, pParam);
                                sentLeaseRequests++;
                            } catch (Exception failed) {
                                // ignored
                            }
                        }
                    }
                } else {
                    // We don't need any of the current seeds. Get new ones when we need them.
                    seeds.clear();
                }

            } catch (Throwable t) {
                Logging.logCheckedWarning(LOG, "Uncaught throwable in thread :", Thread.currentThread().getName(), "\n", t);
            }
        }
    }
}
