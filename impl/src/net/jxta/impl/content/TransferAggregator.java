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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentSourceLocationState;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentTransferAggregator;
import net.jxta.content.ContentTransferAggregatorListener;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferException;
import net.jxta.content.ContentTransferAggregatorEvent;
import net.jxta.content.TransferCancelledException;
import net.jxta.protocol.ContentShareAdvertisement;
import static net.jxta.content.ContentSourceLocationState.*;
import static net.jxta.content.ContentTransferState.*;

/**
 * Provides multi-provider content transfer capabilities.  Specifically, this
 * class will randomly select one of the configured content providers
 * and attempt to use it to complete the requested transfer.  If the provider
 * is unable to retrieve the remote Content, another provider will be selected
 * at random to take it's place.  This process will continue until the Content
 * is either successfully retrieved or no providers remain.
 */
public class TransferAggregator
        implements ContentTransferAggregator, ContentTransferListener {
    /**
     * Log4J logger.
     */
    private static final Logger LOG = Logger.getLogger(
            TransferAggregator.class.getName());

    /**
     * The number of transfer instances that will be held "in standby",
     * meaning that active remote source location will continue until they
     * have "enough" remote sources.
     */
    private static final int standbyCount =
            Integer.getInteger(TransferAggregator.class.getName()
            + ".standbyCount", 2).intValue();

    /**
     * ContentTransferListeners.
     */
    private final List<ContentTransferListener> ctListeners =
            new CopyOnWriteArrayList<ContentTransferListener>();

    /**
     * ContentTransferAggregatorListeners.
     */
    private final List<ContentTransferAggregatorListener> ctaListeners =
            new CopyOnWriteArrayList<ContentTransferAggregatorListener>();
    
    /**
     * ContentProvider which created and manages this transfer.
     */
    private final ContentProvider provider;

    /**
     * List of providers left to select from.
     */
    private List<ContentTransfer> idle = new ArrayList<ContentTransfer>();

    /**
     * List of providers left to select from.
     */
    private List<ContentTransfer> standby = new ArrayList<ContentTransfer>();

    /**
     * Currently selected transfer.
     */
    private ContentTransfer selected;

    /**
     * Destination file of the resulting transfer.
     */
    private File destFile;

    /**
     * Resulting Content.
     */
    private Content content;

    /**
     * Current source location state.
     */
    private ContentSourceLocationState locationState = NOT_LOCATING;

    /**
     * Current transfer state.
     */
    private ContentTransferState transferState = PENDING;

    /**
     * Captured exception describing the cause of the failure.
     */
    private TransferException failureCause;

    /**
     * Creates a new tansfer with the specified providers.  Provider list
     * given is a private list that we can modify at will.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param providers list of providers
     * @param adv ContentShareAdvertisement
     * @throws TransferException if no providers can even attempt to retrieve
     *  the content in question
     */
    public TransferAggregator(
            ContentProvider origin,
            List<ContentProviderSPI> providers,
            ContentShareAdvertisement adv)
            throws TransferException {
        this(origin, providers, (Object) adv);
    }
    
    /**
     * Creates a new tansfer with the specified providers.  Provider list
     * given is a private list that we can modify at will.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param providers list of providers
     * @param contentID ID of the Content we would like to retrieve
     * @throws TransferException if no providers can even attempt to retrieve
     *  the content in question
     */
    public TransferAggregator(
            ContentProvider origin,
            List<ContentProviderSPI> providers,
            ContentID contentID)
            throws TransferException {
        this(origin, providers, (Object) contentID);
    }
    
    /**
     * Private constructor used to consolidate code which would be repeated
     * in the available public constructors.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param providers list of providers
     * @param adv ContentShareAdvertisement or ContentID object
     * @throws TransferException if no providers can even attempt to retrieve
     *  the content in question
     */
    private TransferAggregator(
            ContentProvider origin,
            List<ContentProviderSPI> providers,
            Object obj)
            throws TransferException {
        provider = origin;
        ContentTransfer transfer;
        for (ContentProvider prov : providers) {
            try {
                // null test is for mock object testing
                if (obj == null || obj instanceof ContentShareAdvertisement) {
                    transfer = prov.retrieveContent(
                            (ContentShareAdvertisement) obj);
                } else {
                    transfer = prov.retrieveContent(
                            (ContentID) obj);
                }
                if (transfer == null) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Provider returned null transfer: " + prov);
                    }
                } else {
                    if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Provider '" + prov + "' returned transfer: "
                                + transfer);
                    }
                    idle.add(transfer);
                    transfer.addContentTransferListener(this);
                }
            } catch (UnsupportedOperationException unsupx) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Provider does not support operation: " + prov);
                }
            }
        }

        if (idle.size() == 0) {
            throw(new TransferException(
                    "No transfer providers are able to retrieve this Content"));
        }

        // Randomize the list, effectively randomizing which impl(s) we use
        Collections.shuffle(idle);
    }


    ///////////////////////////////////////////////////////////////////////////
    // ContentTransfer methods:

    /**
     * {@inheritDoc}
     */
    public void addContentTransferListener(ContentTransferListener listener) {
        ctListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentTransferListener(ContentTransferListener listener) {
        ctListeners.remove(listener);
    }
    
    /**
     * {@inheritDoc}
     */
    public ContentProvider getContentProvider() {
        return provider;
    }

    /**
     * {@inheritDoc}
     */
    public void startSourceLocation() {
        // Map to our next state or early out if we can
        synchronized(this) {
            if (transferState.isSuccessful() || locationState.isLocating()) {
                // Nothing to do
                return;
            }
            locationState = locationState.getEquivalent(true);
        }

        // Make sure we load up the selected and standby transfers
        try {
            populateSelected();
            populateStandby();
        } catch (TransferException transx) {
            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Ignoring exception", transx);
            }
        }

        // This indirectly provides event notification
        checkSelectedAndStandby();
    }

    /**
     * {@inheritDoc}
     */
    public void stopSourceLocation() {
        synchronized(this) {
            if (!locationState.isLocating()) {
                // Nothing to do
                return;
            }
            locationState = locationState.getEquivalent(false);
            checkSelectedAndStandby();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentSourceLocationState getSourceLocationState() {
        return locationState;
    }

    /**
     * {@inheritDoc}
     */
    public void startTransfer(File destination) {
        if (destination == null) {
            throw(new IllegalArgumentException(
                    "Destination cannot be null"));
        } else if (destFile != null) {
            throw(new IllegalArgumentException(
                    "This transfer has already been started"));
        }

        startSourceLocation();
        synchronized(this) {
            destFile = destination;
            try {
                checkTransferState();
            } catch (TransferException transx) {
                notifyAll();
            }
        }
        // After this, we rely on events to keep track of status.
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransferState getTransferState() {
        return transferState;
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        synchronized(this) {
            if (transferState.isFinished()) {
                // Cancelling now has no effect.
                return;
            }

            transferState = CANCELLED;
            notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void waitFor()
    throws InterruptedException, TransferException {
        waitFor(0);
    }

    /**
     * {@inheritDoc}
     */
    public void waitFor(long timeout)
    throws InterruptedException, TransferException {
        long exitTime = System.currentTimeMillis() + timeout;
        long remaining = timeout;
        synchronized(this) {
            checkTransferState();
            while (!transferState.isFinished()) {
                wait(remaining);
                if (timeout > 0) {
                    remaining = exitTime - System.currentTimeMillis();
                    if (remaining <= 0) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Content getContent() throws TransferException {
        checkTransferState();
        do {
            try {
                waitFor();
                return content;
            } catch (InterruptedException intx) {
                // Ignore
            }
        } while (true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ContentTransferAggregator methods:

    /**
     * {@inheritDoc}
     */
    public void addContentTransferAggregatorListener(
            ContentTransferAggregatorListener listener) {
        ctaListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentTransferAggregatorListener(
            ContentTransferAggregatorListener listener) {
        ctaListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransfer getCurrentContentTransfer() {
        synchronized(this) {
            return selected;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ContentTransfer> getContentTransferList() {
        List<ContentTransfer> result = new ArrayList<ContentTransfer>();
        synchronized(this) {
            if (selected != null) {
                result.add(selected);
            }
            result.addAll(standby);
            result.addAll(idle);
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ContentTransferListener methods:

    /**
     * {@inheritDoc}
     */
    public void contentLocationStateUpdated(ContentTransferEvent event) {
        ContentTransfer transfer = event.getContentTransfer();
        ContentTransferEvent toFire = null;

        synchronized(this) {
            // Update our state if transfer is selected, but manage the location
            if (transfer == selected) {
                ContentSourceLocationState state =
                        event.getSourceLocationState();
                ContentSourceLocationState oldState = locationState;

                locationState =
                        state.getEquivalent(locationState.isLocating());
                Integer locationCount = event.getSourceLocationCount();

                // Try to send events only when useful
                if (oldState != locationState || locationCount != null) {
                    if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Location update (location count: "
                                + locationCount + ")");
                        LOG.finer("    Was  : " + oldState);
                        LOG.finer("    Is   : " + locationState);
                        LOG.finer("    Cause: " + state);
                    }
                    toFire = new ContentTransferEvent(
                            this, locationCount, locationState, null);
                }
            }
        }

        // Fire first, then check to see if we need to change states
        if (toFire != null) {
            fireLocationStateUpdated(toFire);
        }
        checkLocationState(transfer);
    }

    /**
     * {@inheritDoc}
     */
    public void contentTransferStateUpdated(ContentTransferEvent event) {
        contentTransferStateUpdated(
                event.getContentTransfer(), event.getTransferState());
    }

    /**
     * This is our internal version which allows <code>populateSelected</code>
     * to execute this functionality without creating a new event object.
     * This allows us to notify of transfer state differences when selecting
     * new transfer instances.
     */
    private void contentTransferStateUpdated(
            ContentTransfer transfer, ContentTransferState state) {
        ContentTransferEvent toFire = null;
        TransferException exception;
        boolean terminateTransfer = false;
        boolean doTransferStart = false;

        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Transfer state updated: " + transfer + " (" + state + ")");
        }

        if (state.isSuccessful()) {
            /* NOTE mcumings 20061229: This is a bit odd in that
             * theoretically an idle transfer can declare that it is
             * completed, bypassing the whole of the randomization process.
             * This is probably acceptable and desirable though, since
             * Content which is already available locally will have close
             * to zero retrieval overhead as long as providers check
             * their local sources prior to looking for remote sources.
             */

            // make sure it isn't lying
            try {
                content = transfer.getContent();

                stopSourceLocation();
                cancelAll(transfer, true);
                synchronized(this) {
                    transferState = COMPLETED;
                    toFire = new ContentTransferEvent(
                            this, null, locationState, transferState);
                    notifyAll();
                }
            } catch (InterruptedException intx) {
                // Bad timing?
                terminateTransfer = true;
            } catch (TransferException transx) {
                // LIAR!
                terminateTransfer = true;
            }
        } else if (state.isFinished()) {
            try {
                // Dummy call to gain access to exception
                while(true) {
                    try {
                        transfer.getContent();
                        break;
                    } catch (InterruptedException inx) {
                        Thread.interrupted();
                        // Ignore and retry
                    }
                }
            } catch (TransferException transx) {
                exception = transx;
            }
            terminateTransfer = true;
        } else {
            synchronized(this) {
                if (transfer == selected && transferState != state) {
                    if (destFile != null && !state.isRetrieving()) {
                        doTransferStart = true;
                    }
                    transferState = state;
                    toFire = new ContentTransferEvent(
                            this, null, locationState, transferState);
                }
            }
        }

        if (terminateTransfer) {
            transfer.cancel();
            synchronized(this) {
                if (selected == transfer) {
                    selected = null;
                }
                if (standby.contains(transfer)) {
                    standby.remove(transfer);
                }
                if (idle.contains(transfer)) {
                    idle.remove(transfer);
                }
            }
            try {
                populateSelected();
                populateStandby();
                checkSelectedAndStandby();
            } catch (TransferException transx) {
                synchronized(this) {
                    failureCause = transx;
                    transferState = FAILED;
                    toFire = new ContentTransferEvent(
                            this, null, locationState, transferState);
                    notifyAll();
                }
            }
        }

        if (toFire != null) {
            fireTransferStateUpdated(toFire);
        }

        if (doTransferStart) {
            transfer.startTransfer(destFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void contentTransferProgress(ContentTransferEvent event) {
        ContentTransfer source = event.getContentTransfer();

        // Only notify listeners of progress of the selected transfer
        synchronized(this) {
            if (source != selected) {
                return;
            }
        }
        for (ContentTransferListener listener : ctListeners) {
            listener.contentTransferProgress(event);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Checks to make sure that there is a selected content transfer node,
     * backfilling the standby queue if need be.  If there is no selected
     * transfer node and there are no transfers in the standby or idle
     * queues, an exception is thrown.  Upon new selected node promotion,
     * notifies all CTA listeners.
     *
     * @throws TransferException when no transfers are remaining to become the
     *  selected transfer
     */
    private void populateSelected() throws TransferException {
        ContentTransfer newSelected = null;
        ContentTransferState newState = null;
        boolean selectNext;

        synchronized(this) {
            if (selected == null) {
                selectNext = true;
            } else if (selected.getTransferState().isSuccessful()) {
                selectNext = false;
            } else if (selected.getTransferState().isFinished()) {
                selectNext = true;
            } else {
                selectNext = false;
            }

            if (selectNext) {
                if (standby.size() == 0) {
                    if (idle.size() == 0) {
                        // Failed.
                        transferState = FAILED;
                        throw(new TransferException(
                                "No transfer providers remain"));
                    } else {
                        selected = idle.remove(0);
                    }
                } else {
                    selected = standby.remove(0);
                }

                // Backfill as needed
                populateStandby();

                newSelected = selected;
                newState = newSelected.getTransferState();
            }
        }

        if (newSelected != null) {
            fireSelectedContentTransfer(newSelected);
            contentTransferStateUpdated(newSelected, newState);
        }
    }

    /**
     * Ensures that the configured number of transfer instances are held
     * in the standby queue, if available.
     */
    private void populateStandby() {
        // Populate the standby list
        synchronized(this) {
            while (standby.size() < standbyCount) {
                if (idle.size() == 0) {
                    // No more to add
                    break;
                }
                standby.add(idle.remove(0));
            }
        }
    }

    /**
     * Checks the source location state of the specified transfer instance,
     * starting or stopping source location as deemed appropriate.  If the
     * transfer instance is the selected node, source location proceeds until
     * "many" sources have been identified.  If the transfer instance is on
     * the standby queue, source location proceeds until "enough" sources
     * have been identified.  In all cases, if source location has been
     * programatically requested to stop, source location is stopped.
     *
     * @param transfer transfer instance to inspect
     */
    private void checkLocationState(ContentTransfer transfer) {
        // Protect against null selected transfer
        if (transfer == null) {
            return;
        }

        ContentSourceLocationState state = transfer.getSourceLocationState();
        boolean doStart = false;
        boolean doStop = false;
        synchronized(this) {
            if (locationState.isLocating()) {
                boolean beyondThreshold = false;
                if (transfer == selected && state.hasMany()) {
                    beyondThreshold = true;
                } else if (standby.contains(transfer) && state.hasEnough()) {
                    beyondThreshold = true;
                }

                if (state.isLocating()) {
                    if (beyondThreshold) {
                        doStop = true;
                    }
                } else {
                    if (!beyondThreshold) {
                        doStart = true;
                    }
                }
            } else {
                // We've been asked to stop.
                doStop = true;
            }
        }

        /*
         * Now affect changes outside synchronized block, as these actions
         * may cause events to be thrown downstream.
         */
        if (doStart) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Starting source location for transfer: " + transfer);
            }
            transfer.startSourceLocation();
        } else if (doStop) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Stopping source location for transfer: " + transfer);
            }
            transfer.stopSourceLocation();
        }
    }

    /**
     * Checks the location state of the selected transfer as well as all
     * standby transfers.
     */
    private void checkSelectedAndStandby() {
        checkLocationState(selected);
        for (ContentTransfer transfer : standby) {
            checkLocationState(transfer);
        }
    }

    /**
     * Checks the transfer state of the aggregated transfer, checking to
     * to see if the cancelled or failed flags have been raised.  If the
     * cancelled flag is present a transfer cancelled exception is raised.
     * If the failure flag is present the stored failure cause will be
     * thrown if it exists, or a new transfer exception will be thrown
     * if it does not exist.
     *
     * @throws TransferCancelledException if the transfer has been cancelled
     * @throws TransferException if the transfer hsa failed
     */
    private void checkTransferState() throws TransferException {
        synchronized(this) {
            if (transferState.isSuccessful()) {
                return;
            } else if (transferState.isFinished()) {
                if (transferState == CANCELLED) {
                    throw(new TransferCancelledException());
                } else {
                    if (failureCause != null) {
                        throw(failureCause);
                    }
                    throw(new TransferException("Transfer failed"));
                }
            } else if (!transferState.isRetrieving() && selected != null) {
                // Make sure the transfer has started
                selected.startTransfer(destFile);
            }
        }
    }

    /**
     * Cancels all outstanding transfers, except for the one optionally
     * provided.  In the process of doing this, we stop listening to all
     * transfers - even exceptThisOne - if the becomeDeaf param is set
     * to true.
     *
     * @param exceptThisOne transfer to not cancel, or null
     * @param becomeDeaf true if we should remove ourselves as a listener
     *  of the transfers prior to cancelling, false to spam ourselves with
     *  events
     */
    private void cancelAll(ContentTransfer exceptThisOne, boolean becomeDeaf) {
        // This list is built while synchronized
        List<ContentTransfer> allTransfers = getContentTransferList();
        // But we cancel outside of synchronization due to event throwing
        for (ContentTransfer transfer : allTransfers) {
            if (becomeDeaf) {
                transfer.removeContentTransferListener(this);
            }
            if (transfer != exceptThisOne) {
                transfer.cancel();
            }
        }
    }

    /**
     * Notify listeners of potential changes to the source location
     * status.
     *
     * @param event event information to send to listeners
     */
    private void fireLocationStateUpdated(ContentTransferEvent event) {
        for (ContentTransferListener listener : ctListeners) {
            try {
                listener.contentLocationStateUpdated(event);
            } catch (Throwable t) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING,
                            "Uncaught throwable from listener", t);
                }
            }
        }
    }

    /**
     * Notify listeners of potential changes to the transfer status.
     *
     * @param event event information to send to listeners
     */
    private void fireTransferStateUpdated(ContentTransferEvent event) {
        for (ContentTransferListener listener : ctListeners) {
            try {
                listener.contentTransferStateUpdated(event);
            } catch (Throwable t) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING,
                            "Uncaught throwable from listener", t);
                }
            }
        }
    }

    /**
     * Notify listeners that a new transfer instance has been selected as
     * the current/main instance.
     *
     * @param newSelected the newly selected primary transfer instance
     */
    private void fireSelectedContentTransfer(ContentTransfer newSelected) {
        ContentTransferAggregatorEvent event = null;

        for (ContentTransferAggregatorListener listener : ctaListeners) {
            try {
                if (event == null) {
                    event = new ContentTransferAggregatorEvent(
                            this, newSelected);
                }
                listener.selectedContentTransfer(event);
            } catch (Throwable t) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING,
                            "Uncaught throwable from listener", t);
                }
            }
        }
    }
}
