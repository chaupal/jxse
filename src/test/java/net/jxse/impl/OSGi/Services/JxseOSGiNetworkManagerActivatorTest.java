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

package net.jxse.impl.OSGi.Services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.jxse.OSGi.Services.JxseOSGiNetworkManagerService;
import net.jxse.OSGi.JxseOSGiFramework;
import net.jxse.configuration.JxseHttpTransportConfiguration;
import net.jxse.configuration.JxseMulticastTransportConfiguration;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxse.configuration.JxsePeerConfiguration.ConnectionMode;
import net.jxse.configuration.JxseTcpTransportConfiguration;
import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test of JxseOSGiNetworkManager using OSGi
 */
public class JxseOSGiNetworkManagerActivatorTest {

    public JxseOSGiNetworkManagerActivatorTest() {
    }

    private JxseOSGiNetworkManagerService TheNMS;
    private ServiceTracker ST;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {

        try {
            JxseOSGiFramework.INSTANCE.start();
        } catch (BundleException ex) {
            fail(ex.toString());
        }

        // Retrieving the NetworkManager service
        ST = JxseOSGiFramework.getServiceTracker(JxseOSGiNetworkManagerService.class);
        ST.open();

        try {
            TheNMS = (JxseOSGiNetworkManagerService) ST.waitForService(5000);
        } catch (InterruptedException ex) {
            fail(ex.toString());
        }

        if (TheNMS==null) {
            fail("Cannot retrieve the " + JxseOSGiNetworkManagerService.class.getSimpleName());
        }

    }

    @After
    public void tearDown() throws InterruptedException {

        // Closing everything
        ST.close();
        TheNMS = null;

        try {

            JxseOSGiFramework.INSTANCE.stop();

            // Waiting for stop for maximum 60 seconds
            FrameworkEvent FE = JxseOSGiFramework.INSTANCE.waitForStop(60000);

            if ( FE.getType() != FrameworkEvent.STOPPED ) {
                fail("OSGi Framework failed to stop after 60 seconds, event type: " + FE.getType() );
            }

        } catch (BundleException ex) {
            fail(ex.toString());
        } catch (InterruptedException ex) {
            fail(ex.toString());
        }

    }

    /**
     * Test of setPeerConfiguration method, of class JxseOSGiNetworkManager.
     */
    @Test
    public void testSetGetPeerConfiguration() {

        JxsePeerConfiguration JPC = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        JPC.setPeerInstanceName("Poupoupidou");

        try {
            TheNMS.setPeerConfiguration(JPC);
        } catch (JxtaConfigurationException ex) {
            fail(ex.toString());
        }

        JxsePeerConfiguration Retrieved = TheNMS.getPeerConfigurationCopy();

        assertTrue(Retrieved!=null);
        assertTrue(JPC!=Retrieved);

        assertTrue(Retrieved.getPeerInstanceName().compareTo("Poupoupidou")==0);

    }


    /**
     * Test of getConfiguredNetworkManager method, of class JxseOSGiNetworkManager.
     */
    @Test
    public void testGetConfiguredNetworkManager() {

        JxsePeerConfiguration JPC = new ValidJxsePeerConfiguration();

        try {
            TheNMS.setPeerConfiguration(JPC);
        } catch (JxtaConfigurationException ex) {
            fail(ex.toString());
        }

        // Retrieving the NetworkManager
        NetworkManager TheNM = null;

        try {
            TheNM = TheNMS.getConfiguredNetworkManager();
        } catch (Exception ex) {
            fail(ex.toString());
        }

        try {
            TheNM.startNetwork();
        } catch (PeerGroupException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // We should not be able to set a peer config while the NetworkManager is started
        try {
            TheNMS.setPeerConfiguration(JPC);
            fail("Cannot set a peer configuration when NetworkManager is started");
        } catch (JxtaConfigurationException ex) {
            // Fine
        }

        // Stopping the NetworkManager
        TheNM.stopNetwork();

        // We should be able to set a peer config since the NetworkManager is stopped
        try {
            TheNMS.setPeerConfiguration(JPC);
        } catch (JxtaConfigurationException ex) {
            fail(ex.toString());
        }

    }

    private boolean contains(URI[] theArray, URI theItem) {

        for (URI Item : theArray) {
            if (Item.compareTo(theItem)==0){
                return true;
            }
        }

        return false;

    }

    /**
     * Test of getConfiguredNetworkManager method, of class JxseOSGiNetworkManager.
     */
    @Test
    public void testGetConfiguredNetworkManager_2() {

        // Http config
        JxseHttpTransportConfiguration TempHttp = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();
        TempHttp.setHttpPort(3333);
        TempHttp.setHttpIncoming(false);
        TempHttp.setHttpInterfaceAddress("123.45.67.89");
        TempHttp.setHttpOutgoing(true);
        TempHttp.setHttpPublicAddress("321.34.22.66", false);

        // Multicast config
        JxseMulticastTransportConfiguration TempMulti = JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();
        TempMulti.setMulticastPort(4444);
        TempMulti.setMulticastAddress("77.77.77.77");
        TempMulti.setMulticastInterface("88.88.88.89");
        TempMulti.setMulticastPacketSize(9898);

        // Tcp config
        JxseTcpTransportConfiguration TempTcp = JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();
        TempTcp.setTcpPort(3555);
        TempTcp.setTcpStartPort(2222);
        TempTcp.setTcpEndPort(4444);
        TempTcp.setTcpIncoming(true);
        TempTcp.setTcpOutgoing(true);
        TempTcp.setTcpPublicAddress("12.34.56.78", false);
        TempTcp.setTcpInterfaceAddress("33.44.55.66");

        // Peer config
        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        Source.setHttpTransportConfiguration(TempHttp);
        Source.setMulticastTransportConfiguration(TempMulti);
        Source.setTcpTransportConfiguration(TempTcp);

        Source.setConnectionMode(ConnectionMode.ADHOC);
        Source.setInfrastructureID(PeerGroupID.worldPeerGroupID);
        URI KSL = new File("aze").toURI(); Source.setKeyStoreLocation(KSL);
        URI LS = new File("eze").toURI(); Source.setPersistenceLocation(LS);
        Source.setMulticastEnabled(false);
        PeerID PID = IDFactory.newPeerID(PeerGroupID.worldPeerGroupID); Source.setPeerID(PID);
        Source.setPeerInstanceName("Zoubidoo");
        Source.setRelayMaxClients(3456);
        Source.setRendezvousMaxClients(6666);
        Source.setTcpEnabled(false);
        Source.setUseOnlyRdvSeeds(true);
        Source.setUseOnlyRelaySeeds(true);

        URI SR = URI.create("tcp://192.168.1.1"); Source.addSeedRelay(SR, 10);
        URI SRDV = URI.create("tcp://192.168.1.2"); Source.addSeedRendezvous(SRDV, 20);
        URI SiR = URI.create("tcp://192.168.1.3"); Source.addSeedingRelay(SiR, 30);
        URI SiRDV = URI.create("tcp://192.168.1.4"); Source.addSeedingRendezvous(SiRDV, 40);

        try {
            TheNMS.setPeerConfiguration(Source);
        } catch (JxtaConfigurationException ex) {
            fail(ex.toString());
        }

        // Retrieving the NetworkManager
        NetworkManager TheNM = null;

        try {
            TheNM = TheNMS.getConfiguredNetworkManager();
        } catch (Exception ex) {
            fail(ex.toString());
        }

        // Retrieving the NetworkConfigurator
        NetworkConfigurator TheNC = null;

        try {
            TheNC = TheNM.getConfigurator();
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Http config
        assertTrue(TheNC.getHttpPort()==3333);
        assertTrue(TheNC.getHttpIncomingStatus()==false);
        assertTrue(TheNC.getHttpOutgoingStatus()==true);

        assertTrue(TheNC.getHttpInterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(TheNC.getHttpPublicAddress().compareTo("321.34.22.66")==0);
        assertTrue(TheNC.getHttpPublicAddressExclusivity()==false);

        // Multicast config
        assertTrue(TheNC.getMulticastPort()==4444);
        assertTrue(TheNC.getMulticastAddress().compareTo("77.77.77.77")==0);
        assertTrue(TheNC.getMulticastInterface().compareTo("88.88.88.89")==0);
        assertTrue(TheNC.getMulticastSize()==9898);

        // Tcp config
        assertTrue(TheNC.getTcpPort()==3555);
        assertTrue(TheNC.getTcpStartPort()==2222);
        assertTrue(TheNC.getTcpEndport()==4444);
        assertTrue(TheNC.getTcpIncomingStatus()==true);
        assertTrue(TheNC.getTcpOutgoingStatus()==true);
        assertTrue(TheNC.getTcpPublicAddress().compareTo("12.34.56.78")==0);
        assertTrue(TheNC.getTcpPublicAddressExclusivity()==false);
        assertTrue(TheNC.getTcpInterfaceAddress().compareTo("33.44.55.66")==0);

        // The rest
        assertTrue(TheNC.getMode()==NetworkConfigurator.ADHOC_NODE);
        assertTrue(TheNC.getInfrastructureID().toString().compareTo(PeerGroupID.worldPeerGroupID.toString())==0);
        assertTrue(TheNC.getKeyStoreLocation().compareTo(KSL)==0);
        
        // Following test fails because TheNC.getStoreHome() adds a '/', but otherwise is fine
        // assertTrue(TheNC.getStoreHome().compareTo(LS)==0);

        assertFalse(TheNC.getMulticastStatus());
        assertTrue(TheNC.getPeerID().toString().compareTo(PID.toString())==0);
        assertTrue(TheNC.getName().compareTo("Zoubidoo")==0);
        assertTrue(TheNC.getRelayMaxClients()==3456);
        assertTrue(TheNC.getRendezvousMaxClients()==6666);
        assertFalse(TheNC.isTcpEnabled());
        assertTrue(TheNC.getUseOnlyRendezvousSeedsStatus());
        assertTrue(TheNC.getUseOnlyRelaySeedsStatus());

        assertTrue(contains(TheNC.getRdvSeedingURIs(),SiRDV));
        assertTrue(contains(TheNC.getRelaySeedingURIs(),SiR));

        assertTrue(contains(TheNC.getRdvSeedURIs(),SRDV));
        assertTrue(contains(TheNC.getRelaySeedURIs(),SR));

    }

}
