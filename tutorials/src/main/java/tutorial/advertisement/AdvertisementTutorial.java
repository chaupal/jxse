/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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

package tutorial.advertisement;

import net.jxta.document.*;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Simple Advertisement Tutorial creates a advertisment describing a system
 * <p/>
 * <pre>
 * &lt;?xml version="1.0"?>
 * &lt;!DOCTYPE jxta:System>
 * &lt;jxta:System xmlns:jxta="http://jxta.org">
 *   &lt;id>id&lt;/id>
 *   &lt;name>Device Name&lt;/name>
 *   &lt;ip>ip address&lt;/ip>
 *   &lt;hwarch>x86&lt;/hwarch>
 *   &lt;hwvendor>Sun MicroSystems&lt;/hwvendor>
 *   &lt;OSName>&lt;/OSName>
 *   &lt;OSVer>&lt;/OSVer>
 *   &lt;osarch>&lt;/osarch>
 *   &lt;sw>&lt;/sw>
 * &lt;/jxta:System>
 * </pre>
 */
public class AdvertisementTutorial extends Advertisement implements Comparable, Cloneable, Serializable {
    private String hwarch;
    private String hwvendor;
    private ID id = ID.nullID;
    private String ip;
    private String name;
    private String osname;
    private String osversion;
    private String osarch;
    private String inventory;

    private final static Logger LOG = Logger.getLogger(AdvertisementTutorial.class.getName());
    private final static String OSNameTag = "OSName";
    private final static String OSVersionTag = "OSVer";
    private final static String OSarchTag = "osarch";
    private final static String hwarchTag = "hwarch";
    private final static String hwvendorTag = "hwvendor";
    private final static String idTag = "ID";
    private final static String ipTag = "ip";
    private final static String nameTag = "name";
    private final static String swTag = "sw";

    /**
     * Indexable fields.  Advertisements must define the indexables, in order
     * to properly index and retrieve these advertisements locally and on the
     * network
     */
    private final static String[] fields = {idTag, nameTag, hwarchTag};

    /**
     * Default Constructor
     */
    public AdvertisementTutorial() {
    }

    /**
     * Construct from a StructuredDocument
     *
     * @param root Root element
     */
    public AdvertisementTutorial(Element root) {
        TextElement doc = (TextElement) root;

        if (!getAdvertisementType().equals(doc.getName())) {
            throw new IllegalArgumentException(
                    "Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        initialize(doc);
    }

    /**
     * Construct a doc from InputStream
     *
     * @param stream the underlying input stream.
     * @throws IOException if an I/O error occurs.
     */

    public AdvertisementTutorial(InputStream stream) throws IOException {
        StructuredTextDocument doc = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, stream);
        initialize(doc);
    }

    /**
     * Sets the hWArch attribute of the AdvertisementTutorial object
     *
     * @param hwarch The new hWArch value
     */
    public void setHWArch(String hwarch) {
        this.hwarch = hwarch;
    }

    /**
     * Sets the OSArch attribute of the AdvertisementTutorial object
     *
     * @param osarch The new hWArch value
     */
    public void setOSArch(String osarch) {
        this.osarch = osarch;
    }

    /**
     * Sets the hWVendor attribute of the AdvertisementTutorial object
     *
     * @param hwvendor The new hWVendor value
     */
    public void setHWVendor(String hwvendor) {
        this.hwvendor = hwvendor;
    }

    /**
     * sets the unique id
     *
     * @param id The id
     */
    public void setID(ID id) {
        this.id = (id == null ? null : id);
    }

    /**
     * Sets the iP attribute of the AdvertisementTutorial object
     *
     * @param ip The new iP value
     */
    public void setIP(String ip) {
        this.ip = ip;
    }

    /**
     * Sets the name attribute of the AdvertisementTutorial object
     *
     * @param name The new name value
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the oSName attribute of the AdvertisementTutorial object
     *
     * @param osname The new oSName value
     */
    public void setOSName(String osname) {
        this.osname = osname;
    }

    /**
     * Sets the oSVersion attribute of the AdvertisementTutorial object
     *
     * @param osversion The new oSVersion value
     */
    public void setOSVersion(String osversion) {
        this.osversion = osversion;
    }

    /**
     * Sets the SWInventory attribute of the AdvertisementTutorial object
     *
     * @param inventory the software inventory of the system
     */
    public void setSWInventory(String inventory) {
        this.inventory = inventory;
    }

    /**
     * {@inheritDoc}
     *
     * @param asMimeType Document encoding
     * @return The document value
     */
    @Override
    public Document getDocument(MimeMediaType asMimeType) {
        StructuredDocument adv = StructuredDocumentFactory.newStructuredDocument(asMimeType, getAdvertisementType());

        if (adv instanceof Attributable) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }
        Element e;

        e = adv.createElement(idTag, getID().toString());
        adv.appendChild(e);
        e = adv.createElement(nameTag, getName().trim());
        adv.appendChild(e);
        e = adv.createElement(OSNameTag, getOSName().trim());
        adv.appendChild(e);
        e = adv.createElement(OSVersionTag, getOSVersion().trim());
        adv.appendChild(e);
        e = adv.createElement(OSarchTag, getOSArch().trim());
        adv.appendChild(e);
        e = adv.createElement(ipTag, getIP().trim());
        adv.appendChild(e);
        e = adv.createElement(hwarchTag, getHWArch().trim());
        adv.appendChild(e);
        e = adv.createElement(hwvendorTag, getHWVendor().trim());
        adv.appendChild(e);
        e = adv.createElement(swTag, getSWInventory().trim());
        adv.appendChild(e);

        return adv;
    }

    /**
     * Gets the hWArch attribute of the AdvertisementTutorial object
     *
     * @return The hWArch value
     */
    public String getHWArch() {
        return hwarch;
    }

    /**
     * Gets the OSArch attribute of the AdvertisementTutorial object
     *
     * @return The OSArch value
     */
    public String getOSArch() {
        return osarch;
    }

    /**
     * Gets the hWVendor attribute of the AdvertisementTutorial object
     *
     * @return The hWVendor value
     */
    public String getHWVendor() {
        return hwvendor;
    }

    /**
     * returns the id of the device
     *
     * @return ID the device id
     */
    @Override
    public ID getID() {
        return (id == null ? null : id);
    }

    /**
     * Gets the IP attribute of the AdvertisementTutorial object
     *
     * @return The IP value
     */
    public String getIP() {
        return ip;
    }

    /**
     * Gets the name attribute of the AdvertisementTutorial object
     *
     * @return The name value
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the OSName attribute of the AdvertisementTutorial object
     *
     * @return The OSName value
     */
    public String getOSName() {
        return osname;
    }

    /**
     * Gets the Software Inventory text element
     *
     * @return The Inventory value
     */
    public String getSWInventory() {
        if (inventory == null) {
            inventory = "";
        }
        return inventory;
    }

    /**
     * Gets the OSVersion attribute of the AdvertisementTutorial object
     *
     * @return The OSVersion value
     */
    public String getOSVersion() {
        return osversion;
    }

    /**
     * Process an individual element from the document.
     *
     * @param elem the element to be processed.
     * @return true if the element was recognized, otherwise false.
     */
    protected boolean handleElement(TextElement elem) {
        if (elem.getName().equals(idTag)) {
            try {
                URI id = new URI(elem.getTextValue());

                setID(IDFactory.fromURI(id));
            } catch (URISyntaxException badID) {
                throw new IllegalArgumentException("unknown ID format in advertisement: " + elem.getTextValue());
            } catch (ClassCastException badID) {
                throw new IllegalArgumentException("Id is not a known id type: " + elem.getTextValue());
            }
            return true;
        }
        if (elem.getName().equals(nameTag)) {
            setName(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(OSNameTag)) {
            setOSName(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(OSVersionTag)) {
            setOSVersion(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(OSarchTag)) {
            setOSArch(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(ipTag)) {
            setIP(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(hwarchTag)) {
            setHWArch(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(hwvendorTag)) {
            setHWVendor(elem.getTextValue());
            return true;
        }
        if (elem.getName().equals(swTag)) {
            setSWInventory(elem.getTextValue());
            return true;
        }
        // element was not handled
        return false;
    }

    /**
     * Intialize a System advertisement from a portion of a structured document.
     *
     * @param root document root
     */
    protected void initialize(Element root) {
        if (!TextElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() + " only supports TextElement");
        }
        TextElement doc = (TextElement) root;

        if (!doc.getName().equals(getAdvertisementType())) {
            throw new IllegalArgumentException(
                    "Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }
        Enumeration elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            TextElement elem = (TextElement) elements.nextElement();

            if (!handleElement(elem)) {
                LOG.warning("Unhandleded element \'" + elem.getName() + "\' in " + doc.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String[] getIndexFields() {
        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AdvertisementTutorial) {
            AdvertisementTutorial adv = (AdvertisementTutorial) obj;

            return getID().equals(adv.getID());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Object other) {
        return getID().toString().compareTo(other.toString());
    }

    /**
     * All messages have a type (in xml this is &#0033;doctype) which
     * identifies the message
     *
     * @return String "jxta:AdvertisementTutorial"
     */
    public static String getAdvertisementType() {
        return "jxta:AdvertisementTutorial";
    }

    /**
     * Instantiator
     */
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        /**
         * Returns the identifying type of this Advertisement.
         *
         * @return String the type of advertisement
         */
        public String getAdvertisementType() {
            return AdvertisementTutorial.getAdvertisementType();
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the
         * type specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         *         could not be created.
         */
        public Advertisement newInstance() {
            return new AdvertisementTutorial();
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the
         * type specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @param root Specifies a portion of a StructuredDocument which will
         *             be converted into an Advertisement.
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         *         could not be created.
         */
        public Advertisement newInstance(net.jxta.document.Element root) {
            return new AdvertisementTutorial(root);
        }
    }

    /**
     * Main method
     *
     * @param args command line arguments.  None defined
     */
    public static void main(String args[]) {

        // The following step is required and only need to be done once,
        // without this step the AdvertisementFactory has no means of
        // associating an advertisement name space with the proper obect
        // in this cast the AdvertisementTutorial
        AdvertisementFactory.registerAdvertisementInstance(AdvertisementTutorial.getAdvertisementType()
                ,
                new AdvertisementTutorial.Instantiator());

        AdvertisementTutorial advTutorial = new AdvertisementTutorial();

        advTutorial.setID(ID.nullID);
        advTutorial.setName("AdvertisementTutorial");
        try {
            advTutorial.setIP(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ignored) {// ignored
        }
        advTutorial.setOSName(System.getProperty("os.name"));
        advTutorial.setOSVersion(System.getProperty("os.version"));
        advTutorial.setOSArch(System.getProperty("os.arch"));
        advTutorial.setHWArch(System.getProperty("HOSTTYPE", System.getProperty("os.arch")));
        advTutorial.setHWVendor(System.getProperty("java.vm.vendor"));
        System.out.println(advTutorial.toString());
    }
}
