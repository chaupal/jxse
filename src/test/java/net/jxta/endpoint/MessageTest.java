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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.ConcurrentModificationException;

import junit.framework.*;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

/**
 *
 * @author mike
 */
public class MessageTest extends TestCase {

    public MessageTest(java.lang.String testName) {
        super(testName);
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());

        System.err.flush();
        System.out.flush();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(MessageTest.class);

        return suite;
    }

    /**
     *   Tests Default Namespace
     **/
    public void testMessageDefaultNamespaces() {
        Message msg1 = new Message();
        Message msg2 = new Message("default");

        assertTrue(!msg1.getDefaultNamespace().equals(msg2.getDefaultNamespace()));

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element1", "test2", null);

        msg1.addMessageElement(elm1);
        msg2.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm2);
        msg2.addMessageElement("not-default", elm2);

        assertTrue("Messages should not be equal", !msg1.equals(msg2));
    }

    /**
     *   Tests Simple AddElement
     **/
    public void testMessageAddGetElement() {
        Message msg1 = new Message();
        List    list = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element1", "test2", null);

        int initialModCount = msg1.getMessageModCount();

        msg1.addMessageElement(elm1);
        int firstElementModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", initialModCount != firstElementModCount);

        msg1.addMessageElement("not-default", elm2);
        int secondElementModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", firstElementModCount != secondElementModCount);

        assertTrue("message should have had an element1 in default namespace", elm1 == msg1.getMessageElement(null, "element1"));
        assertTrue("message should have had an element1 in non-default namespace"
                ,
                elm2 == msg1.getMessageElement("not-default", "element1"));
        assertTrue("message should not have had an element bogus", null == msg1.getMessageElement("bogus"));
        assertTrue("message should not have had an element bogus", null == msg1.getMessageElement(null, "bogus"));
        assertTrue("message should not have had an element bogus", null == msg1.getMessageElement("not-default", "bogus"));
    }

    /**
     *    tests the clear method
     **/
    public void testMessageClear() {
        Message msg1 = new Message();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);

        int initialModCount = msg1.getMessageModCount();

        msg1.addMessageElement(elm1);

        int afterAddModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", initialModCount != afterAddModCount);

        Iterator elements = msg1.getMessageElements();

        assertTrue("message should have had an element", elements.hasNext());

        msg1.clear();

        int afterClearModCount = msg1.getMessageModCount();

        assertTrue("cleared, modcounts should differ", afterAddModCount != afterClearModCount);

        elements = msg1.getMessageElements();

        assertTrue("message should not have had an element", !elements.hasNext());
    }

    /**
     *    tests the getMessageElements iterator
     **/
    public void testMessageGetMessageElements() {
        Message msg1 = new Message();
        List    list = new ArrayList();
        List    namespaces = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        list.add(elm1);
        namespaces.add("");
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        namespaces.add("not-default");
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);
        namespaces.add("not-default");
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);
        namespaces.add("not-default");

        // check if all of the elements are returned
        Message.ElementIterator eachElement = msg1.getMessageElements();
        Iterator eachListElement = list.iterator();
        Iterator eachNamesElement = namespaces.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("namespaces should have matched", eachElement.getNamespace().equals(eachNamesElement.next()));
            assertTrue("should be the same element", aElement == aListElement);
            assertTrue("elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    /**
     *    tests the getMessageElements( String ) iterator
     **/
    public void testMessageGetMessageElementsNamed() {
        Message msg1 = new Message();
        List    list = new ArrayList();
        List    namespaces = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        list.add(elm1);
        namespaces.add("");
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        namespaces.add("not-default");

        // check if all of the elements are returned
        Message.ElementIterator eachElement = msg1.getMessageElements("element1");
        Iterator eachListElement = list.iterator();
        Iterator eachNamesElement = namespaces.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("elements should have the same name", "element1".equals(aElement.getElementName()));
            assertTrue("elements should be in the same namespace", eachElement.getNamespace().equals(eachNamesElement.next()));
            assertTrue("should be the same element", aElement == aListElement);
            assertTrue("elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    /**
     *    tests the getMessageElementsOfNamespace iterator
     **/
    public void testMessagegeGetMessageElementsOfNamespace() {
        Message msg1 = new Message();
        List    list = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);

        // check if all of the elements of the namespace "not-default" are returned.
        Message.ElementIterator eachElement = msg1.getMessageElementsOfNamespace("not-default");
        Iterator eachListElement = list.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("elements should be in the same namespace", eachElement.getNamespace().equals("not-default"));
            assertTrue("should be the same element", aElement == aListElement);
            assertTrue("elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    /**
     *    tests the getMessageElements Mimetype iterator
     **/
    public void testMessagegeGetMessageElementsMimeType() {
        Message msg1 = new Message();
        List    list = new ArrayList();
        MimeMediaType foo = MimeMediaType.valueOf("text/foo");
        MimeMediaType bar = MimeMediaType.valueOf("text/bar");

        MessageElement elm1 = new ByteArrayMessageElement("element1", foo, "test1".getBytes(), null);
        MessageElement elm2 = new ByteArrayMessageElement("element2", bar, "test2".getBytes(), null);

        msg1.addMessageElement(elm1);
        list.add(elm1);
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        msg1.addMessageElement("not-default", elm2);
        msg1.addMessageElement("not-default", elm2);

        // check if all of the elements matching type "foo" are returned.
        Message.ElementIterator eachElement = msg1.getMessageElements(foo);
        Iterator eachListElement = list.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();

            assertTrue("Should still be contents in the test vector.", eachListElement.hasNext());
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("Should be the correct mime type", aElement.getMimeType().equals(foo));
            assertTrue("Should be the same element", aElement == aListElement);
            assertTrue("Elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    /**
     *    tests the getMessageElements Mimetype iterator
     **/
    public void testMessagegeGetMessageElementsNamespaceMimeType() {
        Message msg1 = new Message();
        List    list = new ArrayList();
        MimeMediaType foo = MimeMediaType.valueOf("text/foo");
        MimeMediaType bar = MimeMediaType.valueOf("text/bar");

        MessageElement elm1 = new ByteArrayMessageElement("element1", foo, "test1".getBytes(), null);
        MessageElement elm2 = new ByteArrayMessageElement("element2", bar, "test2".getBytes(), null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        msg1.addMessageElement("not-default", elm2);
        msg1.addMessageElement("not-default", elm2);

        // check if all of the elements matching type "foo" are returned.
        Message.ElementIterator eachElement = msg1.getMessageElements("not-default", foo);
        Iterator eachListElement = list.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();

            assertTrue("Should still be contents in the test vector.", eachListElement.hasNext());
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("Should be the correct mime type", aElement.getMimeType().equals(foo));
            assertTrue("Should be the same element", aElement == aListElement);
            assertTrue("Elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    public void testMessageGetElement() {
        Message msg1 = new Message("default");

        MessageElement elm1 = new StringMessageElement("element1", "test1", (MessageElement) null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", (MessageElement) null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);

        msg1.addMessageElement("not-default", elm2);

        // try to get back an element by name
        MessageElement elm3 = msg1.getMessageElement("element1");

        assertSame("Did not get back the right element", elm3, elm1);

        // try to get back on in a non default name space
        elm3 = msg1.getMessageElement("element2");

        assertSame("Did not get back the right element", elm3, elm2);

        // get an element from a named namespace
        elm3 = msg1.getMessageElement("default", "element1");

        assertSame("Did not get back the right element", elm3, elm1);

        // get an element from a non-default named name space.
        elm3 = msg1.getMessageElement("not-default", "element1");

        assertSame("Did not get back the right element", elm3, elm1);

        // make sure we can't get an element from a namespace its not in.
        elm3 = msg1.getMessageElement("default", "element2");

        assertNull("Should not have gotten an element", elm3);
    }

    public void testMessageGetNamespaces() {
        Message msg1 = new Message("default");
        Set namespaces = new HashSet();

        MessageElement elm1 = new StringMessageElement("element1", "test1", (MessageElement) null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", (MessageElement) null);

        msg1.addMessageElement(elm1);
        namespaces.add("default");

        msg1.addMessageElement("not-default", elm2);
        namespaces.add("not-default");

        Iterator eachNamespace = msg1.getMessageNamespaces();

        while (eachNamespace.hasNext()) {
            String aNamespace = (String) eachNamespace.next();

            assertTrue(namespaces.remove(aNamespace));
        }

        assertTrue(namespaces.isEmpty());
    }

    /**
     *    tests the replace method
     **/
    public void testMessageReplacement() {
        Message msg1 = new Message();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element1", "test1", null);

        int initialModCount = msg1.getMessageModCount();

        msg1.addMessageElement(elm1);

        int afterAddModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", initialModCount != afterAddModCount);

        Iterator elements = msg1.getMessageElements();

        assertTrue("message should have had an element", elements.hasNext());

        assertTrue("should have replaced elem1", elm1 == msg1.replaceMessageElement(elm2));

        int afterReplaceModCount = msg1.getMessageModCount();

        assertTrue("replaced, modcounts should differ", afterAddModCount != afterReplaceModCount);

        elements = msg1.getMessageElements();

        assertTrue("message should have had an element", elements.hasNext());

        assertTrue("should have returned elm2", elm2 == elements.next());
    }

    /**
     *    tests the replace method
     **/
    public void testIteratorSet() {
        Message msg1 = new Message();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element1", "test1", null);

        int initialModCount = msg1.getMessageModCount();

        msg1.addMessageElement(elm1);

        int afterAddModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", initialModCount != afterAddModCount);

        Message.ElementIterator elements = msg1.getMessageElements();

        assertTrue("message should have had an element", elements.hasNext());

        assertTrue("should have returned elm1", elm1 == elements.next());

        elements.set(elm2);

        int afterReplaceModCount = msg1.getMessageModCount();

        assertTrue("replaced, modcounts should differ", afterAddModCount != afterReplaceModCount);

        elements = msg1.getMessageElements();

        assertTrue("message should have had an element", elements.hasNext());

        assertTrue("should have returned elm2", elm2 == elements.next());
    }

    /**
     *   Tests Message Element
     **/
    public void testMessageLength() {
        Message msg1 = new Message();
        List    list = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element1", "test23", null);

        assertTrue("Message should have had length 0", 0 == msg1.getByteLength());

        int initialModCount = msg1.getMessageModCount();

        msg1.addMessageElement(elm1);
        int firstElementModCount = msg1.getMessageModCount();

        long msgLen1 = msg1.getByteLength();

        assertTrue("Message should have had length > 0", msgLen1 > 0);
        assertTrue("element was added, modcounts should differ", initialModCount != firstElementModCount);

        msg1.addMessageElement("not-default", elm2);
        int secondElementModCount = msg1.getMessageModCount();

        assertTrue("element was added, modcounts should differ", firstElementModCount != secondElementModCount);
        long msgLen2 = msg1.getByteLength();

        assertTrue("Message length should have increased", msgLen2 > msgLen1);

        msg1.clear();

        int afterClearModCount = msg1.getMessageModCount();

        assertTrue("cleared, modcounts should differ", secondElementModCount != afterClearModCount);

        assertTrue("Message should have had length 0", 0 == msg1.getByteLength());
    }

    /**
     *   Tests Message Element
     **/
    public void testMessageToString() {
        Message msg1 = new Message();

        String toString = msg1.toString();

        assertTrue("Message should have had a toString", 0 != toString.length());
    }

    /**
     *    Test Message Element Remove
     **/
    public void testMessageRemoveElement() {
        Message msg1 = new Message();
        List    list = new ArrayList();
        List    namespaces = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);
        namespaces.add("not-default");
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);
        namespaces.add("not-default");
        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);
        namespaces.add("not-default");

        assertTrue("should have removed an element", msg1.removeMessageElement(elm1));

        // check if all of the elements are returned
        Message.ElementIterator eachElement = msg1.getMessageElements();
        Iterator eachListElement = list.iterator();
        Iterator eachNamesElement = namespaces.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("namespaces should have matched", eachElement.getNamespace().equals(eachNamesElement.next()));
            assertTrue("should be the same element", aElement == aListElement);
            assertTrue("elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());

        assertTrue("should not have removed an element", !msg1.removeMessageElement("bogus", elm2));
    }

    public void testIssue991() {
        Message msg1 = new Message();
        List    list = new ArrayList();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);
        MessageElement elm3 = new StringMessageElement("element3", "test3", null);

        msg1.addMessageElement(elm1);
        list.add(elm1);

        msg1.addMessageElement("not-default", elm1);
        list.add(elm1);

        msg1.addMessageElement(elm2);
        list.add(elm2);

        msg1.addMessageElement("not-default", elm2);
        list.add(elm2);

        msg1.addMessageElement(elm3);
        list.add(elm3);

        msg1.addMessageElement("not-default", elm3);
        list.add(elm3);

        msg1.addMessageElement("not-default", elm3);
        list.add(elm3);

        // Remove an element.
        Message.ElementIterator eachElement = msg1.getMessageElements();
        Iterator eachListElement = list.iterator();

        eachElement.next();
        eachListElement.next(); // "", elm1
        eachElement.next();
        eachListElement.next(); // "not-default", elm1
        eachElement.next();
        eachListElement.next(); // "", elm2
        eachElement.next();
        eachListElement.next(); // "not-default", elm2
        eachElement.next();
        eachListElement.next(); // "", elm3
        eachElement.next();
        eachListElement.next(); // "not-default", elm3
        eachElement.next();
        eachListElement.next(); // "not-default", elm3
        eachElement.remove();
        eachListElement.remove(); // should be removing elm3

        eachElement = msg1.getMessageElements();
        eachListElement = list.iterator();

        while (eachElement.hasNext()) {
            MessageElement aElement = eachElement.next();
            MessageElement aListElement = (MessageElement) eachListElement.next();

            assertTrue("should be the same element", aElement == aListElement);
            assertTrue("elements should be equal", aElement.equals(aListElement));
        }

        assertTrue("iterators should have ended at the same time", !eachListElement.hasNext());
    }

    public void testConcurrentMod() {
        Message msg1 = new Message();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);

        msg1.addMessageElement("not-default", elm2);

        msg1.addMessageElement("not-default", elm2);

        Iterator allElems = msg1.getMessageElements();

        MessageElement elm3 = (MessageElement) allElems.next();

        // this should cause a concurrent mod except if allElms.next() is called
        msg1.addMessageElement(elm1);

        try {
            elm3 = (MessageElement) allElems.next();
        } catch (ConcurrentModificationException failed) {}
    }

    public void testMessageSerialization() {
        Message msg1 = new Message();

        MessageElement elm1 = new StringMessageElement("element1", "test1", null);
        MessageElement elm2 = new StringMessageElement("element2", "test2", null);

        msg1.addMessageElement(elm1);
        msg1.addMessageElement("not-default", elm1);

        msg1.addMessageElement("not-default", elm2);

        msg1.addMessageElement("not-default", elm2);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(msg1);

            oos.close();
            bos.close();

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message msg2 = (Message) ois.readObject();

            assertTrue("Messages should have been equal()", msg1.equals(msg2));
        } catch (Throwable failure) {
            fail("Exception during test! " + failure.getMessage());
        }

    }
}
