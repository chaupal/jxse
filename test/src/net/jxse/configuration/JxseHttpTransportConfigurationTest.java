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

package net.jxse.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
 * Testing the http transport configuration object.
 */
public class JxseHttpTransportConfigurationTest {

    public JxseHttpTransportConfigurationTest() {
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

    /**
     * Test of getDefaultHttpTransportConfiguration method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testGetDefaultHttpTransportConfiguration() {

        JxseHttpTransportConfiguration Source = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();

        assertTrue(Source!=null);
        assertTrue(Source.size()==0);

        Properties Defaults = Source.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP_OUTGOING"))==true);
        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP_INCOMING"))==false);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_HTTP_PORT"))==JxseHttpTransportConfiguration.DEFAULT_HTTP_PORT);

        assertTrue(Source.getHttpOutgoing()==true);
        assertTrue(Source.getHttpIncoming()==false);
        assertTrue(Source.getHttpPort()==JxseHttpTransportConfiguration.DEFAULT_HTTP_PORT);
        
    }

    /**
     * Test of getTransportName method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testGetTransportName() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        assertTrue(Temp.getTransportName().compareToIgnoreCase("HTTP")==0);
        assertTrue(JxseHttpTransportConfiguration.TRANSPORT_NAME.compareToIgnoreCase("HTTP")==0);

    }

    /**
     * Test of set/getHttpIncoming method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testSetGetHttpIncoming() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        Temp.setHttpIncoming(true);
        assertTrue(Temp.getHttpIncoming()==true);

        Temp.setHttpIncoming(false);
        assertTrue(Temp.getHttpIncoming()==false);

    }

    /**
     * Test of setHttpOutgoing method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testSetGetHttpOutgoing() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        Temp.setHttpOutgoing(true);
        assertTrue(Temp.getHttpOutgoing()==true);

        Temp.setHttpOutgoing(false);
        assertTrue(Temp.getHttpOutgoing()==false);

    }

    /**
     * Test of set/getHttpPort method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testSetGetHttpPort() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        assertTrue(Temp.getHttpPort()==-1);

        Temp.setHttpPort(3333);
        assertTrue(Temp.getHttpPort()==3333);

        Temp.setHttpPort(-1);
        assertTrue(Temp.getHttpPort()==-1);

        Temp.setHttpPort(4444);
        assertTrue(Temp.getHttpPort()==4444);

        Temp.setHttpPort(65536);
        assertTrue(Temp.getHttpPort()==-1);

    }


    /**
     * Test of set/getHttpInterfaceAddress method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testSetGetHttpInterfaceAddress() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        assertTrue(Temp.getHttpInterfaceAddress()==null);

        Temp.setHttpInterfaceAddress("12.whatever.com.255");
        assertTrue(Temp.getHttpInterfaceAddress().compareTo("12.whatever.com.255")==0);

        Temp.setHttpInterfaceAddress(null);
        assertTrue(Temp.getHttpInterfaceAddress()==null);

    }

    /**
     * Test of set/getHttpPublicAddress method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testSetGetHttpPublicAddress() {

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();

        assertTrue(Temp.getHttpPublicAddress()==null);

        Temp.setHttpPublicAddress("12.whatever.com.255", true);
        assertTrue(Temp.getHttpPublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Temp.getHttpPublicAddressExclusive()==true);

        Temp.setHttpPublicAddress(null, false);
        assertTrue(Temp.getHttpPublicAddress()==null);
        assertTrue(Temp.getHttpPublicAddressExclusive()==false);

        Temp.setHttpPublicAddress("22.whatover.com.245", false);
        assertTrue(Temp.getHttpPublicAddress().compareTo("22.whatover.com.245")==0);
        assertTrue(Temp.getHttpPublicAddressExclusive()==false);

    }

    /**
     * Test of store/loadFromXML method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testStoreLoadFromToXML() {

        JxseHttpTransportConfiguration Temp = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();

        Temp.setHttpPort(3333);
        Temp.setHttpIncoming(true);
        Temp.setHttpOutgoing(false);
        Temp.setHttpInterfaceAddress("12.whatever.com.255");
        Temp.setHttpPublicAddress("12.whatever.com.255", true);

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Temp.storeToXML(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxseHttpTransportConfiguration Restore = new JxseHttpTransportConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        assertTrue(Restore.getHttpPort()==3333);
        assertTrue(Restore.getHttpIncoming()==true);
        assertTrue(Restore.getHttpOutgoing()==false);
        assertTrue(Restore.getHttpInterfaceAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.getHttpPublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.getHttpPublicAddressExclusive()==true);

        Properties Defaults = Restore.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP_OUTGOING"))==true);
        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP_INCOMING"))==false);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_HTTP_PORT"))==JxseHttpTransportConfiguration.DEFAULT_HTTP_PORT);

    }

}
