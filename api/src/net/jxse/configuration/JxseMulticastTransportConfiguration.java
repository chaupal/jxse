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

import java.util.logging.Logger;
import net.jxta.configuration.JxtaTransportConfiguration;

/**
 * Defines the multicasting transport configuration when connecting to the JXTA network.
 */
public class JxseMulticastTransportConfiguration extends JxtaTransportConfiguration {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseMulticastTransportConfiguration.class.getName());

    /**
     * Transport name.
     */
    public static final String TRANSPORT_NAME = "MULTI";

    /**
     * Default IP group multicast address
     */
    public static final String DEFAULT_IP_MULTICAST_ADDRESS = "224.0.1.85";

    /**
     * Default IP group multicast port
     */
    public static final int DEFAULT_IP_MULTICAST_PORT = 1234;

    /**
     * Return an HTTP transport configuration containing defaults
     */
    public static final JxseMulticastTransportConfiguration getDefaultMulticastTransportConfiguration() {

        // Preparing return value
        JxseMulticastTransportConfiguration Result = new  JxseMulticastTransportConfiguration();

        Result.setDefaultPropertyValue(JXSE_MULTICAST_ADDRESS, DEFAULT_IP_MULTICAST_ADDRESS);
        Result.setDefaultPropertyValue(JXSE_MULTICAST_PORT, Integer.toString(DEFAULT_IP_MULTICAST_PORT));

        return Result;

    }

    /**
     * Simple constructor.
     */
    public JxseMulticastTransportConfiguration() {

        super();

    }

    /**
     * This constructor copies all entries from the provided parameter into this object.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxseMulticastTransportConfiguration(JxseMulticastTransportConfiguration toCopy) {

        super(toCopy);

    }

    @Override
    public String getTransportName() {

        return TRANSPORT_NAME;

    }

    //
    // Setters and Getters
    //

    /**
     * Property key value
     */
    private static final String JXSE_MULTICAST_PACKET_SIZE = "JXSE_MULTICAST_PACKET_SIZE";

    /**
     * Sets the IP group multicast packet size. If size < 0, any existing configuration is removed.
     *
     * @param size the new multicast packet size
     */
    public void setMulticastPacketSize(int size) {

        if ( size < 0 ) {
            this.remove(JXSE_MULTICAST_PACKET_SIZE);
        } else {
            this.setProperty(JXSE_MULTICAST_PACKET_SIZE, Integer.toString(size));
        }

    }

    /**
     * Returns the IP group multicast packet size, or -1 if not configuration is available
     *
     * @return the multicast packet size or -1.
     */
    public int getMulticastPacketSize() {

        String Temp = this.getProperty(JXSE_MULTICAST_PACKET_SIZE);

        if ( Temp!=null ) {
            return Integer.parseInt(Temp);
        } else {
            return -1;
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_MULTICAST_ADDRESS = "JXSE_MULTICAST_ADDRESS";

    /**
     * Sets the IP group multicast address, or removes any existing configuration if parameter is
     * {@code null}.
     *
     * @param mcastAddress the IP multicast group address
     * @see #setMulticastPort
     */
    public void setMulticastAddress(String mcastAddress) {

        if ( mcastAddress == null ) {
            this.remove(JXSE_MULTICAST_ADDRESS);
        } else {
            this.setProperty(JXSE_MULTICAST_ADDRESS, mcastAddress);
        }

    }

    /**
     * Returns the IP group multicast address, or {@code null} if no configuration is available.
     */
    public String getMulticastAddress() {

        return this.getProperty(JXSE_MULTICAST_ADDRESS);

    }


    /**
     * Property key value
     */
    private static final String JXSE_MULTICAST_INTERFACE = "JXSE_MULTICAST_INTERFACE";

    /**
     * Sets the multicast network interface or removes any existing configuration if parameter is
     * {@code null}.
     *
     * @param interfaceAddress multicast network interface
     */
    public void setMulticastInterface(String interfaceAddress) {

        if ( interfaceAddress == null ) {
            this.remove(JXSE_MULTICAST_INTERFACE);
        } else {
            this.setProperty(JXSE_MULTICAST_INTERFACE, interfaceAddress);
        }

    }

    /**
     * Returns the IP multicast network interface or {@code null} if no configuration is available.
     *
     * @return the multicast network interface or {@code null}
     */
    public String getMulticastInterface() {

        return this.getProperty(JXSE_MULTICAST_INTERFACE);

    }

    /**
     * Property key value
     */
    private static final String JXSE_MULTICAST_PORT = "JXSE_MULTICAST_PORT";


    /**
     * Sets the IP group multicast port or removes any existing configuration if parameter is < 0.
     *
     * @param port the IP group multicast port
     * @see #setMulticastAddress
     */
    public void setMulticastPort(int port) {

        if ( port < 0 ) {
            this.remove(JXSE_MULTICAST_PORT);
        } else {
            this.setProperty(JXSE_MULTICAST_PORT, Integer.toString(port));
        }

    }

    /**
     * Returns the current multicast port, or -1 if no configuration is available.
     *
     * @return the current mutlicast port
     * @see #setMulticastPort
     */
    public int getMulticastPort() {

        String Temp = this.getProperty(JXSE_MULTICAST_PORT);

        if ( Temp!=null ) {
            return Integer.parseInt(Temp);
        } else {
            return -1;
        }

    }

}
