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
import java.io.ByteArrayOutputStream;
import junit.framework.*;

import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;


/**
 *
 * @author mike
 */
public class StringMessageElementTest extends TestCase {
    
    static final String one = "happy\u022a";
    static final String two = "happy\u0229";
    static final String three = "happy\u0229";
    
    public StringMessageElementTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        
        System.err.flush();
        System.out.flush();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(StringMessageElementTest.class);

        return suite;
    }
    
    public void testAccessors() {
        try {
            // message with UTF-8 encoding
            TextMessageElement el1 = new StringMessageElement("element1", one, (MessageElement) null);
            
            // message with UTF-16 encoding
            TextMessageElement el2 = new StringMessageElement("element2", two, "UTF-16", (MessageElement) null);
            
            // message with system encoding
            TextMessageElement el3 = new StringMessageElement(null, three, null, (MessageElement) null);
            
            // toString.
            assertTrue(one.equals(el1.toString()));
            
            assertTrue(two.equals(el2.toString()));
            
            assertTrue(three.equals(el3.toString()));
            
            // charLength
            assertTrue(one.length() == el1.getCharLength());
            
            assertTrue(two.length() == el2.getCharLength());
            
            assertTrue(three.length() == el3.getCharLength());
            
            // getByteLength
            assertTrue(el1.getByteLength() == el1.getBytes(false).length);
            
            assertTrue(el2.getByteLength() == el2.getBytes(false).length);
            
            assertTrue(el3.getByteLength() == el3.getBytes(false).length);
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
    
    public void testStreams() {
        try {
            // message with UTF-8 encoding
            TextMessageElement el1 = new StringMessageElement("element1", one, (MessageElement) null);
            
            // message with UTF-16 encoding
            TextMessageElement el2 = new StringMessageElement("element2", two, "UTF-16", (MessageElement) null);
            
            // message with system encoding
            TextMessageElement el3 = new StringMessageElement(null, three, null, (MessageElement) null);
            
            // sendDataStream
            
            System.out.print("el1 send (" + el1.getElementName() + "," + el1.getMimeType() + ") : ");
            ByteArrayOutputStream el1stream = new ByteArrayOutputStream();
            
            el1.sendToStream(el1stream);
            
            el1.sendToStream(System.out);
            
            System.out.println();
            
            System.out.print("el2 send (" + el2.getElementName() + "," + el2.getMimeType() + ") : ");
            ByteArrayOutputStream el2stream = new ByteArrayOutputStream();
            
            el2.sendToStream(el2stream);
            
            el2.sendToStream(System.out);
            
            System.out.println();
            
            System.out.print("el3 send (" + el3.getElementName() + "," + el3.getMimeType() + ") : ");
            ByteArrayOutputStream el3stream = new ByteArrayOutputStream();
            
            el3.sendToStream(el3stream);
            
            el3.sendToStream(System.out);
            
            System.out.println();
            
            System.out.print("el1 read (" + el1.getElementName() + "," + el1.getMimeType() + ") : ");
            
            // getDataStream
            
            InputStream in = el1.getStream();
            byte outputbytes[] = el1stream.toByteArray();
            int index = 0;
            
            do {
                int aChar = in.read();
                
                if (-1 == aChar) {
                    break;
                }
                
                assertTrue("characters did not match at index " + index, aChar == (outputbytes[index++] & 0xFF));
                
                System.out.print("0x" + Integer.toHexString(aChar) + " ");
                
            } while (true);
            
            assertTrue(index == outputbytes.length);
            
            System.out.println();
            
            System.out.print("el2 read (" + el2.getElementName() + "," + el2.getMimeType() + ") : ");
            
            in = el2.getStream();
            outputbytes = el2stream.toByteArray();
            index = 0;
            
            do {
                int aChar = in.read();
                
                if (-1 == aChar) {
                    break;
                }
                
                assertTrue("characters did not match at index " + index, aChar == (outputbytes[index++] & 0xFF));
                
                System.out.print("0x" + Integer.toHexString(aChar) + " ");
                
            } while (true);
            
            assertTrue(index == outputbytes.length);
            
            System.out.println();
            
            System.out.print("el3 read (" + el3.getElementName() + "," + el3.getMimeType() + ") : ");
            
            in = el3.getStream();
            outputbytes = el3stream.toByteArray();
            index = 0;
            
            do {
                int aChar = in.read();
                
                if (-1 == aChar) {
                    break;
                }
                
                assertTrue("characters did not match at index " + index, aChar == (outputbytes[index++] & 0xFF));
                
                System.out.print("0x" + Integer.toHexString(aChar) + " ");
                
            } while (true);
            
            assertTrue(index == outputbytes.length);
            
            System.out.println();
            
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
}
