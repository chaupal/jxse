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

package net.jxta.document;


import java.io.*;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.security.ProviderException;

import net.jxta.impl.document.LiteXMLDocument;
import net.jxta.impl.document.PlainTextDocument;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;


public final class DocumentTest {
       
    final static String badlittleimpl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE jxta:MIA>\n"
            + "<jxta:MIA xmlns:jxta=\"http://jxta.org\">\n" + "  	<MSID/>\n" + " 	<Parm>\n" + " 		<Svc>\n"
            + " 			<jxta:MIA xmlns:jxta=\"http://jxta.org\">\n" + " 				<MSID/>\n" + " 			</jxta:MIA>\n" + " 		</Svc>\n"
            + " 		<Proto>\n" + " 			<jxta:MIA xmlns:jxta=\"http://jxta.org\">\n" + " 				<MSID/>\n" + " 			</jxta:MIA>\n"
            + " 		</Proto>\n" + " 	</Parm>\n" + " </jxta:MIA>\n";
    
    final static String badInclusion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE jxta:CP>"
            + "<jxta:CP type=\"jxta:PlatformConfig\" xmlns:jxta=\"http://jxta.org\">"
            + "	<InfraDesc>"
            + "		&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "&lt;!DOCTYPE InfraDesc>"
            + "&lt;InfraDesc>"
            + "	Default Infrastructure NetPeerGroup Created by net.jxta.platform.NetworkConfigurator"
            + "&lt;/InfraDesc>"
            + "	</InfraDesc>"
            + "</jxta:CP>";
    
    public static class LiteXMLBug {
        public LiteXMLBug(String xml) {
            MimeMediaType mimeMediaType = MimeMediaType.XMLUTF8;
            StringReader reader = new StringReader(xml);

            try {
                LiteXMLDocument document = (LiteXMLDocument)
                        LiteXMLDocument.INSTANTIATOR.newInstance(mimeMediaType, reader);

                spew(document);
            } catch (IOException caught) {
                fail("should not have thrown an exception");
            } finally {
                reader.close();
            }
        }
        
        public void spew(XMLElement element) {
            System.out.println(element.getValue());
            
            Enumeration<XMLElement> children = element.getChildren();

            while (children.hasMoreElements()) {
                XMLElement child = children.nextElement();

                spew(child);
            }
        }        
    }
    
    private void _test(StructuredDocumentFactory.Instantiator instantiator, MimeMediaType type) {
        try {
            final String useDocType = "Test";
            StructuredTextDocument doc = null;

            try {
                doc = (StructuredTextDocument) instantiator.newInstance(type, useDocType);
            } catch (Throwable thrown) {
                thrown.printStackTrace(System.err);
                fail("exception thrown during construction!" + thrown.toString());
            }
            
            assertTrue("could not construct object for type : " + type, doc != null);
            
            String itsType = doc.getName();
            
            assertTrue("returned doctype does not equal type document was created with!", useDocType.equals(itsType));
            
            assertTrue("returned doc name does not equal name of document element", doc.getName().equals(itsType));
            
            TextElement testElement = doc.createElement("element");
            
            doc.appendChild(testElement);
            
            try {
                Element firstchild = (Element) doc.getChildren().nextElement();

                assertTrue("added a single element, but something else was returned", testElement.equals(firstchild));
            } catch (Exception e) {
                e.printStackTrace();
                fail("added a single element, but it was not returned");
            }
            
            final String useName = "element2";
            final String useValue = "value&<!";
            
            TextElement testElement2 = doc.createElement(useName, useValue);
            
            testElement.appendChild(testElement2);
            
            String itsName = testElement2.getName();

            assertTrue("name of element was not correct after creation", useName.equals(itsName));
            
            String itsValue = testElement2.getTextValue();

            assertTrue("value of element was not correct after creation. was '" + itsValue + "' should be '" + useValue + "'"
                    ,
                    useValue.equals(itsValue));
            
            testElement2 = doc.createElement("element3", useValue);
            
            testElement.appendChild(testElement2);
            
            testElement2 = doc.createElement("element4", "1");
            
            testElement.appendChild(testElement2);
            
            itsValue = testElement2.getTextValue();
            assertTrue("value of element was not correct after creation (length 1)", "1".equals(itsValue));
            
            if (type.getSubtype().equalsIgnoreCase("XML")) {
                try {
                    TextElement testElement5 = doc.createElement("really wrong and long", "1");
                    
                    fail("Tag names with spaces should be disallowed");
                } catch (Exception failed) {// that's ok
                }
            }
            
            int count = 0;

            for (Enumeration<Element> eachChild = doc.getChildren(); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue("Doc didnt have one child", 1 == count);
            
            count = 0;
            for (Enumeration<Element> eachChild = doc.getChildren("element"); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue("Doc didnt have one child named 'element'", 1 == count);
            
            count = 0;
            for (Enumeration<Element> eachChild = doc.getChildren("bogus"); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue(" Doc shouldnt have had a child named 'bogus'", 0 == count);
            
            count = 0;
            for (Enumeration<Element> eachChild = testElement.getChildren(); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue("element didnt have expected number of children", 3 == count);
            
            count = 0;
            for (Enumeration<Element> eachChild = testElement.getChildren(useName); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue("element didnt have expected number of children named '" + useName + "'", 1 == count);
            
            // This check also is important for checking that the behaviour of the
            // tree is correct when there are nodes with the same name as the parent in subtrees.
            
            Element testElement3 = doc.createElement(useName, useValue);

            testElement2.appendChild(testElement3);
            
            testElement3 = doc.createElement(useName);
            testElement2.appendChild(testElement3);
            
            count = 0;
            for (Enumeration eachChild = testElement2.getChildren(useName); eachChild.hasMoreElements(); count++, eachChild.nextElement()) {
                ;
            }
            
            assertTrue("element didnt have expected number of children named '" + useName + "'", 2 == count);
            
            StructuredDocument likeMe = null;
            
            try {
                likeMe = (StructuredTextDocument) instantiator.newInstance(doc.getMimeType(), doc.getStream());
            } catch (java.security.ProviderException thrown) {
                ;
            } catch (Throwable thrown) {
                thrown.printStackTrace();
                fail("Exception thrown during reconstruction! " + thrown.toString());
            }
            
            if (testElement instanceof Attributable) {
                _testAttributes((Attributable) testElement);
                _testAttributes((Attributable) testElement3);
            }
            
            try {
                likeMe = instantiator.newInstance(doc.getMimeType(), doc.getStream());
            } catch (java.security.ProviderException thrown) {
                ;
            } catch (Throwable thrown) {
                thrown.printStackTrace();
                fail("Exception thrown during reconstruction! " + thrown.toString());
            }
            
            Writer somewhere = new StringWriter();
            
            (doc).sendToWriter(somewhere);
            
            String docAsString = somewhere.toString().trim();
            
            testElement3 = doc.createElement(useName, docAsString);
            testElement2.appendChild(testElement3);
            
            String docFromElement = (String) testElement3.getValue();
            
            assertTrue("Could not faithfully store stream representation of doc in doc. (lengths dont match)",
                    docAsString.length() == docFromElement.length());
            
            for (int eachChar = 0; eachChar < docAsString.length(); eachChar++) {
                assertTrue("Could not faithfully store stream representation of doc in doc. (failed at index: " + eachChar + ")",
                        docAsString.charAt(eachChar) == docFromElement.charAt(eachChar));
            }
            
            Element testElement4 = doc.createElement("shortname", "shortvalue");
            Element testElement5 = doc.createElement(
                    "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongname",
                    "shortvalue");
            Element testElement6 = doc.createElement(
                    "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongname",
                    "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongvalue");
            Element testElement7 = doc.createElement("shortname",
                    "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongvalue");
            
            doc.appendChild(testElement4);
            doc.appendChild(testElement5);
            doc.appendChild(testElement6);
            doc.appendChild(testElement7);
            
            System.out.println(testElement4.toString());
            // System.out.println( testElement5.toString() );
            // System.out.println( testElement6.toString() );
            // System.out.println( testElement7.toString() );
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("caught an unexpected exception - " + everything.toString());
        }
    }
    
    public void _testAttributes(Attributable element) {
        try {
            final String someName = "attrName";
            final String someValue = "attrValue";
            
            Enumeration<Attribute> attribs = element.getAttributes();
            
            assertTrue("Element already had attributes!", !attribs.hasMoreElements());
            
            assertTrue("New attribute returned previous value!", null == element.addAttribute(someName, someValue));
            
            String oldValue = element.addAttribute(new Attribute(someName, someValue));
            
            assertTrue("New attribute didnt return previous value!", (null != oldValue) && (oldValue.equals(someValue)));
            
            Attribute anAttrib = element.getAttribute(someName);
            
            assertTrue("Could not get attribute back!", null != anAttrib);
            
            assertTrue("value of attribute was not correct", anAttrib.getValue().equals(someValue));
            
            anAttrib = element.getAttribute("bogusName");
            
            assertTrue("Should not have been able to get an unknown attribute name", null == anAttrib);            
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    private void _testConstructors(StructuredDocumentFactory.Instantiator instantiator, MimeMediaType type) {
        try {
            final String useDocType = "Test";
            StructuredTextDocument doc = null;
            
            doc = (StructuredTextDocument) instantiator.newInstance(type, useDocType);
            
            doc = (StructuredTextDocument) instantiator.newInstance(type, useDocType, null);
            
            doc = (StructuredTextDocument) instantiator.newInstance(type, useDocType, "value");
            
            String stringdoc = doc.toString();
            
            if (type.getSubtype().equalsIgnoreCase("XML")) {
                try {
                    
                    doc = (StructuredTextDocument) instantiator.newInstance(type, "Really wrong and long");
                    
                    fail("Tag names with spaces should be disallowed");
                } catch (Exception failed) {// that's ok
                }
            }
            
            try {
                doc = (StructuredTextDocument) instantiator.newInstance(type, new ByteArrayInputStream(stringdoc.getBytes()));
            } catch (ProviderException notsupported) {// thats ok.
            }
            
            if (instantiator instanceof StructuredDocumentFactory.TextInstantiator) {
                try {
                    doc = (StructuredTextDocument) ((StructuredDocumentFactory.TextInstantiator) instantiator).newInstance(type
                            ,
                            new StringReader(stringdoc));
                } catch (ProviderException notsupported) {// thats ok.
                }
            }
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    public void _testAttributesSolo(StructuredDocumentFactory.Instantiator instantiator, MimeMediaType type) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            StructuredDocument doc = instantiator.newInstance(type, "Message");
            
            assertTrue("should be no attributes", !((Attributable) doc).getAttributes().hasMoreElements());
            
            ((Attributable) doc).addAttribute("version", "123");
            doc.sendToStream(os);
            
            String old = ((Attributable) doc).addAttribute("version", "1xx23");

            assertTrue("updating attribute gave wrong result", "123".equals(old));
            doc.sendToStream(os);
            
            List attrs = Collections.list(((Attributable) doc).getAttributes());
            
            assertTrue("should be 1 attribute", 1 == attrs.size());
            
            if (type.getSubtype().equalsIgnoreCase("XML")) {
                try {
                    
                    ((Attributable) doc).addAttribute(new Attribute("really long and wrong", "whatever"));
                    
                    fail("Attribute names with spaces should be disallowed");
                } catch (Exception failed) {// that's ok
                }
            }
            
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    public void _testLiteXMLEmptyElement(StructuredDocumentFactory.TextInstantiator instantiator, MimeMediaType type) {
        try {
            String doc = "<?xml version=\"1.0\"?><whatever/>";
            
            XMLDocument xmldoc = (XMLDocument) instantiator.newInstance(type, new StringReader(doc));
            Element anElement = xmldoc.createElement("whynot");
            
            xmldoc.appendChild(anElement);
            
            XMLElement anotherElement = xmldoc.createElement("why", "because");
            
            anElement.appendChild(anotherElement);
            
            System.out.println(xmldoc.toString());
            
            StringWriter writer = new StringWriter();
            
            xmldoc.sendToWriter(writer);
            
            StringReader reader = new StringReader(writer.toString());
            
            XMLDocument roundtrip = (XMLDocument) instantiator.newInstance(xmldoc.getMimeType(), reader);
            
            System.out.println(roundtrip.toString());
            
            StringWriter secondroundwriter = new StringWriter();
            
            roundtrip.sendToWriter(secondroundwriter);
            
            StringReader secondroundreader = new StringReader(secondroundwriter.toString());
            
            XMLDocument secondroundtrip = (XMLDocument) instantiator.newInstance(roundtrip.getMimeType(), secondroundreader);
            
            System.out.println(secondroundtrip.toString());
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    @Test public void testLiteXMLStructuredDoc() {
        try {
            _test(LiteXMLDocument.INSTANTIATOR, MimeMediaType.XML_DEFAULTENCODING);
            _testConstructors(LiteXMLDocument.INSTANTIATOR, MimeMediaType.XML_DEFAULTENCODING);
            _testAttributesSolo(LiteXMLDocument.INSTANTIATOR, MimeMediaType.XML_DEFAULTENCODING);
            _testLiteXMLEmptyElement(LiteXMLDocument.INSTANTIATOR, MimeMediaType.XML_DEFAULTENCODING);
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    @Ignore("To be investigated")
    @Test
    public void testDOMXMLStructuredDoc() {
        StructuredDocumentFactory.Instantiator domInstantiator = null;

        try {
            domInstantiator = (StructuredDocumentFactory.Instantiator) Class.forName("net.jxta.impl.document.DOMXMLDocument").getField("INSTANTIATOR").get(
                    null);
        } catch (ClassNotFoundException noDOM) {
            ;
        } catch (NoSuchFieldException noDOM) {
            ;
        } catch (IllegalAccessException noDOM) {
            ;
        }

        try {
            if (null != domInstantiator) {
                _test(domInstantiator, MimeMediaType.XML_DEFAULTENCODING);
                _testConstructors(domInstantiator, MimeMediaType.XML_DEFAULTENCODING);
                _testAttributesSolo(domInstantiator, MimeMediaType.XML_DEFAULTENCODING);
            }
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    @Test public void testPlainTextDoc() {
        try {
            _test(PlainTextDocument.INSTANTIATOR, MimeMediaType.TEXT_DEFAULTENCODING);
            _testConstructors(PlainTextDocument.INSTANTIATOR, MimeMediaType.TEXT_DEFAULTENCODING);
            _testAttributesSolo(PlainTextDocument.INSTANTIATOR, MimeMediaType.TEXT_DEFAULTENCODING);
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.toString());
        }
    }
    
    @Test public void testExtensionMapping() {
        MimeMediaType refMime = new MimeMediaType("Text/Xml");
        String refExt = "xml";
        
        String ext = StructuredDocumentFactory.getFileExtensionForMimeType(refMime);
        
        MimeMediaType mime = StructuredDocumentFactory.getMimeTypeForFileExtension(ext);
        
        assertTrue("mime type was not the same after reflex mapping", refMime.equals(mime));
        
        assertTrue("extension was not the same after reflex mapping", refExt.equals(ext));
    }
    
    @Test public void testIssue102() {
        String WORKS = "<xml><stooges>Moe, Larry, &#x41;&#65;&#0666;& Curly</stooges></xml>";
        
        String DOES_NOT_WORK = "<xml><stooges>Moe, Larry, & Joe</stooges></xml>";
        
        LiteXMLBug works = new LiteXMLBug(WORKS);
        LiteXMLBug doesNotWork = new LiteXMLBug(DOES_NOT_WORK);
    }
    
    @Test public void testIssue1282() {
        
        try {
            // create document
            MimeMediaType mime = new MimeMediaType("text/xml");
            XMLDocument document = (XMLDocument) LiteXMLDocument.INSTANTIATOR.newInstance(mime, "items");
            
            for (int i = 0; i < 10; i++) {
                XMLElement testElem = document.createElement("item");

                document.appendChild(testElem);
                testElem.addAttribute("name", "n" + i);
                if (i == 3) {
                    for (int j = 0; j < 2; j++) {
                        XMLElement childElem = document.createElement("item");

                        testElem.appendChild(childElem);
                        childElem.addAttribute("name", "ch" + j);
                    }
                }
            }
            
            // Serialize the message
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            document.sendToStream(out);
            
            InputStream is = new ByteArrayInputStream(out.toByteArray());
            
            XMLDocument document2 = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(mime, is);
            
            Enumeration<XMLElement> eachOrig = document.getChildren();
            Enumeration<XMLElement> eachNew = document2.getChildren();
            
            while (eachOrig.hasMoreElements()) {
                if (!eachNew.hasMoreElements()) {
                    fail("Enumeration did not end at same time.");
                }
                
                XMLElement anOrig = eachOrig.nextElement();
                XMLElement aNew = eachNew.nextElement();
                
                assertEquals("Elements names should be equivalent", aNew.getKey(), anOrig.getKey());
                assertEquals("Elements values should be equivalent", aNew.getValue(), anOrig.getValue());
                
                Attribute anOrigName = anOrig.getAttribute("name");
                Attribute aNewName = aNew.getAttribute("name");
                
                assertEquals("Element attribute name should be equivalent", anOrigName.getValue(), aNewName.getValue());
            }
            
            if (eachNew.hasMoreElements()) {
                fail("Enumeration did not end at same time.");
            }
        } catch (Throwable everything) {
            everything.printStackTrace();
            fail("Caught an unexpected exception - " + everything.getMessage());
        }
    }
    
    @Test public void testIssue1372() {
        XMLDocument document = null;
        XMLDocument document2 = null;
        
        try {
            for (int depth = 1; depth <= 10; depth++) {
                // create document
                document = (XMLDocument) LiteXMLDocument.INSTANTIATOR.newInstance(MimeMediaType.XML_DEFAULTENCODING, "items");
                for (int elemCount = 1; elemCount <= 2; elemCount++) {
                    XMLElement parentElem = document;

                    for (int layer = 1; layer <= depth; layer++) {
                        XMLElement testElem = document.createElement("item");

                        parentElem.appendChild(testElem);
                        testElem.addAttribute("name", depth + "-" + elemCount + ":" + layer);
                        parentElem = testElem;
                    }
                }
                // Serialize the message
                StringWriter out = new StringWriter();

                document.sendToWriter(out);
                Reader is = new StringReader(out.toString());

                document2 = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XML_DEFAULTENCODING, is);
                Enumeration<Element> eachOrig = document.getChildren();
                Enumeration<Element> eachNew = document2.getChildren();

                // FIXME 20050607 bondolo comparison doesn't recurse.
                while (eachOrig.hasMoreElements()) {
                    if (!eachNew.hasMoreElements()) {
                        fail("Enumeration did not end at same time.");
                    }
                    XMLElement anOrig = (XMLElement) eachOrig.nextElement();
                    XMLElement aNew = (XMLElement) eachNew.nextElement();

                    assertEquals("Elements names should be equivalent", aNew.getKey(), anOrig.getKey());
                    assertEquals("Elements values should be equivalent", aNew.getValue(), anOrig.getValue());
                    Attribute anOrigName = anOrig.getAttribute("name");
                    Attribute aNewName = aNew.getAttribute("name");

                    assertEquals("Element attribute name should be equivalent", anOrigName.getValue(), aNewName.getValue());
                }
                if (eachNew.hasMoreElements()) {
                    fail("Enumeration did not end at same time.");
                }
            }
        } catch (Throwable everything) {
            System.err.flush();
            System.out.flush();
            everything.printStackTrace(System.err);
            fail("Caught an unexpected exception - " + everything.getMessage());
        }
    }
    
    @Test public void testIssue13XX() {
        XMLDocument document = null;
        
        try {
            Reader is = new StringReader(badlittleimpl);

            document = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XML_DEFAULTENCODING, is);

            System.err.println(document);            
        } catch (Throwable everything) {
            System.err.flush();
            System.out.flush();
            everything.printStackTrace(System.err);
            fail("Caught an unexpected exception - " + everything.getMessage());
        }
    }
    
    
    @Test public void testIssue15() {
        XMLDocument document = null;
        
        try {
            Reader is = new StringReader(badInclusion);

            document = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XML_DEFAULTENCODING, is);

            System.err.println(document);            
        } catch (Throwable everything) {
            System.err.flush();
            System.out.flush();
            everything.printStackTrace(System.err);
            fail("Caught an unexpected exception - " + everything.getMessage());
        }
    }

}
