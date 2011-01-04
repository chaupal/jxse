/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jxta.socket.examples;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import net.jxta.id.ID;

import net.jxta.peer.PeerID;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEConfig;

/**
 *
 * @author nick
 */
public class DefaultSecureTCPMessageTest extends DefaultTCPMessageTest {

    public static void main(String[] args) {
        try {
            DefaultSecureTCPMessageTest t = new DefaultSecureTCPMessageTest();
            t.init();
            t.run();
            t.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    protected void run() {
        try {

            PeerID alicePeerID = aliceManager.getNetPeerGroup().getPeerID();
            PeerID bobPeerID = bobManager.getNetPeerGroup().getPeerID();
            
            PSEMembershipService alicePSEMembershipService = (PSEMembershipService)aliceManager.getNetPeerGroup().getMembershipService();
            PSEMembershipService bobPSEMembershipService = (PSEMembershipService)bobManager.getNetPeerGroup().getMembershipService();

            PSEConfig alicePSEConfig = alicePSEMembershipService.getPSEConfig();
            PSEConfig bobPSEConfig = bobPSEMembershipService.getPSEConfig();

            X509Certificate aliceCertificate = alicePSEConfig.getTrustedCertificate(alicePeerID);
            X509Certificate bobCertificate = bobPSEConfig.getTrustedCertificate(bobPeerID);

            alicePSEConfig.setTrustedCertificate(bobPeerID, bobCertificate);
            bobPSEConfig.setTrustedCertificate(alicePeerID, aliceCertificate);

            testColocatedPeerBidiPipeComms(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
