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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.net.URISyntaxException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;

/**
 * This class was formerly known as SrdiIndexTest. It contains a number
 * of stress tests of the XIndiceSrdiIndexBackend, which is the original
 * SrdiIndex implementation. The tests take some time to run so are not
 * technically unit tests. In future, these tests may be refactored into
 * a more generic load testing suite for all SrdiIndexBackend implementations.
 */
public class XIndiceSrdiIndexBackendOldLoadTest extends TestCase {
    static final String peerStr = "urn:jxta:uuid-59616261646162614A7874615032503346A235E18A1D427FAB4E8CA426964ADD03";
    static final String phantomStr = "urn:jxta:uuid-59616261646162614A7874615032503346A235E18A1D427ABA4E8CA426964ADD03";
    static final String pipeID = "urn:jxta:uuid-59616261646162614E50472050325033DCD44908E42B4EF790A4B9715E5AE29904";

    static final PeerID pid;

    static {
        try {
            pid = (PeerID) IDFactory.fromURI(new URI(peerStr));
        } catch (URISyntaxException failed) {
            throw new UnknownError("can't build hard coded id");
        }
    }

    static final PeerID phantomPid;

    static {
        try {
            phantomPid = (PeerID) IDFactory.fromURI(new URI(phantomStr));
        } catch (URISyntaxException failed) {
            throw new UnknownError("can't build hard coded id");
        }
    }

    private static final int ITERATIONS = 20000;
    private List<String> queue = Collections.synchronizedList(new ArrayList<String>());
    private static boolean failed = false;

    /**
     * Constructor for the SrdiIndexTest object
     *
     * @param testName test name
     */
    public XIndiceSrdiIndexBackendOldLoadTest(String testName) {
        super(testName);
    }

    /**
     * A unit test suite for JUnit
     *
     * @return The test suite
     */
    public static Test suite() {
        return new TestSuite(XIndiceSrdiIndexBackendOldLoadTest.class);
    }

    @Override
    public void tearDown() {
        System.gc();
    }

    /**
     * The main program to test CmCache
     *
     * @param argv The command line arguments
     * @throws Exception Description of Exception
     */
    public static void main(String[] argv) throws Exception {

        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public void testRefAddDelGC() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        srdi.add("pkey", "ID", pipeID, pid, 1000);
        srdi.add("pkey", "ID", pipeID, pid, 0);
        srdi.garbageCollect();
        List<PeerID> res = srdi.query("pkey", "ID", pipeID, 1);

        assertTrue("query should not have returned a result", res.size() == 0);
        srdi.stop();
    }

    public void testQuery() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        for (int i = 0; i < ITERATIONS; i++) {
            srdi.add("pkey", "attr", "value" + i, pid, Long.MAX_VALUE);
        }
        for (int i = 0; i < ITERATIONS; i++) {
            List<PeerID> res = srdi.query("pkey", "attr", "value" + i, 1);

            assertTrue("query should have returned a result", res.size() > 0);
            if (res.size() > 0) {
                PeerID path = res.get(0);

                assertEquals("Incorrect result", peerStr, path.toString());
            }
        }
        srdi.stop();
    }

    public void testGC() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        for (int i = 0; i < ITERATIONS; i++) {
            srdi.add("pkey", "attr", "value" + i, pid, 1000);
        }

        srdi.garbageCollect();
        srdi.stop();
    }

    public void testRemovePath() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        for (int i = 0; i < ITERATIONS; i++) {
            srdi.add("pkey", "attr", "value" + i, pid, 100000000);
        }
        long t0 = System.currentTimeMillis();
        srdi.remove(pid);
        System.out.println("Removed  :" + ITERATIONS + "  in " + (System.currentTimeMillis() - t0) + " ms");
        for (int i = 0; i < ITERATIONS; i++) {
            List<PeerID> res = srdi.query("pkey", "attr", "value+i", 1);

            assertTrue("query should not have returned a result", res.size() == 0);
        }
        // the following should not produce exceptions
        srdi.remove(pid);
        srdi.stop();
    }

    public void testRemovePhantom() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        for (int i = 0; i < ITERATIONS; i++) {
            srdi.add("pkey", "attr", "value" + i, pid, 1000);
        }
        srdi.remove(phantomPid);
        srdi.stop();
    }

    public void testMultithread() {
        SrdiIndex srdi = new SrdiIndex(null, "SrdiIndexTest");

        System.out.println("mt starting...");
        final int THREADS = 5;
        Thread adders[] = new Thread[THREADS];
        Thread removers[] = new Thread[THREADS];
        Thread searchers[] = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            adders[i] = new Thread(new Adder(i, srdi));
            removers[i] = new Thread(new Remover(i, srdi));
            searchers[i] = new Thread(new Searcher(i, srdi));
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
            } catch (InterruptedException ignore) {
            }
        }

        if (failed) {
            fail("mt test failed");
        }

        System.out.println("mt all done");
        srdi.stop();
    }

    private class Remover implements Runnable {
        private int id = 0;
        private SrdiIndex srdi;

        public Remover(int id, SrdiIndex srdi) {
            this.id = id;
            this.srdi = srdi;
        }

        public void run() {
            for (int i = 0; i < (ITERATIONS / 10) && !failed; i++) {
                while (queue.size() < 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }

                    queue.remove(0);
                    srdi.remove(phantomPid);
                    srdi.remove(pid);
                }
            }
            System.out.println("remover thread " + id + " done");
        }
    }


    private class Adder implements Runnable {
        private int id = 0;
        private SrdiIndex srdi;

        public Adder(int id, SrdiIndex srdi) {
            this.id = id;
            this.srdi = srdi;
        }

        public void run() {
            for (int i = 0; i < ITERATIONS && !failed; i++) {
                String value = "value" + i;
                srdi.add("pkey", "attr", value, pid, Long.MAX_VALUE);
                queue.add(value);
            }
            System.out.println("adder thread +" + id + " done");
        }
    }


    private class Searcher implements Runnable {
        private int id = 0;
        private SrdiIndex srdi;

        public Searcher(int id, SrdiIndex srdi) {
            this.id = id;
            this.srdi = srdi;
        }

        public void run() {
            for (int i = 0; i < (ITERATIONS / 10) && !failed; i++) {
                List<PeerID> res = srdi.query("pkey", "attr", "value" + i, 1);

                if (res.size() > 0) {
                    PeerID path = res.get(0);
                    assertEquals("Incorrect result", peerStr, path.toString());
                }
            }
            System.out.println("searcher thread " + id + " done");
        }
    }
}
