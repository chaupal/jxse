/*
 * Copyright (c) 2006-2008 Sun Microsystems, Inc.  All rights reserved.
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
package tutorial.content;

import net.jxta.content.ContentID;
import net.jxta.content.ContentService;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferAggregator;
import net.jxta.content.ContentTransferAggregatorEvent;
import net.jxta.content.ContentTransferAggregatorListener;
import net.jxta.content.TransferException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import java.io.*;
import java.net.*;

/**
 * This tutorial illustrates the use of the Content API from the
 * perspective of the client.
 * <p/>
 * The client will attempt to locate and retrieve a Content instance
 * identified by a specified ContentID.  Once the retrieval completes,
 * the client will exit.
 */
public class ContentClient {

    private transient NetworkManager manager = null;

    private transient PeerGroup netPeerGroup = null;
    private transient boolean waitForRendezvous = false;
    private final ContentID contentID;
    
    /**
     * Content transfer listener used to receive asynchronous updates regarding
     * the transfer in progress.
     */
    private ContentTransferListener xferListener =
            new ContentTransferListener() {
        public void contentLocationStateUpdated(ContentTransferEvent event) {
            System.out.println("Transfer location state: "
                    + event.getSourceLocationState());
            System.out.println("Transfer location count: "
                    + event.getSourceLocationCount());
        }

        public void contentTransferStateUpdated(ContentTransferEvent event) {
            System.out.println("Transfer state: " + event.getTransferState());
        }

        public void contentTransferProgress(ContentTransferEvent event) {
            Long bytesReceived = event.getBytesReceived();
            Long bytesTotal = event.getBytesTotal();
            System.out.println("Transfer progress: "
                    + bytesReceived + " / " + bytesTotal);
        }
    };
    
    /**
     * Content transfer aggregator listener used to detect transitions
     * between multiple ContentProviders.
     */
    private ContentTransferAggregatorListener aggListener =
            new ContentTransferAggregatorListener() {
        public void selectedContentTransfer(ContentTransferAggregatorEvent event) {
            System.out.println("Selected ContentTransfer: "
                    + event.getDelegateContentTransfer());
        }

        public void updatedContentTransferList(ContentTransferAggregatorEvent event) {
            ContentTransferAggregator aggregator =
                event.getContentTransferAggregator();
            System.out.println("ContentTransfer list updated:");
            for (ContentTransfer xfer : aggregator.getContentTransferList()) {
                System.out.println("    " + xfer);
            }
        }
    };
    
    /**
     * Constructor.
     * 
     * @param id ContentID of the Content we are going to attempt to retrieve.
     * @param waitForRendezvous true to wait for rdv connection, false otherwise
     */
    public ContentClient(ContentID id, boolean waitForRendezvous) {
        contentID = id;

            // Start the JXTA network
        try {
            manager = new NetworkManager(
                    NetworkManager.ConfigMode.ADHOC, "ContentClient",
                    new File(new File(".cache"), "ContentClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        netPeerGroup = manager.getNetPeerGroup();
        if (waitForRendezvous) {
            manager.waitForRendezvousConnection(0);
        }
    }

    /**
     * Interact with the server.
     */
    public void run() {
        try {
            /*
             * Get the PeerGroup's ContentService instance.
             */
            ContentService service = netPeerGroup.getContentService();

            System.out.println("Initiating Content transfer");
            // Create a transfer instance
            ContentTransfer transfer = service.retrieveContent(contentID);
            if (transfer == null) {
                /*
                 * This can happen if no ContentProvider implementations
                 * have the ability to locate and retrieve this ContentID.
                 * In most scenarios, this wouldn't happen.
                 */
                System.out.println("Could not retrieve Content");
            } else {
                /*
                 * Add a listener so that we can watch the state transitions
                 * as they occur.
                 */
                transfer.addContentTransferListener(xferListener);

                /*
                 * If the transfer is actually an aggregation fronting multiple
                 * provider implementations, we can listen in on what the
                 * aggregator is doing.
                 */
                if (transfer instanceof ContentTransferAggregator) {
                    ContentTransferAggregator aggregator = (ContentTransferAggregator) transfer;
                    aggregator.addContentTransferAggregatorListener(aggListener);
                }

                /*
                 * This step is not required but is here to illustrate the
                 * possibility.  This advises the ContentProviders to
                 * try to find places to retrieve the Content from but will
                 * not actually start transferring data.  We'll sleep after
                 * we initiate source location to allow this to proceed
                 * outwith actual retrieval attempts.
                 */
                System.out.println("Starting source location");
                transfer.startSourceLocation();
                System.out.println("Waiting for 5 seconds to demonstrate source location...");
                Thread.sleep(5000);

                /*
                 * Now we'll start the transfer in earnest.  If we had chosen
                 * not to explicitly request source location as we did above,
                 * this would implicitly locate sources and start the transfer
                 * as soon as enough sources were found.
                 */
                transfer.startTransfer(new File("content"));

                /*
                 * Finally, we wait for transfer completion or failure.
                 */
                transfer.waitFor();
            }
        } catch (TransferException transx) {
            transx.printStackTrace(System.err);
        } catch (InterruptedException intx) {
            System.out.println("Interrupted");
        } finally {
            stop();
        }
    }

    /**
     * If the java property RDVWAIT set to true then this demo
     * will wait until a rendezvous connection is established before
     * initiating a connection
     *
     * @param args none recognized.
     */
    public static void main(String args[]) {

        /*
         System.setProperty("net.jxta.logging.Logging", "FINEST");
         System.setProperty("net.jxta.level", "FINEST");
         System.setProperty("java.util.logging.config.file", "logging.properties");
         */
        if (args.length > 1) {
            System.err.println("USAGE: ContentClient [ContentID]");
            System.exit(1);
        }
        try {
            Thread.currentThread().setName(ContentClient.class.getName() + ".main()");
            URI uri;
            if (args.length == 0) {
                uri = new URI(ContentServer.DEFAULT_CONTENT_ID);
            } else {
                uri = new URI(args[0]);
            }
            ContentID id = (ContentID) IDFactory.fromURI(uri);
            String value = System.getProperty("RDVWAIT", "false");
            boolean waitForRendezvous = Boolean.valueOf(value);
            ContentClient socEx = new ContentClient(id, waitForRendezvous);
            socEx.run();
        } catch (Throwable e) {
            System.out.flush();
            System.err.println("Failed : " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    private void stop() {
        manager.stopNetwork();
    }

}

