/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.util;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 *
 * @author  Me
 */
public class Password extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;
	
	/** Creates new form Password */
    private Password(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.actionOK=new ActionOK("OK");
        this.actionCancel=new ActionCancel("Cancel");
        initComponents();
        this.jButtonOK.setAction(this.actionOK);
        this.jPasswordFieldPasswordValue.setAction(this.actionOK);
        this.jButtonCancel.setAction(this.actionCancel);
    }
    public final static synchronized Password singleton()
    {
        if(myself==null)
        {
            myself = new Password(new javax.swing.JFrame(), true);
            myself.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    JOptionPane.showMessageDialog(null, "No password provided. Can't proceed to use the system.");
                    
                    System.exit(0);
                }
            });
        }
        return myself;
    }
    
    public void setUsername(String paramUsername)
    {
        this.username=paramUsername;
    }
    public char[] getPassword()
    {
        if(Boolean.getBoolean(Password.class.getName()+".enableDebug"))
        {
            return "mypassword".toCharArray();
        }
        try
        {
            if(this.password==null)
            {
                java.awt.EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        Password.this.jLabelPeerIDValue.setText(Password.this.username);
                        Password.this.setVisible(true);
                    }
                });
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return this.password;
    }
    
    public void resetPassword()
    {
        this.password=null;
        this.jPasswordFieldPasswordValue.setText("");
    }
    private class ActionOK extends AbstractAction
    {
		private static final long serialVersionUID = 1L;
		
		public ActionOK(String pName)
        {
            super(pName);
        }
        public void actionPerformed(ActionEvent e) {
            Password.this.password=Password.this.jPasswordFieldPasswordValue.getPassword();
            Password.this.setVisible(false);
        }
    }
    private class ActionCancel extends AbstractAction
    {
		private static final long serialVersionUID = 1L;

		public ActionCancel(String pName)
        {
            super(pName);
        }
        public void actionPerformed(ActionEvent e) 
        {
            Password.this.dispose();
            JOptionPane.showMessageDialog(null, "No correct password provided. Can't proceed to use the system.");
            System.exit(0);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanelUpper = new javax.swing.JPanel();
        jLabelPeerID = new javax.swing.JLabel();
        jLabelPeerIDValue = new javax.swing.JLabel();
        jLabelPassword = new javax.swing.JLabel();
        jPasswordFieldPasswordValue = new javax.swing.JPasswordField();
        jPanelLower = new javax.swing.JPanel();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jPanelUpper.setLayout(new java.awt.GridBagLayout());

        jLabelPeerID.setBackground(new java.awt.Color(204, 204, 255));
        jLabelPeerID.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelPeerID.setText("Peer ID:");
        jLabelPeerID.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jLabelPeerID.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelUpper.add(jLabelPeerID, gridBagConstraints);

        jLabelPeerIDValue.setBackground(new java.awt.Color(204, 204, 255));
        jLabelPeerIDValue.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelUpper.add(jLabelPeerIDValue, gridBagConstraints);

        jLabelPassword.setBackground(new java.awt.Color(204, 204, 255));
        jLabelPassword.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelPassword.setText("Password:");
        jLabelPassword.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jLabelPassword.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelUpper.add(jLabelPassword, gridBagConstraints);

        jPasswordFieldPasswordValue.setColumns(12);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 17;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        jPanelUpper.add(jPasswordFieldPasswordValue, gridBagConstraints);

        jPanelLower.setLayout(new java.awt.GridBagLayout());

        jButtonOK.setText("OK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 16;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        jPanelLower.add(jButtonOK, gridBagConstraints);

        jButtonCancel.setText("Cancel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        jPanelLower.add(jButtonCancel, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanelLower, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelUpper, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(41, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jPanelUpper, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(jPanelLower, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(34, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabelPassword;
    private javax.swing.JLabel jLabelPeerID;
    private javax.swing.JLabel jLabelPeerIDValue;
    private javax.swing.JPanel jPanelLower;
    private javax.swing.JPanel jPanelUpper;
    private javax.swing.JPasswordField jPasswordFieldPasswordValue;
    // End of variables declaration//GEN-END:variables
    private String username="";
    private char[] password;
    private static Password myself=null;
    private Action actionOK;
    private Action actionCancel;

}
