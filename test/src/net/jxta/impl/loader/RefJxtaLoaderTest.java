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

package net.jxta.impl.loader;

import java.io.File;
import java.net.URI;
import net.jxta.content.ContentProviderEvent;
import net.jxta.impl.peergroup.*;
import net.jxta.impl.util.threads.TaskManager;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.JUnit4TestAdapter;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentProviderListener;
import net.jxta.content.ContentService;
import net.jxta.content.ContentShare;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.test.util.TempDir;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class RefJxtaLoaderTest {
    
    static final Logger LOG =
            Logger.getLogger(RefJxtaLoaderTest.class.getName());
    
    /**
     * Default compatibility equater instance.
     */
    private static final CompatibilityEquater COMP_EQ =
            new CompatibilityEquater() {
                public boolean compatible(Element test) {
                    return CompatibilityUtils.isCompatible(test);
                }
            };

    
    static TempDir home;
    static NetworkManager netMan;
    static PeerGroup pg;
    static ContentService service;
    static Content parentContent;
    static Content bothContent;
    static URL testJar;
    
    ContentService testService;
    RefJxtaLoader loader;
    PeerGroup testGroup;
    
    public static junit.framework.Test suite() { 
        return new JUnit4TestAdapter(RefJxtaLoaderTest.class); 
    }
    
    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("============ Begin setupClass");
        home = new TempDir();
        netMan = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "test");
        netMan.setInstanceHome(home.toURI());
        pg = netMan.startNetwork();
        netMan.waitForRendezvousConnection(1000);
        
        testJar = RefJxtaLoaderTest.class.getResource("/TestJar.jar");
        assertNotNull("TestJar could not be located", testJar);

        service = pg.getContentService();
        assertNotNull("ContentService not present in peer group");
        
        Document jarDoc = new URIDocument(testJar.toURI());
        
        ContentID contentID = IDFactory.newContentID(pg.getPeerGroupID(), true);
        bothContent = new Content(contentID, null, jarDoc);
        List<ContentShare> shares = service.shareContent(bothContent);
        assertNotNull(shares);
        assertTrue(shares.size() > 0);

        contentID = IDFactory.newContentID(pg.getPeerGroupID(), true);
        parentContent = new Content(contentID, null, jarDoc);
        shares = service.shareContent(parentContent);
        assertNotNull(shares);
        assertTrue(shares.size() > 0);

        LOG.finest("NPG PGA: " + pg.getPeerGroupAdvertisement());
        
        LOG.info("============ End setupClass");
    }
    
    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        LOG.info("============ Begin tearDownClass");
        service.unshareContent(bothContent.getContentID());
        home.delete();
        netMan.stopNetwork();
        LOG.info("============ End tearDownClass");
        System.err.flush();
        System.out.flush();
        Thread.sleep(500);
    }
    
    @Before
    public void setup() throws Exception {
        LOG.info("============ Begin setup");
        PeerGroupAdvertisement pga;
        
        /*
         * Setup a test-specific peer group.
         */
        PeerGroupID pgid = IDFactory.newPeerGroupID();
        ModuleImplAdvertisement pgMIA =
                pg.getAllPurposePeerGroupImplAdvertisement();
        testGroup = pg.newGroup(pgid, pgMIA, "Test group", "Test group");
        testService = testGroup.getContentService();
        if (LOG.isLoggable(Level.FINE)) {
            for (ContentProvider provider : testService.getContentProviders()) {
                provider.addContentProviderListener(
                        new ContentProviderListener() {

                    public void contentShared(ContentProviderEvent event) {
                        LOG.fine(event.toString());
                    }

                    public void contentUnshared(ContentProviderEvent event) {
                        LOG.fine(event.toString());
                    }

                    public boolean contentSharesFound(
                            ContentProviderEvent event) {
                        LOG.fine(event.toString());
                        return true;
                    }
                    
                });
            }
        }
        
        assertNotNull(testService);
        List<ContentShare> shares = testService.shareContent(bothContent);
        assertNotNull(shares);
        assertTrue(shares.size() > 0);
        
        /*
         * Create a test loader referencing our test-specific group.
         */
        loader = new RefJxtaLoader(new URL[0], null, COMP_EQ, testGroup);
        
        LOG.info("============ End setup");
    }
    
    @After
    public void tearDown() throws Exception {
        LOG.info("============ Begin tearDown");
        testService.unshareContent(bothContent.getContentID());
        testGroup.unref();
        Thread.sleep(300);
        LOG.info("============ End tearDown");
    }
    
    /**
     * Verify that we cannot load the POJO class as a Module when the package
     * URI references a jar URL.
     */
    @Test
    public void loadPOJO() {
        LOG.info("loadPOJO");
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestPOJO");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Non-Module in a Jar");
        
            Class clazz = loader.defineClass(mia);
            fail("Was able to load a POJO as a Module");
        } catch (ClassFormatError err) {
            // Good.  Fall through.
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
    }
    
    /**
     * Verify that we can load Modules when the package URI references a
     * Jar URL.
     */
    @Test
    public void loadModule() {
        LOG.info("loadModule");
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
        
            Class clazz = loader.defineClass(mia);
            assertTrue(Module.class.isAssignableFrom(clazz));
        } catch (Error err) {
            fail("Was not able to load a Module");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
    }

    /**
     * Verify that once a package is referenced that the other classes in the
     * package can be resolved without additional packages being referenced.
     */
    @Test
    public void packageLoadIsSticky() {
        LOG.info("packageLoadIsSticky");
        
        // First, make sure we cant load TestPOJO
        try {
            Class clazz = loader.loadClass("TestPOJO");
            fail("Shouldn't be able to load TestPOJO at this point");
        } catch (Exception ex) {
            // Good.
        }
        
        // Now load in the Module requiring the additional package
        Class moduleClazz = null;
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
            moduleClazz = loader.defineClass(mia);
        } catch (Error err) {
            fail("Was not able to load a Module");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
        
        // Now we should be able to load TestPOJO
        try {
            Class pojo = loader.loadClass("TestPOJO");
            assertSame(moduleClazz.getClassLoader(), pojo.getClassLoader());
        } catch (Exception ex) {
            fail("Should have been able to load TestPOJO at this point");
        }
    }

    /**
     * Verify that we can load the Module class via MIA referencing a
     * package URI that is in the form of a ContentID.
     */
    @Test
    public void loadModuleContent() {
        LOG.info("loadModuleContent");
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(bothContent.getContentID().toString());
            mia.setCompat(
                    pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");

            Class clazz = loader.defineClass(mia);
            assertTrue(Module.class.isAssignableFrom(clazz));

            // Check that it is stored in the correct location
            URI storeHomeURI = testGroup.getStoreHome();
            File storeHome = new File(storeHomeURI);
            assertTrue(storeHome.exists());

            File grpHome = new File(storeHome,
                    testGroup.getPeerGroupID().getUniqueValue().toString());
            assertTrue(grpHome.isDirectory());

            ModuleSpecID groupMSID =
                    testGroup.getPeerGroupAdvertisement().getModuleSpecID();
            File modHome = new File(grpHome,
                    groupMSID.getUniqueValue().toString());
            assertTrue(modHome.isDirectory());

            File svcHome =
                    new File(modHome, RefJxtaLoader.class.getSimpleName());
            assertTrue(svcHome.isDirectory());

            File contentFile = new File(svcHome,
                    bothContent.getContentID().getUniqueValue().toString());
            assertTrue(contentFile.exists());
        } catch (ClassFormatError err) {
            LOG.log(Level.SEVERE, "Caught error", err);
            fail("Module load failed");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
    }
    
    /**
     * Test the loading of a module which references a package URI that is
     * in the form of a ContentID and cannot be retreived.
     */
    @Test(timeout=120000)
    public void loadModuleContentNotFound() throws Exception {
        LOG.info("loadModuleNotFound");
        ModuleClassID baseClass = IDFactory.newModuleClassID();
        ContentID contentID = IDFactory.newContentID(
                testGroup.getPeerGroupID(), true);
        ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());
        mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
        mia.setCode("Non-existant Module");
        mia.setUri(contentID.toString());
        mia.setCompat(pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
        mia.setDescription("Module which can't be found");
        
        try {
            loader.defineClass(mia);
            fail("Was able to load module which did not exist");
        } catch (ClassFormatError err) {
            // Good.  Fall through.
        }
    }
    
    /**
     * Test that classes loaded in sibling groups when the class definition
     * is not provided by the parent group will resolve to difference class
     * instances, indicating different class loaders were used to load
     * them.
     */
    @Test
    public void siblingGroupsDifferentClasses() throws Exception {
        LOG.info("siblingGroupsDifferentClasses");
        
        // Create the sibling peer groups
        DiscoveryService disco = pg.getDiscoveryService();
        PeerGroupAdvertisement sib1adv = createGroupAdv(disco, "sibling1");
        PeerGroup sibling1 = pg.newGroup(sib1adv);
        PeerGroupAdvertisement sib2adv = createGroupAdv(disco, "sibling2");
        PeerGroup sibling2 = pg.newGroup(sib2adv);
        
        // Load the Module into sibling1
        Class<?> clazz1 = null;
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    sibling1.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
            JxtaLoader groupLoader = sibling1.getLoader();
            clazz1 = groupLoader.defineClass(mia);
        } catch (Error err) {
            fail("Was not able to load a Module into sibling1");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
        
        // Load the Module into sibling2
        Class<?> clazz2 = null;
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    sibling2.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
            JxtaLoader groupLoader = sibling2.getLoader();
            clazz2 = groupLoader.defineClass(mia);
        } catch (Error err) {
            fail("Was not able to load a Module into sibling2");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
        
        // Ensure they are not the same class
        assertFalse(clazz1.isAssignableFrom(clazz2));
        assertFalse(clazz2.isAssignableFrom(clazz1));
    }
    
    /**
     * Test that classes loaded in a parent group will be used in child
     * groups such that the Classes.
     */
    @Test
    public void parentGroupSameClasses() throws Exception {
        LOG.info("parentGroupSameClasses");
        
        // Create the hierarchy
        DiscoveryService disco = pg.getDiscoveryService();
        PeerGroupAdvertisement childAdv = createGroupAdv(disco, "child");
        PeerGroup child = pg.newGroup(childAdv);
        
        // Load the Module into parent
        Class<?> clazz1 = null;
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    pg.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
            JxtaLoader groupLoader = pg.getLoader();
            clazz1 = groupLoader.defineClass(mia);
        } catch (Error err) {
            fail("Was not able to load a Module into parent");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
        
        // Load the Module into child
        Class<?> clazz2 = null;
        try {
            ModuleClassID baseClass = IDFactory.newModuleClassID();
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                    ModuleImplAdvertisement.getAdvertisementType());
            mia.setModuleSpecID(IDFactory.newModuleSpecID(baseClass));
            mia.setCode("TestModule");
            mia.setUri(testJar.toString());
            mia.setCompat(
                    child.getAllPurposePeerGroupImplAdvertisement().getCompat());
            mia.setDescription("Module in a Jar");
            JxtaLoader groupLoader = child.getLoader();
            clazz2 = groupLoader.defineClass(mia);
        } catch (Error err) {
            fail("Was not able to load a Module into child");
        } catch (Exception exc) {
            LOG.log(Level.SEVERE, "Caught exception", exc);
            fail("Caught exception");
        }
        
        // Ensure they are the same class
        assertTrue(clazz1.isAssignableFrom(clazz2));
        assertTrue(clazz2.isAssignableFrom(clazz1));
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
            DiscoveryService disco, String name)
            throws Exception {
        ModuleClassID mcid = IDFactory.newModuleClassID();
        ModuleImplAdvertisement mia =
                pg.getAllPurposePeerGroupImplAdvertisement();
        
        ModuleSpecID msid = IDFactory.newModuleSpecID(mcid);
        mia.setDescription(name + " impl");
        mia.setModuleSpecID(msid);
        disco.publish(mia);
        LOG.finest(name + " MIA:\n" + mia);
        
        PeerGroupID pgid = IDFactory.newPeerGroupID();
        PeerGroupAdvertisement pga = (PeerGroupAdvertisement)
                AdvertisementFactory.newAdvertisement(
                PeerGroupAdvertisement.getAdvertisementType());
        pga.setName(name);
        pga.setModuleSpecID(msid);
        pga.setPeerGroupID(pgid);
        disco.publish(pga);
        LOG.finest(name + " PGA:\n" + pga);
        return pga;
    }
    
}
