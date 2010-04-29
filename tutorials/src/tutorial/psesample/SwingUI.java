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

package tutorial.psesample;


import net.jxta.credential.AuthenticationCredential;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.impl.protocol.Certificate;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * Main User Interface for the PSE Sample Peer Group application.
 * <p/>
 * <p/>Provides access to a wide variety of fun and interesting PSE operations.
 * <p/>
 * <p/>This user interface is appropriate for this sample application but is
 * not appropriate for real applications. The major difference is the strategy
 * used for dynamically updating buttons and panels. After some experimentation
 * and feedback it was decided that this application would not dynamically
 * enable and disable most buttons. By leaving all buttons enabled, but
 * including status messages when unavailable options are attempted developers
 * can better experiment and understand why the application behaves as it does.
 * <p/>
 * <p/>Real applications should not present users with unavailable options.
 */
public class SwingUI extends javax.swing.JFrame {

    /**
     * The peer group which is the parent for our PSE peer group. Normally this
     * will be the Net Peer Group, but it is a bad idea to assume that it
     * always will be the Net Peer Group.
     * <p/>
     * <p/>The PSE peer group is instantiated into the parent peer group. The
     * parent peer group is also used for publishing our  peer group
     * advertisement and the module implementation advertisement for the PSE
     * peer group.
     */
    final PeerGroup parentgroup;

    /**
     * Our peer group object, the PSE Peer Group.
     */
    final PeerGroup group;

    /**
     * The Membership service of the PSE Peer Group.
     */
    final PSEMembershipService membership;

    /**
     * Credential which is created when the user successfully authenticates
     * for the invitation certificate. This requires that they know the
     * password used to encrypt the private key.
     */
    PSECredential invitationCredential = null;

    /**
     * Authenticator which is used for generating the invitation credential.
     */
    StringAuthenticator invitationAuthenticator = null;

    /**
     * Credential which is created when the user successfully authenticates
     * for the member certificate. This requires that they know the password
     * used to encrypt the private key.
     */
    PSECredential memberCredential = null;

    /**
     * Authenticator which is used for generating the invitation credential.
     */
    StringAuthenticator memberAuthenticator = null;

    /**
     * Credential which is created when the user successfully authenticates
     * for the owner certificate. This requires that they know the password
     * used to encrypt the private key.
     */
    PSECredential ownerCredential = null;

    /**
     * Authenticator which is used for generating the invitation credential.
     */
    StringAuthenticator ownerAuthenticator = null;

    /**
     * Creates new form SwingUI
     */
    public SwingUI(PeerGroup parent, PeerGroupAdvertisement pse_pga) {
        parentgroup = parent;
        try {
            group = parentgroup.newGroup(pse_pga);
        } catch (PeerGroupException failed) {
            JOptionPane.showMessageDialog(null, failed.getMessage(), "Couldn't create PSE Peer Group", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("Can't continue without being able to create a peergroup.");
        }

        membership = (PSEMembershipService) group.getMembershipService();

        initComponents();

        membership.addPropertyChangeListener("defaultCredential", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {// FIXME 20050624 bondolo how do I tell the swing UI????
                    }
                });

            }
        });
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        javax.swing.JLabel invitationDescriptionText;
        javax.swing.JLabel invitationPasswordLabel;
        javax.swing.JLabel memberPasswordLabel;

        memberTab = new javax.swing.JPanel();
        memberPasswordLabel = new javax.swing.JLabel();
        memberPasswordField = new javax.swing.JPasswordField();
        generateMemberCertButton = new javax.swing.JButton();
        memberAuthenticateButton = new javax.swing.JButton();
        memberGenerateCSRButton = new javax.swing.JButton();
        memberImportCertButton = new javax.swing.JButton();
        memberResignButton = new javax.swing.JButton();
        adminTab = new javax.swing.JPanel();
        adminSignCSRButton = new javax.swing.JButton();
        adminInviteButton = new javax.swing.JButton();
        adminInvitationPasswordLabel = new javax.swing.JLabel();
        adminInvitationPasswordField = new javax.swing.JPasswordField();
        ownerTab = new javax.swing.JPanel();
        ownerSignCSRButton = new javax.swing.JButton();
        ownerPasswordLabel = new javax.swing.JLabel();
        ownerPasswordField = new javax.swing.JPasswordField();
        ownerAuthenticateButton = new javax.swing.JButton();
        ownerResignButton = new javax.swing.JButton();
        invitationTab = new javax.swing.JPanel();
        invitationDescriptionText = new javax.swing.JLabel();
        invitationPasswordLabel = new javax.swing.JLabel();
        invitationPasswordField = new javax.swing.JPasswordField();
        invitationConfirmButton = new javax.swing.JButton();
        keyStorePasswordLabel = new javax.swing.JLabel();
        keyStorePasswordField = new javax.swing.JPasswordField();
        tabs = new javax.swing.JTabbedPane();
        authenticationStatus = new javax.swing.JTextField();

        memberTab.setLayout(new java.awt.GridBagLayout());

        memberTab.setToolTipText("Actions for Peer Group Members");
        memberTab.setName("Member");
        memberTab.setNextFocusableComponent(adminTab);
        if (membership.getPSEConfig().isInitialized()) {
            tabs.add(memberTab);
        }
        memberPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        memberPasswordLabel.setLabelFor(memberPasswordField);
        memberPasswordLabel.setText("Member Password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 3);
        memberTab.add(memberPasswordLabel, gridBagConstraints);

        memberPasswordField.setColumns(16);
        memberPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberPasswordFieldActionPerformed(evt);
            }
        });
        memberPasswordField.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                memberPasswordFieldKeyReleasedHandler(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        memberTab.add(memberPasswordField, gridBagConstraints);

        generateMemberCertButton.setText("Generate Certificate ");
        generateMemberCertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateMemberCertButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        memberTab.add(generateMemberCertButton, gridBagConstraints);

        memberAuthenticateButton.setText("Authenticate");
        memberAuthenticateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberAuthenticateButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        memberTab.add(memberAuthenticateButton, gridBagConstraints);

        memberGenerateCSRButton.setText("Generate CSR...");
        memberGenerateCSRButton.setEnabled(false);
        memberGenerateCSRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberGenerateCSRButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        memberTab.add(memberGenerateCSRButton, gridBagConstraints);

        memberImportCertButton.setText("Import Signed Certificate...");
        memberImportCertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberImportCertButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        memberTab.add(memberImportCertButton, gridBagConstraints);

        memberResignButton.setText("Resign");
        memberResignButton.setEnabled(false);
        memberResignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memberResignButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        memberTab.add(memberResignButton, gridBagConstraints);

        adminTab.setLayout(new java.awt.GridBagLayout());

        adminTab.setToolTipText("Actions for Peer Group Administrators");
        adminTab.setName("Administrator");
        adminTab.setNextFocusableComponent(ownerTab);
        adminSignCSRButton.setText("Sign CSR...");
        adminSignCSRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminSignCSRButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        adminTab.add(adminSignCSRButton, gridBagConstraints);

        adminInviteButton.setText("Generate Invitation...");
        adminInviteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminInviteButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        adminTab.add(adminInviteButton, gridBagConstraints);

        adminInvitationPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        adminInvitationPasswordLabel.setLabelFor(adminInvitationPasswordField);
        adminInvitationPasswordLabel.setText("Invitation Password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 3);
        adminTab.add(adminInvitationPasswordLabel, gridBagConstraints);

        adminInvitationPasswordField.setColumns(16);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        adminTab.add(adminInvitationPasswordField, gridBagConstraints);

        ownerTab.setLayout(new java.awt.GridBagLayout());

        ownerTab.setToolTipText("Actions for Peer Group Owner");
        ownerTab.setName("Owner");
        ownerTab.setNextFocusableComponent(keyStorePasswordField);
        ownerSignCSRButton.setText("Sign CSR...");
        ownerSignCSRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ownerSignCSRButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        ownerTab.add(ownerSignCSRButton, gridBagConstraints);

        ownerPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        ownerPasswordLabel.setLabelFor(ownerPasswordField);
        ownerPasswordLabel.setText("Owner Password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 3);
        ownerTab.add(ownerPasswordLabel, gridBagConstraints);

        ownerPasswordField.setColumns(16);
        ownerPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ownerPasswordFieldActionPerformed(evt);
            }
        });
        ownerPasswordField.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ownerPasswordFieldKeyReleasedHandler(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        ownerTab.add(ownerPasswordField, gridBagConstraints);

        ownerAuthenticateButton.setText("Authencticate");
        ownerAuthenticateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ownerAuthenticateButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        ownerTab.add(ownerAuthenticateButton, gridBagConstraints);

        ownerResignButton.setText("Resign");
        ownerResignButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ownerResignButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        ownerTab.add(ownerResignButton, gridBagConstraints);

        invitationTab.setLayout(new java.awt.GridBagLayout());

        invitationTab.setToolTipText("Actions for Confirming a Peer Group Invitation");
        invitationTab.setFocusable(false);
        invitationTab.setName("Invitation");
        invitationTab.setNextFocusableComponent(keyStorePasswordField);
        if (!membership.getPSEConfig().isInitialized()) {
            tabs.add(invitationTab);
        }
        invitationDescriptionText.setFont(new java.awt.Font("Dialog", 0, 12));
        invitationDescriptionText.setText("Confirm the invitation \"%1\" from \"%2\" to join the JXTA Peer Group \"%3\".");
        invitationDescriptionText.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        if (!membership.getPSEConfig().isInitialized()) {
            try {
                AuthenticationCredential application = new AuthenticationCredential(group, "StringAuthentication", null);

                invitationAuthenticator = (StringAuthenticator) membership.apply(application);
            } catch (ProtocolNotSupportedException noAuthenticator) {
                throw new UndeclaredThrowableException(noAuthenticator, "String authenticator not available!");
            }

            // The invitation authenticator allows us to get the invitation
            // certificate even if we don't have a keystore password. The certificate
            // will be requestable via the local peer's peer id.
            X509Certificate invitationCert = invitationAuthenticator.getCertificate(new char[0], group.getPeerID());

            StringBuilder description = new StringBuilder(invitationDescriptionText.getText());

            String subjectName = PSEUtils.getCertSubjectCName(invitationCert);
            int replaceIdx = description.indexOf("%1");

            if ((-1 != replaceIdx) && (null != subjectName)) {
                description.replace(replaceIdx, replaceIdx + 2, subjectName);
            }

            String issuerName = PSEUtils.getCertIssuerCName(invitationCert);

            replaceIdx = description.indexOf("%2");
            if ((-1 != replaceIdx) && (null != issuerName)) {
                description.replace(replaceIdx, replaceIdx + 2, issuerName);
            }

            replaceIdx = description.indexOf("%3");
            if (-1 != replaceIdx) {
                String groupName = group.getPeerGroupName();

                if (null == groupName) {
                    groupName = "ID " + group.getPeerGroupID().toString();
                }
                description.replace(replaceIdx, replaceIdx + 2, groupName);
            }

            invitationDescriptionText.setText(description.toString());
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        invitationTab.add(invitationDescriptionText, gridBagConstraints);

        invitationPasswordLabel.setLabelFor(invitationPasswordField);
        invitationPasswordLabel.setText("Invitation Password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 3);
        invitationTab.add(invitationPasswordLabel, gridBagConstraints);

        invitationPasswordField.setColumns(16);
        invitationPasswordField.setToolTipText("Enter the password for the invitation");
        invitationPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invitationPasswordFieldActionPerformed(evt);
            }
        });
        invitationPasswordField.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                invitationPasswordFieldKeyReleased(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 2, 2, 4);
        invitationTab.add(invitationPasswordField, gridBagConstraints);

        invitationConfirmButton.setEnabled(!invitationTab.isEnabled());
        invitationConfirmButton.setText("Confirm");
        invitationConfirmButton.setToolTipText("Click to confirm the peer group invitation.");
        invitationConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invitationConfirmButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        invitationTab.add(invitationConfirmButton, gridBagConstraints);

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PSE Peer Group Sample");
        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosed(java.awt.event.WindowEvent evt) {
                swingUIClosed(evt);
            }
        });

        keyStorePasswordLabel.setLabelFor(keyStorePasswordField);
        keyStorePasswordLabel.setText("Key Store Password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.ipadx = 3;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 3, 1, 0);
        getContentPane().add(keyStorePasswordLabel, gridBagConstraints);

        keyStorePasswordField.setColumns(16);
        keyStorePasswordField.setNextFocusableComponent(invitationTab);
        keyStorePasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyStorePasswordFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 1, 2);
        getContentPane().add(keyStorePasswordField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 3;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        getContentPane().add(tabs, gridBagConstraints);

        authenticationStatus.setColumns(32);
        authenticationStatus.setEditable(false);
        authenticationStatus.setFont(new java.awt.Font("Dialog", 0, 10));
        authenticationStatus.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        authenticationStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                authenticationStatusActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 4, 0);
        getContentPane().add(authenticationStatus, gridBagConstraints);

        pack();
    }

    // </editor-fold>//GEN-END:initComponents

    private void memberPasswordFieldKeyReleasedHandler(java.awt.event.KeyEvent evt) { // GEN-FIRST:event_memberPasswordFieldKeyReleasedHandler
        if (null == memberAuthenticator) {
            try {
                AuthenticationCredential application = new AuthenticationCredential(group, "StringAuthentication", null);

                memberAuthenticator = (StringAuthenticator) membership.apply(application);
            } catch (ProtocolNotSupportedException noAuthenticator) {
                authenticationStatus.setText("Could not create authenticator: " + noAuthenticator.getMessage());
                return;
            }

            memberAuthenticator.setAuth1_KeyStorePassword(keyStorePasswordField.getPassword());
            memberAuthenticator.setAuth2Identity(group.getPeerID());
        }

        memberAuthenticator.setAuth3_IdentityPassword(memberPasswordField.getPassword());

        memberAuthenticateButton.setEnabled(memberAuthenticator.isReadyForJoin());
    }// GEN-LAST:event_memberPasswordFieldKeyReleasedHandler

    private void memberPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_memberPasswordFieldActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_memberPasswordFieldActionPerformed

    private void ownerPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ownerPasswordFieldActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_ownerPasswordFieldActionPerformed

    private void ownerPasswordFieldKeyReleasedHandler(java.awt.event.KeyEvent evt) { // GEN-FIRST:event_ownerPasswordFieldKeyReleasedHandler
        if (null == ownerAuthenticator) {
            try {
                AuthenticationCredential application = new AuthenticationCredential(group, "StringAuthentication", null);

                ownerAuthenticator = (StringAuthenticator) membership.apply(application);
            } catch (ProtocolNotSupportedException noAuthenticator) {
                authenticationStatus.setText("Could not create authenticator: " + noAuthenticator.getMessage());
                return;
            }

            ownerAuthenticator.setAuth1_KeyStorePassword(keyStorePasswordField.getPassword());
            ownerAuthenticator.setAuth2Identity(group.getPeerGroupID());
        }

        ownerAuthenticator.setAuth3_IdentityPassword(ownerPasswordField.getPassword());

        ownerAuthenticateButton.setEnabled(ownerAuthenticator.isReadyForJoin());
    }// GEN-LAST:event_ownerPasswordFieldKeyReleasedHandler

    private void adminInviteButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_adminInviteButtonActionPerformed
        if (null == memberCredential) {
            authenticationStatus.setText("Not authenticated -- cannot create invitation.");
            return;
        }

        X509Certificate[] issuerChain = memberCredential.getCertificateChain();

        PrivateKey issuerKey = null;

        try {
            issuerKey = memberCredential.getPrivateKey();
        } catch (IllegalStateException notLocal) {
            ;
        }

        if (null == issuerKey) {
            authenticationStatus.setText("Member credential is not a local login credential.");
            return;
        }

        if (issuerChain.length < 2) {
            authenticationStatus.setText("Member credential is not certified as a Peer Group Administrator.");
            return;
        }

        if (!issuerChain[1].getPublicKey().equals(Main.PSE_SAMPLE_GROUP_ROOT_CERT.getPublicKey())) {
            authenticationStatus.setText("Member credential is not certified as a Peer Group Administrator.");
            return;
        }

        // Build the Module Impl Advertisemet we will use for our group.
        ModuleImplAdvertisement pseImpl = Main.build_psegroup_impl_adv(parentgroup);

        // Publish the Module Impl Advertisement to the group where the
        // peergroup will be advertised. This should be done in every peer
        // group in which the Peer Group is also advertised.
        // We use the same expiration and lifetime that the Peer Group Adv
        // will use (the default).
        try {
            parentgroup.getDiscoveryService().publish(pseImpl, PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
        } catch (IOException failed) {
            ;
        }

        PeerGroupAdvertisement pse_pga = null;

        PSEUtils.IssuerInfo issuer = new PSEUtils.IssuerInfo();

        issuer.cert = issuerChain[0];
        issuer.subjectPkey = issuerKey;

        PSEUtils.IssuerInfo newcert = PSEUtils.genCert("Invitation", issuer);

        List<X509Certificate> chain = new ArrayList<X509Certificate>();

        chain.add(newcert.cert);
        chain.addAll(Arrays.asList(issuerChain));

        EncryptedPrivateKeyInfo encryptedInvitationKey = PSEUtils.pkcs5_Encrypt_pbePrivateKey(
                adminInvitationPasswordField.getPassword(), newcert.subjectPkey, 10000);

        // Create the invitation.
        pse_pga = Main.build_psegroup_adv(pseImpl, (X509Certificate[]) chain.toArray(new X509Certificate[chain.size()])
                ,
                encryptedInvitationKey);

        XMLDocument asXML = (XMLDocument) pse_pga.getDocument(MimeMediaType.XMLUTF8);

        try {
            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter invitation_file = new FileWriter(fc.getSelectedFile());

                asXML.sendToWriter(invitation_file);

                invitation_file.close();

                authenticationStatus.setText("Invitation created as file : " + fc.getSelectedFile().getAbsolutePath());
            } else {
                authenticationStatus.setText("Invitation creation cancelled.");
            }
        } catch (IOException failed) {
            authenticationStatus.setText("Failed invitation creation : " + failed);
        }
    }// GEN-LAST:event_adminInviteButtonActionPerformed

    private void ownerSignCSRButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_ownerSignCSRButtonActionPerformed
        if (null == ownerCredential) {
            authenticationStatus.setText("Not authenticated -- cannot sign certificates.");
            return;
        }

        PSEUtils.IssuerInfo issuer = null;
        X509Certificate[] issuerChain = null;

        issuerChain = ownerCredential.getCertificateChain();

        PrivateKey issuerKey = null;

        try {
            issuerKey = ownerCredential.getPrivateKey();
        } catch (IllegalStateException notLocal) {
            ;
        }

        if (null == issuerKey) {
            authenticationStatus.setText("Owner credential is not a local login credential.");
            return;
        }

        issuer = new PSEUtils.IssuerInfo();

        issuer.cert = issuerChain[0];
        issuer.subjectPkey = issuerKey;
        org.bouncycastle.jce.PKCS10CertificationRequest csr;

        try {
            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showOpenDialog(this);

            XMLDocument csr_doc = null;

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileReader csr_file = new FileReader(fc.getSelectedFile());

                csr_doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, csr_file);

                csr_file.close();
            } else {
                authenticationStatus.setText("Certificate signing cancelled.");
                return;
            }

            net.jxta.impl.protocol.CertificateSigningRequest csr_msg = new net.jxta.impl.protocol.CertificateSigningRequest(
                    csr_doc);

            csr = csr_msg.getCSR();
        } catch (IOException failed) {
            authenticationStatus.setText("Failed to read certificate signing request: " + failed);
            return;
        }

        // set validity 10 years from today
        Date today = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(today);
        cal.add(Calendar.DATE, 10 * 365);
        Date until = cal.getTime();

        // generate cert
        try {
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.setIssuerDN(new X509Principal(true, issuer.cert.getSubjectX500Principal().getName()));
            certGen.setSubjectDN(csr.getCertificationRequestInfo().getSubject());
            certGen.setNotBefore(today);
            certGen.setNotAfter(until);
            certGen.setPublicKey(csr.getPublicKey());
            // certGen.setSignatureAlgorithm("SHA1withDSA");
            certGen.setSignatureAlgorithm("SHA1withRSA");
            // FIXME bondolo 20040317 needs fixing.
            certGen.setSerialNumber(BigInteger.valueOf(1));

            // return issuer info for generating service cert

            // the cert
            X509Certificate newCert = certGen.generateX509Certificate(issuer.subjectPkey);

            net.jxta.impl.protocol.Certificate cert_msg = new net.jxta.impl.protocol.Certificate();

            List<X509Certificate> newChain = new ArrayList<X509Certificate>(Arrays.asList(issuerChain));

            newChain.add(0, newCert);

            cert_msg.setCertificates(newChain);

            XMLDocument asXML = (XMLDocument) cert_msg.getDocument(MimeMediaType.XMLUTF8);

            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter csr_file = new FileWriter(fc.getSelectedFile());

                asXML.sendToWriter(csr_file);

                csr_file.close();

                authenticationStatus.setText("Signed admin certificate saved.");
            } else {
                authenticationStatus.setText("Save admin certificate cancelled.");
            }
        } catch (NoSuchAlgorithmException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (NoSuchProviderException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (InvalidKeyException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (SignatureException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (IOException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        }
    }// GEN-LAST:event_ownerSignCSRButtonActionPerformed

    private void ownerResignButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_ownerResignButtonActionPerformed
        if (null == ownerCredential) {
            authenticationStatus.setText("Already resigned.");
            return;
        }

        ownerCredential = null;
    }// GEN-LAST:event_ownerResignButtonActionPerformed

    private void ownerAuthenticateButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_ownerAuthenticateButtonActionPerformed
        if (null == membership.getDefaultCredential()) {
            // if the keychain hasn't been unlocked then set the keystore password.
            membership.getPSEConfig().setKeyStorePassword(keyStorePasswordField.getPassword());
        }

        StringAuthenticator ownerAuthenticator = null;

        try {
            AuthenticationCredential application = new AuthenticationCredential(group, "StringAuthentication", null);

            ownerAuthenticator = (StringAuthenticator) membership.apply(application);
        } catch (ProtocolNotSupportedException noAuthenticator) {
            authenticationStatus.setText("Could not create authenticator: " + noAuthenticator.getMessage());
            return;
        }

        ownerAuthenticator.setAuth1_KeyStorePassword(keyStorePasswordField.getPassword());
        ownerAuthenticator.setAuth2Identity(group.getPeerGroupID());
        ownerAuthenticator.setAuth3_IdentityPassword(ownerPasswordField.getPassword());

        // clear the password
        ownerPasswordField.setText("");

        try {
            ownerCredential = (PSECredential) membership.join(ownerAuthenticator);

            authenticationStatus.setText("Owner authentication successful.");
        } catch (PeerGroupException failed) {
            authenticationStatus.setText("Owner authentication failed: " + failed.getMessage());
        }
    }// GEN-LAST:event_ownerAuthenticateButtonActionPerformed

    private void memberResignButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_memberResignButtonActionPerformed
        if (null == memberCredential) {
            authenticationStatus.setText("Already resigned.");
            return;
        }

        memberGenerateCSRButton.setEnabled(false);
        memberResignButton.setEnabled(false);

        memberCredential = null;
    }// GEN-LAST:event_memberResignButtonActionPerformed

    private void memberImportCertButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_memberImportCertButtonActionPerformed
        if (null == memberCredential) {
            authenticationStatus.setText("Not authenticated -- cannot import certificates.");
            return;
        }

        JFileChooser fc = new JFileChooser();

        // In response to a button click:
        int returnVal = fc.showOpenDialog(this);

        XMLDocument certs_doc = null;

        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileReader certs_file = new FileReader(fc.getSelectedFile());

                certs_doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, certs_file);

                certs_file.close();
            } else {
                authenticationStatus.setText("Certificate import cancelled.");
                return;
            }
        } catch (IOException failed) {
            authenticationStatus.setText("Certificate import failed: " + failed.getMessage());
        }

        Certificate cert_msg = new Certificate(certs_doc);

        try {
            Iterator<X509Certificate> sourceChain = Arrays.asList(cert_msg.getCertificates()).iterator();

            int imported = 0;
            X509Certificate aCert = sourceChain.next();
            ID createid = group.getPeerGroupID();

            do {
                if (null != membership.getPSEConfig().getTrustedCertificateID(aCert)) {
                    break;
                }

                membership.getPSEConfig().erase(createid);
                membership.getPSEConfig().setTrustedCertificate(createid, aCert);
                imported++;

                // create a codat id for the next certificate in the chain.
                aCert = null;
                if (sourceChain.hasNext()) {
                    aCert = sourceChain.next();

                    if (null != membership.getPSEConfig().getTrustedCertificateID(aCert)) {
                        // it's already in the pse, time to bail!
                        break;
                    }

                    byte[] der = aCert.getEncoded();

                    createid = IDFactory.newCodatID(group.getPeerGroupID(), new ByteArrayInputStream(der));
                }
            } while (null != aCert);

            authenticationStatus.setText(" Imported " + imported + " certificates. ");
        } catch (CertificateEncodingException failure) {
            authenticationStatus.setText("Bad certificate: " + failure);
        } catch (KeyStoreException failure) {
            authenticationStatus.setText("KeyStore failure while importing certificate: " + failure);
        } catch (IOException failure) {
            authenticationStatus.setText("IO failure while importing certificate: " + failure);
        }
    }// GEN-LAST:event_memberImportCertButtonActionPerformed

    private void adminSignCSRButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_adminSignCSRButtonActionPerformed
        if (null == memberCredential) {
            authenticationStatus.setText("Not authenticated -- cannot sign certificates.");
            return;
        }

        PSEUtils.IssuerInfo issuer = null;
        X509Certificate[] issuerChain = null;

        issuerChain = memberCredential.getCertificateChain();

        PrivateKey issuerKey = null;

        try {
            issuerKey = memberCredential.getPrivateKey();
        } catch (IllegalStateException notLocal) {
            ;
        }

        if (null == issuerKey) {
            authenticationStatus.setText("Credential is not a local login credential.");
            return;
        }

        issuer = new PSEUtils.IssuerInfo();

        issuer.cert = issuerChain[0];
        issuer.subjectPkey = issuerKey;
        org.bouncycastle.jce.PKCS10CertificationRequest csr;

        try {
            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showOpenDialog(this);

            XMLDocument csr_doc = null;

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileReader csr_file = new FileReader(fc.getSelectedFile());

                csr_doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, csr_file);

                csr_file.close();
            } else {
                authenticationStatus.setText("Certificate Signing cancelled.");
                return;
            }

            net.jxta.impl.protocol.CertificateSigningRequest csr_msg = new net.jxta.impl.protocol.CertificateSigningRequest(
                    csr_doc);

            csr = csr_msg.getCSR();
        } catch (IOException failed) {
            authenticationStatus.setText("Failed to read certificate signing request: " + failed);
            return;
        }

        // set validity 10 years from today
        Date today = new Date();
        Calendar cal = Calendar.getInstance();

        cal.setTime(today);
        cal.add(Calendar.DATE, 10 * 365);
        Date until = cal.getTime();

        // generate cert
        try {
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.setIssuerDN(new X509Principal(true, issuer.cert.getSubjectX500Principal().getName()));
            certGen.setSubjectDN(csr.getCertificationRequestInfo().getSubject());
            certGen.setNotBefore(today);
            certGen.setNotAfter(until);
            certGen.setPublicKey(csr.getPublicKey());
            // certGen.setSignatureAlgorithm("SHA1withDSA");
            certGen.setSignatureAlgorithm("SHA1withRSA");
            // FIXME bondolo 20040317 needs fixing.
            certGen.setSerialNumber(BigInteger.valueOf(1));

            // return issuer info for generating service cert

            // the cert
            X509Certificate newCert = certGen.generateX509Certificate(issuer.subjectPkey);

            net.jxta.impl.protocol.Certificate cert_msg = new net.jxta.impl.protocol.Certificate();

            List<X509Certificate> newChain = new ArrayList<X509Certificate>(Arrays.asList(issuerChain));

            newChain.add(0, newCert);

            cert_msg.setCertificates(newChain);

            XMLDocument asXML = (XMLDocument) cert_msg.getDocument(MimeMediaType.XMLUTF8);

            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter csr_file = new FileWriter(fc.getSelectedFile());

                asXML.sendToWriter(csr_file);

                csr_file.close();

                authenticationStatus.setText("Signed certificate saved.");
            } else {
                authenticationStatus.setText("Save certificate cancelled.");
            }
        } catch (NoSuchAlgorithmException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (NoSuchProviderException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (InvalidKeyException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (SignatureException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        } catch (IOException failed) {
            authenticationStatus.setText("Certificate signing failed:" + failed.getMessage());
        }
    }// GEN-LAST:event_adminSignCSRButtonActionPerformed

    private void memberGenerateCSRButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_memberGenerateCSRButtonActionPerformed
        if (null == memberCredential) {
            authenticationStatus.setText("Not authenticated -- cannot generate Certificate Signing Request.");
            return;
        }

        X509Certificate cert = memberCredential.getCertificate();

        PrivateKey key = null;

        try {
            key = memberCredential.getPrivateKey();
        } catch (IllegalStateException notLocal) {
            ;
        }

        if (null == key) {
            authenticationStatus.setText("Credential is not a local login credential.");
            return;
        }

        try {
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest("SHA1withRSA"
                    ,
                    new X509Principal(cert.getSubjectX500Principal().getEncoded()), cert.getPublicKey(), new DERSet(), key);

            net.jxta.impl.protocol.CertificateSigningRequest csr_msg = new net.jxta.impl.protocol.CertificateSigningRequest();

            csr_msg.setCSR(csr);

            XMLDocument asXML = (XMLDocument) csr_msg.getDocument(MimeMediaType.XMLUTF8);

            JFileChooser fc = new JFileChooser();

            // In response to a button click:
            int returnVal = fc.showSaveDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                FileWriter csr_file = new FileWriter(fc.getSelectedFile());

                asXML.sendToWriter(csr_file);

                csr_file.close();

                authenticationStatus.setText(
                        "Certificate Signing Request saved as file: " + fc.getSelectedFile().getCanonicalPath());
            } else {
                authenticationStatus.setText("Certificate Signing Request not saved.");
            }
        } catch (NoSuchAlgorithmException failed) {
            authenticationStatus.setText("Certificate Signing Request generation failed:" + failed.getMessage());
        } catch (NoSuchProviderException failed) {
            authenticationStatus.setText("Certificate Signing Request generation failed:" + failed.getMessage());
        } catch (InvalidKeyException failed) {
            authenticationStatus.setText("Certificate Signing Request generation failed:" + failed.getMessage());
        } catch (SignatureException failed) {
            authenticationStatus.setText("Certificate Signing Request generation failed:" + failed.getMessage());
        } catch (IOException failed) {
            authenticationStatus.setText("Certificate Signing Request generation failed:" + failed.getMessage());
        }
    }// GEN-LAST:event_memberGenerateCSRButtonActionPerformed

    private void memberAuthenticateButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_memberAuthenticateButtonActionPerformed
        if (null != memberCredential) {
            authenticationStatus.setText("Already authenticated.");
            return;
        }

        StringAuthenticator memberAuthenticator = null;

        try {
            AuthenticationCredential application = new AuthenticationCredential(group, "StringAuthentication", null);

            memberAuthenticator = (StringAuthenticator) membership.apply(application);
        } catch (ProtocolNotSupportedException noAuthenticator) {
            authenticationStatus.setText("Could not create authenticator: " + noAuthenticator.getMessage());
            return;
        }

        memberAuthenticator.setAuth1_KeyStorePassword(keyStorePasswordField.getPassword());
        memberAuthenticator.setAuth2Identity(group.getPeerID());
        memberAuthenticator.setAuth3_IdentityPassword(memberPasswordField.getPassword());

        // clear the password
        memberPasswordField.setText("");

        try {
            memberCredential = (PSECredential) membership.join(memberAuthenticator);

            authenticationStatus.setText("Member authentication successful.");
        } catch (PeerGroupException failed) {
            authenticationStatus.setText("Member authentication failed: " + failed.getMessage());
            return;
        }

        X509Certificate[] chain = memberCredential.getCertificateChain();

        memberGenerateCSRButton.setEnabled(true);
        memberResignButton.setEnabled(true);

        if (chain.length > 1) {
            // If there's a certificate chain then perhaps admin and owner
            // be should enabled.
            if (chain[1].getPublicKey().equals(Main.PSE_SAMPLE_GROUP_ROOT_CERT.getPublicKey())) {
                // Signed by the root? That makes us an admin and maybe an owner
                tabs.add(adminTab);
                tabs.add(ownerTab);
            }
        }
    }// GEN-LAST:event_memberAuthenticateButtonActionPerformed

    private void swingUIClosed(java.awt.event.WindowEvent evt) { // GEN-FIRST:event_swingUIClosed
        // Shutdown the pse peer group.
        group.stopApp();
        group.unref();

        // Un-reference the parent peer group.
        parentgroup.unref();
    }// GEN-LAST:event_swingUIClosed

    private void invitationPasswordFieldKeyReleased(java.awt.event.KeyEvent evt) { // GEN-FIRST:event_invitationPasswordFieldKeyReleased
        invitationAuthenticator.setAuth3_IdentityPassword(invitationPasswordField.getPassword());

        invitationConfirmButton.setEnabled(invitationAuthenticator.isReadyForJoin());
    }// GEN-LAST:event_invitationPasswordFieldKeyReleased

    private void invitationConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_invitationConfirmButtonActionPerformed
        boolean ownerInvite = invitationAuthenticator.getCertificate(null, group.getPeerID()).getPublicKey().equals(
                Main.PSE_SAMPLE_GROUP_ROOT_CERT.getPublicKey());

        invitationAuthenticator.setAuth1_KeyStorePassword(keyStorePasswordField.getPassword());
        if (ownerInvite) {
            // If the invitation is for the owner identity then store it under the peer group id.
            invitationAuthenticator.setAuth2Identity(group.getPeerGroupID());
        } else {
            // Otherwise store it under another random key.
            invitationAuthenticator.setAuth2Identity(IDFactory.newCodatID(group.getPeerGroupID()));
        }
        invitationAuthenticator.setAuth3_IdentityPassword(invitationPasswordField.getPassword());

        // clear the password
        invitationPasswordField.setText("");

        try {
            invitationCredential = (PSECredential) membership.join(invitationAuthenticator);

            tabs.remove(invitationTab);
            tabs.add(memberTab);
            if (ownerInvite) {
                tabs.add(ownerTab);
            }
            authenticationStatus.setText("Invitation confirmed.");
        } catch (PeerGroupException failed) {
            authenticationStatus.setText("Invitation confirmation failed: " + failed.getMessage());
        }
    }// GEN-LAST:event_invitationConfirmButtonActionPerformed

    private void invitationPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_invitationPasswordFieldActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_invitationPasswordFieldActionPerformed

    private void keyStorePasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_keyStorePasswordFieldActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_keyStorePasswordFieldActionPerformed

    private void generateMemberCertButtonActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_generateMemberCertButtonActionPerformed
        try {
            X509Certificate checkCert = membership.getPSEConfig().getTrustedCertificate(group.getPeerID());

            if (null != checkCert) {
                authenticationStatus.setText("Member certificate already present.");
            }

            PSEUtils.IssuerInfo issuer = null;

            if (null != invitationCredential) {
                issuer = new PSEUtils.IssuerInfo();

                issuer.cert = invitationCredential.getCertificate();
                issuer.subjectPkey = invitationCredential.getPrivateKey();
            }

            PSEUtils.IssuerInfo certs = PSEUtils.genCert(group.getPeerName(), issuer);
            X509Certificate chain[];

            if (null != issuer) {
                chain = new X509Certificate[] { certs.cert, certs.issuer};
            } else {
                chain = new X509Certificate[] { certs.cert};
            }

            if (null == membership.getDefaultCredential()) {
                // if the keychain hasn't been unlocked then set the keystore password.
                membership.getPSEConfig().setKeyStorePassword(keyStorePasswordField.getPassword());
            }

            // Save our new certificate into the keystore.
            membership.getPSEConfig().setKey(group.getPeerID(), chain, certs.subjectPkey, memberPasswordField.getPassword());

            authenticationStatus.setText("New member certificate generated.");
            memberAuthenticateButton.setEnabled(true);
        } catch (KeyStoreException failed) {
            authenticationStatus.setText("Certificate generation failed: " + failed.getMessage());
        } catch (IOException failed) {
            authenticationStatus.setText("Certificate generation failed: " + failed.getMessage());
        }

    }// GEN-LAST:event_generateMemberCertButtonActionPerformed

    private void authenticationStatusActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_authenticationStatusActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_authenticationStatusActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPasswordField adminInvitationPasswordField;
    private javax.swing.JLabel adminInvitationPasswordLabel;
    private javax.swing.JButton adminInviteButton;
    private javax.swing.JButton adminSignCSRButton;
    private javax.swing.JPanel adminTab;
    private javax.swing.JTextField authenticationStatus;
    private javax.swing.JButton generateMemberCertButton;
    private javax.swing.JButton invitationConfirmButton;
    private javax.swing.JPasswordField invitationPasswordField;
    private javax.swing.JPanel invitationTab;
    private javax.swing.JPasswordField keyStorePasswordField;
    private javax.swing.JLabel keyStorePasswordLabel;
    private javax.swing.JButton memberAuthenticateButton;
    private javax.swing.JButton memberGenerateCSRButton;
    private javax.swing.JButton memberImportCertButton;
    private javax.swing.JPasswordField memberPasswordField;
    private javax.swing.JButton memberResignButton;
    private javax.swing.JPanel memberTab;
    private javax.swing.JButton ownerAuthenticateButton;
    private javax.swing.JPasswordField ownerPasswordField;
    private javax.swing.JLabel ownerPasswordLabel;
    private javax.swing.JButton ownerResignButton;
    private javax.swing.JButton ownerSignCSRButton;
    private javax.swing.JPanel ownerTab;
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables
}
