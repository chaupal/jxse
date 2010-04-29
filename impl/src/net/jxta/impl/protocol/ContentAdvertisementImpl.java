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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.ContentID;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attribute;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.id.IDFactory;
import net.jxta.protocol.ContentAdvertisement;

/**
 * Basic implementation of the ContentAdvertisement which contains enough
 * information to reproduce everything about a Content with the exception
 * of the data payload itself.  This advertisement is intended to be
 * extended by those creating more capable ContentProvider implementations
 * such that additional information can be provided alongside this minimal
 * data set.
 */
public class ContentAdvertisementImpl extends ContentAdvertisement {
    /**
     * Logger.
     */
    private static final Logger LOG =
            Logger.getLogger(ContentAdvertisementImpl.class.getName());

    /**
     * ContentID field.
     */
    private static final String contentIDTag = "ContentID";

    /**
     * MetaID field.
     */
    private static final String metaIDTag = "MetaID";

    /**
     * MIME type field.
     */
    private static final String docMimeTag = "DocMime";

    /**
     * Fields to index on.
     */
    private static final String [] fields = {
        contentIDTag, metaIDTag, docMimeTag
    };

    /**
     * Instantiator for this Advertisement type.
     */
    public static class Instantiator
            implements AdvertisementFactory.Instantiator {
        /**
         *  {@inheritDoc}
         */
        public String getAdvertisementType() {
            return ContentAdvertisement.getAdvertisementType();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance() {
            return new ContentAdvertisementImpl();
        }

        /**
         *  {@inheritDoc}
         */
        public Advertisement newInstance( net.jxta.document.Element root ) {
            return new ContentAdvertisementImpl( root );
        }
    };

    public ContentAdvertisementImpl() {
        // set defaults
        setContentID( null );
        setMetaID( null );
    }

    public ContentAdvertisementImpl( Element root ) {
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

            if ( !handleElement(elem) ) {
                Logging.logCheckedFine(LOG, "Unhandled Element: ", elem);
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
        MimeMediaType mimeType;
        String str;
        URI uri;

        if (nm.equals(contentIDTag)) {
            try {
                uri = new URI( elem.getTextValue() );
                setContentID((ContentID) IDFactory.fromURI( uri ));
            } catch ( URISyntaxException badID ) {
                throw new IllegalArgumentException(
                        "Bad Content ID in advertisement", badID);
            }
            return true;
        }

        if (nm.equals(metaIDTag)) {
            try {
                uri =  new URI( elem.getTextValue() );
                setMetaID((ContentID) IDFactory.fromURI( uri ));
            } catch ( URISyntaxException badID ) {
                throw new IllegalArgumentException(
                        "Bad Meta ID in advertisement", badID);
            }
            return true;
        }

        if (nm.equals(docMimeTag)) {
            str =  elem.getTextValue();
            mimeType = new MimeMediaType(str);
            setMimeType(mimeType);
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

        e = adv.createElement(contentIDTag, getContentID().toString());
        adv.appendChild(e);

        if (getMetaID() != null) {
            e = adv.createElement(metaIDTag, getMetaID().toString());
            adv.appendChild(e);
        }

        if (getMimeType() != null) {
            e = adv.createElement(docMimeTag, getMimeType().toString());
            adv.appendChild(e);
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
