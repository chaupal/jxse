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

package net.jxta.protocol;

import junit.framework.*;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.XMLDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import java.net.URI;

public class RdvAdvertisementTest extends TestCase {
  
    private static final String TestName = "Testing J2SE JXTA Peer";
    private static final String TestPeerID = "urn:jxta:uuid-59616261646162614A787461503250336ACC981CFAF047CFADA8A31FC6D0B88C03";
    private static final String TestGroupID = "urn:jxta:jxta-NetGroup";

    public RdvAdvertisementTest(java.lang.String testName) {
        super(testName);
    }
  
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    
        System.err.flush();
        System.out.flush();
    }
  
    public static Test suite() {
        TestSuite suite = new TestSuite(RdvAdvertisementTest.class);

        return suite;
    }
  
    @Override
    protected void setUp() {}

    // Test building an RdvAdv from XML (with optional Name)

    private void _testReadXMLWithName(Document doc) {

        // Create the advertisement
        RdvAdvertisement adv = (RdvAdvertisement) AdvertisementFactory.newAdvertisement((XMLDocument) doc);
        
        assertNotNull("cannot create RdvAdv from template", doc);
	
        // Check values

        assertEquals("Name is corrupted", TestName, adv.getName());

        assertEquals("PeerID is corrupted", TestPeerID, adv.getPeerID().toString());

        assertEquals("GroupID is corrupted", TestGroupID, adv.getGroupID().toString());
    }

    public void testReadWXMLWithName() {
        // Build an RdvAdversitement template
        XMLDocument doc = buildXMLTemplate(true);
	
        _testReadXMLWithName(doc);
    }

    // Test building an RdvAdv from XML (with optional Name)

    public void _testReadXMLWithoutName(Document doc) {
        // Create the advertisement
        RdvAdvertisement adv = (RdvAdvertisement) AdvertisementFactory.newAdvertisement((XMLDocument) doc);

        assertNotNull("cannot create RdvAdv from template", doc);
	
        // Check values

        assertNull("Name is corrupted", adv.getName());

        assertEquals("PeerID is corrupted", TestPeerID, adv.getPeerID().toString());

        assertEquals("GroupID is corrupted", TestGroupID, adv.getGroupID().toString());
    }

    public void testReadXMLWithoutName() {
        // Build an RdvAdversitement template
        XMLDocument doc = buildXMLTemplate(false);

        _testReadXMLWithoutName(doc);
    }

    // Test writing XML with optional Name

    public void testWriteXMLWithName() {

        RdvAdvertisement adv = (RdvAdvertisement) AdvertisementFactory.newAdvertisement(RdvAdvertisement.getAdvertisementType());

        adv.setName(TestName);

        adv.setPeerID(PeerID.create(URI.create(TestPeerID)));

        adv.setGroupID(PeerGroupID.create(URI.create(TestGroupID)));

        adv.setServiceName(TestName);

        // Check values
        assertEquals("Name is corrupted", TestName, adv.getName());

        assertEquals("PeerID is corrupted", TestPeerID, adv.getPeerID().toString());

        assertEquals("GroupID is corrupted", TestGroupID, adv.getGroupID().toString());

        assertEquals("ServiceName is corrupted", TestName, adv.getServiceName());

        // Write XML
        Document doc = adv.getDocument(MimeMediaType.XMLUTF8);

        _testReadXMLWithName(doc);
    }

    // Test writing XML without optional Name
    public void testWriteXMLWithoutName() {

        RdvAdvertisement adv = (RdvAdvertisement) AdvertisementFactory.newAdvertisement(RdvAdvertisement.getAdvertisementType());

        adv.setPeerID(PeerID.create(URI.create(TestPeerID)));

        adv.setGroupID(PeerGroupID.create(URI.create(TestGroupID)));

        adv.setServiceName(TestName);
        
        assertEquals("PeerID is corrupted", TestPeerID, adv.getPeerID().toString());

        assertEquals("GroupID is corrupted", TestGroupID, adv.getGroupID().toString());

        assertEquals("ServiceName is corrupted", TestName, adv.getServiceName());

        // Write XML
        Document doc = adv.getDocument(MimeMediaType.XMLUTF8);

        _testReadXMLWithoutName(doc);
    }

    private XMLDocument buildXMLTemplate(boolean withName) {

        StringBuffer xmlTemplate = new StringBuffer();

        xmlTemplate.append('<').append(RdvAdvertisement.GroupIDTag).append('>');
        xmlTemplate.append(TestGroupID);
        xmlTemplate.append("</").append(RdvAdvertisement.GroupIDTag).append('>');

        xmlTemplate.append('<').append(RdvAdvertisement.PeerIDTag).append('>');
        xmlTemplate.append(TestPeerID);
        xmlTemplate.append("</").append(RdvAdvertisement.PeerIDTag).append('>');

        xmlTemplate.append('<').append(RdvAdvertisement.ServiceNameTag).append('>');
        xmlTemplate.append(TestName);
        xmlTemplate.append("</").append(RdvAdvertisement.ServiceNameTag).append('>');

        if (withName) {
            xmlTemplate.append('<').append(RdvAdvertisement.NameTag).append('>');
            xmlTemplate.append(TestName);
            xmlTemplate.append("</").append(RdvAdvertisement.NameTag).append('>');
        }

        XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8
                ,
                RdvAdvertisement.getAdvertisementType(), xmlTemplate.toString());

        assertNotNull("cannot create Structured Document for the template", doc);

        return doc;
    }
}

