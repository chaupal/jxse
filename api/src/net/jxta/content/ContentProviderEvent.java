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

package net.jxta.content;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Iterator;
import net.jxta.protocol.ContentShareAdvertisement;

/**
 * ContentProvider event object containing information related to the event
 * being published.
 */
public class ContentProviderEvent extends EventObject {

    /**
     * Serialized version.
     */
    private static final long serialVersionUID = 2009110500L;

    /**
     * Content ID.
     */
    private final ContentID contentID;

    /**
     * List of ContentShare objects.
     */
    private final List<ContentShare> contentShares;

    /**
     * Flag indicating that this event signifies the last record of a
     * series of events.
     */
    private final Boolean amLastRecord;

    /**
     * Builder pattern.
     */
    public static class Builder {
        // Required parameters:
        private final ContentProvider bSource;

        // Optional parameters:
        private ContentID bContentID;
        private List<ContentShare> bShares;
        private Boolean bLastRecord;

        /**
         * Constructs a new builder, used to create and initialize the
         * event instance.
         *
         * @param provider ContentProvider issueing this event
         * @param contentID id of the Content which this event pertains to
         */
        public Builder(
                final ContentProvider provider,
                final ContentID contentID) {
            if (provider == null) {
                throw new IllegalArgumentException("provider argument cannot be null");
            }
            if (contentID == null) {
                throw new IllegalArgumentException("contentID argument cannot be null");
            }
            bSource = provider;
            bContentID = contentID;
        }

        /**
         * Constructs a new builder, used to create and initialize the
         * event instance.
         *
         * @param provider ContentProvider issueing this event
         * @param shareList list of ContentShares
         */
        public Builder(
                final ContentProvider provider,
                final List<ContentShare> shareList) {
            if (provider == null) {
                throw new IllegalArgumentException("provider argument cannot be null");
            }
            if (shareList == null) {
                throw new IllegalArgumentException("shareList argument cannot be null");
            }
            bSource = provider;
            bShares = Collections.unmodifiableList(shareList);
        }

        /**
         * Sets the ContentID which this event pertains to.
         *
         * @param id of the Content which this event pertains to
         * @return builder instance
         */
        public Builder contentID(ContentID id) {
            bContentID = id;
            return this;
        }

        /**
         * Sets the flag indicating that this is the last event in a
         * series of events from this provider source.
         *
         * @param flag {@code true} if this is the last event in the series,
         *  {@code false} otherwise
         * @return builder instance
         */
        public Builder lastRecord(boolean flag) {
            bLastRecord = Boolean.valueOf(flag);
            return this;
        }


        /**
         * Build the event object.
         *
         * @return event instance
         */
        public ContentProviderEvent build() {
            if (bContentID == null && bShares != null) {
                bContentID = probeContentID();
            }
            return new ContentProviderEvent(this);
        }

        /**
         * Probes to obtain a ContentID from the list of shares provided.
         * Will be successful if all advs define the same CotnentID.
         *
         * @return probed ContentID value, or {@code null} if probe failed
         */
        private ContentID probeContentID() {
            if (bShares == null) {
                return null;
            }
            ContentID newID = null;
            final Iterator<ContentShare> iter = bShares.iterator();
            while (iter.hasNext()) {
                final ContentShareAdvertisement adv =
                        iter.next().getContentShareAdvertisement();
                if (newID == null) {
                    // First one.  Set it using the adv's ID.
                    if (adv != null) {
                        newID = adv.getContentID();
                    }
                } else if (adv == null
                            || adv.getContentID() != null
                            || (!newID.equals(adv.getContentID()))) {
                    // We found one that differs. We can't probe.
                    return null;
                }
            }
            return newID;
        }
    }

    /**
     * Create a new instance of ContentProviderEvent.
     *
     * @param Builder builder with our data
     */
    private ContentProviderEvent(Builder builder) {
        super(builder.bSource);
        contentID = builder.bContentID;
        contentShares = builder.bShares;
        amLastRecord = builder.bLastRecord;
    }

    /**
     * Get the ContentProvider which produced this event.
     *
     * @return the originator of this event
     */
    public ContentProvider getContentProvider() {
        return (ContentProvider) getSource();
    }

    /**
     * Get the ContentID of the Content to which this event pertains, if
     * applicable.  Note that this is only used when the event itself
     * deals only with one particular Content.
     *
     * @return Content ID
     */
    public ContentID getContentID() {
        return contentID;
    }

    /**
     * Get the list of ContentShare objects associated with this event,
     * if applicable.
     *
     * @return list of content shares
     */
    public List<ContentShare> getContentShares() {
        return contentShares;
    }

    /**
     * Gets the flag indicating whether or not this event is the last in a
     * series of events from this provider.
     *
     * @return true if it is the last record, false if it is not, or null if
     *  not specified
     */
    public Boolean isLastRecord() {
        return amLastRecord;
    }

}
