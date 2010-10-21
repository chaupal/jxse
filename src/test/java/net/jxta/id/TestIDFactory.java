/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jxta.id;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.SecureRandom;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.PSEUtils.IssuerInfo;

import org.junit.Ignore;
import java.util.Hashtable;

@Ignore
public class TestIDFactory {

//    private static Hashtable certMap = new Hashtable();
    private static SecureRandom secureRandom = new SecureRandom();

    public static PeerID newPeerID(PeerGroupID groupID) {
        PeerID peerID = null;
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        peerID = IDFactory.newPeerID(groupID, buffer);
        return peerID;
    }

//    private static X509Certificate getX509Certificate(PeerGroupID groupID) {
//        X509Certificate cert = (X509Certificate)certMap.get(groupID);
//        if (cert == null) {
//            IssuerInfo info = PSEUtils.genCert("PeerGroupName", null);
//            certMap.put(groupID, info.cert);
//            cert = info.cert;
//        }
//        return cert;
//    }

//    private static X509Certificate getX509Certificate(PeerGroupID groupID) {
//        IssuerInfo info = PSEUtils.genCert("PeerGroupName", null);
//        return info.cert;
//    }

    


}
