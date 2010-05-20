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
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the server (receiver) side of the Bi-directional Pipe Tutorial.
 * <p/>
 * This example does the following :
 * <ol>
 *  <li>Open a server pipe.</li>
 *  <li>Listen for connect requests via {@code accept()}.</li>
 *  <li>For each connect request spawn a thread which:
 *      <ol>
 *          <li>Sends {@code ITERATIONS} messages to the connection.</li>
 *          <li>Waits {@code ITERATIONS} responses.</li>
 *      </ol></li>
 * </ol>
 */
public class JxtaServerPipeExample {
    
    /**
     *  Logger.
     */
    private final static transient Logger LOG = Logger.getLogger(JxtaServerPipeExample.class.getName());
    
    /**
     *  Connection count.
     */
    private final static AtomicInteger connection_count = new AtomicInteger(0);
    
    /**
     * Number of messages to send
     */
    final static int ITERATIONS = 1000;
    
    final static String MESSAGE_NAMESPACE_NAME = "bidi_tutorial";
    final static String MESSAGE_ELEMENT_NAME = "sequence";
    final static String RESPONSE_ELEMENT_NAME = "response";
    
    private final static PipeID BIDI_TUTORIAL_PIPEID = PipeID.create(URI.create("urn:jxta:uuid-59616261646162614E504720503250338944BCED387C4A2BBD8E9411B78C284104"));
    
    /**
     * Gets the pipeAdvertisement attribute of the JxtaServerPipeExample class
     *
     * @return The pipeAdvertisement
     */
    public static PipeAdvertisement getPipeAdvertisement() {
        PipeAdvertisement advertisement = (PipeAdvertisement)
        AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        
        advertisement.setPipeID(BIDI_TUTORIAL_PIPEID);
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName("JxtaBiDiPipe tutorial");
        
        return advertisement;
    }
    
    /**
     * Connection wrapper. Once started, it sends ITERATIONS messages and
     * receives a response from the initiator for each message.
     */
    private static class ConnectionHandler implements Runnable, PipeMsgListener {
        private final JxtaBiDiPipe pipe;
        
        private final AtomicInteger received_count = new AtomicInteger(0);
        
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
            synchronized (received_count) {
                received_count.incrementAndGet();
                received_count.notify();
            }
            
            try {
                // grab the message from the event
                Message msg = event.getMessage();

                Logging.logCheckedFiner(LOG, "[" + Thread.currentThread().getName() + "] Received a response");
                
                // get the message element named SenderMessage
                MessageElement msgElement = msg.getMessageElement(MESSAGE_NAMESPACE_NAME, RESPONSE_ELEMENT_NAME);
                
                if(null == msgElement) {

                    Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] Missing message element");
                    return;

                }
                
                // Get message
                if (msgElement.toString() == null) {

                    Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] Null message receved");
                    return;

                } 
                
                // System.out.println("Got Message :" + msgElement.toString());
            } catch (Exception e) {

                Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] Failure during message receipt.\n", e);

            }
        }
        
        /**
         * Send a series of messages over a pipe
         *
         * @param pipe the pipe to send messages over
         * @throws IOException Thrown for errors sending messages.
         */
        private void sendTestMessages(JxtaBiDiPipe pipe) throws IOException {
            long start = System.currentTimeMillis();
            
            // Send ITERATIONS messages to the initiator.
            for (int send_count = 0; send_count < ITERATIONS; send_count++) {
                Message msg = new Message();
                String data = "Seq #" + send_count;
                
                msg.addMessageElement(MESSAGE_NAMESPACE_NAME, new StringMessageElement(MESSAGE_ELEMENT_NAME, data, null));
                
                System.out.println("[" + Thread.currentThread().getName() + "] Sending message :" + send_count);
                pipe.sendMessage(msg);
            }
            
            // Wait for the last responses to arrive.
            synchronized(received_count) {
                while(received_count.get() < ITERATIONS) {
                    try {
                        received_count.wait();
                    } catch(InterruptedException woken) {
                        Thread.interrupted();
                        if(!pipe.isBound()) {
                            break;
                        }
                    }
                }
            }
            
            // Compute the message throughput. 
            int transactions = received_count.get();
            long finish = System.currentTimeMillis();
            long delta = finish - start;
            double tps = (0 != delta) ? transactions * 1000.0 / delta : transactions * 1000.0;
            
            System.out.println("[" + Thread.currentThread().getName() + "] Completed " + transactions + " in " + delta + "ms. (" + tps + "/TPS).");
        }
        
        /**
         * Main processing method for the ConnectionHandler object
         */
        public void run() {
            try {
                sendTestMessages(pipe);
                System.out.println("[" + Thread.currentThread().getName() + "] Closing the pipe");
                pipe.close();
            } catch (Throwable all) {
                LOG.log(Level.SEVERE, "[" + Thread.currentThread().getName() + "] Failure in ConnectionHandler\n", all);
            }
        }
    }
    
    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        try {
            final File home = new File(new File(".cache"), "server");
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "JxtaServerPipeExample", home.toURI());
            manager.startNetwork();
            
            PeerGroup netPeerGroup = manager.getNetPeerGroup();
            
            PipeAdvertisement serverPipeAdv = JxtaServerPipeExample.getPipeAdvertisement();
            JxtaServerPipe serverPipe = new JxtaServerPipe(netPeerGroup, serverPipeAdv);
            
            // block forever until a connection is accepted
            serverPipe.setPipeTimeout(0);
            
            System.out.println("Waiting for JxtaBidiPipe connections on JxtaServerPipe : " + serverPipeAdv.getPipeID());
            while (true) {
                JxtaBiDiPipe bipipe = serverPipe.accept();
                if (bipipe != null) {
                    System.out.println("JxtaBidiPipe accepted from " + bipipe.getRemotePeerAdvertisement().getPeerID() + " : " + bipipe.getRemotePipeAdvertisement().getPipeID() + " sending " + ITERATIONS + " messages.");
                    // Send messages
                    Thread thread = new Thread(new ConnectionHandler(bipipe), "Connection Handler " + connection_count.incrementAndGet());
                    thread.start();
                }
            }
        } catch (Throwable all) {
            LOG.log(Level.SEVERE,"Failure opening server pipe.\n", all);
            System.exit(-1);
        }
    }
}
