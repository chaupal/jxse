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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentShare;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentProviderEvent;
import net.jxta.content.ContentProviderListener;
import net.jxta.content.NullContentTransfer;
import net.jxta.endpoint.Message;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.content.ModuleWrapperFactory;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

/**
 * Reference implementation of the ContentService.  This implementation works
 * as follows:
 * <ul>
 *      <li>
 *          Being the default/fallback implementation, this provider makes no
 *          assumptions as to the data contained within the Content.  It may be
 *          static or dynamic content and may or may not be different across
 *          multiple peers who are sharing the same Content ID.  Each client
 *          will be served using a single call to a <code>Content</code>'s
 *          stream.
 *      </li>
 *      <li>
 *          Content is shared by calling the shareContent(Content) method and
 *          then making another peer aware of the returned
 *          ContentShareAdvertisement.  How the remote peer is made aware of
 *          the ContentShareAdvertisement is of no concern.
 *      </li>
 *      <li>
 *          No validation is performed on the transferred data.  All validation
 *          must be done out-of-band and post-transfer.
 *      </li>
 *      <li>
 *          Service to remote peers will be performed internally as follows:
 *          <ul>
 *              <li>
 *                  Requestors who have requested data first will receive higher
 *                  priority on subsequent requests.  This biases responses
 *                  toward a single remote peer, allowing that peer to complete
 *                  as quickly as possible.
 *              </li>
 *              <li>
 *                  Only a certain number of requestors will be able to be
 *                  served concurrently.  Those requestors who cannot be served
 *                  will be completely ignored.
 *              </li>
 *              <li>
 *                  The requestors are expected, though not required, to
 *                  access data linearly.
 *              </li>
 *          </ul>
 *      </li>
 * </ul>
 *
 * See the <code>DataRequest</code> and <code>DataResponse</code>
 * classes for the protocol description.
 *
 * This implementation currently uses a rather bogus method for locating
 * data sources.  This bogus implementation amounts to a periodic
 * ResolverService-based broadcast and should be rewritten to use a
 * subscription service of some sort.
 */
public class DefaultContentProvider implements
        ContentProviderSPI, PipeMsgListener, ActiveTransferTrackerListener {
    /**
     * Logger instance.
     */
    private static final Logger LOG =
            Logger.getLogger(DefaultContentProvider.class.getName());

    /**
     * Maximum incoming message queue size.  Once full, additional incoming
     * requests will be dropped.
     */
    private static final int MAX_QUEUE_SIZE =
            Integer.getInteger(DefaultContentProvider.class.getName()
            + ".maxQueue", 256).intValue();

    /**
     * Message namespace used to identify our message elements.
     */
    protected static final String MSG_NAMESPACE = "DefCont";

    /**
     * Message element name used to identify our data requests/responses.
     */
    protected static final String MSG_ELEM_NAME = "DR";

    /**
     * Module spec ID for this provider.
     */
    private static final String MODULE_SPEC_ID =
            "urn:jxta:uuid-AC3AA08FC4A14C15A78A88B4D4F87554"
            + "CDC361792F3F4EF9A6488BE56396AAEB06";

    /**
     * Parsed and ready-to-use version of MODULE_SPEC_ID.
     */
    private static final ModuleSpecID specID;
    
    // Initialized at construction
    private final Map<ID, DefaultContentShare> shares =
            new HashMap<ID, DefaultContentShare>();
    private final CopyOnWriteArrayList<ContentProviderListener> listeners =
            new CopyOnWriteArrayList<ContentProviderListener>();
    private final Queue<PipeMsgEvent> msgQueue =
            new ArrayBlockingQueue<PipeMsgEvent>(MAX_QUEUE_SIZE);

    // Initialized by init
    private PeerGroup peerGroup;
    private ScheduledExecutorService executor;
    private PipeAdvertisement pipeAdv;

    // Initialized and managed by start/stop
    private ActiveTransferTracker tracker;
    private InputPipe requestPipe;
    private boolean running = false;

    //////////////////////////////////////////////////////////////////////////
    // Inner classes:

    /**
     * Executor thread factory to configure reasonable thread names and
     * settings, etc..
     */
    private class ThreadFactoryImpl
            implements ThreadFactory, UncaughtExceptionHandler {
        private ThreadGroup threadGroup;

        public ThreadFactoryImpl(PeerGroup group) {
            StringBuilder name = new StringBuilder();
            name.append(group.getPeerGroupName());
            name.append(" - ");
            name.append(DefaultContentProvider.class.getName());
            name.append(" pool");

            threadGroup = new ThreadGroup(name.toString());
            threadGroup.setDaemon(true);
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(threadGroup, runnable);
            thread.setUncaughtExceptionHandler(this);
            return thread;
        }

        public void uncaughtException(Thread thread, Throwable throwable) {
            
            Logging.logCheckedSevere(LOG, "Uncaught throwable in pool thread: " + thread, throwable);
            
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Constructors and initializers:
    
    /**
     * Static initializer.
     */
    static {
        try {
            URI specURI = new URI(MODULE_SPEC_ID);
            specID = (ModuleSpecID) IDFactory.fromURI(specURI);
        } catch (URISyntaxException urisx) {
            throw(new RuntimeException(
                    "Illegal ModuleSpecURI in code: " + MODULE_SPEC_ID,
                    urisx));
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // ContentProviderSPI interface methods:

    /**
     * {@inheritDoc}
     */
    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) {

        Logging.logCheckedFine(LOG, "initProvider(): group=" + group);
        
        peerGroup = group;
        executor = Executors.newScheduledThreadPool(
                5, new ThreadFactoryImpl(group));

        pipeAdv =
                (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                PipeAdvertisement.getAdvertisementType());
        pipeAdv.setType(PipeService.UnicastType);

        PipeID pipeID = IDFactory.newPipeID(peerGroup.getPeerGroupID());
        pipeAdv.setPipeID(pipeID);

        tracker = new ActiveTransferTracker(group, executor);
        tracker.addActiveTransferListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int startApp(String[] args) {

        Logging.logCheckedFine(LOG, "startApp()");
        
        if (running) return Module.START_OK;
        
        running = true;

        if (requestPipe == null) {

            try {

                PipeService pipeService = peerGroup.getPipeService();

                if (pipeService == null) {

                    Logging.logCheckedWarning(LOG, "Stalled until there is a pipe service");
                    return Module.START_AGAIN_STALLED;

                }

                requestPipe = pipeService.createInputPipe(pipeAdv, this);

            } catch (IOException iox) {

                Logging.logCheckedWarning(LOG, "Could not create input pipe", iox);
                requestPipe = null;
                return Module.START_AGAIN_STALLED;

            }
        }

        // Start the accept loop
        executor.execute(new Runnable() {
            public void run() {
                try {
                    processMessages();
                } catch (InterruptedException intx) {
                    Logging.logCheckedFine(LOG, "Interrupted\n" + intx.toString());
                    Thread.interrupted();
                }
            }
        });

        tracker.start();

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stopApp() {
        
        Logging.logCheckedFine(LOG, "stopApp()");
        
        if (!running) return;
        
        tracker.stop();
        msgQueue.clear();

        /*
         * XXX 20070911 mcumings: We really need to be able to abort all
         * ContentTransfer instances that we've created that are still
         * in-flight.  Right now the ContentTransfers will silently
         * fail if the ScheduledExecutorService is shutdown while the
         * transfer is in-flight.  I don't like the idea of maintaining
         * references to every ContentTransfer instance, but I also don't
         * like the idea of each instance using it's own dedicated thread.
         * Suggestions?
         */
        
        running = false;
        notifyAll();
    }
    
    /**
     * {@inheritDoc}
     */
    public ContentProviderSPI getInterface() {
        return  (ContentProviderSPI) ModuleWrapperFactory.newWrapper(
                new Class[] { ContentProviderSPI.class },
                this);        
    }

    /**
     * {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        ModuleImplAdvertisement adv =
                (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());
        adv.setModuleSpecID(specID);
        adv.setCode(getClass().getName());
        adv.setProvider("https://jxta.dev.java.net/");
        adv.setDescription("ContentProvider implementation using a simple, "
                + "proprietary, portable, but slow protocol");
        
        return adv;
    }


    //////////////////////////////////////////////////////////////////////////
    // ContentProvider interface methods:

    /**
     * {@inheritDoc}
     */
    public void addContentProviderListener(ContentProviderListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentProviderListener(
            ContentProviderListener listener) {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransfer retrieveContent(ContentID contentID) {

        Logging.logCheckedFine(LOG, "retrieveContent(" + contentID + ")");
        
        synchronized(this) {
            if (!running) return null;
        }

        synchronized(shares) {
            ContentShare share = getShare(contentID);
            if (share != null) {
                return new NullContentTransfer(this, share.getContent());
            }
        }
        return new DefaultContentTransfer(this, executor, peerGroup, contentID);
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransfer retrieveContent(ContentShareAdvertisement adv) {

        Logging.logCheckedFine(LOG, "retrieveContent(" + adv + ")");
        
        synchronized(this) {
            if (!running) return null;
        }

        synchronized(shares) {
            ContentShare share = getShare(adv.getContentID());
            if (share != null) {
                return new NullContentTransfer(this, share.getContent());
            }
        }
        return new DefaultContentTransfer(this, executor, peerGroup, adv);
    }

    /**
     * {@inheritDoc}
     */
    public List<ContentShare> shareContent(Content content) {

        Logging.logCheckedFine(LOG, "shareContent(): Content=" + content + " " + this);

        PipeAdvertisement pAdv;

        synchronized(this) {
            pAdv = pipeAdv;
        }

        if (pipeAdv == null) {
            Logging.logCheckedFine(LOG, "Cannot create share before initialization");
            return null;
        }

        List<ContentShare> result = new ArrayList<ContentShare>(1);
        ID id = content.getContentID();
        DefaultContentShare share;
        synchronized(shares) {
            share = getShare(id);
            if (share == null) {
                share = new DefaultContentShare(this, content, pAdv);
                shares.put(id, share);
                result.add(share);
            }
        }

        if (result.size() == 0) {
            /*
             * This content was already shared.  We'll skip notifying our
             * listeners but will return it in the results.
             */
            result.add(share);
        } else {
            fireContentShared(result);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unshareContent(ContentID contentID) {

        Logging.logCheckedFine(LOG, "unhareContent(): ContentID=" + contentID);
        
        ContentShare oldShare;
        synchronized(shares) {
            oldShare = shares.remove(contentID);
        }
        if (oldShare == null) {
            return false;
        } else {
            fireContentUnshared(contentID);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void findContentShares(
            int maxNum, ContentProviderListener listener) {
        List<ContentShare> shareList = new ArrayList<ContentShare>();

        synchronized(shares) {
            shareList = new ArrayList<ContentShare>(
                    Math.min(maxNum, shares.size()));
            for (ContentShare share: shares.values()) {
                if (shareList.size() >= maxNum) {
                    break;
                }
                shareList.add(share);
            }
        }

        listener.contentSharesFound(
                new ContentProviderEvent.Builder(this, shareList)
                    .lastRecord(true)
                    .build());
    }

    //////////////////////////////////////////////////////////////////////////
    // Package methods:

    /**
     * Returns the peer peerGroup the service is running in.
     * 
     * @return peer group the service is running in
     */
    protected PeerGroup getPeerGroup() {
        return peerGroup;
    }

    //////////////////////////////////////////////////////////////////////////
    // PipeMsgListener interface methods:

    /**
     * Processes incoming Content service requests.
     * 
     * @param pme incoming pipe message event
     */
    public synchronized void pipeMsgEvent(PipeMsgEvent pme) {
        if (!running) {
            return;
        }

        if (msgQueue.offer(pme)) {
            notifyAll();
        } else {
            Logging.logCheckedFine(LOG, "Dropped message due to full queue");
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // ActiveTransferTrackerListener interface methods:

    /**
     * {@inheritDoc}
     */
    public void sessionCreated(ActiveTransfer transfer) {
        DefaultContentShare share = transfer.getContentShare();
        share.fireShareSessionOpened(transfer);
    }

    /**
     * {@inheritDoc}
     */
    public void sessionCollected(ActiveTransfer transfer) {
        DefaultContentShare share = transfer.getContentShare();
        share.fireShareSessionClosed(transfer);
    }

    //////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Returns the content share entry for the specified ContentID.
     *
     * @return content share
     */
    private DefaultContentShare getShare(ID id) {
        synchronized(shares) {
            return shares.get(id);
        }
    }

    /**
     * Reentrant method used for multi-threaded processing of incoming
     * messages.  This method and all those it calls must remain perfectly
     * reentrant.
     */
    private void processMessages() throws InterruptedException {

        PipeMsgEvent pme;
        Message msg;

        Logging.logCheckedFine(LOG, "Worker thread starting");
        
        while (true) {
            synchronized(this) {
                if (!running) {
                    break;
                }

                pme = msgQueue.poll();
                if (pme == null) {
                    wait();
                    continue;
                }
            }

            try {

                msg = pme.getMessage();
                processMessage(msg);

            } catch (Exception x) {

                Logging.logCheckedWarning(LOG, "Uncaught exception", x);
                
            }
        }

        Logging.logCheckedFine(LOG, "Worker thread closing up shop");

    }

    /**
     * Process the incoming message.
     */
    private void processMessage(Message msg) {

        MessageElement msge;
        ListIterator it;
        StructuredDocument doc;
        DataRequest req;

        Logging.logCheckedFinest(LOG, "Incoming message:\n" + msg.toString() + "\n");

        it = msg.getMessageElementsOfNamespace(MSG_NAMESPACE);

        while (it.hasNext()) {

            msge = (MessageElement) it.next();

            if (!MSG_ELEM_NAME.endsWith(msge.getElementName())) {
                // Not a data request
                continue;
            }

            try {

                doc = StructuredDocumentFactory.newStructuredDocument(msge);
                req = new DataRequest(doc);

            } catch (IOException iox) {

                Logging.logCheckedFine(LOG, "Could not process message\n" + iox.toString());
                return;

            }

            Logging.logCheckedFinest(LOG, "Request: " + req.getDocument(MimeMediaType.XMLUTF8).toString());
            processDataRequest(req);

        }

    }

    /**
     * Processes an incoming data request.
     */
    private void processDataRequest(DataRequest req) {

        ByteArrayOutputStream byteOut = null;
        DataResponse resp;
        DefaultContentShare share;
        int written;

        Logging.logCheckedFinest(LOG, "DataRequest:");
        Logging.logCheckedFinest(LOG, "   ContentID: " + req.getContentID());
        Logging.logCheckedFinest(LOG, "   Offset : " + req.getOffset());
        Logging.logCheckedFinest(LOG, "   Length : " + req.getLength());
        Logging.logCheckedFinest(LOG, "   QID    : " + req.getQueryID());
        Logging.logCheckedFinest(LOG, "   PipeAdv: " + req.getResponsePipe());

        share = getShare(req.getContentID());

        if (share == null) {

            Logging.logCheckedWarning(LOG, "Content not shared");
            return;

        }

        try {

            ActiveTransfer session = tracker.getSession(
                    share, req.getResponsePipe());
            byteOut = new ByteArrayOutputStream();
            written = session.getData(
                    req.getOffset(), req.getLength(), byteOut);
            
            // Send response
            resp = new DataResponse(req);
            if (written <= 0) {
                written = -written;
                resp.setEOF(true);
            }
            resp.setLength(written);
            share.fireShareAccessed(session, resp);

            sendDataResponse(resp, session.getOutputPipe(),
                    (written == 0) ? null : byteOut.toByteArray());

        } catch (TooManyClientsException tmcx) {

            Logging.logCheckedWarning(LOG, "Too many concurrent clients.  Discarding.");
            
        } catch (IOException iox) {

            Logging.logCheckedWarning(LOG, "Exception while handling data request", iox);
            
        }
    }

    /**
     * Sends a response to the destination specified.
     */
    private void sendDataResponse(DataResponse resp, OutputPipe destPipe,
            byte[] data) {
        MessageElement msge;
        XMLDocument doc;
        Message msg;

        msg = new Message();
        doc = (XMLDocument) resp.getDocument(MimeMediaType.XMLUTF8);
        msge = new TextDocumentMessageElement(MSG_ELEM_NAME, doc, null);
        msg.addMessageElement(MSG_NAMESPACE, msge);

        if (data != null) {
            msge = new ByteArrayMessageElement("data",
                    new MimeMediaType("application", "octet-stream"),
                    data, null);
            msg.addMessageElement(MSG_NAMESPACE, msge);
        }

        Logging.logCheckedFiner(LOG, "Sending response: " + msg.toString());

        try {
            if (destPipe.send(msg)) return;
        } catch (IOException iox) {
            Logging.logCheckedWarning(LOG, "IOException during message send", iox);
        }

        Logging.logCheckedFine(LOG, "Did not send message");
        
    }
    
    /**
     * Notify our listeners that the provided shares are being exposed.
     * 
     * @param shares list of fresh shares
     */
    private void fireContentShared(List<ContentShare> shares) {
        ContentProviderEvent event = null;
        for (ContentProviderListener listener : listeners) {
            if (event == null) {
                event = new ContentProviderEvent.Builder(this, shares)
                        .build();
            }
            listener.contentShared(event);
        }
    }

    /**
     * Notify our listeners that the provided shares are that are no
     * longer being exposed.
     * 
     * @param contentID ContentID of the content which is no longer
     *  being shared
     */
    private void fireContentUnshared(ContentID contentID) {
        ContentProviderEvent event = null;
        for (ContentProviderListener listener : listeners) {
            if (event == null) {
                event = new ContentProviderEvent.Builder(this, contentID)
                        .build();
            }
            listener.contentUnshared(event);
        }
    }

}
