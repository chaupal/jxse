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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProvider;
import net.jxta.content.ContentSourceLocationState;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferCancelledException;
import net.jxta.content.TransferException;
import net.jxta.content.TransferFailedException;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.protocol.ContentShareAdvertisement;

/**
 * Abstract implementation of a ContentProvider, providing the basic
 * infrastructure for locating the desired quantity of remote advertisements,
 * notifying listeners of location state changes, and basic infrastrucure
 * around transfer attempts.  This class is by no means required to be
 * extended to implement a ContentTransfer but simply attempts to consolidate
 * much of the logic that would be repeated throughout many implementations.
 */
public abstract class AbstractContentTransfer
        implements ContentTransfer {
    /**
     * The default source location interval, in seconds.  This constant
     * is provided for information purposes only - please do not use.
     */
    protected static final int DEFAULT_SOURCE_LOCATION_INTERVAL = 15;

    /**
     * The default number of advertisements that will be requested from
     * the discovery service when attempting to discover more source
     * locations.  This constant is provided for information purposes
     * only - please do not use.
     */
    protected static final int DEFAULT_DISCOVERY_THRESHOLD = 10;

    /**
     * The default number of consecutive transfer attempts that report
     * stalled status before the transfer provider will be de-selected.
     */
    protected static final int DEFAULT_MAX_STALLS = 3;

    /**
     * The default value used to determine if local advertisements
     * will be searched for when looking for new source locations.
     * This constant is provided for information purposes only - please
     * do not use.
     */
    protected static final boolean DEFAULT_ENABLE_LOCAL = true;

    /**
     * The default value used to determine if remote advertisements
     * will be searched for when looking for new source locations.
     * This constant is provided for information purposes only - please
     * do not use.
     */
    protected static final boolean DEFAULT_ENABLE_REMOTE = true;

    /**
     * Logger instance.
     */
    private final Logger LOG;

    /**
     * List of our listeners.
     */
    private final CopyOnWriteArrayList<ContentTransferListener> listeners =
            new CopyOnWriteArrayList<ContentTransferListener>();

    /**
     * Executor service that we will use to schedule tasks.
     */
    private final ScheduledExecutorService executor;

    /**
     * Our parent peer group.
     */
    private final PeerGroup peerGroup;
    
    /**
     * ContentProvider which created and manages this transfer.
     */
    private final ContentProvider provider;

    /**
     * The master content ID that we are using to identify
     * our intended content to retrieve.
     */
    private final ContentID masterID;

    /**
     * DiscoveryListener interface to use during source location.
     */
    private final DiscoListener discoveryListener = new DiscoListener();

    /**
     * Runnable to use when creating new location tasks.
     */
    private final Runnable locationRunnable = new Runnable() {
        public void run() {
            try {
                locationExecution();
            } catch (RuntimeException rtx) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING,
                            "Uncaught exception in location execution", rtx);
                }
            }
        }
    };

    /**
     * Runnable to use when creating new transfer tasks.
     */
    private final Runnable transferRunnable = new Runnable() {
        public void run() {
            try {
                transferExecution();
            } catch (InterruptedException intx) {
                Thread.interrupted();
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Transfer thread interrupted", intx);
                }
            }
        }
    };

    /**
     * Object used to lock, wait, and notify on internally.
     */
    private final Object lockObject = this;

    /**
     * The number of seconds between source discovery attempts.
     */
    private int sourceLocationInterval = DEFAULT_SOURCE_LOCATION_INTERVAL;

    /**
     * The number of of allSources considered to be "enough".
     */
    private int enoughSources = getEnoughLocationCount();

    /**
     * The number of of allSources considered to be "many".
     */
    private int manySources = getManyLocationCount();

    /**
     * The discovery threshold to use.
     */
    private int discoveryThreshold = DEFAULT_DISCOVERY_THRESHOLD;

    /**
     * Flag indicating whether or not local advertisements will be queried
     * when searching for data allSources.  Used for debug.
     */
    private boolean allowLocalDiscovery = DEFAULT_ENABLE_LOCAL;

    /**
     * Flag indicating whether or not remote advertisements will be queried
     * when searching for data allSources. Used for debug.
     */
    private boolean allowRemoteDiscovery = DEFAULT_ENABLE_REMOTE;

    /**
     * Maximum consecutive times a transfer attempt can report stalled
     * before the provider is de-selected.
     */
    private int maxStalls = DEFAULT_MAX_STALLS;

    /**
     * Current location state.
     */
    private ContentSourceLocationState locationState
            = ContentSourceLocationState.NOT_LOCATING;

    /**
     * Current transfer state.
     */
    private ContentTransferState transferState =
            ContentTransferState.PENDING;

    // Managed over the course of the transfer

    /**
     * Transfer state when the last check was performed.  This allows
     * us to detect changes which require event notification.
     */
    private ContentTransferState lastTransferState = transferState;

    /**
     * Sources that we know about that we have not yet tried to retrieve
     * from.
     */
    private List<ContentShareAdvertisement> allSources =
            new CopyOnWriteArrayList<ContentShareAdvertisement>();

    /**
     * Sources that we know about but which we've marked as not being
     * usable.
     */
    private List<ContentShareAdvertisement> uselessSources =
            new CopyOnWriteArrayList<ContentShareAdvertisement>();

    /**
     * Sources which have been found since the last transfer attempt.
     */
    private List<ContentShareAdvertisement> newSources =
            new ArrayList<ContentShareAdvertisement>();

    /**
     * Future used when scheduling source location activity.
     */
    private ScheduledFuture locationTask = null;

    /**
     * Future used when scheduling transfer activity.
     */
    private ScheduledFuture transferTask = null;

    /**
     * The destination file that the content data should be dumped into
     * when transferring.
     */
    private File destFile;

    /**
     * Once the transfer has completed successfully, a reference to the
     * Content object that was created.
     */
    private Content content;

    /**
     * Transfer state goal.
     */
    private ContentTransferState goalState =
            ContentTransferState.PENDING;

    /**
     * Exception caught during transfer.  This is used to hold the exception
     * for use in wait blocks.
     */
    private TransferException reason;

    //////////////////////////////////////////////////////////////////////////
    // Inner classes:

    /**
     * This class is used to hide our discovery listener interface from the
     * rest of the world.
     */
    private class DiscoListener implements DiscoveryListener {
        /**
         * {@inheritDoc}
         */
        public void discoveryEvent(DiscoveryEvent event) {
            addDiscoveredSources(event.getSearchResults());
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Contructor.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param schedExecutor scheduled executor to use when scheduling source
     *  location activities.
     * @param group peer group that this transfer is taking place within
     * @param contentAdv master advertisement used to identify the desired
     *  content
     * @param loggerID logger name under which to log messages
     */
    protected AbstractContentTransfer(
            ContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentShareAdvertisement contentAdv,
            String loggerID) {
        provider = origin;
        executor = schedExecutor;
        peerGroup = group;
        masterID = contentAdv.getContentID();
        LOG = Logger.getLogger(
                AbstractContentTransfer.class.getName() + "-" + loggerID);
        addDiscoveredSource(contentAdv);
    }

    /**
     * Contructor.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param schedExecutor scheduled executor to use when scheduling source
     *  location activities.
     * @param group peer group that this transfer is taking place within
     * @param contentID ID of the content that we would like to retrieve
     * @param loggerID logger name under which to log messages
     */
    protected AbstractContentTransfer(
            ContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentID contentID,
            String loggerID) {
        provider = origin;
        executor = schedExecutor;
        peerGroup = group;
        masterID = contentID;
        LOG = Logger.getLogger(
                AbstractContentTransfer.class.getName() + "-" + loggerID);
    }

    //////////////////////////////////////////////////////////////////////////
    // ContentTransfer interface methods:

    /**
     * {@inheritDoc}
     */
    public void addContentTransferListener(
            ContentTransferListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentTransferListener(
            ContentTransferListener listener) {
        listeners.remove(listener);
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
        boolean doFire = false;

        synchronized(lockObject) {
            if (!locationState.isLocating() && !locationState.hasMany()) {
                // Start locating
                doFire = true;
                locationState = locationState.getEquivalent(true);
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Starting source location (state="
                            + locationState + ", interval="
                            + sourceLocationInterval + ", locationTask="
                            + locationTask + ")");
                }
                if (locationTask == null || locationTask.isDone()) {
                    locationTask = executor.scheduleWithFixedDelay(
                            locationRunnable, 0, sourceLocationInterval,
                            TimeUnit.SECONDS);
                }
            }
        }
        if (doFire) {
            fireLocationStateChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stopSourceLocation() {
        boolean doFire = false;

        synchronized(lockObject) {
            if (locationState.isLocating()) {
                // Stop locating
                locationState = locationState.getEquivalent(false);
                locationTask.cancel(false);
                locationTask = null;
                doFire = true;
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Stopping source location (state="
                            + locationState + ")");
                }
            }
        }

        if (doFire) {
            fireLocationStateChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentSourceLocationState getSourceLocationState() {
        synchronized(lockObject) {
            return locationState;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startTransfer(File destination) {
        if (destination == null) {
            throw(new IllegalArgumentException(
                    "Destination cannot be null"));
        } else if (destFile != null) {
            throw(new IllegalStateException(
                    "This transfer has already been started"));
        }

        synchronized(lockObject) {
            if (goalState.isFinished() || goalState.isRetrieving()) {
                // Ignore.
                return;
            }
            goalState = ContentTransferState.RETRIEVING;
            destFile = destination;
        }
        startSourceLocation();
        checkTransferState();
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransferState getTransferState() {
        synchronized(lockObject) {
            return transferState;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        synchronized(lockObject) {
            goalState = ContentTransferState.CANCELLED;
            if (reason == null) {
                reason = new TransferCancelledException();
            }
        }
        checkTransferState();
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
        synchronized(lockObject) {
            if (transferState.isFinished()) {
                if (transferState.isSuccessful()) {
                    return;
                } else {
                    throw(reason);
                }
            }

            lockObject.wait(timeout);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Content getContent() throws InterruptedException, TransferException {
        synchronized(lockObject) {
            waitFor();

            // If we get here, we're successful
            return content;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Protected and abstract methods:

    /**
     * Gets the ContentID which we are attempting to transfer.
     * 
     * @return ContentID provided either directly or indirectly through
     *  one of the constructors.
     */
    protected ContentID getTransferContentID() {
        return masterID;
    }
    
    /**
     * Sets the resulting Content object.  This method should be called as
     * during a transferAttempt implementation execution run, prior to that
     * execution run returning ContentTransferState.COMPLETED.
     *
     * @param finalContent successfully retrieved content instance
     */
    protected void setContent(Content finalContent) {
        if (finalContent == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Attempt to set null Content");
            }
            // Ignore
            return;
        }
        if (!finalContent.getContentID().equals(masterID)) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Attempt to set Content with wrong ID: " +
                    finalContent.getContentID());
            }
            // Ignore
            return;
        }
        synchronized(lockObject) {
            if (content == null) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Setting Content to: " + finalContent);
                }
                content = finalContent;
                lockObject.notifyAll();
            }
        }
    }

    /**
     * Gets the list of ContentTransferListeners associated with this
     * transfer instance.  This list is safe to iterate over and modify,
     * as it is backed by a copy-on-write implementation.
     *
     * @return list of ContentTransferListeners
     */
    protected List<ContentTransferListener> getContentTransferListeners() {
        return listeners;
    }

    /**
     * Sets the source location interval that will be used when scheduling
     * new discovery requests when attempting to locate more source locations.
     *
     * @param newInterval new source location interval, in seconds
     */
    protected void setSourceLocationInterval(int newInterval) {
        if (newInterval <= 0) {
            throw(new IllegalArgumentException(
                    "New interval must be > 0 (was: " + newInterval + ")"));
        }
        synchronized(lockObject) {
            sourceLocationInterval = newInterval;
        }
    }

    /**
     * Sets the discovery threshold used to limit the number of responses
     * that will be returned by the discovery service when attempting to
     * locate more data allSources.
     *
     * @param newThreshold the new maximum number of discovery responses to
     *  request from the discovery service
     */
    protected void setDiscoveryThreshold(int newThreshold) {
        if (newThreshold <= 0) {
            throw(new IllegalArgumentException(
                    "New threshold must be > 0 (was: " + newThreshold + ")"));
        }
        synchronized(lockObject) {
            discoveryThreshold = newThreshold;
        }
    }

    /**
     * Sets the flag indicating whether or not local advertisements will be
     * search for when attempting to locate new source locations.  This is
     * primarily intended for debug and is safe to leave enabled.
     *
     * @param enabled true to search local advertisements, false to not
     */
    protected void setLocalDiscoveryEnabled(boolean enabled) {
        synchronized(lockObject) {
            allowLocalDiscovery = enabled;
        }
    }

    /**
     * Sets the flag indicating whether or not remote advertisements will be
     * search for when attempting to locate new source locations.  This is
     * primarily intended for debug and is safe to leave enabled.
     *
     * @param enabled true to search remote advertisements, false to not
     */
    protected void setRemoteDiscoveryEnabled(boolean enabled) {
        synchronized(lockObject) {
            allowRemoteDiscovery = enabled;
        }
    }

    /**
     * Gets the number of source location advertisements that must be
     * obtained before attempting to start a transfer.  Typically, this
     * will be a small number, even 1.
     *
     * @return number of advertisements to discover before transfer will
     *  be attempted
     */
    protected abstract int getEnoughLocationCount();

    /**
     * Gets the number of source location advertisements that must be
     * obtained before source location will be shut down.  This should
     * represent some happy medium between having too few allSources to be
     * efficient and having too many allSources to practically use.
     *
     * @return number of advertisements to discover before source location
     *  will be stopped
     */
    protected abstract int getManyLocationCount();

    /**
     * Determines whether or not the advertisement provided is of use
     * to the provider.  Advertisements not of use are not counted toward
     * the provider's "enough" and "many" thresholds.
     *
     * @param adv advertisement to test
     * @return true if advertisment is of use, false otherwise
     */
    protected abstract boolean isAdvertisementOfUse(ContentShareAdvertisement adv);

    /**
     * Attempt to transfer the content from the allSources provided, using the
     * specified destination file for data storage.
     *
     * @param dest destination file which should be used to store the
     *  content data
     * @param sources all currently known sources
     * @param newSources all sources which have been discovered since the
     *  last transfer attempt
     * @return content transfer state best representing the results of this
     *  attempt.  Must be set to ContentState.COMPLETED on success, in which
     *  case the implementation must have called setContent().
     * @throws TransferFailedException when the transfer has irrecoverably
     *  failed.  No more transfer attempts will be made and the failure will
     *  be propagated to the user.
     * @throws TransferException when the transfer has failed and it is likely
     *  that a retry (either now or in the near future) will have better
     *  success.
     */
    protected abstract ContentTransferState transferAttempt(
            File dest,
            List<ContentShareAdvertisement> sources,
            List<ContentShareAdvertisement> newSources)
            throws TransferException;

    //////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Checks to see if the transfer state has changed or if any action
     * needs to be taked to keep the transfer progressing.  In general, this
     * method is responsible for tracking and keeping the transfer state
     * healthy.
     */
    private void checkTransferState() {
        boolean doFire = false;
        boolean doNotify = false;
        boolean doInterrupt = false;
        boolean doStopLocation = false;
        synchronized(lockObject) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Check transfer state");
                LOG.fine("   Last State: " + lastTransferState);
                LOG.fine("   State     : " + transferState);
                LOG.fine("   Goal      : " + goalState);
            }
            if (lastTransferState != transferState) {
                doFire = true;
            }
            if (goalState != transferState) {
                switch(goalState) {
                case RETRIEVING:
                    if (transferState.isSuccessful()) {
                        goalState = transferState;
                        doFire = true;
                        doNotify = true;
                        doStopLocation = true;
                    } else if (!transferState.isFinished()
                            && !transferState.isRetrieving()) {
                        // Start retrieving
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Setting up transfer execution");
                        }
                        transferState = goalState;
                        if (transferTask == null || transferTask.isDone()){
                            transferTask = executor.schedule(
                                    transferRunnable, 0, TimeUnit.SECONDS);
                        }
                        doFire = true;
                    }
                    break;
                case COMPLETED:
                    // Fall through
                case CANCELLED:
                    // Fall through
                case FAILED:
                    if (!transferState.isFinished()) {
                        transferState = goalState;
                        doFire = true;
                        doNotify = true;
                        doInterrupt = true;
                        doStopLocation = true;
                    }
                    break;
                default:
                    throw(new IllegalStateException(
                            "Unhandled goal state: " + goalState
                            + " -  Please report!"));
                }

                if (lastTransferState != transferState) {
                    doFire = true;
                }
            }

            lastTransferState = transferState;
        }

        if (doFire) {
            fireTransferStateChanged();
        }

        if (doStopLocation) {
            stopSourceLocation();
        }

        synchronized(lockObject) {
            if (doInterrupt && transferTask != null) {
                if (transferTask.cancel(true)) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Cancelled/interrupted transfer task");
                    }
                }
                transferTask = null;
            }

            if (doNotify) {
                lockObject.notifyAll();
            }
        }
    }

    /**
     * Source location execution.
     */
    private void locationExecution() {
        String desiredID = masterID.toString();
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Locating more data sources for ID: " + desiredID);
        }
        DiscoveryService discoveryService = peerGroup.getDiscoveryService();
        if (allowLocalDiscovery) {
            try {
                Enumeration localAdvs =
                        discoveryService.getLocalAdvertisements(
                        DiscoveryService.ADV,
                        "ContentID", desiredID);
                addDiscoveredSources(localAdvs);
            } catch (IOException iox) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING,
                            "Could not query local advertisements", iox);
                }
            }
        }
        if (allowRemoteDiscovery) {
            discoveryService.getRemoteAdvertisements(
                    null, DiscoveryService.ADV,
                    "ContentID", desiredID,
                    discoveryThreshold, discoveryListener);
        }
    }

    /**
     * Adds the discovery results to our known list of allSources.
     */
    private void addDiscoveredSources(Enumeration advs) {
        while(advs.hasMoreElements()) {
            Object adv = advs.nextElement();
            try {
                LOG.fine("Discovered adv: " + adv);
                if (adv instanceof ContentShareAdvertisement) {
                    addDiscoveredSource((ContentShareAdvertisement) adv);
                }
            } catch (ClassCastException castx) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Found unusable advertisement: " + adv);
                }
            }
        }
    }

    /**
     * Checks to see if the source provided is known already.  If not, it
     * adds the advertisement to the list of known allSources and updates
     * the location state as appropriate.
     */
    private void addDiscoveredSource(ContentShareAdvertisement adv) {
        boolean doFire;
        boolean doStop;

        synchronized(lockObject) {
            if (allSources.contains(adv)) {
                // Already found this source.
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Advertisement was already found:\n" + adv);
                }
                return;
            }
            if (uselessSources.contains(adv)) {
                // Already found this one too.
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Advertisement was already found to be unusable:\n"
                            + adv);
                }
                return;
            }

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Found advertisement: " + adv);
            }
            if (isAdvertisementOfUse(adv)) {
                allSources.add(adv);
                newSources.add(adv);
                lockObject.notifyAll();
                doFire = checkSources();
                doStop = locationState.hasMany();
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Sources known now at: "
                            + allSources.size() + "   State: " + locationState);
                }
            } else {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Advertisement determined to not be usable");
                }
                uselessSources.add(adv);
                doFire = false;
                doStop = false;
            }
        }
        if (doFire) {
            fireLocationStateChanged();
        }
        if (doStop) {
            stopSourceLocation();
        }
    }

    /**
     * Checks to see if we have "enough" and/or "many" allSources and updates
     * the location state as appropriate.
     *
     * @return true if location state has changed, false otherwise
     */
    private boolean checkSources() {
        ContentSourceLocationState newState;
        synchronized(lockObject) {
            if (allSources.size() >= manySources) {
                newState = ContentSourceLocationState.LOCATING_HAS_MANY;
            } else if (allSources.size() >= enoughSources) {
                newState = ContentSourceLocationState.LOCATING_HAS_ENOUGH;
            } else {
                newState = ContentSourceLocationState.LOCATING;
            }
            newState = newState.getEquivalent(locationState.isLocating());
            if (locationState == newState) {
                return false;
            } else {
                locationState = newState;
                return true;
            }
        }
    }

    /**
     * Notify listeners of a change in the source location state.
     */
    private void fireLocationStateChanged() {
        ContentTransferEvent event = null;
        for (ContentTransferListener listener : listeners) {
            if (event == null) {
                event = createTransferEvent();
            }
            listener.contentLocationStateUpdated(event);
        }
    }

    /**
     * Notify listeners of a change in the source location state.
     */
    private void fireTransferStateChanged() {
        ContentTransferEvent event = null;
        for (ContentTransferListener listener : listeners) {
            if (event == null) {
                event = createTransferEvent();
            }
            listener.contentTransferStateUpdated(event);
        }
    }

    /**
     * Creates a content transfer event containing our current status.
     */
    private ContentTransferEvent createTransferEvent() {
        synchronized(lockObject) {
            return new ContentTransferEvent.Builder(this)
                    .locationCount(allSources.size())
                    .locationState(locationState)
                    .transferState(transferState)
                    .build();
        }
    }

    /**
     * Transfer execution mainline.
     */
    @SuppressWarnings("fallthrough")
    private void transferExecution() throws InterruptedException {
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Transfer execution starting");
        }
        ContentTransferState attemptResult = ContentTransferState.PENDING;
        List<ContentShareAdvertisement> newList =
                new ArrayList<ContentShareAdvertisement>();
        List<ContentShareAdvertisement> allList =
                new ArrayList<ContentShareAdvertisement>();
        int stalls = 0;
        boolean doDelay;
        boolean running = true;

        try {
            do {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Updating transfer attempt source lists");
                }

                doDelay = false;
                allList.clear();
                newList.clear();

                synchronized(lockObject) {
                    if (!transferState.isRetrieving()) {
                        // We've been instructured to stop
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Transfer state no longer retrieving");
                        }
                        break;
                    }
                    allList.addAll(allSources);
                    newList.addAll(newSources);

                    // Reset the new sources list
                    newSources.clear();
                }

                if (allList.size() + newList.size() == 0) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("No sources found.  Waiting.");
                    }
                    stalls++;
                    synchronized(lockObject) {
                        transferState = ContentTransferState.STALLED;
                        doDelay = true;
                    }
                } else {
                    boolean doCheck = false;
                    synchronized(lockObject) {
                        if (transferState != ContentTransferState.RETRIEVING) {
                            transferState = ContentTransferState.RETRIEVING;
                            doCheck = true;
                        }
                    }
                    if (doCheck) {
                        // Fire an event off immediately
                        checkTransferState();
                    }

                    try {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Transfer attempt commencing");
                        }
                        attemptResult = transferAttempt(
                                destFile, allList, newList);
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Transfer attempt result: " + attemptResult);
                        }
                        switch(attemptResult) {
                        case CANCELLED:
                            cancel();
                            running = false;
                            break;
                        case FAILED:
                            throw(new TransferFailedException(
                                    "Transfer attempt result: "
                                    + attemptResult));
                        case PENDING:
                        case RETRIEVING:
                        case STALLED:
                            // Remap these to something that makes sense
                            attemptResult = ContentTransferState.STALLED;
                            doDelay = true;
                            stalls++;
                            // Fall through...
                        case RETRYING:
                            synchronized(lockObject) {
                                transferState = attemptResult;
                            }
                            break;
                        case COMPLETED:
                            synchronized(lockObject) {
                                if (content == null) {
                                    LOG.warning("Transfer attempt returned "
                                            + attemptResult
                                            + " but content was not set!");
                                } else {
                                    transferState = attemptResult;
                                    running = false;
                                }
                            }
                            break;
                        }
                    } catch (TransferException transx) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Transfer attempt failed", transx);
                        }
                        // Irrecoverable failure.  Bubble this up to user.
                        synchronized(lockObject) {
                            transferState = ContentTransferState.FAILED;
                            if (reason == null) {
                                reason = transx;
                            }
                        }
                        running = false;
                    }
                }

                if (stalls >= maxStalls) {
                    synchronized(lockObject) {
                        transferState = ContentTransferState.FAILED;
                        if (reason == null) {
                            reason = new TransferException(
                                    "Maximum number of stalled transfer "
                                    + "attempts reached (" + maxStalls + ")");
                        }
                    }
                }

                checkTransferState();
                if (doDelay) {
                    /*
                     * We wait with timeout to allow incoming advertisements
                     * to be acted on immediately after they arrive, since
                     * that is likely to be the cause of the stalls, etc..
                     */
                    synchronized(lockObject) {
                        try {
                            lockObject.wait(sourceLocationInterval * 1000L);
                        } catch (InterruptedException intx) {
                            if (Thread.interrupted()) {
                                // Rethrow
                                throw(intx);
                            }
                        }
                    }
                }
            } while(running);
        } catch (RuntimeException rtx) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Caught runtime exception", rtx);
            }
            throw(rtx);
        } finally {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Transfer execution exiting");
            }
        }
    }
}
