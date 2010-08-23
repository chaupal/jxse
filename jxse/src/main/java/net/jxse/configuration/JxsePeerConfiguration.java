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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.jxta.configuration.JxtaPeerConfiguration;
import net.jxta.configuration.PropertiesUtil;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

/**
 *
 */
public class JxsePeerConfiguration extends JxtaPeerConfiguration {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxsePeerConfiguration.class.getName());

    /**
     * Types of connections mode to the JXTA network provided by the JXSE implementation.
     */
    public enum ConnectionMode {

        /**
         * AD-HOC node
         */
        ADHOC,
        /**
         * Edge node
         */
        EDGE,
        /**
         * Rendezvous node
         */
        RENDEZVOUS,
        /**
         * Relay node
         */
        RELAY,
        /**
         * Rendezvous and Relay node -
         */
        SUPER

    }

    /**
     * Return an HTTP transport configuration containing defaults
     *
     * @return a JXSE peer configuration
     */
    public static final JxsePeerConfiguration getDefaultJxsePeerConfiguration() {

        // Preparing return value
        JxsePeerConfiguration Result = new  JxsePeerConfiguration();

        Result.HttpConfig = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();
        Result.TcpConfig =  JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();
        Result.MulticastConfig =  JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();

        // Enabling TCP and Multicasting
        Result.setDefaultPropertyValue(JXSE_TCP_ENABLED, Boolean.toString(true));
        Result.setDefaultPropertyValue(JXSE_MULTICAST_ENABLED, Boolean.toString(true));

        // Use seeds only
        Result.setDefaultPropertyValue(JXSE_USE_ONLY_RELAY_SEED, Boolean.toString(false));
        Result.setDefaultPropertyValue(JXSE_USE_ONLY_RDV_SEED, Boolean.toString(false));

        return Result;

    }

    /**
     * This constructor copies all entries from the provided parameter into this object, including
     * those of transports.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxsePeerConfiguration(JxsePeerConfiguration toCopy) {

        // Copying values
        super(toCopy);

        // Initializing transports
        this.HttpConfig = new JxseHttpTransportConfiguration(toCopy.getHttpTransportConfiguration());
        this.MulticastConfig = new JxseMulticastTransportConfiguration(toCopy.getMulticastTransportConfiguration());
        this.TcpConfig = new JxseTcpTransportConfiguration(toCopy.getTcpTransportConfiguration());

    }

    /**
     * Simple constructor, initialized with empty transport configurations.
     */
    public JxsePeerConfiguration() {

        // Calling super
        super();

        // Initializing transports
        this.HttpConfig = new JxseHttpTransportConfiguration();
        this.MulticastConfig = new JxseMulticastTransportConfiguration();
        this.TcpConfig = new JxseTcpTransportConfiguration();

    }

    /**
     * Reference to the HTTP configuration
     */
    private JxseHttpTransportConfiguration HttpConfig = null;
    
    /**
     * Sets the HTTP transport configuration by copying the parameter. If the parameter  
     * is {@code null}, an empty HTTP transport configuration is set.
     *
     * @param inConfig The HTTP transport configuration
     */
    public void setHttpTransportConfiguration(JxseHttpTransportConfiguration inConfig) {

        if (inConfig!=null) {
            HttpConfig = new JxseHttpTransportConfiguration(inConfig);
        } else {
            HttpConfig = new JxseHttpTransportConfiguration();
        }

    }

    /**
     * Returns a copy of the HTTP transport configuration.
     *
     * @return a copy of the HTTP transport configuration
     */
    public JxseHttpTransportConfiguration getHttpTransportConfiguration() {

        return new JxseHttpTransportConfiguration(HttpConfig);

    }

    /**
     * Reference to the Multicasting configuration
     */
    private JxseMulticastTransportConfiguration MulticastConfig = null;

    /**
     * Sets the multicasting transport configuration by copying the parameter. If the parameter
     * is {@code null}, an empty multicasting transport configuration is set.
     *
     * @param inConfig The Multicasting transport configuration
     */
    public void setMulticastTransportConfiguration(JxseMulticastTransportConfiguration inConfig) {

        if (inConfig!=null) {
            MulticastConfig = new JxseMulticastTransportConfiguration(inConfig);
        } else {
            MulticastConfig = new JxseMulticastTransportConfiguration();
        }

    }

    /**
     * Returns a copy of the Multicasting transport configuration or {@code null} if it is not available.
     *
     * @return a copy of the Multicasting transport configuration or {@code null}
     */
    public JxseMulticastTransportConfiguration getMulticastTransportConfiguration() {

        return new JxseMulticastTransportConfiguration(MulticastConfig);

    }


    /**
     * Reference to the TCP configuration
     */
    private JxseTcpTransportConfiguration TcpConfig = null;

    /**
     * Sets the TCP transport configuration by copying the parameter. If the parameter
     * is {@code null}, an empty TCP transport configuration is set.
     *
     * @param inConfig The TCP transport configuration
     */
    public void setTcpTransportConfiguration(JxseTcpTransportConfiguration inConfig) {

        if (inConfig!=null) {
            TcpConfig = new JxseTcpTransportConfiguration(inConfig);
        } else {
            TcpConfig = new JxseTcpTransportConfiguration();
        }

    }

    /**
     * Returns a copy of the TCP transport configuration or {@code null} if it is not available.
     *
     * @return a copy of the TCP transport configuration or {@code null}
     */
    public JxseTcpTransportConfiguration getTcpTransportConfiguration() {

        return new JxseTcpTransportConfiguration(TcpConfig);

    }

    //
    // Setters and Getters
    //

    /**
     * Property key value
     */
    public static final String JXSE_CONNECTION_MODE = "JXSE_CONNECTION_MODE";
    
    /**
     * Sets the connection mode configuration. If {@code null}, any existing connection mode 
     * configuration is removed.
     * 
     * @param inMode Connection mode configuration or {@code null} to remove any existing.
     */
    public void setConnectionMode(ConnectionMode inMode) {

        if (inMode==null) {
            this.remove(JXSE_CONNECTION_MODE);
        } else {
            this.setProperty(JXSE_CONNECTION_MODE, inMode.toString());
        }

    }

    /**
     * Returns the connection mode configuration or {@code null} if none is available.
     *
     * @return a connection mode or {@code null}
     */
    public ConnectionMode getConnectionMode() {

        String Temp = this.getProperty(JXSE_CONNECTION_MODE);

        if (Temp!=null) {
            return ConnectionMode.valueOf(Temp);
        }

        return null;

    }

    /**
     * Property key value
     */
    public static final String JXSE_INFRASTRUCTURE_ID = "JXSE_INFRASTRUCTURE_ID";

    /**
     * Sets the infrastructure ID configuration. If {@code null}, any existing infrastructure ID
     * configuration is removed.
     *
     * @param infrastructureID Connection mode configuration or {@code null} to remove any existing.
     */
    public void setInfrastructureID(PeerGroupID infrastructureID) {

        if (infrastructureID==null) {
            this.remove(JXSE_INFRASTRUCTURE_ID);
        } else {
            this.setProperty(JXSE_INFRASTRUCTURE_ID, infrastructureID.toURI().toString());
        }

    }

    /**
     * Returns the infrastructure ID configuration or {@code null} if none is available.
     *
     * <p/>By Setting an alternate infrastructure PeerGroup ID (from NetPeerGroup),
     * it prevents heterogeneous infrastructure PeerGroups from intersecting.
     * <p/>This is highly recommended practice for application deployment
     *
     * @return Infrastructure ID or {@code null} if none available.
     */
    public PeerGroupID getInfrastructureID() {

        String Temp = this.getProperty(JXSE_INFRASTRUCTURE_ID);

        if (Temp!=null) {
            return PeerGroupID.create(URI.create(Temp));
        }

        return null;

    }

    /**
     * Property key value
     */
    public static final String JXSE_PEER_INSTANCE_NAME = "JXSE_PEER_INSTANCE_NAME";

    /**
     * Sets the peer instance name configuration. If {@code null}, any existing peer instance name
     * configuration is removed.
     *
     * @param peerInstanceName The name of the the peer or {@code null} to remove any existing.
     */
    public void setPeerInstanceName(String peerInstanceName) {

        if (peerInstanceName==null) {
            this.remove(JXSE_PEER_INSTANCE_NAME);
        } else {
            this.setProperty(JXSE_PEER_INSTANCE_NAME, peerInstanceName);
        }

    }

    /**
     * Returns the peer instance name or {@code null} if none is available.
     *
     * @return the peer instance name
     */
    public String getPeerInstanceName() {

        return this.getProperty(JXSE_PEER_INSTANCE_NAME);

    }

    /**
     * Property key value
     */
    public static final String JXSE_LOCAL_STORAGE = "JXSE_LOCAL_STORAGE";

    /**
     * Sets the peer's persistence location configuration. If {@code null}, any existing peer's
     * persistence location configuration is removed.

     * @param persistenceLocation URI indicating the local storage location or {@code null}
     * if none is available.
     */
    public void setPersistenceLocation(URI persistenceLocation) {

        if (persistenceLocation==null) {
            this.remove(JXSE_LOCAL_STORAGE);
        } else {
            this.setProperty(JXSE_LOCAL_STORAGE, persistenceLocation.toString());
        }

    }

    /**
     * Returns the URI to the persistence location, or {@code null} if none is available.
     *
     * @return persistence location URI, or {@code null} if none is available.
     */
    public URI getPersistenceLocation() {

        String Temp = this.getProperty(JXSE_LOCAL_STORAGE);

        if (Temp!=null) {
            return URI.create(Temp);
        }

        return null;

    }

    /**
     * Property key value
     */
    public static final String JXSE_PEER_ID = "JXSE_PEER_ID";

    /**
     * Sets the peer ID configuration. If {@code null}, any existing peer ID
     * configuration is removed.
     *
     * @param peerID The peer ID or {@code null} to remove any existing peer ID
     */
    public void setPeerID(PeerID peerID) {

        if (peerID==null) {
            this.remove(JXSE_PEER_ID);
        } else {
            this.setProperty(JXSE_PEER_ID, peerID.toURI().toString());
        }

    }

    /**
     * Returns the peer ID configuration or {@code null} if none is available.
     *
     * @return peer ID or {@code null} if none available.
     */
    public PeerID getPeerID() {

        String Temp = this.getProperty(JXSE_PEER_ID);

        if (Temp!=null) {
            return PeerID.create(URI.create(Temp));
        }

        return null;

    }

    /**
     * Property key value
     */
    private static final String JXSE_KEYSTORE_LOCATION = "JXSE_KEYSTORE_LOCATION";

    /**
     * Sets freestanding keystore location URI or removes any existing configuration if
     * the parameter is {@code null}.
     *
     * @param keyStoreLocation the absolute location of the freestanding keystore
     */
    public void setKeyStoreLocation(URI keyStoreLocation) {

        if (keyStoreLocation==null) {
            this.remove(JXSE_KEYSTORE_LOCATION);
        } else {
            this.setProperty(JXSE_KEYSTORE_LOCATION, keyStoreLocation.toString());
        }

    }

    /**
     * Provides the freestanding keystore location URI, or {@code null} if none is available.
     *
     * @return the URI location of the freestanding keystore or {@code null}
     */
    public URI getKeyStoreLocation() {

        String Temp = this.getProperty(JXSE_KEYSTORE_LOCATION);

        if (Temp!=null) {
            return URI.create(Temp);
        }

        return null;

    }

    /**
     * Property key value
     */
    private static final String JXSE_RELAY_MAX_CLIENT = "JXSE_RELAY_MAX_CLIENT";

    /**
     * Sets the RelayService maximum number of simultaneous relay clients, or removes any existing
     * configuration if the parameter is < 0.
     *
     * @param relayMaxClients the maximum number of relay clients
     */
    public void setRelayMaxClients(int relayMaxClients) {

        if (relayMaxClients <= 0) {
            this.remove(JXSE_RELAY_MAX_CLIENT);
        } else {
            this.setProperty(JXSE_RELAY_MAX_CLIENT, Integer.toString(relayMaxClients));
        }

    }

    /**
     * Returns the RelayService maximum number of simultaneous relay clients, or -1 if no
     * configuration is available.
     *
     * @return the maximum number of relay clients, or -1.
     */
    public int getRelayMaxClients() {

        String Temp = this.getProperty(JXSE_RELAY_MAX_CLIENT);

        if (Temp==null) {
            return -1;
        } else {
            return Integer.parseInt(Temp);
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_RENDEZVOUS_MAX_CLIENT = "JXSE_RENDEZVOUS_MAX_CLIENT";

    /**
     * Sets the RendezVousService maximum number of simultaneous rendezvous clients
     *
     * @param rdvMaxClients the new rendezvousMaxClients value
     */
    public void setRendezvousMaxClients(int rdvMaxClients) {

        if (rdvMaxClients <= 0) {
            this.remove(JXSE_RENDEZVOUS_MAX_CLIENT);
        } else {
            this.setProperty(JXSE_RENDEZVOUS_MAX_CLIENT, Integer.toString(rdvMaxClients));
        }

    }

    /**
     * Returns the Rendezvous maximum number of simultaneous relay clients, or -1 if no
     * configuration is available.
     *
     * @return the maximum number of relay clients, or -1.
     */
    public int getRendezvousMaxClients() {

        String Temp = this.getProperty(JXSE_RENDEZVOUS_MAX_CLIENT);

        if (Temp==null) {
            return -1;
        } else {
            return Integer.parseInt(Temp);
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_TCP_ENABLED = "JXSE_TCP_ENABLED";

    /**
     * Sets the TCP transport activation status.
     *
     * @param enabled if true, enables TCP transport
     */
    public void setTcpEnabled(boolean enabled) {

        this.setProperty(JXSE_TCP_ENABLED, Boolean.toString(enabled));

    }

    /**
     * Indicates whether the TCP transport is activated. If not configuration can be found,
     * this method returns false.
     *
     * @return boolean indicating whether the TCP transport is activated.
     */
    public boolean getTcpEnabled() {

        return Boolean.parseBoolean(this.getProperty(JXSE_TCP_ENABLED));

    }

    /**
     * Property key value
     */
    private static final String JXSE_MULTICAST_ENABLED = "JXSE_MULTICAST_ENABLED";

    /**
     * Sets the multicasting transport activation status.
     *
     * @param enabled if true, enables multicasting transport
     */
    public void setMulticastEnabled(boolean enabled) {

        this.setProperty(JXSE_MULTICAST_ENABLED, Boolean.toString(enabled));

    }

    /**
     * Indicates whether the multicasting transport is activated. If not configuration
     *  can be found, this method returns false.
     *
     * @return boolean indicating whether the TCP transport is activated.
     */
    public boolean getMulticastEnabled() {

        return Boolean.parseBoolean(this.getProperty(JXSE_MULTICAST_ENABLED));

    }

    /**
     * Property key value
     */
    private static final String JXSE_USE_ONLY_RELAY_SEED = "JXSE_USE_ONLY_RELAY_SEED";

    /**
     * Determines whether to restrict RelayService leases to those defined in
     * the seed list
     *
     * @param useOnlyRelaySeeds restrict RelayService lease to seed list
     */
    public void setUseOnlyRelaySeeds(boolean useOnlyRelaySeeds) {

        this.setProperty(JXSE_USE_ONLY_RELAY_SEED, Boolean.toString(useOnlyRelaySeeds));

    }

    /**
     * Indicates whether to restrict RelayService leases to those defined in
     * the seed list
     *
     * @return useOnlyRelaySeeds restrict RelayService lease to seed list
     */
    public boolean getUseOnlyRelaySeeds() {

        return Boolean.parseBoolean(this.getProperty(JXSE_USE_ONLY_RELAY_SEED));

    }

    /**
     * Property key value
     */
    private static final String JXSE_USE_ONLY_RDV_SEED = "JXSE_USE_ONLY_RDV_SEED";

    /**
     * Determines whether to restrict RendezvousService leases to those defined in
     * the seed list
     *
     * @param useOnlyRdvSeeds restrict RendezvousService lease to seed list
     */
    public void setUseOnlyRdvSeeds(boolean useOnlyRdvSeeds) {

        this.setProperty(JXSE_USE_ONLY_RDV_SEED, Boolean.toString(useOnlyRdvSeeds));

    }

    /**
     * Indicates whether to restrict RendezvousService leases to those defined in
     * the seed list
     *
     * @return useOnlyRdvSeeds restrict RendezvousService lease to seed list
     */
    public boolean getUseOnlyRdvSeeds() {

        return Boolean.parseBoolean(this.getProperty(JXSE_USE_ONLY_RDV_SEED));

    }

    /**
     * Property key value
     */
    private static final String JXSE_SEED_RELAY_URI = "JXSE_SEED_RELAY_URI";

    /**
     * Adds RelayService peer seed address using item numbers. If the {@code seedURI}
     * parameter is {@code null} or if the item number is negative, the corresponding
     * seed URI item is removed. Different item numbers allows the configuration of
     * several relay seed URIs.
     *
     * <p/>A RelayService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seedURI the relay seed URI
     * @param itemNumber seed URI item number
     */
    public void addSeedRelay(URI seedURI, int itemNumber) {

        String TempKey = JXSE_SEED_RELAY_URI + "_" + itemNumber;

        if ( (seedURI==null) || (itemNumber<0) ) {
            this.remove(TempKey);
        } else {
            this.setProperty(TempKey, seedURI.toString());
        }

    }

    /**
     * Return the relay seed URI corresponding to the item number or {@code null} if none is
     * available.
     *
     * @param itemNumber the URI item number
     * @return a relay seed URI or {@code null}
     */
    public URI getSeedRelay(int itemNumber) {

        String TempKey = JXSE_SEED_RELAY_URI + "_" + itemNumber;
        String Retrieved = this.getProperty(TempKey);

        if (Retrieved==null) {
            return null;
        } else {
            return URI.create(Retrieved);
        }

    }

    /**
     * Provides the complete set of registered seed relays in a map, where the key are
     * the item numbers.
     *
     * @return Map of item number and corresponding seed relay URI.
     */
    public synchronized Map<Integer,URI> getAllSeedRelays() {

        HashMap<Integer,URI> Result = new HashMap<Integer,URI>();

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {

            if ( Key.startsWith(JXSE_SEED_RELAY_URI)) {

                int ItemNumber = Integer.parseInt(Key.substring(JXSE_SEED_RELAY_URI.length()+1));
                Result.put(ItemNumber, URI.create(this.getProperty(Key)));

            }

        }

        return Result;

    }

    /**
     * Clears the list of RendezVousService seeds
     */
    public void clearSeedRelays() {

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {
            if ( Key.startsWith(JXSE_SEED_RELAY_URI)) {
                this.remove(Key);
            }
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_SEED_RDV_URI = "JXSE_SEED_RDV_URI";

    /**
     * Adds RendezvousService peer seed address using item numbers. If the {@code seedURI}
     * parameter is {@code null} or if the item number is negative, the corresponding
     * seed URI item is removed. Different item numbers allows the configuration of
     * several relay seed URIs.
     *
     * <p/>A RendezVousService seed is defined as a physical endpoint address
     * <p/>e.g. http://192.168.1.1:9700, or tcp://192.168.1.1:9701
     *
     * @param seedURI the relay seed URI
     * @param itemNumber seed URI item number
     */
    public void addSeedRendezvous(URI seedURI, int itemNumber) {

        String TempKey = JXSE_SEED_RDV_URI + "_" + itemNumber;

        if ( (seedURI==null) || (itemNumber<0) ) {
            this.remove(TempKey);
        } else {
            this.setProperty(TempKey, seedURI.toString());
        }

    }

    /**
     * Return the rendezvous seed URI corresponding to the item number or {@code null} if none is
     * available.
     *
     * @param itemNumber the URI item number
     * @return a rendezvous seed URI or {@code null}
     *
     */
    public URI getSeedRendezvous(int itemNumber) {

        String TempKey = JXSE_SEED_RDV_URI + "_" + itemNumber;
        String Retrieved = this.getProperty(TempKey);

        if (Retrieved==null) {
            return null;
        } else {
            return URI.create(Retrieved);
        }

    }

    /**
     * Provides the complete set of registered seed rendezvous in a map, where the key are
     * the item numbers.
     *
     * @return Map of item number and corresponding seed relay URI.
     */
    public synchronized Map<Integer,URI> getAllSeedRendezvous() {

        HashMap<Integer,URI> Result = new HashMap<Integer,URI>();

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {

            if ( Key.startsWith(JXSE_SEED_RDV_URI)) {

                int ItemNumber = Integer.parseInt(Key.substring(JXSE_SEED_RDV_URI.length()+1));
                Result.put(ItemNumber, URI.create(this.getProperty(Key)));

            }

        }

        return Result;

    }

    /**
     * Clears the list of RendezVousService seeds
     */
    public void clearSeedRendezvous() {

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {
            if ( Key.startsWith(JXSE_SEED_RDV_URI)) {
                this.remove(Key);
            }
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_SEEDING_RELAY_URI = "JXSE_SEEDING_RELAY_URI";

    /**
     * Adds RelayService seeding using item numbers. The URI should point to a resource
     * containing peer seed addresses. If the {@code seedURI}
     * parameter is {@code null} or if the item number is negative, the corresponding
     * URI item is removed. Different item numbers allows the configuration of
     * several relay seeding URIs.
     *
     * <p/>http://rdv.jxtahosts.net/cgi-bin/relays.cgi?3
     *
     * @param seedURI the relay seed URI
     * @param itemNumber seed URI item number
     */
    public void addSeedingRelay(URI seedURI, int itemNumber) {

        String TempKey = JXSE_SEEDING_RELAY_URI + "_" + itemNumber;

        if ( (seedURI==null) || (itemNumber<0) ) {
            this.remove(TempKey);
        } else {
            this.setProperty(TempKey, seedURI.toString());
        }

    }

    /**
     * Return the relay seeding URI corresponding to the item number or {@code null}
     * if none is available.
     *
     * @param itemNumber the URI item number
     * @return a relay seeding URI or {@code null}
     */
    public URI getSeedingRelay(int itemNumber) {

        String TempKey = JXSE_SEEDING_RELAY_URI + "_" + itemNumber;
        String Retrieved = this.getProperty(TempKey);

        if (Retrieved==null) {
            return null;
        } else {
            return URI.create(Retrieved);
        }

    }

    /**
     * Provides the complete set of registered seeding relays in a map, where the keys are
     * the item numbers.
     *
     * @return Map of item number and corresponding seeding relay URI.
     */
    public synchronized Map<Integer,URI> getAllSeedingRelays() {

        HashMap<Integer,URI> Result = new HashMap<Integer,URI>();

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {

            if ( Key.startsWith(JXSE_SEEDING_RELAY_URI)) {

                int ItemNumber = Integer.parseInt(Key.substring(JXSE_SEEDING_RELAY_URI.length()+1));
                Result.put(ItemNumber, URI.create(this.getProperty(Key)));

            }

        }

        return Result;

    }

    /**
     * Clears the list of RelayService seeds
     */
    public void clearSeedingRelays() {

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {
            if ( Key.startsWith(JXSE_SEEDING_RELAY_URI)) {
                this.remove(Key);
            }
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_SEEDING_RDV_URI = "JXSE_SEEDING_RDV_URI";

    /**
     * Adds RendezvousService seeding using item numbers. The URI should point to a resource
     * containing peer seed addresses. If the {@code seedURI}
     * parameter is {@code null} or if the item number is negative, the corresponding
     * URI item is removed. Different item numbers allows the configuration of
     * several relay seeding URIs.
     *
     * <p/>http://rdv.jxtahosts.net/cgi-bin/rendezvous.cgi?3
     *
     * @param seedURI the rendezvous seed URI
     * @param itemNumber seed URI item number
     */
    public void addSeedingRendezvous(URI seedURI, int itemNumber) {

        String TempKey = JXSE_SEEDING_RDV_URI + "_" + itemNumber;

        if ( (seedURI==null) || (itemNumber<0) ) {
            this.remove(TempKey);
        } else {
            this.setProperty(TempKey, seedURI.toString());
        }

    }

    /**
     * Return the rendezvous seeding URI corresponding to the item number or {@code null}
     * if none is available.
     *
     * @param itemNumber the URI item number
     * @return a rendezvous seeding URI or {@code null}
     */
    public URI getSeedingRendezvous(int itemNumber) {

        String TempKey = JXSE_SEEDING_RDV_URI + "_" + itemNumber;
        String Retrieved = this.getProperty(TempKey);

        if (Retrieved==null) {
            return null;
        } else {
            return URI.create(Retrieved);
        }

    }

    /**
     * Provides the complete set of registered seeding rendezvous in a map, where the keys are
     * the item numbers.
     *
     * @return Map of item number and corresponding seeding relay URI.
     */
    public Map<Integer,URI> getAllSeedingRendezvous() {

        HashMap<Integer,URI> Result = new HashMap<Integer,URI>();

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {

            if ( Key.startsWith(JXSE_SEEDING_RDV_URI)) {

                int ItemNumber = Integer.parseInt(Key.substring(JXSE_SEEDING_RDV_URI.length()+1));
                Result.put(ItemNumber, URI.create(this.getProperty(Key)));

            }

        }

        return Result;

    }

    /**
     * Clears the list of RelayService seeds
     */
    public void clearSeedingRendezvous() {

        for (String Key : PropertiesUtil.stringPropertyNames(this)) {
            if ( Key.startsWith(JXSE_SEEDING_RDV_URI)) {
                this.remove(Key);
            }
        }

    }

    // Preparing prefixes
    private static final String HttpPrefix = JxseHttpTransportConfiguration.TRANSPORT_NAME + "_";
    private static final String MulticastPrefix = JxseMulticastTransportConfiguration.TRANSPORT_NAME + "_";
    private static final String TcpPrefix = JxseTcpTransportConfiguration.TRANSPORT_NAME + "_";

    /**
     * Removing any existing transport configuration sub-entries.
     */
    private void removeExistingTransportConfigurationEntries() {

        // Removing any existing sub-entries
        for (String Item : PropertiesUtil.stringPropertyNames(this)) {
            if ( Item.startsWith(HttpPrefix) || Item.startsWith(TcpPrefix) || Item.startsWith(MulticastPrefix) ) {
                this.remove(Item);
            }
        }

    }

    /**
     * Copies the entries of the sub-transport configuration at this level.
     */
    private void prepareTransportConfigurationEntries() {

        // Removing any existing entries
        removeExistingTransportConfigurationEntries();

        // Processing HTTP config
        if ( this.HttpConfig != null ) {

            for (String Item : PropertiesUtil.stringPropertyNames(HttpConfig)) {

                String TempSub = HttpPrefix + Item;
                this.setProperty(TempSub, HttpConfig.getProperty(Item));

            }

        }

        // Processing Multicasting config
        if ( this.MulticastConfig != null ) {

            for (String Item : PropertiesUtil.stringPropertyNames(MulticastConfig)) {

                String TempSub = MulticastPrefix + Item;
                this.setProperty(TempSub, MulticastConfig.getProperty(Item));

            }

        }

        // Processing TCP config
        if ( this.TcpConfig != null ) {

            for (String Item : PropertiesUtil.stringPropertyNames(TcpConfig)) {

                String TempSub = TcpPrefix + Item;
                this.setProperty(TempSub, TcpConfig.getProperty(Item));

            }

        }

    }

    /**
     * Recreates the transport configuration
     */
    private synchronized void recreateTransportConfiguration() {

        // Recreating transports
        this.HttpConfig = JxseHttpTransportConfiguration.getDefaultHttpTransportConfiguration();
        this.MulticastConfig = JxseMulticastTransportConfiguration.getDefaultMulticastTransportConfiguration();
        this.TcpConfig = JxseTcpTransportConfiguration.getDefaultTcpTransportConfiguration();

        // Searching for sub-entries
        for (String Item : PropertiesUtil.stringPropertyNames(this)) {

            if (Item.startsWith(HttpPrefix)) {

                // Registering entry in the transport
                HttpConfig.setProperty(Item.substring(HttpPrefix.length()), this.getProperty(Item));
                this.remove(Item);

            } else if (Item.startsWith(MulticastPrefix)) {

                // Registering entry in the transport
                MulticastConfig.setProperty(Item.substring(MulticastPrefix.length()), this.getProperty(Item));
                this.remove(Item);

            } else if (Item.startsWith(TcpPrefix)) {

                // Registering entry in the transport
                TcpConfig.setProperty(Item.substring(TcpPrefix.length()), this.getProperty(Item));
                this.remove(Item);

            } // else the entry should stay at this level

        }

    }

    /**
     * In addition to loading this configuration, it creates the HTTP, TCP and Multicasting
     * configuration too.
     *
     */
    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException {

        // Calling super
        super.loadFromXML(in);

        // ...and recreating transport configuration
        recreateTransportConfiguration();
        removeExistingTransportConfigurationEntries();

    }
    
    @Override
    public synchronized void storeToXML(OutputStream os, String comment) throws IOException {

        // Preparing transport configuration entries
        prepareTransportConfigurationEntries();

        // ...and calling super
        super.storeToXML(os, comment);

        // Cleaning the mess
        removeExistingTransportConfigurationEntries();

    }

    @Override
    public synchronized void storeToXML(OutputStream os, String comment, String encoding) throws IOException {

        // Preparing transport configuration entries
        prepareTransportConfigurationEntries();

        // ...and calling super
        super.storeToXML(os, comment, encoding);

        // Cleaning the mess
        removeExistingTransportConfigurationEntries();

    }

    @Override
    public synchronized void store(OutputStream out, String comments) throws IOException {

        // Preparing transport configuration entries
        prepareTransportConfigurationEntries();

        // ...and calling super
        super.store(out, comments);
        
        // Cleaning the mess
        removeExistingTransportConfigurationEntries();

    }

}
