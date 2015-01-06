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
import java.net.URI;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.jxta.document.*;
import net.jxta.id.ID;
import net.jxta.pipe.PipeID;

/**
 * @author nadment
 */
public class PipeAdvertisementTest extends TestCase {
    private static final String TestName = "Testing J2SE JXTA Pipe (����)";
    private static final String TestType = "SuperPipe";
    private static final String TestDescription = "Testing J2SE JXTA Pipe desc (����)";
    private static final PipeID TestPipeID = (PipeID) ID.create(
            URI.create("urn:jxta:uuid-59616261646162614A787461503250336ACC981CFAF047CFADA8A31FC6D0B88C04"));

    /**
     * Constructor for PipeAdvertisementTest.
     * @param arg0
     */
    public PipeAdvertisementTest(String arg0) {
        super(arg0);
    }

    private PipeAdvertisement buildPipe() {
        PipeAdvertisement pipe = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                PipeAdvertisement.getAdvertisementType());

        assertNotNull("AdvertisementFactory cannot create PipeAdvertisement", pipe);

        pipe.setPipeID(TestPipeID);
        pipe.setName(TestName);
        pipe.setType(TestType);
        pipe.setDesc(buildDesc());

        return pipe;
    }

    public void testDeprecated() {
        PipeAdvertisement pipe = buildPipe();

        pipe.setDescription(TestDescription);
        assertEquals("Description is corrupted (deprecated)", TestDescription, pipe.getDescription());
    }

    public void testDocument() {
        PipeAdvertisement pipe1 = buildPipe();
        PipeAdvertisement pipe2 = null;

        try {
            XMLDocument<?> doc = (XMLDocument<?>) pipe1.getDocument(MimeMediaType.XMLUTF8);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            doc.sendToStream(bos);
            bos.close();

            InputStream bis = new ByteArrayInputStream(bos.toByteArray());
            XMLDocument<?> newDoc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, bis);

            pipe2 = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(newDoc);
            bis.close();
        } catch (IOException e) {
            fail("Transfer document to stream:" + e.toString());
        }

        assertEquals("Name is corrupted", pipe1.getName(), pipe2.getName());
        assertEquals("Desc is corrupted", pipe1.getDesc().toString(), pipe2.getDesc().toString());
        assertEquals("PipeID is corrupted", pipe1.getPipeID(), pipe2.getPipeID());
        assertEquals("Pipe type is corrupted", pipe1.getType(), pipe2.getType());

        // FIXME: test services
    }

    public void testClone() {
        PipeAdvertisement pipe1 = buildPipe();
        PipeAdvertisement pipe2 = pipe1.clone();

        assertEquals("Name is corrupted", pipe1.getName(), pipe2.getName());
        assertEquals("Desc is corrupted", pipe1.getDesc().toString(), pipe2.getDesc().toString());
        assertEquals("PipeID is corrupted", pipe1.getPipeID(), pipe2.getPipeID());
        assertEquals("Pipe type is corrupted", pipe1.getType(), pipe2.getType());

        // FIXME: test services
    }

    private Element<?> buildDesc() {
        StructuredTextDocument desc = (StructuredTextDocument<?>) StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.XMLUTF8, "Desc");

        desc.appendChild(desc.createElement("Text1", TestDescription));
        desc.appendChild(desc.createElement("Text2", TestDescription));
        desc.appendChild(desc.createElement("Text3", TestDescription));
        return desc;
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PipeAdvertisementTest.class);

        return suite;
    }

}
