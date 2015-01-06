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

package net.jxta.protocol;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.impl.protocol.ResolverQuery;
import net.jxta.peer.PeerID;
import net.jxta.test.util.AdvUtil;

/**
 *  A JUnit test for ResolverQueryMsgTest
 */
public class ResolverQueryMsgTest extends TestCase {

    private static final String handlername = "urn:jxta:uuid-DEADBEEFDEAFBABAFEEDBABE0000000006";
    PeerID src;
    String querystr = "Some arbitrary query";
    int  hc = 0;
    int qid = 0;

    /**
     *Constructor for the object
     *
     */
    public ResolverQueryMsgTest(String testName) {
        super(testName);
    }

    /**
     *  The main program for the ResolverQueryMsgTest class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    /**
     *  A unit test suite for JUnit
     *
     * @return    The test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ResolverQueryMsgTest.class);

        return suite;
    }

    /**
     *  The JUnit setup method
     */
    @Override
    protected void setUp() {}

    public void testConstructMessage() {
        try {
            PeerAdvertisement padv = AdvUtil.newPeerAdv("Fakey1", "0.0.0.0", 0, false);

            src = (PeerID)padv.getPeerID();
            ResolverQueryMsg query = new ResolverQuery();

            query.setHandlerName(handlername);
            query.setQuery(querystr);
            query.setQueryId(qid);
            query.setSrcPeer(src);

            StructuredDocument<?> doc = (StructuredDocument<?>) query.getDocument(new MimeMediaType("text/xml"));

            assertNotNull("Failed to construct ResolverQueryMsg", doc);

            assertEquals("Corrupted handlername", handlername, query.getHandlerName());
            assertEquals("Corrupted hopcount", hc, query.getHopCount());
            query.incrementHopCount();
            assertEquals("Corrupted hopcount", hc + 1, query.getHopCount());

            assertEquals("Corrupted query", querystr, query.getQuery());
            assertEquals("Corrupted source peer", src.toString(), query.getSrcPeer().toString());

            ResolverQueryMsg doctest = new ResolverQuery(doc);

            assertNotNull("Failed to construct ResolverQueryMsg", doctest);
        } catch (Exception e) {
            fail("exception thrown : " + e.getMessage());
        }
    }

}

