/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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
package tutorial.bidi;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

import java.io.File;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the Server side of the example. This example awaits bidirectional
 * pipe connections, then spawn a new thread to deal with The connection
 */
public class JxtaServerPipeExample {

    /**
     * Number of messages to send
     */
    public final static int ITERATIONS = 10 * 1000;
    private transient PeerGroup netPeerGroup = null;
    private transient JxtaServerPipe serverPipe;
    private final static Logger LOG = Logger.getLogger(JxtaServerPipeExample.class.getName());
    private final static String SenderMessage = "pipe_tutorial";
    private final static String PIPEIDSTR = "urn:jxta:uuid-59616261646162614E504720503250338944BCED387C4A2BBD8E9411B78C284104";
    private transient NetworkManager manager = null;
    private final transient File home = new File(new File(".cache"), "server");
    private final String receipt = "Receipt";

    /**
     * Constructor for the JxtaServerPipeExample object
     */
    public JxtaServerPipeExample() {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "JxtaServerPipeExample", home.toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        netPeerGroup = manager.getNetPeerGroup();
    }

    /**
     * Gets the pipeAdvertisement attribute of the JxtaServerPipeExample class
     *
     * @return The pipeAdvertisement
     */
    public static PipeAdvertisement getPipeAdvertisement() {
        PipeID pipeID = (PipeID) ID.create(URI.create(PIPEIDSTR));
        PipeAdvertisement advertisement = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName("JxtaBiDiPipe tutorial");
        return advertisement;
    }

    /**
     * Connection wrapper. Once started, it send a message, awaits a response.
     * repeats the above steps for a pre-defined number of iterations
     */
    private class ConnectionHandler implements Runnable, PipeMsgListener {
        JxtaBiDiPipe pipe = null;

        /**
         * Constructor for the ConnectionHandler object
         *
         * @param pipe message pipe
         */
        ConnectionHandler(JxtaBiDiPipe pipe) {
            this.pipe = pipe;
            pipe.setMessageListener(this);
        }

        /**
         * {@inheritDoc}
         */
        public void pipeMsgEvent(PipeMsgEvent event) {
            Message msg;

            synchronized (receipt) {
                // for every message we get, we pong
                receipt.notify();
            }
            try {
                // grab the message from the event
                msg = event.getMessage();
                if (msg == null) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Received an empty message, returning");
                    }
                    return;
                }
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Received a response");
                }
                // get the message element named SenderMessage
                MessageElement msgElement = msg.getMessageElement(SenderMessage, SenderMessage);

                // Get message
                if (msgElement.toString() == null) {
                    System.out.println("null msg received");
                } else {
                    // System.out.println("Got Message :" + msgElement.toString());
                }
            } catch (Exception e) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine(e.toString());
                }
            }
        }

        /**
         * Send a series of messages over a pipe
         *
         * @param pipe the pipe to send messages over
         */
        private void sendTestMessages(JxtaBiDiPipe pipe) {
            long t0, t1, t2, t3, delta;

            try {
                t2 = System.currentTimeMillis();
                for (int i = 0; i < ITERATIONS; i++) {
                    t0 = System.currentTimeMillis();
                    Message msg = new Message();
                    String data = "Seq #" + i;

                    msg.addMessageElement(SenderMessage, new StringMessageElement(SenderMessage, data, null));
                    //System.out.println("Sending message :" + i);
                    // t0 = System.currentTimeMillis();
                    pipe.sendMessage(msg);
                    /*
                    t1 = System.currentTimeMillis();
                    delta = (t1 - t0);
                    if (delta > 50) {
                        System.out.println(" completed message sequence #" + i + " in :" + delta);
                    }
                    */
                }
                t3 = System.currentTimeMillis() +10;
                long t4 = t3 - t2;
                if (t4 <= 1000) {
                    t4 = 1000;
                }
                System.out.println(" completed " + ITERATIONS / (t4 / 1000) + " transactions/sec. Total time :" + (t3 - t2));
            } catch (Exception ie) {
                ie.printStackTrace();
            }
        }

        /**
         * Main processing method for the ConnectionHandler object
         */
        public void run() {
            try {
                sendTestMessages(pipe);
                Thread.sleep(10000);
                System.out.println("Closing the pipe");
                pipe.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main processing method for the JxtaServerPipeExample object
     */
    public void run() {

        System.out.println("Waiting for JxtaBidiPipe connections on JxtaServerPipe");
        while (true) {
            try {
                JxtaBiDiPipe bipipe = serverPipe.accept();
                if (bipipe != null) {
                    System.out.println("JxtaBidiPipe accepted, sending " + ITERATIONS + " messages to the other end");
                    // Send messages
                    Thread thread = new Thread(new ConnectionHandler(bipipe), "Connection Handler Thread");
                    thread.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        // System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.OFF.toString());
        
        JxtaServerPipeExample eg = new JxtaServerPipeExample();
        try {
            //System.out.println(JxtaServerPipeExample.getPipeAdvertisement().toString());
            eg.serverPipe = new JxtaServerPipe(eg.netPeerGroup, JxtaServerPipeExample.getPipeAdvertisement());
            // block forever until a connection is accepted
            eg.serverPipe.setPipeTimeout(0);
        } catch (Exception e) {
            System.out.println("failed to bind to the JxtaServerPipe due to the following exception");
            e.printStackTrace();
            System.exit(-1);
        }
        // run on this thread
        eg.run();
    }
}

