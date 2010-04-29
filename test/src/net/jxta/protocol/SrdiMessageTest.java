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

import junit.framework.*;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.impl.protocol.SrdiMessageImpl;
import java.util.Collection;
import java.util.Vector;

/**
 *  A JUnit test for SrdiMessage
 */
public class SrdiMessageTest extends TestCase {

    private final static String TestName = "Srdi Message Test";
    private static final String srcpeer = "urn:jxta:uuid-59616261646162614A787461503250330000000000000000000000000000000103";
    private static final String key = "key";
    private static final String val = "value";
    private static final long exp = Long.MAX_VALUE;
    private String strdoc;
    private SrdiMessageImpl srdiMsg = null;

    /**
     *Constructor for the SrdiMessageTest object
     *
     * @param  testName  test name
     */
    public SrdiMessageTest(java.lang.String testName) {
        super(testName);
    }

    /**
     *  The main program for the SrdiMessageTest class
     *
     * @param  args  The command line arguments
     */
    public static void main(java.lang.String[] args) {
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
        TestSuite suite = new TestSuite(SrdiMessageTest.class);

        return suite;
    }

    /**
     *  The JUnit setup method
     */
    @Override
    protected void setUp() {}

    public void testConstructMessage() {

        Vector entries = new Vector(5);

        for (int i = 0; i < 5; i++) {
            SrdiMessage.Entry entry = new SrdiMessage.Entry(key + i, val + i, exp);
            entries.add(entry);
        }

        try {
            srdiMsg = new SrdiMessageImpl(srcpeer, 0, "Peers", entries);
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
            iae.printStackTrace();
        }

        StructuredDocument doc = (StructuredDocument) srdiMsg.getDocument(new MimeMediaType("text/xml"));

        assertNotNull("Failed to construct SrdiMessage", doc);

    }
    
    public void testConstructDocFromStr() {

        Vector entries = new Vector(5);

        for (int i = 0; i < 5; i++) {
            SrdiMessage.Entry entry = new SrdiMessage.Entry(key + i, val + i,
                                                            exp);

            entries.add(entry);
        }

        try {
            srdiMsg = new SrdiMessageImpl(srcpeer, 0, "Peers", entries);
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
            iae.printStackTrace();
        }

        StructuredDocument doc = (StructuredDocument) srdiMsg.getDocument(new MimeMediaType("text/xml"));

        assertNotNull("Failed to construct SrdiMessage", doc);

        this.strdoc = srdiMsg.toString();
        assertNotNull("Corrupted SrdiMessage string representation", this.strdoc);
        createMessagefromString(srdiMsg.toString());
    }

    private StructuredDocument createMessagefromString(String strdoc) {

        StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(new MimeMediaType("text/xml")
                ,
                SrdiMessage.getMessageType(), strdoc);

        assertNotNull("Failed to create SrdiMessage from a String", doc);
        return doc;
    }

    public void testGetElements() {

        Vector entries = new Vector(5);

        for (int i = 0; i < 5; i++) {
            SrdiMessage.Entry entry = new SrdiMessage.Entry(key + i, val + i,
                                                            exp);

            entries.add(entry);
        }

        try {
            srdiMsg = new SrdiMessageImpl(srcpeer, 0, "Peers", entries);
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
            iae.printStackTrace();
        }

        StructuredDocument doc = (StructuredDocument) srdiMsg.getDocument(new MimeMediaType("text/xml"));

        assertNotNull("Failed to construct SrdiMessage", doc);

        assertEquals("Corrupted src peer", srcpeer, srdiMsg.getPeerID().toString());
        assertEquals("Corrupted Primary Key", "Peers", srdiMsg.getPrimaryKey());

        Collection gotEntries = srdiMsg.getEntries();
        
        assertTrue("all entries didn't make it", gotEntries.containsAll(entries));
        
        gotEntries.removeAll(entries);
        
        assertTrue("should have been empty", gotEntries.isEmpty());
    }    
}

