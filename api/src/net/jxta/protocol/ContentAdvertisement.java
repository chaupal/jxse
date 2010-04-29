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
import net.jxta.content.ContentID;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;

/**
 * A ContentAdvertisement describes a Content object.
 * Its main purpose is to formally document the existence of a Content
 * instance on the network.
 *
 * @see net.jxta.content.Content
 * @see net.jxta.content.ContentID
 */
public abstract class ContentAdvertisement
        extends ExtendableAdvertisement
        implements Cloneable {
    private ContentID id;
    private ContentID meta;
    private MimeMediaType mimeType;

    private transient ID hashID;

    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     */
    public static String getAdvertisementType() {
        return "jxta:Content";
    }

    /**
     * {@inheritDoc}
     */
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }

    /**
     * Clone this ContentAdvertisement.
     *
     * @return a copy of this ContentAdvertisement
     */
    @Override
    public ContentAdvertisement clone() throws CloneNotSupportedException {

        // All members are either immutable or never modified nor allowed to
        // be modified: all accessors return clones.
        ContentAdvertisement clone = (ContentAdvertisement) super.clone();
        clone.setContentID(getContentID());
        clone.setMetaID(getMetaID());
        clone.setMimeType(getMimeType());
        return clone;

    }

    /**
     * Returns the ID of the Content.
     *
     * @return the content ID
     */
    public ContentID getContentID() {
        return id;
    }

    /**
     * Sets the ID of the Content.
     *
     * @param id ID of the Content
     */
    public void setContentID(ContentID id) {
        this.id = id;
    }

    /**
     * Returns a unique ID for that advertisement (for indexing purposes).
     * The ContentID uniquely identifies this adv.
     *
     * @return the Content ID as a basic ID.
     */
    public ID getID() {
        if (hashID == null) {
            if (id == null) {
                throw new IllegalStateException("cannot build ID: no ContentID");
            }

            try {
                // We have not yet built it. Do it now
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
     * Returns the id of the Content for which this Content is meta data.
     *
     * @return the metadata Content ID
     */
    public ContentID getMetaID() {
        return meta;
    }

    /**
     * Sets the id of the Content for which this Content is metadata.
     *
     * @param id The id of the Content
     */
    public void setMetaID(ContentID id) {
        this.meta = id;
    }

    /**
     * Returns the MIME media type for the Content data.
     *
     * @return MimeMediaType the MIME type of the Content's data.
     */
    public MimeMediaType getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME media type of the Content data.
     *
     * @param type The MIME type of the data.
     */
    public void setMimeType(MimeMediaType type) {
        this.mimeType = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObj) {
        if (otherObj instanceof ContentAdvertisement) {
            ContentAdvertisement otherAdv = (ContentAdvertisement) otherObj;
            ID otherIdxID = otherAdv.getID();
            return getID().equals(otherIdxID);
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
