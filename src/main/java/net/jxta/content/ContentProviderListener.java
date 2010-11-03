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

import java.util.EventListener;

/**
 * Interface provided to notify interested parties of ContentProvider
 * events which may be useful to them.  This can be used by the 
 * application to centralize monitoring hooks, allowing the application
 * to (for example) maintain asynchronously updated lists of current
 * shares and/or automatically perform remote publishing of new
 * advertisements.
 * <p/>
 * Implementors should take note that even if a listener is added to a
 * single provider, events may arrive from multiple providers.  This is
 * due to the fact that some providers (the one to which the listener was
 * added in this scenario) may be aggregations of multiple underlying
 * provider implementations, as is the case with the ContentService
 * implementation.  Unless the event source is explicitly checked the
 * consumer should not make any assumptions relating to the arrival order
 * of the events, etc..
 */
public interface ContentProviderListener extends EventListener {

    /**
     * Called when Content is publicly shared by a provider.
     *
     * @param event content provider event object
     */
    void contentShared(ContentProviderEvent event);

    /**
     * Called when an existing ContentShare is no longer publicly shared
     * by a provider.
     *
     * @param event content provider event object
     */
    void contentUnshared(ContentProviderEvent event);

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
    boolean contentSharesFound(ContentProviderEvent event);

}
