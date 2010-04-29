/*
 * TextDocumentMessageElementTest.java
 * JUnit based test
 *
 * Created on May 8, 2006, 6:26 PM
 */

package net.jxta.endpoint;

import junit.framework.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import net.jxta.document.MimeMediaType;

/**
 *
 * @author mike
 */
public class TextDocumentMessageElementTest extends TestCase {
    
    public TextDocumentMessageElementTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {}

    @Override
    protected void tearDown() throws Exception {}

    public static Test suite() {
        TestSuite suite = new TestSuite(TextDocumentMessageElementTest.class);
        
        return suite;
    }

    /**
     * Test of equals method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testEquals() {
        System.out.println("equals");
        
        Object target = null;
        TextDocumentMessageElement instance = null;
        
        boolean expResult = true;
        boolean result = instance.equals(target);

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testHashCode() {
        System.out.println("hashCode");
        
        TextDocumentMessageElement instance = null;
        
        int expResult = 0;
        int result = instance.hashCode();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testToString() {
        System.out.println("toString");
        
        TextDocumentMessageElement instance = null;
        
        String expResult = "";
        String result = instance.toString();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMimeType method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetMimeType() {
        System.out.println("getMimeType");
        
        TextDocumentMessageElement instance = null;
        
        MimeMediaType expResult = null;
        MimeMediaType result = instance.getMimeType();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFileExtension method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetFileExtension() {
        System.out.println("getFileExtension");
        
        TextDocumentMessageElement instance = null;
        
        String expResult = "";
        String result = instance.getFileExtension();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStream method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetStream() throws Exception {
        System.out.println("getStream");
        
        TextDocumentMessageElement instance = null;
        
        InputStream expResult = null;
        InputStream result = instance.getStream();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sendToStream method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testSendToStream() throws Exception {
        System.out.println("sendToStream");
        
        OutputStream sendTo = null;
        TextDocumentMessageElement instance = null;
        
        instance.sendToStream(sendTo);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getReader method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetReader() throws Exception {
        System.out.println("getReader");
        
        TextDocumentMessageElement instance = null;
        
        Reader expResult = null;
        Reader result = instance.getReader();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sendToWriter method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testSendToWriter() throws Exception {
        System.out.println("sendToWriter");
        
        Writer sendTo = null;
        TextDocumentMessageElement instance = null;
        
        instance.sendToWriter(sendTo);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBytes method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetBytes() {
        System.out.println("getBytes");
        
        boolean copy = true;
        TextDocumentMessageElement instance = null;
        
        byte[] expResult = null;
        byte[] result = instance.getBytes(copy);

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCharLength method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetCharLength() {
        System.out.println("getCharLength");
        
        TextDocumentMessageElement instance = null;
        
        long expResult = 0L;
        long result = instance.getCharLength();

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChars method, of class net.jxta.endpoint.TextDocumentMessageElement.
     */
    public void testGetChars() {
        System.out.println("getChars");
        
        boolean copy = true;
        TextDocumentMessageElement instance = null;
        
        char[] expResult = null;
        char[] result = instance.getChars(copy);

        assertEquals(expResult, result);
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
