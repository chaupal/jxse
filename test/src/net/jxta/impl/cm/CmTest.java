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

package net.jxta.impl.cm;


import net.jxta.peergroup.PeerGroupID;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.peer.PeerID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.textui.TestRunner;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.Element;
import net.jxta.document.AdvertisementFactory;


/**
 *  A CmTest unit test
 */
public class CmTest extends TestCase {

    private static final int ITERATIONS = 1000;

    private static final String[] dirname = { "Peers", "Groups", "Adv", "Raw"};
    private static final PeerGroupID pgID = IDFactory.newPeerGroupID();

    private static Cm cm = null;
    private static boolean failed = false;

    private static Random random = new Random();

    private List queue = Collections.synchronizedList(new ArrayList());

    /**
     * Constructor for the CmTest object
     *
     * @param  testName  test name
     */
    public CmTest(String testName) {
        super(testName);
        synchronized (CmTest.class) {
            if (null == cm) {
                cm = new Cm("CmTest", true);
            }
        }
    }

    /**
     *  A unit test suite for JUnit
     *
     * @return    The test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(CmTest.class);

        return suite;
    }

    /**
     *  {@inheritDoc}
     */
    public static void fail(String message) {
        failed = true;
        junit.framework.TestCase.fail(message);
    }

    /**
     *  The main program to test Cm
     *
     */
    public static void main(String[] argv) throws Exception {
        try {
            TestRunner.run(suite());
        } finally {
            synchronized (CmTest.class) {
                if (null != cm) {
                    cm.stop();
                    cm = null;
                }
            }
        }
        System.err.flush();
        System.out.flush();
    }

    /**
     * Create expired adv, and GarbageCollect
     */
    public void testGarbageCollect() {
        deletePeer();
        createPeer(true);
        createPipe(true);
        cm.garbageCollect();
    }

    /**
     * Run all the Cm tests sequentially. There can only be one single Cm test because
     * otherwise tearDown (which is called after every test case) will stop the Cm.
     */
    public void testEverything() {
        deletePeer();
        createPeer(false);
        createPipe(false);
        searchPeer();
        multithreadPeer();
    }

    public void testRaw() {
        createRaw();
        checkRaw();
    }

    private void createPeer(boolean expired) {
        ID advID = null;
        String advName = null;

        long t0 = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            PeerAdvertisement adv = generatePeerAdv(i);

            advID = adv.getID();
            advName = advID.getUniqueValue().toString();

            try {
                if (!expired) {
                    cm.save(dirname[0], advName, adv);
                } else {
                    cm.save(dirname[0], advName, adv, 1, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to create Peer Adv: " + e.getMessage());
            }
        }
        System.out.println(
                "Completed Creation of " + ITERATIONS + " PeerAdvertisements in: " + (System.currentTimeMillis() - t0) / 1000
                + " seconds");
    }

    private void createRaw() {

        long t0 = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            byte[] testdata = new byte[ 1 << (i % 16) ];

            Arrays.fill(testdata, (byte) (i % 16));

            try {
                cm.save(dirname[3], Integer.toString(i), testdata, Long.MAX_VALUE, Long.MAX_VALUE);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to raw data: " + e.getMessage());
            }
        }
        System.out.println(
                "Completed Creation of " + ITERATIONS + " Raw data in: " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
    }

    private void checkRaw() {

        long t0 = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            byte[] testdata = new byte[ 1 << (i % 16) ];

            Arrays.fill(testdata, (byte) (i % 16));

            try {
                byte[] check = cm.restoreBytes(dirname[3], Integer.toString(i));

                assertTrue("values should have been equal at" + i, Arrays.equals(testdata, check));
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to raw data: " + e.getMessage());
            }
        }
        System.out.println(
                "Completed checking of " + ITERATIONS + " Raw data in: " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
    }

    private void createPipe(boolean expired) {
        ID advID = null;
        String advName = null;
        StructuredTextDocument doc = null;

        long t0 = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            PipeAdvertisement adv = generatePipeAdv(i);

            advID = adv.getID();
            if (advID == null || advID.equals(ID.nullID)) {
                advName = Cm.createTmpName(doc);
            } else {
                advName = advID.getUniqueValue().toString();
            }
            try {
                if (!expired) {
                    cm.save(dirname[2], advName, adv);
                } else {
                    cm.save(dirname[2], advName, adv, 1, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to create Pipe Adv: " + e.getMessage());
            }
        }

        System.out.println(
                "Completed Creation of " + ITERATIONS + " PipeAdvertisements in: " + (System.currentTimeMillis() - t0) / 1000
                + " seconds");
    }

    private void searchPeer() {
        long t0 = System.currentTimeMillis();
        List<net.jxta.protocol.SrdiMessage.Entry> entries = cm.getEntries(dirname[0], false);

        assertTrue("empty keys", entries.size() != 0);
        System.out.println(
                "getEntries retrieved " + entries.size() + " peers in: " + (System.currentTimeMillis() - t0) / 1000 + " seconds");

        for (int i = 0; i < (ITERATIONS / 10); i++) {
            findPeerAdv(random.nextInt(ITERATIONS));
            findPeerAdvEndswith(random.nextInt(ITERATIONS));
            findPeerAdvStartswith(random.nextInt(ITERATIONS));
            findPeerAdvContains(random.nextInt(ITERATIONS));
        }

        t0 = System.currentTimeMillis();
        List searchResults = cm.search(dirname[0], null, null, 10000, null);

        System.out.println("non-existent test should find 0, found: " + searchResults.size());
        System.out.println("retrieved " + searchResults.size() + " records in: " + (System.currentTimeMillis() - t0) + " ms");

        int threshold = 10;
        List expirations = new Vector();
        List results = cm.getRecords(dirname[0], threshold, null, expirations);

        assertTrue("cm.getRecords failed", threshold == results.size());
        System.out.println("Testing Query for non-existent records");
        results = cm.getRecords(dirname[1], threshold, null, expirations);
        assertTrue("cm.getRecords(dirname[1]) should not return results", results.size() == 0);
        System.out.println("End Testing Query for non-existent records");
    }

    private void findPeerAdv(int i) {
        long t0 = System.currentTimeMillis();

        try {
            List searchResults = cm.search(dirname[0], "Name", "CmTestPeer" + i, 1, null);

            assertNotNull("Null search result", searchResults);
            Enumeration result = Collections.enumeration(searchResults);

            assertNotNull("Null search enumerator", result);
            assertTrue("empty Search Result for query attr=Name value=CmTestPeer" + i, result.hasMoreElements());
            while (result.hasMoreElements()) {
                ByteArrayInputStream dataStream = (ByteArrayInputStream) result.nextElement();
                StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, dataStream);
                Enumeration en = doc.getChildren("Name");

                while (en.hasMoreElements()) {
                    String val = (String) ((Element) en.nextElement()).getValue();

                    assertTrue("Name mismatch ", val.equals("CmTestPeer" + i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("findPeerAdv failed: " + e.getMessage());
        }
        System.out.println("findPeerAdv retrieved CmTestPeer" + i + " in: " + (System.currentTimeMillis() - t0) + " ms");
    }

    private void findPeerAdvEndswith(int i) {

        /* to make things more interesting, we remove the first digit from the
         * id if it is longer than 2 digits.
         */
        String queryString = Integer.toString(i);

        if (queryString.length() > 2) {
            queryString = queryString.substring(1, queryString.length());
        }

        long t0 = System.currentTimeMillis();
        List searchResults = null;

        try {
            searchResults = cm.search(dirname[0], "Name", "*" + queryString, 10, null);
            assertNotNull("Null search result", searchResults);
            Enumeration result = Collections.enumeration(searchResults);

            assertNotNull("Null search enumerator", result);
            assertTrue("Enumerator empty", result.hasMoreElements());
            while (result.hasMoreElements()) {
                ByteArrayInputStream dataStream = (ByteArrayInputStream) result.nextElement();
                StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, dataStream);
                Enumeration en = doc.getChildren("Name");

                while (en.hasMoreElements()) {
                    String val = (String) ((Element) en.nextElement()).getValue();

                    System.out.println("EndsWith: Queried for *" + queryString + ", found: " + val);
                    assertTrue("result returned " + val + " does not end with " + queryString, val.endsWith(queryString));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("findPeerEndsWith failed: " + e.getMessage());
        }
        System.out.println(
                "EndsWith: retrieved " + searchResults.size() + " entries in: " + (System.currentTimeMillis() - t0) + " ms");
    }

    private void findPeerAdvStartswith(int i) {

        /* to make things more interesting, we remove the last digit from the
         * queryString if it is longer than 2 digits.
         */
        String queryString = Integer.toString(i);

        if (queryString.length() > 2) {
            queryString = queryString.substring(0, queryString.length() - 1);
        }

        long t0 = System.currentTimeMillis();
        List searchResults = null;

        try {
            searchResults = cm.search(dirname[0], "Name", "CmTestPeer" + queryString + "*", 10, null);
            assertNotNull("Null search result", searchResults);
            Enumeration result = Collections.enumeration(searchResults);

            assertNotNull("Null search enumerator", result);
            assertTrue("Enumerator empty", result.hasMoreElements());
            while (result.hasMoreElements()) {
                ByteArrayInputStream dataStream = (ByteArrayInputStream) result.nextElement();
                StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, dataStream);
                Enumeration en = doc.getChildren("Name");

                while (en.hasMoreElements()) {
                    String val = (String) ((Element) en.nextElement()).getValue();

                    System.out.println("StartsWith: Queried for CmTestPeer" + queryString + "*, found: " + val);
                    assertTrue("result returned " + val + " does not start with CmTestPeer" + queryString
                            ,
                            val.startsWith("CmTestPeer" + queryString));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("findPeerAdvStartsWith failed: " + e.getMessage());
        }
        System.out.println(
                "StartsWith: retrieved " + searchResults.size() + " entries in: " + (System.currentTimeMillis() - t0) + " ms");
    }

    private void findPeerAdvContains(int i) {

        /* to make things more interesting, we remove the first digit from the
         * queryString if it is longer than 2 digits.
         */
        String queryString = Integer.toString(i);

        if (queryString.length() > 2) {
            queryString = queryString.substring(1, queryString.length());
        }

        long t0 = System.currentTimeMillis();
        List searchResults = null;

        try {
            searchResults = cm.search(dirname[0], "Name", "*" + queryString + "*", 10, null);
            assertNotNull("Null search result", searchResults);
            Enumeration result = Collections.enumeration(searchResults);

            assertNotNull("Null search enumerator", result);
            assertTrue("Enumerator empty", result.hasMoreElements());
            while (result.hasMoreElements()) {
                ByteArrayInputStream dataStream = (ByteArrayInputStream) result.nextElement();
                StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, dataStream);
                Enumeration en = doc.getChildren("Name");

                while (en.hasMoreElements()) {
                    String val = (String) ((Element) en.nextElement()).getValue();

                    System.out.println("Contains: Queried for *" + queryString + "*, found: " + val);
                    assertTrue("result returned " + val + " does not contain " + queryString, val.indexOf(queryString) != -1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("findPeerAdvContains failed: " + e.getMessage());
        }
        System.out.println(
                "Contains: retrieved " + searchResults.size() + " entries in: " + (System.currentTimeMillis() - t0) + " ms");
    }

    private void deletePeer() {
        ArrayList advNameList = new ArrayList(ITERATIONS);
        long t0 = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            PeerAdvertisement adv = generatePeerAdv(i);
            String advName = adv.getID().getUniqueValue().toString();

            try {
                cm.save(dirname[0], advName, adv);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to create Peer Adv: " + e.getMessage());
            }
            advNameList.add(advName);
        }

        // randomize the list to make deletion a little more unpredictable
        Collections.shuffle(advNameList);

        for (int i = 0; i < ITERATIONS; i++) {
            try {
                cm.remove(dirname[0], (String) advNameList.get(i));
            } catch (Exception e) {
                e.printStackTrace();
                fail("Failed to delete Peer Adv: " + e.getMessage());
            }
        }

        List searchResults = null;

        try {
            searchResults = cm.search(dirname[0], "Name", "*", ITERATIONS, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to search Peer Adv: " + e.getMessage());
        }

        // always start unit test with an empty cm (rm -r .jxta)
        assertTrue("remove failed for " + searchResults.size(), searchResults.size() == 0);

        System.out.println(
                "Completed Creation+Deletion of " + ITERATIONS + " PeerAdvertisements in: "
                + (System.currentTimeMillis() - t0) / 1000 + " seconds");
    }

    private PeerAdvertisement generatePeerAdv(int number) {
        try {
            PeerAdvertisement peerAdv = (PeerAdvertisement)
                    AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());

            peerAdv.setPeerGroupID(pgID);
            peerAdv.setPeerID(IDFactory.newPeerID(pgID));
            peerAdv.setName("CmTestPeer" + number);
            return peerAdv;
        } catch (Exception e) {
            e.printStackTrace();
            fail("generatePeerAdv failed: " + e.getMessage());
        }
        return null;
    }

    private PipeAdvertisement generatePipeAdv(int number) {
        try {
            PipeAdvertisement adv = (PipeAdvertisement)
                    AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

            adv.setPipeID(IDFactory.newPipeID(pgID));
            adv.setName("CmTestPipe" + number);
            adv.setType(PipeService.UnicastType);
            return adv;
        } catch (Exception e) {
            e.printStackTrace();
            fail("generatePipeAdv failed: " + e.getMessage());
        }
        return null;
    }

    private void multithreadPeer() {

        System.out.println("mt starting...");

        final int THREADS = 2;
        Thread adders[] = new Thread[THREADS];
        Thread removers[] = new Thread[THREADS];
        Thread searchers[] = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            adders[i] = new Thread(new PeerAdder(i));
            removers[i] = new Thread(new PeerRemover(i));
            searchers[i] = new Thread(new PeerSearcher(i));
        }

        for (int i = 0; i < THREADS; i++) {
            adders[i].start();
            removers[i].start();
            searchers[i].start();
        }

        // wait for all adders and removers to get done
        for (int i = 0; i < THREADS; i++) {
            try {
                adders[i].join();
                removers[i].join();
            } catch (InterruptedException ignore) {}
        }

        if (failed) {
            fail("mt test failed");
        }

        System.out.println("mt all done");
    }

    private class PeerRemover implements Runnable {
        private int id = 0;
        public PeerRemover(int id) {
            this.id = id;
        }

        public void run() {
            for (int i = 0; i < ITERATIONS && !failed; i++) {
                while (queue.size() < 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {}
                }

                PeerAdvertisement adv = (PeerAdvertisement) queue.remove(0);
                String advName = adv.getID().getUniqueValue().toString();

                try {
                    long expiry = cm.getExpirationtime(dirname[0], advName);

                    assertTrue("found expired advertisement " + advName, expiry > 0);
                    cm.remove(dirname[0], advName);
                } catch (Exception e) {
                    fail("Failed to remove Peer Adv: " + e.getMessage());
                }
                System.out.println("mt (-) " + id + " " + i + " " + advName);
            }
            System.out.println("mt (-) " + id + " all done");
        }
    }


    private class PeerAdder implements Runnable {
        private int id = 0;
        public PeerAdder(int id) {
            this.id = id;
        }

        public void run() {
            for (int i = 0; i < ITERATIONS && !failed; i++) {
                PeerAdvertisement adv = generatePeerAdv(i);
                String advName = adv.getID().getUniqueValue().toString();

                try {
                    cm.save(dirname[0], advName, adv);
                } catch (Exception e) {
                    fail("Failed to create Peer Adv: " + e.getMessage());
                }
                queue.add(adv);
                System.out.println("mt (+) " + id + " " + i + " " + advName);
            }
            System.out.println("mt (+) " + id + " all done");
        }
    }


    private class PeerSearcher implements Runnable {
        private int id = 0;
        public PeerSearcher(int id) {
            this.id = id;
        }

        public void run() {
            int count = 0;
            final int offset = "urn:jxta:".length();

            for (int i = 0; i < (ITERATIONS / 10) && !failed; i++) {
                try {
                    synchronized (cm) {
                        List searchResults = cm.search(dirname[0], "Name", "CmTestPeer" + "*", 10, null);
                        Enumeration result = Collections.enumeration(searchResults);

                        while (result.hasMoreElements() && !failed) {
                            ByteArrayInputStream dataStream = (ByteArrayInputStream) result.nextElement();
                            StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, dataStream);
                            Enumeration en = doc.getChildren("PID");

                            while (en.hasMoreElements() && !failed) {
                                String val = (String) ((Element) en.nextElement()).getValue();
                                String fn = val.substring(offset);

                                System.out.println("mt (Q) " + id + " " + (count++) + " " + fn);
                                try {
                                    byte[] bits = cm.restoreBytes(dirname[0], fn);

                                    if (bits == null) {
                                        fail("mt (Q) " + id + " db/index consistency failure: " + "\nfailed to restore " + fn);
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                    fail("mt (Q) " + id + " restore failed: " + ex.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("mt ?" + id + " failed: " + e.getMessage());
                }
            }
            System.out.println("mt (Q) " + id + " all done");
        }
    }
}
