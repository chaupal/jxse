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


import java.io.InputStream;
import junit.framework.*;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.ByteArrayMessageElement;


/**
 *
 * @author mike
 */
public class ByteArrayMessageElementTest extends TestCase {
  
    public ByteArrayMessageElementTest(java.lang.String testName) {
        super(testName);
    }
  
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
  
    public static Test suite() {
        TestSuite suite = new TestSuite(ByteArrayMessageElementTest.class);

        return suite;
    }
  
    public void testEmptyArray() {
        try {
            byte[] source1 = {};
      
            // default blob, default mime type
            ByteArrayMessageElement el1 = new ByteArrayMessageElement("element1", null, source1, (MessageElement) null);
      
            // getdatasize

            assertEquals(source1.length, el1.getByteLength());

            // getBytes.
      
            byte[] gotBytes = el1.getBytes();

            System.out.print("el1 (" + gotBytes.length + ") : ");
            for (int eachByte = 0; eachByte < gotBytes.length; eachByte++) {
                System.out.print("0x" + Integer.toHexString(gotBytes[eachByte] & 0x0FF) + " ");
            }
      
            System.out.println();

            // sendDataStream

            System.out.print("el1 send (" + el1.getMimeType() + ") : "); 
      
            el1.sendToStream(System.out);
      
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

        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
  
    public void testFullArray() {
        try {
            byte[] source1 = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48 };
      
            // default blob, default mime type
            ByteArrayMessageElement el1 = new ByteArrayMessageElement("element1", null, source1, (MessageElement) null);
      
            // getdatasize

            assertEquals(source1.length, el1.getByteLength());

            // getBytes.
      
            byte[] gotBytes = el1.getBytes();

            System.out.print("el1 (" + gotBytes.length + ") : ");
            for (int eachByte = 0; eachByte < gotBytes.length; eachByte++) {
                System.out.print("0x" + Integer.toHexString(gotBytes[eachByte] & 0x0FF) + " ");
            }
      
            System.out.println();

            // sendDataStream

            System.out.print("el1 send (" + el1.getMimeType() + ") : "); 
      
            el1.sendToStream(System.out);
      
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

        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
  
    public void testSubsetArray() {
        try {
            byte[] source1 = { 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68 };
      
            // subset blob, binary mime type
            ByteArrayMessageElement el1 = new ByteArrayMessageElement("element1", new MimeMediaType("image/jpeg"), source1, 1, 6
                    ,
                    (MessageElement) null);
      
            // getdatasize

            assertEquals(6, el1.getByteLength());

            // getBytes.
      
            byte[] gotBytes = el1.getBytes();

            System.out.print("el1 (" + gotBytes.length + ") : ");
            for (int eachByte = 0; eachByte < gotBytes.length; eachByte++) {
                System.out.print("0x" + Integer.toHexString(gotBytes[eachByte] & 0x0FF) + " ");
            }
      
            System.out.println();

            // sendDataStream

            System.out.print("el1 send (" + el1.getMimeType() + ") : "); 
      
            el1.sendToStream(System.out);
      
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

        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
}
