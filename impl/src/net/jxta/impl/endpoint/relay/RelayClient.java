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

package net.jxta.impl.endpoint.relay;

import java.util.Collection;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerStateListener;
import net.jxta.id.ID;
import net.jxta.impl.protocol.RelayConfigAdv;
import net.jxta.impl.util.SeedingManager;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.URISeedingManager;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RelayClient manages the relationship with the RelayServer(s)
 */
public class RelayClient implements MessageReceiver, Runnable {
    
    /**
     *  Logger
     */
    private final static transient Logger LOG = Logger.getLogger(RelayClient.class.getName());
    
    private final static long DEFAULT_EXPIRATION = 20L * TimeUtils.AMINUTE;

    private final PeerGroup group;
    private final String serviceName;
    private EndpointService endpoint;
    private final EndpointAddress publicAddress;
    private final String groupName;
    private final String peerId;
    
    private final int maxServers;
    private final long leaseLengthToRequest;
    private final long messengerPollInterval;
    
    private Thread thread = null;
    
    private AtomicBoolean closed = new AtomicBoolean(false);
    
    /**
     * The peergroups which want notification when we connect to a relay.
     */
    private final List<PeerGroup> activeRelayListeners = new ArrayList<PeerGroup>();
    
    /**
     *  The currently connected relays.
     */
    private final Map<EndpointAddress, RouteAdvertisement> activeRelays = new Hashtable<EndpointAddress, RouteAdvertisement>();
    
    /**
     * Our source for relay servers.
     */
    private final SeedingManager seedingManager;
    
    protected RelayServerConnection currentServer = null;
    
    public RelayClient(PeerGroup group, String serviceName, RelayConfigAdv relayConfig) {
        this.group = group;
        this.groupName = group.getPeerGroupID().getUniqueValue().toString();
        
        this.serviceName = serviceName;
        
        maxServers = (-1 != relayConfig.getMaxRelays()) ? relayConfig.getMaxRelays() : RelayTransport.DEFAULT_MAX_SERVERS;
        leaseLengthToRequest = (-1 != relayConfig.getClientLeaseDuration())
                ? relayConfig.getClientLeaseDuration()
                : RelayTransport.DEFAULT_LEASE;
        messengerPollInterval = (-1 != relayConfig.getMessengerPollInterval())
                ? relayConfig.getMessengerPollInterval()
                : RelayTransport.DEFAULT_POLL_INTERVAL;
        
        URISeedingManager uriSeedingManager = new URISeedingManager(relayConfig.getAclUri(), relayConfig.getUseOnlySeeds(), group, serviceName);
        
        for (EndpointAddress aSeeder : Arrays.asList(relayConfig.getSeedRelays())) {
            uriSeedingManager.addSeed(aSeeder.toURI());
        }
        
        for (URI aSeed : Arrays.asList(relayConfig.getSeedingURIs())) {
            uriSeedingManager.addSeedingURI(aSeed);
        }
        
        this.seedingManager = uriSeedingManager;
        
        // sanity check
        
        peerId = group.getPeerID().getUniqueValue().toString();
        publicAddress = new EndpointAddress(RelayTransport.protocolName, peerId, null, null);
        
        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {

            StringBuilder configInfo = new StringBuilder("Configuring Relay Client");
            
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : ").append(group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : ").append(group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : ").append(group.getPeerID());
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tService Name : ").append(serviceName);
            configInfo.append("\n\t\tPublic Address : ").append(publicAddress);
            configInfo.append("\n\t\tMax Relay Servers : ").append(maxServers);
            configInfo.append("\n\t\tMax Lease Length : ").append(leaseLengthToRequest).append("ms.");
            configInfo.append("\n\t\tMessenger Poll Interval : ").append(messengerPollInterval).append("ms.");

            LOG.config(configInfo.toString());
        }
    }
    
    public synchronized boolean startClient() {

        endpoint = group.getEndpointService();
        
        if (endpoint.addMessageTransport(this) == null) {

            Logging.logCheckedSevere(LOG, "Transport registration refused");
            return false;

        }
        
        // start the client thread
        thread = new Thread(group.getHomeThreadGroup(), this, "Relay Client Worker Thread for " + publicAddress);
        thread.setDaemon(true);
        thread.start();
        
        Logging.logCheckedInfo(LOG, "Started client : ", publicAddress);
        
        return true;
    }

    /**
     * Returns the list of currently connected relay peers.
     *
     * @return a map of peer ids and corresponding route advertisements
     */
    public Map<PeerID,RouteAdvertisement> getConnectedRelays() {

        // Preparing result
        Map<PeerID,RouteAdvertisement> Result = new HashMap<PeerID,RouteAdvertisement>();

        Collection<RouteAdvertisement> Temp = this.activeRelays.values();
        Iterator<RouteAdvertisement> TheIter = Temp.iterator();

        while (TheIter.hasNext()) {

            RouteAdvertisement TempRA = TheIter.next();

            // Adding connected relay entries
            Result.put(TempRA.getDestPeerID(), TempRA);
            
        }

        // Return result
        return Result;

    }
    
    public synchronized void stopClient() {
        
        if (closed.get()) return;
        
        closed.set(true);
        
        endpoint.removeMessageTransport(this);
        
        // make sure the thread is not running
        Thread tempThread = thread;
        
        thread = null;
        if (tempThread != null) {
            tempThread.interrupt();
        }
        
        Logging.logCheckedInfo(LOG, "Stopped client : " + publicAddress);
        
    }
    
    /**
     * {@inheritDoc}
     */
    public Iterator<EndpointAddress> getPublicAddresses() {
        
        return Collections.singletonList(publicAddress).iterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public String getProtocolName() {
        return RelayTransport.protocolName;
    }
    
    /**
     * {@inheritDoc}
     */
    public EndpointService getEndpointService() {
        return endpoint;
    }
    
    /**
     *  Logic for the relay client
     *
     *  <ol>
     *      <li>Pick a relay server to try</li>
     *      <li>try getting a messenger to relay server, if can not get messenger, start over</li>
     *      <li>use the messenger to send a connect message</li>
     *     <li> wait for a response, if there is no response or a disconnect response, start over</li>
     *      <li>while still connected
     *          <ol>
     *          <li>renew the lease as needed and keep the messenger connected</li>
     *          <ol></li>
     *  </ol>
     *
     *  <p/>FIXME 20041102 bondolo The approach used here is really, really
     *  stupid. The calls to <code>connectToRelay()</code> will not return if a
     *  connection to a relay is achieved. This makes continued iteration over
     * seeds after return incredibly silly. <code>connectToRelay()</code> only
     *  returns when it can <b>NO LONGER CONNECT</b> to the relay. The only
     *  hack I can think of to subvert this is to stop iteration of advs/seeds
     *  if <code>connectToRelay()</code> takes a long time. bizarre.
     */
    public void run() {

        Logging.logCheckedInfo(LOG, "Start relay client thread");
        
        try {
            long nextConnectAttemptAt = 0;
            
            RdvAdvertisement referral = null;
            List<RouteAdvertisement> allSeeds = null;
            long gotLastSeedsAt = 0;
            
            // run until the service is stopped
            while (!closed.get()) {
                // Attempt to use any referral immediately.
                if (null != referral) {
                    RouteAdvertisement relayRoute = referral.getRouteAdv();

                    relayRoute.setDestPeerID(referral.getPeerID());
                    
                    referral = connectToRelay(new RelayServerConnection(this, relayRoute));
                    
                    continue;
                }
                
                // Sleep until it is time for the next connection attempt.
                long untilNextConnectAttempt = TimeUtils.toRelativeTimeMillis(nextConnectAttemptAt);
                
                if (untilNextConnectAttempt > 0) {
                    try {
                        Thread.sleep(untilNextConnectAttempt);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        
                        continue;
                    }
                }
                
                // Don't allow next connection attempt to start any sooner than this.
                nextConnectAttemptAt = TimeUtils.toAbsoluteTimeMillis(30 * TimeUtils.ASECOND);
                
                // Get seeds if we need them or the ones we have are old.
                if ((TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), gotLastSeedsAt) > (5 * TimeUtils.AMINUTE))
                        || allSeeds.isEmpty()) {
                    allSeeds = new ArrayList<RouteAdvertisement>(Arrays.asList(seedingManager.getActiveSeedRoutes()));
                    gotLastSeedsAt = TimeUtils.timeNow();
                }
                
                // Try seeds until we get a connection, a referral or are closed.
                while ((null == referral) && !allSeeds.isEmpty() && !closed.get()) {
                    RouteAdvertisement aSeed = allSeeds.remove(0);
                    
                    if (null == aSeed.getDestPeerID()) {
                        // It is an incomplete route advertisement. We are going to assume that it is only a wrapper for a single ea.
                        Vector<String> seed_eas = aSeed.getDest().getVectorEndpointAddresses();
                        
                        if (!seed_eas.isEmpty()) {
                            EndpointAddress aSeedHost = new EndpointAddress(seed_eas.get(0));
                            Logging.logCheckedFine(LOG, "Attempting relay connect to : ", aSeedHost);
                            referral = connectToRelay(new RelayServerConnection(this, aSeedHost));
                        }
                    } else {
                        // We have a full route, send it to the virtual address of the route!
                        Logging.logCheckedFine(LOG, "Attempting relay connect to : ", aSeed.getDestPeerID());
                        referral = connectToRelay(new RelayServerConnection(this, aSeed));
                    }
                }
            }
            
        } catch (Throwable all) {

            Logging.logCheckedSevere(LOG, "Uncaught Throwable in thread :", Thread.currentThread().getName(), "\n", all);
            
        } finally {

            thread = null;
            Logging.logCheckedInfo(LOG, "stop client thread");
            
        }
    }
    
    protected boolean isRelayConnectDone() {
        return (thread == null || Thread.currentThread() != thread);
    }
    
    /**
     *  @param  server  The relay server to connect to
     *  @return The advertisement of an alternate relay server to try.
     */
    public RdvAdvertisement connectToRelay(RelayServerConnection server) {
        
        Logging.logCheckedFine(LOG, "Connecting to ", server);
        
        RdvAdvertisement referral = null;
        
        // make this the current server
        currentServer = server;
        
        // try getting a messenger to the relay peer
        if (!server.createMessenger(leaseLengthToRequest)) return referral;
        
        Logging.logCheckedFine(LOG, "got messenger ", server);
        
        // check the peerId of the relay peer
        if (server.logicalAddress != null && "jxta".equals(server.logicalAddress.getProtocolName())) {
            server.peerId = server.logicalAddress.getProtocolAddress();
        }
        
        // make sure that the peerId was found.
        if (server.peerId == null) {
            if (server.messenger != null) {
                server.sendDisconnectMessage();
                server.messenger.close();
            }
            return referral;
        }
        
        Logging.logCheckedFine(LOG, "got peerId ", server);
        
        synchronized (this) {

            // wait for a response from the server
            // There is no real damage other than bandwidth usage in sending
            // a message on top of the connection request, so we realy do not
            // wait very long before doing it.
            long requestTimeoutAt = TimeUtils.toAbsoluteTimeMillis(5 * TimeUtils.ASECOND);
            
            while (currentServer != null && currentServer.leaseLength == 0 && !isRelayConnectDone()) {
                long waitTimeout = requestTimeoutAt - System.currentTimeMillis();
                
                if (waitTimeout <= 0) {
                    // did not receive the response in time ?
                    break;
                }
                
                try {
                    wait(waitTimeout);
                } catch (InterruptedException e) {
                    // ignore interrupt
                    Logging.logCheckedFine(LOG, "wait got interrupted early\n", e);
                }
                
                Logging.logCheckedFine(LOG, "wait done");
                
            }
        }
        
        if (currentServer == null) {
            return server.alternateRelayAdv;
        }
        
        if (isRelayConnectDone()) {
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer = null;
            return server.alternateRelayAdv;
        }
        
        // If we did not get a lease in the first 5 secs, maybe it is because
        // the server knows us from a previous session. Then it will wait for
        // a lease renewal message before responding, not just the connection.
        // Send one and wait another 15.
        if (currentServer.leaseLength == 0) {
            
            currentServer.sendConnectMessage(leaseLengthToRequest);
            
            synchronized (this) {
                
                // wait for a response from the server
                long requestTimeoutAt = TimeUtils.toAbsoluteTimeMillis(15 * TimeUtils.ASECOND);
                
                while (currentServer != null && currentServer.leaseLength == 0 && !isRelayConnectDone()) {
                    long waitTimeout = requestTimeoutAt - System.currentTimeMillis();
                    
                    if (waitTimeout <= 0) {
                        // did not receive the response in time ?
                        break;
                    }
                    
                    try {
                        wait(waitTimeout);
                    } catch (InterruptedException e) {
                        // ignore interrupt
                        Logging.logCheckedFine(LOG, "wait got interrupted early\n", e);
                    }
                    
                    Logging.logCheckedFine(LOG, "wait done");
                    
                }
            }
        }
        
        // If we had a messenger but are going to give up that relay server because it is
        // not responsive or rejected us. Make sure that the messenger is closed.
        if (currentServer == null) {
            
            Logging.logCheckedFine(LOG, "did not get connect from ", server);
            
            // return any alternate relay advertisements
            return server.alternateRelayAdv;
            
        }
        
        if (currentServer.relayAdv == null || currentServer.leaseLength == 0 || isRelayConnectDone()) {
            
            Logging.logCheckedFine(LOG, "did not get connect from ", server);
            
            if (currentServer.messenger != null) {
                currentServer.sendDisconnectMessage();
                currentServer.messenger.close();
            }
            currentServer = null;
            
            // return any alternate relay advertisements
            return server.alternateRelayAdv;
        }
        
        Logging.logCheckedFine(LOG, "Connected to ", server);
        
        RouteAdvertisement holdAdv = server.relayAdv;
        EndpointAddress holdDest = server.logicalAddress;
        
        // register this relay server
        addActiveRelay(holdDest, holdAdv);
        
        // maintain the relay server connection
        referral = maintainRelayConnection(server);
        
        // unregister this relay server
        removeActiveRelay(holdDest, holdAdv);
        
        return referral;
    }
    
    // FIXME: jice@jxta.org 20030212. This is junk code: that should be a
    // method of RelayServerConnection and at least not refer to currentServer
    // other than to assign the reference.
    protected RdvAdvertisement maintainRelayConnection(RelayServerConnection server) {
        
        Logging.logCheckedFine(LOG, "maintainRelayConnection() start ", currentServer);
        
        if (server == null) {
            Logging.logCheckedFine(LOG, "RelayConnection() failed at start ", currentServer);
            return null;
        }
        
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            long renewLeaseAt = currentServer.leaseObtainedAt + currentServer.leaseLength / 3;
            long waitTimeout = 0;
            
            // This will be true if we need to do the first lease renewal early
            // (that is at the time of the next connection check).
            // We'll do that if we did not know the relay server's adv (seed).
            // In that case we told the relay server to send us its own
            // adv, else we told it to send us some alternate adv (we have to
            // chose). In the former case, we want to do a lease connect
            // request soon so that the server has an opportunity to send us
            // the alternate adv that we did not get during initial connection.
            
            boolean earlyRenew = currentServer.seeded;
            
            while (currentServer != null && !isRelayConnectDone()) {
                // calculate how long to wait
                waitTimeout = renewLeaseAt - currentTime;
                
                // check that the waitTimeout is not greater than the messengerPollInterval
                // We want to make sure that we poll. Most of the time it cost nothing.
                // Also, if we urgently need to renew our lease we may wait
                // less, but if we fail to get our lease renewed in time, the
                // delay may become negative. In that case we do not want
                // to start spinning madly. The only thing we can do is just
                // wait some arbitrary length of time for the lease to be
                // renewed. (If that gets badly overdue, we should probably
                // give up on that relay server, though).
                if (waitTimeout > messengerPollInterval || waitTimeout <= 0) {
                    waitTimeout = messengerPollInterval;
                }
                
                Logging.logCheckedFine(LOG, "waitTimeout=", waitTimeout, " server=", currentServer);
                
                try {
                    wait(waitTimeout);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
                
                Logging.logCheckedFine(LOG, "wait done, server=", currentServer);
                
                // make sure the server did not disconnect while waiting
                if (currentServer == null) {
                    break;
                }
                
                // get the current time
                currentTime = System.currentTimeMillis();
                
                Logging.logCheckedFine(LOG, "check messenger ", currentServer);
                
                // check if the messenger is still open
                if (currentServer.messenger.isClosed()) {
                    
                    Logging.logCheckedFine(LOG, "Server connection broken");
                    
                    // See if we can re-open, that happens often.
                    // That's a reason to renew the connection,
                    // Not a reason to give up on the server yet.
                    // Note we do not renew the lease. This is a transient
                    // and if the server forgot about us, it will respond
                    // to the connection alone. Otherwise, we'd rather avoid
                    // getting a response, since in some cases http connections
                    // close after each received message.
                    if (!currentServer.createMessenger(currentServer.leaseLength)) {
                        
                        Logging.logCheckedFine(LOG, "Server connection NOT re-established");
                        
                        // lost connection to relay server
                        currentServer = null;
                        break;
                        
                    }
                    
                    Logging.logCheckedFine(LOG, "Server connection re-established");
                    
                    // getMessenger asks for a new lease.
                    // In the meantime, we'll just assume our old lease is
                    // still current and that the messenger breakage was just
                    // a transient.
                    if (!isRelayConnectDone()) {
                        continue;
                    }
                }
                
                // We've been asked to leave. Be nice and tell the
                // server about it.
                if (isRelayConnectDone()) {
                    break;
                }
                
                // check if the lease needs to be renewed
                renewLeaseAt = currentServer.leaseObtainedAt + currentServer.leaseLength / 3;
                
                if (currentTime >= renewLeaseAt || earlyRenew) {
                    
                    earlyRenew = false;
                    
                    Logging.logCheckedFine(LOG, "renew lease ", currentServer);
                    
                    // If we do not receive any response to our lease renewals
                    // (that is the response is overdue badly), then we give
                    // up and try another relayServer. We give up after 4 minutes
                    // because if we go as far as 5 we start overshooting other
                    // timeouts such as the local peer becoming a rdv in a sub-group.
                    // This later timeout is usually set to 5 minutes or more.
                    
                    if ((currentTime > currentServer.leaseObtainedAt + currentServer.leaseLength / 3 + 4 * TimeUtils.AMINUTE)
                            || (!currentServer.sendConnectMessage(leaseLengthToRequest))) {
                        
                        Logging.logCheckedInfo(LOG, "renew lease failed", currentServer);
                        
                        if (currentServer.messenger != null) {
                            currentServer.messenger.close();
                        }

                        currentServer.messenger = null;
                        currentServer.peerId = null;
                        currentServer.leaseLength = 0;
                        currentServer.leaseObtainedAt = 0;
                        currentServer.relayAdv = null;
                        currentServer = null;

                        break;

                    }
                }
            }
        }
        
        if (isRelayConnectDone() && currentServer != null) {
            currentServer.sendDisconnectMessage();
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer.messenger = null;
            currentServer.peerId = null;
            currentServer.leaseLength = 0;
            currentServer.leaseObtainedAt = 0;
            currentServer.relayAdv = null;
            // Make sure that we will not suggest an alternate
            // since we're asked to terminate.
            currentServer.alternateRelayAdv = null;
            
            currentServer = null;
        }
        
        Logging.logCheckedFine(LOG, "maintainRelayConnection() terminated ", currentServer);
        
        return server.alternateRelayAdv;
    }
    
    protected synchronized void handleResponse(Message message, EndpointAddress dstAddr) {
        
        Logging.logCheckedFine(LOG, "handleResponse ", currentServer);
        
        // ignore all responses if there is not a current server
        if (currentServer == null) {
            return;
        }
        
        // get the request, make it lowercase so that case is ignored
        String response = RelayTransport.getString(message, RelayTransport.RESPONSE_ELEMENT);
        
        if (response == null) return;
        
        response = response.toLowerCase();
        
        Logging.logCheckedFine(LOG, "response = ", response);
        
        // check if a relay advertisement was included
        RdvAdvertisement relayAdv = null;
        
        MessageElement advElement = message.getMessageElement(RelayTransport.RELAY_NS, RelayTransport.RELAY_ADV_ELEMENT);
        
        if (null != advElement) {
            try {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(advElement);
                Advertisement adv = AdvertisementFactory.newAdvertisement(asDoc);
                
                if (adv instanceof RdvAdvertisement) {
                    relayAdv = (RdvAdvertisement) adv;
                }
                
            } catch (IOException e) {
                Logging.logCheckedFine(LOG, "Could not read Relay RdvAdvertisement\n", e);
            }
        }
        
        // WATCHOUT: this is not a pid, just the unique string portion.
        String serverPeerId = dstAddr.getServiceParameter();
        
        // only process the request if a client peer id was sent
        if (serverPeerId == null) {
            return;
        }
        
        // ignore all responses that are not from the current server
        if (!serverPeerId.equals(currentServer.peerId)) {
            return;
        }
        
        Logging.logCheckedFine(LOG, "serverPeerId = ", serverPeerId);
        
        // Figure out which response it is
        if (RelayTransport.CONNECTED_RESPONSE.equals(response)) {

            // Connect Response
            Logging.logCheckedFine(LOG, "connected response for ", currentServer);
            
            String responseLeaseString = RelayTransport.getString(message, RelayTransport.LEASE_ELEMENT);
            
            long responseLease = 0;
            
            if (responseLeaseString != null) {

                try {
                    responseLease = Long.parseLong(responseLeaseString);
                } catch (NumberFormatException e) {
                    Logging.logCheckedWarning(LOG, "could not parse response lease string\n", e);
                }
                
            }
            
            // make sure the lease is valid
            if (responseLease <= 0) {
                // invalid lease value
                return;
            }
            
            // update the lease values
            currentServer.leaseLength = responseLease;
            currentServer.leaseObtainedAt = System.currentTimeMillis();
            
            // Since we got the lease, if we requested a queue flush, it's
            // now done. We never send it with a new messenger creation, but
            // when the server already has us as a client it does not respond
            // to connections through messenger creation, so we're sure we
            // will have to send an explicit connect message before we get
            // a response. So, we're sure it's done if it was needed.
            currentServer.flushNeeded = false;
            
            if (relayAdv != null) {
                // Set it only if it is the server's own. Else it got
                // published. Still set alternateRelayAdv so that we
                // can return something that could be usefull when this
                // connection breaks.
                PeerID pidOfAdv = relayAdv.getPeerID();
                String pidOfAdvUnique = pidOfAdv.getUniqueValue().toString();
                
                if (currentServer.peerId.equals(pidOfAdvUnique)) {
                    currentServer.relayAdv = relayAdv.getRouteAdv();
                    // Fix the embedded route adv !
                    currentServer.relayAdv.setDestPeerID(pidOfAdv);
                } else {
                    currentServer.alternateRelayAdv = relayAdv;
                }
            }
            
            notifyAll();
            
        } else if (RelayTransport.DISCONNECTED_RESPONSE.equals(response)) {

            // Disconnect Response
            Logging.logCheckedFine(LOG, "disconnected from ", currentServer);
            
            // If our request was denied, the adv that came back is
            // always an alternate one.
            currentServer.alternateRelayAdv = relayAdv;
            
            if (currentServer.messenger != null) {
                currentServer.messenger.close();
            }
            currentServer.messenger = null;
            currentServer.peerId = null;
            currentServer.leaseLength = 0;
            currentServer.leaseObtainedAt = 0;
            currentServer.relayAdv = null;
            currentServer = null;
            notifyAll();
        }
        
        Logging.logCheckedFine(LOG, "response handled for ", currentServer);

    }
    
    static class RelayServerConnection {

        private final RelayClient client;
        
        private Messenger messenger = null;
        private EndpointAddress logicalAddress = null;
        private String peerId = null;
        private long leaseLength = 0;
        private long leaseObtainedAt = 0;
        
        // If seeded out of a raw address, we have relayAddress.
        // relayAdv comes only later.
        public RouteAdvertisement relayAdv = null;
        private EndpointAddress relayAddress = null;
        
        private RdvAdvertisement alternateRelayAdv = null;
        private boolean seeded = false;
        private boolean flushNeeded = true; // true until we know it's been done
        
        private final MessengerStateListener failureListener = new MessengerStateListener() {
			
			public boolean messengerStateChanged(int newState) {
				if((newState & Messenger.TERMINAL) != 0) {
					// wake up the relay client, so that it will attempt to
					// re-establish the connection or switch to a different
					// relay server.
					synchronized(client) {
						client.notifyAll();
					}
				}
				return true;
			}
		};
        
        protected RelayServerConnection(RelayClient client, EndpointAddress addr) {
            this.client = client;
            relayAddress = new EndpointAddress(addr, null, null);
            seeded = true;
        }
        
        protected RelayServerConnection(RelayClient client, RouteAdvertisement relayAdv) {
            this.client = client;
            this.relayAdv = relayAdv;
        }
        
        protected boolean createMessenger(long leaseLengthToRequest) {
            
            // make sure the old messenger is closed
            if (messenger != null) {
                messenger.close();
                messenger = null;
            }
            
            List<String> endpointAddresses = null;
            
            // check for a relay advertisement
            if (relayAdv != null) {
                AccessPointAdvertisement accessPointAdv = relayAdv.getDest();
                
                if (accessPointAdv != null) {
                    endpointAddresses = accessPointAdv.getVectorEndpointAddresses();
                }
            } else {
                // silly but if we use getVetorEndpointAddresses, we get
                // strings. It's realy simpler to have only one kind of obj
                // inthere.
                endpointAddresses = new ArrayList<String>(1);
                endpointAddresses.add(relayAddress.toString());
            }
            
            Logging.logCheckedFine(LOG, "createMessenger to ", endpointAddresses);
            
            // make sure we found some endpoint addresses to try
            if (endpointAddresses == null) {
                return false;
            }
            
            // try each endpoint address until one is successful
            for (String s : endpointAddresses) {

                if (s == null) continue;
                
                EndpointAddress addr = new EndpointAddress(s);

                Logging.logCheckedFine(LOG, "find transport for ", addr);
                
                // get the list of messengers on this endpoint
                Iterator transports = client.endpoint.getAllMessageTransports();

                while (transports.hasNext() && messenger == null) {

                    MessageTransport transport = (MessageTransport) transports.next();

                    // only try transports that are senders and allow routing
                    if (transport instanceof MessageSender && ((MessageSender) transport).allowsRouting()) {

                        Logging.logCheckedFine(LOG, "try transport ", transport);
                        
                        if (addr.getProtocolName().equals(transport.getProtocolName())) {
                            // NOTE: here we're creating a messenger.
                            // For risk management reason, we refrain from
                            // including the flush request at this time in
                            // this. There is the possibility that the
                            // connection will be repeatedly established
                            // by the transport in our bakck, and would keep
                            // including the flush request ! Normaly this
                            // does not matter because the server should
                            // disregard it when it come in that way, but
                            // still, let's be defensive. We will still send
                            // the flush in a subsequent explicit message.
                            String reqStr = RelayTransport.createConnectString(leaseLengthToRequest, relayAdv == null, false);
                            // NOTE: this is simulating address mangling by CrossgroupMessenger.
                            // The real service param is after the "/" in the below serviceParam arg.
                            EndpointAddress addrToUse = new EndpointAddress(addr, "EndpointService:" + client.groupName
                                    ,
                                    client.serviceName + "/" + reqStr);

                            messenger = ((MessageSender) transport).getMessenger(addrToUse, null);
                            if (messenger != null && messenger.isClosed()) {
                                messenger = null;
                            }
                            if (messenger != null) {
                                logicalAddress = messenger.getLogicalDestinationAddress();
                                // We're using a known adv, which means that
                                // we did not ask to get the adv back.
                                // Make sure that we do not keep going with
                                // an adv for the wrong peer. That can happen.
                                if (relayAdv != null && !RelayTransport.addr2pid(logicalAddress).equals(relayAdv.getDestPeerID())) {
                                    // oops, wrong guy !
                                    messenger.close();
                                    messenger = null;
                                    logicalAddress = null;
                                } else {
                                	messenger.addStateListener(failureListener);
                                }
                                // In case it was not given, set relayAddress
                                // for toString purposes.
                                relayAddress = addr;
                            }
                        }
                    }
                }
            }

            Logging.logCheckedFine(LOG, "messenger=", messenger);
            
            return (messenger != null);
        }
        
        protected boolean sendConnectMessage(long leaseLengthToRequest) {
            if (messenger == null || messenger.isClosed()) {
                return false;
            }
            
            Message message = RelayTransport.createConnectMessage(leaseLengthToRequest, (relayAdv == null), flushNeeded);
            
            try {

                messenger.sendMessage(message, "EndpointService:" + client.groupName, client.serviceName + "/" + client.peerId);

            } catch (IOException e) {

                Logging.logCheckedWarning(LOG, "could not send connect message\n", e);
                
                // connection attempt failed
                return false;

            }
            
            return true;
        }
        
        protected boolean sendDisconnectMessage() {
            if (messenger == null || messenger.isClosed()) {
                return false;
            }
            
            Message message = RelayTransport.createDisconnectMessage();
            
            try {

                messenger.sendMessage(message, "EndpointService:" + client.groupName, client.serviceName + "/" + client.peerId);

            } catch (IOException e) {

                Logging.logCheckedWarning(LOG, "could not send disconnect message\n", e);
                
                // connection attempt failed
                return false;

            }
            
            return true;
        }
        
        /**
         *  {@inheritDoc}
         */
        @Override
        public String toString() {
            
            return
                    ((relayAddress == null) ? "(adv to " + relayAdv.getDestPeerID() + ")" : relayAddress.toString()) + " ["
                    + leaseLength + ", " + leaseObtainedAt + "] ";
        }
    }
    
    /**
     * Register an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     */
    public synchronized boolean addActiveRelayListener(PeerGroup service) {
        
        boolean added = false;
        
        if (!activeRelayListeners.contains(service)) {

            Logging.logCheckedFine(LOG, "Register group to relay connection ", service.getPeerGroupName());
            activeRelayListeners.add(service);
            
            added = true;
        }
        
        return added;
    }
    
    /**
     * Unregister an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     */
    public synchronized boolean removeActiveRelayListener(PeerGroup service) {
        activeRelayListeners.remove(service);
        
        return true;
    }
    
    /**
     * Notify of a new relay connection
     *
     */
    public synchronized boolean addActiveRelay(EndpointAddress address, RouteAdvertisement relayRoute) {
        
        Logging.logCheckedFine(LOG, "notify add relay connection for ", address);
        
        // need to notify all our listeners
        for (PeerGroup pg : activeRelayListeners) {
            addRelay(pg, relayRoute);
        }

        // maintain the list of active relays
        activeRelays.put(address, relayRoute);
        return true;
    }
    
    /**
     * Notify of a relay connection removal
     *
     */
    public synchronized boolean removeActiveRelay(EndpointAddress address, RouteAdvertisement relayRoute) {
        
        // need to notify all our listeners

        for (PeerGroup pg : activeRelayListeners) {
            removeRelay(pg, relayRoute);
        }

        activeRelays.remove(address);
        
        return true;
    }
    
    /**
     * Register an active Relay to the endpoint. This is done
     * so the Route Advertisement of the PeerAdvertisement is
     * updated
     *
     * @param relayRoute address of the relay to add
     */
    private void addRelay(PeerGroup pg, RouteAdvertisement relayRoute) {
        
        ID assignedID = PeerGroup.endpointClassID;
        
        try {

            // get the advertisement of the associated endpoint address as we
            // need to get the peer Id and available route
            
            // update our own peer advertisement
            PeerAdvertisement padv = pg.getPeerAdvertisement();
            XMLDocument myParam = (XMLDocument) padv.getServiceParam(assignedID);
            
            RouteAdvertisement route = null;
            
            if (myParam == null) {

                // we should have found a route here. This is not good
                Logging.logCheckedWarning(LOG, "no route found in peer adv");
                return;

            } else {

                Enumeration<XMLElement> paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                XMLElement param = null;
                
                if (paramChilds.hasMoreElements()) {
                    param = paramChilds.nextElement();
                    route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);

                }

            }
            
            if (route == null) return; // we should have a route here
            
            // ready to stich the Relay route in our route advertisement
            Logging.logCheckedFine(LOG, "found route info for local peer \n", route.display());
            
            // update the new hops info
            Logging.logCheckedFine(LOG, "OLD route info to local peer \n", route.display());
            
            // If we already have the relay in our list of hops, remove it.
            // The new version can only be more accurate.
            route.removeHop(relayRoute.getDestPeerID());
            
            // Get a hold of the hops list AFTER removing: removeHop
            // rebuilds the vector !
            Vector<AccessPointAdvertisement> hops = route.getVectorHops();
            
            // Create the new relay Hop
            hops.add(relayRoute.getDest());
            
            // update the new hops info
            Logging.logCheckedFine(LOG, "NEW route info to local peer", route.display());
            
            // create the new param route
            myParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            StructuredTextDocument xptDoc = (StructuredTextDocument)
                    route.getDocument(MimeMediaType.XMLUTF8);
            
            StructuredDocumentUtils.copyElements(myParam, myParam, xptDoc);
            
            padv.putServiceParam(assignedID, myParam);
            
            // publish the new peer advertisement
            DiscoveryService discovery = pg.getDiscoveryService();
            
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }

        } catch (Exception ex) {
            
            Logging.logCheckedFine(LOG, "exception adding relay route\n", ex);
            
        }
        
    }
    
    /**
     * remove relay hop from the peer advertisement
     *
     * @param group which peer advertisement needs to be updated
     * @param relayRoute address of the relay to be removed
     */
    private void removeRelay(PeerGroup group, RouteAdvertisement relayRoute) {
        
        // we can keep the advertisement for now (should remove it)
        // remove the relay from its active list
        ID assignedID = PeerGroup.endpointClassID;
        PeerID relayPid = relayRoute.getDestPeerID();
        
        try {
            // get the advertisement of the associated endpoint address as we
            // need to get the peer Id and available route
            
            PeerAdvertisement padv;
            
            // update our peer advertisement
            padv = group.getPeerAdvertisement();
            XMLDocument myParam = (XMLDocument) padv.getServiceParam(assignedID);
            
            RouteAdvertisement route = null;
            
            if (myParam == null) {

                // no route found we should really have one
                Logging.logCheckedWarning(LOG, "no route found in peer adv");
                return;

            } else {

                Enumeration<XMLElement> paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                XMLElement param = null;
                
                if (paramChilds.hasMoreElements()) {
                    param = paramChilds.nextElement();
                    route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement( param);
                }
                
            }
            
            if (route == null) {
                return;
            } // we should have a route here
            
            // update the new hops info
            route.removeHop(relayPid);
            
            Logging.logCheckedFine(LOG, "new route info to the peer", route.display());
            
            // create the new param route
            myParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            XMLDocument xptDoc = (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8);
            
            StructuredDocumentUtils.copyElements(myParam, myParam, xptDoc);
            
            padv.putServiceParam(assignedID, myParam);
            
            // publish the new advertisement
            DiscoveryService discovery = group.getDiscoveryService();
            
            if (discovery != null) 
                discovery.publish(padv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            
        } catch (Throwable theMatter) {

            Logging.logCheckedWarning(LOG, "Failed adding relay route", theMatter);
            
        }
    }
    
    /**
     * return the list of connected relays
     */
    public Vector<AccessPointAdvertisement> getActiveRelays(PeerGroup pg) {

        Logging.logCheckedFine(LOG, "get active Relays list");
        
        Vector<AccessPointAdvertisement> hops = new Vector<AccessPointAdvertisement>();

        for (RouteAdvertisement route : activeRelays.values()) {
            
            try {

                // publish our route if pg is not null
                if (pg != null) {

                    DiscoveryService discovery = pg.getDiscoveryService();

                    if (discovery != null) {
                        Logging.logCheckedFine(LOG, "publishing route to active relay ", route.display());
                        discovery.publish(route, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
                    }

                }

            } catch (Exception ex) {

                Logging.logCheckedWarning(LOG, "error publishing active relay\n", ex);
                continue;
                
            }

            hops.add(route.getDest());
        }

        return hops;        
    }

}
