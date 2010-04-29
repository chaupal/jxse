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

package net.jxta.protocol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import net.jxta.content.ContentID;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroupID;

/**
 * A ContentShareAdvertisement describes a ContentAdvertisement
 * which is being shared by a specific provider implementation.
 * It will always include a copy of the ContentAdvertisement of
 * the Content being shared and will typically be extended by
 * provider implementations to add information relating to how
 * to contact the provider to obtain the content being advertised.
 *
 * @see net.jxta.content.Content
 * @see net.jxta.protocol.ContentAdvertisement
 */
public abstract class ContentShareAdvertisement
        extends ExtendableAdvertisement
        implements Cloneable {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(ContentShareAdvertisement.class.getName());

    private ContentAdvertisement contentAdv;

    private transient ID hashID;

    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     */
    public static String getAdvertisementType() {
        return "jxta:ContentShare";
    }

    /**
     * {@inheritDoc}
     */
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }

    /**
     * Clone this ContentShareAdvertisement.
     *
     * @return a copy of this ContentShareAdvertisement
     */
    @Override
    public ContentShareAdvertisement clone() {
        
        try {
            // All members are either immutable or never modified nor allowed to
            // be modified: all accessors return clones.
            ContentShareAdvertisement clone = (ContentShareAdvertisement) super.clone();
            clone.setContentAdvertisement(getContentAdvertisement());
            return clone;
        } catch (CloneNotSupportedException ex) {
            Logging.logCheckedSevere(LOG, ex.toString());
            return null;
        }

    }

    /**
     * Returns a unique ID for that advertisement (for indexing purposes).
     * ContentShareAdvertisements will by default return an ID based on
     * the combination of the base adtertisement type, the advertisement
     * type, and the ContentID being advertised.  This allows multiple
     * ContentProviders instances to create unique advertisements that
     * can coexist along side each other while preventing multiple
     * variants of the same ContentID from the same provider.  Some
     * providers may want to override this method to return
     * <code>null</code>, indicating that an ID based on a hash of the
     * entire document should be used, insted.
     *
     * @return ContentID 
     */
    public ID getID() {
        if (hashID == null) {
            if (contentAdv == null) {
                throw new IllegalStateException(
                        "cannot build ID: no ContentAdvertisement");
            }

            try {
                // We have not yet built it. Do it now
                ContentID id = contentAdv.getContentID();
                StringBuilder builder = new StringBuilder();
                builder.append(getBaseAdvType());
                builder.append("_");
                builder.append(getAdvType());
                builder.append("_");
                builder.append(id.toString());
                String seed = builder.toString();

                InputStream in = new ByteArrayInputStream(seed.getBytes());
                hashID = IDFactory.newContentID(
                        (PeerGroupID) id.getPeerGroupID(),
                        id.isStatic(), seed.getBytes(), in);
            } catch (Throwable thr) {
                throw new IllegalStateException("cannot build ID", thr);
            }
        }
        return hashID;
    }

    /**
     * Gets the ID of the Content object being advertised by this document.
     * 
     * @return ContentID for this advertisement
     */
    public ContentID getContentID() {
        if (contentAdv == null) {
            return null;
        } else {
            return contentAdv.getContentID();
        }
    }

    /**
     * Returns the ContentAdvertisement which this share represents.
     *
     * @return the ContentAdvertisement of the Content being shared
     */
    public ContentAdvertisement getContentAdvertisement() {
        return contentAdv;
    }

    /**
     * Sets the ContentAdvertisment which this share is representing.
     *
     * @param adv the ContentAdvertisement of the Content being shared
     */
    public void setContentAdvertisement(ContentAdvertisement adv) {
        contentAdv = adv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObj) {
        if (otherObj instanceof ContentShareAdvertisement) {
            ContentShareAdvertisement otherSAdv =
                    (ContentShareAdvertisement) otherObj;
            return otherSAdv.getID().equals(getID());
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }

}
