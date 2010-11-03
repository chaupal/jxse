/*
 * customgroupservice.java
 *
 * Created on July 17, 2007, 9:30 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package tutorial.customgroupservice;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

// Please read the @deprecation warning carefully.
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;

/**
 * This sample shows how to define a Peer Group with custom services. In this
 * case we add a simple demonstration service "Gossip" to the peer group.
 */
public class CustomGroupService {

    /**
     *  The ID that our custom peer group will use. We use a hardcoded id so that all instances use the same value.
     *  This ID was generated using the <tt>newpgrp -s</tt> JXSE Shell command.
     */ 
    private final static PeerGroupID CUSTOM_PEERGROUP_ID = PeerGroupID.create(URI.create("urn:jxta:uuid-425A5C703CD5454F9C03938A0D65BD5002"));

    /**
     *  The MSID that our custom peer group will use. We use a hardcoded id so that all instances use the same value.
     *  This ID was generated using the <tt>newpgrp -s</tt> JXSE Shell command.
     */ 
    private final static ModuleSpecID CUSTOM_PEERGROUP_MSID = ModuleSpecID.create(URI.create("urn:jxta:uuid-DEADBEEFDEAFBABAFEEDBABE00000001ACEFC090A5D74844A9DEF43F3003D35A06"));

    /**
     * Main method
     *
     * @param args none defined
     */
    public static void main(String args[]) {
        try {
            // Set the main thread name for debugging.
            Thread.currentThread().setName(CustomGroupService.class.getSimpleName());

            // Configure JXTA
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "customgroupservice", new File(new File(".cache"), "customgroupservice").toURI());
            manager.setPeerID(IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID));  // create a random peer id.

            // Start JXTA.
            System.out.println("Starting JXTA");
            PeerGroup npg = manager.startNetwork();
            System.out.println("JXTA Started : " + npg);

            // Register the Gossip Service Configuration Advertisement with the advertisement factory.
            // This would normally be done by defining the GossipServiceConfigAdv in the META-INF/services/net.jxta.document.Advertisement
            // file of the jar containing the gossip service.
            AdvertisementFactory.registerAdvertisementInstance(GossipServiceConfigAdv.getAdvertisementType(), new GossipServiceConfigAdv.Instantiator());

            // Create the Module Impl Advertisement for our custom group.

            // Start with a standard peer group impl advertisement.
            ModuleImplAdvertisement customGroupImplAdv = npg.getAllPurposePeerGroupImplAdvertisement();

            // It's custom so we have to change the module spec ID.
            customGroupImplAdv.setModuleSpecID(CUSTOM_PEERGROUP_MSID);

            // StdPeerGroup uses the ModuleImplAdvertisement param section to describe what services to run.
            StdPeerGroupParamAdv customGroupParamAdv = new StdPeerGroupParamAdv(customGroupImplAdv.getParam());

            // Add our Gossip Service to the group
            customGroupParamAdv.addService(GossipService.GOSSIP_SERVICE_MCID, GossipService.GOSSIP_SERVICE_MSID);

            // update the customGroupImplAdv by updating its param
            customGroupImplAdv.setParam((XMLElement) customGroupParamAdv.getDocument(MimeMediaType.XMLUTF8));

            // publish the ModuleImplAdvertisement for our custom peer group.
            npg.getDiscoveryService().publish(customGroupImplAdv);

            // Create the module Impl Advertisement for our custom service.

            // Create a new ModuleImplAdvertisement instance.
            ModuleImplAdvertisement gossipImplAdv = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());

            // Our implementation implements the given ModuleSpecID.
            gossipImplAdv.setModuleSpecID(GossipService.GOSSIP_SERVICE_MSID);

            // copy compatibility statement from the peer group impl advertisement.
            gossipImplAdv.setCompat(customGroupImplAdv.getCompat());

            // The code for the implementation is the "tutorial.customgroupservice.GossipService" class.
            gossipImplAdv.setCode(GossipService.class.getName());

            // If the code needed to be downloaded, where should it be downloaded from. This is not normally used.
            gossipImplAdv.setUri("http://jxta-jxse.dev.java.net/download/jxta.jar");

            // The provider of the implementation.
            gossipImplAdv.setProvider("JXTA Orgainzation.");

            // A description of the service. (optional)
            gossipImplAdv.setDescription("Tutorial Gossip Service");

            // publish the gossip service module implementation advertisement.
            npg.getDiscoveryService().publish(gossipImplAdv);

            // Create the Peer Group Advertisement.

            // Crete a new PeerGroupAdvertisement instance.
            PeerGroupAdvertisement customGroupAdv = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());

            // Set our chosen peer group ID.
            customGroupAdv.setPeerGroupID(CUSTOM_PEERGROUP_ID);

            // Set the peer group name.
            customGroupAdv.setName("Custom Gossip Peer Group");

            // The custom group uses our custom Module Specification.
            customGroupAdv.setModuleSpecID(customGroupImplAdv.getModuleSpecID());

            // Add Gossip Service configuration parameters to the group adv.
            GossipServiceConfigAdv gossipConfig = (GossipServiceConfigAdv) AdvertisementFactory.newAdvertisement(GossipServiceConfigAdv.getAdvertisementType());
            if(args.length != 0) {
                // Use the command line parameters as the gossip text.
                StringBuilder gossipText = new StringBuilder();

                for(String arg : Arrays.asList(args)) {
                    if(0 != gossipText.length()) {
                        gossipText.append(" ");
                    }
                    gossipText.append(arg);
                }
                gossipConfig.setGossip(gossipText.toString());
            } else {
                // Use default gossip text.
                gossipConfig.setGossip("Custom Peer Group Services are fun!");
            }
            gossipConfig.setShowOwn(true);

            // Save the gossip config into the peer group advertisement.
            XMLDocument asDoc = (XMLDocument) gossipConfig.getDocument(MimeMediaType.XMLUTF8);
            customGroupAdv.putServiceParam(GossipService.GOSSIP_SERVICE_MCID, asDoc);

            // publish the peer group advertisement.
            npg.getDiscoveryService().publish(customGroupAdv);

            // Now we can instantiate the peer group.
            PeerGroup cpg = npg.newGroup(customGroupAdv);
            cpg.startApp(new String[0]);

            System.out.println("Started Custom Peer Group : " + cpg);

            // Sleep for a while. The Gossip Service will do it's gossiping.
            try {
                Thread.sleep( 2 * 60 * 1000);
            } catch(InterruptedException woken) {
                Thread.interrupted();
            }

            System.out.println("Stopping Custom Peer Group : " + cpg);
            cpg.stopApp();

            System.out.println("Stopping JXTA");
            manager.stopNetwork();
        } catch (Throwable e) {
            System.err.println("Fatal error -- quitting.");
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
