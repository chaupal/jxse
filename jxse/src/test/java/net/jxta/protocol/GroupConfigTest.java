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


import java.io.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.jxta.document.*;
import net.jxta.peergroup.PeerGroup;

import net.jxta.impl.protocol.*;
import net.jxta.protocol.PipeAdvertisement;

/**
 */
public class GroupConfigTest extends TestCase {
    
    /**
     * Constructor for GroupConfigTest.
     * 
     * @param name
     */
    public GroupConfigTest(String name) {
        super(name);
    }
    
    public void testDeprecated() {
    }
    
    private static GroupConfig createTestInstance() {
        Advertisement params = AdvertisementFactory.newAdvertisement(GroupConfig.getAdvertisementType());
        
        assertTrue("AdvertisementFactory did not create GroupConfig instance", params instanceof GroupConfig);
        
        GroupConfig cp = (GroupConfig) params;
        
        // Add some sections.
        cp.putServiceParam(PeerGroup.httpProtoClassID, wrapParm(AdvertisementFactory.newAdvertisement(HTTPAdv.getAdvertisementType()), false));
        cp.setSvcConfigAdvertisement(PeerGroup.relayProtoClassID, AdvertisementFactory.newAdvertisement(RelayConfigAdv.getAdvertisementType()), true);
        cp.setSvcConfigAdvertisement(PeerGroup.rendezvousClassID, AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType()), false);
        cp.putServiceParam(PeerGroup.discoveryClassID, wrapParm(AdvertisementFactory.newAdvertisement(DiscoveryConfigAdv.getAdvertisementType()), true));
        
        return cp;
    }
    
    public void testSerialization() {
        try {
            GroupConfig cp = createTestInstance();
            
            XMLDocument toDoc = (XMLDocument) cp.getDocument(MimeMediaType.XMLUTF8);
            StringWriter writer = new StringWriter();
            toDoc.sendToWriter(writer);
            
            XMLDocument fromDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(writer.toString()));
            
            Advertisement rawAdv = AdvertisementFactory.newAdvertisement(fromDoc);
            
            assertTrue("AdvertisementFactory did not create GroupConfig instance", rawAdv instanceof GroupConfig);
            
            GroupConfig cp2 = (GroupConfig) rawAdv;
            
            assertEquals("Original instance and deserialized instance were not identical.", cp, cp2);
        } catch(Exception failed) {
            fail(failed.getMessage());
        }
    }
    
    public void testClone() {
        GroupConfig cp = createTestInstance();
        GroupConfig cp2 = cp.clone();
        
        
        assertEquals("Original instance and clone instance were not identical.", cp, cp2);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(GroupConfigTest.class);
        
        return suite;
    }
    
    private static XMLDocument wrapParm(Advertisement srcAdv, boolean enabled) {
        try {
            XMLDocument advDoc = (XMLDocument) srcAdv.getDocument(MimeMediaType.XMLUTF8);

            XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");

            StructuredDocumentUtils.copyElements(doc, doc, advDoc);
            if (!enabled) {
                doc.appendChild(doc.createElement("isOff"));
            }

            return doc;
        } catch (Throwable ez1) {
            ez1.printStackTrace();
            return null;
        }
    }

}
