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
 * ContentTransfer objects represent the client / requesting side
 * of a Content transfer mechanism.  ContentTransfer instances are
 * obtained through the use of a ContentProvider's
 * <code>retrieveContent</code> method.  This interface defines the bare
 * minimum functionality to work with the client end of shared
 * Content, although it was designed to be extended by ContentProvider
 * implementations such that more detailed information and control
 * over the transfer can be achieved.
 * <p/>
 * This object may represent an intermediate layer between the application
 * and one or more content provider implementations.  This scenario occurs
 * when ContentService implementations create a wrapping layer around
 * multiple provider-specific ContentTransfer object instances.  In this
 * instance, the ContentTransferAggregator sub-interface should be used
 * by the ContentProvider creating the transfer object instance to
 * allow the end-user to maintain visibility through to the actual
 * ContentTransfer instances being utilized.
 * <p/>
 * An explicit effort was made to ensure that ContentTransfer instances
 * would always be directly accessible to the end-user.  This maintains
 * the end-user's ability to use operations such as instanceof to identify
 * a specific provider programatically, allowing a typecast or bean
 * introspection to provde them with in-depth tuning controls, etc..
 * <p/>
 * To enable failover, no access to the actual retrieved data should
 * be provided/accessible until the entire Content has been retrieved.
 * 
 * @see net.jxta.content.ContentShare
 * @see net.jxta.content.ContentTransferAggregator
 * @see net.jxta.content.ContentProvider
 * @see net.jxta.content.ContentService
 */
public interface ContentTransfer {

    /**
     * Adds an event listener to track the operation and progress of the
     * transfer.
     *
     * @param listener listener instance to add
     */
    void addContentTransferListener(ContentTransferListener listener);

    /**
     * Removes a previously registered event listener.
     *
     * @param listener listener instance to remove
     */
    void removeContentTransferListener(ContentTransferListener listener);

    /**
     * Get the provider which created and manages this transfer.
     * 
     * @return ContentProvider instance which originated this transfer
     *  object instance
     */
    ContentProvider getContentProvider();

    /**
     * Recommend or prompt the implementation to begin the process of locating
     * remote data sources.  This process is allowed to take place
     * independently of the transfer itself and can be used to help select
     * a specific transfer implementation to use when used in a transfer
     * aggregation.  As the source location process implementation is
     * entirely dependent on the transfer implementation this method is
     * treated as an advisory and is not required to be called prior to
     * calling <code>startTransfer</code>.
     */
    void startSourceLocation();

    /**
     * Recommend or prompt the implementation to cease the process of locating
     * remote data sources.  As the source location process implementation is
     * entirely dependent on the transfer implementation and as the cost of
     * source location may vary widely from provider to provider, this method
     * is treated as advisory and is not required to be called prior to
     * or after calling <code>startTransfer</code>.
     */
    void stopSourceLocation();

    /**
     * Returns the current state of this transfer's remote source location
     * activities.
     *
     * @return current source location state
     */
    ContentSourceLocationState getSourceLocationState();

    /**
     * Begins the process of finding and retrieving the Content, storing the
     * results into the File location specified on as as-needed basis,
     * overwriting any pre-existing data at this location.  The state
     * of the contents of the file path provided are to be considered by
     * the end-user to be undefined until the completion of the transfer.
     * At transfer completion, the file should only exist if the transfer
     * was successful and only if the ContentProvided needed to use the
     * file location to store data.  Some transfer implementations may opt
     * to not use the file location provided, in which case the ContentProvider
     * should ensure that the file is deleted.
     *
     * @param destination file location to write the resulting data into
     */
    void startTransfer(File destination);

    /**
     * Returns the current data transfer state.
     *
     * @return current transfer state
     */
    ContentTransferState getTransferState();

    /**
     * Cancels all remote data source location and Content transfer activity,
     * if still in progress.  After a transfer has been cancelled it can
     * no longer be used to locate sources or retrieve data.   If a transfer
     * has reached a terminal state (i.e., {@code transferState.isFinished()}
     * returns {@code true}) then this method should have no effect.
     */
    void cancel();

    /**
     * Wait for the completion of the transfer.
     *
     * @throws InterruptedException when the thread is interrupted.
     * @throws TransferException if there is an unrecoverable problem during
     *  the transfer
     * @throws TransferCancelledException if the cancel method is called
     *  or if the ContentProvider is shut down prior to transfer completion
     */
    void waitFor()
    throws InterruptedException, TransferException;

    /**
     * Wait for the completion of the transfer for a maximum of timeout
     * milliseconds.  If the timeout expires, the method will return
     * without raising an exception.
     *
     * @param timeout maximum amount of time to wait, in milliseconds
     * @throws InterruptedException when the thread is interrupted.
     * @throws TransferException if there is an unrecoverable problem during
     *  the transfer
     * @throws TransferCancelledException if the cancel method is called
     *  or if the ContentProvider is shut down prior to transfer completion
     */
    void waitFor(long timeout)
    throws InterruptedException, TransferException;

    /**
     * Blocks until the Content transfer has completed (or failed) and then
     * returns the completed Content.
     *
     * @throws InterruptedException when the thread is interrupted.
     * @throws TransferException if there is an unrecoverable problem during
     *  the transfer
     * @throws TransferCancelledException if the cancel method is called
     *  or if the ContentProvider is shut down prior to transfer completion
     */
    Content getContent()
    throws InterruptedException, TransferException;

}
