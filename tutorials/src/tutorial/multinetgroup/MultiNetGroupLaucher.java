/*
 * MultiNetGroupLaucher.java
 *
 * Created on July 17, 2007, 9:30 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package tutorial.multinetgroup;

import java.io.File;
import java.net.URI;
import java.util.Map;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.platform.ModuleClassID;

import net.jxta.impl.endpoint.mcast.McastTransport;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;

/**
 *
 * @author mike
 */
public class MultiNetGroupLaucher {
    
    private final static String PEER_NAME = "Peer";
    
    /**
     * main
     *
     * @param args command line arguments
     */
    public static void main(String args[]) throws Exception {
        final URI storeHome = new File(".jxta/").toURI();
        
        // Configure the world peer group.
        
        NetworkConfigurator worldGroupConfig = NetworkConfigurator.newAdHocConfiguration(storeHome);
        
        PeerID peerid = tutorial.id.IDTutorial.createPeerID( PeerGroupID.worldPeerGroupID, PEER_NAME );
        worldGroupConfig.setName("Peer");
        worldGroupConfig.setPeerID(peerid);
        worldGroupConfig.setUseMulticast(false);
        
        // Instantiate the world peer group
        
        WorldPeerGroupFactory wpgf = new WorldPeerGroupFactory(worldGroupConfig.getPlatformConfig(), storeHome);
        
        PeerGroup wpg = wpgf.getInterface();
        
        System.out.println("World Peer Group : " + wpg + " started!");
        
        // Create the ModuleImplAdvertisement for the Net Peer Groups. (Adds multicast)
        
        ModuleImplAdvertisement npgImplAdv = wpg.getAllPurposePeerGroupImplAdvertisement();
        npgImplAdv.setModuleSpecID(PeerGroup.allPurposePeerGroupSpecID);
        
        
        StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(npgImplAdv.getParam());

        params.addProto(McastTransport.MCAST_TRANSPORT_CLASSID, McastTransport.MCAST_TRANSPORT_SPECID);
        
        npgImplAdv.setParam((XMLDocument) params.getDocument(MimeMediaType.XMLUTF8));
        
        // Configure for cluster 1
        
        NetworkConfigurator cluster1GroupConfig = NetworkConfigurator.newAdHocConfiguration(storeHome);
        
        PeerGroupID cluster1id = tutorial.id.IDTutorial.createPeerGroupID("cluster1");
        cluster1GroupConfig.setInfrastructureID(cluster1id);
        cluster1GroupConfig.setName("Peer");
        cluster1GroupConfig.setPeerID(peerid);
        cluster1GroupConfig.setUseMulticast(true);
        int cluster1multicastport = 1025 + ("cluster1".hashCode() % 60000);
        cluster1GroupConfig.setMulticastPort(cluster1multicastport);
        
        // Instantiate cluster 1 net peer group
        
        NetPeerGroupFactory npgf1 = new NetPeerGroupFactory(wpg, cluster1GroupConfig.getPlatformConfig(), npgImplAdv);
        
        PeerGroup cluster1 = npgf1.getInterface();
        
        System.out.println("Cluster 1 Peer Group : " + cluster1 + " started!");
        
        // Configure for cluster 2
        
        NetworkConfigurator cluster2GroupConfig = NetworkConfigurator.newAdHocConfiguration(storeHome);
        
        PeerGroupID cluster2id = tutorial.id.IDTutorial.createPeerGroupID("cluster2");
        cluster2GroupConfig.setInfrastructureID(cluster2id);
        cluster2GroupConfig.setName("Peer");
        cluster2GroupConfig.setPeerID(peerid);
        cluster2GroupConfig.setUseMulticast(true);
        int cluster2multicastport = 1025 + ("cluster2".hashCode() % 60000);
        cluster2GroupConfig.setMulticastPort(cluster2multicastport);
        
        // Instantiate cluster 2 net peer group
        
        NetPeerGroupFactory npgf2 = new NetPeerGroupFactory(wpg, cluster2GroupConfig.getPlatformConfig(), npgImplAdv);
        
        PeerGroup cluster2 = npgf2.getInterface();
        
        System.out.println("Cluster 2 Peer Group : " + cluster2 + " started!");
        
        cluster1.stopApp();
        cluster2.stopApp();
        
        wpg.stopApp();
        
        System.out.println("World Peer Group : " + wpg + " stopped!");
    }    
}
