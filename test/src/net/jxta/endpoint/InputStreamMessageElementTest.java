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

package net.jxta.endpoint;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Vector;
import java.util.Collections;

import java.io.IOException;

import junit.framework.*;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.InputStreamMessageElement;


/**
 *
 * @author mike
 */
public class InputStreamMessageElementTest extends TestCase {

    String data = "11111111111111111111111111111111111111111111111111111111111111\n\r"
            + "22222222222222222222222222222222222222222222222222222222222222\n\r"
            + "33333333333333333333333333333333333333333333333333333333333333\n\r";

    public InputStreamMessageElementTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(InputStreamMessageElementTest.class);

        return suite;
    }

    static class noMarkInputStream extends java.io.FilterInputStream {

        noMarkInputStream(InputStream in) {
            super(in);
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public void mark(int limit) {}

        @Override
        public void reset() throws IOException {
            throw new IOException("no way");
        }

    }

    private void printStream(InputStream is, String comment) {
        try {
            System.err.println(comment + "_START________");
            while (is.available() > 0) {
                System.err.print((char) is.read());
            }

            System.err.println(comment + "_END__________");
        } catch (IOException e) {
            System.err.println("EEROR");
        }

    }

    private byte[] streamToBytes(InputStream is) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        
        try {
            while (true) {
                int res = is.read();
                
                if (-1 == res) {
                    break;
                }
                
                bout.write(res);
            }
            
        } catch (Throwable e) {
            this.fail("Stream to bytes");
        }
        
        return bout.toByteArray();
    }
    
    public void testVisual() throws IOException {

        InputStream stream = new ByteArrayInputStream(data.getBytes());

        printStream(stream, "Origine");
        stream.reset();

        MessageElement element = new InputStreamMessageElement("TEST", null, stream, null);

        printStream(element.getStream(), "Element");
    }
	
    public void testGetStream() throws IOException {

        InputStream stream = new ByteArrayInputStream(data.getBytes());
        MessageElement element = new InputStreamMessageElement("TEST", null, stream, null);

        this.assertTrue("getStream()", Arrays.equals(data.getBytes(), streamToBytes(element.getStream())));
    }
		
    public void testToString() throws IOException {

        InputStream stream = new ByteArrayInputStream(data.getBytes());
        MessageElement element = new InputStreamMessageElement("TEST", null, stream, null);
        
        this.assertEquals("toString()", data, element.toString());
    }

    public void testNewByteArrayMessageElement() {
        try {
            byte[] source1 = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48 };
            byte[] source2 = { 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68 };
            byte[] source3 = {};

            InputStream in1 = new ByteArrayInputStream(source1);
            InputStream in2 = new ByteArrayInputStream(source2);

            Vector concat = new Vector(0);

            concat.add(new ByteArrayInputStream(source1));
            concat.add(new ByteArrayInputStream(source2));

            InputStream in3 = new SequenceInputStream(Collections.enumeration(concat));

            InputStream in4 = new noMarkInputStream(new ByteArrayInputStream(source1));

            InputStream in5 = new ByteArrayInputStream(source3);

            // default blob, default mime type
            InputStreamMessageElement el1 = new InputStreamMessageElement("element1", null, in1, (MessageElement) null);

            // subset blob, binary mime type
            InputStreamMessageElement el2 = new InputStreamMessageElement("element2", new MimeMediaType("image/jpeg"), in2, 7
                    ,
                    (MessageElement) null);

            // combined blob, binary mime type
            InputStreamMessageElement el3 = new InputStreamMessageElement("element3", new MimeMediaType("image/jpeg"), in3, 11
                    ,
                    (MessageElement) null);

            // subset blob, default mime type, no mark on stream
            InputStreamMessageElement el4 = new InputStreamMessageElement("element4", null, in4, 6, (MessageElement) null);

            // subset blob, default mime type, no mark on stream
            InputStreamMessageElement el5 = new InputStreamMessageElement("element5", null, in5, null);

            // getdatasize

            System.out.println(el1.getByteLength());

            System.out.println(el2.getByteLength());

            System.out.println(el3.getByteLength());

            System.out.println(el4.getByteLength());

            // hashcode

            System.out.println(el1.hashCode());

            System.out.println(el2.hashCode());

            System.out.println(el3.hashCode());

            System.out.println(el4.hashCode());

            System.out.println(el5.hashCode());

            // sendDataStream

            System.out.print("el1 send (" + el1.getMimeType() + ") : ");

            el1.sendToStream(System.out);

            System.out.println();

            System.out.print("el2 send (" + el2.getMimeType() + ") : ");

            el2.sendToStream(System.out);

            System.out.println();

            System.out.print("el3 send (" + el3.getMimeType() + ") : ");

            el3.sendToStream(System.out);

            System.out.println();

            System.out.print("el4 send (" + el4.getMimeType() + ") : ");

            el4.sendToStream(System.out);

            System.out.println();

            // getDataStream

            System.out.print("el1 read (" + el1.getMimeType() + ") : ");

            InputStream in = el1.getStream();

            do {
                int aChar = in.read();

                if (-1 == aChar) {
                    break;
                }

                System.out.print("0x" + Integer.toHexString(aChar) + " ");

            } while (true);
            System.out.println();

            System.out.print("el2 read (" + el2.getMimeType() + ") : ");

            in = el2.getStream();

            do {
                int aChar = in.read();

                if (-1 == aChar) {
                    break;
                }

                System.out.print("0x" + Integer.toHexString(aChar) + " ");

            } while (true);
            System.out.println();

            System.out.print("el3 read (" + el3.getMimeType() + ") : ");

            in = el3.getStream();

            do {
                int aChar = in.read();

                if (-1 == aChar) {
                    break;
                }

                System.out.print("0x" + Integer.toHexString(aChar) + " ");

            } while (true);
            System.out.println();

            System.out.print("el4 read (" + el4.getMimeType() + ") : ");

            in = el4.getStream();

            do {
                int aChar = in.read();

                if (-1 == aChar) {
                    break;
                }

                System.out.print("0x" + Integer.toHexString(aChar) + " ");

            } while (true);
            System.out.println();

        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
}
