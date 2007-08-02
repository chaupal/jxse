/*
 * customgroupservice.java
 *
 * Created on July 17, 2007, 9:30 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package tutorial.customgroupservice;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;

// Please read the @deprecation warning carefully.
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.protocol.PeerGroupAdvertisement;

/**
 *  This sample shows how to define a Peer Groups with custom services. In this
 *  case the Membership service is replaced with the PSE Membership Service.
 */
public class customgroupservice {

    /**
     * Main method
     *
     * @param args none defined
     */
    public static void main(String args[]) {
        try {
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "HelloWorld");
            System.out.println("Starting JXTA");
            PeerGroup npg = manager.startNetwork();
            System.out.println("JXTA Started : " + npg );
            
            // Create the Module Impl Advertisement for our custom group.
            
            // Start with a standard peer group impl advertisement.
            ModuleImplAdvertisement customGroupImplAdv = npg.getAllPurposePeerGroupImplAdvertisement();
            
            // It's custom so we have to change the module spec ID. We make a random one with the same base class.
            customGroupImplAdv.setModuleSpecID(IDFactory.newModuleSpecID(customGroupImplAdv.getModuleSpecID().getBaseClass()));
            
            // StdPeerGroup uses the ModuleImplAdvertisement param section to describe what services to run.
            StdPeerGroupParamAdv customGroupParamAdv = new StdPeerGroupParamAdv(customGroupImplAdv.getParam());

            // Change the membership service to the PSE Membership Service.
            customGroupParamAdv.addService(PeerGroup.membershipClassID, PSEMembershipService.pseMembershipSpecID);

            // update the customGroupImplAdv by updating its param
            customGroupImplAdv.setParam((XMLElement) customGroupParamAdv.getDocument(MimeMediaType.XMLUTF8));
            
            // publish it.
            npg.getDiscoveryService().publish(customGroupImplAdv);
            
            // Create the Peer Group Advertisement.
            
            // Get a PeerGroupAdvertisement instance.
            PeerGroupAdvertisement customGroupAdv = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());
            // Choose a random peer group.
            customGroupAdv.setPeerGroupID(IDFactory.newPeerGroupID());
            // The custom group uses our custom Module Specification.
            customGroupAdv.setModuleSpecID(customGroupImplAdv.getModuleSpecID());
            
            // publish it too.
            npg.getDiscoveryService().publish(customGroupAdv);
            
            // Now we can instantiate the peer group.
            PeerGroup cpg = npg.newGroup(customGroupAdv);
            
            System.out.println("Started Custom Peer Group : " + cpg);
            
            // This is where the peer group would be used in a real example.
           
            cpg.stopApp(); 
            System.out.println("Stopped Custom Peer Group : " + cpg);

            System.out.println("Stopping JXTA");
            manager.stopNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
