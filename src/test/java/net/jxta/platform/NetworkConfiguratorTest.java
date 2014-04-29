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
package net.jxta.platform;

import java.io.IOException;
import java.io.File;

import junit.framework.TestSuite;
import junit.framework.TestCase;
import junit.framework.Test;
import net.jxta.exception.ConfiguratorException;

/**
 *  A JUnit test for NetworkConfigurator
 */
public class NetworkConfiguratorTest extends TestCase {
    private File home = new File(System.getProperty("JXTA_HOME", ".jxta"));
    private File adhocHome = new File(home, "adhoc");
    private File edgeHome = new File(home, "edge");
    private File rdvHome = new File(home, "rdv");
    private File rlyHome = new File(home, "rly");
    private File proxHome = new File(home, "prox");
    private File rdvRelayHome = new File(home, "rdv_rly");
    private File rdvRelayProxyHome = new File(home, "rdv_rly_prox");
    private static boolean failed = false;
    private static final int rdvRelay = NetworkConfigurator.RELAY_SERVER + NetworkConfigurator.RDV_SERVER;
    private static final int rdvRelayProx = NetworkConfigurator.RELAY_SERVER + NetworkConfigurator.RDV_SERVER
            + NetworkConfigurator.PROXY_SERVER;

    /**
     *Constructor for the NetworkConfiguratorTest object
     *
     * @param  testName  test name
     */
    public NetworkConfiguratorTest(java.lang.String testName) {
        super(testName);
    }

    /**
     *  The main program for the NetworkConfiguratorTest class
     *
     * @param  args  The command line arguments
     */
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    /**
     *  A unit test suite for JUnit
     *
     * @return    The test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(NetworkConfiguratorTest.class);

        return suite;
    }

    /**
     *  The JUnit setup method
     */
    public void testCreateConfiguration() throws ConfiguratorException {
        createConfiguration(NetworkConfigurator.ADHOC_NODE);
        createConfiguration(NetworkConfigurator.EDGE_NODE);
        createConfiguration(NetworkConfigurator.RDV_NODE);
//        createConfiguration(NetworkConfigurator.PROXY_NODE);
        createConfiguration(NetworkConfigurator.RELAY_NODE);
//        createConfiguration(NetworkConfigurator.RDV_RELAY_PROXY_NODE);
    }

    private void createConfiguration(int mode) throws ConfiguratorException {
        try {
            NetworkConfigurator config = null;

            switch (mode) {
            case NetworkConfigurator.ADHOC_NODE:
                config = new NetworkConfigurator(mode, adhocHome.toURI());
                config.setName("ADHOC_NODE");
                break;

            case NetworkConfigurator.EDGE_NODE:
                config = new NetworkConfigurator(mode, edgeHome.toURI());
                config.setName("EDGE");
//                config.addSeedRelay(URI.create("tcp://192.18.37.37:9701"));
//                config.addSeedRendezvous(URI.create("tcp://192.18.37.37:9701"));
//                config.addRdvSeedingURI(URI.create("http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?3"));
//                config.addRelaySeedingURI(URI.create("http://rdv.jxtahosts.net/cgi-bin/relays.cgi?3"));
                break;

            case NetworkConfigurator.RELAY_NODE:
                config = new NetworkConfigurator(mode, rlyHome.toURI());
                config.setName("RELAY");
//                config.addRelaySeedingURI(URI.create("http://rdv.jxtahosts.net/cgi-bin/relays.cgi?3"));
                break;

            case NetworkConfigurator.RDV_NODE:
                config = new NetworkConfigurator(mode, rdvHome.toURI());
//                config.addRdvSeedingURI(URI.create("http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?3"));
                config.setName("RDV_SERVER");
                break;

//            case NetworkConfigurator.PROXY_NODE:
//                config = new NetworkConfigurator(mode, proxHome.toURI());
//                config.setName("PROXY_SERVER");
//                break;
//
//            case NetworkConfigurator.RDV_RELAY_PROXY_NODE:
//                config = new NetworkConfigurator(mode, rdvRelayProxyHome.toURI());
//                config.setName("rdvrlyprox");
//                break;

            default:
                fail("Invalid configuration mode :" + Integer.toString(mode, 2));
            }
            config.setPrincipal("principal");
            config.setPassword("password");
            config.save();
            assertTrue("Configuration does not exist", config.exists());
        } catch (IOException io) {
            io.printStackTrace();
            fail("Failed to create edge configuration :" + io.getMessage());
        }
    }

    /**
     *  {@inheritDoc}
     */
    public static void fail(String message) {
        failed = true;
        junit.framework.TestCase.fail(message);
    }

}

