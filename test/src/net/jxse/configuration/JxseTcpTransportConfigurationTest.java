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
 * Testing the tcp transport configuration object.
 */
public class JxseTcpTransportConfigurationTest {

    public JxseTcpTransportConfigurationTest() {
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
     * Test of getDefaultTcpTransportConfiguration method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testGetDefaultTcpTransportConfiguration() {

        JxseTcpTransportConfiguration Source = JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();

        assertTrue(Source!=null);
        assertTrue(Source.size()==0);

        Properties Defaults = Source.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_PORT"))==JxseTcpTransportConfiguration.DEFAULT_PORT);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_START_PORT"))==JxseTcpTransportConfiguration.DEFAULT_START_PORT);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_END_PORT"))==JxseTcpTransportConfiguration.DEFAULT_END_PORT);

        assertTrue(Source.getTcpPort()==JxseTcpTransportConfiguration.DEFAULT_PORT);
        assertTrue(Source.getTcpStartPort()==JxseTcpTransportConfiguration.DEFAULT_START_PORT);
        assertTrue(Source.getTcpEndPort()==JxseTcpTransportConfiguration.DEFAULT_END_PORT);

    }

    /**
     * Test of set/getTcpPort method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpPort() {
        
        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTcpPort()==-1);

        Temp.setTcpPort(3333);
        assertTrue(Temp.getTcpPort()==3333);

        Temp.setTcpPort(-1);
        assertTrue(Temp.getTcpPort()==-1);

        Temp.setTcpPort(4444);
        assertTrue(Temp.getTcpPort()==4444);

        Temp.setTcpPort(65536);
        assertTrue(Temp.getTcpPort()==-1);

    }


    /**
     * Test of set/getTcpStartPort method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpStartPort() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTcpStartPort()==-1);

        Temp.setTcpStartPort(3333);
        assertTrue(Temp.getTcpStartPort()==3333);

        Temp.setTcpStartPort(-1);
        assertTrue(Temp.getTcpStartPort()==-1);

        Temp.setTcpStartPort(4444);
        assertTrue(Temp.getTcpStartPort()==4444);

        Temp.setTcpStartPort(65536);
        assertTrue(Temp.getTcpPort()==-1);

    }

    /**
     * Test of set/getTcpEndPort method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpEndPort() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTcpEndPort()==-1);

        Temp.setTcpEndPort(3333);
        assertTrue(Temp.getTcpEndPort()==3333);

        Temp.setTcpEndPort(-1);
        assertTrue(Temp.getTcpEndPort()==-1);

        Temp.setTcpEndPort(4444);
        assertTrue(Temp.getTcpEndPort()==4444);

        Temp.setTcpEndPort(65536);
        assertTrue(Temp.getTcpEndPort()==-1);

    }

    /**
     * Test of set/getTcpIncoming method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpIncoming() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        Temp.setTcpIncoming(true);
        assertTrue(Temp.getTcpIncoming()==true);

        Temp.setTcpIncoming(false);
        assertTrue(Temp.getTcpIncoming()==false);

    }

    /**
     * Test of set/getTcpOutgoing method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpOutgoing() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        Temp.setTcpOutgoing(true);
        assertTrue(Temp.getTcpOutgoing()==true);

        Temp.setTcpOutgoing(false);
        assertTrue(Temp.getTcpOutgoing()==false);

    }

    /**
     * Test of set/getTcpInterfaceAddress method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpMulticastAddress() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTcpInterfaceAddress()==null);

        Temp.setTcpInterfaceAddress("123.456.789.123");
        assertTrue(Temp.getTcpInterfaceAddress().compareTo("123.456.789.123")==0);

        Temp.setTcpInterfaceAddress(null);
        assertTrue(Temp.getTcpInterfaceAddress()==null);

        Temp.setTcpInterfaceAddress("111.222.333.444");
        assertTrue(Temp.getTcpInterfaceAddress().compareTo("111.222.333.444")==0);

    }

    /**
     * Test of set/getTcpPublicAddress method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testSetGetTcpPublicAddress() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTcpPublicAddress()==null);

        Temp.setTcpPublicAddress("12.whatever.com.255", true);
        assertTrue(Temp.getTcpPublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Temp.getTcpPublicAddressExclusivity()==true);

        Temp.setTcpPublicAddress(null, false);
        assertTrue(Temp.getTcpPublicAddress()==null);
        assertTrue(Temp.getTcpPublicAddressExclusivity()==false);

        Temp.setTcpPublicAddress("22.whatover.com.245", false);
        assertTrue(Temp.getTcpPublicAddress().compareTo("22.whatover.com.245")==0);
        assertTrue(Temp.getTcpPublicAddressExclusivity()==false);

    }

    /**
     * Test of getTransportName method, of class JxseTcpTransportConfiguration.
     */
    @Test
    public void testGetTransportName() {

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();

        assertTrue(Temp.getTransportName().compareToIgnoreCase("TCP")==0);
        assertTrue(JxseTcpTransportConfiguration.TRANSPORT_NAME.compareToIgnoreCase("TCP")==0);

    }

    /**
     * Test of store/loadFromXML method, of class JxseHttpTransportConfiguration.
     */
    @Test
    public void testStoreLoadFromToXML() {

        JxseTcpTransportConfiguration Temp = JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();

        Temp.setTcpIncoming(false);
        Temp.setTcpOutgoing(true);
        Temp.setTcpPublicAddress("12.whatever.com.255", true);
        Temp.setTcpInterfaceAddress("12.whatever.com.255");
        Temp.setTcpPort(15000);
        Temp.setTcpEndPort(20000);
        Temp.setTcpStartPort(10000);

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Temp.storeToXML(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxseTcpTransportConfiguration Restore = new JxseTcpTransportConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        assertTrue(Restore.getTcpIncoming()==false);
        assertTrue(Restore.getTcpOutgoing()==true);

        assertTrue(Restore.getTcpPort()==15000);
        assertTrue(Restore.getTcpStartPort()==10000);
        assertTrue(Restore.getTcpEndPort()==20000);

        assertTrue(Restore.getTcpInterfaceAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.getTcpPublicAddress().compareTo("12.whatever.com.255")==0);
        assertTrue(Restore.getTcpPublicAddressExclusivity()==true);

        Properties Defaults = Restore.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==3);

        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_PORT"))==JxseTcpTransportConfiguration.DEFAULT_PORT);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_START_PORT"))==JxseTcpTransportConfiguration.DEFAULT_START_PORT);
        assertTrue(Integer.parseInt(Defaults.getProperty("JXSE_TCP_END_PORT"))==JxseTcpTransportConfiguration.DEFAULT_END_PORT);

    }

}
