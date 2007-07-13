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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.exception.JxtaError;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.cbjx.CbJxDefs;
import net.jxta.impl.membership.PasswdMembershipService;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;


/**
 * Provides the implementation for the World PeerGroup.
 * <p/>
 * Key differences from regular groups are:
 * <p/>
 * <ul>
 *     <li>Provides a mechanism for peer group configuration parameter.</li>
 *
 *     <li>Ensures that only a single instance of the World PeerGroup exists
 *     within the context of the current classloader.</li>
 * </ul>
 */
public class Platform extends StdPeerGroup {

    /**
     *  Logger
     */
    private final static transient Logger LOG = Logger.getLogger(Platform.class.getName());

    /**
     * The Module Impl Advertisement we will return in response to
     * {@link #getAllPurposePeerGroupImplAdvertisement()} requests.
     */
    private ModuleImplAdvertisement allPurposeImplAdv = null;

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
     * @param config The configuration.
     * @param storeHome Persistent store home.
     */
    public Platform(ConfigParams config, URI storeHome) {
        // initialize the store location.
        jxtaHome = storeHome;

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

        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) impl;

        if (nullParent != null) {
            LOG.severe("World PeerGroup cannot be instantiated with a parent group!");
            throw new PeerGroupException("World PeerGroup cannot be instantiated with a parent group!");
        }

        // if we weren't given a module impl adv then make one from scratch.
        if (null == implAdv) {
            try {
                // Build the World PeerGroup's impl adv.
                implAdv = mkWorldPeerGroupImplAdv();
            } catch (Throwable e) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Fatal Error making World PeerGroup Impl Adv", e);
                }
                throw new PeerGroupException("Fatal Error making World PeerGroup Impl Adv", e);
            }
        }

        if (null != jxtaHome) {
            try {
                URL downloadablesURL = jxtaHome.resolve("Downloaded/").toURL();

                getJxtaLoader().addURL(downloadablesURL);
            } catch (MalformedURLException badPath) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Could not install path for downloadables into JXTA Class Loader.");
                }
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
        // Nothing special for now, but we might want to move some steps
        // from initFirst, in the future.
        super.initLast();

        // XXX bondolo 20040415 Hack to initialize class loader with specID for password membership
        // This is to provide compatibility with apps which imported passwd membership directly.
        ModuleSpecID id = PasswdMembershipService.passwordMembershipSpecID;

        // Publish our own adv.
        try {
            publishGroup("World PeerGroup", "Standard World PeerGroup Reference Implementation");
        } catch (IOException e) {
            throw new PeerGroupException("Failed to publish World PeerGroup Advertisement", e);
        }
    }

    protected static ModuleImplAdvertisement mkWorldPeerGroupImplAdv() throws Exception {

        // Start building the implAdv for the World PeerGroup intself.
        ModuleImplAdvertisement worldGroupDef = mkImplAdvBuiltin(PeerGroup.refPlatformSpecID, 
                "World PeerGroup",
                "Standard World PeerGroup Reference Implementation");

        // Build the param section now.

        // Build ModuleImplAdvs for each of the modules
        JxtaLoader loader = getJxtaLoader();
        ModuleImplAdvertisement moduleAdv;
        
        // Do the Services

        Map<ModuleClassID, Object> services = new HashMap<ModuleClassID, Object>();

        // "Core" Services
        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refEndpointSpecID);
        services.put(PeerGroup.endpointClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refResolverSpecID);
        services.put(PeerGroup.resolverClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refMembershipSpecID);
        services.put(PeerGroup.membershipClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refAccessSpecID);
        services.put(PeerGroup.accessClassID, moduleAdv);

        // "Standard" Services

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refDiscoverySpecID);
        services.put(PeerGroup.discoveryClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refRendezvousSpecID);
        services.put(PeerGroup.rendezvousClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refPeerinfoSpecID);
        services.put(PeerGroup.peerinfoClassID, moduleAdv);

        // Do the protocols

        Map<ModuleClassID, Object> protos = new HashMap<ModuleClassID, Object>();

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refTcpProtoSpecID);
        protos.put(PeerGroup.tcpProtoClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refHttpProtoSpecID);
        protos.put(PeerGroup.httpProtoClassID, moduleAdv);

        // Do the Apps
        
        Map<ModuleClassID, Object> apps = new HashMap<ModuleClassID, Object>();

        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        paramAdv.setServices(services);
        paramAdv.setProtos(protos);
        paramAdv.setApps(apps);

        // Pour the paramAdv in the World PeerGroup Def
        worldGroupDef.setParam((XMLDocument) paramAdv.getDocument(MimeMediaType.XMLUTF8));

        return worldGroupDef;
    }

    /**
     * Returns the all purpose peer group implementation advertisement that
     * is most useful when called in the context of the World PeerGroup: the
     * description of an infrastructure group.
     * <p/>
     * This definition is always the same and has a well known ModuleSpecID.
     * It includes the basic service, high-level transports and the shell for
     * main application. It differs from the one returned by StdPeerGroup only
     * in that it includes the high-level transports (and different specID,
     * name and description, of course). However, in order to avoid confusing
     * inheritance schemes (class hierarchy is inverse of object hierarchy)
     * other possible dependency issues, we just redefine it fully, right here.
     * <p/>
     * The user must remember to change the specID if the set of services
     * protocols or applications is altered before use.
     *
     * @return ModuleImplAdvertisement The new peergroup impl adv.
     */
    @Override
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {

        // Build it only the first time; then clone it.
        if (allPurposeImplAdv != null) {
            return allPurposeImplAdv.clone();
        }

        // Make a new impl adv
        // For now, use the well know NPG naming, it is not identical to the 
        // allPurpose PG because we use the class ShadowPeerGroup which 
        // initializes the peer config from its parent.
        ModuleImplAdvertisement implAdv = mkImplAdvBuiltin(PeerGroup.refNetPeerGroupSpecID, 
                ShadowPeerGroup.class.getName(),
                "Default Network PeerGroup reference implementation");

        XMLElement paramElement = (XMLElement) implAdv.getParam();
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();
        JxtaLoader loader = getJxtaLoader();
        ModuleImplAdvertisement moduleAdv;

        // set the services
        Map<ModuleClassID, Object> services = new HashMap<ModuleClassID, Object>();

        // "Core" Services

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refEndpointSpecID);
        services.put(PeerGroup.endpointClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refResolverSpecID);
        services.put(PeerGroup.resolverClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PSEMembershipService.pseMembershipSpecID);
        services.put(PeerGroup.membershipClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refAccessSpecID);
        services.put(PeerGroup.accessClassID, moduleAdv);

        // "Standard" Services

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refDiscoverySpecID);
        services.put(PeerGroup.discoveryClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refRendezvousSpecID);
        services.put(PeerGroup.rendezvousClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refPipeSpecID);
        services.put(PeerGroup.pipeClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refPeerinfoSpecID);
        services.put(PeerGroup.peerinfoClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refProxySpecID);
        services.put(PeerGroup.proxyClassID, moduleAdv);

        paramAdv.setServices(services);

        // High-level Transports.
        
        Map<ModuleClassID, Object> protos = new HashMap<ModuleClassID, Object>();

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refRouterProtoSpecID);
        protos.put(PeerGroup.routerProtoClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refTlsProtoSpecID);
        protos.put(PeerGroup.tlsProtoClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(CbJxDefs.cbjxMsgTransportSpecID);
        protos.put(CbJxDefs.msgtptClassID, moduleAdv);

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refRelayProtoSpecID);
        protos.put(PeerGroup.relayProtoClassID, moduleAdv);

        paramAdv.setProtos(protos);

        // Main app is the shell

        Map<ModuleClassID, Object> apps = new HashMap<ModuleClassID, Object>();

        moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refShellSpecID);
        if(null != moduleAdv) {        
            apps.put(PeerGroup.applicationClassID, moduleAdv);
        }
        paramAdv.setApps(apps);

        // Pour our newParamAdv in implAdv
        paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);

        implAdv.setParam(paramElement);

        allPurposeImplAdv = implAdv;

        return implAdv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getStoreHome() {
        return jxtaHome;
    }
}
