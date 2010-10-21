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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import net.jxse.configuration.JxsePeerConfiguration.ConnectionMode;
import net.jxta.id.IDFactory;
import net.jxta.id.TestIDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Testing the peer configuration object.
 */
public class JxsePeerConfigurationTest {

    public JxsePeerConfigurationTest() {
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
     * Test of set/getHttpTransportConfiguration method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetHttpTransportConfiguration() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getHttpTransportConfiguration()!=null);

        JxseHttpTransportConfiguration Temp = new JxseHttpTransportConfiguration();
        Temp.setHttpInterfaceAddress("ZZZ.RRR.TTT.EEE");
        Temp.setHttpPort(3245);

        Source.setHttpTransportConfiguration(Temp);
        JxseHttpTransportConfiguration Copy = Source.getHttpTransportConfiguration();

        assertTrue(Copy!=Temp);
        assertTrue(Copy.getHttpPort()==3245);
        assertTrue(Copy.getHttpInterfaceAddress().compareTo("ZZZ.RRR.TTT.EEE")==0);

        Source.setHttpTransportConfiguration(null);
        Copy = Source.getHttpTransportConfiguration();

        assertTrue(Copy.getHttpPort()!=3245);
        assertTrue(Copy.getHttpInterfaceAddress()==null);

    }

    /**
     * Test of set/getMulticastTransportConfiguration method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetMulticastTransportConfiguration() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getMulticastTransportConfiguration()!=null);

        JxseMulticastTransportConfiguration Temp = new JxseMulticastTransportConfiguration();
        Temp.setMulticastAddress("ZZZ.RRR.TTT.EEE");
        Temp.setMulticastPort(3245);

        Source.setMulticastTransportConfiguration(Temp);
        JxseMulticastTransportConfiguration Copy = Source.getMulticastTransportConfiguration();

        assertTrue(Copy!=Temp);
        assertTrue(Copy.getMulticastPort()==3245);
        assertTrue(Copy.getMulticastAddress().compareTo("ZZZ.RRR.TTT.EEE")==0);

        Source.setMulticastTransportConfiguration(null);
        Copy = Source.getMulticastTransportConfiguration();

        assertTrue(Copy.getMulticastPort()!=3245);
        assertTrue(Copy.getMulticastAddress()==null);

    }

    /**
     * Test of set/getTcpTransportConfiguration method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetTcpTransportConfiguration() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getMulticastTransportConfiguration()!=null);

        JxseTcpTransportConfiguration Temp = new JxseTcpTransportConfiguration();
        Temp.setTcpInterfaceAddress("ZZZ.RRR.TTT.EEE");
        Temp.setTcpPort(3245);

        Source.setTcpTransportConfiguration(Temp);
        JxseTcpTransportConfiguration Copy = Source.getTcpTransportConfiguration();

        assertTrue(Copy!=Temp);
        assertTrue(Copy.getTcpPort()==3245);
        assertTrue(Copy.getTcpInterfaceAddress().compareTo("ZZZ.RRR.TTT.EEE")==0);

        Source.setTcpTransportConfiguration(null);
        Copy = Source.getTcpTransportConfiguration();

        assertTrue(Copy.getTcpPort()!=3245);
        assertTrue(Copy.getTcpInterfaceAddress()==null);

    }

    /**
     * Test of set/getConnectionMode method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetConnectionMode() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getConnectionMode()==null);
        
        Source.setConnectionMode(ConnectionMode.ADHOC);
        assertTrue(Source.getConnectionMode().compareTo(ConnectionMode.ADHOC)==0);
        
        Source.setConnectionMode(null);
        assertTrue(Source.getConnectionMode()==null);
        
        Source.setConnectionMode(ConnectionMode.EDGE);
        assertTrue(Source.getConnectionMode().compareTo(ConnectionMode.EDGE)==0);

    }

    /**
     * Test of set/getInfrastructureID method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetInfrastructureID() {
        
        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getInfrastructureID()==null);

        PeerGroupID TempPID = IDFactory.newPeerGroupID();

        Source.setInfrastructureID(TempPID);
        assertTrue(Source.getInfrastructureID().toString().compareTo(TempPID.toString())==0);
        
        Source.setInfrastructureID(null);
        assertTrue(Source.getInfrastructureID()==null);
        
        PeerGroupID TempPID2 = IDFactory.newPeerGroupID();

        Source.setInfrastructureID(TempPID2);
        assertTrue(Source.getInfrastructureID().toString().compareTo(TempPID2.toString())==0);
        
    }

    /**
     * Test of set/getPeerInstanceName method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetPeerInstanceName() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getPeerInstanceName()==null);

        Source.setPeerInstanceName("Trulu");
        assertTrue(Source.getPeerInstanceName().compareTo("Trulu")==0);

        Source.setPeerInstanceName(null);
        assertTrue(Source.getPeerInstanceName()==null);

        Source.setPeerInstanceName("Trala");
        assertTrue(Source.getPeerInstanceName().compareTo("Trala")==0);

    }

    /**
     * Test of set/getPersistenceLocation method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetPersistenceLocation() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF1 = new File("xxx");
        URI LocStor1 = TempF1.toURI();

        assertTrue(Source.getPersistenceLocation()==null);

        Source.setPersistenceLocation(LocStor1);
        assertTrue(Source.getPersistenceLocation().toString().compareTo(LocStor1.toString())==0);

        Source.setPersistenceLocation(null);
        assertTrue(Source.getPersistenceLocation()==null);

        File TempF2 = new File("yyy");
        URI LocStor2 = TempF2.toURI();

        Source.setPersistenceLocation(LocStor2);
        assertTrue(Source.getPersistenceLocation().toString().compareTo(LocStor2.toString())==0);

        TempF1.delete();
        TempF2.delete();

    }

    /**
     * Test of set/getPeerID method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetPeerID() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getPeerID()==null);

        PeerID TempPID = TestIDFactory.newPeerID(PeerGroupID.worldPeerGroupID);

        Source.setPeerID(TempPID);
        assertTrue(Source.getPeerID().toString().compareTo(TempPID.toString())==0);

        Source.setPeerID(null);
        assertTrue(Source.getPeerID()==null);

        PeerID TempPID2 = TestIDFactory.newPeerID(PeerGroupID.worldPeerGroupID);

        Source.setPeerID(TempPID2);
        assertTrue(Source.getPeerID().toString().compareTo(TempPID2.toString())==0);

    }

    /**
     * Test of set/getKeyStoreLocation method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetKeyStoreLocation() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF1 = new File("ddd");
        URI LocStor1 = TempF1.toURI();

        assertTrue(Source.getKeyStoreLocation()==null);

        Source.setKeyStoreLocation(LocStor1);
        assertTrue(Source.getKeyStoreLocation().toString().compareTo(LocStor1.toString())==0);

        Source.setKeyStoreLocation(null);
        assertTrue(Source.getKeyStoreLocation()==null);

        File TempF2 = new File("ccc");
        URI LocStor2 = TempF2.toURI();

        Source.setPersistenceLocation(LocStor2);
        assertTrue(Source.getPersistenceLocation().toString().compareTo(LocStor2.toString())==0);

        TempF1.delete();
        TempF2.delete();

    }

    /**
     * Test of set/getRelayMaxClients method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetRelayMaxClients() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getRelayMaxClients()==-1);

        Source.setRelayMaxClients(33);
        assertTrue(Source.getRelayMaxClients()==33);

        Source.setRelayMaxClients(-1);
        assertTrue(Source.getRelayMaxClients()==-1);

        Source.setRelayMaxClients(44);
        assertTrue(Source.getRelayMaxClients()==44);
        
    }

    /**
     * Test of set/getRendezvousMaxClients method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetRendezvousMaxClients() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getRendezvousMaxClients()==-1);

        Source.setRendezvousMaxClients(33);
        assertTrue(Source.getRendezvousMaxClients()==33);

        Source.setRendezvousMaxClients(-1);
        assertTrue(Source.getRendezvousMaxClients()==-1);

        Source.setRendezvousMaxClients(44);
        assertTrue(Source.getRendezvousMaxClients()==44);

    }

    /**
     * Test of set/getTcpEnabled method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetTcpEnabled() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getTcpEnabled());

        Source.setTcpEnabled(false);
        assertFalse(Source.getTcpEnabled());

        Source.setTcpEnabled(true);
        assertTrue(Source.getTcpEnabled());

    }

    /**
     * Test of set/getMulticastEnabled method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetMulticastEnabled() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getMulticastEnabled());

        Source.setMulticastEnabled(false);
        assertFalse(Source.getMulticastEnabled());

        Source.setMulticastEnabled(true);
        assertTrue(Source.getMulticastEnabled());

    }

    /**
     * Test of set/getUseOnlyRelaySeeds method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetUseOnlyRelaySeeds() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertFalse(Source.getUseOnlyRelaySeeds());

        Source.setUseOnlyRelaySeeds(false);
        assertFalse(Source.getUseOnlyRelaySeeds());

        Source.setUseOnlyRelaySeeds(true);
        assertTrue(Source.getUseOnlyRelaySeeds());

    }

    /**
     * Test of set/getUseOnlyRdvSeeds method, of class JxsePeerConfiguration.
     */
    @Test
    public void testSetGetUseOnlyRdvSeeds() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertFalse(Source.getUseOnlyRdvSeeds());

        Source.setUseOnlyRdvSeeds(false);
        assertFalse(Source.getUseOnlyRdvSeeds());

        Source.setUseOnlyRdvSeeds(true);
        assertTrue(Source.getUseOnlyRdvSeeds());

    }

    /**
     * Test of add/getSeedRelay method, of class JxsePeerConfiguration.
     */
    @Test
    public void testAddGetSeedRelay() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getSeedRelay(10)==null);

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRelay(Item_10, 10);
        assertTrue(Source.getSeedRelay(10).compareTo(Item_10)==0);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRelay(Item_20, 20);
        assertTrue(Source.getSeedRelay(20).compareTo(Item_20)==0);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Source.addSeedRelay(Item_30, 10);
        assertTrue(Source.getSeedRelay(10).compareTo(Item_30)==0);

        Source.addSeedRelay(null, 10);
        assertTrue(Source.getSeedRelay(10)==null);

    }

    /**
     * Test of getAllSeedRelays method, of class JxsePeerConfiguration.
     */
    @Test
    public void testGetAllSeedRelays() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRelay(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRelay(Item_20, 20);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Map<Integer, URI> All = Source.getAllSeedRelays();

        assertTrue(All.containsKey(10));
        assertTrue(All.containsValue(Item_10));
        assertTrue(All.get(10).compareTo(Item_10)==0);

        assertTrue(All.containsKey(20));
        assertTrue(All.containsValue(Item_20));
        assertTrue(All.get(20).compareTo(Item_20)==0);

        assertFalse(All.containsKey(30));
        assertFalse(All.containsValue(Item_30));
        assertTrue(All.get(30)==null);

        assertTrue(All.size()==2);

    }

    /**
     * Test of clearSeedRelays method, of class JxsePeerConfiguration.
     */
    @Test
    public void testClearSeedRelays() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRelay(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRelay(Item_20, 20);

        assertTrue(Source.getSeedRelay(10).compareTo(Item_10)==0);
        assertTrue(Source.getSeedRelay(20).compareTo(Item_20)==0);
        assertTrue(Source.size()==2);

        Source.clearSeedRelays();

        assertTrue(Source.getSeedRelay(10)==null);
        assertTrue(Source.getSeedRelay(20)==null);
        assertTrue(Source.size()==0);

    }

    /**
     * Test of add/getSeedRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testAddGetSeedRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getSeedRendezvous(10)==null);

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRendezvous(Item_10, 10);
        assertTrue(Source.getSeedRendezvous(10).compareTo(Item_10)==0);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRendezvous(Item_20, 20);
        assertTrue(Source.getSeedRendezvous(20).compareTo(Item_20)==0);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Source.addSeedRendezvous(Item_30, 10);
        assertTrue(Source.getSeedRendezvous(10).compareTo(Item_30)==0);

        Source.addSeedRendezvous(null, 10);
        assertTrue(Source.getSeedRendezvous(10)==null);

    }

    /**
     * Test of getAllSeedRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testGetAllSeedRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRendezvous(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRendezvous(Item_20, 20);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Map<Integer, URI> All = Source.getAllSeedRendezvous();

        assertTrue(All.containsKey(10));
        assertTrue(All.containsValue(Item_10));
        assertTrue(All.get(10).compareTo(Item_10)==0);

        assertTrue(All.containsKey(20));
        assertTrue(All.containsValue(Item_20));
        assertTrue(All.get(20).compareTo(Item_20)==0);

        assertFalse(All.containsKey(30));
        assertFalse(All.containsValue(Item_30));
        assertTrue(All.get(30)==null);

        assertTrue(All.size()==2);

    }

    /**
     * Test of clearSeedRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testClearSeedRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedRendezvous(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedRendezvous(Item_20, 20);

        assertTrue(Source.getSeedRendezvous(10).compareTo(Item_10)==0);
        assertTrue(Source.getSeedRendezvous(20).compareTo(Item_20)==0);
        assertTrue(Source.size()==2);

        Source.clearSeedRendezvous();

        assertTrue(Source.getSeedRendezvous(10)==null);
        assertTrue(Source.getSeedRendezvous(20)==null);
        assertTrue(Source.size()==0);

    }

    /**
     * Test of add/getSeedingRelay method, of class JxsePeerConfiguration.
     */
    @Test
    public void testAddGetSeedingRelay() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getSeedingRelay(10)==null);

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRelay(Item_10, 10);
        assertTrue(Source.getSeedingRelay(10).compareTo(Item_10)==0);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRelay(Item_20, 20);
        assertTrue(Source.getSeedingRelay(20).compareTo(Item_20)==0);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Source.addSeedingRelay(Item_30, 10);
        assertTrue(Source.getSeedingRelay(10).compareTo(Item_30)==0);

        Source.addSeedingRelay(null, 10);
        assertTrue(Source.getSeedingRelay(10)==null);

    }

    /**
     * Test of getAllSeedingRelays method, of class JxsePeerConfiguration.
     */
    @Test
    public void testGetAllSeedingRelays() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRelay(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRelay(Item_20, 20);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Map<Integer, URI> All = Source.getAllSeedingRelays();

        assertTrue(All.containsKey(10));
        assertTrue(All.containsValue(Item_10));
        assertTrue(All.get(10).compareTo(Item_10)==0);

        assertTrue(All.containsKey(20));
        assertTrue(All.containsValue(Item_20));
        assertTrue(All.get(20).compareTo(Item_20)==0);

        assertFalse(All.containsKey(30));
        assertFalse(All.containsValue(Item_30));
        assertTrue(All.get(30)==null);

        assertTrue(All.size()==2);

    }

    /**
     * Test of clearSeedingRelays method, of class JxsePeerConfiguration.
     */
    @Test
    public void testClearSeedingRelays() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRelay(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRelay(Item_20, 20);

        assertTrue(Source.getSeedingRelay(10).compareTo(Item_10)==0);
        assertTrue(Source.getSeedingRelay(20).compareTo(Item_20)==0);
        assertTrue(Source.size()==2);

        Source.clearSeedingRelays();

        assertTrue(Source.getSeedingRelay(10)==null);
        assertTrue(Source.getSeedingRelay(20)==null);
        assertTrue(Source.size()==0);

    }

    /**
     * Test of add/getSeedingRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testAddGetSeedingRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        assertTrue(Source.getSeedingRendezvous(10)==null);

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRendezvous(Item_10, 10);
        assertTrue(Source.getSeedingRendezvous(10).compareTo(Item_10)==0);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRendezvous(Item_20, 20);
        assertTrue(Source.getSeedingRendezvous(20).compareTo(Item_20)==0);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Source.addSeedingRendezvous(Item_30, 10);
        assertTrue(Source.getSeedingRendezvous(10).compareTo(Item_30)==0);

        Source.addSeedingRendezvous(null, 10);
        assertTrue(Source.getSeedingRendezvous(10)==null);

    }

    /**
     * Test of getAllSeedingRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testGetAllSeedingRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRendezvous(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRendezvous(Item_20, 20);

        File TempF30 = new File("30");
        URI Item_30 = TempF30.toURI();

        Map<Integer, URI> All = Source.getAllSeedingRendezvous();

        assertTrue(All.containsKey(10));
        assertTrue(All.containsValue(Item_10));
        assertTrue(All.get(10).compareTo(Item_10)==0);

        assertTrue(All.containsKey(20));
        assertTrue(All.containsValue(Item_20));
        assertTrue(All.get(20).compareTo(Item_20)==0);

        assertFalse(All.containsKey(30));
        assertFalse(All.containsValue(Item_30));
        assertTrue(All.get(30)==null);

        assertTrue(All.size()==2);

    }

    /**
     * Test of clearSeedingRendezvous method, of class JxsePeerConfiguration.
     */
    @Test
    public void testClearSeedingRendezvous() {

        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        File TempF10 = new File("10");
        URI Item_10 = TempF10.toURI();

        Source.addSeedingRendezvous(Item_10, 10);

        File TempF20 = new File("20");
        URI Item_20 = TempF20.toURI();

        Source.addSeedingRendezvous(Item_20, 20);

        assertTrue(Source.getSeedingRendezvous(10).compareTo(Item_10)==0);
        assertTrue(Source.getSeedingRendezvous(20).compareTo(Item_20)==0);
        assertTrue(Source.size()==2);

        Source.clearSeedingRendezvous();

        assertTrue(Source.getSeedingRendezvous(10)==null);
        assertTrue(Source.getSeedingRendezvous(20)==null);
        assertTrue(Source.size()==0);

    }

    /**
     * Test of loadFromXML method, of class JxsePeerConfiguration.
     */
    @Test
    public void testLoadStoreFromToXML() {

        // Http config
        JxseHttpTransportConfiguration TempHttp = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();
        TempHttp.setHttpPort(3333);

        // Multicast config
        JxseMulticastTransportConfiguration TempMulti = JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();
        TempMulti.setMulticastPort(4444);

        // Tcp config
        JxseTcpTransportConfiguration TempTcp = JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();
        TempTcp.setTcpPort(5555);

        // Peer config
        JxsePeerConfiguration Source = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

        Source.setHttpTransportConfiguration(TempHttp);
        Source.setMulticastTransportConfiguration(TempMulti);
        Source.setTcpTransportConfiguration(TempTcp);

        Source.setConnectionMode(ConnectionMode.RENDEZVOUS);
        Source.setInfrastructureID(PeerGroupID.worldPeerGroupID);
        URI KSL = new File("aze").toURI(); Source.setKeyStoreLocation(KSL);
        URI LS = new File("eze").toURI(); Source.setPersistenceLocation(LS);
        Source.setMulticastEnabled(false);
        PeerID PID = TestIDFactory.newPeerID(PeerGroupID.worldPeerGroupID); Source.setPeerID(PID);
        Source.setPeerInstanceName("Zoubidoo");
        Source.setRelayMaxClients(3456);
        Source.setRendezvousMaxClients(6666);
        Source.setTcpEnabled(false);
        Source.setUseOnlyRdvSeeds(true);
        Source.setUseOnlyRelaySeeds(true);
        URI SR = new File("eze").toURI(); Source.addSeedRelay(SR, 10);
        URI SRDV = new File("zze").toURI(); Source.addSeedRendezvous(SRDV, 20);
        URI SiR = new File("fdc").toURI(); Source.addSeedingRelay(SiR, 30);
        URI SiRDV = new File("flc").toURI(); Source.addSeedingRendezvous(SiRDV, 40);

        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

        try {
            Source.storeToXML(BAOS, "Test");
        } catch (IOException ex) {
            fail(ex.toString());
        }

        ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());

        JxsePeerConfiguration Restore = new JxsePeerConfiguration();

        try {
            Restore.loadFromXML(BAIS);
        } catch (InvalidPropertiesFormatException ex) {
            fail(ex.toString());
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Http config
        TempHttp = Restore.getHttpTransportConfiguration();
        assertTrue(TempHttp.getHttpPort()==3333);

        // Multicast config
        TempMulti = Restore.getMulticastTransportConfiguration();
        assertTrue(TempMulti.getMulticastPort()==4444);

        // Tcp config
        TempTcp = Restore.getTcpTransportConfiguration();
        assertTrue(TempTcp.getTcpPort()==5555);


        assertTrue(Restore.getConnectionMode().compareTo(ConnectionMode.RENDEZVOUS)==0);
        assertTrue(Restore.getInfrastructureID().toString().compareTo(PeerGroupID.worldPeerGroupID.toString())==0);
        assertTrue(Restore.getKeyStoreLocation().compareTo(KSL)==0);
        assertTrue(Restore.getPersistenceLocation().compareTo(LS)==0);
        assertFalse(Restore.getMulticastEnabled());
        assertTrue(Restore.getPeerID().toString().compareTo(PID.toString())==0);
        assertTrue(Restore.getPeerInstanceName().compareTo("Zoubidoo")==0);
        assertTrue(Restore.getRelayMaxClients()==3456);
        assertTrue(Restore.getRendezvousMaxClients()==6666);
        assertFalse(Restore.getTcpEnabled());
        assertTrue(Restore.getUseOnlyRdvSeeds());
        assertTrue(Restore.getUseOnlyRelaySeeds());
        assertTrue(Restore.getSeedRelay(10).compareTo(SR)==0);
        assertTrue(Restore.getSeedRendezvous(20).compareTo(SRDV)==0);
        assertTrue(Restore.getSeedingRelay(30).compareTo(SiR)==0);
        assertTrue(Restore.getSeedingRendezvous(40).compareTo(SiRDV)==0);

        Properties Defaults = Restore.getDefaultsCopy();

        assertTrue(Defaults!=null);
        assertTrue(Defaults.size()==4);


        // Enabling TCP and Multicasting
        assertTrue(Defaults.containsKey("JXSE_TCP_ENABLED"));
        assertTrue(Defaults.containsKey("JXSE_MULTICAST_ENABLED"));

        // Use seeds only
        assertTrue(Defaults.containsKey("JXSE_USE_ONLY_RELAY_SEED"));
        assertTrue(Defaults.containsKey("JXSE_USE_ONLY_RDV_SEED"));

    }

}
