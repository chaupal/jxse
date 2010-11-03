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

import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;

import junit.framework.*;
import org.junit.Ignore;

@Ignore("JXTA Configurator Required")
public class LightWeightPeerGroupTest extends TestCase {

    static int inits = 0;
    static PeerGroup npg;
    static LightWeightPeerGroup pg;

    public LightWeightPeerGroupTest(java.lang.String testName) {
        super(testName);
    }

    @Override
    public void setUp() throws Exception {

        System.setProperty("net.jxta.tls.password", "password");
        System.setProperty("net.jxta.tls.principal", "password");

        synchronized (LightWeightPeerGroup.class) {

            inits++;
            if (npg == null) {
                NetPeerGroupFactory npgf = new NetPeerGroupFactory();
                npg = npgf.getInterface();

                // Create a LightWeightPeerGroup
                pg = new LightWeightPeerGroup(createPeerGroupAdv());
                pg.init(npg, null, null);
            }
        }
    }

    @Override
    protected void tearDown() {
        synchronized (LightWeightPeerGroupTest.class) {
            inits--;

            if (inits == 0) {
                pg.stopApp();
                pg.unref();
                pg = null;

                npg.stopApp();
                npg.unref();
                npg = null;
            }
        }
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        // finalize();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(LightWeightPeerGroupTest.class);

        return suite;
    }

    private PeerGroupAdvertisement createPeerGroupAdv() {

        PeerGroupAdvertisement adv = (PeerGroupAdvertisement)
                AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());

        PeerGroupID id = IDFactory.newPeerGroupID(npg.getPeerGroupID());

        adv.setPeerGroupID(id);
        adv.setName("LigthWeightPeerGroupTest");
        adv.setDescription("Automatically generated by LightWeightPeerGroupTest");

        return adv;
    }

    public void testCreated() {

        PeerGroup group = pg.getParentGroup();

        if (group != npg) {
            // npg must be the parent of pg
            fail("LightWeightPeerGroup's parent PeerGroup is incorrect");
            return;
        }
    }

    public void test_PeerAdvertisement() {

        PeerAdvertisement fromnpg = null;
        PeerAdvertisement frompg = null;

        fromnpg = npg.getPeerAdvertisement();
        frompg = pg.getPeerAdvertisement();

        if (!fromnpg.getPeerID().equals(frompg.getPeerID())) {
            // Both groups must have the same PeerID
            fail("LightPeerGroup does not have same PeerID as parent");
            return;
        }
    }
}
