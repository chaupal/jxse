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

import java.util.List;
import net.jxta.protocol.ContentShareAdvertisement;

/**
 * ContentProvider implementations define a single methodology
 * and/or protocol to obtain content from a (potentially) remote
 * data source.  This interface describes the publicly accessible
 * portion of the larger ContentProviderSPI interface and is meant
 * for direct use by the end user, possibly (and more desirably)
 * through the ContentService layer.
 * <p/>
 * If you are a user interested in the general functionality provided by
 * this package, you will more likely be interested in using the higher-
 * level ContentService interface rather than directly using a single
 * ContentProvider.
 * <p/>
 * Why the separation between the ContentProvider and ContentProviderSPI
 * interfaces?  This was done to allow a ContentService implementation
 * to maintain a collection of ContentProviderSPI objects for it's own
 * use and allow a restricting typecast of this list to be returned
 * in the ContentService.getContentProviders() interface method, without
 * needlessly exposing lifecycle methods to end users.
 * <p/>
 * An explicit effort was made to ensure that ContentProvider instances
 * would always be accessible to the end-user.  This maintains the
 * ability to use operations such as instanceof to identify
 * a specific provider programatically, allowing a typecast or bean
 * introspection to provide in-depth provider-specific tuning
 * controls, etc..
 * <p/>
 * You'll note some methods are allowed to be unsupported.  This was done
 * for the following reason(s):
 * <ul>
 *  <li>
 *      Some providers may be based on an underlying data store
 *      such as an existing read-only filesystem.  In such a scenario,
 *      explicitly sharing or unsharing Content instances may not
 *      always make sense.
 *  </li>
 * </ul>
 * <p/>
 * The findContentShares method was created in such a way as to allow
 * asynchronous results to be returned to the requestor.  This was
 * done for three primary reasons:
 * <ol>
 *  <li>
 *      If the ContentProvider fronted a huge filesystem, returning the
 *      results in a single chunk (array, List, etc.) could easily become
 *      limited by available memory.
 *  </li>
 *  <li>
 *      Returning partial sets more often (as opposed to a single set
 *      upon completion) allows applications to remain responsive for
 *      large result sets.
 *  </li>
 *  <li>
 *      It allows for large operations to be asynchronously cancelled.
 *  </li>
 * </ol>
 *
 * 
 * @see net.jxta.content.ContentProviderSPI
 * @see net.jxta.content.ContentService
 */
public interface ContentProvider {

    /**
     * Adds a provider listener.
     *
     * @param listener interested party.
     */
    void addContentProviderListener(ContentProviderListener listener);

    /**
     * Removes a provider listener.
     *
     * @param listener uninterested party.
     */
    void removeContentProviderListener(ContentProviderListener listener);

    /**
     * Retrieve the advertised Content if the provider supports automatic
     * (or implicit) discovery of remote shares.
     *
     * @param contentID ContentID Unique ID of the Content to attempt to
     *  retrieve.
     * @return ContentTransfer object representing a potential future transfer
     *  of the Content or <tt>null</tt> if this provider knows immediately
     *  and up-front that it cannot handle the advertisement provided.
     */
    ContentTransfer retrieveContent(ContentID contentID)
            throws UnsupportedOperationException;

    /**
     * Retrieve the advertised Content if the share advertisement can be
     * understood or (optionally) if the ContentAdvertisement embedded in
     * the share advertisement can be used to automatically discover
     * remote shares.
     * <p/>
     * Provider implementations should use the ContentAdvertisement contained
     * within the share advertisement if it can be understood.  If it
     * cannot be understood, or if the provider implementations detects a
     * failure to retrieve the Content, the provider may attempt to locate
     * alternative data sources automatically.
     *
     * @param adv ContentAdvertisement of the Content to attempt to retrieve.
     * @return ContentTransfer object representing a potential future transfer
     *  of the Content or <tt>null</tt> if this provider knows immediately
     *  and up-front that it cannot handle the advertisement provided.
     */
    ContentTransfer retrieveContent(ContentShareAdvertisement adv);

    /**
     * Serve the provided Content to remote requestors.  The Content will be
     * served to all requestors until either the JXTA platform exits or until
     * this Content is explicitly unshared.
     *
     * @param content Content to share publicly.
     * @return list of all share tracking objects, or <tt>null</tt> if this
     *      Content can not be shared by the provider for some reason
     * @throws UnsupportedOperationException if the provider
     *     does not support explicit share addition.
     */
    List<ContentShare> shareContent(Content content)
    throws UnsupportedOperationException;

    /**
     * If the specified ContentID has been previously shared this method
     * disposes of any references to the Content and no longer serves any
     * additional requests for the Content in question.
     *
     * @param contentID ID of the Content to no longer share.
     * @return true if a previously shared object was unshared, false otherwise.
     * @throws UnsupportedOperationException if the provider
     *     does not support explicit share removal.
     */
    boolean unshareContent(ContentID contentID)
    throws UnsupportedOperationException;

    /**
     * Collects a list of all ContentShares being served by this
     * ContentProvicer instance, notifying the provided ContentListener as
     * they are discovered by calling the listener's
     * <code>contentSharesFound</code> method.  Only ContentShare objects
     * being shared by this service instance are returned - no remote
     * discovery is performed.
     * <p/>
     * Implementations may choose whether to respond immediately by
     * notifying the listener prior to the call to findContentShares returns,
     * or optionally by scheduling the lookup operation for deferred background
     * processing.  The caller should not make any assumptions as to which of
     * the two approaches may be used.
     *
     * @param maxNum maximum number of shares to return,
     *  or &lt; 0 for unlimited
     * @param listener listener to notify when shares are found
     * @throws UnsupportedOperationException if the underlying provider
     *     mechanism does not support share enumeration.
     */
    void findContentShares(
            int maxNum, ContentProviderListener listener)
            throws UnsupportedOperationException;

}
