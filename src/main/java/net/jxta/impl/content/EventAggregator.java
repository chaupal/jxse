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

package net.jxta.impl.content;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentProviderEvent;
import net.jxta.content.ContentProviderListener;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentShare;

/**
 * Consolidates responses from multiple providers into a single response
 * for the end user.
 */
public class EventAggregator implements ContentProviderListener {

    /**
     * Number of records desired, or < 0 for infinite.
     */
    private int desired;

    /**
     * List of the providers who have not yet sent an end of record
     * event.
     */
    private final List<ContentProvider> desiredProviders =
            new CopyOnWriteArrayList<ContentProvider>();

    /**
     * The listener who should receive the consolidated events.
     */
    private List<ContentProviderListener> listeners;

    /**
     * List of all providers.
     */
    private List<ContentProviderSPI> providers;

    ///////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Creates a new instance.
     *
     * @param providerListeners recipients of consolidated events
     * @param contentProviders iterable list of providers
     */
    public EventAggregator(
            List<ContentProviderListener> providerListeners,
            List<ContentProviderSPI> contentProviders) {
        listeners= providerListeners;
        providers = contentProviders;
        for (ContentProviderSPI provider : contentProviders) {
            provider.addContentProviderListener(this);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ContentProviderListener interface methods:

    /**
     * Called when a Content instance is publicly shared.
     *
     * @param event content provider event object
     */
    public void contentShared(ContentProviderEvent event) {
        // Simply act as a proxy
        for (ContentProviderListener listener : listeners) {
            listener.contentShared(event);
        }
    }

    /**
     * Called when an existing ContentShare is no longer publicly shared.
     *
     * @param event content provider event object
     */
    public void contentUnshared(ContentProviderEvent event) {
        // Simply act as a proxy
        for (ContentProviderListener listener : listeners) {
            listener.contentUnshared(event);
        }
    }

    /**
     * Called when shares are found as the result of a call to the
     * ContentProvider's <code>findContentShares()</code> method, and only
     * called against the listener specified as an argument to the
     * method.
     *
     * @param event content provider event object
     * @return true if the listing request should continue, false to cancel
     *  this request and prevent further processing
     */
    public boolean contentSharesFound(ContentProviderEvent event) {
        ContentProvider provider = event.getContentProvider();
        ContentID id = event.getContentID();
        List<ContentShare> shares;
        Boolean isLastRecord = event.isLastRecord();
        boolean wantMore = true;
        boolean needsNewEvent = false;

        // Object already has its own synchronization mechanism (FindBugs)
//        synchronized(desiredProviders) {

            // Make sure the provider is one we are expecting more data from
            if (!desiredProviders.contains(provider)) {
                return false;
            }

            // Dont send more than requested
            if (desired <= 0) {
                return false;
            }

            // Trim the results if necessary
            shares = event.getContentShares();
            if (desired > 0 && shares.size() > desired) {
                List<ContentShare> newShares =
                        new ArrayList<ContentShare>();
                newShares.addAll(shares.subList(0, desired));
                shares = newShares;
                needsNewEvent = true;
            }

            // Check to see if we've reached the desired max
            if (desired >= 0) {
                desired -= shares.size();
                if (desired <= 0) {
                    wantMore = false;
                }
            }

            // Or if we've reached the end of the result set for this provider
            if (isLastRecord) {
                desiredProviders.remove(provider);
                if (desiredProviders.size() == 1) {
                    // This is our last aggregated event, too
                    wantMore = false;
                    needsNewEvent = true;
                }
            } else if (!wantMore) {
                // Make this our last aggregated event
                isLastRecord = true;
                needsNewEvent = true;
            }
//        }

        ContentProviderEvent aggEvent;
        if (needsNewEvent) {
            aggEvent = new ContentProviderEvent.Builder(provider, shares)
                    .contentID(id)
                    .lastRecord(isLastRecord)
                    .build();
        } else {
            aggEvent = event;
        }

        for (ContentProviderListener listener : listeners) {
            listener.contentSharesFound(aggEvent);
        }

        return wantMore;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods:

    /**
     * Initiates a <code>findContentShares</code> call on each of the providers.
     *
     * @param maxNum maximum number of result shares to return
     */
    public void dispatchFindRequest(int maxNum) {

        // Object already has its own synchronization mechanism (FindBugs)
//        synchronized(desiredProviders) {
            desired = maxNum;
            desiredProviders.clear();
            desiredProviders.addAll(providers);
//        }

        for (ContentProvider provider : desiredProviders) {
            provider.findContentShares(maxNum, this);
        }
    }

}