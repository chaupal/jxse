/*
 * Copyright (c) 2002-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.membership.pse;

import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import junit.framework.*;
import net.jxta.exception.PeerGroupException;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import net.jxta.test.util.JUnitRuleMockery;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.SecureRandom;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.PSEUtils.IssuerInfo;

import net.jxta.credential.Credential;
import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import java.util.Hashtable;


import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import javax.crypto.EncryptedPrivateKeyInfo;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Map;

import java.net.URISyntaxException;

import junit.framework.*;
import org.junit.Ignore;

import net.jxta.credential.Credential;
import net.jxta.credential.PrivilegedOperation;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.discovery.DiscoveryService;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.membership.InteractiveAuthenticator;
import net.jxta.membership.MembershipService;

import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.PSEUtils.IssuerInfo;
import net.jxta.impl.protocol.PSEConfigAdv;

import net.jxta.impl.util.threads.TaskManager;

@Ignore
public class TestPSEMembershipServiceSupport {

    @Rule
    public static JUnitRuleMockery mockContext = new JUnitRuleMockery();

    public static PeerGroup createGroupWithPSEMembership(final PeerGroupID groupId, final String name, final java.io.File storeFile) {

            PSEConfigAdv pseConf = (PSEConfigAdv) AdvertisementFactory.newAdvertisement(
                    PSEConfigAdv.getAdvertisementType());

            PSEUtils.IssuerInfo info = PSEUtils.genCert("bob", null);

            pseConf.setCertificate(info.cert);
            pseConf.setPrivateKey(info.subjectPkey, "password".toCharArray());

            final PeerID peerId = IDFactory.newPeerID(PeerGroupID.worldPeerGroupID, info.cert.getPublicKey().getEncoded());

            net.jxta.impl.protocol.GroupConfig.Instantiator instantiator = new net.jxta.impl.protocol.GroupConfig.Instantiator();
            final net.jxta.impl.protocol.GroupConfig newConfigAdv = (net.jxta.impl.protocol.GroupConfig)instantiator.newInstance();

            XMLDocument pseDoc = (XMLDocument) pseConf.getDocument(MimeMediaType.XMLUTF8);

            newConfigAdv.putServiceParam(PeerGroup.membershipClassID, pseDoc);

            final PeerAdvertisement peerAdvertisement = buildPeer(peerId, PeerGroupID.worldPeerGroupID);

            final TaskManager taskManager = new TaskManager();

            final PeerGroup group = mockContext.mock(PeerGroup.class, name);
            mockContext.checking(new Expectations() {{
                    ignoring(group).getTaskManager(); will(returnValue(taskManager));
                    ignoring(group).getPeerAdvertisement(); will(returnValue(peerAdvertisement));
                    ignoring(group).getPeerID(); will(returnValue(peerId));
                    ignoring(group).getPeerGroupName(); will(returnValue(name));
                    ignoring(group).getPeerGroupID(); will(returnValue(groupId));
                    ignoring(group).getConfigAdvertisement(); will(returnValue(newConfigAdv));
            }});

            try {
                final FileKeyStoreManager keyStoreManager = new FileKeyStoreManager(null, null, storeFile);
                
                PSEMembershipService.setPSEKeyStoreManagerFactory(new PSEMembershipService.PSEKeyStoreManagerFactory() {
                    public KeyStoreManager getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {
                        return keyStoreManager;
                    }
                });
            } catch (NoSuchProviderException ex) {
                Logger.getLogger(TestPSEMembershipServiceSupport.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            } catch (KeyStoreException ex) {
                Logger.getLogger(TestPSEMembershipServiceSupport.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }


            PSEMembershipService newMembershipService = null;
            try {
                newMembershipService = new PSEMembershipService();
                newMembershipService.init(group, PeerGroup.membershipClassID, null);
                
            } catch (PeerGroupException ex) {
                Logger.getLogger(TestPSEMembershipServiceSupport.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }


            AuthenticationCredential authCred = new AuthenticationCredential(group, "StringAuthentication", null);

            StringAuthenticator auth = null;

            try {
                auth = (StringAuthenticator) newMembershipService.apply(authCred);

                auth.setAuth1_KeyStorePassword("password".toCharArray());
                auth.setAuth2Identity(group.getPeerID());
                auth.setAuth3_IdentityPassword("password".toCharArray());

                Credential newCred = newMembershipService.join(auth);
                
            } catch (Exception failed) {
                throw new RuntimeException(failed);
            }

            final PSEMembershipService membershipService = newMembershipService;

            if (membershipService.getDefaultCredential() == null)
                throw new RuntimeException("no default credential!");

            mockContext.checking(new Expectations() {{
                    ignoring(group).getMembershipService(); will(returnValue(membershipService));
            }});

            return group;
    }
    
    private static final String TestName = "Testing J2SE JXTA Peer";
    private static final String TestDescription = "Testing J2SE JXTA Peer desc";

    private static PeerAdvertisement buildPeer(PeerID peerId, PeerGroupID peerGroupID) {
        PeerAdvertisement peer = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(
                PeerAdvertisement.getAdvertisementType());

        peer.setPeerID(peerId);

        peer.setPeerGroupID(peerGroupID);

        peer.setName(TestName);
        peer.setDesc(buildDesc());

        return peer;
    }

    private static Element buildDesc() {
        StructuredTextDocument desc = (StructuredTextDocument) StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.XMLUTF8, "Desc");

        desc.appendChild(desc.createElement("Text1", TestDescription));
        desc.appendChild(desc.createElement("Text2", TestDescription));
        desc.appendChild(desc.createElement("Text3", TestDescription));
        return desc;
    }
}
