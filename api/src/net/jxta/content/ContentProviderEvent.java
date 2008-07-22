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
     * Content ID.
     */
    private ContentID contentID;

    /**
     * List of ContentShare objects.
     */
    private List<ContentShare> contentShares;

    /**
     * Flag indicating that this event signifies the last record of a
     * series of events.
     */
    private Boolean amLastRecord;
    
    /**
     * Flag indicating whether or not a ContentID has been calculated.
     * This is only used when no ContentID is provided in the constructor
     * and getContentID() is called.
     */
    private transient boolean contentIDSet = false;

    /**
     * Creates a new instance of ContentProviderEvent.  This form is typically
     * used in creating content unshared events.
     *
     * @param source ContentProvider issueing this event
     * @param id of the Content which this event pertains to
     */
    public ContentProviderEvent(
            ContentProvider source, ContentID id) {
        this(source, id, null, null);
    }

    /**
     * Creates a new instance of ContentProviderEvent.  This form is typically
     * used in creating content shared events.
     *
     * @param source ContentProvider issueing this event
     * @param shareList list of ContentShares
     */
    public ContentProviderEvent(
            ContentProvider source, List<ContentShare> shareList) {
        this(source, null, shareList, null);
    }

    /**
     * Creates a new instance of ContentProviderEvent.  This is the full form
     * of the constructor and is typically used to support the
     * <code>findCodatShares</code> capability.
     *
     * @param source ContentProvider issueing this event
     * @param id ContentID of the Content which this event pertains to
     * @param shareList list of ContentShares
     * @param lastRecord flag indicating that this is the last event in a
     *  series of events from this provider source.
     */
    public ContentProviderEvent(
            ContentProvider source, ContentID id,
            List<ContentShare> shareList, Boolean lastRecord) {
        super(source);
        contentID = id;
        contentShares = shareList;
        amLastRecord = lastRecord;
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
         /*
         * Attempt to lazily populate ContentID field if all the
         * ContentShare instances share the same value.
         */
        if (contentIDSet == false && contentID == null) {
            if (contentShares != null) {
                ContentID newID = null;
                Iterator<ContentShare> iter = contentShares.iterator();
                while (iter.hasNext()) {
                    ContentShareAdvertisement adv =
                            iter.next().getContentShareAdvertisement();
                    if (contentIDSet) {
                        if ((newID == null
                                && (adv == null || adv.getContentID() != null))
                            || (!newID.equals(adv.getContentID()))) {
                            // We found one that differs. Unset things.
                            newID = null;
                            break;
                        }
                    } else {
                        // First one.  Set it using the adv's ID.
                        if (adv != null) {
                            newID = adv.getContentID();
                        }
                        contentIDSet = true;
                    }
                }
                contentID = newID;
            }
        }
        
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
