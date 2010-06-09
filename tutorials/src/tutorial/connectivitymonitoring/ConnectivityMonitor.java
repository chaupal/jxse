/*
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package tutorial.connectivitymonitoring;

import java.awt.Component;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import net.jxta.endpoint.EndpointService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

/**
 * This frame collects and displays connectivity information from a peergroup.
 */
public class ConnectivityMonitor extends JFrame implements Runnable {

    // Static

    public static final ScheduledExecutorService TheExecutor = Executors.newScheduledThreadPool(5);

    // Attributes

    private final JFrame ThisFrame;

    private PeerGroup ThePeerGroup = null;
    
    private Future TheMonitorFuture = null;
    
    private DefaultTableModel LocalRDVs_TM = null;
    private String[] LocalRDV_Col = { "Local RDV View IDs" };

    private DefaultTableModel LocalEdges_TM = null;
    private String[] LocalEdge_Col = { "Local EDGE View IDs" };

    private static final String[][] EmptyTableContent = new String[0][1];

    private ArrayList<LogEntry> TheLogs = new ArrayList<LogEntry>();

    private static final SimpleDateFormat TheDateFormat =
        new SimpleDateFormat("'['HH:mm:ss']'");

    /** Creates new form ConnectivityMonitor */
    public ConnectivityMonitor(PeerGroup inGroup) {

        // Registering as rendezvous event listener
        inGroup.getRendezVousService().addListener(new RdvEventMonitor(this));

        // Registering this JFrame
        ThisFrame = this;

        // JFrame initialization
        initComponents();

        // Displaying the frame on the awt queue
        SetDefaultLookAndFeel();
        putScreenAtTheCenter(this);
        resettingFrameValues();

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ThisFrame.setVisible(true);
            }
        });
        
        // Initialization
        ThePeerGroup = inGroup;

        // Setting own default table models
        LocalRDVs_TM = new DefaultTableModel(EmptyTableContent, LocalRDV_Col);
        this.LocalRDVTable.setModel(LocalRDVs_TM);

        LocalEdges_TM = new DefaultTableModel(EmptyTableContent, LocalEdge_Col);
        this.LocalEdgeTable.setModel(LocalEdges_TM);

        // Starting the monitor
        TheLogs.add(new LogEntry(new Date(System.currentTimeMillis()),
            "Starting to monitor the peergroup"));

        TheMonitorFuture = TheExecutor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);

    }

    public static void SetDefaultLookAndFeel() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException Ex) {
            System.err.println(Ex.toString());
        } catch (InstantiationException Ex) {
            System.err.println(Ex.toString());
        } catch (IllegalAccessException Ex) {
            System.err.println(Ex.toString());
        } catch (UnsupportedLookAndFeelException Ex) {
            System.err.println(Ex.toString());

        }

    }

    public static void putScreenAtTheCenter(Component TheComponent) {

        // Retrieving horizontal value
        int WidthPosition = (Toolkit.getDefaultToolkit().getScreenSize().width
                - TheComponent.getWidth()) / 2;

        int HeightPosition = (Toolkit.getDefaultToolkit().getScreenSize().height
                - TheComponent.getHeight()) / 2;

        TheComponent.setLocation(WidthPosition, HeightPosition);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        StatusPane = new javax.swing.JPanel();
        PeerIDLabel = new javax.swing.JLabel();
        PeerNameTextField = new javax.swing.JTextField();
        PeerGroupIDLabel = new javax.swing.JLabel();
        PeerGroupNameTextField = new javax.swing.JTextField();
        ParentGroupLabel = new javax.swing.JLabel();
        ParentGroupNameTextField = new javax.swing.JTextField();
        PeerIDTextField = new javax.swing.JTextField();
        PeerGroupIDTextField = new javax.swing.JTextField();
        ParentGroupIDTextField = new javax.swing.JTextField();
        ScrollLogPane = new javax.swing.JScrollPane();
        LogPane = new javax.swing.JTextPane();
        DisplayPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        LocalEdgeTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        LocalRDVTable = new javax.swing.JTable();
        RelayIDTextField = new javax.swing.JTextField();
        IsConnectedToRelayCheckBox = new javax.swing.JCheckBox();
        IsConnectedToRDVCheckBox = new javax.swing.JCheckBox();
        CurrentModeLabel2 = new javax.swing.JLabel();
        AliveRadioButton = new javax.swing.JRadioButton();
        IsRDVCheckBox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        PeerIDLabel.setFont(new java.awt.Font("Tahoma", 1, 11));
        PeerIDLabel.setText("Peer");

        PeerNameTextField.setEditable(false);
        PeerNameTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        PeerGroupIDLabel.setFont(new java.awt.Font("Tahoma", 1, 11));
        PeerGroupIDLabel.setText("Peer Group");

        PeerGroupNameTextField.setEditable(false);
        PeerGroupNameTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        ParentGroupLabel.setFont(new java.awt.Font("Tahoma", 1, 11));
        ParentGroupLabel.setText("Parent Group");

        ParentGroupNameTextField.setEditable(false);
        ParentGroupNameTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        PeerIDTextField.setEditable(false);
        PeerIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        PeerGroupIDTextField.setEditable(false);
        PeerGroupIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        ParentGroupIDTextField.setEditable(false);
        ParentGroupIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9));

        LogPane.setEditable(false);
        LogPane.setFont(new java.awt.Font("Tahoma", 0, 9));
        LogPane.setName("LogPane"); // NOI18N
        ScrollLogPane.setViewportView(LogPane);

        javax.swing.GroupLayout StatusPaneLayout = new javax.swing.GroupLayout(StatusPane);
        StatusPane.setLayout(StatusPaneLayout);
        StatusPaneLayout.setHorizontalGroup(
            StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StatusPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ScrollLogPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addComponent(PeerGroupIDTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addComponent(PeerIDTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, StatusPaneLayout.createSequentialGroup()
                        .addComponent(PeerIDLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PeerNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, StatusPaneLayout.createSequentialGroup()
                        .addComponent(ParentGroupLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ParentGroupNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, StatusPaneLayout.createSequentialGroup()
                        .addComponent(PeerGroupIDLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PeerGroupNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE))
                    .addComponent(ParentGroupIDTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE))
                .addContainerGap())
        );
        StatusPaneLayout.setVerticalGroup(
            StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PeerIDLabel)
                    .addComponent(PeerNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PeerIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PeerGroupIDLabel)
                    .addComponent(PeerGroupNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PeerGroupIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StatusPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ParentGroupLabel)
                    .addComponent(ParentGroupNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ParentGroupIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(ScrollLogPane, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
        );

        LocalEdgeTable.setFont(new java.awt.Font("Tahoma", 0, 9));
        LocalEdgeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                "Local Edge View IDs"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(LocalEdgeTable);
        LocalEdgeTable.getColumnModel().getColumn(0).setResizable(false);

        LocalRDVTable.setFont(new java.awt.Font("Tahoma", 0, 9));
        LocalRDVTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                "Local RDV View IDs"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(LocalRDVTable);
        LocalRDVTable.getColumnModel().getColumn(0).setResizable(false);

        RelayIDTextField.setEditable(false);
        RelayIDTextField.setFont(new java.awt.Font("Tahoma", 0, 9)); // NOI18N

        IsConnectedToRelayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11));
        IsConnectedToRelayCheckBox.setText("is connected to Relay");
        IsConnectedToRelayCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IsConnectedToRelayCheckBoxActionPerformed(evt);
            }
        });

        IsConnectedToRDVCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11));
        IsConnectedToRDVCheckBox.setText("is connected to RDV");
        IsConnectedToRDVCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IsConnectedToRDVCheckBoxActionPerformed(evt);
            }
        });

        CurrentModeLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        CurrentModeLabel2.setText("Relay ID");

        AliveRadioButton.setFont(new java.awt.Font("Tahoma", 1, 11));
        AliveRadioButton.setText("Alive");

        IsRDVCheckBox.setFont(new java.awt.Font("Tahoma", 1, 11));
        IsRDVCheckBox.setText("is RDV");
        IsRDVCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IsRDVCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout DisplayPanelLayout = new javax.swing.GroupLayout(DisplayPanel);
        DisplayPanel.setLayout(DisplayPanelLayout);
        DisplayPanelLayout.setHorizontalGroup(
            DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DisplayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addComponent(RelayIDTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, DisplayPanelLayout.createSequentialGroup()
                        .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CurrentModeLabel2)
                            .addComponent(AliveRadioButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 93, Short.MAX_VALUE)
                        .addComponent(IsRDVCheckBox)
                        .addGap(18, 18, 18)
                        .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(IsConnectedToRDVCheckBox)
                            .addComponent(IsConnectedToRelayCheckBox))))
                .addContainerGap())
        );
        DisplayPanelLayout.setVerticalGroup(
            DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, DisplayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IsConnectedToRDVCheckBox)
                    .addComponent(AliveRadioButton)
                    .addComponent(IsRDVCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(DisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(IsConnectedToRelayCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CurrentModeLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                .addComponent(RelayIDTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(StatusPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DisplayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(DisplayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(StatusPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void IsConnectedToRelayCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IsConnectedToRelayCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IsConnectedToRelayCheckBoxActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed

        // Stopping monitor task
        stopMonitorTask();

    }//GEN-LAST:event_formWindowClosed

    private void IsConnectedToRDVCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IsConnectedToRDVCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IsConnectedToRDVCheckBoxActionPerformed

    private void IsRDVCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IsRDVCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IsRDVCheckBoxActionPerformed

    private synchronized void stopMonitorTask() {

        if ( TheMonitorFuture != null ) {
            TheMonitorFuture.cancel(false);
        }

    }

    @Override
    protected void finalize() {

        // Stopping monitor task
        stopMonitorTask();

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton AliveRadioButton;
    private javax.swing.JLabel CurrentModeLabel2;
    private javax.swing.JPanel DisplayPanel;
    private javax.swing.JCheckBox IsConnectedToRDVCheckBox;
    private javax.swing.JCheckBox IsConnectedToRelayCheckBox;
    private javax.swing.JCheckBox IsRDVCheckBox;
    private javax.swing.JTable LocalEdgeTable;
    private javax.swing.JTable LocalRDVTable;
    private javax.swing.JTextPane LogPane;
    private javax.swing.JTextField ParentGroupIDTextField;
    private javax.swing.JLabel ParentGroupLabel;
    private javax.swing.JTextField ParentGroupNameTextField;
    private javax.swing.JLabel PeerGroupIDLabel;
    private javax.swing.JTextField PeerGroupIDTextField;
    private javax.swing.JTextField PeerGroupNameTextField;
    private javax.swing.JLabel PeerIDLabel;
    private javax.swing.JTextField PeerIDTextField;
    private javax.swing.JTextField PeerNameTextField;
    private javax.swing.JTextField RelayIDTextField;
    private javax.swing.JScrollPane ScrollLogPane;
    private javax.swing.JPanel StatusPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables

    public void resettingFrameValues() {
        
        // Resetting frame value
        this.setTitle("Connectivity Monitor");

        this.PeerNameTextField.setText("<unknown>");
        this.PeerIDTextField.setText("");

        this.PeerGroupNameTextField.setText("<unknown>");
        this.PeerGroupIDTextField.setText("");

        this.ParentGroupNameTextField.setText("<unknown>");
        this.ParentGroupIDTextField.setText("");

        this.IsConnectedToRelayCheckBox.setSelected(false);
        this.IsConnectedToRDVCheckBox.setSelected(false);

    }

    private void updateTableContent(DefaultTableModel inTM, List<String> inNewContent, String[] inColumns) {

        // Do we have the same number of elements
        if ( inTM.getRowCount() == inNewContent.size() ) {

            // Sorting new candidates
            Collections.sort(inNewContent);

            // Replacing items that have to be replaced
            for (int i=0;i<inNewContent.size();i++) {

                if ( inNewContent.get(i).compareTo((String) inTM.getValueAt(i, 0)) != 0 )
                    inTM.setValueAt(inNewContent.get(i), i, 0);

            }

            // Done
            return;

        }

        // We need a new data vector
        String[][] NewContent = new String[inNewContent.size()][1];
        for (int i=0;i<inNewContent.size();i++) NewContent[i][0] = inNewContent.get(i);
        inTM.setDataVector(NewContent, inColumns);

    }

    public void run() {

        // Alive notification
        this.AliveRadioButton.setSelected(!this.AliveRadioButton.isSelected());

        if ( this.ThePeerGroup == null ) {

            LogEntry TheEntry = new LogEntry(new Date(System.currentTimeMillis()),
                "Monitored peergroup is NULL");
            this.TheLogs.add(TheEntry);

            // Resetting frame value
            resettingFrameValues();

            return;

        }

        this.setTitle("Connectivity Monitor - "
                + ThePeerGroup.getPeerName() + " - "
                + ThePeerGroup.getPeerID().toString());

        this.PeerNameTextField.setText(ThePeerGroup.getPeerName());
        this.PeerIDTextField.setText(ThePeerGroup.getPeerID().toString());

        this.PeerGroupNameTextField.setText(ThePeerGroup.getPeerGroupName());
        this.PeerGroupIDTextField.setText(ThePeerGroup.getPeerGroupID().toString());

        PeerGroup ParentPG = this.ThePeerGroup.getParentGroup();

        if ( ParentPG != null) {

            this.ParentGroupNameTextField.setText(ParentPG.getPeerGroupName());
            this.ParentGroupIDTextField.setText(ParentPG.getPeerGroupID().toString());

        }

        RendezVousService TmpRDVS = this.ThePeerGroup.getRendezVousService();

        if ( TmpRDVS != null ) {

            this.IsRDVCheckBox.setSelected(TmpRDVS.isRendezVous());
            this.IsConnectedToRDVCheckBox.setSelected(TmpRDVS.isConnectedToRendezVous());

            List<PeerID> Items = TmpRDVS.getLocalRendezVousView();

            // Sorting Peer IDs
            List<String> StrItems = new ArrayList<String>();
            for (int i=0;i<Items.size();i++) 
                StrItems.add(Items.get(i).toString());

            updateTableContent(LocalRDVs_TM, StrItems, LocalRDV_Col);

        } else {

            TheLogs.add(new LogEntry(new Date(System.currentTimeMillis()),
                "Rendezvous service is NULL"));

        }

        EndpointService TmpES = this.ThePeerGroup.getEndpointService();

        if ( TmpES != null ) {

            Collection<PeerID> x = TmpES.getConnectedRelayPeers();

            if ( x.isEmpty() ) {

                this.IsConnectedToRelayCheckBox.setSelected(false);
                this.RelayIDTextField.setText("");

            } else {
                
                this.IsConnectedToRelayCheckBox.setSelected(true);
                PeerID[] TmpPID = x.toArray(new PeerID[x.size()]);

                this.RelayIDTextField.setText(TmpPID[0].toString());

            }

            List<PeerID> Items = TmpRDVS.getLocalEdgeView();

            // Sorting Peer IDs
            List<String> StrItems = new ArrayList<String>();
            for (int i=0;i<Items.size();i++) 
                StrItems.add(Items.get(i).toString());

            updateTableContent(LocalEdges_TM, StrItems, LocalEdge_Col);

        } else {

            TheLogs.add(new LogEntry(new Date(System.currentTimeMillis()),
                "Endpoint service is NULL"));

        }

        // Adding logs

        String Content = "";
        Collections.sort(TheLogs);

        for (int i=(TheLogs.size()-1);i>=0;i--)
            Content = Content + TheDateFormat.format(TheLogs.get(i).TheDate)
                + " " + TheLogs.get(i).TheLog + "\n";

        LogPane.setText(Content);

    }

    private static class LogEntry implements Comparable<LogEntry> {

        public Date TheDate = null;
        public String TheLog = null;

        public LogEntry(Date inDate, String inLog) {

            TheDate = inDate;
            TheLog = inLog;

        }

        public int compareTo(LogEntry o) {

            if ( o == null ) return 1;

            return TheDate.compareTo(o.TheDate);

        }

    }

    public static class RdvEventMonitor implements RendezvousListener {

        private ConnectivityMonitor TheMonitor = null;

        public RdvEventMonitor(ConnectivityMonitor inCM) {

            TheMonitor = inCM;

        }

        public void rendezvousEvent(RendezvousEvent event) {

            if ( event == null ) return;

            Date TimeStamp = new Date(System.currentTimeMillis());
            String Log = null;

            if ( event.getType() == RendezvousEvent.RDVCONNECT ) {
                Log = "Connection to RDV";
            } else if ( event.getType() == RendezvousEvent.RDVRECONNECT ) {
                Log = "Reconnection to RDV";
            } else if ( event.getType() == RendezvousEvent.CLIENTCONNECT ) {
                Log = "EDGE client connection";
            } else if ( event.getType() == RendezvousEvent.CLIENTRECONNECT ) {
                Log = "EDGE client reconnection";
            } else if ( event.getType() == RendezvousEvent.RDVDISCONNECT ) {
                Log = "Disconnection from RDV";
            } else if ( event.getType() == RendezvousEvent.RDVFAILED ) {
                Log = "Connection to RDV failed";
            } else if ( event.getType() == RendezvousEvent.CLIENTDISCONNECT ) {
                Log = "EDGE client disconnection from RDV";
            } else if ( event.getType() == RendezvousEvent.CLIENTFAILED ) {
                Log = "EDGE client connection to RDV failed";
            } else if ( event.getType() == RendezvousEvent.BECAMERDV ) {
                Log = "This peer became RDV";
            } else if ( event.getType() == RendezvousEvent.BECAMEEDGE ) {
                Log = "This peer became EDGE";
            }

            String TempPID = event.getPeer();
            if ( TempPID != null ) Log = Log + "\n  " + TempPID;

            // Adding the entry
            TheMonitor.TheLogs.add(new LogEntry(TimeStamp, Log));

        }

    }

}
