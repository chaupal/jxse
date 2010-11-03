/*
 *  Copyright (c) 2001-2005 Sun Microsystems, Inc. All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
package tutorial.psesample.old;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.impl.protocol.PSEConfigAdv;

/**
 * A sample program which demonstrates usage of the JXTA PSE Membership Service.
 * <p/>
 * <p/>This sample is focused upon secure management of certificate chains,
 * certificate signing and identity management. It implements a peergroup which
 * provides four types of users:
 * <ul>
 * <li>Owner</li>
 * <li>Administrator</li>
 * <li>Member</li>
 * <li>Invitee</li>
 * <li>Interloper (not valid)</li>
 * </ul>
 * <p/>
 * <p/>Each of these user types has certain abilities within the peergroup.
 * <p/>
 * <dl>
 * <dt>Owner</dt>
 * <dd>Peer Group Owners have all rights within the group and are the owner
 * of the private key for the group's root certificate. Peers may become the
 * Peer Group Owner by knowing the password used to encrypt the peer group's
 * private key. The Peer Group Owner may designate peers as Administrators by
 * signing their certificate with the peer group private key.</dd>
 * <p/>
 * <dt>Administrator<dt>
 * <dd>Peer Group Administrators are peers designated by the Peer Group
 * Owners to have rights beyond that of regular Member peers. Administrator
 * peers may generate peer group invitations and may validate Member peers.
 * Administrator peers have their certifcate signed with the peer group owner
 * private key. Administrators can sign the certificate of Peer Group Invitees
 * to allow them to become Peer Group Members. Administrators can generate
 * Peer Group invitations which can be used by peers to join the peer group.
 * A Peer Group invitation contains a certificate signed with the
 * Administrator's private key and the matching private. The private key is
 * encryped with a password. To use the invitation a peer must know the
 * password for the invitations private key.</dd>
 * <p/>
 * <dt>Member</dt>
 * <dd>Peer Group Members are the standard user class for this sample. Peer
 * Group Members have a certificate that is signed with the private key of a
 * Peer Group Administrator (which in turn is signed with the private key of a
 * Peer Group Owner). Peer Group Members can request their certificate to be
 * signed by a Peer Group Owner in order to become a Peer Group Administrator.
 * <dd>
 * <p/>
 * <dt>Invitee</dt>
 * <dd>Peer Group Invitees are peers who's certificate is signed by an
 * invitation certificate. Invitees sign their own certificate with the private
 * key from the Peer Group invitation they used when first joining the peer
 * group. Peer Group Invitees can request that their certificate be signed by
 * a Peer Group Adminstrator in order to become a Peer Group Member.</dd>
 * <p/>
 * <dt>Interloper</dt>
 * <dd>Peer Group Interlopers are unauthorized peers who do not have a
 * correctly signed certificate. Interloper peers may attempt to interact with
 * other peers within the peer group, but all of their requests will be
 * ignored without the correct certificates. While potentially annoying Peer
 * Group Interloper peers are easily detected and can do no harm within the
 * peer group.</dd>
 * </dl>
 *
 * @see net.jxta.membership.MembershipService
 * @see net.jxta.impl.membership.pse.PSEMembershipService
 */
public class Main {

    /**
     * If true then the net peer group will be started which causes the
     * JXTA Shell to be started. This is useful for debugging.
     */
    private final static boolean START_NETGROUP = true;
    /**
     * The PeerGroup ID used by this example. We use a constant ID so that
     * every peer running this test program runs within in the same group.
     * A real application would need to support more than one peergroup and
     * should use discovery to locate appropriage peer groups.
     * <p/>
     * <p/>This ID was created by making a new group adv in the JXTA shell
     * with <tt>newpgrp</tt> and then copying the ID from the advertisement.
     */
    private final static PeerGroupID PSE_SAMPLE_PGID = (PeerGroupID) ID.create(
            URI.create("urn:jxta:uuid-6E0C1C2781794A2F983EA4D2ACB758E602"));
    /**
     * The ModuleSpec ID used for this example's peer group. We use a
     * constant ID so that every peer which creates peer groups uses the
     * same Module Spec ID.
     * <p/>
     * <p/>This ID was created by making a new group adv in the JXTA shell
     * with <tt>newpgrp</tt> and then copying the ID from the advertisement.
     */
    private final static ModuleSpecID PSE_SAMPLE_MSID = (ModuleSpecID) ID.create(
            URI.create("urn:jxta:uuid-DEADBEEFDEAFBABAFEEDBABE0000000133BF5414AC624CC8AD3AF6AEC2C8264306"));
    /**
     * This Certificate is the root certificate for the sample PSE peer group. All important
     * certificate chains in the group have this certificate as their root. Since
     * every peer in the group is supposed to know this cert sometimes it's
     * omitted from chains if the last certificate in the chain is signed by
     * this certificate.
     * <p/>
     * <p/>The certificate (and the private key) were created using the JXTA
     * Shell with the following command:
     * <p/>
     * <pre>
     *   JXTA><b>pse.createkey PSE_Sample_Root  urn:jxta:uuid-6E0C1C2781794A2F983EA4D2ACB758E602 topsecret</b>
     *   JXTA><b>psecred = login</b>
     *   peer - Enter the identity you want to use for group 'NetPeerGroup' :
     *   KeyStorePassword : <b>password</b>
     *   Identity : <b>urn:jxta:uuid-6E0C1C2781794A2F983EA4D2ACB758E602</b>
     *   IdentityPassword : <b>topsecret</b>
     *   JXTA><b>pse.dumpcred -a psecred</b>
     *  </pre>
     * <p/>
     * <p/>Certificate presented here has been BASE64ed for easier inclusion.
     * <p/>
     * <p/>When stored in the PSE keystore this certificate will be stored
     * using the Peer Group ID as it's ID.
     */
    private final static String PSE_SAMPLE_GROUP_ROOT_CERT_BASE64 = 
            "MIICGTCCAYKgAwIBAgIBATANBgkqhkiG9w0BAQUFADBTMRUwEwYDVQQKEwx3d3cuanh0YS5vcmcx" + 
            "GzAZBgNVBAMTElBTRV9TYW1wbGVfUm9vdC1DQTEdMBsGA1UECxMURERFMkNGQ0MyMjVBNDM0OUQx" + 
            "QTAwHhcNMDUwNjE5MDIyODM4WhcNMTUwNjE5MDIyODM4WjBTMRUwEwYDVQQKEwx3d3cuanh0YS5v" + 
            "cmcxGzAZBgNVBAMTElBTRV9TYW1wbGVfUm9vdC1DQTEdMBsGA1UECxMURERFMkNGQ0MyMjVBNDM0" +
            "OUQxQTAwgZ4wDQYJKoZIhvcNAQEBBQADgYwAMIGIAoGAdVgeJotJWEEfh/NtusfI8cAIMAq7WxXA" + 
            "ZsPIOYnybHPXFNmCTozs/KW0dx01zI6kfHwO1qYXmR/djJAFhr3VhFdUp8y1wDCf6DT63vFOi47t" + 
            "6TC1yywjZe59VIAxhDt0B8XJnkEbsEl+uO95ec6/U6dYI1vrtWU4ORdSYz615XMCAwEAATANBgkq" + 
            "hkiG9w0BAQUFAAOBgQBRJXLRyIGHvw3GJC3lYwQUDwRSm6vaPKPlCA5Axfwy+jPuStldhuPYOvxz" + 
            "a3NxQ/iBlzTGwoVzgxzArM6oLRvtAAvvkQl8z6Lu+NF2ugMs6XfuzRKqrBvSjNaSYM83E51niga2" + 
            "3UGc4Brbn3RCTPRADykVhWxgiCADNGVBIBUAMw==";
    /**
     * Private key for the root certificate of the sample PSE peer group. <b>In
     * most applications the private key would not be included as part of the
     * source code</b> For this sample to be entirely standalone it must be
     * included.</b>
     * <p/>
     * <p/>The certificate (and the private key) were created using the JXTA
     * Shell with the following commands:
     * <p/>
     * <pre>
     *   <i>... commands shown in comment of "PSE_SAMPLE_GROUP_ROOT_CERT" ...</i>
     *   JXTA><b>pse.dumpcred -a -p -x topsecret psecred</b>
     *  </pre>
     * <p/>
     * <p/>The private key has been protected using PKCS#8 password based
     * encryption. The password for the encrypted private key is the string
     * "<tt>topsecret</tt>".
     * <p/>
     * <p/>As you use this sample you will see that only one peer is required
     * to know the root key.
     * <p/>
     * <p/>The key presented here has been encrypted using PKCS#5 PBKD, wrapped
     * as a PKCS#8 encrpyted key and then finally BASE64ed for easier inclusion.
     * <p/>
     * <p/>When stored in the PSE keystore this key will be stored using the
     * Peer Group ID as it's ID.
     */
    private final static String PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY_BASE64 = 
            "MIICoTAbBgkqhkiG9w0BBQMwDgQIPPpnqsvvaS0CAicQBIICgJAYTLxQfaUMFL08DnrO/tAZioTu" + 
            "TlUnt32h3n9nE/L0UM8u7Q9elq2YwBNN72LD6ODzZKPmS/PnUl0NnE1AOnLVuMUgl1OBXgmUtC4P" + 
            "jfgA+En7S0YEmgZN42ceqMpcKGDiBNdr0ebGD9SVy4/XkTLrNcEcHqrhyC6JkSOAo2EKL9OkS6gR" + 
            "bVp59JSEiAruDvAZnz3XTjlXJYchZGcMVNfCDJVEMCgCsaKkr1Pf5JAfj1kKBJbazwlvqVrU0eI7" + 
            "nPXTdTNVUaZLA7ucbUialef2/osefm5oB00DVkgIkUQSjesVM+THKu3UxIFe+3yTbUsI3zDja+DK" + 
            "36l+UBmCLwFSOzJ1HAzP2qj1yvE/crEsvZMr9QrfNp7acfZQCgJZWFBG0wkdkvpTC0SBbzD6TqdW" + 
            "hbGq8rca4KDkI4HeVoB3yBnMDm52NOtvh2uTKHul7Zz+3GTjXTIT7B4WcdiKmYo5hzdAidHzrWHV" +
            "eTmBnda34kM4o0uX1rQjWe3pfpp7rKG/zRDMUsqaZhK0k3t8IiNZroMnH39wz/kiRWgh+LBZOmi6" + 
            "vG4LeaNDom6+o1tH4lHFXh0uCOSjOOKvX91BaptgXXLuFpny1ZMPnSkWzZA20nCJgNB1+S5RLQGg" + 
            "jObczNUFtI8c/nSlbn339fN9G9/EpGaQuoMqxoSWwVnMnfmBnYlq2LehZ3UC3DgSaxRI9XN/F2Ul" + 
            "ako4dwiccGcMsGHB/eKHQU/Csk9E19GGghwC2L7Tb2zIx01Ctd2yecpK3clhvN35xR5cvtnKKLtA" + 
            "KSi8v6rCLDJ0cPa88QfIHRk+M5ZTDP5QN4A0uFKnsWtMI/xjA9tK4VsMEMtxqjBFem8=";
    /**
     * Root certificate of the sample PSE peer group.
     */
    final static X509Certificate PSE_SAMPLE_GROUP_ROOT_CERT;
    /**
     * Private key for the root certificate of the sample PSE peer group.
     */
    private final static EncryptedPrivateKeyInfo PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY;

    static {

        /* Initialize some static final variables */
        try {
            // Initialize the Root certificate.
            byte[] cert_der = PSEUtils.base64Decode(new StringReader(PSE_SAMPLE_GROUP_ROOT_CERT_BASE64));

            CertificateFactory cf = CertificateFactory.getInstance("X509");

            // Initialize the Root private key.
            PSE_SAMPLE_GROUP_ROOT_CERT = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert_der));

            byte[] key_der = PSEUtils.base64Decode(new StringReader(PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY_BASE64));

            PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY = new EncryptedPrivateKeyInfo(key_der);
        } catch (IOException failure) {
            IllegalStateException failed = new IllegalStateException("Could not read certificate or key.");

            failed.initCause(failure);
            throw failed;
        } catch (CertificateException failure) {
            IllegalStateException failed = new IllegalStateException("Could not process certificate.");

            failed.initCause(failure);
            throw failed;
        }
    }

    /**
     * Creates a new instance of Main
     */
    public Main() throws Exception {

        // Start JXTA, instantiate the net peer group.
        NetPeerGroupFactory npgf = new NetPeerGroupFactory();
        final PeerGroup npg = npgf.getInterface();

        // This will cause the JXTA Shell to start if enabled.
        if (START_NETGROUP) {
            npg.startApp(new String[0]);
        }

        // Build the Module Impl Advertisemet we will use for our group.
        ModuleImplAdvertisement pseImpl = build_psegroup_impl_adv(npg);

        // Publish the Module Impl Advertisement to the group where the
        // peergroup will be advertised. This should be done in every peer
        // group in which the Peer Group is also advertised.
        // We use the same expiration and lifetime that the Peer Group Adv
        // will use (the default).
        DiscoveryService disco = npg.getDiscoveryService();

        disco.publish(pseImpl, PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);

        PeerGroupAdvertisement pse_pga = null;

        // Options for the Option Pane Dialog
        final Object[] options = {
            "Create self-invitation", "Use invitation..."
        };

        while (null == pse_pga) {

            /* We use a JOptionPane to quickly choose between the choices
             *  available for starting the PSE Sample Peer Group.
             *
             *  The choices are:
             *      - Create a self-invitation
             *      - Use an invitation from a file
             *
             *  "Self Invite" - If you know the password for the hard coded
             *  root certificate/private key pair then you can create a group
             *  advertisement which enables you to create and instantiate the
             *  peer group with owner privleges. Only one peer ever needs to do
             *  this as the res of the peers can join using generated invitations.
             *
             *  "Use Invitation" - Use a peer group advertisement provided by
             *  another peer in the group in order to join the peergroup.
             */
            int optionPicked = JOptionPane.showOptionDialog(null,
                    "To start the PSE Sample Peer Group you must choose a source for the Peer Group Advertisement.",
                    "Start PSE Sample Peer Group", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                    options[1]);

            if (JOptionPane.CLOSED_OPTION == optionPicked) {
                // User cancelled dialog. They want to quit.
                break;
            }

            switch (optionPicked) {
                case 0:
                    // Create Self-Invitation.
                    X509Certificate[] invitationCertChain = {PSE_SAMPLE_GROUP_ROOT_CERT};

                    pse_pga = build_psegroup_adv(pseImpl, invitationCertChain, PSE_SAMPLE_GROUP_ROOT_ENCRYPTED_KEY);
                    break;

                case 1:
                default:
                    // Use an invitation from a file.
                    final JFileChooser fc = new JFileChooser();

                    // In response to a button click:
                    int returnVal = fc.showOpenDialog(null);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        FileReader invitation = new FileReader(fc.getSelectedFile());

                        XMLDocument advDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                                invitation);

                        pse_pga = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(advDoc);

                        invitation.close();
                    }
                    break;
            }
        }

        // If we have a Peer Group Advertisement then instantiate the group
        // and run the main application.
        if (null != pse_pga) {
            // Publish the Peer Group. This will also be done when the group is
            // instantiated, but we do it here because we want to ensure
            // what lifetime and expiration to use.
            disco.publish(pse_pga, PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);

            // The invokeLater inner class needs it to be "final". So dupe it.
            final PeerGroupAdvertisement ui_pga = pse_pga;

            // `Invoke the Swing based user interface.
            javax.swing.SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    new tutorial.psesample.old.SwingUI(npg, ui_pga).setVisible(true);
                }
            });
        }
    }

    /**
     * Start of the PSE peer group sample.
     *
     * @param args the command line arguments. Unused by this sample.
     */
    public static void main(String[] args) {
        // Give the Thread we are running on a name for debuggers.
        Thread.currentThread().setName(Main.class.getName() + ".main()");

        try {
            new Main();
        } catch (Throwable all) {
            // Something bad happened, print out the failure and quit.
            System.err.println("Uncaught Throwable in main() :");
            all.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Build a Module Implementation Advertisement suitable for the PSE Sample
     * Peer Group. The <tt>ModuleImplAdvertisement</tt> is built using the
     * result of <tt>base.getAllPurposePeerGroupImplAdvertisement()</tt> to
     * ensure that the result will be appropriate for running as a child
     * peer group of <tt>base</tt>.
     * <p/>
     * <p/>The default advertisement is modified to use the PSE Membership
     * Service as it's membership service replacing whatever membership
     * service was originally specified.
     * <p/>
     * <p/>The Module Spec ID of the ModuleImplAdvertisement is set to the
     * hard-coded id <tt>PSE_SAMPLE_MSID</tt> so that all peer group instances
     * have a consistent (and compatible) specification.
     *
     * @param base The Peer Group from which we will retrieve the default
     *             Module Implementation Advertisement.
     * @return The Module Implementation Advertisement for the PSE Sample
     *         Peer Group.
     */
    static ModuleImplAdvertisement build_psegroup_impl_adv(PeerGroup base) {
        ModuleImplAdvertisement newGroupImpl;

        try {
            newGroupImpl = base.getAllPurposePeerGroupImplAdvertisement();
        } catch (Exception unlikely) {
            // getAllPurposePeerGroupImplAdvertisement() doesn't really throw expections.
            throw new IllegalStateException("Could not get All Purpose Peer Group Impl Advertisement.");
        }

        newGroupImpl.setModuleSpecID(PSE_SAMPLE_MSID);
        newGroupImpl.setDescription("PSE Sample Peer Group Implementation");

        // FIXME bondolo Use something else to edit the params.
        StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(newGroupImpl.getParam());

        Map services = params.getServices();

        ModuleImplAdvertisement aModuleAdv = (ModuleImplAdvertisement) services.get(PeerGroup.membershipClassID);

        services.remove(PeerGroup.membershipClassID);

        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());

        implAdv.setModuleSpecID(PSEMembershipService.pseMembershipSpecID);
        implAdv.setCompat(aModuleAdv.getCompat());
        implAdv.setCode(PSEMembershipService.class.getName());
        implAdv.setUri(aModuleAdv.getUri());
        implAdv.setProvider(aModuleAdv.getProvider());
        implAdv.setDescription("PSE Membership Service");

        // Add our selected membership service to the peer group service as the
        // group's default membership service.
        services.put(PeerGroup.membershipClassID, implAdv);

        // Save the group impl parameters
        newGroupImpl.setParam((Element) params.getDocument(MimeMediaType.XMLUTF8));

        return newGroupImpl;
    }

    /**
     * Build the Peer Group Advertisement for the PSE Sample Peer Group.
     * <p/>
     * <p/>The Peer Group Advertisement will be generated to contain an
     * invitation certificate chain and encrypted private key. Peers which
     * know the password for the Peer Group Root Certificate Key can generate
     * their own invitation otherwise peers must get an invitation from
     * another group member.
     * <p/>
     * <p/>The invitation certificate chain appears in two forms:
     * <ul>
     * <li>Self Invitation : PSE Sample Group Root Certificate + Encrypted Private Key</li>
     * <li>Regular Invitation :
     * <ul>
     * <li>Invitation Certificate + Encrpyted Private Key</li>
     * <li>Peer Group Member Certificate</li>
     * <li>Peer Group Administrator Certificate</li>
     * <li>PSE Sample Group Root Certificate</li>
     * </ul></li>
     * </ul>
     * <p/>
     * <p/>Invitations are provided to prospective peer group members. You can
     * use a unique invitation for each prospective member or a single
     * static invitation for every prospective member. If you use a static
     * invitation certificate keep in mind that every copy will use the same
     * shared password and thus the invitation will provide only very limited
     * security.
     * <p/>
     * <p/>In some applications the invitation password will be built in to the
     * application and the human user will never have to know of it's use.
     * This can be useful if you wish your PSE Peer Group used only by a single
     * application.
     *
     * @param pseImpl              The Module Impl Advertisement which the Peer Group
     *                             Advertisement will reference for its Module Spec ID.
     * @param invitationCertChain  The certificate chain which comprises the
     *                             PeerGroup Invitation.
     * @param invitationPrivateKey The private key of the invitation.
     * @return The Peer Group Advertisement.
     */
    static PeerGroupAdvertisement build_psegroup_adv(ModuleImplAdvertisement pseImpl, X509Certificate[] invitationCertChain, EncryptedPrivateKeyInfo invitationPrivateKey) {
        PeerGroupAdvertisement newPGAdv = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(
                PeerGroupAdvertisement.getAdvertisementType());

        newPGAdv.setPeerGroupID(PSE_SAMPLE_PGID);
        newPGAdv.setModuleSpecID(pseImpl.getModuleSpecID());
        newPGAdv.setName("PSE Sample Peer Group");
        newPGAdv.setDescription("Created by PSE Sample!");

        PSEConfigAdv pseConf = (PSEConfigAdv) AdvertisementFactory.newAdvertisement(PSEConfigAdv.getAdvertisementType());

        pseConf.setCertificateChain(invitationCertChain);
        pseConf.setEncryptedPrivateKey(invitationPrivateKey, invitationCertChain[0].getPublicKey().getAlgorithm());

        XMLDocument pseDoc = (XMLDocument) pseConf.getDocument(MimeMediaType.XMLUTF8);

        newPGAdv.putServiceParam(PeerGroup.membershipClassID, pseDoc);

        return newPGAdv;
    }
}
