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
 * Defines the http transport configuration when connecting to the JXTA network.
 */
public class JxseHttpTransportConfiguration extends JxtaTransportConfiguration {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseHttpTransportConfiguration.class.getName());

    /**
     * Transport name.
     */
    public static final String TRANSPORT_NAME = "HTTP";

    /**
     * Default HTTP port value.
     */
    public static final int DEFAULT_HTTP_PORT = 9901;

    /**
     * Return an HTTP transport configuration containing defaults.
     *
     * @return a default HTTP transport configuration
     */
    public static final JxseHttpTransportConfiguration getDefaultHttpTransportConfiguration() {

        // Preparing return value
        JxseHttpTransportConfiguration Result = new JxseHttpTransportConfiguration();

        Result.setDefaultPropertyValue(JXSE_HTTP_OUTGOING, Boolean.toString(true));
        Result.setDefaultPropertyValue(JXSE_HTTP_INCOMING, Boolean.toString(false));
        Result.setDefaultPropertyValue(JXSE_HTTP_PORT, Integer.toString(DEFAULT_HTTP_PORT));

        return Result;

    }

    /**
     * Simple constructor.
     */
    public JxseHttpTransportConfiguration() {

        super();

    }

    /**
     * This constructor copies all entries from the provided parameter into this object.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxseHttpTransportConfiguration(JxseHttpTransportConfiguration toCopy) {

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
    private static final String JXSE_HTTP_INCOMING = "JXSE_HTTP_INCOMING";

    /**
     * To allow or disable incoming HTTP communication.
     *
     * @param incoming 
     */
    public void setHttpIncoming(boolean incoming) {

        this.setProperty(JXSE_HTTP_INCOMING, Boolean.toString(incoming));

    }

    /**
     * Returns the HTTP incoming communication configuration.
     *
     * @return {@code true} if incoming HTTP communication is allowed, {@code false} otherwise
     */
    public boolean getHttpIncoming() {

        return Boolean.parseBoolean(this.getProperty(JXSE_HTTP_INCOMING));

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP_OUTGOING = "JXSE_HTTP_OUTGOING";

    /**
     * To allow or disable outgoing HTTP communication.
     *
     * @param outgoing Allows HTTP outgoing communication if {@code true}, else does not.
     */
    public void setHttpOutgoing(boolean outgoing) {

        this.setProperty(JXSE_HTTP_OUTGOING, Boolean.toString(outgoing));

    }

    /**
     * Returns the HTTP outgoing communication configuration.
     *
     * @return {@code true} if outgoing HTTP communication is allowed, {@code false} otherwise
     */
    public boolean getHttpOutgoing() {

        return Boolean.parseBoolean(this.getProperty(JXSE_HTTP_OUTGOING));

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP_PORT = "JXSE_HTTP_PORT";

    /**
     * Sets the HTTP listening port value. If negative or greater than 65535, any
     * existing configuration is removed.
     *
     * @param port the new HTTP port value
     */
    public void setHttpPort(int port) {

        if ( ( port < 0 )  || ( port > 65535 ) ) {
            this.remove(JXSE_HTTP_PORT);
        } else {
            this.setProperty(JXSE_HTTP_PORT, Integer.toString(port));
        }

    }

    /**
     * Returns the HTTP listening port value. If no configuration is available, -1 is return.
     *
     * @return the HTTP listening port value
     */
    public int getHttpPort() {

        String Temp = this.getProperty(JXSE_HTTP_PORT);

        if ( Temp!=null ) {
            return Integer.parseInt(Temp);
        } else {
            return -1;
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP_INTERFACE_ADDRESS = "JXSE_HTTP_INTERFACE_ADDRESS";

    /**
     * Sets the HTTP interface Address to bind the outgoing HTTP transport (<p/>e.g. "192.168.1.1").
     *
     * <p>Remember that HTTP is not a symmetric transport (like TCP). This interface address is for
     * request going out from this peer.
     *
     * @param address the address value
     */
    public void setHttpInterfaceAddress(String address) {

        if ( address == null ) {
            this.remove(JXSE_HTTP_INTERFACE_ADDRESS);
        } else {
            this.setProperty(JXSE_HTTP_INTERFACE_ADDRESS, address);
        }

    }

    /**
     * Returns the HTTP interface address or {@code null} if none is available.
     *
     * @return HTTP interface Address
     */
    public String getHttpInterfaceAddress() {

        return this.getProperty(JXSE_HTTP_INTERFACE_ADDRESS);

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP_PUBLIC_ADDRESS = "JXSE_HTTP_PUBLIC_ADDRESS";

    /**
     * Property key value
     */
    private static final String JXSE_HTTP_PUBLIC_ADDRESS_EXCLUSIVE = "JXSE_HTTP_PUBLIC_ADDRESS_EXCLUSIVE";

    /**
     * Sets the HTTP JXTA Public Address for incoming http requests (e.g. "133.42.7.28:9700").
     *
     * <p>Remember that HTTP is not a symmetric transport (like TCP). This interface address is for
     * request coming to this peer.
     *
     * <p>The {@code exclusive} boolean indicate whether this address should be use for both incoming
     * and outgoing http requests. This is sometimes necessary when peers are located behind NAT and
     * firewalls.
     *
     * @param address   the HTTP transport public address
     * @param exclusive determines whether this address should advertised exclusively
     */
    public void setHttpPublicAddress(String address, boolean exclusive) {

        if ( address == null ) {
            this.remove(JXSE_HTTP_PUBLIC_ADDRESS);
            this.remove(JXSE_HTTP_PUBLIC_ADDRESS_EXCLUSIVE);
        } else {
            this.setProperty(JXSE_HTTP_PUBLIC_ADDRESS, address);
            this.setProperty(JXSE_HTTP_PUBLIC_ADDRESS_EXCLUSIVE, Boolean.toString(exclusive));
        }

    }

    /**
     * Returns the HTTP public address configuration if any is available or {@code null} otherwise.
     *
     * @return HTTP public address configuration
     */
    public String getHttpPublicAddress() {

        return this.getProperty(JXSE_HTTP_PUBLIC_ADDRESS);

    }

    /**
     * Indicates whether the HTTP public address is used exclusively. If no configuration is available
     * {@code false} is returned.
     *
     * @return HTTP public address exclusive use
     */
    public boolean getHttpPublicAddressExclusive() {

        String Temp = this.getProperty(JXSE_HTTP_PUBLIC_ADDRESS_EXCLUSIVE);

        if (Temp==null) {
            return false;
        } else {
            return Boolean.parseBoolean(Temp);
        }

    }

}
