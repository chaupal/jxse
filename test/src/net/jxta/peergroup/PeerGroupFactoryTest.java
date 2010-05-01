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

package net.jxta.peergroup;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.PeerGroupFactory;

import net.jxta.impl.util.TimeUtils;

/**
 * Test the PeerGroupFactory.
 * 
 * @deprecated PeerGroupFactory is deprecated but still needs to be tested....
 */
@Deprecated
public class PeerGroupFactoryTest extends TestCase {
    
    public static void main(java.lang.String[] args) {
        TestRunner.run(suite());
           
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(PeerGroupFactoryTest.class);

        return suite;
    }
    
    private static void waitForPeerGroupShutdown(PeerGroupID pgid, long maxWait) throws Exception {
        
        long waitUntil = TimeUtils.toAbsoluteTimeMillis(maxWait);
                
        while (TimeUtils.timeNow() < waitUntil) {
            if (!PeerGroup.globalRegistry.registeredInstance(pgid)) {
                return;
            }
            
            Thread.sleep(TimeUtils.ASECOND);
        }
        
        fail("PeerGroup did not shut down within expected time.");        
    }
    
    public void testNewPlatform() {
        try {
            PeerGroup wpg = PeerGroupFactory.newPlatform();
                                    
            assertTrue("Group ID was not as expected.", wpg.getPeerGroupID().equals(PeerGroupID.worldPeerGroupID));
            
            Thread.sleep(TimeUtils.ASECOND * 10);
            
            wpg.unref();
            wpg = null;
            
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }    
        
    public void testNewPlatformTwice() {
        try {
            testNewPlatform();
            testNewPlatform();
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }    
        
    public void testSimpleNewNetPeerGroup() {
        try {
            PeerGroup npg = PeerGroupFactory.newNetPeerGroup();
                                   
            assertTrue("Group ID was not as expected.", npg.getPeerGroupID().equals(PeerGroupID.defaultNetPeerGroupID));
            
            Thread.sleep(TimeUtils.ASECOND * 10);
                        
            npg.unref();
            npg = null;
            
            waitForPeerGroupShutdown(PeerGroupID.defaultNetPeerGroupID, 60 * TimeUtils.ASECOND);
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testParentedNewNetPeerGroup() {
        try {
            PeerGroup wpg = PeerGroupFactory.newPlatform();
                                    
            assertTrue("Group ID was not as expected.", wpg.getPeerGroupID().equals(PeerGroupID.worldPeerGroupID));
            
            Thread.sleep(TimeUtils.ASECOND * 10);

            PeerGroup npg = PeerGroupFactory.newNetPeerGroup(wpg);
                                   
            assertTrue("Group ID was not as expected.", npg.getPeerGroupID().equals(PeerGroupID.defaultNetPeerGroupID));
                         
            wpg.unref();
            wpg = null;            
            
            Thread.sleep(TimeUtils.ASECOND * 10);
                        
            npg.unref();
            npg = null;
           
            waitForPeerGroupShutdown(PeerGroupID.defaultNetPeerGroupID, 60 * TimeUtils.ASECOND);
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
   
    public void testCustomNewNetPeerGroup() {
        try {
            PeerGroupID pgid = (PeerGroupID) ID.create(URI.create("urn:jxta:uuid-7FA4D7164D7B4BF9888EF7C8B2D0D46702"));

            PeerGroupFactory.setNetPGID(pgid);
            PeerGroupFactory.setNetPGName("Custom Net Group");
            PeerGroupFactory.setNetPGDesc("Custom Net Peer Group");
            
            PeerGroup npg = PeerGroupFactory.newNetPeerGroup();
                                   
            assertTrue("Group ID was not as expected.", npg.getPeerGroupID().equals(pgid));    
            
            Thread.sleep(TimeUtils.ASECOND * 10);
                        
            npg.unref();
            npg = null;
           
            waitForPeerGroupShutdown(pgid, 60 * TimeUtils.ASECOND);
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
            
            // reset
            PeerGroupFactory.setNetPGID(null);
            PeerGroupFactory.setNetPGName(null);
            PeerGroupFactory.setNetPGDesc(null);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
        
    public void testSetHomeNewNetPeerGroup() {
        try {
            File tmpHome = new File(System.getProperty("java.io.tmpdir"), "test" + Double.toHexString(Math.random()));

            tmpHome.mkdirs();
            
            URI original = PeerGroupFactory.getStoreHome();

            PeerGroupFactory.setStoreHome(tmpHome.toURI());
            
            PeerGroup npg = PeerGroupFactory.newNetPeerGroup();
                                              
            Thread.sleep(TimeUtils.ASECOND * 10);
            
            assertTrue("Store home location was not as expected", npg.getStoreHome().equals(tmpHome.toURI()));
            
            npg.unref();
            npg = null;
           
            waitForPeerGroupShutdown(PeerGroupID.defaultNetPeerGroupID, 60 * TimeUtils.ASECOND);
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
            
            // reset
            PeerGroupFactory.setStoreHome(original);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testConfigPropertiesNewNetPeerGroup() {
        try {
            PeerGroupID pgid = (PeerGroupID) ID.create(URI.create("urn:jxta:uuid-69D1DFAB233943D18C13E73AF058455E02"));
            File tmpHome = new File(System.getProperty("java.io.tmpdir"), "test" + Double.toHexString(Math.random()));

            tmpHome.mkdirs();
            
            Properties props = new Properties();

            props.setProperty("NetPeerGroupID", pgid.getUniqueValue().toString());
            props.setProperty("NetPeerGroupName", "config.properties group");
            props.setProperty("NetPeerGroupDesc", "config.properties peer group");
            
            FileOutputStream out = new FileOutputStream(new File(tmpHome, "config.properties"));

            props.store(out, "PeerGroupFactoryTest::testConfigPropertiesNewNetPeerGroup");
            out.close();
            
            URI original = PeerGroupFactory.getStoreHome();

            PeerGroupFactory.setStoreHome(tmpHome.toURI());
            
            PeerGroup npg = PeerGroupFactory.newNetPeerGroup();
                                              
            Thread.sleep(TimeUtils.ASECOND * 10);
            
            assertTrue("Store home location was not as expected", npg.getStoreHome().equals(tmpHome.toURI()));
            assertTrue("Group ID was not as expected.", npg.getPeerGroupID().equals(pgid));
            assertTrue("Group Name was not as expected.", "config.properties group".equals(npg.getPeerGroupName()));
            
            npg.unref();
            npg = null;
           
            waitForPeerGroupShutdown(PeerGroupID.defaultNetPeerGroupID, 60 * TimeUtils.ASECOND);
            waitForPeerGroupShutdown(PeerGroupID.worldPeerGroupID, 60 * TimeUtils.ASECOND);
            
            // reset
            PeerGroupFactory.setStoreHome(original);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }  
}
