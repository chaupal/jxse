/*
 * Copyright (c) 2004-2007 Sun Microsystems, Inc.  All rights reserved.
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

package tutorial.customgroupservice;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.document.Attributable;


/**
 * Defines Gossip Service configuration parameters.
 * <p/>
 * <tt><pre>
 *      &lt;jxta:GossipServiceConfigAdv showOwn="true">
 *          &lt;Gossip>
 *              Bonjour!
 *          &lt;/Gossip>
 *      &lt;/jxta:GossipServiceConfigAdv>
 * </pre></tt>
 */
public final class GossipServiceConfigAdv extends ExtendableAdvertisement implements Cloneable {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(GossipServiceConfigAdv.class.getName());
    /**
     *  The advertisement index fields. (currently none).
     */
    private static final String[] INDEX_FIELDS = {};
    /**
     * The DOCTYPE
     */
    private static final String advType = "jxta:GossipServiceConfigAdv";
    private static final String SHOW_OWN_ATTR = "showOwn";
    private static final String GOSSIP_TEXT_TAG = "jxta:GossipServiceConfigAdv";

/**
     * Instantiator for GossipServiceConfigAdv
     */
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        /**
         * {@inheritDoc}
         */
        public String getAdvertisementType() {
            return advType;
        }

        /**
         * {@inheritDoc}
         */
        public Advertisement newInstance() {
            return new GossipServiceConfigAdv();
        }

        /**
         * {@inheritDoc}
         */
        public Advertisement newInstance(Element root) {
            if (!XMLElement.class.isInstance(root)) {
                throw new IllegalArgumentException(getClass().getName() + " only supports XLMElement");
            }

            return new GossipServiceConfigAdv((XMLElement) root);
        }
    }
    /**
     * If {@code true} then the gossip service should show it's own gossips.
     */
    private Boolean showOwn = null;
    /**
     * The text we will "gossip".
     */
    private String gossip = null;

    /**
     * Returns the identifying type of this Advertisement.
     * <p/>
     * <b>Note:</b> This is a static method. It cannot be used to determine
     * the runtime type of an advertisement. ie.
     * </p><code><pre>
     *      Advertisement adv = module.getSomeAdv();
     *      String advType = adv.getAdvertisementType();
     *  </pre></code>
     * <p/>
     * <b>This is wrong and does not work the way you might expect.</b>
     * This call is not polymorphic and calls
     * Advertisement.getAdvertisementType() no matter what the real type of the
     * advertisement.
     *
     * @return String the type of advertisement
     */
    public static String getAdvertisementType() {
        return advType;
    }

    /**
     * Use the Instantiator through the factory
     */
    private GossipServiceConfigAdv() {
    }

    /**
     * Use the Instantiator method to construct Peer Group Config Advs.
     *
     * @param doc the element
     */
    private GossipServiceConfigAdv(XMLElement doc) {
        String doctype = doc.getName();

        String typedoctype = "";
        Attribute itsType = doc.getAttribute("type");

        if (null != itsType) {
            typedoctype = itsType.getValue();
        }

        if (!doctype.equals(getAdvertisementType()) && !getAdvertisementType().equals(typedoctype)) {
            throw new IllegalArgumentException("Could not construct : " + getClass().getName() + "from doc containing a " + doc.getName());
        }

        /* Process attributes from root element */
        Enumeration<Attribute> eachAttr = doc.getAttributes();

        while (eachAttr.hasMoreElements()) {
            Attribute aConfigAttr = eachAttr.nextElement();

            if (super.handleAttribute(aConfigAttr)) {
                // nothing to do
            } else if (SHOW_OWN_ATTR.equals(aConfigAttr.getName())) {
                setShowOwn(Boolean.valueOf(aConfigAttr.getValue().trim()));
            } else {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Unhandled Attribute: " + aConfigAttr.getName());
                }
            }
        }

        /* process child elements of root */
        Enumeration<XMLElement> elements = doc.getChildren();

        while (elements.hasMoreElements()) {
            XMLElement elem = elements.nextElement();

            if (!handleElement(elem)) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Unhandled Element: " + elem.toString());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleElement(Element raw) {
        if (super.handleElement(raw)) {
            return true;
        }

        XMLElement elem = (XMLElement) raw;

        if (GOSSIP_TEXT_TAG.equals(elem.getName())) {
            String value = elem.getTextValue();

            if (null == value) {
                return false;
            }

            value = value.trim();
            if (0 == value.length()) {
                return false;
            }

            gossip = value;

            return true;
        }


        return false;
    }

    /**
     * Make a safe clone of this GossipServiceConfigAdv.
     *
     * @return Object A copy of this GossipServiceConfigAdv
     */
    @Override
    public GossipServiceConfigAdv clone() {
        try {
            GossipServiceConfigAdv clone = (GossipServiceConfigAdv) super.clone();

            clone.setShowOwn(getShowOwn());
            clone.setGossip(getGossip());

            return clone;
        } catch (CloneNotSupportedException impossible) {
            throw new Error("Object.clone() threw CloneNotSupportedException", impossible);
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
     * {@inheritDoc}
     */
    @Override
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ID getID() {
        return ID.nullID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructuredDocument getDocument(MimeMediaType encodeAs) {
        StructuredDocument adv = (StructuredDocument) super.getDocument(encodeAs);

        if (!(adv instanceof Attributable)) {
            throw new IllegalArgumentException("Only document types supporting atrributes are allowed");
        }

        if (null != getShowOwn()) {
            ((Attributable) adv).addAttribute(SHOW_OWN_ATTR, Boolean.toString(getShowOwn()));
        }

        if (null != getGossip()) {
            Element e = adv.createElement(GOSSIP_TEXT_TAG, getGossip());

            adv.appendChild(e);
        }

        return adv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getIndexFields() {
        return INDEX_FIELDS;
    }

    /**
     * Returns the gossip text which should be used by the gossip service.
     *
     * @return The gossip text which should be used by the gossip service or
     * {@code null} if the service should use it's default value.
     */
    public String getGossip() {
        return gossip;
    }

    /**
     * Sets the gossip text which should be used by the gossip service.
     *
     * @param gossip The gossip text which should be used by the gossip service.
     */
    public void setGossip(String gossip) {
        this.gossip = gossip;
    }

    /**
     * Returns whether the gossip service should show it's own gossip text.
     *
     * @return If {@code true} then we should show our own gossip text,
     * {@code false} to omit it or {@code null} to use service default.
     */
    public Boolean getShowOwn() {
        return showOwn;
    }

    /**
     * Sets whether the gossip service should show it's own gossip text.
     *
     * @param showOwn If {@code true} then we should show our own gossip text,
     * {@code false} to omit it or {@code null} to use service default.
     */
    public void setShowOwn(Boolean showOwn) {
        this.showOwn = showOwn;
    }
}