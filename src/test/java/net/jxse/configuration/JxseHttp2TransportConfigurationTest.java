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
public class JxseHttp2TransportConfigurationTest {

    public JxseHttp2TransportConfigurationTest() {
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
     * Test of getDefaultHttp2TransportConfiguration method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testGetDefaultHttp2TransportConfiguration() {

        JxseHttp2TransportConfiguration Source = JxseHttp2TransportConfiguration.getDefaultHttp2TransportConfiguration();

        assertTrue(Source!=null);
        assertTrue(Source.size()==0);

        Properties Defaults = Source.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP2_OUTGOING"))==true);
        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP2_INCOMING"))==true);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_HTTP2_PORT"))==JxseHttp2TransportConfiguration.DEFAULT_HTTP2_PORT);

        assertTrue(Source.getHttp2Outgoing()==true);
        assertTrue(Source.getHttp2Incoming()==true);
        assertTrue(Source.getHttp2Port()==JxseHttp2TransportConfiguration.DEFAULT_HTTP2_PORT);
        
    }

    /**
     * Test of getTransportName method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testGetTransportName() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        assertTrue(Temp.getTransportName().compareToIgnoreCase("HTTP2")==0);
        assertTrue(JxseHttp2TransportConfiguration.TRANSPORT_NAME.compareToIgnoreCase("HTTP2")==0);

    }

    /**
     * Test of set/getHttp2Incoming method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testSetGetHttp2Incoming() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        Temp.setHttp2Incoming(true);
        assertTrue(Temp.getHttp2Incoming()==true);

        Temp.setHttp2Incoming(false);
        assertTrue(Temp.getHttp2Incoming()==false);

    }

    /**
     * Test of setHttp2Outgoing method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testSetGetHttp2Outgoing() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        Temp.setHttp2Outgoing(true);
        assertTrue(Temp.getHttp2Outgoing()==true);

        Temp.setHttp2Outgoing(false);
        assertTrue(Temp.getHttp2Outgoing()==false);

    }

    /**
     * Test of set/getHttp2Port method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testSetGetHttp2Port() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        assertTrue(Temp.getHttp2Port()==-1);

        Temp.setHttp2Port(3333);
        assertTrue(Temp.getHttp2Port()==3333);

        Temp.setHttp2Port(-1);
        assertTrue(Temp.getHttp2Port()==-1);

        Temp.setHttp2Port(4444);
        assertTrue(Temp.getHttp2Port()==4444);

        Temp.setHttp2Port(65536);
        assertTrue(Temp.getHttp2Port()==-1);

    }


    /**
     * Test of set/getHttp2InterfaceAddress method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testSetGetHttp2InterfaceAddress() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        assertTrue(Temp.getHttp2InterfaceAddress()==null);

        Temp.setHttp2InterfaceAddress("12.whatever.com.255");
        assertTrue(Temp.getHttp2InterfaceAddress().compareTo("12.whatever.com.255")==0);

        Temp.setHttp2InterfaceAddress(null);
        assertTrue(Temp.getHttp2InterfaceAddress()==null);

    }

    /**
     * Test of set/getHttp2PublicAddress method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testSetGetHttp2PublicAddress() {

        JxseHttp2TransportConfiguration Temp = new JxseHttp2TransportConfiguration();

        assertTrue(Temp.getHttp2PublicAddress()==null);

        Temp.setHttp2PublicAddress("12.whatever.com.255", true);
        assertTrue(Temp.getHttp2PublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Temp.isHttp2PublicAddressExclusive()==true);

        Temp.setHttp2PublicAddress(null, false);
        assertTrue(Temp.getHttp2PublicAddress()==null);
        assertTrue(Temp.isHttp2PublicAddressExclusive()==false);

        Temp.setHttp2PublicAddress("2.whatover.com.245", false);
        assertTrue(Temp.getHttp2PublicAddress().compareTo("2.whatover.com.245")==0);
        assertTrue(Temp.isHttp2PublicAddressExclusive()==false);

    }

    /**
     * Test of store/loadFromXML method, of class JxseHttp2TransportConfiguration.
     */
    @Test
    public void testStoreLoadFromToXML() {

        JxseHttp2TransportConfiguration Temp = JxseHttp2TransportConfiguration.getDefaultHttp2TransportConfiguration();

        Temp.setHttp2Port(3333);
        Temp.setHttp2Incoming(true);
        Temp.setHttp2Outgoing(true);
        Temp.setHttp2InterfaceAddress("12.whatever.com.255");
        Temp.setHttp2PublicAddress("12.whatever.com.255", true);
        Temp.setHttp2StartPort(555);
        Temp.setHttp2EndPort(666);

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Temp.storeToXML(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxseHttp2TransportConfiguration Restore = new JxseHttp2TransportConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        assertTrue(Restore.getHttp2Port()==3333);
        assertTrue(Restore.getHttp2Incoming()==true);
        assertTrue(Restore.getHttp2Outgoing()==true);
        assertTrue(Restore.getHttp2InterfaceAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.getHttp2PublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.isHttp2PublicAddressExclusive()==true);
        assertTrue(Restore.getHttp2StartPort()==555);
        assertTrue(Restore.getHttp2EndPort()==666);

        Properties Defaults = Restore.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP2_OUTGOING"))==true);
        assertTrue(Boolean.parseBoolean(Defaults.getProperty("JXSE_HTTP2_INCOMING"))==true);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_HTTP2_PORT"))==JxseHttp2TransportConfiguration.DEFAULT_HTTP2_PORT);

    }

}
