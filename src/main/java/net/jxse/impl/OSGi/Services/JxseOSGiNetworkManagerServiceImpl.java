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
import java.util.logging.Logger;
import net.jxse.OSGi.Services.JxseOSGiNetworkManagerService;
import net.jxse.configuration.JxseConfigurationTool;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxta.configuration.JxtaConfigurationException;
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

        return JxseConfigurationTool.getConfiguredNetworkManager(TheConfig);

    }

    /**
     * Making sure any NetworkManager has been stopped
     */
    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {

        if (this.TheNM!=null) {
            if (this.TheNM.isStarted()) {
                this.TheNM.stopNetwork();
            }
            this.TheNM=null;
        }

        // Calling super
        super.finalize();

    }

}
