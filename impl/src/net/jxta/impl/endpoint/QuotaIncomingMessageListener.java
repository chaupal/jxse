/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
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
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.endpoint;


import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.Message;
import net.jxta.impl.endpoint.endpointMeter.EndpointMeterBuildSettings;
import net.jxta.impl.endpoint.endpointMeter.InboundMeter;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.util.*;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A wrapper around an EndpointListener which imposes fair sharing quotas.
 * <p/>
 * <p/><b>NOTE</b>: 20020526 jice
 * There would be great value in making such an object the explicit interface
 * between the endpoint and its clients, rather than a bland listener interface
 * The client would have the ability to specify the threads limit, possibly
 * setting it zero, and then could have access to the buffer.
 * To implement that with a simple listener would mean that the endpoint
 * has to TRUST the client that the listener does no-more than queuing and
 * signaling or else, force a full and possibly redundant hand-shake in all
 * cases, as is the case now.  To be improved.
 */
public class QuotaIncomingMessageListener implements EndpointListener {

    /**
     * Log4J Logger
     */
    private final static Logger LOG = Logger.getLogger(QuotaIncomingMessageListener.class.getName());

    /**
     * All QuotaIncomingMessageListener share one global resource manager for
     * threads. Its budget is hardcoded for now.
     * <p/>
     * <p/>Parameters read as follows:
     * <p/>
     * <p/><pre>
     * new ResourceDispatcher(
     *      100,  // support at least this many listeners
     *      1,    // each with at least that many reserved threads each
     *      5,    // let a listener reserve up to this many threads
     *      500,  // additional un-reserved threads
     *      50,   // any listener can have up to this many un-reserved threads
     *      20,   // threads that can never be reserved
     *      true  // use round robin
     *      );
     * </pre>
     * <p/>
     * <p/>It means that we will authorize up to 1000 threads total (<code>
     * 100 listeners * 5 reserved threads + 500 additional unreserved threads
     * </code>).
     * <p/>
     * <p/>We can support more than 100 listeners, but we cannot guarantee
     * that we will be able to reserve even 1 thread for them. If we do
     * it will be by pulling it out of the un-reserved set if there are
     * any available at that time. If every listener uses only the minimal
     * guaranteed of 1 thread, then we can support 600 listeners (<code>
     * 100 listeners + 500 additional unreserved threads</code>).
     * <p/>
     * <p/>Round robin means that listeners that want to use threads beyond
     * their reservation are queued waiting for an extra thread to become
     * available.
     */
    private final static ResourceDispatcher threadDispatcher = new ResourceDispatcher(100, 1, 2, 150, 20, 20, true
            ,
            "threadDispatcher");

    /*
     All next hop peers that send us messages share one global resource
     manager for message queing. Its budget is hardcoded for now.
     If you tune only the first two values, the derived ones
     compute to a total commitment of GmaxSenders * GmaxMsgSize * 10

     Examples:
     150 senders * 20K messages * 10 max allocation => 30M
     350 senders * 300K messages * 10 max allocation => 1G
     3500 senders * 8k messages * 10 max allocation => 280M
     10000 senders * 8k messages * 20 max allocation => 1.6GB
     */

    /**
     * Max guaranteed supported message size (bytes). This is a theoretical
     * maximum message size and should reflect the maximum size of messages
     * used in relevant protocols. All other calculations assume this as the
     * "default" message size. If message size is very variable then this should
     * be the median message size rather than the maximum.
     */
    static int GmaxMsgSize = 6 * 1024;

    /**
     * Max guaranteed senders (integer). Expected number of message sources
     * amongst whom resources are to be shared.
     */
    static int GmaxSenders = 550;

    /**
     * Every sender account will always be granted 2 messages worth of queue
     * size.
     */
    static int GminResPerSender = 2 * GmaxMsgSize;

    /**
     * Every sender account can over allocate up to 5 messages worth of queue
     * size if the space is available. If peers are very bursty with messages
     * then this should be higher.
     */
    static int GmaxResPerSender = 3 * GminResPerSender;

    /**
     * Additional resources in reserve, to be allocated on the fly. Available
     * reservations beyond GminResPerSender are taken from there, so, we
     * must have enough. This space is fairly shared by all senders who are
     * over their minimum reserved allocation.
     */
    static int TotalExtra = 2 * GmaxResPerSender * GmaxSenders;

    /**
     * There is a limit to the amount of on-the-fly that a single sender can
     * hog. If peers are very bursty with messages then this should be higher.
     */
    static int MaxExtraPerSender = 8 * GmaxResPerSender;

    /**
     * There is a part of the non-reserved resources that we will never use for
     * reservation by senders in excess of GmaxSenders even if the number of
     * accounts is way beyond the max guaranteed. Instead we'll prefer to grant
     * 0 reserved items to additional senders.
     */
    static int NeverReserved = TotalExtra / 8;

    private final static ResourceDispatcher messageDispatcher = new ResourceDispatcher(GmaxSenders, GminResPerSender
            ,
            GmaxResPerSender, TotalExtra, MaxExtraPerSender, NeverReserved, false, // No RoundRobin
            "messageDispatcher");

    /**
     * A canonical mapping of all the message originators.
     * This is a cache, since peers can disappear silently.
     * <p/>
     * <p/>Note: a number of accounts might not be the best criterion though.
     * since just a few stale accounts could hog a lot of resources.
     * May be we should just make that cache sensitive to the inconvenience
     * endured by live accounts.
     */
    static class MyCacheListener implements CacheEntryListener {
        // This may only be called when something gets added to the
        // cache or when an item is made purgeable. In our case that means
        // all the synchro we need is already there.
        public void purged(CacheEntry entry) {
            ((ResourceAccount) entry.getValue()).close();
        }
    }

    /**
     * We put a large hard limit on the cache because we detect the
     * need for purging normally before that limit is reached, and we purge
     * it explicitly.
     * <p/>
     * <p/>The number 100 is the maximum number of idle accounts that
     * we keep around in case the peer comes back.
     */
    private final static Cache allSources = new Cache(100, new MyCacheListener());

    private final PeerGroup group;

    private final UnbiasedQueue messageQueue = new UnbiasedQueue(Integer.MAX_VALUE, false, new LinkedList());

    private final String name;
    private final InboundMeter incomingMessageListenerMeter;

    private final ResourceAccount myAccount;

    /**
     * The "real" listener.
     */
    private volatile EndpointListener listener = null;

    // Close may be called redundantly and it costs.
    private boolean closed = false;

    /**
     * The last time we warned about having a long queue.
     */
    private long lastLongQueueNotification = 0L;

    /**
     * An incoming message in the queue with its addresses and accounting
     */
    private static class MessageFromSource {
        final Message msg;
        final EndpointAddress srcAddress;
        final EndpointAddress destAddress;
        final ResourceAccount src;
        final long timeReceived;
        final long size;

        MessageFromSource(Message msg, EndpointAddress srcAddress, EndpointAddress destAddress, ResourceAccount src, long timeReceived, long size) {
            this.msg = msg;
            this.src = src;
            this.srcAddress = srcAddress;
            this.destAddress = destAddress;
            this.timeReceived = timeReceived;
            this.size = size;
        }
    }


    /**
     * The thread which services removing items from the queue
     */
    private static class ListenerExecutorTask implements Runnable {

        private QuotaIncomingMessageListener listener;

        ListenerExecutorTask(QuotaIncomingMessageListener listener) {
            this.listener = listener;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {

            try {
                // Process chained jobs.

                while (listener != null) {
                    listener = listener.doOne();
                }
            } catch (Throwable all) {
                LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        }
    }

    /**
     * Constructor for the QuotaIncomingMessageListener object
     *
     * @param name     a unique name for this listener
     * @param listener the recipient listener.
     */
    public QuotaIncomingMessageListener(PeerGroup group, String name, EndpointListener listener) {
        this(group, name, listener, null);
    }

    /**
     * Constructor for the QuotaIncomingMessageListener object
     *
     * @param name                         a unique name for this listener
     * @param listener                     the recipient listener.
     * @param incomingMessageListenerMeter metering handler.
     */
    public QuotaIncomingMessageListener(PeerGroup group, String name, EndpointListener listener, InboundMeter incomingMessageListenerMeter) {
        this.group = group;
        this.listener = listener;
        this.name = name;
        this.incomingMessageListenerMeter = incomingMessageListenerMeter;

        synchronized (threadDispatcher) {
            myAccount = threadDispatcher.newAccount(1, -1, this);
            threadDispatcher.notify(); // makes a contender run earlier
        }

        // Make sure account creation cannot become a denial of service.
        // Give away a time slice.
        Thread.yield();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Returns our name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the listener attribute of the QuotaIncomingMessageListener object
     *
     * @return The listener value
     */
    public EndpointListener getListener() {
        return listener;
    }

    /**
     * Close this listener and release all of its resources.
     */
    public void close() {
        LinkedList<MessageFromSource> rmdMessages = new LinkedList<MessageFromSource>();

        synchronized (threadDispatcher) {
            if (closed) {
                return;
            }
            closed = true;

            // Unplug the listener right away. We no longer want to invoke it, even if another
            // thread has picked up a message and is about to process it. (this is circumvents
            // bugs in applications that may not expect the listener to be invoked during or
            // slightly after closure. We still make no absolute guarantee about that. Making it
            // 100% sure would create deadlocks).

            listener = null;

            messageQueue.close();

            // close myAccount
            if (myAccount.isIdle()) {
                myAccount.close();
            }

            // Drain the queue into a local list
            // Do not use (pop(0));
            // we do not need to block and since we're not using a
            // synchronizedUnbiasedQueue, we're not supposed to use the
            // timeout based routines; they're inconsistent with the
            // non timeout ones, synch-wise

            MessageFromSource mfs = null;

            while ((mfs = (MessageFromSource) messageQueue.pop()) != null) {
                rmdMessages.add(mfs);
            }

            threadDispatcher.notify(); // makes a contender run earlier
        }

        // Make sure account deletion cannot become a denial of service
        // Give away a time slice.
        Thread.yield();

        synchronized (messageDispatcher) {
            // Explicitly release each message in the queue
            // so that the per-peer accounting is maintained.
            while (!rmdMessages.isEmpty()) {

                MessageFromSource mfs = rmdMessages.removeFirst();

                if (EndpointMeterBuildSettings.ENDPOINT_METERING && (incomingMessageListenerMeter != null)) {
                    incomingMessageListenerMeter.inboundMessageDropped(mfs.msg, System.currentTimeMillis() - mfs.timeReceived);
                }

                mfs.src.inNeed(false);
                mfs.src.releaseQuantity(mfs.size);
                // Check src account idleness here. Idleness status is
                // stable under messageDispatcher synchronization.
                if (mfs.src.isIdle()) {
                    allSources.stickyCacheEntry((CacheEntry) mfs.src.getUserObject(), false);
                }
            }
        }

        rmdMessages = null;
    }

    /**
     * process one message and move on.
     * @return  next listener to process, or null if none remaining
     */
    public QuotaIncomingMessageListener doOne() {
        MessageFromSource mfs = null;

        // Dequeue a message and update the thread's account "need" status.
        synchronized (threadDispatcher) {
            mfs = (MessageFromSource) messageQueue.pop();
            myAccount.inNeed(messageQueue.getCurrentInQueue() != 0);
            threadDispatcher.notify(); // makes a contender run earlier
        }

        // Release the resource to the message account and process.

        // Msg can be null on occasions since we release the lock between
        // picking a listener and dequeing...more than one thread
        // could decide it has work, while there's a single
        // message. Not too easy to avoid.
        if (mfs != null) {
            // We discount that message right now, because we have no idea
            // what resources are going to be kept, freed, allocated in
            // relation to that message or not, until the listener comes
            // back. We cannot assume anything.
            synchronized (messageDispatcher) {
                mfs.src.inNeed(false); // Make sure we won't get to keep it.
                mfs.src.releaseQuantity(mfs.size);
                // Check idleness here. Idleness is stable under
                // messageDispatcher synchronization.
                if (mfs.src.isIdle()) {
                    CacheEntry ce = (CacheEntry) mfs.src.getUserObject();

                    if (null != ce) {
                        allSources.stickyCacheEntry(ce, false);
                    }
                }
            }

            long timeDequeued = 0;

            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (incomingMessageListenerMeter != null)) {
                timeDequeued = System.currentTimeMillis();
                incomingMessageListenerMeter.inboundMessageDeQueued(mfs.msg, timeDequeued - mfs.timeReceived);
            }

            // call the listener for this message
            EndpointListener l = listener;

            if (l != null) {
                // Latch the listener and test it before use. Close() may be racing with us.
                // If it turns out that the application has closed this quota listener by now, 
                // do not invoke the app listener. We cannot be holding the lock while
                // invoking the listener. So, it is possible for this QuotaListener to close
                // between the time we latch the application listener and the time we invoke it.
                // As a result, it is possible, though unlikely that the application listener
                // is invoked after removal. Applications must expect it.  If an application is
                // bogus in that respect we make it unlikely that the bug will ever show itself.
                // This is as far as we can go without creating deadlocks.
                try {
                    l.processIncomingMessage(mfs.msg, mfs.srcAddress, mfs.destAddress);
                    if (EndpointMeterBuildSettings.ENDPOINT_METERING && (incomingMessageListenerMeter != null)) {
                        incomingMessageListenerMeter.inboundMessageProcessed(mfs.msg, System.currentTimeMillis() - timeDequeued);
                    }
                } catch (Throwable ignored) {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "Uncaught Throwable in listener : " + this + "(" + l.getClass().getName() + ")"
                                ,
                                ignored);
                    }
                }
            }
        }

        ResourceAccount next;

        synchronized (threadDispatcher) {
            myAccount.inNeed(messageQueue.getCurrentInQueue() > 0);
            next = myAccount.releaseItem();
            if ((messageQueue.isClosed()) && myAccount.isIdle()) {

                // We have been laid off and it looks like all threads
                // have returned. We can close the shop.
                myAccount.close();
            }
            threadDispatcher.notify(); // makes a contender run earlier
        }

        if (next == null) {
            return null;
        }

        return (QuotaIncomingMessageListener) next.getUserObject();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Try to give a new thread for this message (this listener).
     * Subsequently it will run other listenersaccording to what the dispatcher
     * says.
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        if (messageQueue.isClosed()) {
            return;
        }

        long timeReceived = 0;

        if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
            timeReceived = System.currentTimeMillis();
        }

        ResourceAccount msgSrcAccount;
        // XXX 20040930 bondolo why not use EndpointAddress as key?
        String srcAddrStr = srcAddr.toString();
        CacheEntry ce = null;
        long msgSize = message.getByteLength();

        int attempt = 0;

        while (true) {
            if (attempt > 0) {
                // Give some cpu to upper layers.
                Thread.yield();
            }

            synchronized (messageDispatcher) {
                ce = allSources.getCacheEntry(srcAddrStr);

                if (ce == null) {
                    // Cross-ref the cache entry as the cookie in the account.
                    // we'll need it to efficiently manipulate the purgeability
                    // of the cache entry. Each time we need the cache entry, it
                    // cost us a lookup. Rather do it just once.
                    // At first the user object in the account is just a string
                    // for traces.
                    // We change it when we know what to set.
                    msgSrcAccount = messageDispatcher.newAccount(4 * 10240, -1, srcAddrStr);
                    if (msgSrcAccount.getNbReserved() < 1) {
                        // That's bad ! We must get rid of some stale
                        // accounts. Purge 1/10 of the idle accounts.
                        msgSrcAccount.close();
                        allSources.purge(10);
                        msgSrcAccount = messageDispatcher.newAccount(4 * 10240, -1, "retrying:" + srcAddrStr);
                    }

                    allSources.put(srcAddrStr, msgSrcAccount);

                    ce = allSources.getCacheEntry(srcAddrStr);
                    msgSrcAccount.setUserObject(ce);
                } else {
                    msgSrcAccount = (ResourceAccount) ce.getValue();
                }

                if (!msgSrcAccount.obtainQuantity(msgSize)) {
                    if (++attempt < 2) {
                        // During the retry, we'll give up the cpu. It helps a lot because otherwise input threads can run non-stop
                        // and nothing runs up-top.
                        continue;
                    }

                    // Too many backloged messages from there.
                    // discard right away.
                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                        LOG.info("Peer exceeds queuing limits; msg discarded.");
                    }
                    return;
                }

                // Now, we hold a message resource for that source, so it
                // cannot be purged from the cache.
                allSources.stickyCacheEntry(ce, true);
                break;
            }
        }

        boolean obtained = false;
        boolean pushed = false;

        synchronized (threadDispatcher) {
            do {
                pushed = messageQueue.push(new MessageFromSource(message, srcAddr, dstAddr, msgSrcAccount, timeReceived, msgSize));

                if ((!pushed) && messageQueue.isClosed()) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("queue closed, message discarded");
                    }
                    break;
                }
            } while (!pushed);

            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                int queueLen = messageQueue.getCurrentInQueue();
                long timeNow = TimeUtils.timeNow();

                if ((queueLen > 100) && (TimeUtils.toRelativeTimeMillis(timeNow, lastLongQueueNotification) > TimeUtils.ASECOND)) {
                    lastLongQueueNotification = timeNow;
                    LOG.warning("Very long queue (" + queueLen + ") for listener: " + this);
                }
            }

            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (incomingMessageListenerMeter != null)) {
                incomingMessageListenerMeter.inboundMessageQueued(message);
            }

            if (pushed) {
                obtained = myAccount.obtainItem();
            }
            threadDispatcher.notify(); // makes a contender run earlier
        }

        if (!pushed) {
            // We need to release the resources that we have obtained.
            // The acount cannot have possibly been purged; it is marked
            // sticky and we hold resources. So, we can re-use the cache
            // entry we obtained the last time we had the messageDispatcher
            // modnitor.
            synchronized (messageDispatcher) {
                msgSrcAccount.inNeed(false); // Make sure we won't get to keep it.
                msgSrcAccount.releaseQuantity(msgSize);

                // Check idleness here. Idleness is stable under
                // messageDispatcher synchronization.
                if (msgSrcAccount.isIdle()) {
                    allSources.stickyCacheEntry(ce, false);
                }
            }

            return;
        }

        if (obtained) {
            ListenerExecutorTask task = new ListenerExecutorTask(this);

            ((StdPeerGroup) group).getExecutor().execute(task);
        } else {
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Listener \'" + this + "\' exceeds thread\'s limits; msg waits.");
            }
        }
    }
}
