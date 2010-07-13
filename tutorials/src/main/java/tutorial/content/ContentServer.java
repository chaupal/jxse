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

import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProviderEvent;
import net.jxta.content.ContentProviderListener;
import net.jxta.content.ContentService;
import net.jxta.content.ContentShare;
import net.jxta.content.ContentShareEvent;
import net.jxta.content.ContentShareListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.FileDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ContentShareAdvertisement;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * This tutorial illustrates the use Content API from the
 * perspective of the serving peer.
 * <p/>
 * The server is started and instructed to serve a file
 * to all peers.
 */
public class ContentServer {

    static final String DEFAULT_CONTENT_ID =
        "urn:jxta:uuid-59616261646162614E50472050325033901EA80A652D476C9D1089545CEDE7B007";

    private transient NetworkManager manager = null;
    private transient PeerGroup netPeerGroup = null;
    private transient boolean waitForRendezvous = false;
    private final ContentID contentID;
    private final File file;
    
    /**
     * Content provider listener used to be notified of any activity being
     * performed by the contrnt provider.
     */
    private ContentProviderListener provListener =
            new ContentProviderListener() {
        public void contentShared(ContentProviderEvent event) {
            logEvent("Content shared:", event);
        }

        public void contentUnshared(ContentProviderEvent event) {
            logEvent("Content unshared: ", event);
        }

        public boolean contentSharesFound(ContentProviderEvent event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    
    /**
     * Content share listener used to be notified of any activity during
     * a content transfer.
     */
    private ContentShareListener shareListener = new ContentShareListener() {
        public void shareSessionOpened(ContentShareEvent event) {
            logEvent("Session opened: ", event);
        }

        public void shareSessionClosed(ContentShareEvent event) {
            logEvent("Session closed", event);
        }

        public void shareAccessed(ContentShareEvent event) {
            logEvent("Share access", event);
        }
    };
    
    /**
     * Constructor.
     * 
     * @param toServe file to serve
     * @param id ContentID to use when serving the toServer file, or
     *  {@code null} to generate a random ContentID
     * @param waitForRendezvous true to wait for rdv connection, false otherwise
     */
    public ContentServer(File toServe, ContentID id, boolean waitForRendezvous) {
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
        file = toServe;
        if (id == null) {
            contentID = IDFactory.newContentID(
                    netPeerGroup.getPeerGroupID(), false);
        } else {
            contentID = id;
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
           
            /*
             * Here we setup a Content object that we plan on sharing.
             */
            System.out.println("Creating Content object");
            System.out.println("   ID     : " + contentID);
            FileDocument fileDoc = new FileDocument(file, MimeMediaType.AOS);
            Content content = new Content(contentID, null, fileDoc);
            System.out.println("   Content: " + content);
            
            /*
             * Now we'll ask the ContentProvider implementations to share
             * the Content object we just created.  This single action may
             * result in more than one way to share the object, so we get
             * back a list of ContentShare objects.  Pragmatically, we
             * are likely to get one ContentShare per ContentProvider
             * implementation, though this isn't necessarily true.
             */
            List<ContentShare> shares = service.shareContent(content);

            /*
             * Now that we have some shares, we can advertise them so that
             * other peers can access them.  In this tutorial we are using
             * the DiscoveryService to publish the advertisements for the
             * ContentClient program to be able to discover them.
             */
            DiscoveryService discoService = netPeerGroup.getDiscoveryService();
            for (ContentShare share : shares) {
                /*
                 * We'll attach a listener to the ContentShare so that we
                 * can see any activity relating to it.
                 */
                share.addContentShareListener(shareListener);

                /*
                 * Each ContentShare has it's own Advertisement, so we publish
                 * them all.
                 */
                ContentShareAdvertisement adv = share.getContentShareAdvertisement();
                discoService.publish(adv);
            }
           
            /*
             * Wait forever, allowing peers to retrieve the shared Content
             * until we terminate.
             */
            System.out.println("Waiting for clients...");
            synchronized(this) {
                try {
                    wait();
                } catch (InterruptedException intx) {
                    System.out.println("Interrupted");
                }
            }
            System.out.println("Exiting");
        } catch (IOException io) {
            io.printStackTrace();
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
        if (args.length > 2) {
            System.err.println("USAGE: ContentServer [File] [ContentID]");
            System.exit(1);
        }
       
        try {
            File file;
            if (args.length > 0) {
                file = new File(args[0]);
                // Use the file specified
                if (!file.exists()) {
                    System.err.println("ERROR: File '" + args[0] + "' does not exist");
                    System.exit(-1);
                }
            } else {
                // Create and use a temporary file
                file = File.createTempFile("ContentServer_", ".tmp");
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write("This is some test data for our demonstration Content");
                fileWriter.close();
            }
        
            Thread.currentThread().setName(ContentServer.class.getName() + ".main()");
            URI uri;
            if (args.length == 2) {
                uri = new URI(args[1]);
            } else {
                uri = new URI(DEFAULT_CONTENT_ID);
            }
            ContentID id = (ContentID) IDFactory.fromURI(uri);
            String value = System.getProperty("RDVWAIT", "false");
            boolean waitForRendezvous = Boolean.valueOf(value);
            ContentServer socEx = new ContentServer(file, id, waitForRendezvous);
            socEx.run();
        } catch (Throwable e) {
            System.out.flush();
            System.err.println("Failed : " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    private void logEvent(String title, ContentProviderEvent event) {
        System.out.println("ContentProviderEvent " + title);
        System.out.println("        ContentID: " + event.getContentID());
        System.out.println("        Provider : " + event.getContentProvider());
        System.out.println("        Shares   : " + event.getContentShares().size());
        for (ContentShare share : event.getContentShares()) {
            System.out.println("            " + share.toString());
        }
    }

    private void logEvent(String title, ContentShareEvent event) {
        System.out.println("ContentShareEvent - " + title);
        System.out.println("        Source    : " + event.getContentShare());
        System.out.println("        Name      : " + event.getRemoteName());
        System.out.println("        Data Start: " + event.getDataStart());
        System.out.println("        Data Size : " + event.getDataSize());
    }

    private void stop() {
        manager.stopNetwork();
    }

}

