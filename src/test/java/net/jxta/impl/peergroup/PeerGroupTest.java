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

package net.jxta.impl.peergroup;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.loader.JxtaLoaderModuleManager;
import net.jxta.impl.protocol.PeerGroupConfigAdv;
import net.jxta.impl.protocol.PeerGroupConfigFlag;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.JxtaApplication;
import net.jxta.peergroup.core.IJxtaLoader;
import net.jxta.peergroup.core.Module;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.test.util.TempDir;

import org.junit.*;

import static org.junit.Assert.*;

@Ignore("Passes when run independantly")
public class PeerGroupTest {

    static final Logger LOG = Logger.getLogger(PeerGroupTest.class.getName());

    static TempDir home;
    static NetworkManager netMan;
    static PeerGroup pg1;
    PeerGroup pg11;
    PeerGroup pg111;
    PeerGroup pg12;
    PeerGroup pg121;
    ModuleImplAdvertisement pojoMIA;
    ModuleImplAdvertisement moduleMIA;

    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("============ Begin setupClass");
        home = new TempDir();
        //netMan = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "test");
        netMan = JxtaApplication.getNetworkManager(NetworkManager.ConfigMode.ADHOC, "test", home.toURI());        
        pg1 = netMan.startNetwork();
        netMan.waitForRendezvousConnection(1000);

        LOG.finest("NPG PGA: " + pg1.getPeerGroupAdvertisement());

        LOG.info("============ End setupClass");
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        LOG.info("============ Begin tearDownClass");
        home.delete();
        netMan.stopNetwork();
        LOG.info("============ End tearDownClass");
    }

    @Before
    public void setup() throws Exception {
        LOG.info("============ Begin setup");
        PeerGroupAdvertisement pga;

        /*
         * We now create a tree of peer groups for testign various
         * scenarios.
         */

        pga = createGroupAdv(pg1.getDiscoveryService(), "pg11");
        pg11 = pg1.newGroup(pga);

        pga = createGroupAdv(pg11.getDiscoveryService(), "pg111");
        pg111 = pg11.newGroup(pga);

        pga = createGroupAdv(pg1.getDiscoveryService(), "pg12");
        pg12 = pg1.newGroup(pga);

        pga = createGroupAdv(pg12.getDiscoveryService(), "pg121",
                PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER);
        pg121 = pg12.newGroup(pga);

        /*
         * Setup some stuff to load. 
         */

        URL testJar = getClass().getResource("/TestJar.jar");
        assertNotNull("TestJar could not be located", testJar);
        ModuleClassID baseClass = IDFactory.newModuleClassID();

        pojoMIA = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());
        pojoMIA.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
        pojoMIA.setCode("TestPOJO");
        pojoMIA.setUri(testJar.toString());
        pojoMIA.setCompat(pg1.getAllPurposePeerGroupImplAdvertisement().getCompat());
        pojoMIA.setDescription("Test non-Module in a jar");

        moduleMIA = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());
        moduleMIA.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
        moduleMIA.setCode("TestModule");
        moduleMIA.setUri(testJar.toString());
        moduleMIA.setCompat(pg1.getAllPurposePeerGroupImplAdvertisement().getCompat());
        moduleMIA.setDescription("Test Module in a jar");

        LOG.info("============ End setup");
    }

    @After
    public void tearDown() throws Exception {
        LOG.info("============ Begin tearDown");
//        pg111.unref();
//        pg11.unref();
//        pg121.unref();
//        pg12.unref();
        Thread.sleep(300);
        LOG.info("============ End tearDown");
    }

    @Test
    public void newGroupFromAdv() {
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleSpecID msid = IDFactory.newModuleSpecID(baseClass);
            ModuleImplAdvertisement mia = pg1.getAllPurposePeerGroupImplAdvertisement();
            mia.setModuleSpecID(msid);

            LOG.info("New MIA:\n" + mia);
            pg1.getDiscoveryService().publish(mia);

            PeerGroupAdvertisement pga = (PeerGroupAdvertisement)
                    AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
            pga.setPeerGroupID(IDFactory.newPeerGroupID());
            pga.setModuleSpecID(mia.getModuleSpecID());
            LOG.info("New PGA:\n" + pga);

            PeerGroup newpg = pg1.newGroup(pga);

            assertTrue("Group id should match", newpg.getPeerGroupID().equals(pga.getPeerGroupID()));
//            newpg.unref();
            newpg = null;
        } catch (Exception caught) {
            LOG.log(Level.SEVERE, "Caught exception\n", caught);
            fail("exception thrown : " + caught.getMessage());
        }
    }

    @Test
    public void newGroupFromParams() {
        try {
            ModuleImplAdvertisement mia = pg1.getAllPurposePeerGroupImplAdvertisement();

            pg1.getDiscoveryService().publish(mia);

            PeerGroupID pgid = IDFactory.newPeerGroupID();

            PeerGroup newpg = pg1.newGroup(pgid, mia, "test", "testdesc");

            assertTrue("Group id should match", newpg.getPeerGroupID().equals(pgid));

//            newpg.unref();
            newpg = null;

            newpg = pg1.newGroup(null, mia, null, null);

            assertTrue("Group id should match", !newpg.getPeerGroupID().equals(pg1.getPeerGroupID()));

//            newpg.unref();
            newpg = null;

        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    @Test
    public void newFromID() {
        try {
            PeerGroup newpg = pg1.newGroup(PeerGroupID.defaultNetPeerGroupID);

            assertTrue("Group id should match", newpg.getPeerGroupID().equals(PeerGroupID.defaultNetPeerGroupID));

//            newpg.unref();
            newpg = null;
        } catch (Exception caught) {
            caught.printStackTrace(); 
            fail("exception thrown : " + caught.getMessage());
        }
    }

    /**
     * Verify that the static loader by default cannot load the class in
     * our test jar.
     */
    @Test
    public void staticJxtaLoader() {
        ClassLoader loader = (ClassLoader) JxtaLoaderModuleManager.getRoot( PeerGroupTest.class ).getLoader();
        try {
            Class<?> clazz = loader.loadClass("TestClass");
            fail("Static loader could see the test class: " + clazz);
        } catch (ClassNotFoundException cnfx) {
            // Good.  Fall through.
        }
    }

    /**
     * Make sure the PeerGroups dont use, but inherit the static loader.
     * This will make it easier to remove the static loader altogether at
     * some point in the future.
     */
/*
    @Test
    public void staticInheritedOnly() {
        JxtaLoader staticLoader = (JxtaLoader) GenericPeerGroup.getJxtaLoader();
        PeerGroup group = pg111;
        do {
            PeerGroup parentGroup = group.getParentGroup();
            //TODO CP: Change this test
            JxtaLoader groupLoader = null;//group.getLoader();

            // Make sure the groupLoader is not the staticLoader
            if (groupLoader == staticLoader) {
                fail("groupLoader should never be the staticLoader");
            }

            if (parentGroup == null) {
                // Make sure our class loader's parent loader is staticLoader
                ClassLoader rootLoader = groupLoader.getParent();
                if (rootLoader != staticLoader) {
                    fail("root loader was not staticLoader");
                }
            }
            group = parentGroup;
        } while (group != null);
    }
*/
    
    /**
     * Make sure each group has it's own, unique loader instance
     */
    @Test
    public void uniqueGroupLoaders() {
        JxtaLoaderModuleManager<? extends Module> manager = JxtaLoaderModuleManager.getRoot();
    	IJxtaLoader lastLoader = manager.getLoader();
        PeerGroup group = pg111;
        do {
        	JxtaLoaderModuleManager<? extends Module> pmm = (JxtaLoaderModuleManager<? extends Module>) manager.getModuleManager(group);
        	if (lastLoader == pmm.getLoader()) {
                fail("Group loader was not unique");
            }
            lastLoader = pmm.getLoader();
            group = group.getParentGroup();
        } while (group != null);
    }

    /**
     * Test class loading inheritance.
     */
    @Test
    public void classLoadingInheritance() {
        assertFlagState(pg11, PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER, false);
        assertFlagState(pg111, PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER, false);

        // Define it
        //TODO CP: Change this test
        //pg11.getLoader().defineClass(moduleMIA);
        /*
        Class<? extends Module> mod11 = null;
        try {
            mod11 = pg11.getLoader().loadClass(moduleMIA.getModuleSpecID());
        } catch (ClassNotFoundException cnfx) {
            fail("Could not load moduleMIA into pg11");
        }

        // Check that child inherits the same definition
        //Class<? extends Module> mod111 = null;
        try {
        	mod111 = pg111.getLoader().loadClass(moduleMIA.getModuleSpecID());
        } catch (ClassNotFoundException cnfx) {
            fail("Could not load moduleMIA from child group pg111");
        }
        assertSame(mod11, mod111);

        // Check that the parent is unable to load it
        Class<? extends Module> mod1 = null;
        try {
         	mod1 = pg1.getLoader().loadClass(moduleMIA.getModuleSpecID());
            fail("Parent group pg1 was able to load moduleMIA");
        } catch (ClassNotFoundException cnfx) {
            // Good
        }

        // Check that the peer is unable to load it
        Class<? extends Module> mod12 = null;
        try {
        	mod1 = pg12.getLoader().loadClass(moduleMIA.getModuleSpecID());
            fail("Parent group pg12 was able to load moduleMIA");
        } catch (ClassNotFoundException cnfx) {
            // Good
        }
        */
    }

    /**
     * Test parent classloading disable flag.
     */
    @Test
    public void testParentClassLoadDisabled() {
        assertFlagState(pg12, PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER, false);
        assertFlagState(pg121, PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER, true);

        // Define it
        LOG.info("Defining Module in pg12");
        //TODO CP: Change this test
        /*
        pg12.getLoader().defineClass(moduleMIA);

        LOG.info("Checking for Module in pg12");
        Class<? extends Module> mod12 = null;
        try {
            mod12 = pg12.getLoader().loadClass(moduleMIA.getModuleSpecID());
        } catch (ClassNotFoundException cnfx) {
            fail("Could not load moduleMIA into pg12");
        }

        // Check that child does not load it from it's parent
        LOG.info("Checking for Module in pg121");
        Class<? extends Module> mod121 = null;
        try {
            mod121 = pg121.getLoader().loadClass(moduleMIA.getModuleSpecID());
            fail("pg121 was able to load the module from it's parent");
        } catch (ClassNotFoundException cnfx) {
            // Good
        }
        */
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Creates a peer group advertisement set for testing.
     * 
     * @param disco discovery service to publish into
     * @param name peer group name
     * @return peer group advertisement
     * @throws java.lang.Exception on error
     */
    private PeerGroupAdvertisement createGroupAdv(
            DiscoveryService disco, String name, PeerGroupConfigFlag ... flags)
            throws Exception {
        ModuleClassID mcid = IDFactory.newModuleClassID();
        ModuleImplAdvertisement mia =
                pg1.getAllPurposePeerGroupImplAdvertisement();

        ModuleSpecID msid = IDFactory.newModuleSpecID(mcid);
        mia.setDescription(name + " impl");
        mia.setModuleSpecID(msid);
        disco.publish(mia);
        LOG.finest(name + " MIA:\n" + mia);

        PeerGroupID pgid = IDFactory.newPeerGroupID();
        PeerGroupConfigAdv pgca = (PeerGroupConfigAdv)
                AdvertisementFactory.newAdvertisement(
                PeerGroupConfigAdv.getAdvertisementType());
        pgca.setPeerGroupID(pgid);
        for (PeerGroupConfigFlag flag : flags) {
            pgca.setFlag(flag);
        }

        PeerGroupAdvertisement pga = (PeerGroupAdvertisement)
                AdvertisementFactory.newAdvertisement(
                PeerGroupAdvertisement.getAdvertisementType());
        pga.setName(name);
        pga.setModuleSpecID(msid);
        pga.setPeerGroupID(pgid);
        pga.putServiceParam(IModuleDefinitions.peerGroupClassID,
                (Element<?>) pgca.getDocument(MimeMediaType.XMLUTF8));
        disco.publish(pga);
        LOG.finest(name + " PGA:\n" + pga);
        return pga;
    }

    /**
     * Asserts that the peer group provided is configured as expected with
     * respect to a specific flag's value.
     * 
     * @param pg peer group to assert
     * @param flag flag to check for
     * @param enabled expected value
     */
    private void assertFlagState(
            PeerGroup pg, PeerGroupConfigFlag flag, boolean enabled) {
        LOG.fine("Testing PeerGroup: " + pg.getClass() + " " + pg);
        PeerGroupAdvertisement pga = pg.getPeerGroupAdvertisement();
        PeerGroupConfigAdv pgca = (PeerGroupConfigAdv)
                AdvertisementFactory.newAdvertisement((XMLElement<?>)
                pga.getServiceParam(IModuleDefinitions.peerGroupClassID));
        if (enabled) {
            assertTrue(pgca.isFlagSet(flag));
        } else {
            assertFalse(pgca.isFlagSet(flag));
        }
    }

}
