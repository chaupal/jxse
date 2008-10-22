/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 */

package net.jxta.impl.content;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentService;
import net.jxta.content.ContentShare;
import net.jxta.content.ContentTransfer;
import net.jxta.content.TransferException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.BinaryDocument;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.loader.RefJxtaLoaderTest;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.platform.NetworkManager.ConfigMode;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.test.util.DelegateClassLoader;
import net.jxta.test.util.TempDir;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * General tests to apply to ContentProviders to verify compliance and
 * functionality.
 */
public abstract class AbstractContentProviderTest {
    private static Logger LOG =
            Logger.getLogger(AbstractContentProviderTest.class.getName());
    
    static TempDir home;
    static NetworkManager netMan;
    static PeerGroup pg;
    static ContentService service;
    static URL testJar;

    private final ContentProviderSPI provider;

    /**
     * Due to the DelegateClassLoader usage, this class is loaded in a
     * totally separate ClassLoader from where it is used.  This helps work
     * around having to use tons of reflection calls but we need to make
     * sure that the info coming into and out of this class are in a
     * ClassLoader-nuetral form (i.e., system classes only).
     */
    public static class ContentSharer implements ContentSharerSPI {
        NetworkManager nm;
        ContentService service;
        String targetProvClass;
        
        public ContentSharer(final File tempDir, final String className)
                throws Exception {
            LOG.info("Constructing ContentSharer for class: " + className);
            targetProvClass = className;
            nm = new NetworkManager(
                    ConfigMode.EDGE, "TestNet", tempDir.toURI());
            nm.setInstanceHome(tempDir.toURI());
            nm.setUseDefaultSeeds(false);
            NetworkConfigurator nc = nm.getConfigurator();
            nc.setTcpStartPort(60000);
            nc.setTcpEndPort(65535);
            nc.setHttpEnabled(false);
            nc.setUseMulticast(false);
            nc.setRendezvousSeeds(Collections.singleton("tcp://127.0.0.1:9701"));
            LOG.info("Created NM: " + nm + " (" + nm.getClass().getClassLoader() + ")");
        }
        
        public void init() {
            try {
                LOG.info("Initializing: " + this);
                PeerGroup netPeerGroup = nm.startNetwork();
                nm.waitForRendezvousConnection(15000);
                LOG.info("Am  RDV? " + netPeerGroup.isRendezvous());
                LOG.info("Got RDV? " + netPeerGroup.getRendezVousService().isConnectedToRendezVous());
                LOG.info("I'm in: " + netPeerGroup);
                LOG.info("I am  : " + netPeerGroup.getPeerID());
                Enumeration<ID> rdvPeers =
                        netPeerGroup.getRendezVousService().getConnectedRendezVous();
                while (rdvPeers.hasMoreElements()) {
                    LOG.info("RDV   : " + rdvPeers.nextElement());
                }
                Thread.sleep(1000);
                service = netPeerGroup.getContentService();
                List<? extends ContentProvider> provs =
                        service.getContentProviders();
                for (ContentProvider prov : provs) {
                    if (prov.getClass().getName().equals(targetProvClass)) {
                        LOG.info("Keeping provider: " + prov);
                    } else {
                        LOG.info("Removing untargeted provider: " + prov);
                        service.removeContentProvider(prov);
                    }
                }
                assertEquals(1, service.getContentProviders().size());
                assertEquals(1, service.getActiveContentProviders().size());
            } catch (Exception exc) {
                // Rethrow as unchecked
                throw(new RuntimeException(
                        "Could not init: " + exc.getMessage(), exc));
            }
        }
        
        public byte[] share(URI id, byte[] data, String mimeType, boolean pub) {
            try {
                MimeMediaType mType = MimeMediaType.valueOf(mimeType);
                BinaryDocument bDoc = new BinaryDocument(data, mType);
                ContentID cID = (ContentID) IDFactory.fromURI(id);
                Content content = new Content(cID, null, bDoc);
                LOG.info("ContentSharer: Sharing content: " + content);
                List<ContentShare> shares = service.shareContent(content);
                assertNotNull(shares);
                assertEquals(1, shares.size());
                ContentShare share = shares.get(0);
                ContentShareAdvertisement adv =
                        share.getContentShareAdvertisement();
                if (pub) {
                    nm.getNetPeerGroup().getDiscoveryService().publish(adv);
                }
                LOG.info("ContentSharer created share adv:\n" + adv);
                Document advDoc = adv.getDocument(MimeMediaType.XMLUTF8);
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                advDoc.sendToStream(byteOut);
                return byteOut.toByteArray();
            } catch (Exception exc) {
                // Rethrow as unchecked
                throw(new RuntimeException(
                        "Could not share: " + exc.getMessage(), exc));
            }
        }

        public void destroy() {
            LOG.info("Destroying: " + this);
            nm.stopNetwork();
            nm = null;
            service = null;
        }
    }
    
    /**
     * Default constructor.
     * 
     * @param spi implementation to test
     */
    public AbstractContentProviderTest(ContentProviderSPI spi) {
        provider = spi;
    }
    
    /**
     * Starts a local JXTA instance in preparation for testing.
     * 
     * @throws java.lang.Exception on error
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("============ Begin setupClass");
        home = new TempDir();
        netMan = new NetworkManager(NetworkManager.ConfigMode.SUPER, "test");
        netMan.setInstanceHome(home.toURI());
        netMan.setUseDefaultSeeds(false);
        NetworkConfigurator nc = netMan.getConfigurator();
        nc.setHttpEnabled(false);
        nc.setUseMulticast(false);
        nc.setTcpPort(9701);
        pg = netMan.startNetwork();
        LOG.info("Am  RDV? " + pg.isRendezvous());
        LOG.info("Got RDV? " + pg.getRendezVousService().isConnectedToRendezVous());
        LOG.info("I'm in: " + pg);
        LOG.info("I am  : " + pg.getPeerID());
        Enumeration<ID> rdvPeers =
                pg.getRendezVousService().getConnectedRendezVous();
        while (rdvPeers.hasMoreElements()) {
            LOG.info("RDV   : " + rdvPeers.nextElement());
        }
        
        testJar = RefJxtaLoaderTest.class.getResource("/TestJar.jar");
        assertNotNull("TestJar could not be located", testJar);

        service = pg.getContentService();
        assertNotNull("ContentService not present in peer group", service);
        
        LOG.info("============ End setupClass");
    }
    
    /**
     * Tears down the local JXTA instance.
     * 
     * @throws java.lang.InterruptedException on error
     */
    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        LOG.info("============ Begin tearDownClass");
        home.delete();
        netMan.stopNetwork();
        LOG.info("============ End tearDownClass");
        System.err.flush();
        System.out.flush();
        Thread.sleep(500);
    }
    
    /**
     * Mark the beginning of a test.
     */
    @Before
    public void setup() {
        LOG.info("============ Begin test");
    }

    /**
     * Mark the end of a test.
     */
    @After
    public void tearDown() {
        LOG.info("============ End test");
        Thread.yield();
        System.out.flush();
        System.err.flush();
    }

    /**
     * This test checks to see that the provider can successfully retrieve
     * content if provided an unpublished ContentShareAdvertisement.
     * 
     * @throws Throwable on test error
     */
    @Test
    public void retrieveByAdvertisement() throws Throwable {
        ContentSharerSPI spi = createContentSharer();
        try {
            spi.init();
            ContentID cID = IDFactory.newContentID(pg.getPeerGroupID(), true);
            byte[] sharedData = new String("This is the test content: "
                    + toString()).getBytes();
            ContentShareAdvertisement shareAdv =
                    wrappedShare(spi, cID, sharedData, MimeMediaType.XMLUTF8, false);
            LOG.finest("Share produced advertisement:\n" + shareAdv);
            ContentTransfer xfer = service.retrieveContent(shareAdv);
            File dest = new File(home, "received");
            LOG.fine("Starting transfer");
            xfer.startTransfer(dest);
            try {
                xfer.waitFor(30000);
                if (!xfer.getTransferState().isFinished()) {
                    fail("Transfer timed out");
                }
                Content received = xfer.getContent();
                LOG.fine("Received Content: " + received);
                assertEqual(contentOf(cID, sharedData, MimeMediaType.XMLUTF8),
                        received);
            } catch (TransferException xferx) {
                LOG.log(Level.SEVERE, "Transfer failed");
                fail("Caught transfer exception: " + xferx);
            }
        } catch (Throwable thr) {
            LOG.log(Level.WARNING, "Caught throwable", thr);
            throw(thr);
        } finally {
            spi.destroy();
        }        
    }

    /**
     * This test checks to see that the provider can successfully retrieve
     * content if provided the ContentID of a published ContentShare.
     * 
     * @throws Throwable on test error
     */
    @Test
    public void retrieveByID() throws Throwable {
        ContentSharerSPI spi = createContentSharer();
        try {
            spi.init();
            ContentID cID = IDFactory.newContentID(pg.getPeerGroupID(), true);
            byte[] sharedData = new String("This is the test content: "
                    + toString()).getBytes();
            ContentShareAdvertisement shareAdv =
                    wrappedShare(spi, cID, sharedData, MimeMediaType.XMLUTF8, true);
            LOG.finest("Share produced advertisement:\n" + shareAdv);
            ContentTransfer xfer = service.retrieveContent(cID);
            File dest = new File(home, "received");
            LOG.fine("Starting transfer");
            xfer.startTransfer(dest);
            try {
                xfer.waitFor(30000);
                if (!xfer.getTransferState().isFinished()) {
                    fail("Transfer timed out");
                }
                Content received = xfer.getContent();
                LOG.fine("Received Content: " + received);
                assertEqual(contentOf(cID, sharedData, MimeMediaType.XMLUTF8),
                        received);
            } catch (TransferException xferx) {
                LOG.log(Level.SEVERE, "Transfer failed");
                fail("Caught transfer exception: " + xferx);
            }
        } catch (Throwable thr) {
            LOG.log(Level.WARNING, "Caught throwable", thr);
            throw(thr);
        } finally {
            spi.destroy();
        }        
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Creates another JXTA instance within this JVM which will serve Content
     * when requested to do so.  This additional instance is loaded within
     * a separate ClassLoader structure and must be interacted with via
     * standard java Classes and the specially handled ContentSharerSPI
     * interface.
     * 
     * @return content sharer instance
     */
    private ContentSharerSPI createContentSharer() {
        try {
            DelegateClassLoader dcl = new DelegateClassLoader(
                    AbstractContentProviderTest.class.getClassLoader());
            dcl.addClassRedefinePattern(Pattern.compile(".*"));
            dcl.addClassNeverRedefinePattern(Pattern.compile("org.junit.*"));
            dcl.addClassNeverRedefinePattern(Pattern.compile(
                    ContentSharerSPI.class.getName()));
            Class fc = dcl.loadClass(ContentSharer.class.getName());
            Constructor constructor = fc.getDeclaredConstructor(
                    File.class, String.class);
            File sharerHome = new File(home, "sharerHome");
            Object obj = constructor.newInstance(
                    sharerHome, provider.getClass().getName());
            assertTrue(obj instanceof ContentSharerSPI);
            return (ContentSharerSPI) obj;
        } catch (Exception exc) {
            // Rethrow as unchecked
            throw(new RuntimeException("Caught exception", exc));
        }
    }
    
    /**
     * Requests a content sharer to share a content built from the specified
     * components, then has it communicate the ContentShareAdvertisement of
     * the newly shared Content in a form which can be understood from the
     * calling ClassLoader context.
     * 
     * @param spi sharer to have share the Content
     * @param cID ContentID of the content to be shared
     * @param data data of the Content to be shared
     * @param mimeType MIME media type of the Content to be shared
     * @return ContentShareAdvertisement of the shared Content
     * @throws java.io.IOException on error
     */
    private ContentShareAdvertisement wrappedShare(
            ContentSharerSPI spi, ContentID cID, byte[] data,
            MimeMediaType mimeType, boolean doPublish)
    throws IOException {
        byte[] advData = spi.share(cID.toURI(), data, mimeType.getMimeMediaType(), false);
        BinaryDocument advDoc = new BinaryDocument(advData, MimeMediaType.XMLUTF8);
        XMLElement elem = (XMLElement) StructuredDocumentFactory.newStructuredDocument(
                advDoc.getMimeType(), advDoc.getStream());
        ContentShareAdvertisement shareAdv = (ContentShareAdvertisement)
                AdvertisementFactory.newAdvertisement(elem);
        return shareAdv;
    }
    
    private Content contentOf(
            ContentID cID, byte[] data, MimeMediaType mimeType) {
        BinaryDocument bDoc = new BinaryDocument(data, mimeType);
        return new Content(cID, null, bDoc);
    }
    
    /**
     * Compares two Content documents for equality.
     */
    private void assertEqual(Content expected, Content actual) throws IOException {
        // Get the trivial cases out of the way
        if (expected == null && actual == null) {
            return;
        } else if (expected == null || actual == null) {
            // This will catch the failure
            assertEqual(expected, actual);
        } else if (expected == actual) {
            return;
        }
        
        assertEquals(expected.getContentID(), actual.getContentID());
        assertEquals(expected.getMetaID(), actual.getMetaID());
        
        // Compare the documents
        Document eDoc = expected.getDocument();
        Document aDoc = actual.getDocument();
        assertEquals(eDoc.getFileExtension(), aDoc.getFileExtension());
        
        // The MIME type parameters are not transferred
        assertEquals(eDoc.getMimeType().getMimeMediaType(),
                aDoc.getMimeType().toString());
            
        // Compare the data
        
        ByteArrayOutputStream eOut = new ByteArrayOutputStream();
        eDoc.sendToStream(eOut);

        ByteArrayOutputStream aOut = new ByteArrayOutputStream();
        aDoc.sendToStream(aOut);

        byte[] eBytes = eOut.toByteArray();
        byte[] aBytes = aOut.toByteArray();
        assertEquals(new String(eBytes), new String(aBytes));
    }
    
}
