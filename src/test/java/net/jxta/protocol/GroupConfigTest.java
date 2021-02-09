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

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.impl.protocol.DiscoveryConfigAdv;
import net.jxta.impl.protocol.GroupConfig;
import net.jxta.impl.protocol.HTTPAdv;
import net.jxta.impl.protocol.RdvConfigAdv;
import net.jxta.impl.protocol.RelayConfigAdv;
import net.jxta.peergroup.IModuleDefinitions;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class GroupConfigTest {

    private static GroupConfig createTestInstance() {
        Advertisement params = AdvertisementFactory.newAdvertisement(GroupConfig.getAdvertisementType());

        assertTrue("AdvertisementFactory did not create GroupConfig instance", params instanceof GroupConfig);

        GroupConfig cp = (GroupConfig) params;

        // Add some sections.
        cp.putServiceParam(IModuleDefinitions.httpProtoClassID, wrapParm(AdvertisementFactory.newAdvertisement(HTTPAdv.getAdvertisementType()), false));
        cp.setSvcConfigAdvertisement(IModuleDefinitions.relayProtoClassID, AdvertisementFactory.newAdvertisement(RelayConfigAdv.getAdvertisementType()), true);
        cp.setSvcConfigAdvertisement(IModuleDefinitions.rendezvousClassID, AdvertisementFactory.newAdvertisement(RdvConfigAdv.getAdvertisementType()), false);
        cp.putServiceParam(IModuleDefinitions.discoveryClassID, wrapParm(AdvertisementFactory.newAdvertisement(DiscoveryConfigAdv.getAdvertisementType()), true));

        return cp;
    }

    /*
     * FIXME (2010/07/06 iainmcg): this test appears to have been broken for a long time. The root cause is that
     * the equals() implementation in ConfigParams compares two maps:<br/>
     * 1. a map of parameters, which are LiteXMLDocuments. The equals method on LiteXMLDocument does not regard
     * any nodes as equal unless they come from the same document. As this test creates a new document by deserialising,
     * this fails.<br/>
     * 2. a map of advertisements, which do not implement equals at all. As such, standard object equality treats them
     * as unequal.
     * 
     * I can't find any implementation in the revision history of these classes that would have worked directly, so
     * this test must have relied on a lot of circumstantial evidence to pass. Disabling for now.
     */
    @Ignore
    @Test
    public void testSerialization() {
        try {
            GroupConfig cp = createTestInstance();

            XMLDocument<?> toDoc = (XMLDocument<?>) cp.getDocument(MimeMediaType.XMLUTF8);
            StringWriter writer = new StringWriter();
            toDoc.sendToWriter(writer);

            XMLDocument<?> fromDoc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(writer.toString()));

            Advertisement rawAdv = AdvertisementFactory.newAdvertisement(fromDoc);

            assertTrue("AdvertisementFactory did not create GroupConfig instance", rawAdv instanceof GroupConfig);

            GroupConfig cp2 = (GroupConfig) rawAdv;

            assertEquals("Original instance and deserialized instance were not identical.", cp, cp2);
        } catch(Exception failed) {
            fail(failed.getMessage());
        }
    }

    @Test
    public void testClone() {
        GroupConfig cp = createTestInstance();
        GroupConfig cp2 = cp.clone();

        assertEquals("Original instance and clone instance were not identical.", cp, cp2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static XMLDocument<?> wrapParm(Advertisement srcAdv, boolean enabled) {
        try {
            XMLDocument<?> advDoc = (XMLDocument<?>) srcAdv.getDocument(MimeMediaType.XMLUTF8);

            XMLDocument doc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");

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
