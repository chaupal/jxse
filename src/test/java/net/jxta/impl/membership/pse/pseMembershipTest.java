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
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.membership.InteractiveAuthenticator;
import net.jxta.membership.MembershipService;

import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.PSEUtils.IssuerInfo;
import net.jxta.impl.protocol.PSEConfigAdv;


public class pseMembershipTest extends TestCase {
    
    static PeerGroup npg = null;
    static PeerGroup pg = null;
    
    public pseMembershipTest(java.lang.String testName) {
        super(testName);
        synchronized (pseMembershipTest.class) {
            try {
                if (null == npg) {
                    npg = PeerGroupFactory.newNetPeerGroup();
                    // npg.startApp( new String[0]);
                    
                    ModuleImplAdvertisement newGroupImpl = npg.getAllPurposePeerGroupImplAdvertisement();
                    
                    StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(newGroupImpl.getParam());
                    
                    Map services = params.getServices();
                    
                    ModuleImplAdvertisement aModuleAdv = (ModuleImplAdvertisement) services.get(PeerGroup.membershipClassID);

                    services.remove(PeerGroup.membershipClassID);
                    
                    ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(
                            ModuleImplAdvertisement.getAdvertisementType());
                    
                    implAdv.setModuleSpecID(PSEMembershipService.pseMembershipSpecID);
                    implAdv.setCompat(aModuleAdv.getCompat());
                    implAdv.setCode(PSEMembershipService.class.getName());
                    implAdv.setUri(aModuleAdv.getUri());
                    implAdv.setProvider(aModuleAdv.getProvider());
                    implAdv.setDescription("PSE Membership Service");
                    
                    // replace it
                    services.put(PeerGroup.membershipClassID, implAdv);
                    
                    newGroupImpl.setParam((Element) params.getDocument(MimeMediaType.XMLUTF8));
                    
                    // XXX bondolo 20041014 if we knew we were going to create many of this type of group we would use a well known id.
                    newGroupImpl.setModuleSpecID(IDFactory.newModuleSpecID(newGroupImpl.getModuleSpecID().getBaseClass()));
                    
                    npg.getDiscoveryService().publish(newGroupImpl, PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
                    
                    npg.getDiscoveryService().remotePublish(newGroupImpl, PeerGroup.DEFAULT_LIFETIME);
                    
                    PeerGroupAdvertisement newPGAdv = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(
                            PeerGroupAdvertisement.getAdvertisementType());
                    
                    newPGAdv.setPeerGroupID(IDFactory.newPeerGroupID());
                    newPGAdv.setModuleSpecID(newGroupImpl.getModuleSpecID());
                    newPGAdv.setName("Test Group");
                    newPGAdv.setDescription("Created by Unit Test");
                    
                    PSEConfigAdv pseConf = (PSEConfigAdv) AdvertisementFactory.newAdvertisement(
                            PSEConfigAdv.getAdvertisementType());
                    
                    PSEUtils.IssuerInfo info = PSEUtils.genCert("bob", null);
                    
                    pseConf.setCertificate(info.cert);
                    pseConf.setPrivateKey(info.subjectPkey, "password".toCharArray());
                    
                    XMLDocument pseDoc = (XMLDocument) pseConf.getDocument(MimeMediaType.XMLUTF8);
                    
                    newPGAdv.putServiceParam(PeerGroup.membershipClassID, pseDoc);
                    
                    npg.getDiscoveryService().publish(newPGAdv, PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
                    
                    npg.getDiscoveryService().remotePublish(newPGAdv, PeerGroup.DEFAULT_LIFETIME);
                    
                    pg = npg.newGroup(newPGAdv);
                }
            } catch (Throwable all) {
                all.printStackTrace();
                fail("exception thrown : " + all.getMessage());
            }
        }
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());

        synchronized (pseMembershipTest.class) {
            if (null != pg) {
                pg.stopApp();
                pg.unref();
                pg = null;
            }
            
            if (null != npg) {
                npg.stopApp();
                npg.unref();
                npg = null;
            }
        }

        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(pseMembershipTest.class);

        return suite;
    }
    
    public void testLogin() {
        try {
            MembershipService membership = pg.getMembershipService();
            
            membership.resign();
            
            assertTrue("Should be null default", (null == membership.getDefaultCredential()));
            
            AuthenticationCredential authCred = new AuthenticationCredential(pg, "StringAuthentication", null);
            
            StringAuthenticator auth = null;

            try {
                auth = (StringAuthenticator) membership.apply(authCred);
            } catch (Exception failed) {
                ;
            }
            
            if (null != auth) {
                auth.setAuth1_KeyStorePassword("password".toCharArray());
                auth.setAuth2Identity(pg.getPeerID());
                auth.setAuth3_IdentityPassword("password".toCharArray());
                
                assertTrue("should have been ready", auth.isReadyForJoin());
                
                Credential newCred = membership.join(auth);
                
                assertTrue("Should have returned a credential", (null != newCred));
                
                assertTrue("Should be default credential", (newCred == membership.getDefaultCredential()));
            }
        } catch (Throwable all) {
            all.printStackTrace();
            fail("exception thrown : " + all.getMessage());
        }
    }
    
    // public void testInteractiveLogin() {
    // try {
    // MembershipService membership = pg.getMembershipService();
    //
    // membership.resign();
    //
    // AuthenticationCredential authCred = new AuthenticationCredential( pg, "InteractiveAuthentication", null );
    //
    // InteractiveAuthenticator auth = (InteractiveAuthenticator) membership.apply( authCred );
    //
    // if( auth.interact() ) {
    // assertTrue( "should have been ready",  auth.isReadyForJoin() );
    // membership.join( auth );
    // }
    // }   catch( Throwable all ) {
    // all.printStackTrace();
    // fail("exception thrown : " + all.getMessage());
    // }
    // }
    
    public void testPKCS5() {
        try {
            IssuerInfo test = PSEUtils.genCert("test", null);
            
            EncryptedPrivateKeyInfo encPrivKey = PSEUtils.pkcs5_Encrypt_pbePrivateKey("password".toCharArray(), test.subjectPkey
                    ,
                    500);
            
            assertNotNull("Could not encrypt Private Key", encPrivKey);
            
            PrivateKey decPrivKey = PSEUtils.pkcs5_Decrypt_pbePrivateKey("password".toCharArray(), test.subjectPkey.getAlgorithm()
                    ,
                    encPrivKey);
            
            assertNotNull("Could not decrypt Private Key", decPrivKey);
            
            Arrays.equals(test.subjectPkey.getEncoded(), decPrivKey.getEncoded());
            
            byte[] encPrivKeyDer = encPrivKey.getEncoded();
            
            EncryptedPrivateKeyInfo deserialedencPrivKey = new EncryptedPrivateKeyInfo(encPrivKeyDer);
            
            decPrivKey = PSEUtils.pkcs5_Decrypt_pbePrivateKey("password".toCharArray(), test.subjectPkey.getAlgorithm()
                    ,
                    deserialedencPrivKey);
            
            assertNotNull("Could not decrypt Private Key", decPrivKey);
            
            Arrays.equals(test.subjectPkey.getEncoded(), decPrivKey.getEncoded());
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
}
