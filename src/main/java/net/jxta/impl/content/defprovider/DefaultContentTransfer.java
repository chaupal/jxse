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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferCancelledException;
import net.jxta.content.TransferException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.FileDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.content.AbstractContentTransfer;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

/**
 *
 */
public class DefaultContentTransfer extends AbstractContentTransfer
        implements PipeMsgListener {
    /**
     * Logger instance.
     */
    private static final Logger LOG =
            Logger.getLogger(DefaultContentTransfer.class.getName());

    /**
     * The number of seconds between source discovery attempts.
     */
    private static final int SOURCE_LOCATION_INTERVAL =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".sourceLocationInterval", 30).intValue();

    /**
     * The number of of knownSources considered to be "enough".
     */
    private static final int ENOUGH_SOURCES =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".enoughSources", 1).intValue();

    /**
     * The number of of knownSources considered to be "many".
     */
    private static final int MANY_SOURCES =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".manySources", 5).intValue();

    /**
     * The discovery threshold to use.
     */
    private static final int DISCOVERY_THRESHOLD =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".discoveryTreshold", 10).intValue();

    /**
     * Periodic check interval, in seconds.
     */
    private static final int PERIODIC_CHECK_INTERVAL =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".periodicCheckInterval", 5).intValue();

    /**
     * Maximum number of seconds that elapse before the transfer fails for
     * lack of progress.
     */
    private static final int PROGRESS_TIMEOUT =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".progressTimeout", 30).intValue() * 1000;

    /**
     * Pipe resolution timeout, in seconds.
     */
    private static final long PIPE_TIMEOUT =
            Long.getLong(DefaultContentTransfer.class.getName()
            + ".pipeTimeout", 10).longValue() * 1000;

    /**
     * Response timeout, in seconds.
     */
    private static final long RESPONSE_TIMEOUT =
            Long.getLong(DefaultContentTransfer.class.getName()
            + ".responseTimeout", 5).longValue() * 1000;

    /**
     * Maximum number of outstanding requests.
     */
    private static final int MAX_OUTSTANDING =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".maxOutstanding", 3).intValue();

    /**
     * Maximum number of bytes to request at one time.
     */
    private static final int MAX_REQUEST_LENGTH =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".maxRequestLength", 50000).intValue();

    /**
     * Flag to indicate whether or not the Content location code should
     * force message delivery to the local peer.  Used for debug.
     */
    private static final boolean SIMULATE_PACKET_LOSS =
            Boolean.getBoolean(DefaultContentTransfer.class.getName()
            + ".simulatePacketLoss");

    /**
     * Integer indicating what percent of packets should be lost
     * when simulating.  Used for debug.
     */
    private static final int PACKET_LOSS_PERCENT =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".packetLossPercent", 30).intValue();

    /**
     * Maximum incoming message queue size.  Once full, additional incoming
     * requests will be dropped.
     */
    private static final int MAX_QUEUE_SIZE =
            Integer.getInteger(DefaultContentTransfer.class.getName()
            + ".maxQueue", MAX_OUTSTANDING * 2).intValue();

    /**
     * Random number generator used when simulating packet loss.
     */
    private static final Random RANDOM =
            SIMULATE_PACKET_LOSS ? new Random() : null;

    /**
     * Exception to throw when progress stops for too long.
     */
    private static final TransferException STALLED =
            new TransferException("Transfer stalled");

    // Initialized at construction
    private final ScheduledExecutorService executor;
    private final PeerGroup peerGroup;
    private final List<Node> outstanding = new CopyOnWriteArrayList<Node>();
    private final BlockingQueue<PipeMsgEvent> msgQueue =
            new ArrayBlockingQueue<PipeMsgEvent>(MAX_QUEUE_SIZE);

    // Managed over the course of the transfer
    private List<DefaultContentShareAdvertisementImpl> sourcesRemaining =
            new ArrayList<DefaultContentShareAdvertisementImpl>();
    private List<DefaultContentShareAdvertisementImpl> sourcesTried =
            new ArrayList<DefaultContentShareAdvertisementImpl>();

    // Initialized via transferInit()
    private ScheduledFuture periodicTask;
    private PipeAdvertisement responsePipeAdv;
    private InputPipe responsePipe;
    private Content content;
    private TransferException toThrow;
    private boolean running;

    // Managed by the worker thread and periodic threads after initialiation
    private Thread ownerThread = null;
    private OutputPipe sourcePipe;
    private BufferedOutputStream out;
    private long lastProgress;
    private long request;
    private long written;
    private long eofOffset;
    private boolean doPeriodic;

    //////////////////////////////////////////////////////////////////////////
    // Inner classes:

    /**
     * Struct to track requests and responses.
     */
    private static class Node {
        public long timeStamp;
        public long offset;
        public int length;
        public byte[] data;

        @Override
        public String toString() {
            String result = "[Node timeStamp=" + timeStamp
                    + ", offset=" + offset
                    + ", length=" + length;
            if (data != null) {
                result += ", dataLength=" + data.length;
            }
            return result + "]";
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Constructor for use with ContentShareAdvertisements.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param schedExecutor executor to use when running tasks
     * @param group parent peer group
     * @param contentAdv content that we want to retrieve
     */
    public DefaultContentTransfer(
            DefaultContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentShareAdvertisement contentAdv) {
        super(origin, schedExecutor, group, contentAdv, "DefaultContentTransfer");
        setSourceLocationInterval(SOURCE_LOCATION_INTERVAL);
        setDiscoveryThreshold(DISCOVERY_THRESHOLD);
        executor = schedExecutor;
        peerGroup = group;
    }

    /**
     * Constructor for use with ContentIDs.
     *
     * @param origin content provider which created and manager this
     *  transfer
     * @param schedExecutor executor to use when running tasks
     * @param group parent peer group
     * @param contentID ID of the content that we want to retrieve
     */
    public DefaultContentTransfer(
            DefaultContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentID contentID) {
        super(origin, schedExecutor, group, contentID, "DefaultContentTransfer");
        setSourceLocationInterval(SOURCE_LOCATION_INTERVAL);
        setDiscoveryThreshold(DISCOVERY_THRESHOLD);
        executor = schedExecutor;
        peerGroup = group;
    }

    //////////////////////////////////////////////////////////////////////////
    // AbstractContentTransfer methods:

    /**
     * {@inheritDoc}
     */
    protected int getEnoughLocationCount() {
        return ENOUGH_SOURCES;
    }

    /**
     * {@inheritDoc}
     */
    protected int getManyLocationCount() {
        return MANY_SOURCES;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isAdvertisementOfUse(ContentShareAdvertisement adv) {
        return (adv instanceof DefaultContentShareAdvertisementImpl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws TransferException when a problem occurs during transfer attempt
     */
    protected ContentTransferState transferAttempt(
            File dest,
            List<ContentShareAdvertisement> sources,
            List<ContentShareAdvertisement> newSources)
            throws TransferException {

        // Add new sources to our tracked list
        for (ContentShareAdvertisement candidate : newSources) {
            if (candidate instanceof DefaultContentShareAdvertisementImpl) {
                sourcesRemaining.add(
                        (DefaultContentShareAdvertisementImpl) candidate);
            }
        }

        Logging.logCheckedFine(LOG, "Sources remaining: ", sourcesRemaining.size());
        Logging.logCheckedFine(LOG, "Sources tried    : ", sourcesTried.size());

        if (sourcesRemaining.size() == 0) {

            Logging.logCheckedFine(LOG, "No sources remaining to try");
            return ContentTransferState.STALLED;

            /* Another option:
            LOG.fine("Resetting remaining/tried lists");
            sourcesRemaining.addAll(sourcesTried);
            sourcesTried.clear();
             */
        }

        // Find a share adv we can use:
        DefaultContentShareAdvertisementImpl adv = null;
        sourcePipe = null;
        do {
            if (sourcesRemaining.size() == 0) {
                break;
            }
            adv = sourcesRemaining.remove(0);
            sourcesTried.add(adv);

            try {

                PipeService pipeService = peerGroup.getPipeService();
                sourcePipe = pipeService.createOutputPipe(
                        adv.getPipeAdvertisement(), PIPE_TIMEOUT);

            } catch (IOException iox) {

                Logging.logCheckedWarning(LOG, "Could not resolve source pipe for Source: ",
                    adv.getPipeAdvertisement(), iox);

                adv = null;

            }

        } while (adv == null);

        if (adv == null) throw(new TransferException("Could not find usable source"));

        Logging.logCheckedFine(LOG, "Source selected: ", adv);

        try {
            transferInit(dest);
            processMessages();

            criticalEntry();
            try {
                if (toThrow == STALLED) {
                    return ContentTransferState.STALLED;
                }
            } finally {
                criticalExit();
            }
        } catch (InterruptedException intx) {

            Thread.interrupted();
            throw(new TransferException("Transfer interrupted", intx));

        } finally {

            try {

                transferCleanup(dest, adv);

            } catch (InterruptedException intx) {

                Logging.logCheckedWarning(LOG, "Interrupted prior to post-cleanup\n", intx);
                Thread.interrupted();

            }
        }

        // We should only get here on success
        setContent(content);
        return ContentTransferState.COMPLETED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        super.cancel();
        synchronized(this) {
            running = false;
            toThrow = new TransferCancelledException();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // PipeMsgListener interface methods:

    /**
     * Processes incoming Content service requests.
     *
     * @param pme pipe message event received
     */
    public void pipeMsgEvent(PipeMsgEvent pme) {
        synchronized(this) {
            if (running) {
                msgQueue.offer(pme);
            } else {
                msgQueue.clear();
            }
            notifyAll();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Initialize member vars for transfer.
     */
    private void transferInit(File dataFile)
            throws TransferException, InterruptedException  {
        criticalEntry();
        try {
            synchronized(this) {
                running = true;
                content = null;
                toThrow = null;

                setupResponsePipe();

                // Initialized transfer data
                outstanding.clear();
                for (int i=0; i<MAX_OUTSTANDING; i++) {
                    outstanding.add(new Node());
                }

                // Start up periodic health check
                if (periodicTask == null || periodicTask.isDone()) {

                    Logging.logCheckedFine(LOG, "Setting up periodicTask");

                    periodicTask = executor.scheduleWithFixedDelay(

                        new Runnable() {

                            public void run() {
                                try {
                                    criticalEntry();
                                    periodicCheck();
                                } catch (InterruptedException intx) {
                                    Logging.logCheckedFinest(LOG, "Periodic check interrupted\n", intx);
                                } finally {
                                    criticalExit();
                                }
                            }

                        }, 0, PERIODIC_CHECK_INTERVAL, TimeUnit.SECONDS);

                }
            }

            try {
                out = new BufferedOutputStream(new FileOutputStream(dataFile));
            } catch (FileNotFoundException filex) {
                throw(new TransferException(
                        "Could not initialize transfer", filex));
            }

            written = 0;
            request = 0;
            eofOffset = -1;
            lastProgress = System.currentTimeMillis();
        } finally {
            criticalExit();
        }
    }

    /**
     * Cleanup member vars post-transfer.
     */
    private void transferCleanup(File dataFile, ContentShareAdvertisement adv)
            throws TransferException, InterruptedException {
        criticalEntry();
        try {

            synchronized(this) {
                outstanding.clear();

                periodicTask.cancel(false);
                periodicTask = null;

                // Check for latent exceptions (i.e., cancellations)
                if (toThrow != null && toThrow != STALLED) {
                    TransferException toThrowRef = toThrow;
                    toThrow = null;
                    throw toThrowRef;
                }

                if (written == eofOffset && out != null) {
                    // Persist our Content
                    try {
                        out.close();
                        ContentAdvertisement cAdv =
                                adv.getContentAdvertisement();
                        content = new Content(
                                cAdv.getContentID(),
                                cAdv.getMetaID(),
                                new FileDocument(dataFile, cAdv.getMimeType())
                                ) ;
                    } catch (IOException iox) {
                        out = null;
                        throw(new TransferException(
                                "Could not close data file", iox));
                    }
                } else {
                    // Cleanup the data file
                    content = null;
                    dataFile.delete();
                }
            }

            if (sourcePipe != null) {
                sourcePipe.close();
                sourcePipe = null;
            }

            out = null;
            written = 0;
            request = 0;
            running = false;
        } finally {
            criticalExit();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods protected by critical section methods:

    /**
     * Creates the input pipe and associated advertisement used to receive
     * Content requests from remote peers.
     */
    private void setupResponsePipe() {
        PipeService pipeService;
        PipeID pipeID;

        if (responsePipeAdv == null) {
            responsePipeAdv =
                    (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                    PipeAdvertisement.getAdvertisementType());
            responsePipeAdv.setType(PipeService.UnicastType);

            pipeID = IDFactory.newPipeID(peerGroup.getPeerGroupID());
            responsePipeAdv.setPipeID(pipeID);
        }

        if (responsePipe == null) {

            try {

                pipeService = peerGroup.getPipeService();
                responsePipe = pipeService.createInputPipe(responsePipeAdv, this);

            } catch (IOException iox) {

                Logging.logCheckedWarning(LOG, "Could not create input pipe\n", iox);
                responsePipe = null;

            }

        }
    }

    /**
     * Reentrant method used for multi-threaded processing of incoming
     * messages.
     *
     * @throws InterruptedException if interupted while waiting for incoming
     *  messages
     */
    private void processMessages() throws InterruptedException {

        List<PipeMsgEvent> workQueue = new ArrayList<PipeMsgEvent>();
        Message msg;
        long fireWritten = -1;
        long lastWritten = 0;

        Logging.logCheckedFine(LOG, "Worker thread starting");

        while (running) {
            workQueue.clear();
            synchronized(this) {
                int count = msgQueue.drainTo(workQueue);
                if (count == 0) {
                    wait();
                    continue;
                }
            }

            criticalEntry();
            try {
                doPeriodic = false;
                for (PipeMsgEvent pme : workQueue) {

                    msg = pme.getMessage();

                    try {
                        processMessage(msg);
                    } catch (Exception x) {
                        Logging.logCheckedWarning(LOG, "Uncaught exception\n", x);
                    }

                }

                if (written != lastWritten) {
                    lastWritten = written;
                    fireWritten = written;
                }
                if (doPeriodic) {
                    periodicCheck();
                }
            } finally {
                criticalExit();
            }

            if (fireWritten >= 0) {
                fireTransferProgress(written);
            }
        }

        Logging.logCheckedFine(LOG, "Worker thread closing up shop");

    }

    /**
     * Processes incoming Content service responses.
     *
     * @param msg message received
     */
    public void processMessage(Message msg) {
        ByteArrayMessageElement bmsge;
        MessageElement msge;
        ListIterator it;
        StructuredDocument doc;
        DataResponse resp;
        byte data[] = null;

        Logging.logCheckedFiner(LOG, "Incoming message: ", msg);

        it = msg.getMessageElementsOfNamespace(DefaultContentProvider.MSG_NAMESPACE);

        if (!it.hasNext()) {

            Logging.logCheckedWarning(LOG, "Unknown message structure");
            return;

        }

        msge = (MessageElement) it.next();

        if (!DefaultContentProvider.MSG_ELEM_NAME.equals(msge.getElementName())) {

            Logging.logCheckedWarning(LOG, "Not a data response: ", msge.getElementName());

            // Not a data response
            return;

        }

        try {

            doc = StructuredDocumentFactory.newStructuredDocument(msge);
            resp = new DataResponse(doc);

        } catch (IOException iox) {

            Logging.logCheckedWarning(LOG, "Could not process message\n", iox);
            return;

        }

        if (it.hasNext()) {

            try {

                bmsge = (ByteArrayMessageElement) it.next();
                data = bmsge.getBytes();

            } catch (ClassCastException ccx) {

                Logging.logCheckedWarning(LOG, "Second message element not byte array\n", ccx);

            }
        }

        processDataResponse(resp, data);
    }

    /**
     * Notify listeners of a change in the source location state.
     */
    private void fireTransferProgress(long received) {
        ContentTransferEvent event = null;
        for (ContentTransferListener listener : getContentTransferListeners()) {
            if (event == null) {
                event = new ContentTransferEvent.Builder(this)
                        .bytesReceived(received)
                        .build();
            }
            listener.contentTransferProgress(event);
        }
    }

    /**
     * Ensures that the transfer stays healthy.
     */
    private void periodicCheck() throws InterruptedException {

        long millis = System.currentTimeMillis();

        Logging.logCheckedFiner(LOG, "Peridiodic check starting");

        int attempt=0;
        int maxAttempts = outstanding.size();
        boolean progress = true;

        while (progress && attempt++ < maxAttempts) {

            Logging.logCheckedFiner(LOG, "Periodic check attempt #", attempt,
                        " (", maxAttempts, " max)");

            progress = false;
            int i=0;
            int inUse=0;

            for (Node node : outstanding) {

                Logging.logCheckedFiner(LOG, "Evaluating status of Node #", i, ": ", node);

                if (0 == node.timeStamp) {

                    // Node not in use
                    Logging.logCheckedFiner(LOG, "  Node not in use.");

                    if (prepareRequest(node)) {

                        Logging.logCheckedFiner(LOG, "  Node repurposed for request: ", node);

                        progress = true;
                        inUse++;
                        sendRequest(node, i);

                    }

                } else if (node.data != null) {

                    // Node has data, but data hasnt been written out yet.
                    Logging.logCheckedFiner(LOG, "  Node awaiting data write-out.");

                    if (checkWrite(node)) {

                        Logging.logCheckedFiner(LOG, "  Data written.");

                        if (prepareRequest(node)) {

                            Logging.logCheckedFiner(LOG, "  Node repurposed for request: ", node);

                            progress = true;
                            inUse++;
                            sendRequest(node, i);

                        }

                    } else {

                        // Cant write yet.
                        Logging.logCheckedFiner(LOG, "  Can't write yet.");
                        inUse++;

                    }

                } else if (millis - node.timeStamp > RESPONSE_TIMEOUT) {

                    // Request timed out
                    Logging.logCheckedFiner(LOG, "  Timeout detected.");

                    boolean beyondEOF = (eofOffset >= 0) && (eofOffset <= node.offset);

                    if (beyondEOF) {

                        Logging.logCheckedFiner(LOG, "  Request is beyond known EOF. Resetting.");
                        progress = true;
                        node.timeStamp = 0;

                    } else {

                        Logging.logCheckedFiner(LOG, "  Resending request.");
                        inUse++;
                        sendRequest(node, i);

                    }

                } else {

                    // Request outstanding.
                    if (eofOffset >= 0 && eofOffset <= node.offset) {

                        Logging.logCheckedFiner(LOG, "  Request is beyond known EOF. Resetting.");
                        progress = true;
                        node.timeStamp = 0;

                    } else {

                        Logging.logCheckedFiner(LOG, "  Request outstanding.");
                        inUse++;

                    }
                }

                i++;
            }

            if (inUse == 0 && eofOffset >= 0) {

                // We're done.
                Logging.logCheckedFine(LOG, "Transfer complete");

                synchronized(this) {
                    running = false;
                    notifyAll();
                }

                break;

            }

        }

        long timeSinceProgress = System.currentTimeMillis() - lastProgress;
        if (timeSinceProgress > PROGRESS_TIMEOUT) {
            synchronized(this) {
                toThrow = STALLED;
                running = false;
                notifyAll();
            }
        }

        Logging.logCheckedFiner(LOG, "Peridiodic check completed");

    }

    /**
     * Sets up the specified node with the details of the next request
     * needed.
     */
    private boolean prepareRequest(Node node) {
        node.timeStamp = System.currentTimeMillis();

        // Was the last request on this node completely successful?
        if (node.data != null) {
            if (node.data.length != node.length) {
                // We need to request the remaining data
                node.offset += node.data.length;
                if (eofOffset >= 0 && node.offset >= eofOffset) {
                    // No need to request any more data.
                    node.timeStamp = 0;
                    return false;
                }
                node.length -= node.data.length;
                node.data = null;
                return true;
            }
            node.data = null;
        }

        if (eofOffset >= 0) {
            // No more data to request or nobody to request from.Mark as unused.
            node.timeStamp = 0;
            return false;
        }

        // Request next segment.
        node.offset = request;
        node.length = MAX_REQUEST_LENGTH;
        request += node.length;
        return true;
    }

    /**
     * Sends a request to a remote peer.
     */
    private void sendRequest(Node node, int idx) {

        XMLDocument doc;
        DataRequest req;
        Message msg;

        if (null == sourcePipe) {
            Logging.logCheckedFine(LOG, "No source pipe available.  Deferring node: ", node);
            node.timeStamp = 1;
            return;
        }

        node.timeStamp = System.currentTimeMillis();
        req = new DataRequest();
        req.setContentID(getTransferContentID());
        req.setOffset(node.offset);
        req.setLength(node.length);
        req.setQueryID(idx);
        req.setResponsePipe(responsePipeAdv);

        doc = (XMLDocument) req.getDocument(MimeMediaType.XMLUTF8);
        MessageElement msge = new TextDocumentMessageElement(
                DefaultContentProvider.MSG_ELEM_NAME, doc, null);
        msg = new Message();
        msg.addMessageElement(DefaultContentProvider.MSG_NAMESPACE, msge);

        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            Logging.logCheckedFinest(LOG, "Sending DataRequest (idx=", idx, ", node=", node, "):");
            Logging.logCheckedFinest(LOG, "   ContentID: ", req.getContentID());
            Logging.logCheckedFinest(LOG, "   Offset : ", req.getOffset());
            Logging.logCheckedFinest(LOG, "   Length : ", req.getLength());
            Logging.logCheckedFinest(LOG, "   QID    : ", req.getQueryID());
        }

        try {

            if (sourcePipe.send(msg)) return;

        } catch (IOException iox) {

            Logging.logCheckedWarning(LOG, "IOException during message send\n", iox);

        }

        Logging.logCheckedFiner(LOG, "Did not send message");
        node.timeStamp = 1;

    }

    /**
     * Process an incoming data response.
     */
    private void processDataResponse(DataResponse resp, byte[] data) {
        Node node;
        int idx;
        long offs;

        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            Logging.logCheckedFinest(LOG, "DataResponse:");
            Logging.logCheckedFinest(LOG, "   ContentID: ", resp.getContentID());
            Logging.logCheckedFinest(LOG, "   Offset : ", resp.getOffset());
            Logging.logCheckedFinest(LOG, "   Length : ", resp.getLength());
            Logging.logCheckedFinest(LOG, "   QID    : ", resp.getQueryID());
            Logging.logCheckedFinest(LOG, "   EOF    : ", resp.getEOF());
            Logging.logCheckedFinest(LOG, "   Bytes  : ", ((data == null) ? 0 : data.length));
        }

        if (!resp.getContentID().equals(getTransferContentID())) {

            Logging.logCheckedWarning(LOG, "Invalid ContentID.  Discarding.");
            Logging.logCheckedFinest(LOG, "Expected ContentID: ", getTransferContentID());
            return;

        }

        if (resp.getLength() != ((data == null) ? 0 : data.length)) {

            Logging.logCheckedWarning(LOG, "Data length doesnt match length in header.  Discarding.");
            Logging.logCheckedFinest(LOG, "Expected length: ", ((data == null) ? 0 : data.length));
            return;

        }

        idx = resp.getQueryID();

        if (idx >= outstanding.size()) {

            Logging.logCheckedWarning(LOG, "Invalid query ID.  Discarding.");
            Logging.logCheckedFinest(LOG, "Expected max: ", outstanding.size());
            return;

        }

        node = outstanding.get(idx);

        if (node == null) {

            Logging.logCheckedWarning(LOG, "Null node.  Discarding");
            return;

        }

        boolean couldWrite;

        if (resp.getOffset() != node.offset) {

            Logging.logCheckedWarning(LOG, "Invalid offset. Discarding.");
            Logging.logCheckedFinest(LOG, "Expected offset: ", node.offset);
            return;

        }

        // We have what appears to be a good packet.
        if (SIMULATE_PACKET_LOSS) {
            if (RANDOM.nextInt(100) < PACKET_LOSS_PERCENT) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    Logging.logCheckedFine(LOG, "Simulating lost packet");
                    return;
                }
            }
        }

        // We made some progress
        lastProgress = System.currentTimeMillis();

        node.data = data;
        if (resp.getEOF()) {
            offs = node.offset;
            if (null != node.data) {
                offs += node.data.length;
            }
            // Update the best known EOF offset
            if (eofOffset < 0 || offs < eofOffset) {
                eofOffset = offs;
            }
        }
        couldWrite = checkWrite(node);
        if (couldWrite) {

            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                Logging.logCheckedFinest(LOG, "Wrote the following to disk:");
                Logging.logCheckedFinest(LOG, "   Offset : ", resp.getOffset());
                Logging.logCheckedFinest(LOG, "   Length : ", resp.getLength());
                Logging.logCheckedFinest(LOG, "   QID    : ", resp.getQueryID());
                Logging.logCheckedFinest(LOG, "   EOF    : ", resp.getEOF());
                Logging.logCheckedFinest(LOG, "   Bytes  : ", ((data == null) ? 0 : data.length));
            }

            if (prepareRequest(node)) {
                sendRequest(node, idx);
            }

        } else {

            // Signal an evaluation all Nodes
            doPeriodic = true;

        }

        if (eofOffset >= 0) {
            // If we've seen an EOF marker, cause a check
            doPeriodic = true;
        }
    }

    /**
     * If the offset for this node's data is the same as the next byte
     * which needs to be written to disk, write this node's data to disk.
     */
    private boolean checkWrite(Node node) {
        if (node.offset != written) {
            // Cant do anything about this now.
            return false;
        }

        // Write data to disk.
        node.timeStamp = 0;
        if (node.data == null) {
            return true;
        }

        try {

            out.write(node.data, 0, node.data.length);

        } catch (IOException iox) {

            // We'll implicitly try again later
            Logging.logCheckedFinest(LOG, "Could not write data\n", iox);
            return false;

        }

        // If we got here, we wrote data.
        written += node.data.length;

        return true;
    }

    /**
     * Used by the periodic execution and message processing threads to
     * protect access to shared code to help minimize the amount of
     * synchronization required.  This method blocks until the executing
     * thread has become the critical section owner.
     */
    private void criticalEntry() throws InterruptedException {

        Thread me = Thread.currentThread();

        synchronized(this) {

            while (ownerThread != null && ownerThread != me) {
                Logging.logCheckedFinest(LOG, "Waiting for access to critical section");
                wait();
            }

            ownerThread = me;

        }

        Logging.logCheckedFinest(LOG, "Access to critical section granted");

    }

    /**
     * Used by the periodic execution and message processing threads to
     * protect access to shared code to help minimize the amount of
     * synchronizatoin required.  This method releases the lock on the
     * shared/critical code section.
     */
    private void criticalExit() {

        Logging.logCheckedFinest(LOG, "Releasing access to critical section");

        Thread me = Thread.currentThread();

        synchronized(this) {
            if (ownerThread == me) {
                ownerThread = null;
                notifyAll();
            }
        }

    }

}
