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

import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.ModuleImplAdvertisement;

import net.jxta.impl.endpoint.mcast.McastTransport;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;

/**
 * This sample shows how to construct multiple infrastructure (Network) Peer
 * Groups.
 */
public class MultiNetGroupLaucher {

    /**
     * The name of our peer.
     */
    private final static String PEER_NAME = "Peer";

    /**
     * Configure and start the World Peer Group.
     *
     * @param storeHome The location JXTA will use to store all persistent data.
     * @param peername  The name of the peer.
     * @throws PeerGroupException Thrown for errors creating the world peer group.
     * @return the world peergroup
     */
    public static PeerGroup startJXTA(URI storeHome, String peername) throws PeerGroupException {
        NetworkConfigurator worldGroupConfig = NetworkConfigurator.newAdHocConfiguration(storeHome);

        PeerID peerid = tutorial.id.IDTutorial.createPeerID(PeerGroupID.worldPeerGroupID, peername);
        worldGroupConfig.setName(peername);
        worldGroupConfig.setPeerID(peerid);
        // Disable multicast because we will be using a separate multicast in each group.
        worldGroupConfig.setUseMulticast(false);

        // Instantiate the world peer group
        WorldPeerGroupFactory wpgf = new WorldPeerGroupFactory(worldGroupConfig.getPlatformConfig(), storeHome);

        PeerGroup wpg = wpgf.getInterface();

        System.out.println("JXTA World Peer Group : " + wpg + " started!");

        return wpg;
    }

    /**
     * Configure and start a separate top-level JXTA domain.
     *
     * @param wpg        The JXTA context into which the domain will run.
     * @param domainName The name of the domain.
     * @throws PeerGroupException Thrown for errors creating the domain.
     * @return the net peergroup
     */
    public static PeerGroup startDomain(PeerGroup wpg, String domainName) throws PeerGroupException {
        assert wpg.getPeerGroupID().equals(PeerGroupID.worldPeerGroupID);

        ModuleImplAdvertisement npgImplAdv;
        try {
            npgImplAdv = wpg.getAllPurposePeerGroupImplAdvertisement();
            npgImplAdv.setModuleSpecID(PeerGroup.allPurposePeerGroupSpecID);

            StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(npgImplAdv.getParam());

            params.addProto(McastTransport.MCAST_TRANSPORT_CLASSID, McastTransport.MCAST_TRANSPORT_SPECID);

            npgImplAdv.setParam((XMLDocument) params.getDocument(MimeMediaType.XMLUTF8));
        } catch (Exception failed) {
            throw new PeerGroupException("Could not construct domain ModuleImplAdvertisement", failed);
        }

        // Configure the domain

        NetworkConfigurator domainConfig = NetworkConfigurator.newAdHocConfiguration(wpg.getStoreHome());

        PeerGroupID domainID = tutorial.id.IDTutorial.createPeerGroupID(domainName);
        domainConfig.setInfrastructureID(domainID);
        domainConfig.setName(wpg.getPeerName());
        domainConfig.setPeerID(wpg.getPeerID());
        domainConfig.setUseMulticast(true);
        int domainMulticastPort = 1025 + (domainName.hashCode() % 60000);
        domainConfig.setMulticastPort(domainMulticastPort);

        // Instantiate the domain net peer group

        // TODO: Review

        // NetPeerGroupFactory npgf1 = new NetPeerGroupFactory(wpg, domainConfig.getPlatformConfig(), npgImplAdv);
        // PeerGroup domain = npgf1.getInterface();
        // System.out.println("Peer Group : " + domain + " started!");

        return null; // domain;
    }

    /**
     * main
     *
     * @param args command line arguments. (unused)
     * @throws Throwable Thrown for every type of error.
     */
    public static void main(String args[]) throws Throwable {
        final URI storeHome = new File(".jxta/").toURI();

        PeerGroup wpg = startJXTA(storeHome, PEER_NAME);

        PeerGroup cluster1 = startDomain(wpg, "cluster1");
        PeerGroup cluster2 = startDomain(wpg, "cluster2");

        cluster1.stopApp();
        cluster2.stopApp();

        wpg.stopApp();

        System.out.println(wpg + " stopped!");
        
        // Run GC and finalization to see what's still running.
        System.gc();
        System.runFinalization();
        System.gc();
        
        System.out.println("Quitting");        
    }
}
