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

package net.jxta.impl.content.srdisocket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.content.Content;
import net.jxta.peergroup.PeerGroup;
import net.jxta.content.ContentID;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferException;
import net.jxta.document.FileDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.impl.content.AbstractContentTransfer;
import net.jxta.protocol.ContentAdvertisement;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.socket.JxtaSocket;

/**
 * Default implementation of a Content transfer mechanism, utilizing
 * SRDI advertisements and JxtaSocket-based communications.
 */
public class SRDISocketContentTransfer extends AbstractContentTransfer {
    /**
     * Logger instance.
     */
    private static final Logger LOG =
            Logger.getLogger(SRDISocketContentTransfer.class.getName());

    /**
     * The number of seconds between source discovery attempts.
     */
    private static final int SOURCE_LOCATION_INTERVAL =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".sourceLocationInterval", 30).intValue();

    /**
     * The number of seconds before JxtaSocket timeout.
     */
    private static final int SOCKET_TIMEOUT =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".socketTimeout", 30).intValue() * 1000;

    /**
     * The number of of sources considered to be "enough".
     */
    private static final int ENOUGH_SOURCES =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".enoughSources", 1).intValue();

    /**
     * The number of of sources considered to be "many".
     */
    private static final int MANY_SOURCES =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".manySources", 5).intValue();

    /**
     * The discovery threshold to use.
     */
    private static final int DISCOVERY_THRESHOLD =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".discoveryTreshold", 10).intValue();

    /**
     * The buffer size used when retrieving remote Content data.
     */
    private static final int BUFFER_SIZE =
            Integer.getInteger(SRDISocketContentTransfer.class.getName()
            + ".bufferSize", 4096).intValue();

    /**
     * The minimum number of milliseconds between progress updates to our
     * listeners.
     */
    private static final long MIN_EVENT_INTERVAL =
            Long.getLong(SRDISocketContentTransfer.class.getName()
            + ".minimumEventInterval", 200).longValue();

    // Initialized at construction
    private final PeerGroup peerGroup;

    // Managed over the course of the transfer
    private List<SRDISocketContentShareAdvertisementImpl> sourcesRemaining =
            new ArrayList<SRDISocketContentShareAdvertisementImpl>();
    private List<SRDISocketContentShareAdvertisementImpl> sourcesTried =
            new ArrayList<SRDISocketContentShareAdvertisementImpl>();

    //////////////////////////////////////////////////////////////////////////
    // Constructors:

    public SRDISocketContentTransfer(
            SRDISocketContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentShareAdvertisement contentAdv) {
        super(origin, schedExecutor, group, contentAdv, "SRDISocketContentTransfer");
        setSourceLocationInterval(SOURCE_LOCATION_INTERVAL);
        setDiscoveryThreshold(DISCOVERY_THRESHOLD);
        peerGroup = group;
    }

    public SRDISocketContentTransfer(
            SRDISocketContentProvider origin,
            ScheduledExecutorService schedExecutor,
            PeerGroup group,
            ContentID contentID) {
        super(origin, schedExecutor, group, contentID, "SRDISocketContentTransfer");
        setSourceLocationInterval(SOURCE_LOCATION_INTERVAL);
        setDiscoveryThreshold(DISCOVERY_THRESHOLD);
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
        return (adv instanceof SRDISocketContentShareAdvertisementImpl);
    }

    /**
     * {@inheritDoc}
     */
    protected ContentTransferState transferAttempt(
            File dest,
            List<ContentShareAdvertisement> sources,
            List<ContentShareAdvertisement> newSources)
            throws TransferException {

        ContentTransferState result;

        Logging.logCheckedFine(LOG, "Transfer attempt starting");

        // Add new sources to our tracked list
        for (ContentShareAdvertisement candidate : newSources) {
            if (candidate instanceof SRDISocketContentShareAdvertisementImpl) {
                sourcesRemaining.add(
                        (SRDISocketContentShareAdvertisementImpl) candidate);
            }
        }

        Logging.logCheckedFine(LOG, "Sources remaining: " + sourcesRemaining.size());
        Logging.logCheckedFine(LOG, "Sources tried    : " + sourcesTried.size());

        if (sourcesRemaining.size() == 0) {

            Logging.logCheckedFine(LOG, "No sources remaining to try");
            return ContentTransferState.STALLED;

            /* Another option:
            LOG.fine("Resetting reamining/tried lists");
            sourcesRemaining.addAll(sourcesTried);
            sourcesTried.clear();
             */
        }

        SRDISocketContentShareAdvertisementImpl adv = sourcesRemaining.remove(0);
        sourcesTried.add(adv);

        JxtaSocket socket = null;
        Content resultContent = null;

        try {

            ContentRequest request = new ContentRequest();
            request.setContentID(getTransferContentID());

            Logging.logCheckedFiner(LOG, "Sending content request to:\n" + adv.getPipeAdvertisement());
            Logging.logCheckedFiner(LOG, "Request:\n" + request.getDocument(MimeMediaType.XMLUTF8));
            
            socket = new JxtaSocket(
                    peerGroup, null, adv.getPipeAdvertisement(),
                    SOCKET_TIMEOUT, true);

            OutputStream outStream = socket.getOutputStream();
            request.writeToStream(outStream);

            Logging.logCheckedFiner(LOG, "Request sent.  Awaiting response.");
            
            InputStream inStream = socket.getInputStream();
            ContentResponse response = ContentResponse.readFromStream(inStream);

            Logging.logCheckedFiner(LOG, "Got response: " + response.getDocument(MimeMediaType.XMLUTF8));
            
            if (response.getSuccess()) {

                Logging.logCheckedFiner(LOG, "Retrieving content");
                resultContent = transferContent(dest, adv, inStream);

            }
            
            Logging.logCheckedFiner(LOG, "Transaction completed");
            
        } catch (IOException iox) {

            throw(new TransferException("Retrieval attempt failed", iox));

        } finally {

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException iox) {
                    Logging.logCheckedFinest(LOG, "Ignoring exception\n" + iox.toString());
                }
            }

        }

        if (resultContent == null) {
            result = ContentTransferState.RETRYING;
        } else {
            setContent(resultContent);
            result = ContentTransferState.COMPLETED;
        }

        Logging.logCheckedFine(LOG, "Transfer attempt exiting with result: " + result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        super.cancel();
    }

    //////////////////////////////////////////////////////////////////////////
    // Private methods:


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
     * Retrieves the Content data from the stream provided until end-of-stream
     * is reached.
     */
    private Content transferContent(
            File destFile,
            ContentShareAdvertisement adv, InputStream stream)
            throws IOException {
        FileOutputStream fileOut;
        byte[] buffer = new byte[BUFFER_SIZE];
        long lastEvent = 0;
        long delta;
        long totalReceived = 0;
        int readCount;

        try {
            destFile.delete();
            destFile.createNewFile();
            fileOut = new FileOutputStream(destFile);
        } catch (Throwable t) {
            Logging.logCheckedFine(LOG, "Caught exception\n" + t.toString());
            return null;
        }

        do {
            delta = System.currentTimeMillis() - lastEvent;
            if (delta > MIN_EVENT_INTERVAL) {
                lastEvent = System.currentTimeMillis();
                fireTransferProgress(totalReceived);
            }

            readCount = stream.read(buffer);

            Logging.logCheckedFinest(LOG, "Read count: " + readCount);
            
            if (readCount < 0) {
                // EOS
                break;
            }

            totalReceived += readCount;
            fileOut.write(buffer, 0, readCount);

        } while(true);

        fileOut.close();

        // Final status update
        fireTransferProgress(totalReceived);

        Logging.logCheckedFiner(LOG, "Content data transfer successful");

        ContentAdvertisement cAdv = adv.getContentAdvertisement();
        FileDocument fileDocument =
                new FileDocument(destFile, cAdv.getMimeType());
        Content result = new Content(
                adv.getContentID(), cAdv.getMetaID(), fileDocument);

        Logging.logCheckedFiner(LOG, "Result Content object: " + result);
        return result;
    }

}
