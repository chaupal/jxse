
package tutorial.connectivitymonitoring;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.impl.rendezvous.edge.EdgePeerRdvService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.Module;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;

/**
 * Simple EDGE peer connecting via the NetPeerGroup.
 */
public class EDGE_A {

    // Static

    public static final String Name_EDGE_A = "EDGE A";
    public static final PeerID PID_EDGE_A = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name_EDGE_A.getBytes());
    public static final int TcpPort_EDGE_A = 9710;
    public static final File ConfigurationFile_EDGE_A = new File("." + System.getProperty("file.separator") + Name_EDGE_A);

    public static final String ChildPeerGroupName = "Child peer group";
    public static final PeerGroupID ChildPeerGroupID = IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, ChildPeerGroupName.getBytes());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {

            // Removing verbose
            // Logger.getLogger("net.jxta").setLevel(Level.WARNING);
            Logger.getLogger(EdgePeerRdvService.class.getName()).setLevel(Level.CONFIG);

            // Removing any existing configuration?
            NetworkManager.RecursiveDelete(ConfigurationFile_EDGE_A);

            // Creation of the network manager
            final NetworkManager MyNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.EDGE,
                    Name_EDGE_A, ConfigurationFile_EDGE_A.toURI());

            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();

            // Setting Configuration
            MyNetworkConfigurator.setTcpPort(TcpPort_EDGE_A);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);
            MyNetworkConfigurator.setUseMulticast(true);

            // Setting the Peer ID
            MyNetworkConfigurator.setPeerID(PID_EDGE_A);

            // Adding RDV a as a seed
            MyNetworkConfigurator.clearRendezvousSeeds();

            String TheSeed = "tcp://" + InetAddress.getLocalHost().getHostAddress() + ":"
                    + RENDEZVOUS_A.TcpPort_RDV_A;
            URI LocalRendezVousSeedURI = URI.create(TheSeed);
            MyNetworkConfigurator.addSeedRendezvous(LocalRendezVousSeedURI);

            TheSeed = "tcp://" + InetAddress.getLocalHost().getHostAddress() + ":"
                    + RENDEZVOUS_B.TcpPort_RDV_B;
            LocalRendezVousSeedURI = URI.create(TheSeed);
            MyNetworkConfigurator.addSeedRendezvous(LocalRendezVousSeedURI);

            // Starting the JXTA network
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();

            // Starting the connectivity monitor
            new ConnectivityMonitor(NetPeerGroup);

            // Disabling any rendezvous autostart
            NetPeerGroup.getRendezVousService().setAutoStart(false);

            // Retrieving a module implementation advertisement
            ModuleImplAdvertisement TheModuleImplementationAdvertisement = null;

            try {
                TheModuleImplementationAdvertisement = NetPeerGroup.getAllPurposePeerGroupImplAdvertisement();
            } catch (Exception ex) {
                System.err.println(ex.toString());
            }

            // Creating a child group
            PeerGroup ChildPeerGroup = NetPeerGroup.newGroup(
                    ChildPeerGroupID,
                    TheModuleImplementationAdvertisement,
                    ChildPeerGroupName,
                    "For test purposes..."
                    );

            if (Module.START_OK != ChildPeerGroup.startApp(new String[0]))
                System.err.println("Cannot start child peergroup");

            new ConnectivityMonitor(ChildPeerGroup);

            // Stopping the network asynchronously
            ConnectivityMonitor.TheExecutor.schedule(
                new DelayedJxtaNetworkStopper(
                    MyNetworkManager,
                    "Click to stop " + Name_EDGE_A,
                    "Stop"),
                0,
                TimeUnit.SECONDS);

        } catch (IOException Ex) {

            System.err.println(Ex.toString());

        } catch (PeerGroupException Ex) {

            System.err.println(Ex.toString());

        }

    }

}
