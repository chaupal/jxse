
package tutorial.connectivitymonitoring;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.Module;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;

/**
 * Simple RENDEZVOUS peer connecting via the NetPeerGroup.
 */
public class RENDEZVOUS_A {

    // Static

    public static final String Name_RDV_A = "RENDEZVOUS A";
    public static final PeerID PID_RDV_A = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name_RDV_A.getBytes());
    public static final int TcpPort_RDV_A = 9711;
    public static final File ConfigurationFile_RDV_A = new File("." + System.getProperty("file.separator") + Name_RDV_A);

    public static final String ChildPeerGroupName = "Child peer group";
    public static final PeerGroupID ChildPeerGroupID = IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, ChildPeerGroupName.getBytes());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {

            // Removing verbose
            Logger.getLogger("net.jxta").setLevel(Level.WARNING);

            // Removing any existing configuration?
            NetworkManager.RecursiveDelete(ConfigurationFile_RDV_A);

            // Creation of the network manager
            final NetworkManager MyNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.RENDEZVOUS,
                    Name_RDV_A, ConfigurationFile_RDV_A.toURI());

            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();

            // Setting Configuration
            MyNetworkConfigurator.setTcpPort(TcpPort_RDV_A);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);
            MyNetworkConfigurator.setUseMulticast(true);

            // Setting the Peer ID
            MyNetworkConfigurator.setPeerID(PID_RDV_A);

            // Starting the JXTA network
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();

            // Starting the connectivity monitor
            new ConnectivityMonitor(NetPeerGroup);

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

            // Enable rendezvous
            ChildPeerGroup.getRendezVousService().startRendezVous();

            new ConnectivityMonitor(ChildPeerGroup);

            // Stopping the network asynchronously
            ConnectivityMonitor.TheExecutor.schedule(
                new DelayedJxtaNetworkStopper(
                    MyNetworkManager,
                    "Click to stop " + Name_RDV_A,
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
