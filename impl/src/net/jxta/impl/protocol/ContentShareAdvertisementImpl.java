/*
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.protocol;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLElement;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.ContentShareAdvertisement;

/**
 * Basic implementation of the ContentShareAdvertisement which contains
 * just the ContentAdvertisement of the Content being shared.  This
 * advertisement is intended to be extended by provider implementations
 * in order to add information relating to how to contact the provider
 * to obtain the advertised content.
 */
public class ContentShareAdvertisementImpl extends ContentShareAdvertisement {
    /**
     * Logger.
     */
    private static final Logger LOG =
            Logger.getLogger(ContentShareAdvertisementImpl.class.getName());

    /*
     * XXX 20070911 mcumings: The use of a ContentID tag is a hack that
     * should be removed when advertisement indexing improves to allow
     * indexing directly into the embedded ContentAdvertisement.
     */

    /**
     * ContentID field.  This is required for indexing purposes since
     * there is not currently to index into the embedded adv.
     */
    private static final String contentIDTag = "ContentID";

    /**
     * ContentAdvertisement field.
     */
    private static final String contentAdvTag =
            ContentAdvertisement.getAdvertisementType();

    /**
     * Fields to index on.
     */
    private static final String [] fields = {
        contentIDTag
    };

    /**
     * Instantiator for this Advertisement type.
     */
    public static class Instantiator
            implements AdvertisementFactory.Instantiator {
        /**
         *  {@inheritDoc}
         */
        public String getAdvertisementType( ) {
            return ContentShareAdvertisement.getAdvertisementType();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance( ) {
            return new ContentShareAdvertisementImpl();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance( net.jxta.document.Element root ) {
            return new ContentShareAdvertisementImpl( root );
        }
    };

    /**
     * Default constructor.
     */
    public ContentShareAdvertisementImpl() {
        // set defaults
        setContentAdvertisement( null );
    }

    /**
     * Constructs an advertisement instance from the root element
     * provided.
     *
     * @param root root element of the advertisement
     */
    public ContentShareAdvertisementImpl( Element root ) {
        if( !XMLElement.class.isInstance( root ) )
            throw new IllegalArgumentException(
                    getClass().getName() + " only supports XMLElement" );

        XMLElement doc = (XMLElement) root;

        String doctype = doc.getName();

        String typedoctype = "";
        Attribute itsType = doc.getAttribute( "type" );
        if( null != itsType )
            typedoctype = itsType.getValue();

        if( !doctype.equals(getAdvertisementType())
        && !getAdvertisementType().equals(typedoctype) ) {
            throw new IllegalArgumentException("Could not construct : "
                    + getClass().getName() + "from doc containing a "
                    + doc.getName() );
        }

        Enumeration elements = doc.getChildren();
        while (elements.hasMoreElements()) {
            XMLElement elem = (XMLElement) elements.nextElement();

            if ((!handleElement( elem ))
                    && Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine( "Unhandled Element: " + elem.toString());
            }
        }

        // Sanity Check!!!
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    protected boolean handleElement( Element raw ) {

        if ( super.handleElement( raw ) )
            return true;

        XMLElement elem = (XMLElement) raw;
        String nm = elem.getName();

        if (nm.equals(contentAdvTag)) {
             try {
                ContentAdvertisement contentAdv =
                        (ContentAdvertisement) AdvertisementFactory.newAdvertisement(elem);
                setContentAdvertisement(contentAdv);
            } catch (ClassCastException wrongAdv) {
                throw new IllegalArgumentException(
                        "Bad content advertisement in advertisement");
            }
            return true;
        }

        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Document getDocument( MimeMediaType encodeAs ) {
        StructuredDocument adv = (StructuredDocument) super.getDocument( encodeAs );
        Element e;

        ContentAdvertisement cAdv = getContentAdvertisement();
        if (cAdv != null) {
            e = adv.createElement(contentIDTag, cAdv.getContentID().toString());
            adv.appendChild(e);

            StructuredTextDocument advDoc =
                    (StructuredTextDocument) cAdv.getDocument(encodeAs);
            StructuredDocumentUtils.copyElements(adv, adv, advDoc);
        }

        return adv;
    }

    /**
     *  {@inheritDoc}
     */
    public String [] getIndexFields() {
        return fields;
    }

}
