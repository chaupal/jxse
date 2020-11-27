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
import net.jxta.document.XMLElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.content.ContentServiceImpl;
//import net.jxta.impl.membership.none.NoneMembershipService;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.JxtaSocket;

/**
 * ShadowPeerGroup is almost a regular StdPeerGroup, except that it borrows its
 * parent's configuration. The only real use is for the Net Peer Group.
 */
public class ShadowPeerGroup extends StdPeerGroup {

//    /**
//     *  Our application is the JXSE Shell.
//     */
//    private Application shell = null;

    /**
     *  Create and populate the default module impl Advertisement for this class.
     *
     *  @return The default module impl advertisement for this class.
     */
    public static JxtaSocket getDefaultModuleImplAdvertisement() {
        JxtaSocket implAdv = CompatibilityUtils.createModuleImplAdvertisement(
                IModuleDefinitions.refNetPeerGroupSpecID, ShadowPeerGroup.class.getName(),
                "Default Network PeerGroup reference implementation");

        // Build the param section now.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        // Set the services
        // "Core" Services
        paramAdv.addService(IModuleDefinitions.endpointClassID, IModuleDefinitions.refEndpointSpecID);
        paramAdv.addService(IModuleDefinitions.resolverClassID, IModuleDefinitions.refResolverSpecID);
        paramAdv.addService(IModuleDefinitions.membershipClassID, PSEMembershipService.pseMembershipSpecID);
        paramAdv.addService(IModuleDefinitions.accessClassID, IModuleDefinitions.refAccessSpecID);

        // "Standard" Services
        paramAdv.addService(IModuleDefinitions.discoveryClassID, IModuleDefinitions.refDiscoverySpecID);
        paramAdv.addService(IModuleDefinitions.rendezvousClassID, IModuleDefinitions.refRendezvousSpecID);
        paramAdv.addService(IModuleDefinitions.pipeClassID, IModuleDefinitions.refPipeSpecID);
        paramAdv.addService(IModuleDefinitions.peerinfoClassID, IModuleDefinitions.refPeerinfoSpecID);
//        paramAdv.addService(IModuleDefinitions.proxyClassID, IModuleDefinitions.refProxySpecID);
        paramAdv.addService(IModuleDefinitions.contentClassID, ContentServiceImpl.MODULE_SPEC_ID);

        /**
         * NPG used to be a StdPeerGroup in 2.5, however, it should be a ShadowIModuleDefinitions.
         * StdPeerGroup does defines the below protos transports. There is no reason why
         * the NPG should define tls and CbJxDefs in 2.6.
         */
//        // High-level Message Transports.
        paramAdv.addProto(IModuleDefinitions.routerProtoClassID, IModuleDefinitions.refRouterProtoSpecID);
        paramAdv.addProto(IModuleDefinitions.tlsProtoClassID, IModuleDefinitions.refTlsProtoSpecID);
//        paramAdv.addProto(CbJxDefs.msgtptClassID, CbJxDefs.cbjxMsgTransportSpecID);
        paramAdv.addProto(IModuleDefinitions.relayProtoClassID, IModuleDefinitions.refRelayProtoSpecID);

        // Pour our newParamAdv in implAdv
        XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);

        implAdv.setParam(paramElement);

        return implAdv;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>This implementation initializes the configuration advertisement with
     * that of the parent group and otherwise behave exactly like its superclass.
     */
    @Override
    protected void initFirst(PeerGroup parent, ID assignedID, Advertisement impl) throws PeerGroupException {

        // Set the peer configuration before we start.
        setConfigAdvertisement(parent.getConfigAdvertisement());

        // Do the regular stuff now.
        super.initFirst(parent, assignedID, impl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initLast() throws PeerGroupException {
        // Nothing special, but that could change in the future
        // Just remember that the possibility exists.
        super.initLast();
    }

    /**
     * {@inheritDoc}
     * <p/>
     */
    @Override
    public int startApp(String[] args) {
        int result = super.startApp(args);

        if (Module.START_OK != result) {
            return result;
        }

//        // Main app is the shell (if it is available).
//        if (null != getLoader().findModuleImplAdvertisement(IModuleDefinitions.refShellSpecID)) {
//            shell = (Application) loadModule(IModuleDefinitions.applicationClassID, IModuleDefinitions.refShellSpecID, IModuleDefinitions.Both);
//
//            if (null == shell) {
//                return -1;
//            }
//
//            result = shell.startApp(new String[0]);
//        }

        return result;
    }

    /**
     * {@inheritDoc}
     * <p/>If we started the shell we now must stop it.
     */
    @Override
    public void stopApp() {

//        if (null != shell) {
//            shell.stopApp();
//            shell = null;
//        }

        super.stopApp();
    }
}
