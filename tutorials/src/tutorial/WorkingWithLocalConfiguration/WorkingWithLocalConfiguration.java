package tutorial.WorkingWithLocalConfiguration;

import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

import java.text.MessageFormat;
import java.io.File;
import java.io.IOException;
import javax.security.cert.CertificateException;
import javax.swing.JOptionPane;

public class WorkingWithLocalConfiguration {

    public static final String Local_Peer_Name = "My Local Peer";
    public static final String Local_Network_Manager_Name = "My Local Network Manager";

    NetworkManager TheNetworkManager;
    NetworkConfigurator TheConfig;
    PeerGroup TheNetPeerGroup;

    public WorkingWithLocalConfiguration() {
        // Creating the Network Manager
        try {
            System.out.println("Creating the Network Manager");
            TheNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.EDGE, Local_Network_Manager_Name);
            System.out.println("Network Manager created");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        // Persisting it to make sure the Peer ID is not re-created each
        // time the Network Manager is instantiated
        TheNetworkManager.setConfigPersistent(true);
        
        System.out.println("PeerID: " + TheNetworkManager.getPeerID().toString());
        
        // Since we won't be setting our own relay or rendezvous seed peers we
        // will use the default (public network) relay and rendezvous seeding.
        TheNetworkManager.setUseDefaultSeeds(true);

        // Retrieving the Network Configurator
        System.out.println("Retrieving the Network Configurator");
        try {
            TheConfig = TheNetworkManager.getConfigurator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Network Configurator retrieved");

        // Does a local peer configuration exist?
        if (TheConfig.exists()) {

            System.out.println("Local configuration found");
            // We load it
            File LocalConfig = new File(TheConfig.getHome(), "PlatformConfig");
            try {
                System.out.println("Loading found configuration");
                TheConfig.load(LocalConfig.toURI());
                System.out.println("Configuration loaded");
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            } catch (CertificateException ex) {
                // An issue with the existing peer certificate has been encountered
                ex.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("No local configuration found");
            TheConfig.setName(Local_Peer_Name);
            TheConfig.setPrincipal(GetPrincipal());
            TheConfig.setPassword(GetPassword());

            System.out.println("Principal: " + TheConfig.getPrincipal());
            System.out.println("Password : " + TheConfig.getPassword());

            try {
                System.out.println("Saving new configuration");
                TheConfig.save();
                System.out.println("New configuration saved successfully");
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private String GetPrincipal() {
        return (String) JOptionPane.showInputDialog(
                null, "Enter principal", "Principal", JOptionPane.QUESTION_MESSAGE,
                null, null, "");
    }

    private String GetPassword() {
        return (String) JOptionPane.showInputDialog(
                null, "Enter password", "Password", JOptionPane.QUESTION_MESSAGE,
                null, null, "");
    }

    public void SeekRendezVousConnection() {
        try {
            System.out.println("Starting JXTA");
            TheNetPeerGroup = TheNetworkManager.startNetwork();
            System.out.println("JXTA Started");

            System.out.println("Peer name      : "
                    + TheNetPeerGroup.getPeerName());
            System.out.println("Peer Group name: "
                    + TheNetPeerGroup.getPeerGroupName());
            System.out.println("Peer Group ID  : "
                    + TheNetPeerGroup.getPeerID().toString());

        } catch (PeerGroupException ex) {
            // Cannot initialize peer group
            ex.printStackTrace();
            System.exit(-1);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Waiting for a rendezvous connection for 25 seconds � + �(maximum)");
        boolean connected = TheNetworkManager.waitForRendezvousConnection(25000);
        System.out.println(MessageFormat.format("Connected :{0}", connected));
        System.out.println("Stopping JXTA");
        TheNetworkManager.stopNetwork();

    }

    public static void main(String[] args) {
        WorkingWithLocalConfiguration MyLogin = new WorkingWithLocalConfiguration();
        MyLogin.SeekRendezVousConnection();
    }
}