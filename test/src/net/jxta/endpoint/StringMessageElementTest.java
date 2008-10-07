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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.*;

import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

/**
 *
 * @author mike
 */
public class StringMessageElementTest extends TestCase {

    static final CharSequence one = "happy\u0221";
    static final String two = "happy\u0222";
    static final StringBuffer three = new StringBuffer("happy\u0223");
    static final StringBuilder four = new StringBuilder("happy\u0224");
    static final CharBuffer five = CharBuffer.wrap(one);    // message with UTF-8 encoding

    static class ElementTestCase {

        final CharSequence source;
        final TextMessageElement element;

        ElementTestCase(CharSequence source, TextMessageElement element) {
            this.source = source;
            this.element = element;
        }
    }
    final List<ElementTestCase> testElements = new ArrayList<ElementTestCase>();

    public StringMessageElementTest(java.lang.String testName) {
        super(testName);

        try {
            testElements.add(new ElementTestCase(one, new StringMessageElement("element1", one, (MessageElement) null)));
            testElements.add(new ElementTestCase(two, new StringMessageElement("element2", two, "UTF-16", (MessageElement) null)));
            testElements.add(new ElementTestCase(three, new StringMessageElement("element3", three, null, (MessageElement) null)));
            testElements.add(new ElementTestCase(four, new StringMessageElement("element4", four, null, (MessageElement) null)));
            testElements.add(new ElementTestCase(five, new StringMessageElement("element5", five, null, (MessageElement) null)));
        } catch (IOException failed) {
            throw new IllegalStateException("Could not create elements", failed);
        }
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
            for (ElementTestCase elem : testElements) {
                String sourceString = elem.source.toString();
                String elemString = elem.element.toString();

                assertEquals(sourceString, elemString);

                assertTrue(sourceString.length() == elem.element.getCharLength());

                assertTrue(elem.element.getByteLength() == elemString.getBytes(elem.element.getMimeType().getParameter("charset")).length);
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testEquals() {
        try {
            for (ElementTestCase elem : testElements) {
                TextMessageElement newElem = new StringMessageElement(elem.element.getElementName(), elem.source.toString(), elem.element.getMimeType().getParameter("charset"), null);
                
                assertEquals(elem.element, newElem);

                assertEquals(newElem, elem.element);
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testGetBytes() {
        try {
            for (ElementTestCase elem : testElements) {
                byte sourceBytes[] = elem.source.toString().getBytes(elem.element.getMimeType().getParameter("charset"));
                byte elemBytes[] = elem.element.getBytes(false);

                this.assertTrue("values not equal", Arrays.equals(sourceBytes, elemBytes));

                elemBytes = elem.element.getBytes(true);

                this.assertTrue("values not equal", Arrays.equals(sourceBytes, elemBytes));
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testInputStream() {
        try {
            for (ElementTestCase elem : testElements) {
                byte sourceBytes[] = elem.source.toString().getBytes(elem.element.getMimeType().getParameter("charset"));
                InputStream is = elem.element.getStream();
                DataInput dis = new DataInputStream(is);
                byte streamBytes[] = new byte[sourceBytes.length];

                dis.readFully(streamBytes);
                try {
                    dis.readByte();
                    fail("Not at EOF");
                } catch (EOFException atEOF) {
                }

                this.assertTrue("values not equal", Arrays.equals(sourceBytes, streamBytes));
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testReader() {
        try {
            for (ElementTestCase elem : testElements) {
                char sourceChars[] = elem.source.toString().toCharArray();
                Reader reader = new BufferedReader(elem.element.getReader());
                char readerChars[] = new char[sourceChars.length];

                assertEquals("Incorrect Number of chars", reader.read(readerChars), sourceChars.length);

                assertEquals("Not at EOF", -1, reader.read());

                assertTrue("values not equal", Arrays.equals(sourceChars, readerChars));
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testGetChars() {
        try {
            for (ElementTestCase elem : testElements) {
                char sourceChars[] = elem.source.toString().toCharArray();
                char elemChars[] = elem.element.getChars(false);

                this.assertTrue("values not equal", Arrays.equals(sourceChars, elemChars));

                elemChars = elem.element.getChars(true);

                assertTrue("values not equal", Arrays.equals(sourceChars, elemChars));
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testSendToStream() {
        try {
            for (ElementTestCase elem : testElements) {
                byte sourceBytes[] = elem.source.toString().getBytes(elem.element.getMimeType().getParameter("charset"));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                elem.element.sendToStream(baos);
                baos.close();

                assertTrue("values not equal", Arrays.equals(sourceBytes, baos.toByteArray()));
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }

    public void testSendToWriter() {
        try {
            for (ElementTestCase elem : testElements) {
                StringWriter sw = new StringWriter();

                elem.element.sendToWriter(sw);
                sw.close();

                this.assertEquals("values not equal", elem.source.toString(), sw.toString());
            }
        } catch (Exception caught) {
            caught.printStackTrace();
            fail("exception thrown : " + caught.getMessage());
        }
    }
}
