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

import java.io.File;

/**
 * This class wraps an existing Content in an ContentTransfer object.  All
 * operations performed on this object are a no-op, except for the getContent()
 * method which returns the Content with which the transfer instance was
 * instantiated with and addContentTransferListener() which immediately
 * notifies the attaching listener of transfer completion.
 */
public class NullContentTransfer implements ContentTransfer {
    /**
     * Wrapped Content.
     */
    private final Content content;
    
    /**
     * ContentProvider which created this transfer.
     */
    private final ContentProvider provider;

    /**
     * Create a no-op wrapper for the wrappedContent specified.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param wrappedContent local wrappedContent to wrap.
     */
    public NullContentTransfer(
            final ContentProvider origin, Content wrappedContent) {
        provider = origin;
        content = wrappedContent;
    }

    //////////////////////////////////////////////////////////////////////////
    // ContentTransfer interface methods:

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void addContentTransferListener(ContentTransferListener listener) {
        // Immediately notify of completion
        final ContentTransferEvent event =
                new ContentTransferEvent.Builder(this)
                    .locationCount(1)
                    .locationState(
                        ContentSourceLocationState.NOT_LOCATING_HAS_ENOUGH)
                    .transferState(ContentTransferState.COMPLETED)
                    .build();
        listener.contentTransferStateUpdated(event);
    }

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void removeContentTransferListener(ContentTransferListener listener) {
        // Empty
    }
    
    /**
     * {@inheritDoc}
     */
    public ContentProvider getContentProvider() {
        return provider;
    }

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void startSourceLocation() {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void stopSourceLocation() {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * Always NOT_LOCATING_HAS_MANY for this implementation.
     */
    public ContentSourceLocationState getSourceLocationState() {
        return ContentSourceLocationState.NOT_LOCATING_HAS_MANY;
    }

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void startTransfer(File destination) {
        /*
         * XXX 20070911 mcumings:  The ContentTransfer API states that this
         * file destination should be deleted if not needed.  We don't want
         * to delete here unless we know that the Content we are wrapping
         * is not a FileContent using this location as it's backing store.
         * We need to augment the FileDocument API to allow us to determine
         * what File the FileDocument is using so that we can safely delete
         * this destination file.
         */
    }

    /**
     * {@inheritDoc}
     *
     * Always COMPLETED for this implementation.
     */
    public ContentTransferState getTransferState() {
        return ContentTransferState.COMPLETED;
    }

    /**
     * {@inheritDoc}
     *
     * No-op for this implementation.
     */
    public void cancel() {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * Always returns immediately for this implementation.
     */
    public void waitFor() {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * Always returns immediately for this implementation.
     */
    public void waitFor(long timeout) {
        // Empty
    }

    /**
     * {@inheritDoc}
     *
     * @return the content used during construction of this object for this
     * implementation.
     */
    public Content getContent() {
        return content;
    }
    
}
