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
public class JxseHttp2TransportConfiguration extends JxtaTransportConfiguration {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseHttp2TransportConfiguration.class.getName());

    /**
     * Transport name.
     */
    public static final String TRANSPORT_NAME = "HTTP2";

    /**
     * Default HTTP 2 port value.
     */
    public static final int DEFAULT_HTTP2_PORT = 8080;

    /**
     * Return an HTTP 2 transport configuration containing defaults
     */
    public static final JxseHttp2TransportConfiguration getDefaultHttp2TransportConfiguration() {

        // Preparing return value
        JxseHttp2TransportConfiguration Result = new JxseHttp2TransportConfiguration();

        Result.setDefaultPropertyValue(JXSE_HTTP2_OUTGOING, Boolean.toString(true));
        Result.setDefaultPropertyValue(JXSE_HTTP2_INCOMING, Boolean.toString(true));
        Result.setDefaultPropertyValue(JXSE_HTTP2_PORT, Integer.toString(DEFAULT_HTTP2_PORT));

        return Result;

    }

    /**
     * Simple constructor.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxseHttp2TransportConfiguration() {

        super();

    }

    /**
     * This constructor copies all entries from the provided parameter into this object.
     *
     * @param toCopy entries to copy in this object.
     */
    public JxseHttp2TransportConfiguration(JxseHttp2TransportConfiguration toCopy) {

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
    private static final String JXSE_HTTP2_INCOMING = "JXSE_HTTP2_INCOMING";

    /**
     * To allow or disable incoming HTTP 2 communication.
     *
     * @param incoming 
     */
    public void setHttp2Incoming(boolean incoming) {

        this.setProperty(JXSE_HTTP2_INCOMING, Boolean.toString(incoming));

    }

    /**
     * Returns the HTTP 2 incoming communication configuration.
     */
    public boolean getHttp2Incoming() {

        return Boolean.parseBoolean(this.getProperty(JXSE_HTTP2_INCOMING));

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP2_OUTGOING = "JXSE_HTTP2_OUTGOING";

    /**
     * To allow or disable outgoing HTTP 2 communication.
     *
     * @param incoming
     */
    public void setHttp2Outgoing(boolean outgoing) {

        this.setProperty(JXSE_HTTP2_OUTGOING, Boolean.toString(outgoing));

    }

    /**
     * Returns the HTTP 2 outgoing communication configuration.
     */
    public boolean getHttp2Outgoing() {

        return Boolean.parseBoolean(this.getProperty(JXSE_HTTP2_OUTGOING));

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP2_PORT = "JXSE_HTTP2_PORT";

    /**
     * Sets the HTTP 2 listening port value. If negative or greater than 65535, any
     * existing configuration is removed.
     *
     * @param port the new HTTP 2 port value
     */
    public void setHttp2Port(int port) {

        if ( ( port < 0 )  || ( port > 65535 ) ) {
            this.remove(JXSE_HTTP2_PORT);
        } else {
            this.setProperty(JXSE_HTTP2_PORT, Integer.toString(port));
        }

    }

    /**
     * Returns the HTTP 2 listening port value. If no configuration is available, -1 is return.
     */
    public int getHttp2Port() {

        String Temp = this.getProperty(JXSE_HTTP2_PORT);

        if ( Temp!=null ) {
            return Integer.parseInt(Temp);
        } else {
            return -1;
        }

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP2_INTERFACE_ADDRESS = "JXSE_HTTP2_INTERFACE_ADDRESS";

    /**
     * Sets the HTTP 2interface Address to bind the outgoing HTTP 2transport (<p/>e.g. "192.168.1.1").
     *
     * <p>Remember that HTTP 2 is not a symmetric transport (like TCP). This interface address is for
     * request going out from this peer.
     *
     * @param address the address value
     */
    public void setHttp2InterfaceAddress(String address) {

        if ( address == null ) {
            this.remove(JXSE_HTTP2_INTERFACE_ADDRESS);
        } else {
            this.setProperty(JXSE_HTTP2_INTERFACE_ADDRESS, address);
        }

    }

    /**
     * Returns the HTTP 2 interface address or {@code null} if none is available.
     *
     * @return HTTP interface Address
     */
    public String getHttp2InterfaceAddress() {

        return this.getProperty(JXSE_HTTP2_INTERFACE_ADDRESS);

    }

    /**
     * Property key value
     */
    private static final String JXSE_HTTP2_PUBLIC_ADDRESS = "JXSE_HTTP2_PUBLIC_ADDRESS";

    /**
     * Property key value
     */
    private static final String JXSE_HTTP2_PUBLIC_ADDRESS_EXCLUSIVE = "JXSE_HTTP2_PUBLIC_ADDRESS_EXCLUSIVE";

    /**
     * Sets the HTTP 2 JXTA Public Address for incoming http requests (e.g. "133.42.7.28:9700").
     *
     * <p>Remember that HTTP 2 is not a symmetric transport (like TCP). This interface address is for
     * request coming to this peer.
     *
     * @param address the HTTP 2 transport public address
     * @param exclusive determines whether this address should advertised exclusively
     */
    public void setHttp2PublicAddress(String address, boolean exclusive) {

        if ( address == null ) {
            this.remove(JXSE_HTTP2_PUBLIC_ADDRESS);
            this.remove(JXSE_HTTP2_PUBLIC_ADDRESS_EXCLUSIVE);
        } else {
            this.setProperty(JXSE_HTTP2_PUBLIC_ADDRESS, address);
            this.setProperty(JXSE_HTTP2_PUBLIC_ADDRESS_EXCLUSIVE, Boolean.toString(exclusive));
        }

    }

    /**
     * Indicates whether the HTTP2 public address is used exclusively. If no configuration
     * is available {@code false} is returned.
     *
     * @return HTTP2 public address exclusive use
     */
    public boolean isHttp2PublicAddressExclusive() {

        String Temp = this.getProperty(JXSE_HTTP2_PUBLIC_ADDRESS_EXCLUSIVE);

        if (Temp==null) {
            return false;
        } else {
            return Boolean.parseBoolean(Temp);
        }

    }

    /**
     * Returns the HTTP2 public address configuration if any is available or {@code null} otherwise.
     *
     * @return HTTP2 public address configuration
     */
    public String getHttp2PublicAddress() {

        return this.getProperty(JXSE_HTTP2_PUBLIC_ADDRESS);

    }

    /**
     * Property key value
     */
    public static final String JXSE_HTTP2_START_PORT = "JXSE_HTTP2_START_PORT";

    /**
     * Sets the lowest port on which the HTTP2 Transport will listen if configured
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
    public void setHttp2StartPort(int start) {

        if ( ( start < 0 ) || ( start > 65535 ) ){
            this.remove(JXSE_HTTP2_START_PORT);
        } else {
            this.setProperty(JXSE_HTTP2_START_PORT, Integer.toString(start));
        }

    }

    /**
     * Return the HTTP2 start port or -1 if no configuration is available.
     *
     * @return the HTTP2 start port or -1.
     */
    public int getHttp2StartPort() {

        String Temp = this.getProperty(JXSE_HTTP2_START_PORT);

        if (Temp!=null) {
            return Integer.parseInt(Temp);
        }

        return -1;

    }

    /**
     * Property key value
     */
    public static final String JXSE_HTTP2_END_PORT = "JXSE_HTTP2_END_PORT";

    /**
     * Returns the highest port on which the HTTP2 Transport will listen if
     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
     * <code>1-65535</code>.
     * <p>The <code>-1</code> value is used to signify that
     * the port range feature should be disabled. The <code>0</code> specifies
     * that the Socket API dynamic port allocation should be used. For values
     * <code>1-65535</code> the value must be equal to or greater than the value
     * used for start port.
     * <p>If the parameter is < 0 or > 65535, any existing configuration is removed.
     *
     * @param end the new HTTP2 end port
     */
    public void setHttp2EndPort(int end) {

        if ( (end<0) || (end>65535) ){
            this.remove(JXSE_HTTP2_END_PORT);
        } else {
            this.setProperty(JXSE_HTTP2_END_PORT, Integer.toString(end));
        }

    }

    /**
     * Return the HTTP2 end port or -1 if no configuration is available.
     *
     * @return the HTTP2 end port or -1.
     */
    public int getHttp2EndPort() {

        String Temp = this.getProperty(JXSE_HTTP2_END_PORT);

        if (Temp!=null) {
            return Integer.parseInt(Temp);
        }

        return -1;

    }

}
