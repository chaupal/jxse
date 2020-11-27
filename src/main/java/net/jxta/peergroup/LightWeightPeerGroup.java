/*
 * Copyright (c) 2003-2007 Sun Microsystems, Inc.  All rights reserved.
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
package net.jxta.peergroup;

import net.jxta.access.AccessService;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.pipe.PipeService;
import net.jxta.platform.IJxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.JxtaSocket;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.resolver.ResolverService;
import net.jxta.service.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Logger;

import net.jxta.content.ContentService;

/**
 * LightWeightPeerGroup is a class intended to help
 * building PeerGroup that can inherit one or more
 * services from a parent PeerGroup.
 * <p/>
 * An LightWeightPeerGroup implements PeerGroup and is to
 * be used like a PeerGroup by applications.
 * <p/>
 * This class is intended to be extended/implemented.
 * <p/>
 * Note: LightweightPeergroup permits the implementation of peer groups that 
 * borrow all or part of their services from their parent group. One needs to 
 * remember that peers in various such subgroups of a given parent groups may
 * implicitly all share the same services if that is what the PeerGroup
 * implementing LightWeightPeerGroup is doing. Please refer to the documentation
 * of the Peer Group inmplmentations extending LightWeigthPeerGroup to 
 * understand which services are shared, and which are not.
 * 
 * @since JXTA JSE 2.2
 */
public class LightWeightPeerGroup implements PeerGroup {

    /**
     *  Logger
     */
    private static final Logger LOG = Logger.getLogger(LightWeightPeerGroup.class.getName());
    private PeerGroup group = null;
    private ID assignedID = null;
    private JxtaSocket implAdv = null;
    private final PeerGroupAdvertisement adv;

    /**
     * Constructor
     * <p/>
     * All classes that extend this class must invoke this constructor.
     *
     * @param adv PeerGroupAdvertisement of this LightWeightPeerGroup.
     *            Note that only the PeerGroupID is used.
     */
    public LightWeightPeerGroup(PeerGroupAdvertisement adv) {
        this.adv = adv;
    }

    /**
     * {@inheritDoc}
     */
    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) {
        this.group = group;
        this.assignedID = assignedID;
        this.implAdv = (JxtaSocket) implAdv;
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        if (null == group) {
            Logging.logCheckedSevere(LOG, "No base peer group defined.");
            return -1;
        }

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public void stopApp() {
    }

//    /**
//     * {@inheritDoc}
//     */
//    public PeerGroup getInterface() {
//        return this;
//    }

    /**
     * {@inheritDoc}
     */
    public JxtaSocket getImplAdvertisement() {
        return implAdv;
    }

    public GlobalRegistry getGlobalRegistry()
    {
        return group.getGlobalRegistry(); 
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Deprecated
//    public ThreadGroup getHomeThreadGroup() {
//        if (group != null) {
//            return group.getHomeThreadGroup();
//        } else {
//            return null;
//        }
//    }

    /**
     * {@inheritDoc}
     */
    public URI getStoreHome() {
        if (group != null) {
            return group.getStoreHome();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public IJxtaLoader getLoader() {
        if (group != null) {
            return ((LightWeightPeerGroup) group).getLoader();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup getParentGroup() {

        try {
            return group;
        } catch (Exception ex) {
            Logging.logCheckedFine(LOG, "LightWeightPeerGroup is a base PeerGroup: no parent");
            throw new RuntimeException("LightWeightPeerGroup is a base PeerGroup: no parent");
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean isRendezvous() {
        return group != null && group.isRendezvous();
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        if (adv != null) {
            return adv;
        } else if (group != null) {
            return group.getPeerGroupAdvertisement();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerAdvertisement getPeerAdvertisement() {
        if (group != null) {
            return group.getPeerAdvertisement();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Service lookupService(ID name) throws ServiceNotFoundException {
        if (group != null) {
            return group.lookupService(name);
        } else {
            throw new ServiceNotFoundException("Not implemented");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Service lookupService(ID name, int ignoredForNow) throws ServiceNotFoundException {
        if (group != null) {
            return group.lookupService(name);
        } else {
            throw new ServiceNotFoundException("Not implemented");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getRoleMap(ID name) {
        if (group != null) {
            return group.getRoleMap(name);
        } else {
            // No translation; use the given name in a singleton.
            return Collections.singletonList(name).iterator();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean compatible(Element compat) {
        return group != null && group.compatible(compat);
    }

    /**
     * {@inheritDoc}
     */
    public Module loadModule(ID assignedID, Advertisement impl) throws ProtocolNotSupportedException, PeerGroupException {
        if (group != null) {
            return group.loadModule(assignedID, impl);
        } else {
            throw new ProtocolNotSupportedException("LightWeightPeerGroup does not implement this operation");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Module loadModule(ID assignedID, ModuleSpecID specID, int where) {
        if (group != null) {
            return group.loadModule(assignedID, specID, where);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void publishGroup(String name, String description) throws IOException {
        if (group != null) {
            group.publishGroup(name, description);
        } else {
            throw new IOException("Not implemented");
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(Advertisement pgAdv) throws PeerGroupException {
        if (group != null) {
            return group.newGroup(pgAdv);
        } else {
            throw new PeerGroupException("Not implemented");
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description) throws PeerGroupException {
        if (group != null) {
            return group.newGroup(gid, impl, name, description, true);
        } else {
            throw new PeerGroupException("Not implemented");
        }
    }

    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description, boolean publish) throws PeerGroupException {
        if (group != null) {
            return group.newGroup(gid, impl, name, description, publish);
        } else {
            throw new PeerGroupException("Not implemented");
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid) throws PeerGroupException {
        if (group != null) {
            return group.newGroup(gid);
        } else {
            throw new PeerGroupException("Not implemented");
        }
    }

    /*
     * shortcuts to the well-known services, in order to avoid calls to lookup.
     */

    /**
     * {@inheritDoc}
     */
    public RendezVousService getRendezVousService() {
        if (group != null) {
            return group.getRendezVousService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public EndpointService getEndpointService() {

        if (group != null) {
            return group.getEndpointService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResolverService getResolverService() {
        if (group != null) {
            return group.getResolverService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public DiscoveryService getDiscoveryService() {
        if (group != null) {
            return group.getDiscoveryService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerInfoService getPeerInfoService() {
        if (group != null) {
            return group.getPeerInfoService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public MembershipService getMembershipService() {
        if (group != null) {
            return group.getMembershipService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public PipeService getPipeService() {
        if (group != null) {
            return group.getPipeService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public AccessService getAccessService() {
        if (group != null) {
            return group.getAccessService();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentService getContentService() {
        if (group != null) {
            return group.getContentService();
        } else {
            return null;
        }
    }

    /*
     * A few convenience methods. This information is available from
     * the peer and peergroup advertisement.
     */

    /**
     * {@inheritDoc}
     */
    public PeerGroupID getPeerGroupID() {
        if (adv != null) {
            return (PeerGroupID) adv.getID();
        } else if (group != null) {
            return group.getPeerGroupID();
        } else {
            throw new RuntimeException("No PeerGroupID");
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerID getPeerID() {
        if (group != null) {
            return group.getPeerID();
        } else {
            throw new RuntimeException("No PeerID");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerGroupName() {
        if (adv != null) {
            return adv.getName();
        } else if (group != null) {
            return group.getPeerGroupName();
        } else {
            throw new RuntimeException("No name");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerName() {
        if (group != null) {
            return group.getPeerName();
        } else {
            throw new RuntimeException("No name");
        }
    }

    /**
     * {@inheritDoc}
     */
    public ConfigParams getConfigAdvertisement() {
        if (group != null) {
            return group.getConfigAdvertisement();
        } else {
            throw new RuntimeException("No ConfigAdvertisement");
        }
    }

    /**
     * {@inheritDoc}
     */
    public JxtaSocket getAllPurposePeerGroupImplAdvertisement() throws Exception {
        if (group != null) {
            return group.getAllPurposePeerGroupImplAdvertisement();
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

//    /**
//     * {@inheritDoc}
//     */
//    public boolean unref() {
//        return true;
//    }

    /**
     * {@inheritDoc}
     * <p/>
     * A LightWeightPeerGroup is already a weak reference that is not shareable,
     * therefore, return self as a weak reference.
     */
    public PeerGroup getWeakInterface() {
        return this;
    }

    public TaskManager getTaskManager() {
        if(group != null) {
            return group.getTaskManager();
        } else {
            throw new RuntimeException("No wrapped group initialised to delegate to");
        }
    }

}
