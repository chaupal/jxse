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


import net.jxta.document.*;
import net.jxta.exception.JxtaError;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.cbjx.CbJxDefs;
import net.jxta.impl.membership.PasswdMembershipService;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Provides the implementation for the World Peer Group.
 * <p/>
 * Key differences from regular groups are:
 * <p/>
 * <ul>
 * <li>Provides a mechanism for peer group configuration parameter and for
 * reconfiguration via a plugin configurator.</li>
 * <li>Ensures that only a single instance of the World Peer Group exists
 * within the context of the current classloader.</li>
 * </ul>
 */
public class Platform extends StdPeerGroup {

    /**
     * Log4J Logger
     */
    private final static transient Logger LOG = Logger.getLogger(Platform.class.getName());

    /**
     * The Module Impl Advertisement we will return in response to
     * {@link #getAllPurposePeerGroupImplAdvertisement()} requests.
     */
    private ModuleImplAdvertisement allPurposeImplAdv = null;

    /**
     * This constructor was originally the standard constructor and must be retained in case the
     * platform is accidentally instantiated via the module loading infrastructure.
     *
     * @throws PeerGroupException if an initialization error occurs
     */
    public Platform() throws PeerGroupException {
        throw new JxtaError("Zero params constructor is no longer supported for Platform class.");
    }

    /**
     * Default constructor
     *
     * @param config    PlatformConfig
     * @param storeHome persistent store home
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
                // Build the platform's impl adv.
                implAdv = mkPlatformImplAdv();
            } catch (Throwable e) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Fatal Error making Platform Impl Adv", e);
                }
                throw new PeerGroupException("Fatal Error making Platform Impl Adv", e);
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
            throw new PeerGroupException("Failed to publish World Peer Group Advertisement", e);
        }
    }

    protected static ModuleImplAdvertisement mkPlatformImplAdv() throws Exception {

        // Start building the implAdv for the platform intself.
        ModuleImplAdvertisement platformDef = mkImplAdvBuiltin(PeerGroup.refPlatformSpecID, "World PeerGroup"
                ,
                "Standard World PeerGroup Reference Implementation");

        // Build the param section now.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();
        Hashtable protos = new Hashtable();
        Hashtable services = new Hashtable();
        Hashtable apps = new Hashtable();

        // Build ModuleImplAdvs for each of the modules
        ModuleImplAdvertisement moduleAdv;

        // Do the Services

        // "Core" Services
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refEndpointSpecID, "net.jxta.impl.endpoint.EndpointServiceImpl"
                ,
                "Reference Implementation of the Endpoint service");
        services.put(PeerGroup.endpointClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refResolverSpecID, "net.jxta.impl.resolver.ResolverServiceImpl"
                ,
                "Reference Implementation of the Resolver service");
        services.put(PeerGroup.resolverClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refMembershipSpecID, "net.jxta.impl.membership.none.NoneMembershipService"
                ,
                "Reference Implementation of the None Membership Service");
        services.put(PeerGroup.membershipClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refAccessSpecID, "net.jxta.impl.access.always.AlwaysAccessService"
                ,
                "Always Access Service");
        services.put(PeerGroup.accessClassID, moduleAdv);

        // "Standard" Services

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID, "net.jxta.impl.discovery.DiscoveryServiceImpl"
                ,
                "Reference Implementation of the Discovery service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID, "net.jxta.impl.rendezvous.RendezVousServiceImpl"
                ,
                "Reference Implementation of the Rendezvous Service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID, "net.jxta.impl.peer.PeerInfoServiceImpl"
                ,
                "Reference Implementation of the Peerinfo Service");
        services.put(PeerGroup.peerinfoClassID, moduleAdv);

        // Do the protocols

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refTcpProtoSpecID, "net.jxta.impl.endpoint.tcp.TcpTransport"
                ,
                "Reference Implementation of the TCP Message Transport");
        protos.put(PeerGroup.tcpProtoClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refHttpProtoSpecID, "net.jxta.impl.endpoint.servlethttp.ServletHttpTransport"
                ,
                "Reference Implementation of the HTTP Message Transport");
        protos.put(PeerGroup.httpProtoClassID, moduleAdv);

        // Do the Apps

        paramAdv.setServices(services);
        paramAdv.setProtos(protos);
        paramAdv.setApps(apps);

        // Pour the paramAdv in the platformDef
        platformDef.setParam((XMLDocument) paramAdv.getDocument(MimeMediaType.XMLUTF8));

        return platformDef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopApp() {
        super.stopApp();
    }

    /**
     * Returns the all purpose peer group implementation advertisement that
     * is most useful when called in the context of the platform group: the
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
        // For now, use the well know NPG naming, it is not
        // identical to the allPurpose PG because we use the class
        // ShadowPeerGroup which initializes the peer config from its
        // parent.
        ModuleImplAdvertisement implAdv = mkImplAdvBuiltin(PeerGroup.refNetPeerGroupSpecID, ShadowPeerGroup.class.getName()
                ,
                "Default NetPeerGroup reference implementation.");

        XMLElement paramElement = (XMLElement) implAdv.getParam();
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();
        ModuleImplAdvertisement moduleAdv;

        // set the services
        Hashtable services = new Hashtable();

        // "Core" Services

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refEndpointSpecID, "net.jxta.impl.endpoint.EndpointServiceImpl"
                ,
                "Reference Implementation of the Endpoint Service");
        services.put(PeerGroup.endpointClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refResolverSpecID, "net.jxta.impl.resolver.ResolverServiceImpl"
                ,
                "Reference Implementation of the Resolver Service");
        services.put(PeerGroup.resolverClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PSEMembershipService.pseMembershipSpecID, "net.jxta.impl.membership.pse.PSEMembershipService"
                ,
                "Reference Implementation of the PSE Membership Service");
        services.put(PeerGroup.membershipClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refAccessSpecID, "net.jxta.impl.access.always.AlwaysAccessService"
                ,
                "Always Access Service");
        services.put(PeerGroup.accessClassID, moduleAdv);

        // "Standard" Services

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID, "net.jxta.impl.discovery.DiscoveryServiceImpl"
                ,
                "Reference Implementation of the Discovery Service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID, "net.jxta.impl.rendezvous.RendezVousServiceImpl"
                ,
                "Reference Implementation of the Rendezvous Service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refPipeSpecID, "net.jxta.impl.pipe.PipeServiceImpl"
                ,
                "Reference Implementation of the Pipe Service");
        services.put(PeerGroup.pipeClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID, "net.jxta.impl.peer.PeerInfoServiceImpl"
                ,
                "Reference Implementation of the Peerinfo Service");
        services.put(PeerGroup.peerinfoClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refProxySpecID, "net.jxta.impl.proxy.ProxyService"
                ,
                "Reference Implementation of the JXME Proxy Service");
        services.put(PeerGroup.proxyClassID, moduleAdv);

        paramAdv.setServices(services);

        // High-level Transports.
        Hashtable protos = new Hashtable();

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refRouterProtoSpecID, "net.jxta.impl.endpoint.router.EndpointRouter"
                ,
                "Reference Implementation of the Router Message Transport");
        protos.put(PeerGroup.routerProtoClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refTlsProtoSpecID, "net.jxta.impl.endpoint.tls.TlsTransport"
                ,
                "Reference Implementation of the JXTA TLS Message Transport");
        protos.put(PeerGroup.tlsProtoClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(CbJxDefs.cbjxMsgTransportSpecID, "net.jxta.impl.endpoint.cbjx.CbJxTransport"
                ,
                "Reference Implementation of the JXTA Cryptobased-ID Message Transport");
        protos.put(CbJxDefs.msgtptClassID, moduleAdv);

        moduleAdv = mkImplAdvBuiltin(PeerGroup.refRelayProtoSpecID, "net.jxta.impl.endpoint.relay.RelayTransport"
                ,
                "Reference Implementation of the Relay Message Transport");
        protos.put(PeerGroup.relayProtoClassID, moduleAdv);

        paramAdv.setProtos(protos);

        // Main app is the shell
        // Build a ModuleImplAdv for the shell
        ModuleImplAdvertisement newAppAdv = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());

        // The shell's spec id is a canned one.
        newAppAdv.setModuleSpecID(PeerGroup.refShellSpecID);

        // Same compat than the group.
        newAppAdv.setCompat(implAdv.getCompat());
        newAppAdv.setUri(implAdv.getUri());
        newAppAdv.setProvider(implAdv.getProvider());

        // Make up a description
        newAppAdv.setDescription("JXTA Shell");

        // Tack in the class name
        newAppAdv.setCode("net.jxta.impl.shell.bin.Shell.Shell");

        // Put that in a new table of Apps and replace the entry in
        // paramAdv
        Hashtable newApps = new Hashtable();

        newApps.put(PeerGroup.applicationClassID, newAppAdv);
        paramAdv.setApps(newApps);

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
