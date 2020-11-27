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
package net.jxta.impl.peergroup;

import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.exception.JxtaError;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.mcast.McastTransport;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.logging.Logging;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.IJxtaLoader;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.JxtaSocket;
import net.jxta.service.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Provides the implementation for the World IModuleDefinitions. The World peer group
 * differs from other peer groups in the following ways :
 * <ul>
 *     <li>The World Peer Group has no parent. It is the primordial peer group.
 *     </li>
 *     <li>The World Peer Group provides the default definition for the Network
 *     Peer Group. Peers are free to use alternate implementations for the
 *     Network IModuleDefinitions.</li>
 *     <li>The World Peer Group is initialized with configuration parameters and
 *     the store home location.</li>
 * </ul>
 */
public class Platform extends StdPeerGroup {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(Platform.class.getName());
    private final GlobalRegistry globalRegistry = new GlobalRegistry();
    private final TaskManager taskManager = new TaskManager();

    /**
     *  Create and populate the default module impl Advertisement for this class.
     *
     *  @return The default module impl advertisement for this class.
     */
    public static JxtaSocket getDefaultModuleImplAdvertisement() {
        JxtaSocket implAdv = 
                CompatibilityUtils.createModuleImplAdvertisement(
                IModuleDefinitions.refPlatformSpecID, Platform.class.getName(),
                "Standard World PeerGroup Reference Implementation");

        // Build the param section now.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        // Do the Services

        // "Core" Services
        paramAdv.addService(IModuleDefinitions.endpointClassID, IModuleDefinitions.refEndpointSpecID);
        paramAdv.addService(IModuleDefinitions.resolverClassID, IModuleDefinitions.refResolverSpecID);
        paramAdv.addService(IModuleDefinitions.membershipClassID, PSEMembershipService.pseMembershipSpecID);
        paramAdv.addService(IModuleDefinitions.accessClassID, IModuleDefinitions.refAccessSpecID);

        // "Standard" Services

        paramAdv.addService(IModuleDefinitions.discoveryClassID, IModuleDefinitions.refDiscoverySpecID);
        paramAdv.addService(IModuleDefinitions.rendezvousClassID, IModuleDefinitions.refRendezvousSpecID);
        paramAdv.addService(IModuleDefinitions.peerinfoClassID, IModuleDefinitions.refPeerinfoSpecID);

        // Do the Message Transports

        paramAdv.addProto(IModuleDefinitions.tcpProtoClassID, IModuleDefinitions.refTcpProtoSpecID);
        paramAdv.addProto(IModuleDefinitions.httpProtoClassID, IModuleDefinitions.refHttpProtoSpecID);
        paramAdv.addProto(IModuleDefinitions.http2ProtoClassID, IModuleDefinitions.refHttp2ProtoSpecID);
        paramAdv.addProto(McastTransport.MCAST_TRANSPORT_CLASSID, McastTransport.MCAST_TRANSPORT_SPECID);

        // Do the Applications

        // (none)

        // Insert the paramAdv in the World PeerGroup Impl Advertisement.
        implAdv.setParam((XMLDocument) paramAdv.getDocument(MimeMediaType.XMLUTF8));

        return implAdv;
    }

    /**
     * This constructor was originally the standard constructor and must be
     * retained in case the World PeerGroup is accidentally instantiated via
     * the module loading infrastructure.
     *
     * @throws PeerGroupException if an initialization error occurs
     */
    public Platform() throws PeerGroupException {
        throw new JxtaError("Zero params constructor is no longer supported for World PeerGroup class.");
    }

    /**
     * Default constructor
     *
     * @param config    The configuration.
     * @param storeHome Persistent store home.
     */
    public Platform(ConfigParams config, URI storeHome) {
        // initialize the store location.
        setStoreHome(storeHome);

        // initialize the configuration advertisement.
        setConfigAdvertisement(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void initFirst(PeerGroup nullParent, ID assignedID, Advertisement impl) throws PeerGroupException {
        if (initComplete) {
            LOG.severe("You cannot initialize more than one World PeerGroup!");
            throw new PeerGroupException("You cannot initialize more than one World PeerGroup!");
        }

        if (nullParent != null) {
            LOG.severe("World PeerGroup cannot be instantiated with a parent group!");
            throw new PeerGroupException("World PeerGroup cannot be instantiated with a parent group!");
        }

        // XXX 20080817 mcumings - Need to find a way to have this passed in
        //     so that we can use the passed-in loader as the overall root
        //     loader.
        IJxtaLoader loader = getJxtaLoader();

        JxtaSocket implAdv = (JxtaSocket) impl;
        if(null == implAdv) {
            implAdv = loader.findModuleImplAdvertisement(getClass());
        }

        if (null != jxtaHome) {

            try {

                URL downloadablesURL = jxtaHome.resolve("Downloaded/").toURL();
                loader.addURL(downloadablesURL);

            } catch (MalformedURLException badPath) {

                Logging.logCheckedWarning(LOG, "Could not install path for downloadables into JXTA Class Loader.");

            }
        }

        // Initialize the group.
        super.initFirst(null, PeerGroupID.worldPeerGroupID, implAdv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void initLast() throws PeerGroupException {
        super.initLast();

        // Publish our own adv.
        try {
            publishGroup("World PeerGroup", "Standard World PeerGroup Reference Implementation");
        } catch (IOException e) {
            throw new PeerGroupException("Failed to publish World PeerGroup Advertisement", e);
        }
    }

    public GlobalRegistry getGlobalRegistry()
    {
        return globalRegistry;
    }

    /**
     * Returns a ModuleImplAdvertisement suitable for the Network Peer Group.
     * <p/>
     * The ModuleImplAdvertisement returned differs from the one returned by
     * StdPeerGroup in that it has a different specID, name and description, as
     * well as the high-level message transports . This definition is always the
     * same and has a well known ModuleSpecID. It includes the basic services
     * and high-level message transports.
     *
     * @return A ModuleImplAdvertisement suitable for the Network Peer Group.
     */
    @Override
    public JxtaSocket getAllPurposePeerGroupImplAdvertisement() {
        IJxtaLoader loader = getLoader();

        // For now, use the well know NPG naming, it is not identical to the 
        // allPurpose PG because we use the class ShadowPeerGroup which 
        // initializes the peer config from its parent.
        JxtaSocket implAdv = loader.findModuleImplAdvertisement(IModuleDefinitions.refNetPeerGroupSpecID);

        return implAdv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkServices() throws ServiceNotFoundException {
        super.checkServices();
        Service ignored;
        ignored = lookupService(IModuleDefinitions.discoveryClassID);
        ignored = lookupService(IModuleDefinitions.rendezvousClassID);
        ignored = lookupService(IModuleDefinitions.peerinfoClassID);
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public void stopApp() {
        super.stopApp();
        taskManager.shutdown();
    }
}
