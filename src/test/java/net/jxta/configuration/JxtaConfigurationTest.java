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

package net.jxta.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test of the JxtaConfiguration class.
 */
public class JxtaConfigurationTest {

    public JxtaConfigurationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testConstructor_0Param() {

        JxtaConfiguration Source = new JxtaConfiguration();

        assertTrue(Source.size()==0);

        assertTrue(Source.getDefaultsCopy()!=null);
        assertTrue(Source.getDefaultsCopy().size()==0);

        assertTrue(Source.getProperty("PROP1")==null);
        assertTrue(Source.getDefaultPropertyValue("PROP1")==null);

    }

    @Test
    public void testConstructor_1Param() {

        JxtaConfiguration Source = new JxtaConfiguration();

        Source.setProperty("PROP1", "VALUE1");
        Source.setProperty("PROP2", "VALUE2");

        Source.setDefaultPropertyValue("PROP1", "DEFAULT1");

        JxtaConfiguration Destination;

        Destination = new JxtaConfiguration(Source);

        assertTrue(Destination!=null);

        assertTrue(Destination.containsKey("PROP1"));
        assertTrue(Destination.containsKey("PROP2"));

        assertTrue(Destination.containsValue("VALUE1"));
        assertTrue(Destination.containsValue("VALUE2"));

        assertFalse(Destination.containsKey("PROP3"));
        assertFalse(Destination.containsValue("VALUE3"));

        assertTrue(Destination.getDefaultPropertyValue("PROP1").compareTo("DEFAULT1")==0);
        assertTrue(Destination.getDefaultPropertyValue("PROP2")==null);

        assertTrue(Source.size()==2);
        assertTrue(Source.size()==Destination.size());

        assertTrue(Source.getDefaultsCopy().size()==1);
        assertTrue(Destination.getDefaultsCopy().size()==Source.getDefaultsCopy().size());

    }

    @Test
    public void testDefaults() {

        JxtaConfiguration Source = new JxtaConfiguration();

        Source.setProperty("PROP1", "VALUE1");

        Source.setDefaultPropertyValue("PROP1", "DEFAULT1");
        Source.setDefaultPropertyValue("PROP2", "DEFAULT2");

        // Testing source object
        assertTrue(Source.size()==1);

        assertTrue(Source.containsKey("PROP1"));
        assertTrue(Source.containsValue("VALUE1"));

        assertFalse(Source.containsKey("PROP3"));
        assertFalse(Source.containsValue("VALUE3"));

        // Testing Defaults object
        Properties Defaults = Source.getDefaultsCopy();

        assertTrue(Defaults.size()==2);

        assertTrue(Defaults.containsKey("PROP1"));
        assertTrue(Defaults.containsValue("DEFAULT1"));

        assertTrue(Defaults.containsKey("PROP2"));
        assertTrue(Defaults.containsValue("DEFAULT2"));

        assertFalse(Defaults.containsKey("PROP3"));
        assertFalse(Defaults.containsValue("DEFAULT3"));

        // Testing defaults extraction
        String Temp1 = Source.getDefaultPropertyValue("PROP1");
        assertTrue(Temp1.compareTo("DEFAULT1")==0);

        String Temp2 = Source.getDefaultPropertyValue("PROP2");
        assertTrue(Temp2.compareTo("DEFAULT2")==0);

        String Temp3 = Source.getDefaultPropertyValue("PROP3");
        assertTrue(Temp3==null);

        // Testing expected behavior
        String Temp10 = Source.getProperty("PROP1");
        assertTrue(Temp10.compareTo("VALUE1")==0);

        String Temp20 = Source.getProperty("PROP2");
        assertTrue(Temp20.compareTo("DEFAULT2")==0);

        String Temp30 = Source.getProperty("PROP3");
        assertTrue(Temp30==null);

    }

    /**
     * Test of set/getProperty method, of class JxtaConfiguration.
     */
    @Test
    public void testSetGetProperty() {

        JxtaConfiguration Source = new JxtaConfiguration();

        assertTrue(Source!=null);

        Source.setProperty("XXX", "YYY");

        assertTrue(Source.containsKey("XXX"));
        assertTrue(Source.containsValue("YYY"));

        assertFalse(Source.containsKey("AAA"));
        assertFalse(Source.containsValue("BBB"));

    }

    /**
     * Test of Store/loadFromXML method, of class JxtaConfiguration.
     */
    @Test
    public void testStoreLoadFromXML() throws Exception {

        JxtaConfiguration Source = new JxtaConfiguration();

        assertTrue(Source!=null);

        Source.setProperty("XXX", "YYY");
        Source.setDefaultPropertyValue("DDD", "FFF");

        File TempFile = new File("testStoreLoadFromXML");
        TempFile.delete();

        FileOutputStream FOS = new FileOutputStream("testStoreLoadFromXML");

        try {
            Source.storeToXML(FOS, "Test");
            FOS.close();
        } catch (IOException ex) {
            fail(ex.toString());
        }

        FileInputStream FIS = new FileInputStream("testStoreLoadFromXML");

        JxtaConfiguration Restore = new JxtaConfiguration();

        try {
            Restore.loadFromXML(FIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Checking content
        assertTrue(Restore.size()==1);

        assertTrue(Restore.containsKey("XXX"));
        assertTrue(Restore.containsValue("YYY"));

        Properties TempP = Restore.getDefaultsCopy();

        assertTrue(TempP.size()==1);

        assertTrue(TempP.containsKey("DDD"));
        assertTrue(TempP.containsValue("FFF"));

    }

    /**
     * Test of Store/loadFromXML method with encoding, of class JxtaConfiguration.
     */
    @Test
    public void testStoreLoadFromXMLwithEncoding() throws Exception {

        JxtaConfiguration Source = new JxtaConfiguration();

        assertTrue(Source!=null);

        Source.setProperty("���", "�");
        Source.setDefaultPropertyValue("DDD", "���");

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Source.storeToXML(BAOS, "Test", "UTF-16");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxtaConfiguration Restore = new JxtaConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Checking content
        assertTrue(Restore.size()==1);

        assertTrue(Restore.containsKey("���"));
        assertTrue(Restore.containsValue("�"));

        Properties TempP = Restore.getDefaultsCopy();

        assertTrue(TempP.size()==1);

        assertTrue(TempP.containsKey("DDD"));
        assertTrue(TempP.containsValue("���"));

    }

    /**
     * Test of store/load method, of class JxtaConfiguration.
     */
    @Test
    public void testStoreLoad() throws Exception {

        JxtaConfiguration Source = new JxtaConfiguration();

        assertTrue(Source!=null);

        Source.setProperty("XXX", "YYY");
        Source.setDefaultPropertyValue("DDD", "FFF");

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Source.store(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxtaConfiguration Restore = new JxtaConfiguration();

        try {
            Restore.load(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Checking content
        assertTrue(Restore.size()==1);

        assertTrue(Restore.containsKey("XXX"));
        assertTrue(Restore.containsValue("YYY"));

        Properties TempP = Restore.getDefaultsCopy();

        assertTrue(TempP.size()==1);

        assertTrue(TempP.containsKey("DDD"));
        assertTrue(TempP.containsValue("FFF"));

    }

}
