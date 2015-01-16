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
import net.jxta.exception.JxtaException;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.JxtaApplication;
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
     * @param jxsePeerConfiguration
     * @return instance of a NetworkManager.
     * @throws net.jxta.configuration.JxtaConfigurationException
     * @throws java.io.IOException
     * @throws net.jxta.exception.JxtaException
     */
    public static NetworkManager getConfiguredNetworkManager(JxsePeerConfiguration jxsePeerConfiguration) throws JxtaConfigurationException, IOException, JxtaException {

        // Preparing result
        NetworkManager result = null;

        // Extracting constructor data
        JxsePeerConfiguration.ConnectionMode extractedMode = jxsePeerConfiguration.getConnectionMode();

        if (extractedMode == null) {
            LOG.severe("No connection mode available for NetworkManager!!!");
            throw new JxtaConfigurationException("No connection mode available for NetworkManager !!!");
        }

        LOG.log(Level.FINER, "Connection mode: {0}", extractedMode.toString());

        // Peer Instance Name
        String instanceName = jxsePeerConfiguration.getPeerInstanceName();

        if (instanceName == null) {
            instanceName = "";
        }

        LOG.log(Level.FINER, "Peer instance name: {0}", instanceName);

        // Persistence location
        URI instanceHome = jxsePeerConfiguration.getPersistenceLocation();

        LOG.log(Level.FINE, "Creating a NetworkManager instance");
        
        /*if (instanceHome!=null) {
            result = new NetworkManager(convertToNetworkManagerConfigMode(extractedMode), instanceName, instanceHome);            
        } else {            
            result = new NetworkManager(convertToNetworkManagerConfigMode(extractedMode), instanceName);
        }*/
        
        result = JxtaApplication.getNetworkManager(convertToNetworkManagerConfigMode(extractedMode), instanceName, instanceHome);

        // Retrieving the NetworkConfigurator
        NetworkConfigurator networkConfigurator = result.getConfigurator();

        // Seed relays
        Map<Integer, URI> seedRelays = jxsePeerConfiguration.getAllSeedRelays();

        for (URI uri : seedRelays.values()) {
            LOG.log(Level.FINER, "Adding seed relay: {0}", uri.toString());
            networkConfigurator.addSeedRelay(uri);
        }

        // Seed rendezvous
        Map<Integer, URI> seedRendezvouss = jxsePeerConfiguration.getAllSeedRendezvous();

        for (URI uri : seedRendezvouss.values()) {
            LOG.log(Level.FINER, "Adding seed rendezvous: {0}", uri.toString());
            networkConfigurator.addSeedRendezvous(uri);
        }

        // Seeding relays
        Map<Integer, URI> seedingRelays = jxsePeerConfiguration.getAllSeedingRelays();

        for (URI uri : seedingRelays.values()) {
            LOG.log(Level.FINER, "Adding seeding relay: {0}", uri.toString());
            networkConfigurator.addRelaySeedingURI(uri);
        }

        // Seeding rendezvous
        Map<Integer, URI> seedingRendezvouss = jxsePeerConfiguration.getAllSeedingRendezvous();

        for (URI uri : seedingRendezvouss.values()) {
            LOG.log(Level.FINER, "Adding seeding rendezvous: {0}", uri.toString());
            networkConfigurator.addRdvSeedingURI(uri);
        }

        // Infrastructure ID
        PeerGroupID peerGroupId = jxsePeerConfiguration.getInfrastructureID();

        if (peerGroupId != null) {
            networkConfigurator.setInfrastructureID(peerGroupId);
            LOG.log(Level.FINER, "Peer Group ID: {0}", peerGroupId.toString());
        }

        // Persistence location
        URI keyStoreLocation = jxsePeerConfiguration.getKeyStoreLocation();

        if (keyStoreLocation != null) {
            networkConfigurator.setKeyStoreLocation(keyStoreLocation);
            LOG.log(Level.FINER, "Keystore location: {0}", keyStoreLocation.toString());
        }

        // Multicast enabled
        networkConfigurator.setUseMulticast(jxsePeerConfiguration.getMulticastEnabled());
        LOG.log(Level.FINER, "Multicast enabled: {0}", Boolean.toString(jxsePeerConfiguration.getMulticastEnabled()));

        // Tcp enabled
        networkConfigurator.setTcpEnabled(jxsePeerConfiguration.getTcpEnabled());
        LOG.log(Level.FINER, "Multicast enabled: {0}", Boolean.toString(jxsePeerConfiguration.getTcpEnabled()));

        // Peer ID
        PeerID peerId = jxsePeerConfiguration.getPeerID();

        if (peerId != null) {
            networkConfigurator.setPeerID(peerId);
            LOG.log(Level.FINER, "Peer ID: {0}", peerId.toString());
        }

        // Max relay and rdv clients
        if (jxsePeerConfiguration.getRelayMaxClients()>=0) {
            networkConfigurator.setRelayMaxClients(jxsePeerConfiguration.getRelayMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: {0}", jxsePeerConfiguration.getRelayMaxClients());
        }

        if (jxsePeerConfiguration.getRendezvousMaxClients()>=0) {
            networkConfigurator.setRendezvousMaxClients(jxsePeerConfiguration.getRendezvousMaxClients());
            LOG.log(Level.FINER, "Relay Max Client: {0}", jxsePeerConfiguration.getRelayMaxClients());
        }

        // Use only rdv relay seeds
        networkConfigurator.setUseOnlyRelaySeeds(jxsePeerConfiguration.getUseOnlyRelaySeeds());
        LOG.log(Level.FINER, "SetUseOnlyRelaySeeds: {0}", jxsePeerConfiguration.getUseOnlyRelaySeeds());

        networkConfigurator.setUseOnlyRendezvousSeeds(jxsePeerConfiguration.getUseOnlyRdvSeeds());
        LOG.log(Level.FINER, "SetUseOnlyRdvSeeds: {0}", jxsePeerConfiguration.getUseOnlyRdvSeeds());

        // HTTP configuration
        JxseHttpTransportConfiguration httpConfiguration = jxsePeerConfiguration.getHttpTransportConfiguration();

        networkConfigurator.setHttpIncoming(httpConfiguration.getHttpIncoming());
        LOG.log(Level.FINER, "Http incoming: {0}", httpConfiguration.getHttpIncoming());

        networkConfigurator.setHttpOutgoing(httpConfiguration.getHttpOutgoing());
        LOG.log(Level.FINER, "Http outgoing: {0}", httpConfiguration.getHttpOutgoing());

        networkConfigurator.setHttpInterfaceAddress(httpConfiguration.getHttpInterfaceAddress());
        LOG.log(Level.FINER, "Http interface address: {0}", httpConfiguration.getHttpInterfaceAddress());

        networkConfigurator.setHttpPublicAddress(httpConfiguration.getHttpPublicAddress(),httpConfiguration.isHttpPublicAddressExclusive());
        LOG.log(Level.FINER, "Http public address: {0}", httpConfiguration.getHttpPublicAddress());

        if ( (httpConfiguration.getHttpPort()>=0) && (httpConfiguration.getHttpPort()<=65535) ) {
            networkConfigurator.setHttpPort(httpConfiguration.getHttpPort());
            LOG.log(Level.FINER, "Http port: {0}", httpConfiguration.getHttpPort());
        }

        // HTTP2 configuration
        JxseHttp2TransportConfiguration http2Configuration = jxsePeerConfiguration.getHttp2TransportConfiguration();

        networkConfigurator.setHttp2Incoming(http2Configuration.getHttp2Incoming());
        LOG.log(Level.FINER, "Http2 incoming: {0}", http2Configuration.getHttp2Incoming());

        networkConfigurator.setHttp2Outgoing(http2Configuration.getHttp2Outgoing());
        LOG.log(Level.FINER, "Http2 outgoing: {0}", http2Configuration.getHttp2Outgoing());

        networkConfigurator.setHttp2InterfaceAddress(http2Configuration.getHttp2InterfaceAddress());
        LOG.log(Level.FINER, "Http2 interface address: {0}", http2Configuration.getHttp2InterfaceAddress());

        networkConfigurator.setHttp2PublicAddress(http2Configuration.getHttp2PublicAddress(), http2Configuration.isHttp2PublicAddressExclusive());
        LOG.log(Level.FINER, "Http2 public address: {0}", http2Configuration.getHttp2PublicAddress());
        LOG.log(Level.FINER, "Http2 exclusive public address: {0}", http2Configuration.isHttp2PublicAddressExclusive());

        if ( (http2Configuration.getHttp2Port()>=0) && (http2Configuration.getHttp2Port()<=65535) ) {
            networkConfigurator.setHttp2Port(http2Configuration.getHttp2Port());
            LOG.log(Level.FINER, "Http2 port: {0}", http2Configuration.getHttp2Port());
        }

        if ( (http2Configuration.getHttp2StartPort()>=0) && (http2Configuration.getHttp2StartPort()<=65535) ) {
            networkConfigurator.setHttp2StartPort(http2Configuration.getHttp2StartPort());
            LOG.log(Level.FINER, "Http2 start port: {0}", http2Configuration.getHttp2StartPort());
        }

        if ( (http2Configuration.getHttp2EndPort()>=0) && (http2Configuration.getHttp2EndPort()<=65535) ) {
            networkConfigurator.setHttp2EndPort(http2Configuration.getHttp2EndPort());
            LOG.log(Level.FINER, "Http2 end port: {0}", http2Configuration.getHttp2EndPort());
        }

        // Multicast configuration
        JxseMulticastTransportConfiguration multicastConfiguration = jxsePeerConfiguration.getMulticastTransportConfiguration();

        String multicastAddress = multicastConfiguration.getMulticastAddress();

        if (multicastAddress != null) {
            networkConfigurator.setMulticastAddress(multicastAddress);
            LOG.log(Level.FINER, "Multicast address: {0}", multicastAddress);
        }

        String multicastInterface = multicastConfiguration.getMulticastInterface();

        if (multicastInterface != null) {
            networkConfigurator.setMulticastInterface(multicastInterface);
            LOG.log(Level.FINER, "Multicast address: {0}", multicastInterface);
        }

        if (multicastConfiguration.getMulticastPacketSize()>0) {
            networkConfigurator.setMulticastSize(multicastConfiguration.getMulticastPacketSize());
            LOG.log(Level.FINER, "Multicast packet size: {0}", multicastConfiguration.getMulticastPacketSize());
        }

        if ((multicastConfiguration.getMulticastPort()>=0) && (multicastConfiguration.getMulticastPort()<=65535)) {
            networkConfigurator.setMulticastPort(multicastConfiguration.getMulticastPort());
            LOG.log(Level.FINER, "Multicast port: {0}", multicastConfiguration.getMulticastPort());
        }

        // Tcp Configuration
        JxseTcpTransportConfiguration tcpConfiguration = jxsePeerConfiguration.getTcpTransportConfiguration();

        if ( (tcpConfiguration.getTcpStartPort()>=0) && (tcpConfiguration.getTcpStartPort()<=65535) ) {
            networkConfigurator.setTcpStartPort(tcpConfiguration.getTcpStartPort());
            LOG.log(Level.FINER, "Tcp start port: {0}", tcpConfiguration.getTcpStartPort());
        }

        if ((tcpConfiguration.getTcpEndPort()>=0) && (tcpConfiguration.getTcpEndPort()<=65535)) {
            networkConfigurator.setTcpEndPort(tcpConfiguration.getTcpEndPort());
            LOG.log(Level.FINER, "Tcp end port: {0}", tcpConfiguration.getTcpEndPort());
        }

        if ((tcpConfiguration.getTcpPort()>=0) && (tcpConfiguration.getTcpPort()<=65535)) {
            networkConfigurator.setTcpPort(tcpConfiguration.getTcpPort());
            LOG.log(Level.FINER, "Tcp port: {0}", tcpConfiguration.getTcpPort());
        }

        networkConfigurator.setTcpIncoming(tcpConfiguration.getTcpIncoming());
        LOG.log(Level.FINER, "Tcp incoming: {0}", tcpConfiguration.getTcpIncoming());

        networkConfigurator.setTcpOutgoing(tcpConfiguration.getTcpOutgoing());
        LOG.log(Level.FINER, "Tcp outgoing: {0}", tcpConfiguration.getTcpOutgoing());

        String tcpPublicAddress = tcpConfiguration.getTcpPublicAddress();

        if (tcpPublicAddress != null) {
            networkConfigurator.setTcpPublicAddress(tcpPublicAddress, tcpConfiguration.isTcpPublicAddressExclusive());
            LOG.log(Level.FINER, "Tcp public address: {0}", tcpPublicAddress);
        }

        LOG.log(Level.FINER, "Tcp public address exclusivity: {0}", tcpConfiguration.isTcpPublicAddressExclusive());

        String tcpInterfaceAddress = tcpConfiguration.getTcpInterfaceAddress();

        if (tcpInterfaceAddress != null) {
            networkConfigurator.setTcpInterfaceAddress(tcpInterfaceAddress);
        }

        LOG.log(Level.FINER, "Tcp interface address: {0}", tcpInterfaceAddress);

        // Returning result
        return result;
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
        JxsePeerConfiguration result = new JxsePeerConfiguration();

        // Retrieving the NetworkConfigurator
        NetworkConfigurator networkConfigurator = inNM.getConfigurator();

        // Extracting mode
        result.setConnectionMode(JxseConfigurationTool.convertToJxsePeerConfigurationConfigMode(inNM.getMode()));

        // Peer Instance Name
        result.setPeerInstanceName(networkConfigurator.getName());

        // Persistence location
        result.setPersistenceLocation(networkConfigurator.getStoreHome());

        // Seed relays
        URI[] TheURIs = networkConfigurator.getRelaySeedURIs();

        for (int i=0;i<TheURIs.length;i++) {
            result.addSeedRelay(TheURIs[i], i);
        }

       // Seed rendezvous
        URI[] TheURI2s = networkConfigurator.getRdvSeedURIs();

        for (int i=0;i<TheURI2s.length;i++) {
            result.addSeedRendezvous(TheURI2s[i], i);
        }

        // Seeding relays
        URI[] TheURI3s = networkConfigurator.getRelaySeedingURIs();

        for (int i=0;i<TheURI3s.length;i++) {
            result.addSeedingRelay(TheURI3s[i], i);
        }

        // Seeding rendezvous
        URI[] TheURI4s = networkConfigurator.getRdvSeedingURIs();

        for (int i=0;i<TheURI4s.length;i++) {
            result.addSeedingRendezvous(TheURI4s[i], i);
        }

        // Infrastructure ID
        result.setInfrastructureID(inNM.getInfrastructureID());

        // Persistence location
        result.setKeyStoreLocation(networkConfigurator.getKeyStoreLocation());

        // Multicast enabled
        result.setMulticastEnabled(networkConfigurator.getMulticastStatus());

        // Tcp enabled
        result.setTcpEnabled(networkConfigurator.isTcpEnabled());

        // Peer ID
        result.setPeerID(inNM.getPeerID());

        // Max relay and rdv clients
        result.setRelayMaxClients(networkConfigurator.getRelayMaxClients());
        result.setRendezvousMaxClients(networkConfigurator.getRendezvousMaxClients());

        // Use only rdv relay seeds
        result.setUseOnlyRdvSeeds(networkConfigurator.getUseOnlyRendezvousSeedsStatus());
        result.setUseOnlyRelaySeeds(networkConfigurator.getUseOnlyRelaySeedsStatus());

        // HTTP configuration
        JxseHttpTransportConfiguration httpConfiguration = result.getHttpTransportConfiguration();

        httpConfiguration.setHttpIncoming(networkConfigurator.getHttpIncomingStatus());
        httpConfiguration.setHttpOutgoing(networkConfigurator.getHttpOutgoingStatus());
        httpConfiguration.setHttpInterfaceAddress(networkConfigurator.getHttpInterfaceAddress());
        httpConfiguration.setHttpPublicAddress(networkConfigurator.getHttpPublicAddress(), networkConfigurator.isHttpPublicAddressExclusive());
        httpConfiguration.setHttpPort(networkConfigurator.getHttpPort());

        result.setHttpTransportConfiguration(httpConfiguration);

        // HTTP configuration
        JxseHttp2TransportConfiguration http2Configuration = result.getHttp2TransportConfiguration();

        http2Configuration.setHttp2Incoming(networkConfigurator.getHttp2IncomingStatus());
        http2Configuration.setHttp2Outgoing(networkConfigurator.getHttp2OutgoingStatus());
        http2Configuration.setHttp2InterfaceAddress(networkConfigurator.getHttp2InterfaceAddress());
        http2Configuration.setHttp2PublicAddress(networkConfigurator.getHttp2PublicAddress(), networkConfigurator.isHttp2PublicAddressExclusive());
        http2Configuration.setHttp2Port(networkConfigurator.getHttp2Port());

        http2Configuration.setHttp2StartPort(networkConfigurator.getHttp2StartPort());
        http2Configuration.setHttp2EndPort(networkConfigurator.getHttp2EndPort());

        result.setHttp2TransportConfiguration(http2Configuration);
        
        // Multicast configuration
        JxseMulticastTransportConfiguration multicastConfiguration = result.getMulticastTransportConfiguration();

        multicastConfiguration.setMulticastAddress(networkConfigurator.getMulticastAddress());
        multicastConfiguration.setMulticastInterface(networkConfigurator.getMulticastInterface());
        multicastConfiguration.setMulticastPacketSize(networkConfigurator.getMulticastSize());
        multicastConfiguration.setMulticastPort(networkConfigurator.getMulticastPort());

        result.setMulticastTransportConfiguration(multicastConfiguration);

        // Tcp Configuration
        JxseTcpTransportConfiguration tcpConfiguration = result.getTcpTransportConfiguration();

        tcpConfiguration.setTcpStartPort(networkConfigurator.getTcpStartPort());
        tcpConfiguration.setTcpEndPort(networkConfigurator.getTcpEndport());
        tcpConfiguration.setTcpPort(networkConfigurator.getTcpPort());

        tcpConfiguration.setTcpIncoming(networkConfigurator.getTcpIncomingStatus());
        tcpConfiguration.setTcpOutgoing(networkConfigurator.getTcpOutgoingStatus());

        tcpConfiguration.setTcpPublicAddress(networkConfigurator.getTcpPublicAddress(), networkConfigurator.isTcpPublicAddressExclusive());
        tcpConfiguration.setTcpInterfaceAddress(networkConfigurator.getTcpInterfaceAddress());

        result.setTcpTransportConfiguration(tcpConfiguration);

        // Returning result
        return result;
    }
}