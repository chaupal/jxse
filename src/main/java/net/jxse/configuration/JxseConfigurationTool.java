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
package net.jxse.configuration;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

/**
 * Provides a set of tool methods to facilitate the import and export of configuration from
 * {@code NetworkManager} instances.
 */
public class JxseConfigurationTool {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseTcpTransportConfiguration.class.getName());

    /**
     * Returns an instance of the {@code NetworkManager} configured with the provided peer
     * configuration. 
     *
     * @return instance of a NetworkManager.
     * @throws Exception if an issue is encountered while retrieving the {@code NetworkManager}.
     */
    public static NetworkManager getConfiguredNetworkManager(JxsePeerConfiguration inConfig) throws JxtaConfigurationException, IOException {

        // Preparing result
        NetworkManager Result = null;

        // Extracting constructor data
        JxsePeerConfiguration.ConnectionMode ExtractedMode = inConfig.getConnectionMode();

        if (ExtractedMode==null) {
            LOG.severe("No connection mode available for NetworkManager !!!");
            throw new JxtaConfigurationException("No connection mode available for NetworkManager !!!");
        }

        LOG.log(Level.FINER, "Connection mode: {0}", ExtractedMode.toString());

        // Peer Instance Name
        String InstanceName = inConfig.getPeerInstanceName();

        if (InstanceName==null) {
            InstanceName="";
        }

        LOG.log(Level.FINER, "Peer instance name: {0}", InstanceName);

        // Persistence location
        URI InstanceHome = inConfig.getPersistenceLocation();

        LOG.log(Level.FINE, "Creating a NetworkManager instance");
        if (InstanceHome!=null) {
            Result = new NetworkManager(convertToNetworkManagerConfigMode(ExtractedMode), InstanceName, InstanceHome);
        } else {
            Result = new NetworkManager(convertToNetworkManagerConfigMode(ExtractedMode), InstanceName);
        }

        // Retrieving the NetworkConfigurator
        NetworkConfigurator TheNC = Result.getConfigurator();

        // Seed relays
        Map<Integer, URI> TheSeedRelays = inConfig.getAllSeedRelays();

        for (URI Item : TheSeedRelays.values()) {
            LOG.log(Level.FINER, "Adding seed relay: {0}", Item.toString());
            TheNC.addSeedRelay(Item);
        }

        // Seed rendezvous
        Map<Integer, URI> TheSeedRDVs = inConfig.getAllSeedRendezvous();

        for (URI Item : TheSeedRDVs.values()) {
            LOG.log(Level.FINER, "Adding seed rendezvous: {0}", Item.toString());
            TheNC.addSeedRendezvous(Item);
        }

        // Seeding relays
        Map<Integer, URI> TheSeedingRelays = inConfig.getAllSeedingRelays();

        for (URI Item : TheSeedingRelays.values()) {
            LOG.log(Level.FINER, "Adding seeding relay: {0}", Item.toString());
            TheNC.addRelaySeedingURI(Item);
        }

        // Seeding rendezvous
        Map<Integer, URI> TheSeedingRDVs = inConfig.getAllSeedingRendezvous();

        for (URI Item : TheSeedingRDVs.values()) {
            LOG.log(Level.FINER, "Adding seeding rendezvous: {0}", Item.toString());
            TheNC.addRdvSeedingURI(Item);
        }

        // Infrastructure ID
        PeerGroupID PGID = inConfig.getInfrastructureID();

        if (PGID!=null) {
            TheNC.setInfrastructureID(PGID);
            LOG.log(Level.FINER, "Peer Group ID: {0}", PGID.toString());
        }

        // Persistence location
        URI KSLoc = inConfig.getKeyStoreLocation();

        if (KSLoc!=null) {
            TheNC.setKeyStoreLocation(KSLoc);
            LOG.log(Level.FINER, "Keystore location: {0}", KSLoc.toString());
        }

        // Multicast enabled
        TheNC.setUseMulticast(inConfig.getMulticastEnabled());
        LOG.log(Level.FINER, "Multicast enabled: {0}", Boolean.toString(inConfig.getMulticastEnabled()));

        // Tcp enabled
        TheNC.setTcpEnabled(inConfig.getTcpEnabled());
        LOG.log(Level.FINER, "Multicast enabled: {0}", Boolean.toString(inConfig.getTcpEnabled()));

        // Peer ID
        PeerID PID = inConfig.getPeerID();

        if (PID!=null) {
            TheNC.setPeerID(PID);
            LOG.log(Level.FINER, "Peer ID: {0}", PID.toString());
        }

        // Max relay and rdv clients
        if (inConfig.getRelayMaxClients()>=0) {
            TheNC.setRelayMaxClients(inConfig.getRelayMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: {0}", inConfig.getRelayMaxClients());
        }

        if (inConfig.getRendezvousMaxClients()>=0) {
            TheNC.setRendezvousMaxClients(inConfig.getRendezvousMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: {0}", inConfig.getRelayMaxClients());
        }

        // Use only rdv relay seeds
        TheNC.setUseOnlyRelaySeeds(inConfig.getUseOnlyRelaySeeds());
        LOG.log(Level.FINER, "SetUseOnlyRelaySeeds: {0}", inConfig.getUseOnlyRelaySeeds());

        TheNC.setUseOnlyRendezvousSeeds(inConfig.getUseOnlyRdvSeeds());
        LOG.log(Level.FINER, "SetUseOnlyRdvSeeds: {0}", inConfig.getUseOnlyRdvSeeds());

        // HTTP configuration
        JxseHttpTransportConfiguration HttpConfig = inConfig.getHttpTransportConfiguration();

        TheNC.setHttpIncoming(HttpConfig.getHttpIncoming());
        LOG.log(Level.FINER, "Http incoming: {0}", HttpConfig.getHttpIncoming());

        TheNC.setHttpOutgoing(HttpConfig.getHttpOutgoing());
        LOG.log(Level.FINER, "Http outgoing: {0}", HttpConfig.getHttpOutgoing());

        TheNC.setHttpInterfaceAddress(HttpConfig.getHttpInterfaceAddress());
        LOG.log(Level.FINER, "Http interface address: {0}", HttpConfig.getHttpInterfaceAddress());

        TheNC.setHttpPublicAddress(HttpConfig.getHttpPublicAddress(),HttpConfig.isHttpPublicAddressExclusive());
        LOG.log(Level.FINER, "Http public address: {0}", HttpConfig.getHttpPublicAddress());

        if ( (HttpConfig.getHttpPort()>=0) && (HttpConfig.getHttpPort()<=65535) ) {
            TheNC.setHttpPort(HttpConfig.getHttpPort());
            LOG.log(Level.FINER, "Http port: {0}", HttpConfig.getHttpPort());
        }

        // HTTP2 configuration
        JxseHttp2TransportConfiguration Http2Config = inConfig.getHttp2TransportConfiguration();

        TheNC.setHttp2Incoming(Http2Config.getHttp2Incoming());
        LOG.log(Level.FINER, "Http2 incoming: {0}", Http2Config.getHttp2Incoming());

        TheNC.setHttp2Outgoing(Http2Config.getHttp2Outgoing());
        LOG.log(Level.FINER, "Http2 outgoing: {0}", Http2Config.getHttp2Outgoing());

        TheNC.setHttp2InterfaceAddress(Http2Config.getHttp2InterfaceAddress());
        LOG.log(Level.FINER, "Http2 interface address: {0}", Http2Config.getHttp2InterfaceAddress());

        TheNC.setHttp2PublicAddress(Http2Config.getHttp2PublicAddress(), Http2Config.isHttp2PublicAddressExclusive());
        LOG.log(Level.FINER, "Http2 public address: {0}", Http2Config.getHttp2PublicAddress());
        LOG.log(Level.FINER, "Http2 exclusive public address: {0}", Http2Config.isHttp2PublicAddressExclusive());

        if ( (Http2Config.getHttp2Port()>=0) && (Http2Config.getHttp2Port()<=65535) ) {
            TheNC.setHttp2Port(Http2Config.getHttp2Port());
            LOG.log(Level.FINER, "Http2 port: {0}", Http2Config.getHttp2Port());
        }

        if ( (Http2Config.getHttp2StartPort()>=0) && (Http2Config.getHttp2StartPort()<=65535) ) {
            TheNC.setHttp2StartPort(Http2Config.getHttp2StartPort());
            LOG.log(Level.FINER, "Http2 start port: {0}", Http2Config.getHttp2StartPort());
        }

        if ( (Http2Config.getHttp2EndPort()>=0) && (Http2Config.getHttp2EndPort()<=65535) ) {
            TheNC.setHttp2EndPort(Http2Config.getHttp2EndPort());
            LOG.log(Level.FINER, "Http2 end port: {0}", Http2Config.getHttp2EndPort());
        }

        // Multicast configuration
        JxseMulticastTransportConfiguration MultiConfig = inConfig.getMulticastTransportConfiguration();

        String McAdr = MultiConfig.getMulticastAddress();

        if (McAdr!=null) {
            TheNC.setMulticastAddress(McAdr);
            LOG.log(Level.FINER, "Multicast address: {0}", McAdr);
        }

        String McInt = MultiConfig.getMulticastInterface();

        if (McInt!=null) {
            TheNC.setMulticastInterface(McInt);
            LOG.log(Level.FINER, "Multicast address: {0}", McInt);
        }

        if (MultiConfig.getMulticastPacketSize()>0) {
            TheNC.setMulticastSize(MultiConfig.getMulticastPacketSize());
            LOG.log(Level.FINER, "Multicast packet size: {0}", MultiConfig.getMulticastPacketSize());
        }

        if ( (MultiConfig.getMulticastPort()>=0) && (MultiConfig.getMulticastPort()<=65535) ) {
            TheNC.setMulticastPort(MultiConfig.getMulticastPort());
            LOG.log(Level.FINER, "Multicast port: {0}", MultiConfig.getMulticastPort());
        }

        // Tcp Configuration
        JxseTcpTransportConfiguration TcpConfig = inConfig.getTcpTransportConfiguration();

        if ( (TcpConfig.getTcpStartPort()>=0) && (TcpConfig.getTcpStartPort()<=65535) ) {
            TheNC.setTcpStartPort(TcpConfig.getTcpStartPort());
            LOG.log(Level.FINER, "Tcp start port: {0}", TcpConfig.getTcpStartPort());
        }

        if ( (TcpConfig.getTcpEndPort()>=0) && (TcpConfig.getTcpEndPort()<=65535) ) {
            TheNC.setTcpEndPort(TcpConfig.getTcpEndPort());
            LOG.log(Level.FINER, "Tcp end port: {0}", TcpConfig.getTcpEndPort());
        }

        if ( (TcpConfig.getTcpPort()>=0) && (TcpConfig.getTcpPort()<=65535) ) {
            TheNC.setTcpPort(TcpConfig.getTcpPort());
            LOG.log(Level.FINER, "Tcp port: {0}", TcpConfig.getTcpPort());
        }

        TheNC.setTcpIncoming(TcpConfig.getTcpIncoming());
        LOG.log(Level.FINER, "Tcp incoming: {0}", TcpConfig.getTcpIncoming());

        TheNC.setTcpOutgoing(TcpConfig.getTcpOutgoing());
        LOG.log(Level.FINER, "Tcp outgoing: {0}", TcpConfig.getTcpOutgoing());

        String TcpPubAddr = TcpConfig.getTcpPublicAddress();

        if ( TcpPubAddr!=null ) {
            TheNC.setTcpPublicAddress(TcpPubAddr, TcpConfig.isTcpPublicAddressExclusive());
            LOG.log(Level.FINER, "Tcp public address: {0}", TcpPubAddr);
        }

        LOG.log(Level.FINER, "Tcp public address exclusivity: {0}", TcpConfig.isTcpPublicAddressExclusive());

        String TcpIntAddr = TcpConfig.getTcpInterfaceAddress();

        if ( TcpIntAddr!=null ) {
            TheNC.setTcpInterfaceAddress(TcpIntAddr);
        }

        LOG.log(Level.FINER, "Tcp interface address: {0}", TcpIntAddr);

        // Returning result
        return Result;

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
            return NetworkManager.ConfigMode.SUPER;
        }

        return null;

    }

    /**
     * Converts a {@code NetworkManager.ConfigMode} into a Jxse peer configuration
     * connection mode.
     *
     * @param mode a NetworkManager connection mode
     * @return a NetworkManager config mode
     */
    public static JxsePeerConfiguration.ConnectionMode convertToJxsePeerConfigurationConfigMode(NetworkManager.ConfigMode mode) {

        if (mode.compareTo(NetworkManager.ConfigMode.ADHOC)==0) {
            return JxsePeerConfiguration.ConnectionMode.ADHOC;
        } else if (mode.compareTo(NetworkManager.ConfigMode.EDGE)==0) {
            return JxsePeerConfiguration.ConnectionMode.EDGE;
        } else if (mode.compareTo(NetworkManager.ConfigMode.RELAY)==0) {
            return JxsePeerConfiguration.ConnectionMode.RELAY;
        } else if (mode.compareTo(NetworkManager.ConfigMode.RENDEZVOUS)==0) {
            return JxsePeerConfiguration.ConnectionMode.RENDEZVOUS;
        } else if (mode.compareTo(NetworkManager.ConfigMode.SUPER)==0) {
            return JxsePeerConfiguration.ConnectionMode.SUPER;
        }

        return null;

    }

    /**
     * Returns a new {@code JxsePeerConfiguration} containing the configuration of the provided {@code NetworkManager}.
     *
     * @return instance of a Jxse peer configuration.
     */
    public static JxsePeerConfiguration getJxsePeerConfigurationFromNetworkManager(NetworkManager inNM) throws Exception {

        // Preparing result
        JxsePeerConfiguration Result = new JxsePeerConfiguration();

        // Retrieving the NetworkConfigurator
        NetworkConfigurator TheNC = inNM.getConfigurator();

        // Extracting mode
        Result.setConnectionMode(JxseConfigurationTool.convertToJxsePeerConfigurationConfigMode(inNM.getMode()));

        // Peer Instance Name
        Result.setPeerInstanceName(TheNC.getName());

        // Persistence location
        Result.setPersistenceLocation(TheNC.getStoreHome());

        // Seed relays
        URI[] TheURIs = TheNC.getRelaySeedURIs();

        for (int i=0;i<TheURIs.length;i++) {
            Result.addSeedRelay(TheURIs[i], i);
        }

       // Seed rendezvous
        URI[] TheURI2s = TheNC.getRdvSeedURIs();

        for (int i=0;i<TheURI2s.length;i++) {
            Result.addSeedRendezvous(TheURI2s[i], i);
        }

        // Seeding relays
        URI[] TheURI3s = TheNC.getRelaySeedingURIs();

        for (int i=0;i<TheURI3s.length;i++) {
            Result.addSeedingRelay(TheURI3s[i], i);
        }

        // Seeding rendezvous
        URI[] TheURI4s = TheNC.getRdvSeedingURIs();

        for (int i=0;i<TheURI4s.length;i++) {
            Result.addSeedingRendezvous(TheURI4s[i], i);
        }

        // Infrastructure ID
        Result.setInfrastructureID(inNM.getInfrastructureID());

        // Persistence location
        Result.setKeyStoreLocation(TheNC.getKeyStoreLocation());

        // Multicast enabled
        Result.setMulticastEnabled(TheNC.getMulticastStatus());

        // Tcp enabled
        Result.setTcpEnabled(TheNC.isTcpEnabled());

        // Peer ID
        Result.setPeerID(inNM.getPeerID());

        // Max relay and rdv clients
        Result.setRelayMaxClients(TheNC.getRelayMaxClients());
        Result.setRendezvousMaxClients(TheNC.getRendezvousMaxClients());

        // Use only rdv relay seeds
        Result.setUseOnlyRdvSeeds(TheNC.getUseOnlyRendezvousSeedsStatus());
        Result.setUseOnlyRelaySeeds(TheNC.getUseOnlyRelaySeedsStatus());

        // HTTP configuration
        JxseHttpTransportConfiguration HttpConfig = Result.getHttpTransportConfiguration();

        HttpConfig.setHttpIncoming(TheNC.getHttpIncomingStatus());
        HttpConfig.setHttpOutgoing(TheNC.getHttpOutgoingStatus());
        HttpConfig.setHttpInterfaceAddress(TheNC.getHttpInterfaceAddress());
        HttpConfig.setHttpPublicAddress(TheNC.getHttpPublicAddress(), TheNC.isHttpPublicAddressExclusive());
        HttpConfig.setHttpPort(TheNC.getHttpPort());

        Result.setHttpTransportConfiguration(HttpConfig);

        // HTTP configuration
        JxseHttp2TransportConfiguration Http2Config = Result.getHttp2TransportConfiguration();

        Http2Config.setHttp2Incoming(TheNC.getHttp2IncomingStatus());
        Http2Config.setHttp2Outgoing(TheNC.getHttp2OutgoingStatus());
        Http2Config.setHttp2InterfaceAddress(TheNC.getHttp2InterfaceAddress());
        Http2Config.setHttp2PublicAddress(TheNC.getHttp2PublicAddress(), TheNC.isHttp2PublicAddressExclusive());
        Http2Config.setHttp2Port(TheNC.getHttp2Port());

        Http2Config.setHttp2StartPort(TheNC.getHttp2StartPort());
        Http2Config.setHttp2EndPort(TheNC.getHttp2EndPort());

        Result.setHttp2TransportConfiguration(Http2Config);
        
        // Multicast configuration
        JxseMulticastTransportConfiguration MultiConfig = Result.getMulticastTransportConfiguration();

        MultiConfig.setMulticastAddress(TheNC.getMulticastAddress());
        MultiConfig.setMulticastInterface(TheNC.getMulticastInterface());
        MultiConfig.setMulticastPacketSize(TheNC.getMulticastSize());
        MultiConfig.setMulticastPort(TheNC.getMulticastPort());

        Result.setMulticastTransportConfiguration(MultiConfig);

        // Tcp Configuration
        JxseTcpTransportConfiguration TcpConfig = Result.getTcpTransportConfiguration();

        TcpConfig.setTcpStartPort(TheNC.getTcpStartPort());
        TcpConfig.setTcpEndPort(TheNC.getTcpEndport());
        TcpConfig.setTcpPort(TheNC.getTcpPort());

        TcpConfig.setTcpIncoming(TheNC.getTcpIncomingStatus());
        TcpConfig.setTcpOutgoing(TheNC.getTcpOutgoingStatus());

        TcpConfig.setTcpPublicAddress(TheNC.getTcpPublicAddress(), TheNC.isTcpPublicAddressExclusive());
        TcpConfig.setTcpInterfaceAddress(TheNC.getTcpInterfaceAddress());

        Result.setTcpTransportConfiguration(TcpConfig);

        // Returning result
        return Result;

    }

}