/*
 * Copyright (c) 2002-2004 Sun Microsystems, Inc.  All rights reserved.
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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import net.jxta.impl.util.*;

import java.util.logging.Level;
import net.jxta.logging.Logging;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Attribute;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RdvAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

import net.jxta.impl.access.AccessList;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.rendezvous.RendezVousServiceImpl;


/**
 *  Extends the URI Seeding Manager by supplementing the list of active seeds
 *  with the active relay peers.
 */
public class RelayReferralSeedingManager extends URISeedingManager {
    
    /**
     *  Log4J Logger
     */
    private static final transient Logger LOG = Logger.getLogger(RelayReferralSeedingManager.class.getName());

    private final boolean probeRelays;
    
    private final PeerGroup group;
    
    /**
     *  Get an instance of RelayReferralSeedingManager.
     */
    public RelayReferralSeedingManager(URI aclLocation, boolean allowOnlySeeds, boolean probeRelays, PeerGroup group) {
        super(aclLocation, allowOnlySeeds);
        
        this.probeRelays = probeRelays;
        this.group = group;
    }
    
    /**
     */
    @Override
    public synchronized URI[] getActiveSeedURIs() {
        Collection<URI> results = new ArrayList<URI>();
        URI[] superseeds = super.getActiveSeedURIs();       
        Collection<RouteAdvertisement> relays = getRelayPeers();
        
        int eaIndex = 0;
        boolean addedEA;
        
        do {
            addedEA = false;
            
            for (RouteAdvertisement aRA : activeSeeds) {
                if (eaIndex < aRA.size()) {
                    results.add(URI.create(aRA.getDest().getVectorEndpointAddresses().get(eaIndex)));
                    addedEA = true;
                }
            }
            
            // Next loop we use the next most preferred address.
            eaIndex++;
        } while (addedEA);
        
        // Add the non-relay seeds afterwards.
        results.addAll(Arrays.asList(superseeds));
                
        return results.toArray(new URI[results.size()]);
    }

    /**
     * Send our own advertisement to all of the seed rendezvous.
     */
    @Override
    public synchronized RouteAdvertisement[] getActiveSeedRoutes() {
        RouteAdvertisement[] superseeds = super.getActiveSeedRoutes();       

        List<RouteAdvertisement> results = new ArrayList<RouteAdvertisement>(getRelayPeers());
        
        results.addAll(Arrays.asList(superseeds));
                
        return results.toArray(new RouteAdvertisement[results.size()]);
    }
    
    /**
     *  @return List of RouteAdvertisement
     */
    private Collection<RouteAdvertisement> getRelayPeers() {
        Collection<RouteAdvertisement> res = new ArrayList<RouteAdvertisement>();
        
        try {
            EndpointService ep = group.getEndpointService();
            
            Iterator it = ep.getAllMessageTransports();
            
            while (it.hasNext()) {
                MessageTransport mt = (MessageTransport) it.next();
                
                if (!mt.getEndpointService().getGroup().getPeerGroupID().equals(group.getPeerGroupID())) {
                    // We only want relay services in this peer group.
                    continue;
                }
                
                if (mt instanceof RelayClient) {
                    RelayClient er = (RelayClient) mt;
                    
                    RelayClient.RelayServerConnection current = er.currentServer;
                    
                    if (null == current) {
                        continue;
                    }
                    
                    RouteAdvertisement rdvAdv = current.relayAdv;

                    if (null == rdvAdv) {
                        continue;
                    }

                    res.add(rdvAdv.clone());
                }
            }
        } catch (Exception ez1) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Unexpected error getting relays", ez1);
            }
        }
        
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found " + res.size() + " relay seeds.");
        }
        
        return res;
    }
}
