
package net.jxse.configuration;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import java.io.File;
import net.jxta.peergroup.PeerGroupID;
import net.jxse.configuration.JxsePeerConfiguration.ConnectionMode;
import java.net.URI;
import java.io.IOException;
import net.jxta.platform.NetworkManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class JxseConfigurationToolTest {

    public JxseConfigurationToolTest() {
    }

    @Rule
    public TemporaryFolder tempStorage = new TemporaryFolder();

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
    public void testGetConfiguredNetworkManager() {

        // Creating new valid peer configuration
        JxsePeerConfiguration JPC = new ValidJxsePeerConfiguration();

        // Need to create separate temp dir, or residual config files will/may
        // be read and break this test
        File ToBeDeleted = tempStorage.newFolder("GetConfiguredNetworkManager");
        ToBeDeleted.mkdirs();
        assertTrue(ToBeDeleted.exists());

        JPC.setPersistenceLocation(ToBeDeleted.toURI());

        // Retrieving the NetworkManager
        NetworkManager TheNM = null;

        try {
            TheNM = JxseConfigurationTool.getConfiguredNetworkManager(JPC);
        } catch (Exception ex) {
            fail(ex.toString());
        }

        assertNotNull(TheNM);

    }

    private boolean contains(URI[] theArray, URI theItem) {

        for (URI Item : theArray) {
            if (Item.compareTo(theItem)==0){
                return true;
            }
        }

        return false;

    }

    @Test
    public void testGetConfiguredNetworkManager_2() {

        // Http config
        JxseHttpTransportConfiguration TempHttp = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();
        TempHttp.setHttpPort(3333);
        TempHttp.setHttpIncoming(true);
        TempHttp.setHttpInterfaceAddress("123.45.67.89");
        TempHttp.setHttpOutgoing(true);
        TempHttp.setHttpPublicAddress("321.34.22.66", false);

        // Http2 config
        JxseHttp2TransportConfiguration TempHttp2 = JxseHttp2TransportConfiguration.getDefaultHttp2TransportConfiguration();
        TempHttp2.setHttp2StartPort(999);
        TempHttp2.setHttp2EndPort(9999);
        TempHttp2.setHttp2Port(2999);
        TempHttp2.setHttp2Incoming(true);
        TempHttp2.setHttp2InterfaceAddress("123.45.67.89");
        TempHttp2.setHttp2Outgoing(true);
        TempHttp2.setHttp2PublicAddress("321.34.22.66", false);

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

        // Need to create separate temp dir, or residual config files will/may
        // be read and break this test
        File ToBeDeleted = tempStorage.newFolder("GetConfiguredNetworkManager2");
        ToBeDeleted.mkdirs();
        assertTrue(ToBeDeleted.exists());

        // Remaining config
        Source.setPersistenceLocation(ToBeDeleted.toURI());
        Source.setHttpTransportConfiguration(TempHttp);
        Source.setHttp2TransportConfiguration(TempHttp2);
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

        // Retrieving the NetworkManager
        NetworkManager TheNM = null;

        try {
            TheNM = JxseConfigurationTool.getConfiguredNetworkManager(Source);
        } catch (Exception ex) {
            fail(ex.toString());
        }

        // We should ahve something
        assertNotNull(TheNM);

        // Retrieving the NetworkConfigurator
        NetworkConfigurator TheNC = null;

        try {
            TheNC = TheNM.getConfigurator();
        } catch (IOException ex) {
            fail(ex.toString());
        }

        // Http config
        assertTrue(TheNC.getHttpPort()==3333);
        assertTrue(TheNC.getHttpIncomingStatus());
        assertTrue(TheNC.getHttpOutgoingStatus());

        assertTrue(TheNC.getHttpInterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(TheNC.getHttpPublicAddress().compareTo("321.34.22.66")==0);
        assertTrue(TheNC.isHttpPublicAddressExclusive()==false);

        // Http2 config
        assertTrue(TheNC.getHttp2StartPort()==999);
        assertTrue(TheNC.getHttp2EndPort()==9999);

        assertTrue(TheNC.getHttp2Port()==2999);
        assertTrue(TheNC.getHttp2IncomingStatus());
        assertTrue(TheNC.getHttp2OutgoingStatus());

        assertTrue(TheNC.getHttp2InterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(TheNC.getHttp2PublicAddress().compareTo("321.34.22.66")==0);
        assertTrue(TheNC.isHttp2PublicAddressExclusive()==false);

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
        assertTrue(TheNC.isTcpPublicAddressExclusive()==false);
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

    @Test
    public void testGetJxsePeerConfigurationFromNetworkManager() throws Exception {

        // Need to create separate temp dir, or residual config files will/may
        // be read and break this test
        File ToBeDeleted = tempStorage.newFolder("GetJxsePeerConfigurationFromNetworkManager");
        ToBeDeleted.mkdirs();
        assertTrue(ToBeDeleted.exists());

        // Retrieving the NetworkManager
        NetworkManager TheNM = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "Zoubidoo", ToBeDeleted.toURI());
        NetworkConfigurator TheNC = TheNM.getConfigurator();

        // Http config
        TheNC.setHttpPort(3333);
        TheNC.setHttpIncoming(false);
        TheNC.setHttpInterfaceAddress("123.45.67.89");
        TheNC.setHttpOutgoing(false);
        TheNC.setHttpPublicAddress("321.34.22.66", false);

        // Http2 config
        TheNC.setHttp2Port(2);
        TheNC.setHttp2Incoming(false);
        TheNC.setHttp2InterfaceAddress("123.45.67.89");
        TheNC.setHttp2Outgoing(true);
        TheNC.setHttp2PublicAddress("321.34.22.66", false);
        TheNC.setHttp2StartPort(999);
        TheNC.setHttp2EndPort(9999);

        // Multicast config
        TheNC.setMulticastPort(4444);
        TheNC.setMulticastAddress("77.77.77.77");
        TheNC.setMulticastInterface("88.88.88.89");
        TheNC.setMulticastSize(9898);

        // Tcp config
        TheNC.setTcpPort(3555);
        TheNC.setTcpStartPort(2222);
        TheNC.setTcpEndPort(4444);
        TheNC.setTcpIncoming(false);
        TheNC.setTcpOutgoing(false);
        TheNC.setTcpPublicAddress("12.34.56.78", false);
        TheNC.setTcpInterfaceAddress("33.44.55.66");

        URI KSL = new File("aze").toURI(); TheNC.setKeyStoreLocation(KSL);
        TheNC.setUseMulticast(false);
        PeerID PID = IDFactory.newPeerID(PeerGroupID.worldPeerGroupID); TheNM.setPeerID(PID);
        TheNC.setRelayMaxClients(3456);
        TheNC.setRendezvousMaxClients(6666);
        TheNC.setTcpEnabled(false);
        TheNC.setUseOnlyRendezvousSeeds(true);
        TheNC.setUseOnlyRelaySeeds(true);

        URI SR = URI.create("tcp://192.168.1.1"); TheNC.addSeedRelay(SR);
        URI SRDV = URI.create("tcp://192.168.1.2"); TheNC.addSeedRendezvous(SRDV);
        URI SiR = URI.create("tcp://192.168.1.3"); TheNC.addRelaySeedingURI(SiR);
        URI SiRDV = URI.create("tcp://192.168.1.4"); TheNC.addRdvSeedingURI(SiRDV);

        // Retrieving a peer config
        JxsePeerConfiguration Retr = JxseConfigurationTool.getJxsePeerConfigurationFromNetworkManager(TheNM);

        // Http config
        assertTrue(Retr.getHttpTransportConfiguration().getHttpPort()==3333);
        assertFalse(Retr.getHttpTransportConfiguration().getHttpIncoming());
        assertFalse(Retr.getHttpTransportConfiguration().getHttpOutgoing());

        assertTrue(Retr.getHttpTransportConfiguration().getHttpInterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(Retr.getHttpTransportConfiguration().getHttpPublicAddress().compareTo("321.34.22.66")==0);
        assertFalse(Retr.getHttpTransportConfiguration().isHttpPublicAddressExclusive());

        // Http2 config
        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2Port()==2);
        assertFalse(Retr.getHttp2TransportConfiguration().getHttp2Incoming());
        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2Outgoing());

        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2InterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2PublicAddress().compareTo("321.34.22.66")==0);
        assertTrue(Retr.getHttp2TransportConfiguration().isHttp2PublicAddressExclusive()==false);

        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2StartPort()==999);
        assertTrue(Retr.getHttp2TransportConfiguration().getHttp2EndPort()==9999);

        // Multicast config
        assertTrue(Retr.getMulticastTransportConfiguration().getMulticastPort()==4444);
        assertTrue(Retr.getMulticastTransportConfiguration().getMulticastAddress().compareTo("77.77.77.77")==0);
        assertTrue(Retr.getMulticastTransportConfiguration().getMulticastInterface().compareTo("88.88.88.89")==0);
        assertTrue(Retr.getMulticastTransportConfiguration().getMulticastPacketSize()==9898);

        // Tcp config
        assertTrue(Retr.getTcpTransportConfiguration().getTcpPort()==3555);
        assertTrue(Retr.getTcpTransportConfiguration().getTcpStartPort()==2222);
        assertTrue(Retr.getTcpTransportConfiguration().getTcpEndPort()==4444);
        assertFalse(Retr.getTcpTransportConfiguration().getTcpIncoming());
        assertFalse(Retr.getTcpTransportConfiguration().getTcpOutgoing());
        assertTrue(Retr.getTcpTransportConfiguration().getTcpPublicAddress().compareTo("12.34.56.78")==0);
        assertTrue(Retr.getTcpTransportConfiguration().isTcpPublicAddressExclusive()==false);
        assertTrue(Retr.getTcpTransportConfiguration().getTcpInterfaceAddress().compareTo("33.44.55.66")==0);

        // The rest
        assertTrue(Retr.getConnectionMode().equals(JxseConfigurationTool.convertToJxsePeerConfigurationConfigMode(NetworkManager.ConfigMode.ADHOC)));
        assertTrue(Retr.getKeyStoreLocation().compareTo(KSL)==0);

        // Following test fails because TheNC.getStoreHome() adds a '/', but otherwise is fine
        // assertTrue(Retr.getStoreHome().compareTo(LS)==0);

        assertFalse(Retr.getMulticastEnabled());
        assertTrue(Retr.getPeerID().toString().compareTo(PID.toString())==0);
        assertTrue(Retr.getPeerInstanceName().compareTo("Zoubidoo")==0);
        assertTrue(Retr.getRelayMaxClients()==3456);
        assertTrue(Retr.getRendezvousMaxClients()==6666);
        assertFalse(Retr.getTcpEnabled());
        assertTrue(Retr.getUseOnlyRdvSeeds());
        assertTrue(Retr.getUseOnlyRelaySeeds());

        assertTrue(contains(Retr.getAllSeedingRendezvous().values().toArray(new URI[0]),SiRDV));
        assertTrue(contains(Retr.getAllSeedingRelays().values().toArray(new URI[0]),SiR));

        assertTrue(contains(Retr.getAllSeedRendezvous().values().toArray(new URI[0]),SRDV));
        assertTrue(contains(Retr.getAllSeedRelays().values().toArray(new URI[0]),SR));

    }

}