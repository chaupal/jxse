/*
 * ====================================================================
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxse.impl.OSGi.Services;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxse.OSGi.Services.JxseOSGiNetworkManagerService;
import net.jxse.configuration.JxseHttpTransportConfiguration;
import net.jxse.configuration.JxseMulticastTransportConfiguration;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxse.configuration.JxseTcpTransportConfiguration;
import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

/**
 * This class implements the {@code JxseOSGiNetworkManagerService} API.
 */
public class JxseOSGiNetworkManagerServiceImpl extends JxseOSGiNetworkManagerService {

    /**
     * Logging
     */
    private final static transient Logger LOG = Logger.getLogger(JxseOSGiNetworkManagerServiceImpl.class.getName());

    /**
     * Peer configuration to be used for the NetworkManager and the NetworkConfigurator
     */
    private JxsePeerConfiguration TheConfig = JxsePeerConfiguration.getDefaultJxsePeerConfiguration();

    private NetworkManager TheNM = null;

    /**
     * Sets the peer configuration. Throws an exception if the parameter is {@code null} or if
     * the NetworkManager is started.
     *
     * @param config The peer configuration
     * @throws JxtaConfigurationException If parameter is {@code null} or if NetworkManager is started.
     */
    @Override
    public void setPeerConfiguration(JxsePeerConfiguration config) throws JxtaConfigurationException {

        if (config==null) {
            LOG.severe("Null peer configuration");
            throw new JxtaConfigurationException("Null peer configuration");
        }
        
        if (this.TheNM!=null) {
            if (TheNM.isStarted()) {
                throw new JxtaConfigurationException("Cannot change configuration while NetworkManager is started");
            }
        }

        this.TheConfig = new JxsePeerConfiguration(config);

    }

    @Override
    public JxsePeerConfiguration getPeerConfigurationCopy() {

        return new JxsePeerConfiguration(this.TheConfig);
        
    }

    /**
     * {@inheritDoc}
     *
     * @throws JxtaConfigurationException if an issue is encountered with peer configuration.
     * @throws IOException if an issue is encountered when creating the {@code NetworkManager}.
     */
    @Override
    public NetworkManager getConfiguredNetworkManager() throws JxtaConfigurationException, IOException {

        if (this.TheNM!=null) {
            
            if (this.TheNM.isStarted()) {

                // We return current NetworkManager instance
                return TheNM;
                
            }

        }

        // Creating the NetworkManager
        TheNM = this.getNewConfiguredNetworkManager();

        // ...and return it
        return TheNM;

    }
    /**
     * Converts a {@code JxsePeerConfiguration.ConnectionMode} into a {@code NetworkManager.ConfigMode}.
     * 
     * @param mode A {@code JxsePeerConfiguration} connection mode
     * @return A {@code NetworkManager ConfigMode}
     */
    public static NetworkManager.ConfigMode convertToNetworkManagerConfigMode(JxsePeerConfiguration.ConnectionMode mode) {
        
        if (mode.compareTo(JxsePeerConfiguration.ConnectionMode.ADHOC)==0) {
            return NetworkManager.ConfigMode.ADHOC;
        } else if (mode.compareTo(JxsePeerConfiguration.ConnectionMode.EDGE)==0) {
            return NetworkManager.ConfigMode.EDGE;
        } else if (mode.compareTo(JxsePeerConfiguration.ConnectionMode.RELAY)==0) {
            return NetworkManager.ConfigMode.RELAY;
        } else if (mode.compareTo(JxsePeerConfiguration.ConnectionMode.RENDEZVOUS)==0) {
            return NetworkManager.ConfigMode.RENDEZVOUS;
        } else if (mode.compareTo(JxsePeerConfiguration.ConnectionMode.SUPER)==0) {
            return NetworkManager.ConfigMode.RENDEZVOUS_RELAY;
        }
        
        return null;
        
    }

    /**
     * Return a new {@code NetworkManager} instance configured with the available peer configuration
     * using the {@code NetworkConfigurator).
     *
     * @return a {@code NetworkManager} instance.
     * @throws JxtaConfigurationException if the peer configuration is invalid.
     * @throws IOException if there is an issue when creating the {@code NetworkManager}.
     */
    private NetworkManager getNewConfiguredNetworkManager() throws JxtaConfigurationException, IOException {

        // Preparing result
        NetworkManager Result = null;

        // Extracting constructor data
        JxsePeerConfiguration.ConnectionMode ExtractedMode = this.TheConfig.getConnectionMode();

        if (ExtractedMode==null) {
            LOG.severe("No connection mode available for NetworkManager !!!");
            throw new JxtaConfigurationException("No connection mode available for NetworkManager !!!");
        }

        LOG.log(Level.FINER, "Connection mode: " + ExtractedMode.toString());

        // Peer Instance Name
        String InstanceName = this.TheConfig.getPeerInstanceName();

        if (InstanceName==null) {
            InstanceName="";
        }

        LOG.log(Level.FINER, "Peer instance name: " + InstanceName);

        // Persistence location
        URI InstanceHome = this.TheConfig.getPersistenceLocation();

        LOG.log(Level.FINE, "Creating a NetworkManager instance");
        if (InstanceHome!=null) {
            Result = new NetworkManager(convertToNetworkManagerConfigMode(ExtractedMode), InstanceName, InstanceHome);
        } else {
            Result = new NetworkManager(convertToNetworkManagerConfigMode(ExtractedMode), InstanceName);
        }

        // Retrieving the NetworkConfigurator
        NetworkConfigurator TheNC = Result.getConfigurator();

        // Seed relays
        Map<Integer, URI> TheSeedRelays = this.TheConfig.getAllSeedRelays();

        for (URI Item : TheSeedRelays.values()) {
            LOG.log(Level.FINER, "Adding seed relay: " + Item.toString());
            TheNC.addSeedRelay(Item);
        }

        // Seed rendezvous
        Map<Integer, URI> TheSeedRDVs = this.TheConfig.getAllSeedRendezvous();

        for (URI Item : TheSeedRDVs.values()) {
            LOG.log(Level.FINER, "Adding seed rendezvous: " + Item.toString());
            TheNC.addSeedRendezvous(Item);
        }

        // Seeding relays
        Map<Integer, URI> TheSeedingRelays = this.TheConfig.getAllSeedingRelays();

        for (URI Item : TheSeedingRelays.values()) {
            LOG.log(Level.FINER, "Adding seeding relay: " + Item.toString());
            TheNC.addRelaySeedingURI(Item);
        }

        // Seeding rendezvous
        Map<Integer, URI> TheSeedingRDVs = this.TheConfig.getAllSeedingRendezvous();

        for (URI Item : TheSeedingRDVs.values()) {
            LOG.log(Level.FINER, "Adding seeding rendezvous: " + Item.toString());
            TheNC.addRdvSeedingURI(Item);
        }

        // Infrastructure ID
        PeerGroupID PGID = this.TheConfig.getInfrastructureID();
        
        if (PGID!=null) {
            TheNC.setInfrastructureID(PGID);
            LOG.log(Level.FINER, "Peer Group ID: " + PGID.toString());
        }

        // Persistence location
        URI KSLoc = this.TheConfig.getKeyStoreLocation();

        if (KSLoc!=null) {
            TheNC.setKeyStoreLocation(KSLoc);
            LOG.log(Level.FINER, "Keystore location: " + KSLoc.toString());
        }

        // Multicast enabled
        TheNC.setUseMulticast(this.TheConfig.getMulticastEnabled());
        LOG.log(Level.FINER, "Multicast enabled: " + Boolean.toString(this.TheConfig.getMulticastEnabled()));

        // Tcp enabled
        TheNC.setTcpEnabled(this.TheConfig.getTcpEnabled());
        LOG.log(Level.FINER, "Multicast enabled: " + Boolean.toString(this.TheConfig.getTcpEnabled()));

        // Peer ID
        PeerID PID = this.TheConfig.getPeerID();

        if (PID!=null) {
            TheNC.setPeerID(PID);
            LOG.log(Level.FINER, "Peer ID: " + PID.toString());
        }


        // Max relay and rdv clients
        if (this.TheConfig.getRelayMaxClients()>=0) {
            TheNC.setRelayMaxClients(this.TheConfig.getRelayMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: " + this.TheConfig.getRelayMaxClients());
        }
        
        if (this.TheConfig.getRendezvousMaxClients()>=0) {
            TheNC.setRendezvousMaxClients(this.TheConfig.getRendezvousMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: " + this.TheConfig.getRelayMaxClients());
        }

        // Use only rdv relay seeds
        TheNC.setUseOnlyRelaySeeds(this.TheConfig.getUseOnlyRelaySeeds());
        LOG.log(Level.FINER, "SetUseOnlyRelaySeeds: " + this.TheConfig.getUseOnlyRelaySeeds());

        TheNC.setUseOnlyRendezvousSeeds(this.TheConfig.getUseOnlyRdvSeeds());
        LOG.log(Level.FINER, "SetUseOnlyRdvSeeds: " + this.TheConfig.getUseOnlyRdvSeeds());

        // HTTP configuration
        JxseHttpTransportConfiguration HttpConfig = this.TheConfig.getHttpTransportConfiguration();

        TheNC.setHttpIncoming(HttpConfig.getHttpIncoming());
        LOG.log(Level.FINER, "Http incoming: " + HttpConfig.getHttpIncoming());

        TheNC.setHttpOutgoing(HttpConfig.getHttpOutgoing());
        LOG.log(Level.FINER, "Http outgoing: " + HttpConfig.getHttpOutgoing());

        TheNC.setHttpInterfaceAddress(HttpConfig.getHttpInterfaceAddress());
        LOG.log(Level.FINER, "Http interface address: " + HttpConfig.getHttpInterfaceAddress());

        TheNC.setHttpPublicAddress(HttpConfig.getHttpPublicAddress(),HttpConfig.getHttpPublicAddressExclusive());
        LOG.log(Level.FINER, "Http public address: " + HttpConfig.getHttpPublicAddress());

        if ( (HttpConfig.getHttpPort()>=0) && (HttpConfig.getHttpPort()<=65535) ) {
            TheNC.setHttpPort(HttpConfig.getHttpPort());
            LOG.log(Level.FINER, "Http port: " + HttpConfig.getHttpPort());
        }

        // Multicast configuration
        JxseMulticastTransportConfiguration MultiConfig = this.TheConfig.getMulticastTransportConfiguration();
        
        String McAdr = MultiConfig.getMulticastAddress();
        
        if (McAdr!=null) {
            TheNC.setMulticastAddress(McAdr);
            LOG.log(Level.FINER, "Multicast address: " + McAdr);
        }
        
        String McInt = MultiConfig.getMulticastInterface();
        
        if (McInt!=null) {
            TheNC.setMulticastInterface(McInt);
            LOG.log(Level.FINER, "Multicast address: " + McInt);
        }

        if (MultiConfig.getMulticastPacketSize()>0) {
            TheNC.setMulticastSize(MultiConfig.getMulticastPacketSize());
            LOG.log(Level.FINER, "Multicast packet size: " + MultiConfig.getMulticastPacketSize());
        }
        
        if ( (MultiConfig.getMulticastPort()>=0) && (MultiConfig.getMulticastPort()<=65535) ) {
            TheNC.setMulticastPort(MultiConfig.getMulticastPort());
            LOG.log(Level.FINER, "Multicast port: " + MultiConfig.getMulticastPort());
        }
        
        // Tcp Configuration
        JxseTcpTransportConfiguration TcpConfig = this.TheConfig.getTcpTransportConfiguration();

        if ( (TcpConfig.getTcpStartPort()>=0) && (TcpConfig.getTcpStartPort()<=65535) ) {
            TheNC.setTcpStartPort(TcpConfig.getTcpStartPort());
            LOG.log(Level.FINER, "Tcp start port: " + TcpConfig.getTcpStartPort());
        }

        if ( (TcpConfig.getTcpEndPort()>=0) && (TcpConfig.getTcpEndPort()<=65535) ) {
            TheNC.setTcpEndPort(TcpConfig.getTcpEndPort());
            LOG.log(Level.FINER, "Tcp end port: " + TcpConfig.getTcpEndPort());
        }

        if ( (TcpConfig.getTcpPort()>=0) && (TcpConfig.getTcpPort()<=65535) ) {
            TheNC.setTcpPort(TcpConfig.getTcpPort());
            LOG.log(Level.FINER, "Tcp port: " + TcpConfig.getTcpPort());
        }

        TheNC.setTcpIncoming(TcpConfig.getTcpIncoming());
        LOG.log(Level.FINER, "Tcp incoming: " + TcpConfig.getTcpIncoming());

        TheNC.setTcpOutgoing(TcpConfig.getTcpOutgoing());
        LOG.log(Level.FINER, "Tcp outgoing: " + TcpConfig.getTcpOutgoing());

        String TcpPubAddr = TcpConfig.getTcpPublicAddress();

        if ( TcpPubAddr!=null ) {
            TheNC.setTcpPublicAddress(TcpPubAddr, TcpConfig.getTcpPublicAddressExclusivity());
            LOG.log(Level.FINER, "Tcp public address: " + TcpPubAddr);
        }

        LOG.log(Level.FINER, "Tcp public address exclusivity: " + TcpConfig.getTcpPublicAddressExclusivity());

        String TcpIntAddr = TcpConfig.getTcpInterfaceAddress();

        if ( TcpIntAddr!=null ) {
            TheNC.setTcpInterfaceAddress(TcpIntAddr);
        }

        LOG.log(Level.FINER, "Tcp interface address: " + TcpIntAddr);

        // Returning result
        return Result;

    }

    /**
     * Making sure any NetworkManager has been stopped
     */
    @Override
    protected void finalize() {

        if (this.TheNM!=null) {
            if (this.TheNM.isStarted()) {
                this.TheNM.stopNetwork();
            }
        }

    }

}
