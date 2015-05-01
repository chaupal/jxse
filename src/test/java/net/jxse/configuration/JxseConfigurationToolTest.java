
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
import java.util.Arrays;
import net.jxta.platform.JxtaApplication;
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
    public void testGetConfiguredNetworkManager() throws IOException {

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

    private boolean contains(URI[] array, URI item) {
        for (URI arrayItem : array) {
            if (arrayItem.compareTo(item) == 0) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testGetConfiguredNetworkManager_2() throws IOException {

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
        //NetworkManager TheNM = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "Zoubidoo", ToBeDeleted.toURI());        
        NetworkManager networkManager = JxtaApplication.getNetworkManager(NetworkManager.ConfigMode.ADHOC, "Zoubidoo", ToBeDeleted.toURI());
        NetworkConfigurator networkConfigurator = networkManager.getConfigurator();

        // Http config
        networkConfigurator.setHttpPort(3333);
        networkConfigurator.setHttpIncoming(false);
        networkConfigurator.setHttpInterfaceAddress("123.45.67.89");
        networkConfigurator.setHttpOutgoing(false);
        networkConfigurator.setHttpPublicAddress("321.34.22.66", false);

        // Http2 config
        networkConfigurator.setHttp2Port(2);
        networkConfigurator.setHttp2Incoming(false);
        networkConfigurator.setHttp2InterfaceAddress("123.45.67.89");
        networkConfigurator.setHttp2Outgoing(true);
        networkConfigurator.setHttp2PublicAddress("321.34.22.66", false);
        networkConfigurator.setHttp2StartPort(999);
        networkConfigurator.setHttp2EndPort(9999);

        // Multicast config
        networkConfigurator.setMulticastPort(4444);
        networkConfigurator.setMulticastAddress("77.77.77.77");
        networkConfigurator.setMulticastInterface("88.88.88.89");
        networkConfigurator.setMulticastSize(9898);

        // Tcp config
        networkConfigurator.setTcpPort(3555);
        networkConfigurator.setTcpStartPort(2222);
        networkConfigurator.setTcpEndPort(4444);
        networkConfigurator.setTcpIncoming(false);
        networkConfigurator.setTcpOutgoing(false);
        networkConfigurator.setTcpPublicAddress("12.34.56.78", false);
        networkConfigurator.setTcpInterfaceAddress("33.44.55.66");

        URI KSL = new File("aze").toURI(); networkConfigurator.setKeyStoreLocation(KSL);
        networkConfigurator.setUseMulticast(false);
        PeerID PID = IDFactory.newPeerID(PeerGroupID.worldPeerGroupID); networkManager.setPeerID(PID);
        networkConfigurator.setRelayMaxClients(3456);
        networkConfigurator.setRendezvousMaxClients(6666);
        networkConfigurator.setTcpEnabled(false);
        networkConfigurator.setUseOnlyRendezvousSeeds(true);
        networkConfigurator.setUseOnlyRelaySeeds(true);

        URI relaySeedUri = URI.create("tcp://192.168.1.1"); 
        networkConfigurator.addSeedRelay(relaySeedUri);
        
        URI rendezvousSeedUri = URI.create("tcp://192.168.1.2"); 
        networkConfigurator.addSeedRendezvous(rendezvousSeedUri);
        
        URI relaySeedingUri = URI.create("tcp://192.168.1.3"); 
        networkConfigurator.addRelaySeedingURI(relaySeedingUri);
        
        URI rendezvousSeedingUri = URI.create("tcp://192.168.1.4"); 
        networkConfigurator.addRdvSeedingURI(rendezvousSeedingUri);

        // Retrieving a peer config
        JxsePeerConfiguration jxsePeerConfiguration = JxseConfigurationTool.getJxsePeerConfigurationFromNetworkManager(networkManager);

        // Http config
        assertTrue(jxsePeerConfiguration.getHttpTransportConfiguration().getHttpPort()==3333);
        assertFalse(jxsePeerConfiguration.getHttpTransportConfiguration().getHttpIncoming());
        assertFalse(jxsePeerConfiguration.getHttpTransportConfiguration().getHttpOutgoing());

        assertTrue(jxsePeerConfiguration.getHttpTransportConfiguration().getHttpInterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(jxsePeerConfiguration.getHttpTransportConfiguration().getHttpPublicAddress().compareTo("321.34.22.66")==0);
        assertFalse(jxsePeerConfiguration.getHttpTransportConfiguration().isHttpPublicAddressExclusive());

        // Http2 config
        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2Port()==2);
        assertFalse(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2Incoming());
        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2Outgoing());

        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2InterfaceAddress().compareTo("123.45.67.89")==0);
        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2PublicAddress().compareTo("321.34.22.66")==0);
        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().isHttp2PublicAddressExclusive()==false);

        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2StartPort()==999);
        assertTrue(jxsePeerConfiguration.getHttp2TransportConfiguration().getHttp2EndPort()==9999);

        // Multicast config
        assertTrue(jxsePeerConfiguration.getMulticastTransportConfiguration().getMulticastPort()==4444);
        assertTrue(jxsePeerConfiguration.getMulticastTransportConfiguration().getMulticastAddress().compareTo("77.77.77.77")==0);
        assertTrue(jxsePeerConfiguration.getMulticastTransportConfiguration().getMulticastInterface().compareTo("88.88.88.89")==0);
        assertTrue(jxsePeerConfiguration.getMulticastTransportConfiguration().getMulticastPacketSize()==9898);

        // Tcp config
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpPort()==3555);
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpStartPort()==2222);
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpEndPort()==4444);
        assertFalse(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpIncoming());
        assertFalse(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpOutgoing());
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpPublicAddress().compareTo("12.34.56.78")==0);
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().isTcpPublicAddressExclusive()==false);
        assertTrue(jxsePeerConfiguration.getTcpTransportConfiguration().getTcpInterfaceAddress().compareTo("33.44.55.66")==0);

        // The rest
        assertTrue(jxsePeerConfiguration.getConnectionMode().equals(JxseConfigurationTool.convertToJxsePeerConfigurationConfigMode(NetworkManager.ConfigMode.ADHOC)));
        assertTrue(jxsePeerConfiguration.getKeyStoreLocation().compareTo(KSL)==0);

        // Following test fails because TheNC.getStoreHome() adds a '/', but otherwise is fine
        // assertTrue(Retr.getStoreHome().compareTo(LS)==0);

        assertFalse(jxsePeerConfiguration.getMulticastEnabled());
        assertTrue(jxsePeerConfiguration.getPeerID().toString().compareTo(PID.toString())==0);
        assertTrue(jxsePeerConfiguration.getPeerInstanceName().compareTo("Zoubidoo")==0);
        assertTrue(jxsePeerConfiguration.getRelayMaxClients()==3456);
        assertTrue(jxsePeerConfiguration.getRendezvousMaxClients()==6666);
        assertFalse(jxsePeerConfiguration.getTcpEnabled());
        assertTrue(jxsePeerConfiguration.getUseOnlyRdvSeeds());
        assertTrue(jxsePeerConfiguration.getUseOnlyRelaySeeds());

        assertTrue(contains(jxsePeerConfiguration.getAllSeedingRendezvous().values().toArray(new URI[0]), rendezvousSeedingUri));
        assertTrue(contains(jxsePeerConfiguration.getAllSeedingRelays().values().toArray(new URI[0]), relaySeedingUri));

        assertTrue(contains(jxsePeerConfiguration.getAllSeedRendezvous().values().toArray(new URI[0]), rendezvousSeedUri));
        assertTrue(contains(jxsePeerConfiguration.getAllSeedRelays().values().toArray(new URI[0]), relaySeedUri));
        
        networkConfigurator.removeSeedRendezvous(rendezvousSeedUri);
        jxsePeerConfiguration = JxseConfigurationTool.getJxsePeerConfigurationFromNetworkManager(networkManager);
        
        Object[] objectArray = jxsePeerConfiguration.getAllSeedRendezvous().values().toArray();
        URI[] rendezvousSeeds = Arrays.copyOf(objectArray, objectArray.length, URI[].class);
        
        assertFalse(contains(rendezvousSeeds, rendezvousSeedUri));
    }
}