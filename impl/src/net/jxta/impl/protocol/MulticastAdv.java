/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.protocol;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.logging.Logging;
import net.jxta.protocol.TransportAdvertisement;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Provides configuration parameters for the JXTA TCP Message Transport.
 */
public class MulticastAdv extends TransportAdvertisement {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(MulticastAdv.class.getName());

//    private static final String CONFIGMODES[] = {"auto", "manual"};   // To be deleted in a future release
    private static final String INDEXFIELDS[] = {/* none */};

//    private static final String PORT_ELEMENT = "Port";    // To be deleted in a future release
    private static final String MCAST_THREAD_POOL = "Mcast_Pool_Size";
//    private static final String ClientOffTag = "ClientOff";   // To be deleted in a future release
//    private static final String ServerOffTag = "ServerOff";   // To be deleted in a future release
    private static final String MULTICAST_OFF_TAG = "MulticastOff";
    private static final String MULTICAST_INTERFACE_TAG = "MulticastInterface";
    private static final String MULTICAST_ADDRESS_TAG = "MulticastAddr";
    private static final String MULTICAST_PORT_TAG = "MulticastPort";
    private static final String FlagsTag = "Flags";
//    private static final String PublicAddressOnlyAttr = "PublicAddressOnly";  // To be deleted in a future release

//    private String configMode = CONFIGMODES[0];   // To be deleted in a future release
    private String interfaceAddress = null;
    private String mcastInterface = null;
//    private int startPort = -1;   // To be deleted in a future release
//    private int listenPort = -1;  // To be deleted in a future release
//    private int endPort = -1;     // To be deleted in a future release
//    private String publicAddress = null;  // To be deleted in a future release
    private String multicastaddr = null;
    private int multicastport = -1;
    private int poolSize = 5;
    private int multicastsize = -1;
//    private boolean clientEnabled = true; // To be deleted in a future release
//    private boolean serverEnabled = true; // To be deleted in a future release
    private boolean multicastEnabled = true;
//    private boolean publicAddressOnly = false;    // To be deleted in a future release

    /**
     * Our instantiator
     */
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        /**
         * {@inheritDoc}
         */
        public String getAdvertisementType() {
            return MulticastAdv.getAdvertisementType();
        }

        /**
         * {@inheritDoc}
         */
        public Advertisement newInstance() {
            return new MulticastAdv();
        }

        /**
         * {@inheritDoc}
         */
        public Advertisement newInstance(net.jxta.document.Element root) {
            if (!XMLElement.class.isInstance(root)) {
                throw new IllegalArgumentException(getClass().getName() + " only supports XLMElement");
            }

            return new MulticastAdv((XMLElement) root);
        }
    }

    /**
     * Returns the identifying type of this Advertisement.
     *
     * <p/><b>Note:</b> This is a static method. It cannot be used to determine
     * the runtime type of an advertisement. ie.
     * </p><code><pre>
     *      Advertisement adv = module.getSomeAdv();
     *      String advType = adv.getAdvertisementType();
     *  </pre></code>
     *
     * <p/><b>This is wrong and does not work the way you might expect.</b>
     * This call is not polymorphic and calls
     * Advertisement.getAdvertisementType() no matter what the real type of the
     * advertisement.
     *
     * @return String the type of advertisement
     */
    public static String getAdvertisementType() {
        return "jxta:MulticastTransportAdvertisement";
    }

    // FIXME: Should we use a different protocol for multicasting?
    private MulticastAdv() {
        setProtocol("tcp");
    }

    private MulticastAdv(XMLElement doc) {
        this();

        String doctype = doc.getName();

        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }

        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException(
                    "Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }

//        Attribute attr = doc.getAttribute(FlagsTag);
//
//
//        To be deleted in a future release
//
//        if (attr != null) {
//            String options = attr.getValue();
//            publicAddressOnly = (options.indexOf(PublicAddressOnlyAttr) != -1);
//        }

        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {

            XMLElement elem = (XMLElement) elements.nextElement();

            if (!handleElement(elem)) {
                Logging.logCheckedWarning(LOG, "Unhandled Element: ", elem);
            }

        }

//
//        To be deleted in a future release
//
//        // Sanity Check!!!
//        if (!Arrays.asList(CONFIGMODES).contains(configMode)) {
//            throw new IllegalArgumentException("Unsupported configuration mode.");
//        }
//
//        if ((listenPort < -1) || (listenPort > 65535)) {
//            throw new IllegalArgumentException("Illegal Listen Port Value");
//        }
//
//        if ((startPort < -1) || (startPort > 65535)) {
//            throw new IllegalArgumentException("Illegal Start Port Value");
//        }
//
//        if ((endPort < -1) || (endPort > 65535)) {
//            throw new IllegalArgumentException("Illegal End Port Value");
//        }
//
//        if ((0 == startPort) && (endPort != 0) || (0 != startPort) && (endPort == 0)) {
//            throw new IllegalArgumentException("Port ranges must both be 0 or non-0");
//        }
//
//        if ((-1 == startPort) && (endPort != -1) || (-1 != startPort) && (endPort == -1)) {
//            throw new IllegalArgumentException("Port ranges must both be -1 or not -1");
//        }
//
//        if ((null != publicAddress) && ((-1 != startPort) || (listenPort <= 0))) {
//            throw new IllegalArgumentException("Dynamic ports not supported with public address port forwarding.");
//        }

        if (getMulticastState() && (null == getMulticastAddr())) {
            throw new IllegalArgumentException("Multicast enabled and no address specified.");
        }

        if (getMulticastState() && (-1 == getMulticastPort())) {
            throw new IllegalArgumentException("Multicast enabled and no port specified.");
        }

        if (getMulticastState() && ((getMulticastPort() <= 0) || (getMulticastPort() > 65536))) {
            throw new IllegalArgumentException("Illegal Multicast Port Value");
        }

        if (getMulticastState() && (-1 == getMulticastSize())) {
            throw new IllegalArgumentException("Multicast enabled and no size specified.");
        }

        if (getMulticastState() && ((getMulticastSize() <= 0) || (getMulticastSize() > 1048575L))) {
            throw new IllegalArgumentException("Illegal Multicast datagram size");
        }

        // XXX 20050118 bondolo Some versions apparently don't initialize this field. Eventually make it required.
        if (null == getProtocol()) {
            setProtocol("tcp");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdvType() {
        return getAdvertisementType();
    }

    /**
     * Returns the interface which the TCP transport will use.
     *
     * @return The interface to use. May be a DNS name or an IP Address.
     */
    public String getInterfaceAddress() {
        return interfaceAddress;
    }

//    /**
//     * Sets the interface which the TCP transport will use.
//     *
//     * @param interfaceAddress The interface to use. May be a DNS name or an IP Address.
//     */
//    public void setInterfaceAddress(String interfaceAddress) {
//        if (null != interfaceAddress) {
//            interfaceAddress = interfaceAddress.trim();
//
//            if (0 == interfaceAddress.length()) {
//                interfaceAddress = null;
//            }
//        }
//        this.interfaceAddress = interfaceAddress;
//    }
//
//
//    To be deleted in a future release
//
//    /**
//     * Returns the port on which the TCP Transport will listen if configured to
//     * do so. If a port range is specified then this the preference. Valid
//     * values are <code>-1</code>, <code>0</code> and <code>1-65535</code>.
//     * The <code>-1</code> value is used to signify that there is no port
//     * preference and any port in range will be used. The <code>0</code>
//     * specifies that the Socket API dynamic port allocation should be used.
//     * For values <code>1-65535</code> the value specifies the required port on
//     * which the TCP transport will listen.
//     *
//     * @return the port
//     */
//    public int getPort() {
//        return listenPort;
//    }
//
//    /**
//     * Sets the port on which the TCP Transport will listen if configured to
//     * do so. If a port range is specified then this the preference. Valid
//     * values are <code>-1</code>, <code>0</code> and <code>1-65535</code>.
//     * The <code>-1</code> value is used to signify that there is no port
//     * preference and any port in range will be used. The <code>0</code>
//     * specifies that the Socket API dynamic port allocation should be used.
//     * For values <code>1-65535</code> the value specifies the required port on
//     * which the TCP transport will listen.
//     *
//     * @param port the port on which to listen.
//     */
//    public void setPort(int port) {
//        listenPort = port;
//    }
//
//    /**
//     * Return the lowest port on which the TCP Transport will listen if
//     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
//     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
//     * the port range feature should be disabled. The <code>0</code> specifies
//     * that the Socket API dynamic port allocation should be used. For values
//     * <code>1-65535</code> the value must be equal to or less than the value
//     * used for end port.
//     *
//     * @return the lowest port on which to listen.
//     */
//    public int getStartPort() {
//        return startPort;
//    }
//
//    /**
//     * Sets the lowest port on which the TCP Transport will listen if configured
//     * to do so. Valid values are <code>-1</code>, <code>0</code> and
//     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
//     * the port range feature should be disabled. The <code>0</code> specifies
//     * that the Socket API dynamic port allocation should be used. For values
//     * <code>1-65535</code> the value must be equal to or less than the value
//     * used for end port.
//     *
//     * @param startPort the lowest port on which to listen.
//     */
//    public void setStartPort(int startPort) {
//        this.startPort = startPort;
//    }
//
//    /**
//     * Return the highest port on which the TCP Transport will listen if
//     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
//     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
//     * the port range feature should be disabled. The <code>0</code> specifies
//     * that the Socket API dynamic port allocation should be used. For values
//     * <code>1-65535</code> the value must be equal to or greater than the value
//     * used for start port.
//     *
//     * @return the highest port on which to listen.
//     */
//    public int getEndPort() {
//        return endPort;
//    }
//
//    /**
//     * Sets the highest port on which the TCP Transport will listen if
//     * configured to do so. Valid values are <code>-1</code>, <code>0</code> and
//     * <code>1-65535</code>. The <code>-1</code> value is used to signify that
//     * the port range feature should be disabled. The <code>0</code> specifies
//     * that the Socket API dynamic port allocation should be used. For values
//     * <code>1-65535</code> the value must be equal to or greater than the value
//     * used for start port.
//     *
//     * @param endPort the highest port on which to listen.
//     */
//    public void setEndPort(int endPort) {
//        this.endPort = endPort;
//    }

    /**
     * Determine whether multicast if off or not
     *
     * @return boolean  current multicast state
     */
    public boolean getMulticastState() {
        return multicastEnabled;
    }

    /**
     * Enable or disable multicast.
     *
     * @param multicastState the desired state.
     */
    public void setMulticastState(boolean multicastState) {
        multicastEnabled = multicastState;
    }

    /**
     * returns the multicastaddr
     *
     * @return string multicastaddr
     */
    public String getMulticastAddr() {
        return multicastaddr;
    }

    /**
     * set the multicastaddr
     *
     * @param multicastaddr set multicastaddr
     */
    public void setMulticastAddr(String multicastaddr) {
        if (null != multicastaddr) {
            multicastaddr = multicastaddr.trim();

            if (0 == multicastaddr.length()) {
                multicastaddr = null;
            }
        }
        this.multicastaddr = multicastaddr;
    }

    /**
     * set the multicast interface
     *
     * @param multicastInterface set multicastaddr
     */
    public void setMulticastInterface(String multicastInterface) {
        if (null != multicastInterface) {
            multicastInterface = multicastInterface.trim();

            if (0 == multicastInterface.length()) {
                this.mcastInterface = null;
            }
        }
        this.mcastInterface = multicastInterface;
    }

    /**
     * Returns the multicast interface
     * @return the multicast interface, null if none specified, in which case it default to the tcp interface address
     *
     */
    public String getMulticastInterface() {
        return mcastInterface;
    }

    /**
     * returns the multicastport
     *
     * @return int multicastport
     */
    public int getMulticastPort() {
        return multicastport;
    }
    /**
     * returns the multicast pool size
     *
     * @return the multicast pool size
     */
    public int getMulticastPoolSize() {
        return poolSize;
    }

    /**
     * set the multicastport
     *
     * @param multicastport set multicastport
     */
    public void setMulticastPort(int multicastport) {
        this.multicastport = multicastport;
    }

    /**
     * set the multicast thread pool size
     *
     * @param size thread pool size
     */
    public void setMulticastPoolSize(int size) {
        this.poolSize = size;
    }

    /**
     * returns the multicastsize
     *
     * @return integer containting the multicast size
     */
    public int getMulticastSize() {
        return multicastsize;
    }

    /**
     * set the multicastsize
     *
     * @param multicastsize set multicast size
     */
    public void setMulticastSize(int multicastsize) {
        this.multicastsize = multicastsize;
    }

//
//  To be deleted in a future release
//
//    /**
//     * Returns the public address
//     *
//     * @return string public address
//     */
//    public String getServer() {
//        return publicAddress;
//    }
//
//    /**
//     * Set the public address. That is, a the address of a server socket
//     * to connect to from the outside. Argument is in the form host:port
//     *
//     * @param address address
//     */
//    public void setServer(String address) {
//        if (null != address) {
//            address = address.trim();
//
//            if (0 == address.length()) {
//                address = null;
//            }
//        }
//
//        this.publicAddress = address;
//    }
//
//    /**
//     * Returns the configuration for outbound connections.
//     *
//     * @return <code>true</code> if outbound connections are allowed otherwise
//     *         <code>false</code>
//     */
//    public boolean isClientEnabled() {
//        return clientEnabled;
//    }
//
//    /**
//     * Sets the configuration for outbound connections.
//     *
//     * @param enabled <code>true</code> if outbound connections are allowed otherwise
//     *                <code>false</code>
//     */
//    public void setClientEnabled(boolean enabled) {
//        clientEnabled = enabled;
//    }
//
//    /**
//     * Returns the configuration for outbound connections.
//     *
//     * @return enabled <code>true</code> if outbound connections are allowed otherwise
//     *                <code>false</code>
//     */
//    public boolean getClientEnabled() {
//        return clientEnabled;
//    }
//
//    /**
//     * Returns the configuration for inbound connections.
//     *
//     * @return <code>true</code> if inbound connections are allowed otherwise
//     *         <code>false</code>
//     */
//    public boolean isServerEnabled() {
//        return serverEnabled;
//    }
//
//    /**
//     * Sets the configuration for inbound connections.
//     *
//     * @param enabled <code>true</code> if inbound connections are allowed otherwise
//     *                <code>false</code>
//     */
//    public void setServerEnabled(boolean enabled) {
//        serverEnabled = enabled;
//    }
//
//    /**
//     * returns the config mode. That is, how the user prefers to configure
//     * the interface address: "auto", "manual"
//     *
//     * @return string config mode
//     */
//    public String getConfigMode() {
//        return configMode;
//    }
//
//    /**
//     * set the config mode. That is, how the user prefers to configure
//     * the interface address: "auto", "manual"
//     *
//     * This is just a pure config item. It is never in published advs. The TCP
//     * transport strips it when it initializes.
//     *
//     * @param mode Can be "auto", "manual" other settings will act as the default
//     *             which is "auto".
//     */
//    public void setConfigMode(String mode) {
//        if (!Arrays.asList(CONFIGMODES).contains(mode)) {
//            throw new IllegalArgumentException("Unsupported configuration mode.");
//        }
//        configMode = mode;
//    }
//
//    /**
//     * Returns the state of whether to only use public address
//     *
//     * @return boolean true if set to use "Public Address Only"
//     */
//    public boolean getPublicAddressOnly() {
//        return publicAddressOnly;
//    }
//
//    /**
//     * Sets the state of whether to only use public address
//     *
//     * @param only true to use "Public Address Only"
//     */
//    public void setPublicAddressOnly(boolean only) {
//        publicAddressOnly = only;
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleElement(Element raw) {

        if (super.handleElement(raw)) {
            return true;
        }

        XMLElement elem = (XMLElement) raw;

        if (elem.getName().equals(MULTICAST_OFF_TAG)) {
            setMulticastState(false);
            return true;
        }

//
//  To be deleted in a future release
//
//        if (elem.getName().equals(ClientOffTag)) {
//            setClientEnabled(false);
//            return true;
//        }
//
//        if (elem.getName().equals(ServerOffTag)) {
//            setServerEnabled(false);
//            return true;
//        }

        String value = elem.getTextValue().trim();
        if ((null == value) || (0 == value.length())) {
            return false;
        }

        value = value.trim();

        if (elem.getName().equals("Protocol")) {
            setProtocol(value);
            return true;
        }

//        if (PORT_ELEMENT.equals(elem.getName())) {
//            try {
//                setPort(Integer.parseInt(value));
//                Attribute startAttr = elem.getAttribute("start");
//                Attribute endAttr = elem.getAttribute("end");
//
//                if ((null != startAttr) && (null != endAttr)) {
//                    setStartPort(Integer.parseInt(startAttr.getValue().trim()));
//                    setEndPort(Integer.parseInt(endAttr.getValue().trim()));
//                }
//            } catch (NumberFormatException badPort) {
//                throw new IllegalArgumentException("Illegal port value : " + value);
//            }
//            return true;
//        }

        if (elem.getName().equals(MULTICAST_ADDRESS_TAG)) {
            setMulticastAddr(value);
            return true;
        }

        if (elem.getName().equals(MULTICAST_INTERFACE_TAG)) {
           this.setMulticastInterface(value);
            return true;
        }

        if (elem.getName().equals(MULTICAST_PORT_TAG)) {
            try {
                setMulticastPort(Integer.parseInt(value));
            } catch (NumberFormatException badPort) {
                throw new IllegalArgumentException("Illegal multicast port value : " + value);
            }
            return true;
        }
        if (elem.getName().equals(MCAST_THREAD_POOL)) {
            try {
                setMulticastPoolSize(Integer.parseInt(value));
            } catch (NumberFormatException badPort) {
                throw new IllegalArgumentException("Illegal multicast port value : " + value);
            }
            return true;
        }

        if (elem.getName().equals("MulticastSize")) {
            try {
                int theMulticastSize = Integer.parseInt(value);

                setMulticastSize(theMulticastSize);
            } catch (NumberFormatException badPort) {
                throw new IllegalArgumentException("Illegal multicast datagram size : " + value);
            }
            return true;
        }

//        if (elem.getName().equals("Server")) {
//            setServer(value);
//            return true;
//        }
//
//        if (elem.getName().equals("InterfaceAddress")) {
//            setInterfaceAddress(value);
//            return true;
//        }
//
//        if (elem.getName().equals("ConfigMode")) {
//            setConfigMode(value);
//            return true;
//        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document getDocument(MimeMediaType encodeAs) {
        StructuredDocument adv = (StructuredDocument) super.getDocument(encodeAs);

        if (!(adv instanceof Attributable)) {
            throw new IllegalStateException("Only Attributable document types allowed.");
        }

        // XXX 20050118 bondolo Some versions apparently don't initialize this field. Eventually make it required.
        if (null == getProtocol()) {
            setProtocol("tcp");
        }

//        if ((listenPort < -1) || (listenPort > 65535)) {
//            throw new IllegalStateException("Illegal Listen Port Value");
//        }
//
//        if ((startPort < -1) || (startPort > 65535)) {
//            throw new IllegalStateException("Illegal Start Port Value");
//        }
//
//        if ((endPort < -1) || (endPort > 65535)) {
//            throw new IllegalStateException("Illegal End Port Value");
//        }
//
//        if ((0 == startPort) && (endPort != 0) || (0 != startPort) && (endPort == 0)) {
//            throw new IllegalStateException("Port ranges must both be 0 or non-0");
//        }
//
//        if ((-1 == startPort) && (endPort != -1) || (-1 != startPort) && (endPort == -1)) {
//            throw new IllegalStateException("Port ranges must both be -1 or not -1");
//        }
//
//        if ((null != publicAddress) && ((-1 != startPort) || (listenPort <= 0))) {
//            throw new IllegalStateException("Dynamic ports not supported with public address port forwarding.");
//        }

        if (getMulticastState() && (null == getMulticastAddr())) {
            throw new IllegalStateException("Multicast enabled and no address specified.");
        }

        if (getMulticastState() && (-1 == getMulticastPort())) {
            throw new IllegalStateException("Multicast enabled and no port specified.");
        }

        if (getMulticastState() && ((getMulticastPort() <= 0) || (getMulticastPort() > 65536))) {
            throw new IllegalStateException("Illegal Multicast Port Value");
        }

        if (getMulticastState() && (-1 == getMulticastSize())) {
            throw new IllegalStateException("Multicast enabled and no size specified.");
        }

        if (getMulticastState() && ((getMulticastSize() <= 0) || (getMulticastSize() > 1048575L))) {
            throw new IllegalStateException("Illegal Multicast datagram size");
        }

//        if (adv instanceof Attributable) {
//            // Only one flag for now. Easy.
//            if (publicAddressOnly) {
//                ((Attributable) adv).addAttribute(FlagsTag, PublicAddressOnlyAttr);
//            }
//        }

        Element proto = adv.createElement("Protocol", getProtocol());

        adv.appendChild(proto);

//        if (!isClientEnabled()) {
//            Element clientEnabled = adv.createElement(ClientOffTag);
//            adv.appendChild(clientEnabled);
//        }
//
//        if (!isServerEnabled()) {
//            Element serverOff = adv.createElement(ServerOffTag);
//            adv.appendChild(serverOff);
//        }
//
//        if (getConfigMode() != null) {
//            Element configMode = adv.createElement("ConfigMode", getConfigMode());
//            adv.appendChild(configMode);
//        }

        String interfaceAddr = getInterfaceAddress();

        if (null != interfaceAddr) {
            Element interfaceAddrr = adv.createElement("InterfaceAddress", interfaceAddr);
            adv.appendChild(interfaceAddrr);
        }

//        Element portEl = adv.createElement(PORT_ELEMENT, Integer.toString(listenPort));
//        adv.appendChild(portEl);
//        if (adv instanceof Attributable) {
//            Attributable attrElem = (Attributable) portEl;
//            if ((-1 != startPort) && (-1 != endPort)) {
//                attrElem.addAttribute("start", Integer.toString(startPort));
//                attrElem.addAttribute("end", Integer.toString(endPort));
//            }
//        }
//
//        String serverAddr = getServer();
//        if (null != serverAddr) {
//            Element server = adv.createElement("Server", serverAddr);
//            adv.appendChild(server);
//        }

        if (!getMulticastState()) {
            Element mOff = adv.createElement(MULTICAST_OFF_TAG);
            adv.appendChild(mOff);
        }

        if (null != getMulticastAddr()) {
            Element mAddrr = adv.createElement(MULTICAST_ADDRESS_TAG, getMulticastAddr());
            adv.appendChild(mAddrr);
        }

        if (null != this.getMulticastInterface()) {
            Element mInterace = adv.createElement(MULTICAST_INTERFACE_TAG, getMulticastInterface());
            adv.appendChild(mInterace);
        }

        if (-1 != getMulticastPort()) {
            Element mPort = adv.createElement(MULTICAST_PORT_TAG, Integer.toString(getMulticastPort()));
            adv.appendChild(mPort);
        }

        if (-1 != getMulticastPoolSize()) {
            Element poolEl = adv.createElement(MCAST_THREAD_POOL, Integer.toString(getMulticastPoolSize()));
            adv.appendChild(poolEl);
        }

        if (-1 != getMulticastSize()) {
            Element mSize = adv.createElement("MulticastSize", Integer.toString(getMulticastSize()));
            adv.appendChild(mSize);
        }

        return adv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getIndexFields() {
        return INDEXFIELDS;
    }

//    /**
//     * Gets the configuration for inbound connections.
//     *
//     * @return true if inbound connections are allowed, returns false otherwise
//     * @see #setServerEnabled
//     */
//    public boolean getServerEnabled() {
//        return serverEnabled;
//    }

}
