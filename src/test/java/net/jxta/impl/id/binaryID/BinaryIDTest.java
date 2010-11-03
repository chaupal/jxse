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

package net.jxta.impl.id.binaryID;

import java.net.URI;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.jxta.codat.CodatID;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

/**
 * Tests for BinaryID and DigestID
 * net.jxta.id.BinaryIDTest
 * @author Daniel Brookshier <a HREF="mailto:turbogeek@cluck.com">turbogeek@cluck.com</a>
 */
public final class BinaryIDTest extends TestCase {
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BinaryIDTest.class);

        return suite;
    }
	
    /** Creates new DocTest */
    public BinaryIDTest(String name) {
        super(name);
    }
    byte[] data1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
    byte[] data2 = { 16, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
    byte[] data3 = { 11, 22, 33, 44, 55, 66, 77, 88, 99, 99, 99, 99, 99, 99, 99, 99 };

    public void testBinaryID() {
        try {
            // System.out.println("");
            byte type = BinaryID.flagGenericID;
            BinaryID one = BinaryIDFactory.newBinaryID(type, data1, false);
            // System.out.println("one:"+one);
            BinaryID two = BinaryIDFactory.newBinaryID(type, data2, true);
            // System.out.println("two:"+two);
            BinaryID three = BinaryIDFactory.newBinaryID(type, data3, false);

            // System.out.println("three:"+three);
            // Common ID tests

            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            BinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

            // Tests specific to BinaryID
            assertTrue("type match one == two", one.type() == two.type());
            assertTrue("type match one == four", one.type() == four.type());
            assertTrue("type match one == three", one.type() == three.type());

            BinaryID ten = BinaryIDFactory.newBinaryID(BinaryID.flagPeerGroupID, data3, false);

            // System.out.println("ten:"+ten);
            assertTrue("type match one != ten", one.type() != ten.type());
            assertTrue("objec match one != ten", !one.equals(ten));

            assertTrue("id match one == two", one.getID().equals(two.getID()));
            assertTrue("id match one == four", one.getID().equals(four.getID()));
            assertTrue("id match one != three", !one.getID().equals(three.getID()));

        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }

    }

    public void testPeerGroupID() {
        try {
            byte type = BinaryID.flagGenericID;
            PeerGroupBinaryID one = new PeerGroupBinaryID(data1, false);
            PeerGroupBinaryID two = new PeerGroupBinaryID(data2, true);
            PeerGroupBinaryID three = new PeerGroupBinaryID(data3, false);

            // Common ID tests
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PeerGroupBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

            // Create a uuid parent
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();

            one = new PeerGroupBinaryID(base, data1, false);
            two = new PeerGroupBinaryID(base, data2, true);
            three = new PeerGroupBinaryID(base, data3, false);

            // Common ID tests (with a base peer group
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

            // Try comparing a group without a base.
            PeerGroupBinaryID five = new PeerGroupBinaryID(data1, false);

            assertTrue("simple group shouldnt have had a parent", null == five.getParentPeerGroupID());

            assertTrue("comparison of one != five", !one.equals(five));

            // Check that the parent == parent in the new group.
            net.jxta.impl.id.UUID.PeerGroupID base2 = (net.jxta.impl.id.UUID.PeerGroupID) one.getParentPeerGroupID();

            assertTrue("comparison of base == base2", base.equals(base2));

            URI asURI = one.toURI();
            String asString = one.toString();

            // System.out.println("");
            // System.out.println("one:"+one);
            // System.out.println("asURI:"+asURI);
            // System.out.println("asString:"+asString);
            assertTrue("comparison of ID string and string of URI was not the same", one.toString().equals(asURI.toString()));

            PeerGroupBinaryID six = (PeerGroupBinaryID) IDFactory.fromURI(asURI);

            assertTrue("result of conversion to URI and back to ID was not equal to original", one.equals(six));

            Object fromOne = one.getUniqueValue();
            Object fromTwo = two.getUniqueValue();
            Object fromThree = three.getUniqueValue();

            assertTrue("comparison of getUniqueValue ", fromOne.equals(fromTwo));

            assertTrue("comparison of getUniqueValue", !fromOne.equals(fromThree));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }

    }

    public void testPeerID() {
        try {
            // Create a uuid parent
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();
            PeerBinaryID one = new PeerBinaryID(base, data1, false);
            PeerBinaryID two = new PeerBinaryID(base, data2, true);
            PeerBinaryID three = new PeerBinaryID(base, data3, false);

            // Common ID tests
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PeerBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

            // Check that the parent == parent in the new group.
            net.jxta.impl.id.UUID.PeerGroupID base2 = (net.jxta.impl.id.UUID.PeerGroupID) one.getPeerGroupID();

            assertTrue("comparison of base == base2", base.equals(base2));

            URI asURI = one.toURI();
            String asString = one.toString();

            assertTrue("comparison of ID string and string of URI was not the same", one.toString().equals(asURI.toString()));

            PeerBinaryID six = (PeerBinaryID) IDFactory.fromURI(asURI);

            assertTrue("result of conversion to URI and back to ID was not equal to original", one.equals(six));

            Object fromOne = one.getUniqueValue();
            Object fromTwo = two.getUniqueValue();
            Object fromThree = three.getUniqueValue();

            assertTrue("comparison of getUniqueValue ", fromOne.equals(fromTwo));

            assertTrue("comparison of getUniqueValue", !fromOne.equals(fromThree));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testPipeID() {
        try {
            // Create a uuid parent
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();
            PipeBinaryID one = new PipeBinaryID(base, data1, false);
            PipeBinaryID two = new PipeBinaryID(base, data2, true);
            PipeBinaryID three = new PipeBinaryID(base, data3, false);

            // Common ID tests
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PipeBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

            // Check that the parent == parent in the new group.
            net.jxta.impl.id.UUID.PeerGroupID base2 = (net.jxta.impl.id.UUID.PeerGroupID) one.getPeerGroupID();

            assertTrue("comparison of base == base2", base.equals(base2));

            URI asURI = one.toURI();
            String asString = one.toString();

            assertTrue("comparison of ID string and string of URI was not the same", one.toString().equals(asURI.toString()));

            PipeBinaryID six = (PipeBinaryID) IDFactory.fromURI(asURI);

            assertTrue("result of conversion to URI and back to ID was not equal to original", one.equals(six));

            Object fromOne = one.getUniqueValue();
            Object fromTwo = two.getUniqueValue();
            Object fromThree = three.getUniqueValue();

            assertTrue("comparison of getUniqueValue ", fromOne.equals(fromTwo));

            assertTrue("comparison of getUniqueValue", !fromOne.equals(fromThree));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testPeerGroupBinaryIDDigestID() {
        String clearTextID = "turbogeek";
        String function = "test";
        String function2 = "test-test";

        try {
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();
            DigestTool digestTool = new DigestTool();
            PeerGroupBinaryID one = digestTool.createPeerGroupID(base, clearTextID, function);
            PeerGroupBinaryID two = digestTool.createPeerGroupID(base, clearTextID, function);
            PeerGroupBinaryID three = digestTool.createPeerGroupID(base, clearTextID, function2);

            // Common ID tests
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PeerGroupBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testPipeBinaryIDDigestID() {
        String clearTextID = "turbogeek";
        String function = "test";
        String function2 = "test-test";

        try {
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();
            DigestTool digestTool = new DigestTool();
            PipeBinaryID one = digestTool.createPipeID(base, clearTextID, function); 
            PipeBinaryID oneOne = digestTool.createPipeID(base, clearTextID, function); 
            PipeBinaryID two = digestTool.createPipeID(base, clearTextID, function);
            PipeBinaryID three = digestTool.createPipeID(base, clearTextID, function2);

            // Common ID tests
            // System.out.println("\n\nVisual URI check:\n   one.toURI: '"+one.toURI()+"'");
            // System.out.println("oneOne.toURI: '"+oneOne.toURI()+"'");
            // System.out.println("   two.gettoURIURI: '"+toURI.toURI()+"'\n");
            assertTrue("comparison of one == oneOne", one.equals(oneOne));
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PipeBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testPeerBinaryIDDigestID() {
        String clearTextID = "turbogeek";
        String function = "test";
        String function2 = "test-test";

        try {
            net.jxta.impl.id.UUID.PeerGroupID base = new net.jxta.impl.id.UUID.PeerGroupID();
            DigestTool digestTool = new DigestTool();
            PeerBinaryID one = digestTool.createPeerID(base, clearTextID, function);
            PeerBinaryID two = digestTool.createPeerID(base, clearTextID, function);
            PeerBinaryID three = digestTool.createPeerID(base, clearTextID, function2);

            // Common ID tests
            assertTrue("comparison of one == two", one.equals(two));
            assertTrue("comparison of two != three", !two.equals(three));
            assertTrue("comparison of one != three", !one.equals(three));

            PeerBinaryID four = one;

            assertTrue("comparison of clone", one.equals(four));

            assertTrue("hashcode match one == two", one.hashCode() == two.hashCode());
            assertTrue("hashcode match one == four", one.hashCode() == four.hashCode());
            assertTrue("hashcode match one != three", one.hashCode() != three.hashCode());

        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testCodatID() {
        try {
            CodatID first = IDFactory.newCodatID(IDFactory.newPeerGroupID());
            CodatID second = IDFactory.newCodatID(IDFactory.newPeerGroupID());
            CodatID third;
            ID interloper = IDFactory.newPeerID(IDFactory.newPeerGroupID());
            String  asString;
            URI     asURI;
            ID myPeerGroup;
            boolean isStatic;

            assertTrue("comparison of a CodatID against itself failed", first.equals(first));

            assertTrue("comparison of two different CodatIDs should have failed", !first.equals(second));

            assertTrue("comparison of different types should have failed", !first.equals(interloper));

            assertTrue("comparison of different types should have failed", !interloper.equals(first));

            assertTrue("zero hashcodereturned", 0 != first.hashCode());

            asURI = first.toURI();
            asString = first.toString();

            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));

            third = (CodatID) IDFactory.fromURI(asURI);

            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));

            myPeerGroup = first.getPeerGroupID();

            assertTrue("clone of ID is not of same peergroup.", first.getPeerGroupID().equals(third.getPeerGroupID()));

            assertTrue("dynamic CodatID did not test as such.", !first.isStatic());
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());

        }
    }
}
