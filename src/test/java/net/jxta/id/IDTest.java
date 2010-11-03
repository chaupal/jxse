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

package net.jxta.id;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.URI;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.peergroup.PeerGroupID;

/**
 * @author  mike
 */
public final class IDTest extends TestCase {

    /** Creates new DocTest */
    public IDTest(String name) {
        super(name);
    }

    public void testID() {
        try {
            ID first = ID.nullID;
            ID second = ID.nullID;
            ID third;
            String  asString;
            URI     asURI;
            ID myPeerGroup;

            assertTrue("comparison of two IDs failed", first.equals(second));

            assertTrue("zero hashcodereturned", 0 != first.hashCode());

            asURI = first.toURI();
            asString = first.toString();

            assertTrue("comparison of ID string and string of URI was not the same", asString.equals(asURI.toString()));

            third = IDFactory.fromURI(asURI);

            assertTrue("result of conversion to URI and back to ID was not equal to original", first.equals(third));

        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }

    }

    public void testSerialization() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(ID.nullID);
            oos.writeObject(PeerGroupID.worldPeerGroupID);
            oos.writeObject(PeerGroupID.defaultNetPeerGroupID);

            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            ID fakenull = (ID) ois.readObject();
            ID fakeworld = (ID) ois.readObject();
            ID fakenet = (ID) ois.readObject();

            assertTrue("null id != read in null id", ID.nullID == fakenull);
            assertTrue("world id != read in world id", PeerGroupID.worldPeerGroupID == fakeworld);
            assertTrue("net id != read in net id", PeerGroupID.defaultNetPeerGroupID == fakenet);
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testIDFactoryURI() {
        try {
            URI urla = new URI("urn:jxta:idform-1234567890");
            URI urlb = new URI("URN:jxta:idform-1234567890");
            URI urlc = new URI("urn:JXTA:idform-1234567890");
            URI urld = new URI("urn:JXTA:idform-123456789%30");
            URI urle = new URI("urn:JXTA:IDForm-1234567890");
            URI urlf = new URI("urn:jxta:idform2-ABCDEFG");
            URI urlg = new URI("urn:jxta:idform3-31:08:66:42:67:::91:24::73");

            ID ida = IDFactory.fromURI(urla);
            ID idb = IDFactory.fromURI(urlb);
            ID idc = IDFactory.fromURI(urlc);
            ID idd = IDFactory.fromURI(urld);
            ID ide = IDFactory.fromURI(urle);
            ID idf = IDFactory.fromURI(urlf);
            ID idg = IDFactory.fromURI(urlg);

            assertEquals(ida, idb);
            assertEquals(idb, idc);
            assertEquals(ida, idc);
            assertEquals(ida, idd);

            assertTrue(!ida.equals(ide));
            assertTrue(!ida.equals(idf));
            assertTrue(!ida.equals(idg));
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public void testJXTAIDFormatURI() {
        try {
            URI nulluri = ID.nullID.toURI();
            URI worlduri = PeerGroupID.worldPeerGroupID.toURI();
            URI neturi = PeerGroupID.defaultNetPeerGroupID.toURI();

            ID nullid = IDFactory.fromURI(nulluri);
            ID worldid = IDFactory.fromURI(worlduri);
            ID netid = IDFactory.fromURI(neturi);

            assertEquals(nullid, ID.nullID);
            assertEquals(worldid, PeerGroupID.worldPeerGroupID);
            assertEquals(netid, PeerGroupID.defaultNetPeerGroupID);
        } catch (Exception everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(IDTest.class);

        return suite;
    }
}
