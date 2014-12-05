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

package net.jxta.impl.content.defprovider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PipeAdvertisement;

/**
 * This class provides a somewhat efficient mechanism for tracking active
 * transfers and providing a cache for information related to those transfers.
 * As an example, Contents currently only have streamed data accessors.  Requests
 * for data will typically come in a linear fashion, so it makes good
 * sense from a performance perspective to leave the stream open for a period
 * of time.  Additionally, this tracker class puts some absolute limits in
 * place as to how many concurrent clients will be served, giving preference
 * to transfers already in progress.
 */
public class ActiveTransferTracker {
    /**
     * Logger.
     */
    private static final Logger LOG =
            Logger.getLogger(ActiveTransferTracker.class.getName());

    /**
     * Maximum number of clients to serve concurrently.
     */
    private static final int MAX_CLIENTS =
            Integer.getInteger(ActiveTransferTracker.class.getName()
            + ".maxClients", 5).intValue();

    /**
     * Garbage collection interval, in seconds.
     */
    private static final long GC_INTERVAL =
            Long.getLong(ActiveTransferTracker.class.getName()
            + ".gcInterval", 5).intValue();
    
    /**
     *
     */
    private final List<ActiveTransferTrackerListener> listeners =
            new CopyOnWriteArrayList<ActiveTransferTrackerListener>();

    /**
     * PeerGroup to use to resolve pipes.
     */
    private final PeerGroup group;

    /**
     * Timer to use when scheduling tasks.
     */
    private final ScheduledExecutorService schedExec;

    /**
     * Garbage collection task;
     */
    private ScheduledFuture gcTask;

    /**
     * Map of clients being served, keyed off the destination pipe ID.
     */
    private Map<Object, ActiveTransfer> clients =
            new HashMap<Object, ActiveTransfer>();

    /**
     * Constructor.
     *
     * @param peerGroup PeerGroup to use to resolve pipes
     * @param executor executor to submit garbage collection tasks to
     */
    public ActiveTransferTracker(
            PeerGroup peerGroup,
            ScheduledExecutorService executor) {
        group = peerGroup;
        schedExec = executor;
    }

    //////////////////////////////////////////////////////////////////////////
    // Public methods:
    
    /**
     * Adds an tracker listener to notify of interesting events.
     * 
     * @param listener listener to add
     */
    public void addActiveTransferListener(
            ActiveTransferTrackerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a transfer listener, preventin further event
     * notifications.
     * 
     * @param listener listener to add
     */
    public void removeActiveTransferListener(
            ActiveTransferTrackerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Creates and/or retrieves the active transfer session for the
     * provided combination of share and destination.
     *
     * @param share content share to serve to the destination
     * @param destination pipe to send data to
     * @return trnsfer session object
     */
    public ActiveTransfer getSession(
            DefaultContentShare share, PipeAdvertisement destination)
            throws TooManyClientsException, IOException {
        Object key = destination.getPipeID().getUniqueValue();
        ActiveTransfer result;
        boolean newSession = false;
        synchronized(this) {
            result = clients.get(key);
            if (result == null) {
                if (clients.size() < MAX_CLIENTS) {
                    result = new ActiveTransfer(group, share, destination);
                    newSession = true;
                    clients.put(key, result);
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Added client node: " + key);
                    }
                }
            }
        }

        // Too many clients to serve this request.
        if (result == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Cound not add client node.  Too many clients.");
            }
            throw(new TooManyClientsException());
        }
        
        // Notify listners
        if (newSession) {
            fireSessionCreated(result);
        }

        return result;
    }

    /**
     * Start tracking and serving.
     */
    public synchronized void start() {
        if (gcTask == null || gcTask.isDone()) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Starting GC task");
            }
            gcTask = schedExec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    clientGC();
                }
            }, 0, GC_INTERVAL, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop tracking and serving, freeing any resources held.
     */
    public void stop() {
        List<ActiveTransfer> toNotify = new ArrayList<ActiveTransfer>();
        synchronized(this) {
            for (Map.Entry<Object, ActiveTransfer> entry : clients.entrySet()) {
                ActiveTransfer session = entry.getValue();
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Closing client session: " + entry.getKey());
                }
                try {
                    session.close();
                } catch (IOException iox) {
                    LOG.log(Level.FINEST, "Ignoring exception", iox);
                }
                toNotify.add(session);
            }
            clients.clear();

            try {
                if (gcTask != null) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Stopping GC task");
                    }
                    gcTask.cancel(false);
                }
            } catch (IllegalStateException isx) {
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "Ignoring exception", isx);
                }
            } finally {
                gcTask = null;
            }
        }
        
        // Notify listeners
        for (ActiveTransfer transfer : toNotify) {
            fireSessionCollected(transfer);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Periodic cleanup task to remove any inactive clients.
     */
    private void clientGC() {
        Iterator<Map.Entry<Object, ActiveTransfer>> it;
        Map.Entry<Object, ActiveTransfer> entry;
        List<ActiveTransfer> toNotify = null;
        ActiveTransfer session;

        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest("clientGC");
        }
        synchronized(this) {
            it = clients.entrySet().iterator();
            while (it.hasNext()) {
                entry = it.next();
                session = entry.getValue();
                if (session.isIdle()) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Closing client session: " + entry.getKey());
                    }
                    try {
                        session.close();
                    } catch (IOException iox) {
                        if (Logging.SHOW_FINEST
                                && LOG.isLoggable(Level.FINEST)) {
                            LOG.log(Level.FINEST, "Ignoring exception", iox);
                        }
                    }

                    if (toNotify == null) {
                        toNotify = new ArrayList<ActiveTransfer>();
                        toNotify.add(session);
                    }

                    it.remove();
                }
            }
        }
        
        // Notify listeners
        if (toNotify != null) {
            for (ActiveTransfer transfer : toNotify) {
                fireSessionCollected(transfer);
            }
        }
    }
    
    /**
     * Notify all listeners that a new session has been created.
     * 
     * @param transfer the new session
     */
    private void fireSessionCreated(ActiveTransfer transfer) {
        for (ActiveTransferTrackerListener listener : listeners) {
            listener.sessionCreated(transfer);
        }
    }

    /**
     * Notify all listeners that an idle session has been garbage
     * collected.
     * 
     * @param transfer the idle session
     */
    private void fireSessionCollected(ActiveTransfer transfer) {
        for (ActiveTransferTrackerListener listener : listeners) {
            listener.sessionCollected(transfer);
        }
    }
}
