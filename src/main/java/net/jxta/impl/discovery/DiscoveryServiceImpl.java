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
 *
 *  This software consists of voluntary contributions made by many individuals
 *  on behalf of Project JXTA. For more information on Project JXTA, please see
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.impl.discovery;

import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.OutgoingMessageEvent;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.cm.CacheManager;
import net.jxta.impl.cm.SrdiManager;
import net.jxta.impl.cm.Srdi;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.DiscoveryConfigAdv;
import net.jxta.impl.protocol.DiscoveryQuery;
import net.jxta.impl.protocol.DiscoveryResponse;
import net.jxta.impl.protocol.ResolverQuery;
import net.jxta.impl.protocol.ResolverResponse;
import net.jxta.impl.protocol.SrdiMessageImpl;
import net.jxta.impl.resolver.InternalQueryHandler;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.*;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.resolver.ResolverService;
import net.jxta.resolver.SrdiHandler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Discovery Service implementation provides a mechanism to discover
 * Advertisements using the Resolver service and SRDI.
 * <p/>
 * This implementation uses the standard JXTA Peer Discovery Protocol (PDP).
 * <p/>
 * The DiscoveryService service also provides a way to obtain information
 * from a specified peer and request other peer advertisements, this method is
 * particularly useful in the case of a portal where new relationships may be
 * established starting from a predetermined peer (perhaps described in address
 * book, or through an invitation).
 *
 * @see net.jxta.discovery.DiscoveryService
 * @see net.jxta.protocol.DiscoveryQueryMsg
 * @see net.jxta.impl.protocol.DiscoveryQuery
 * @see net.jxta.protocol.DiscoveryResponseMsg
 * @see net.jxta.impl.protocol.DiscoveryResponse
 * @see net.jxta.resolver.ResolverService
 * @see <a href="https://jxta-spec.dev.java.net/nonav/JXTAProtocols.html#proto-pdp" target="_blank">JXTA Protocols Specification : Peer Discovery Protocol</a>
 */
public class DiscoveryServiceImpl implements DiscoveryService, InternalQueryHandler, RendezvousListener, SrdiHandler, SrdiManager.SrdiPushEntriesInterface {

    private final static Logger LOG = Logging.getLogger(DiscoveryServiceImpl.class.getName());

    /**
     * adv types
     */
    final static String[] dirname = {"Peers", "Groups", "Adv"};

    /**
     * The Query ID which will be associated with remote publish operations.
     */
    private final static int REMOTE_PUBLISH_QUERYID = 0;
    private final static String SRDI_INDEXER_FILE_NAME = "discoverySrdi";

    /**
     * The current discovery query ID. static to make debugging easier.
     */
    private final static AtomicInteger qid = new AtomicInteger(0);

    /**
     * The maximum number of responses we will return for ANY query.
     */
    private final static int MAX_RESPONSES = 50;

    /**
     * The cache manager we're going to use to cache jxta advertisements
     */
    protected CacheManager cacheManager;
    private PeerGroup peerGroup = null;

    /**
     * assignedID as a String.
     */
    private String handlerName = null;
    private ModuleImplAdvertisement implAdvertisement = null;
    private ResolverService resolver = null;
    private RendezVousService rendezvous = null;
    private MembershipService membership = null;
    private PeerID localPeerId = null;
    private boolean localonly = false;
    private boolean alwaysUseReplicaPeer = false;
    private boolean forwardBelowThreshold = false;
    private boolean stopped = true;

    /**
     * The table of global discovery listeners.
     */
    private final Set<DiscoveryListener> listeners = new HashSet<>();

    /**
     * The table of discovery query listeners.
     */
    private final Map<Integer, DiscoveryListener> queryListeners = new HashMap<>();
    private final String checkPeerAdvLock = "Check/Update PeerAdvertisement Lock";
    private PeerAdvertisement lastPeerAdv = null;
    private int lastModCount = -1;
    private boolean isRdv = false;
    private Srdi srdiIndex = null;
    private SrdiManager srdiManager = null;
    private final long runInterval = 30 * TimeUtils.ASECOND;

    /**
     * Encapsulates current Membership Service credential.
     */
    final static class CurrentCredential {
        /**
         * The current default credential
         */
        final Credential credential;
        /**
         * The current default credential in serialized XML form.
         */
        final XMLDocument credentialDoc;

        CurrentCredential(Credential credential, XMLDocument credentialDoc) {
            this.credential = credential;
            this.credentialDoc = credentialDoc;
        }
    }

    /**
     * The current Membership service default credential.
     */
    CurrentCredential currentCredential;

    /**
     * Listener we use for membership credential change events.
     */
    private class CredentialListener implements PropertyChangeListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (MembershipService.DEFAULT_CREDENTIAL_PROPERTY.equals(evt.getPropertyName())) {
                Logging.logCheckedDebug(LOG, "New default credential event");

                synchronized (DiscoveryServiceImpl.this) {
                    Credential cred = (Credential) evt.getNewValue();
                    XMLDocument credentialDoc;

                    if (null != cred) {
                        try {
                            credentialDoc = (XMLDocument) cred.getDocument(MimeMediaType.XMLUTF8);
                            currentCredential = new CurrentCredential(cred, credentialDoc);
                        } catch (Exception exception) {
                            Logging.logCheckedWarning(LOG, "Could not generate credential document\n", exception);
                            currentCredential = null;
                        }
                    } else {
                        currentCredential = null;
                    }
                }
            }
        }
    }

    /** 
     * Our listener for membership credential change events.
     */
    private final CredentialListener membershipCredListener = new CredentialListener();

//    /**
//     * {@inheritDoc}
//     *
//     * @since 2.6 This method has been deprecated and now returns {@code this} rather than
//     * an instance of {@code DiscoveryServiceInterface}. It should be removed from the code
//     * in a future release.
//     */
//    public synchronized Service getInterface() {
//
//        return this;
//
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    /**
     * {@inheritDoc}
     * @param peer
     */
    @Override
    public int getRemoteAdvertisements(String peer, int type, String attribute, String value, int threshold) {
        return getRemoteAdvertisements(peer, type, attribute, value, threshold, null);
    }

    /**
     * {@inheritDoc}
     * @param peer
     */
    @Override
    public int getRemoteAdvertisements(String peer, int type, String attribute, String value, int threshold, DiscoveryListener listener) {
        int myQueryID = qid.incrementAndGet();

        if (localonly || stopped) {
            Logging.logCheckedDebug(LOG, "Local only, no network operations performed");
            return myQueryID;
        }

        if (resolver == null) {
            // warn about calling the service before it started
            Logging.logCheckedWarning(LOG, "Resolver has not been started yet, query discarded.");
            return myQueryID;
        }

        if (Logging.SHOW_DEBUG && LOG.isDebugEnabled()) {
            StringBuilder query = new StringBuilder("Sending query#" + myQueryID + " for " + threshold + " " + dirname[type] + " advs");

            if (attribute != null) {
                query.append("\n\tattr = ").append(attribute);
                if (value != null) {
                    query.append("\tvalue = ").append(value);
                }
            }
            LOG.debug(query.toString());
        }

        long t0 = System.currentTimeMillis();

        DiscoveryQueryMsg discoveryQueryMessage = new DiscoveryQuery();

        discoveryQueryMessage.setDiscoveryType(type);
        discoveryQueryMessage.setAttr(attribute);
        discoveryQueryMessage.setValue(value);
        discoveryQueryMessage.setThreshold(threshold);

        if (listener != null) {
            synchronized (queryListeners) {
                queryListeners.put(myQueryID, listener);
            }
        }

        ResolverQueryMsg query = new ResolverQuery();

        query.setHandlerName(handlerName);
        CurrentCredential current = currentCredential;
        if (null != current) {
            query.setCredential(current.credentialDoc);
        }
        query.setSrcPeer(localPeerId);
        query.setQuery(discoveryQueryMessage.getDocument(MimeMediaType.XMLUTF8).toString());
        query.setQueryId(myQueryID);

        // check srdiManager
        if (peer == null && srdiIndex != null) {
            List<PeerID> res = srdiIndex.query(dirname[type], attribute, value, threshold);

            if (!res.isEmpty()) {
                srdiManager.forwardQuery(res, query, threshold);
                Logging.logCheckedDebug(LOG, "Srdi forward a query #", myQueryID, " in ", (System.currentTimeMillis() - t0), "ms.");
                return myQueryID;

            // nothing in srdiManager, get a starting point in rpv
            } else if (peerGroup.isRendezvous() && attribute != null && value != null) {
                PeerID destPeer = srdiManager.getReplicaPeer(dirname[type] + attribute + value);

                if (destPeer != null) {

                    if (!destPeer.equals(localPeerId)) {

                        // forward query increments the hopcount to indicate getReplica
                        // has been invoked once
                        srdiManager.forwardQuery(destPeer, query);
                        Logging.logCheckedDebug(LOG, "Srdi forward query #", myQueryID, " to ", destPeer, " in ", (System.currentTimeMillis() - t0), "ms.");
                        return myQueryID;

                    }
                }
            }
        }

        // no srdiManager, not a rendezvous, start the walk
        resolver.sendQuery(peer, query);

        if (peer == null) {
            Logging.logCheckedDebug(LOG, "Sent a query #", myQueryID, " in ", (System.currentTimeMillis() - t0), "ms.");
        } else {
            Logging.logCheckedDebug(LOG, "Sent a query #", myQueryID, " to ", peer, " in ", (System.currentTimeMillis() - t0), "ms.");
        }

        return myQueryID;
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public Enumeration<Advertisement> getLocalAdvertisements(int type, String attribute, String value) throws IOException {

        if ((type > DiscoveryService.ADV) || (type < DiscoveryService.PEER)) {
            throw new IllegalArgumentException("Unknown Advertisement type");
        }

        if (Logging.SHOW_DEBUG && LOG.isDebugEnabled()) {

            StringBuilder query = new StringBuilder("Searching for " + dirname[type] + " advs");

            if (attribute != null) {
                query.append("\n\tattr = ").append(attribute);
            }
            
            if (value != null) {
                query.append("\tvalue = ").append(value);
            }

            LOG.debug(query.toString());
        }

        return Collections.enumeration(search(type, attribute, value, Integer.MAX_VALUE, null));
    }

    /**
     * {@inheritDoc}
     * @param peerGroup
     * @param impl
     * @throws net.jxta.exception.PeerGroupException
     */
    @Override
    public void init(PeerGroup peerGroup, ID assignedID, Advertisement impl) throws PeerGroupException {
        this.peerGroup = peerGroup;
        handlerName = assignedID.toString();
        implAdvertisement = (ModuleImplAdvertisement) impl;
        localPeerId = this.peerGroup.getPeerID();

        ConfigParams confAdv = peerGroup.getConfigAdvertisement();

        // Get the config. If we do not have a config, we're done; we just keep
        // the defaults (edge peer/no auto-rdv)
        if (confAdv != null) {
            Advertisement adv = null;

            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(assignedID);

                if (null != configDoc) {
                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (NoSuchElementException failed) {
            // ignored
            }

            if (adv instanceof DiscoveryConfigAdv) {
                DiscoveryConfigAdv discoConfigAdv = (DiscoveryConfigAdv) adv;
                alwaysUseReplicaPeer = discoConfigAdv.getForwardAlwaysReplica();
                forwardBelowThreshold = discoConfigAdv.getForwardBelowTreshold();
                localonly |= discoConfigAdv.getLocalOnly();
            }
        }

        cacheManager = ((StdPeerGroup) this.peerGroup).getCacheManager();
        cacheManager.setTrackDeltas(!localonly);

        if (Logging.SHOW_CONFIG && LOG.isConfigEnabled()) {

            StringBuilder configInfo = new StringBuilder("Configuring Discovery Service : " + assignedID);

            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: ").append(implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
            }

            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : ").append(this.peerGroup.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : ").append(this.peerGroup.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : ").append(this.peerGroup.getPeerID());
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tLocal Only : ").append(localonly);
            configInfo.append("\n\t\tAlways Use ReplicaPeer : ").append(alwaysUseReplicaPeer);
            configInfo.append("\n\t\tForward when below threshold responses : ").append(forwardBelowThreshold);

            LOG.config(configInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     * @param arg
     */
    @Override
    public int startApp(String[] arg) {

        resolver = peerGroup.getResolverService();

        if (null == resolver) {
            Logging.logCheckedWarning(LOG, "Stalled until there is a resolver service");
            return Module.START_AGAIN_STALLED;
        }

        membership = peerGroup.getMembershipService();

        if (null == membership) {
            Logging.logCheckedWarning(LOG, "Stalled until there is a membership service");
            return Module.START_AGAIN_STALLED;
        }

        rendezvous = peerGroup.getRendezVousService();

        if (null == rendezvous) {
            Logging.logCheckedWarning(LOG, "Stalled until there is a rendezvous service");
            return Module.START_AGAIN_STALLED;
        }

        // Get the initial credential doc
        synchronized (this) {
            membership.addPropertyChangeListener("defaultCredential", membershipCredListener);

            try {
                membershipCredListener.propertyChange(new PropertyChangeEvent(membership, "defaultCredential", null, membership.getDefaultCredential()));
            } catch (Exception all) {
                Logging.logCheckedWarning(LOG, "Could not get credential\n", all);
            }
        }

        // local only discovery
        if (!localonly) {
            resolver.registerHandler(handlerName, this);
        }

        if (rendezvous.isRendezVous()) {
            beRendezvous();
        } else {
            beEdge();
        }

        rendezvous.addListener(this);

        stopped = false;

        Logging.logCheckedInfo(LOG, "Discovery service started");

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Detach from the resolver and from rendezvous
     */
    @Override
    public void stopApp() {
        stopped = true;
        boolean failed = false;

        membership.removePropertyChangeListener("defaultCredential", membershipCredListener);
        currentCredential = null;

        rendezvous.removeListener(this);

        if (resolver.unregisterHandler(handlerName) == null) {
            failed = true;
        }

        if (rendezvous.isRendezVous()) {
            if (resolver.unregisterSrdiHandler(handlerName) == null) {
                failed = true;
            }
        }

        if (failed) {
            Logging.logCheckedWarning(LOG, "failed to unregister discovery from resolver.");
        }

        // stop SRDI
        if (srdiManager != null) {
            srdiManager.stop();
        }

        srdiIndex = null;

        // Forget about all remaining listeners.
        listeners.clear();
        queryListeners.clear();

        Logging.logCheckedInfo(LOG, "Discovery service stopped.");
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void flushAdvertisements(String id, int type) throws IOException {
        if (stopped) {
            return;
        }
        if ((type >= PEER) && (type <= ADV)) {
            if (null != id) {

                ID advID = ID.create(URI.create(id));
                String advName = advID.getUniqueValue().toString();

                Logging.logCheckedDebug(LOG, "flushing adv ", advName, " of type ", dirname[type]);
                cacheManager.remove(dirname[type], advName);
            } else {
                // XXX bondolo 20050902 For historical purposes we ignore null
                Logging.logCheckedWarning(LOG, "Flush request by type IGNORED. You must delete advertisements individually.");
            }
        } else {
            throw new IllegalArgumentException("Invalid Advertisement type.");
        }
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void flushAdvertisement(Advertisement adv) throws IOException {
        if (stopped) {
            return;
        }

        int type;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else if (adv instanceof PeerGroupAdvertisement) {
            type = GROUP;
        } else {
            type = ADV;
        }

        ID id = adv.getID();
        String advName;

        if (id != null && !id.equals(ID.NULL_ID)) {

            advName = id.getUniqueValue().toString();
            Logging.logCheckedDebug(LOG, "Flushing adv ", advName, " of type ", dirname[type]);

        } else {

            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getSignedDocument();
            } catch (Exception everything) {
                throw new IOException("Failure removing Advertisement", everything);
            }
            advName = CacheManager.createTmpName(doc);
        }

        if (advName != null) {
            cacheManager.remove(dirname[type], advName);
        }
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void publish(Advertisement adv) throws IOException {
        publish(adv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    @Override
    public void publish(Advertisement adv, long lifetime, long expiration) throws IOException {

        if (stopped) {
            return;
        }

        ID advID;
        String advName;

        int type;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else if (adv instanceof PeerGroupAdvertisement) {
            type = GROUP;
        } else {
            type = ADV;
        }

        advID = adv.getID();

        // if we dont have a unique id for the adv, use the hash method
        if ((null == advID) || advID.equals(ID.NULL_ID)) {
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getSignedDocument();
            } catch (Exception everything) {
                Logging.logCheckedWarning(LOG, "Failed to generated document from advertisement\n", everything);
                throw new IOException("Failed to generate document from advertisement", everything);
            }

            try {
                advName = CacheManager.createTmpName(doc);
            } catch (IllegalStateException ise) {
                throw new IOException("Failed to generate tempname from advertisement", ise);
            }
        } else {
            advName = advID.getUniqueValue().toString();
        }

        Logging.logCheckedDebug(LOG, "Publishing a ", adv.getAdvType(), " as ", dirname[type], " / ", advName, "\n\texpiration : ", expiration, "\tlifetime :", lifetime);

        cacheManager.save(dirname[type], advName, adv, lifetime, expiration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remotePublish(Advertisement adv) {
        remotePublish(null, adv, DiscoveryService.DEFAULT_EXPIRATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remotePublish(Advertisement adv, long expiration) {
        remotePublish(null, adv, expiration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remotePublish(String peerid, Advertisement adv) {
        remotePublish(peerid, adv, DiscoveryService.DEFAULT_EXPIRATION);
    }
    
    /**
     * {@inheritDoc}
     * @param timeout
     */
    @Override
    public void remotePublish(String peerid, Advertisement adv, long timeout) {
        if (localonly || stopped) {
            Logging.logCheckedDebug(LOG, "localonly, no network operations performed");
            return;
        }

        int type;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else if (adv instanceof PeerGroupAdvertisement) {
            type = GROUP;
        } else {
            type = ADV;
        }
        remotePublish(peerid, adv, type, timeout);
    }

    /*
     *  Publish advertisement remotely
     */
    private void remotePublish(String peerid, Advertisement adv, int type, long expiration) {
        if (localonly || stopped) {
            Logging.logCheckedDebug(LOG, "localonly, no network operations performed");
            return;
        }

        // In case this is invoked before startApp().
        if (resolver == null) {
            return;
        }

        switch (type) {
            case PEER:
                if (adv instanceof PeerAdvertisement) {
                    break;
                }
                throw new IllegalArgumentException("Not a peer advertisement");
            case GROUP:
                if (adv instanceof PeerGroupAdvertisement) {
                    break;
                }
                throw new IllegalArgumentException("Not a peergroup advertisement");
            case ADV:
                break;
            default:
                throw new IllegalArgumentException("Unknown advertisement type");
        }

        DiscoveryResponseMsg discoveryResponseMessage = new DiscoveryResponse();

        discoveryResponseMessage.setDiscoveryType(type);
        discoveryResponseMessage.setResponses(Collections.singletonList(adv.toString()));
        discoveryResponseMessage.setExpirations(Collections.singletonList(expiration));

        ResolverResponseMsg resolverResponseMessage = new ResolverResponse();

        resolverResponseMessage.setHandlerName(handlerName);
        
        CurrentCredential current = currentCredential;
        if (null != current) {
            resolverResponseMessage.setCredential(current.credentialDoc);
        }
        
        resolverResponseMessage.setQueryId(REMOTE_PUBLISH_QUERYID);
        resolverResponseMessage.setResponse(discoveryResponseMessage.toString());

        Logging.logCheckedDebug(LOG, "Remote publishing");

        resolver.sendResponse(peerid, resolverResponseMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processResponse(ResolverResponseMsg response) {
        processResponse(response, null);
    }

    /**
     * {@inheritDoc}
     * @param srcAddress
     */
    @Override
    public void processResponse(ResolverResponseMsg response, EndpointAddress srcAddress) {
        if (stopped) {
            return;
        }
        
        long t0 = System.currentTimeMillis();
        DiscoveryResponse discoveryResponse;

        try {
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(response.getResponse()));
            discoveryResponse = new DiscoveryResponse(asDoc);
        } catch (Exception e) {
            // we don't understand this msg, let's skip it
            Logging.logCheckedWarning(LOG, "Failed to Read Discovery Response\n", e);
            return;
        }

        /*
         PeerAdvertisement padv = res.getPeerAdvertisement();
         if (padv == null)
         return;

         if (LOG.isLoggable(Level.FINE)) {
         LOG.fine("Got a " + dirname[res.getDiscoveryType()] +
         " from "+padv.getName()+ " response : " +
         res.getQueryAttr() + " = " + res.getQueryValue());
         }

         try {
         // The sender does not put an expiration on that one, but
         // we do not want to keep it around for more than the
         // default duration. It may get updated or become invalid.
         publish(padv, PEER, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
         } catch (Exception e) {
         if (LOG.isLoggable(Level.FINE)) {
         LOG.fine(e, e);
         }
         return;
         }
         */
        Advertisement advertisement;

        Logging.logCheckedDebug(LOG, "Processing responses for query #", response.getQueryId());

        Enumeration<Advertisement> advertisementsEnumeration = discoveryResponse.getAdvertisements();
        Enumeration<Long> exps = discoveryResponse.getExpirations();

        while (advertisementsEnumeration.hasMoreElements()) {
            advertisement = advertisementsEnumeration.nextElement();
            long expirationTime = exps.nextElement();

            if (advertisement != null) {                
                try {
                    if (expirationTime > 0) {
                        publish(advertisement, expirationTime, expirationTime);
                    } 
                } catch (Exception e) {
                    Logging.logCheckedWarning(LOG, "Error publishing Advertisement\n", e);
                }
            }                        
        }

        // Generate an event and callback the query listener (if any).
        DiscoveryEvent discoveryEvent = new DiscoveryEvent(srcAddress, discoveryResponse, response.getQueryId());

        DiscoveryListener discoveryListener;
        synchronized (queryListeners) {
            discoveryListener = queryListeners.get(response.getQueryId());
        }

        if (discoveryListener != null) {
            try {
                discoveryListener.discoveryEvent(new DiscoveryEvent(srcAddress, discoveryResponse, response.getQueryId()));
            } catch (Throwable all) {
                LOG.error("Uncaught Throwable in listener :" + Thread.currentThread().getName(), all);
            }
        }

        Logging.logCheckedDebug(LOG, "processed a response for query #", response.getQueryId(), " in :", (System.currentTimeMillis() - t0));

        // Callback any registered discovery listeners.
        t0 = System.currentTimeMillis();

        Collection<DiscoveryListener> allListeners;
        synchronized (listeners) {
            allListeners = new ArrayList<>(listeners);
        }

        for (DiscoveryListener aListener : allListeners) {
            try {
                aListener.discoveryEvent(discoveryEvent);
            } catch (Throwable all) {
                Logging.logCheckedWarning(LOG, "Uncaught Throwable in listener (", aListener.getClass().getName(), ") :", Thread.currentThread().getName(), "\n", all);
            }
        }

        Logging.logCheckedDebug(LOG, "Called all listeners to query #", response.getQueryId(), " in :", (System.currentTimeMillis() - t0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int processQuery(ResolverQueryMsg query) {
        return processQuery(query, null);
    }

    /**
     * {@inheritDoc}
     * @param srcAddress
     */
    @Override
    public int processQuery(ResolverQueryMsg query, EndpointAddress srcAddress) {

        if (stopped) {
            return ResolverService.OK;
        }

        if (srcAddress != null) {
            Logging.logCheckedDebug(LOG, "Processing query #", query.getQueryId(), " from:", srcAddress);
        } else {
            Logging.logCheckedDebug(LOG, "Processing query #", query.getQueryId(), " from: unknown");
        }

        DiscoveryQuery discoveryQuery;
        long t0 = System.currentTimeMillis();

        try {
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(query.getQuery()));
            discoveryQuery = new DiscoveryQuery(asDoc);
        } catch (Exception e) {
            Logging.logCheckedWarning(LOG, "Malformed query : \n", e);
            return ResolverService.OK;
        }

        if ((discoveryQuery.getThreshold() < 0) || (discoveryQuery.getDiscoveryType() < PEER) || (discoveryQuery.getDiscoveryType() > ADV)) {
            Logging.logCheckedWarning(LOG, "Malformed query");
            return ResolverService.OK;
        }

        Logging.logCheckedDebug(LOG, "Got a ", dirname[discoveryQuery.getDiscoveryType()], " query #", query.getQueryId(), " query :", discoveryQuery.getAttr(), " = ", discoveryQuery.getValue());

        /*
        // Get the Peer Adv from the query and publish it.
        PeerAdvertisement padv = dq.getPeerAdvertisement();
        try {
        if (!(padv.getPeerID().toString()).equals(localPeerId)) {
        // publish others only. Since this one comes from outside,
        // we must not keep it beyond its expiration time.
        // FIXME: [jice@jxta.org 20011112] In theory there should
        // be an expiration time associated with it in the msg, like
        // all other items.
        publish(padv, PEER, DEFAULT_EXPIRATION, DEFAULT_EXPIRATION);
        }
        } catch (Exception e) {
        if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Bad Peer Adv in Discovery Query\n", e);
        }
        }
         */

        int thresh = Math.min(discoveryQuery.getThreshold(), MAX_RESPONSES);

        /*
         *  threshold==0 and type==PEER is a special case. In this case we are
         *  responding for the purpose of providing our own adv only.
         */
        if ((discoveryQuery.getDiscoveryType() == PEER) && (0 == discoveryQuery.getThreshold())) {

            respond(query, discoveryQuery, Collections.singletonList(peerGroup.getPeerAdvertisement().toString()),
                    Collections.singletonList(DiscoveryService.DEFAULT_EXPIRATION));

            Logging.logCheckedDebug(LOG, "Responding to query #", query.getQueryId(), " in :", (System.currentTimeMillis() - t0));

            return ResolverService.OK;

        }

        Logging.logCheckedDebug(LOG, "start local search query", discoveryQuery.getAttr(), " ", discoveryQuery.getValue());

        List<Long> expirations = new ArrayList<>();
        List<InputStream> results = rawSearch(discoveryQuery.getDiscoveryType(), discoveryQuery.getAttr(), discoveryQuery.getValue(), thresh, expirations);

        if (!results.isEmpty()) {

            Logging.logCheckedDebug(LOG, "Responding to ", dirname[discoveryQuery.getDiscoveryType()], " Query : ", discoveryQuery.getAttr(), " = ", discoveryQuery.getValue());
            respond(query, discoveryQuery, results, expirations);
            Logging.logCheckedDebug(LOG, "Responded to query #", query.getQueryId(), " in :", (System.currentTimeMillis() - t0));

        }

        // If this peer is not a rendezvous, just discard the query.
        if (!peerGroup.isRendezvous()) {
            return ResolverService.OK;
        }

        PeerID replicaPeer = srdiManager.getReplicaPeer(dirname[discoveryQuery.getDiscoveryType()] + discoveryQuery.getAttr() + discoveryQuery.getValue());

        if ((null != replicaPeer) && !localPeerId.equals(replicaPeer)) {
            if (alwaysUseReplicaPeer || (forwardBelowThreshold && (results.size() < discoveryQuery.getThreshold()))) {
                Logging.logCheckedDebug(LOG, "Forwarding query #", query.getQueryId(), " to replica peer ", replicaPeer);
                // forward to SRDI replica.
                srdiManager.forwardQuery(replicaPeer, query);
            }

            // In either case we are done.
            return ResolverService.OK;
        }

        // We didn't have sufficient local results or there is no known replica.
        // See if there are any in SRDI.
        if (results.isEmpty() || (forwardBelowThreshold && (results.size() < discoveryQuery.getThreshold()))) {

            Logging.logCheckedDebug(LOG, "Querying SrdiIndex for query #", query.getQueryId());

            List<PeerID> res = srdiIndex.query(dirname[discoveryQuery.getDiscoveryType()], discoveryQuery.getAttr(), discoveryQuery.getValue(), thresh);

            if (!res.isEmpty()) {
                srdiManager.forwardQuery(res, query, thresh);
            } else {
                // start the walk since this peer is this the starting peer
                query.incrementHopCount();
                return ResolverService.Repropagate;
            }

        }

        return ResolverService.OK;
    }

    /**
     * @param query The resolver query we are responding to.
     * @param dq    The discovery query we are responding to.
     * @param results   The results we are responding with(Advertisemets,Strings,InputStreams).
     * @param expirations   Expiration values for the results.
     */
    private void respond(ResolverQueryMsg query, DiscoveryQuery dq, List results, List<Long> expirations) {

        if (localonly || stopped) {
            return;
        }

        ResolverResponseMsg response;
        DiscoveryResponse dresponse = new DiscoveryResponse();

        // peer adv is optional, skip
        dresponse.setDiscoveryType(dq.getDiscoveryType());
        dresponse.setQueryAttr(dq.getAttr());
        dresponse.setQueryValue(dq.getValue());
        dresponse.setResponses(results);
        dresponse.setExpirations(expirations);

        // create a response from the query
        response = query.makeResponse();
        CurrentCredential current = currentCredential;
        if (null != current) {
            response.setCredential(current.credentialDoc);
        }

        response.setResponse(dresponse.toString());

        Logging.logCheckedDebug(LOG, "Responding to query #", query.getQueryId(), " ", query.getSrcPeer());

        resolver.sendResponse(query.getSrcPeer().toString(), response);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDiscoveryListener(DiscoveryListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeDiscoveryListener(DiscoveryListener listener) {
        boolean removed = false;

        synchronized (queryListeners) {
            Iterator<DiscoveryListener> eachDiscoveryListener = queryListeners.values().iterator();

            while (eachDiscoveryListener.hasNext()) {
                if (listener == eachDiscoveryListener.next()) {
                    eachDiscoveryListener.remove();
                    removed = true;
                }
            }
        }

        synchronized (listeners) {
            removed |= listeners.remove(listener);
        }
        return removed;
    }    

    /**
     * Search for Advertisements that matches attr and value.
     *
     * @param type        Discovery type PEER, GROUP, ADV
     * @param threshold   the upper limit of responses from one peer
     * @param expirations List containing the expirations associated with is returned
     * @param attr        attribute name to narrow discovery to Valid values for
     *                    this parameter are null (don't care), or exact element name in the
     *                    advertisement of interest (e.g. "Name")
     * @param value       Value
     * @return list of results either as docs, or Strings
     */
    private List<InputStream> rawSearch(int type, String attr, String value, int threshold, List<Long> expirations) {
        if (stopped) {
            return new ArrayList();
        }

        if (type == PEER) {
            checkUpdatePeerAdv();
        }

        List<InputStream> results;

        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be greater than zero");
        }

        if (expirations != null) {
            expirations.clear();
        }

        if (attr != null) {
            Logging.logCheckedDebug(LOG, "Searching for ", threshold, " entries of type : ", dirname[type]);

            // a discovery query with a specific search criteria.
            results = cacheManager.search(dirname[type], attr, value, threshold, expirations);
        } else {
            Logging.logCheckedDebug(LOG, "Getting ", threshold, " entries of type : ", dirname[type]);

            // Returning any entry that exists
            results = cacheManager.getRecords(dirname[type], threshold, expirations);
        }

        Logging.logCheckedDebug(LOG, "Returning ", results.size(), " results");

        // nothing more to do;
        return results;
    }

    /**
     * Search for Advertisements that matches attr and value.
     *
     * @param type        Discovery type PEER, GROUP, ADV
     * @param threshold   the upper limit of responses from one peer
     * @param expirations List containing the expirations associated with is returned
     * @param attr        attribute name to narrow discovery to Valid values for
     *                    this parameter are null (don't care), or exact element name in the
     *                    advertisement of interest (e.g. "Name")
     * @param value       Value
     * @return list of results either as docs, or Strings
     */
    private List<Advertisement> search(int type, String attr, String value, int threshold, List<Long> expirations) {
        List<InputStream> results = rawSearch(type, attr, value, threshold, expirations);

        // Convert the input streams returned by the cm into Advertisements.
        List<Advertisement> advertisements = new ArrayList<>();

        for (int i = 0; (i < results.size()) && (advertisements.size() < threshold); i++) {
            try {

                InputStream bis = results.get(i);
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, bis);
                Advertisement adv = AdvertisementFactory.newAdvertisement(asDoc);
                advertisements.add(adv);

            } catch (Exception e) {

                Logging.logCheckedWarning(LOG, "Failed building advertisment\n", e);

                // we won't be including this advertisement so remove it's expiration.
                if (null != expirations) expirations.remove(i);

            }
        }

        Logging.logCheckedDebug(LOG, "Returning ", advertisements.size(), " advertisements");

        return advertisements;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAdvExpirationTime(ID id, int type) {

        if (stopped) return -1;

        String advName;

        if (id != null && !id.equals(ID.NULL_ID)) {
            advName = id.getUniqueValue().toString();
            Logging.logCheckedDebug(LOG, "Getting expiration time of ", advName, " of type ", dirname[type]);
        } else {
            Logging.logCheckedDebug(LOG, "invalid attempt to get advertisement expiration time of NullID");
            return -1;
        }

        return cacheManager.getExpirationtime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAdvLifeTime(ID id, int type) {

        if (id == null || id.equals(ID.NULL_ID) || stopped) {

            Logging.logCheckedWarning(LOG, "invalid attempt to get advertisement lifetime of a NullID");
            return -1;

        }

        String advName = id.getUniqueValue().toString();

        Logging.logCheckedDebug(LOG, "Getting lifetime of ", advName, " of type ", dirname[type]);

        return cacheManager.getLifetime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAdvExpirationTime(Advertisement adv) {
        if (stopped) {
            return -1;
        }
        int type;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else if (adv instanceof PeerGroupAdvertisement) {
            type = GROUP;
        } else {
            type = ADV;
        }

        String advName;
        ID id = adv.getID();

        if (id != null && !id.equals(ID.NULL_ID)) {

            advName = id.getUniqueValue().toString();
            Logging.logCheckedDebug(LOG, "attempting to getAdvExpirationTime on ", advName, " of type ", dirname[type]);

        } else {

            XMLDocument doc;

            try {

                doc = (XMLDocument) adv.getSignedDocument();

            } catch (Exception everything) {

                Logging.logCheckedWarning(LOG, "Failed to get document\n", everything);
                return -1;

            }

            advName = CacheManager.createTmpName(doc);
        }

        return cacheManager.getExpirationtime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAdvLifeTime(Advertisement adv) {
        if (stopped) {
            return -1;
        }
        int type;

        if (adv instanceof PeerAdvertisement) {
            type = PEER;
        } else if (adv instanceof PeerGroupAdvertisement) {
            type = GROUP;
        } else {
            type = ADV;
        }

        ID id = adv.getID();
        String advName;

        if (id != null && !id.equals(ID.NULL_ID)) {

            advName = id.getUniqueValue().toString();
            Logging.logCheckedDebug(LOG, "attempting to getAdvLifeTime ", advName, " of type ", dirname[type]);

        } else {

            XMLDocument doc;

            try {

                doc = (XMLDocument) adv.getSignedDocument();

            } catch (Exception everything) {

                Logging.logCheckedWarning(LOG, "Failed to get document\n", everything);
                return -1;

            }

            advName = CacheManager.createTmpName(doc);
        }
        return cacheManager.getLifetime(dirname[type], advName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean processSrdi(ResolverSrdiMsg message) {

        if (stopped) return true;

        Logging.logCheckedDebug(LOG, "[", peerGroup.getPeerGroupID(), "] Received an SRDI messsage");

        SrdiMessage srdiMsg;

        try {

            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(message.getPayload()));
            srdiMsg = new SrdiMessageImpl(asDoc);

        } catch (Exception e) {

            Logging.logCheckedWarning(LOG, "Failed parsing srdi message\n", e);
            return false;

        }

        PeerID pid = srdiMsg.getPeerID();

        for (Object o : srdiMsg.getEntries()) {

            SrdiMessage.Entry entry = (SrdiMessage.Entry) o;
            srdiIndex.add(srdiMsg.getPrimaryKey(), entry.key, entry.value, pid, entry.expiration);
            Logging.logCheckedDebug(LOG, "Primary Key [", srdiMsg.getPrimaryKey(), "] key [", entry.key, "] value [", entry.value, "] exp [", entry.expiration, "]");

        }

        srdiManager.replicateEntries(srdiMsg);
        return true;
    }

    /**
     * {@inheritDoc}
     * @param e
     */
    @Override
    public void messageSendFailed(PeerID peerid, OutgoingMessageEvent e) {
        if (srdiIndex != null) {
            srdiIndex.remove(peerid);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pushEntries(boolean all) {

        pushSrdi(null, PEER, all);
        pushSrdi(null, GROUP, all);
        pushSrdi(null, ADV, all);
    }

    /**
     * push srdiManager entries
     *
     * @param all  if true push all entries, otherwise just deltas
     * @param peer peer id
     * @param type if true sends all entries
     */
    protected void pushSrdi(ID peer, int type, boolean all) {
        if (stopped) {
            return;
        }

        List<SrdiMessage.Entry> entries;

        if (all) {
            entries = cacheManager.getEntries(dirname[type], true);
        } else {
            entries = cacheManager.getDeltas(dirname[type]);
        }

        if (!entries.isEmpty()) {
            SrdiMessage srdiMsg;

            try {
                srdiMsg = new SrdiMessageImpl(localPeerId, 1, // ttl of 1, ensure it is replicated
                        dirname[type], entries);

                // LOGGING: was Finer
                Logging.logCheckedDebug(LOG, "Pushing ", entries.size(), (all ? " entries" : " deltas"), " of type ", dirname[type]);
                srdiManager.pushSrdi(peer, srdiMsg);

            } catch (Exception e) {
                Logging.logCheckedWarning(LOG, "Exception pushing SRDI Entries\n", e);
            }
        } else {
            // LOGGING: was Finer
            Logging.logCheckedDebug(LOG, "No", (all ? " entries" : " deltas"), " of type ", dirname[type], " to push");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void rendezvousEvent(RendezvousEvent event) {

        int theEventType = event.getType();

        Logging.logCheckedDebug(LOG, "[", peerGroup.getPeerGroupName(), "] Processing ", event);

        switch (theEventType) {
            case RendezvousEvent.RDVCONNECT:
            case RendezvousEvent.RDVRECONNECT:
                // start tracking deltas
                cacheManager.setTrackDeltas(true);
                break;
            case RendezvousEvent.CLIENTCONNECT:
            case RendezvousEvent.CLIENTRECONNECT:
                break;
            case RendezvousEvent.RDVFAILED:
            case RendezvousEvent.RDVDISCONNECT:
                // stop tracking deltas until we connect again
                cacheManager.setTrackDeltas(false);
                break;
            case RendezvousEvent.CLIENTFAILED:
            case RendezvousEvent.CLIENTDISCONNECT:
                break;
            case RendezvousEvent.BECAMERDV:
                beRendezvous();
                break;
            case RendezvousEvent.BECAMEEDGE:
                beEdge();
                break;
            default:
                Logging.logCheckedWarning(LOG, MessageFormat.format("[{0}] Unexpected RDV event : {1}", peerGroup.getPeerGroupName(), event));
                break;
        }
    }

    /**
     * Checks to see if the local peer advertisement has been updated and if
     * it has then republish it to the CM.
     */
    private void checkUpdatePeerAdv() {
        PeerAdvertisement newPadv = peerGroup.getPeerAdvertisement();
        int newModCount = newPadv.getModCount();

        boolean updated = false;

        synchronized (checkPeerAdvLock) {
            if ((lastPeerAdv != newPadv) || (lastModCount < newModCount)) {
                lastPeerAdv = newPadv;
                lastModCount = newModCount;
                updated = true;
            }
        }

        if (updated) {
            // Publish the local Peer Advertisement
            try {
                Logging.logCheckedDebug(LOG, "publishing local advertisement");
                // This is our own; we can publish it for a long time in our cache
                publish(newPadv, INFINITE_LIFETIME, DEFAULT_EXPIRATION);
            } catch (Exception ignoring) {
                Logging.logCheckedWarning(LOG, "Could not publish local peer advertisement: \n", ignoring);
            }
        }
    }

    /**
     * Change the behavior to be an rendezvous Peer Discovery Service.
     * If the Service was acting as an Edge peer, cleanup.
     */
    private synchronized void beRendezvous() {
        if (isRdv && (srdiManager != null || srdiIndex != null)) {
            Logging.logCheckedInfo(LOG, "Already a rendezvous -- No Switch is needed");
            return;
        }

        isRdv = true;

        // rdv peers do not need to track deltas
        cacheManager.setTrackDeltas(false);

        if (srdiIndex == null) {

            srdiIndex = new Srdi(peerGroup, SRDI_INDEXER_FILE_NAME);
            Logging.logCheckedDebug(LOG, "srdiIndex created");

        }

        // Kill SRDI, create a new one.
        if (srdiManager != null) {
            srdiManager.stop();
            srdiManager = null;
        }

        if (!localonly) {

            srdiManager = new SrdiManager(peerGroup, handlerName, this, srdiIndex);
            resolver.registerSrdiHandler(handlerName, this);
            Logging.logCheckedDebug(LOG, "srdi created, and registered as an srdi handler ");

        }

        Logging.logCheckedInfo(LOG, "Switched to Rendezvous peer role.");

    }

    /**
     * Change the behavior to be an Edge Peer Discovery Service.
     * If the Service was acting as a Rendezvous, cleanup.
     */
    private synchronized void beEdge() {

        // make sure we have been here before
        if (!isRdv) {
            Logging.logCheckedInfo(LOG, "Already an Edge peer -- No Switch is needed.");
            return;
        }

        isRdv = false;
        if (rendezvous.isConnectedToRendezVous()) {
            // if we have a rendezvous connection track deltas, otherwise wait
            // for a connect event to set this option
            cacheManager.setTrackDeltas(true);
        }
        if (srdiIndex != null) {

            srdiIndex.stop();
            srdiIndex = null;
            resolver.unregisterSrdiHandler(handlerName);
            Logging.logCheckedDebug(LOG, "stopped cache and unregistered from resolver");
        }

        // Kill SRDI
        if (srdiManager != null) {
            srdiManager.stop();
            srdiManager = null;
        }

        if (!localonly) {
            // Create a new SRDI manager
            srdiManager = new SrdiManager(peerGroup, handlerName, this, null);
            srdiManager.startPush(peerGroup.getTaskManager().getScheduledExecutorService(), runInterval);
        }

        Logging.logCheckedInfo(LOG, "Switched to a Edge peer role.");
    }
}
