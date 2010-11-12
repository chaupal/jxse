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
 * Defines the TCP transport configuration when connecting to the JXTA network.
 */
public class JxseTcpTransportConfiguration extends JxtaTransportConfiguration {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseTcpTransportConfiguration.class.getName());

    /**
     * Transport name.
     */
    public static final String TRANSPORT_NAME = "TCP";

    /**
     * Default TCP port
     */
    public static final int DEFAULT_PORT = 9701;

    /**
     * Default TCP port start
     */
    public static final int DEFAULT_START_PORT = 9701;

    /**
     * Default TCP port end
     */
    public static final int DEFAULT_END_PORT = 9799;

    /**
     * Provides a TCP transport configuration containing defaults
     *
     * @return a TCP transport configuration containing defaults
     */
    public static JxseTcpTransportConfiguration getDefaultTcpTransportConfiguration() {

        // Preparing result
        JxseTcpTransportConfiguration Result = new JxseTcpTransportConfiguration();

        Result.setDefaultPropertyValue(JXSE_TCP_PORT, Integer.toString(DEFAULT_PORT));
        Result.setDefaultPropertyValue(JXSE_TCP_START_PORT, Integer.toString(DEFAULT_START_PORT));
        Result.setDefaultPropertyValue(JXSE_TCP_END_PORT, Integer.toString(DEFAULT_END_PORT));

        return Result;

    }

    /**
     * Simple constructor.
     */
    public JxseTcpTransportConfiguration() {

        super();

    }

    /**
     * This constructor copies all entries from the provided parameter into this object.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxseTcpTransportConfiguration(JxseTcpTransportConfiguration toCopy) {

        super(toCopy);

    }

    //
    // Setters and Getters
    //

    /**
     * Property key value
     */
    public static final String JXSE_TCP_PORT = "JXSE_TCP_PORT";

    /**
     * Sets the TCP transport listening port or removes any existing configuration
     * if parameter is lesser than 0 or greater then 65535.
     *
     * @param port the new tcpPort value
     */
    public void setTcpPort(int port) {

        if ( ( port < 0 ) || ( port > 65535 ) ) {
            this.remove(JXSE_TCP_PORT);
        } else {
            this.setProperty(JXSE_TCP_PORT, Integer.toString(port));
        }

    }

    /**
     * Returns the TCP transport listening port or -1 if no existing configuration
     * can be found.
     *
     * @return the TCP port value or -1.
     */
    public int getTcpPort() {

        String Temp = this.getProperty(JXSE_TCP_PORT);

        if (Temp!=null) {
            return Integer.parseInt(Temp);
        }

        return -1;

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_START_PORT = "JXSE_TCP_START_PORT";

    /**
     * Sets the lowest port on which the TCP Transport will listen if configured
     * to do so. Valid values are <code>-1</code>, <code>0</code> and
     * <code>1-65535</code>.
     * <p>The <code>-1</code> value is used to signify that the port range feature
     * should be disabled. The <code>0</code> specifies that the Socket API dynamic port
     * allocation should be used. For values <code>1-65535</code> the value must
     * be equal to or less than the value used for end port.
     * <p>If the parameter is < 0 or > 65535, any existing configuration is removed.
     *
     * @param start the lowest port on which to listen.
     */
    public void setTcpStartPort(int start) {

        if ( ( start < 0 ) || ( start > 65535 ) ){
            this.remove(JXSE_TCP_START_PORT);
        } else {
            this.setProperty(JXSE_TCP_START_PORT, Integer.toString(start));
        }

    }

    /**
     * Return the TCP start port or -1 if no configuration is available.
     *
     * @return the TCP start port or -1.
     */
    public int getTcpStartPort() {

        String Temp = this.getProperty(JXSE_TCP_START_PORT);

        if (Temp!=null) {
            return Integer.parseInt(Temp);
        }

        return -1;

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_END_PORT = "JXSE_TCP_END_PORT";

    /**
     * Returns the highest port on which the TCP Transport will listen if
     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
     * <code>1-65535</code>.
     * <p>The <code>-1</code> value is used to signify that
     * the port range feature should be disabled. The <code>0</code> specifies
     * that the Socket API dynamic port allocation should be used. For values
     * <code>1-65535</code> the value must be equal to or greater than the value
     * used for start port.
     * <p>If the parameter is < 0 or > 65535, any existing configuration is removed.
     *
     * @param end the new TCP end port
     */
    public void setTcpEndPort(int end) {

        if ( (end<0) || (end>65535) ){
            this.remove(JXSE_TCP_END_PORT);
        } else {
            this.setProperty(JXSE_TCP_END_PORT, Integer.toString(end));
        }

    }

    /**
     * Return the TCP end port or -1 if no configuration is available.
     *
     * @return the TCP end port or -1.
     */
    public int getTcpEndPort() {

        String Temp = this.getProperty(JXSE_TCP_END_PORT);

        if (Temp!=null) {
            return Integer.parseInt(Temp);
        }

        return -1;

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_INCOMING = "JXSE_TCP_INCOMING";

    /**
     * Toggles TCP transport server (incoming) mode (default is on)
     *
     * @param incoming the new TCP server mode
     */
    public void setTcpIncoming(boolean incoming) {

        this.setProperty(JXSE_TCP_INCOMING, Boolean.toString(incoming));

    }

    /**
     * Returns the TCP transport server (incoming) mode (default is on)
     *
     * @param incoming the new TCP server mode
     */
    public boolean getTcpIncoming() {

        return Boolean.parseBoolean(this.getProperty(JXSE_TCP_INCOMING));

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_OUTGOING = "JXSE_TCP_OUTGOING";

    /**
     * Toggles TCP transport server (outgoing) mode (default is on)
     *
     * @param incoming the new TCP server mode
     */
    public void setTcpOutgoing(boolean outgoing) {

        this.setProperty(JXSE_TCP_OUTGOING, Boolean.toString(outgoing));

    }

    /**
     * Returns the TCP transport server (incoming) mode (default is on)
     *
     * @param incoming the new TCP server mode
     */
    public boolean getTcpOutgoing() {

        return Boolean.parseBoolean(this.getProperty(JXSE_TCP_OUTGOING));

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_INTERFACE_ADDRESS = "JXSE_TCP_INTERFACE_ADDRESS";

    /**
     * Sets the TCP transport interface address, or removes any existing configuration
     * if the parameter is {@code null}.
     * <p/>e.g. "192.168.1.1"
     *
     * @param address the TCP transport interface address
     */
    public void setTcpInterfaceAddress(String address) {

        if ( address == null ) {
            this.remove(JXSE_TCP_INTERFACE_ADDRESS);
        } else {
            this.setProperty(JXSE_TCP_INTERFACE_ADDRESS, address);
        }

    }

    /**
     * Provides the TCP interface address or {@code null} if none is available.
     *
     * @return the TCP interface address configuration or {@code null}
     */
    public String getTcpInterfaceAddress() {

        return this.getProperty(JXSE_TCP_INTERFACE_ADDRESS);

    }

    /**
     * Property key value
     */
    public static final String JXSE_TCP_PUBLIC_ADDRESS = "JXSE_TCP_PUBLIC_ADDRESS";
    public static final String JXSE_TCP_PUBLIC_ADDRESS_EXCLUSIVE = "JXSE_TCP_PUBLIC_ADDRESS_EXCLUSIVE";

    /**
     * Sets the node public address or removes any configuration if {@code address} is {@code null}.
     * <p/>e.g. "192.168.1.1:9701"
     * <p/>This address is the physical address defined in a node's
     * AccessPointAdvertisement.  This often required for NAT'd/FW nodes
     *
     * @param address   the TCP transport public address
     * @param exclusive public address advertised exclusively
     */
    public void setTcpPublicAddress(String address, Boolean exclusive) {

        if ( address == null ) {
            this.remove(JXSE_TCP_PUBLIC_ADDRESS);
            this.remove(JXSE_TCP_PUBLIC_ADDRESS_EXCLUSIVE);
        } else {
            this.setProperty(JXSE_TCP_PUBLIC_ADDRESS, address);
            this.setProperty(JXSE_TCP_PUBLIC_ADDRESS_EXCLUSIVE, Boolean.toString(exclusive));
        }

    }

    /**
     * Provides the TCP public address or {@code null} if no configuration is available.
     *
     * @return the TCP public address
     */
    public String getTcpPublicAddress() {

        return this.getProperty(JXSE_TCP_PUBLIC_ADDRESS);

    }

    /**
     * Indicates the TCP public address exclusivity or {@code false} if no configuration is available.
     *
     * @return the TCP public address
     */
    public Boolean isTcpPublicAddressExclusive() {

        return Boolean.parseBoolean(this.getProperty(JXSE_TCP_PUBLIC_ADDRESS_EXCLUSIVE));

    }

    @Override
    public String getTransportName() {

        return TRANSPORT_NAME;

    }

}
