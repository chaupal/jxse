/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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

package tutorial.message;


import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * A simple and re-usable example of manipulating JXATA Messages. Included in
 * this tutorial are:
 * <p/>
 * <ul>
 * <li>Adding and reading {@code String}, {@code int} and {@code long} with Message elements.</li>
 * <li>Adding and reading Java {@code Object} with Message Elements.
 * <li>Adding and reading byte arrays with Message Elements.</li>
 * <li>Adding and reading JXTA Advertisements with Message Elements.</li>
 * <li>Compressing message element content with gzip.</li>
 * </ul>
 */
public class MessageTutorial {
    private final static MimeMediaType GZIP_MEDIA_TYPE = new MimeMediaType("application/gzip").intern();

    /**
     * Adds a String to a Message as a StringMessageElement
     *
     * @param message   The message to add to
     * @param nameSpace The namespace of the element to add. a null value assumes default namespace.
     * @param elemName  Name of the Element.
     * @param string    The string to add
     */
    public static void addStringToMessage(Message message, String nameSpace, String elemName, String string) {
        message.addMessageElement(nameSpace, new StringMessageElement(elemName, string, null));
    }

    /**
     * Adds a long to a message
     *
     * @param message   The message to add to
     * @param nameSpace The namespace of the element to add. a null value assumes default namespace.
     * @param elemName  Name of the Element.
     * @param data      The feature to be added to the LongToMessage attribute
     */
    public static void addLongToMessage(Message message, String nameSpace, String elemName, long data) {
        message.addMessageElement(nameSpace, new StringMessageElement(elemName, Long.toString(data), null));
    }

    /**
     * Adds a int to a message
     *
     * @param message   The message to add to
     * @param nameSpace The namespace of the element to add. a null value assumes default namespace.
     * @param elemName  Name of the Element.
     * @param data      The feature to be added to the IntegerToMessage attribute
     */
    public static void addIntegerToMessage(Message message, String nameSpace, String elemName, int data) {
        message.addMessageElement(nameSpace, new StringMessageElement(elemName, Integer.toString(data), null));
    }

    /**
     * Adds an byte array to a message
     *
     * @param message   The message to add to
     * @param nameSpace The namespace of the element to add. a null value assumes default namespace.
     * @param elemName  Name of the Element.
     * @param data      the byte array
     * @param compress  indicates whether to use GZIP compression
     * @throws IOException if an io error occurs
     */
    public static void addByteArrayToMessage(Message message, String nameSpace, String elemName, byte[] data, boolean compress) throws IOException {
        byte[] buffer = data;
        MimeMediaType mimeType = MimeMediaType.AOS;

        if (compress) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(outStream);

            gos.write(data, 0, data.length);
            gos.finish();
            gos.close();
            buffer = outStream.toByteArray();
            mimeType = GZIP_MEDIA_TYPE;
        }
        message.addMessageElement(nameSpace, new ByteArrayMessageElement(elemName, mimeType, buffer, null));
    }

    /**
     * Adds an Object to message within the specified name space and with the specified element name
     * @param message   the message to add the object to
     * @param nameSpace the name space to add the object under
     * @param elemName  the given element name
     * @param object    the object
     * @throws IOException if an io error occurs
     */
    public static void addObjectToMessage(Message message, String nameSpace, String elemName, Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(object);
        oos.close();
        bos.close();
        addByteArrayToMessage(message, nameSpace, elemName, bos.toByteArray(), false);
    }

    /**
     * Returns a String from a message
     *
     * @param message   The message to retrieve from
     * @param nameSpace The namespace of the element to get.
     * @param elemName  Name of the Element.
     * @return The string value or {@code null} if there was no element matching the specified name.
     */
    public static String getStringFromMessage(Message message, String nameSpace, String elemName) {
        MessageElement me = message.getMessageElement(nameSpace, elemName);

        if (null != me) {
            return me.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns an long from a message
     *
     * @param message   The message to retrieve from
     * @param nameSpace The namespace of the element to get.
     * @param elemName  Name of the Element.
     * @return The long value
     * @throws NumberFormatException If the String does not contain a parsable int.
     */
    public static long getLongFromMessage(Message message, String nameSpace, String elemName) throws NumberFormatException {
        String longStr = getStringFromMessage(message, nameSpace, elemName);

        if (null != longStr) {
            return Long.parseLong(longStr);
        } else {
            throw new NumberFormatException("No such Message Element.");
        }
    }

    /**
     * Returns an int from a message
     *
     * @param message   The message to retrieve from
     * @param nameSpace The namespace of the element to get.
     * @param elemName  Name of the Element.
     * @return The int value
     * @throws NumberFormatException If the String does not contain a parsable long.
     */
    public static int getIntegerFromMessage(Message message, String nameSpace, String elemName) throws NumberFormatException {
        String intStr = getStringFromMessage(message, nameSpace, elemName);

        if (null != intStr) {
            return Integer.parseInt(intStr);
        } else {
            throw new NumberFormatException("No such Message Element.");
        }
    }

    /**
     * Returns an InputStream for a byte array
     *
     * @param message   The message to retrieve from
     * @param nameSpace The namespace of the element to get.
     * @param elemName  Name of the Element.
     * @return The {@code InputStream} or {@code null} if the message has no such element, String elemName) throws IOException {
     * @throws IOException if an io error occurs
     */
    public static InputStream getInputStreamFromMessage(Message message, String nameSpace, String elemName) throws IOException {
        InputStream result = null;
        MessageElement element = message.getMessageElement(nameSpace, elemName);

        if (null == element) {
            return null;
        }

        if (element.getMimeType().equals(GZIP_MEDIA_TYPE)) {
            result = new GZIPInputStream(element.getStream());
        } else if (element.getMimeType().equals(MimeMediaType.AOS)) {
            result = element.getStream();
        }
        return result;
    }

    /**
     * Reads a single Java Object from a Message.
     *
     * @param message   The message containing the object.
     * @param nameSpace The name space of the element containing the object.
     * @param elemName  The name of the element containing the object.
     * @return The Object or {@code null} if the Message contained no such element.
     * @throws IOException if an io error occurs
     * @throws ClassNotFoundException if an object could not constructed from the message element
     */
    public static Object getObjectFromMessage(Message message, String nameSpace, String elemName) throws IOException, ClassNotFoundException {
        InputStream is = getInputStreamFromMessage(message, nameSpace, elemName);

        if (null == is) {
            return null;
        }
        ObjectInputStream ois = new ObjectInputStream(is);

        return ois.readObject();
    }

    /**
     * Prints message element names and content and some stats
     *
     * @param msg     message to print
     * @param verbose indicates whether to print elment content
     */
    public static void printMessageStats(Message msg, boolean verbose) {
        try {
            System.out.println("------------------Begin Message---------------------");
            WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, new MimeMediaType("application/x-jxta-msg"), null);

            System.out.println("Message Size :" + serialed.getByteLength());

            ElementIterator it = msg.getMessageElements();

            while (it.hasNext()) {
                MessageElement el = it.next();

                System.out.println("Element : " + it.getNamespace() + " :: " + el.getElementName());
                if (verbose) {
                    System.out.println("[" + el + "]");
                }
            }
            System.out.println("-------------------End Message----------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Illustrates adding and retrieving a String to and from a Message
     */
    public static void stringExample() {
        Message message = new Message();

        addStringToMessage(message, "TutorialNameSpace", "String Test", "This is a test");
        printMessageStats(message, true);
        System.out.println("String Value :" + getStringFromMessage(message, "TutorialNameSpace", "String Test"));
    }

    /**
     * Illustrates adding and retrieving a long to and from a Message
     */
    public static void longExample() {
        Message message = new Message();

        addLongToMessage(message, "TutorialNameSpace", "long test", Long.MAX_VALUE);
        printMessageStats(message, true);
        System.out.println("long Value :" + getLongFromMessage(message, "TutorialNameSpace", "long test"));
    }

    /**
     * Illustrates adding and retrieving an integer to and from a Message
     */
    public static void intExample() {
        Message message = new Message();

        addIntegerToMessage(message, "TutorialNameSpace", "int test", Integer.MAX_VALUE);
        printMessageStats(message, true);
        System.out.println("int Value :" + getIntegerFromMessage(message, "TutorialNameSpace", "int test"));
    }

    /**
     * Illustrates adding and retrieving byte-array to and from a Message
     */
    public static void byteArrayExample() {
        Message message = new Message();

        try {
            File file = new File("message.tst");

            file.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");

            raf.setLength(1024 * 4);
            int size = 4096;
            byte[] buf = new byte[size];

            raf.read(buf, 0, size);
            addByteArrayToMessage(message, "TutorialNameSpace", "byte test", buf, true);
            printMessageStats(message, true);
            InputStream is = getInputStreamFromMessage(message, "TutorialNameSpace", "byte test");
            int count = 0;

            while (is.read() != -1) {
                count++;
            }
            System.out.println("Read " + count + " byte back");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * Illustrates adding and retrieving advertisements to and from a Message
     */
    public static void xmlDocumentExample() {
        Message message = new Message();
        PipeAdvertisement pipeAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

        pipeAdv.setPipeID(IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID));
        pipeAdv.setType(PipeService.UnicastType);
        message.addMessageElement("MESSAGETUT"
                ,
                new TextDocumentMessageElement("MESSAGETUT", (XMLDocument) pipeAdv.getDocument(MimeMediaType.XMLUTF8), null));
        MessageElement msgElement = message.getMessageElement("MESSAGETUT", "MESSAGETUT");

        try {
            XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(msgElement.getMimeType()
                    ,
                    msgElement.getStream());
            PipeAdvertisement newPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);

            System.out.println(newPipeAdv.toString());
        } catch (IOException e) {
            // This is thrown if the message element could not be read.
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // This is thrown if the document or advertisement is invalid (illegal values, missing tags, etc.)
            e.printStackTrace();
        }
    }

    /**
     * Main method
     *
     * @param args command line arguments.  None defined
     */
    public static void main(String args[]) {
        stringExample();
        longExample();
        intExample();
        byteArrayExample();
        xmlDocumentExample();
    }
}
