/*
 * ====================================================================
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
public class JxseMulticastTransportConfigurationTest {

    public JxseMulticastTransportConfigurationTest() {
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
     * Test of getDefaultMulticastTransportConfiguration method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testGetDefaultMulticastTransportConfiguration() {

        JxseMulticastTransportConfiguration Source = JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();

        assertTrue(Source!=null);
        assertTrue(Source.size()==0);

        Properties Defaults = Source.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==2);

        assertTrue(Defaults.getProperty("JXSE_MULTICAST_ADDRESS").compareTo(JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_ADDRESS)==0);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_MULTICAST_PORT"))==JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_PORT);

        assertTrue(Source.getMulticastAddress().compareTo(JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_ADDRESS)==0);
        assertTrue(Source.getMulticastPort()==JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_PORT);

    }

    /**
     * Test of getTransportName method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testGetTransportName() {

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();

        assertTrue(Temp.getTransportName().compareToIgnoreCase("MULTI")==0);
        assertTrue(JxseMulticastTransportConfiguration.TRANSPORT_NAME.compareToIgnoreCase("MULTI")==0);

    }

    /**
     * Test of setMulticastPacketSize method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testSetGetMulticastPacketSize() {

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();

        assertTrue(Temp.getMulticastPacketSize()==-1);

        Temp.setMulticastPacketSize(3333);
        assertTrue(Temp.getMulticastPacketSize()==3333);

        Temp.setMulticastPacketSize(-1);
        assertTrue(Temp.getMulticastPacketSize()==-1);

        Temp.setMulticastPacketSize(4444);
        assertTrue(Temp.getMulticastPacketSize()==4444);

    }

    /**
     * Test of setMulticastAddress method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testSetGetMulticastAddress() {

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();

        assertTrue(Temp.getMulticastAddress()==null);

        Temp.setMulticastAddress("123.456.789.123");
        assertTrue(Temp.getMulticastAddress().compareTo("123.456.789.123")==0);

        Temp.setMulticastAddress(null);
        assertTrue(Temp.getMulticastAddress()==null);

        Temp.setMulticastAddress("111.222.333.444");
        assertTrue(Temp.getMulticastAddress().compareTo("111.222.333.444")==0);

    }

    /**
     * Test of setMulticastInterface method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testSetMulticastInterface() {

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();

        assertTrue(Temp.getMulticastInterface()==null);

        Temp.setMulticastInterface("123.456.789.123");
        assertTrue(Temp.getMulticastInterface().compareTo("123.456.789.123")==0);

        Temp.setMulticastInterface(null);
        assertTrue(Temp.getMulticastInterface()==null);

        Temp.setMulticastInterface("111.222.333.444");
        assertTrue(Temp.getMulticastInterface().compareTo("111.222.333.444")==0);

    }

    /**
     * Test of set/getMulticastPort method, of class JxseMulticastTransportConfiguration.
     */
    @Test
    public void testSetGetMulticastPort() {

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();

        assertTrue(Temp.getMulticastPort()==-1);

        Temp.setMulticastPort(3333);
        assertTrue(Temp.getMulticastPort()==3333);

        Temp.setMulticastPort(-1);
        assertTrue(Temp.getMulticastPort()==-1);

        Temp.setMulticastPort(4444);
        assertTrue(Temp.getMulticastPort()==4444);

    }

    /**
     * Test of store/loadFromXML method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testStoreLoadFromToXML() {

        JxseMulticastTransportConfiguration Temp = JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();

        Temp.setMulticastAddress("AAA.BBB.CCC.DDD");
        Temp.setMulticastInterface("EEE.FFF.GGG.HHH");
        Temp.setMulticastPacketSize(476);
        Temp.setMulticastPort(432);

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Temp.storeToXML(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxseMulticastTransportConfiguration Restore = new JxseMulticastTransportConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        assertTrue(Restore.getMulticastAddress().compareTo("AAA.BBB.CCC.DDD")==0);
        assertTrue(Restore.getMulticastInterface().compareTo("EEE.FFF.GGG.HHH")==0);
        assertTrue(Restore.getMulticastPacketSize()==476);
        assertTrue(Restore.getMulticastPort()==432);

        Properties Defaults = Restore.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==2);

        assertTrue(Defaults.getProperty("JXSE_MULTICAST_ADDRESS").compareTo(JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_ADDRESS)==0);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_MULTICAST_PORT"))==JxseMulticastTransportConfiguration.DEFAULT_IP_MULTICAST_PORT);

    }

}
