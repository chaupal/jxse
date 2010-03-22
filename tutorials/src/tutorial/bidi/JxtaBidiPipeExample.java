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

import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.platform.NetworkManager;
import net.jxta.util.JxtaBiDiPipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.protocol.PipeAdvertisement;

/**
 * This is the client (initiator) side of the Bi-directional Pipe Tutorial.
 * <p/>
 * This example does the following :
 * <ol>
 *  <li>Start {@code PIPE_CONNECTIONS} threads.
 *  <li>For each thread:<ol>
 *      <li>Open a connection to the server pipe.</li>
 *      <li>Wait for messages sent by the server.</li>
 *      <li>Send a response for each message received.</li>
 *      </ol></li>
 * </ol>
 */
public class JxtaBidiPipeExample implements Runnable, PipeMsgListener {
    /**
     *  Logger
     */

    private final static Logger LOG = Logger.getLogger(JxtaBidiPipeExample.class.getName());
    /**
     *  The location of the JXTA cache directory. 
     */
    private final static File home = new File(new File(".cache"), "client");
    /**
     *  The number of pipe connections we will establish with the server.
     */
    private final static int PIPE_CONNECTIONS = 10;
    /**
     *  The connection threads we have created.
     */
    private final static Collection<Thread> connections = new ArrayList<Thread>();
    /**
     *  The peer group context in which we are working.
     */
    private final PeerGroup peergroup;
    /**
     *  The per connection bi-directional pipe instance.
     */
    private JxtaBiDiPipe pipe = null;
    /**
     *  Per connection count of messages we have received.
     */
    private final AtomicInteger received_count = new AtomicInteger(0);

    /**
     *  Standard constructor.
     *
     *  @param peergroup
     */
    private JxtaBidiPipeExample(PeerGroup peergroup) {
        this.peergroup = peergroup;
    }

    /**
     *  Send responses 
     */
    private void sendResponses() {
        int responses_sent = 0;

        while (responses_sent < JxtaServerPipeExample.ITERATIONS && pipe.isBound()) {
            synchronized (received_count) {
                while (responses_sent >= received_count.get()) {
                    try {
                        System.out.println("[" + Thread.currentThread().getName() + "] Waiting for " + (JxtaServerPipeExample.ITERATIONS - received_count.get()) + " more messages.");
                        received_count.wait();
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                }

            }

            // Build the response and send it.
            Message response = new Message();
            MessageElement respElement = new StringMessageElement(JxtaServerPipeExample.RESPONSE_ELEMENT_NAME, Integer.toString(responses_sent), null);
            response.addMessageElement(JxtaServerPipeExample.MESSAGE_NAMESPACE_NAME, respElement);

            try {

                pipe.sendMessage(response);
                responses_sent++;

            } catch (IOException failure) {

                Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] Failed sending a response message", failure);
                return;
                
            }
        }
    }

        /**
     * Called when a message is received for our pipe. Be aware that this may
     * be called concurrently on several threads simultaneously. Since we are
     * sending a response, which may block, it is important that we do not
     * synchronize on this method. 
     *
     * @param event message event
     */
    public void pipeMsgEvent(PipeMsgEvent event) {
        
        Message msg = event.getMessage();

        if (msg == null) {

            Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] Received an empty message, returning");
            return;

        }

        try {

            Logging.logCheckedFiner(LOG, "[" + Thread.currentThread().getName() + "] Received a message");

            // Get the message element.
            MessageElement msgElement = msg.getMessageElement(JxtaServerPipeExample.MESSAGE_NAMESPACE_NAME, JxtaServerPipeExample.MESSAGE_ELEMENT_NAME);

            if (null == msgElement) {

                Logging.logCheckedWarning(LOG, "Missing message element");
                return;

            }

            // Get message
            if (msgElement.toString() == null) {

                Logging.logCheckedWarning(LOG, "[" + Thread.currentThread().getName() + "] null message received.");
                return;

            }

            // Note that we received a message
            synchronized (received_count) {
                received_count.incrementAndGet();
                received_count.notify();
            }
        } catch (Exception failure) {
            Logging.logCheckedSevere(LOG, "[" + Thread.currentThread().getName() + "] Failure receiving event", failure);
        }
    }

    /**
     * Set up this pipe connection and wait until the expected messages have
     * been received.
     */
    public void run() {
        try {
            PipeAdvertisement connect_pipe = JxtaServerPipeExample.getPipeAdvertisement();

            System.out.println("[" + Thread.currentThread().getName() + "] Attempting to establish a connection to : " + connect_pipe.getPipeID());
            pipe = new JxtaBiDiPipe(peergroup, connect_pipe, 20000, this, true);
            System.out.println("[" + Thread.currentThread().getName() + "] JxtaBiDiPipe pipe created");

            // We registered ourself as the msg listener for the pipe. We now
            // just need to wait until the transmission is finished.
            sendResponses();
            pipe.close();

            System.out.println("[" + Thread.currentThread().getName() + "] Done!");

        } catch (IOException failure) {

            Logging.logCheckedSevere(LOG, "[" + Thread.currentThread().getName() + "] Failure opening pipe", failure);
            
        } finally {

            synchronized (connections) {
                connections.remove(Thread.currentThread());
                connections.notify();
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
            // System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.OFF.toString());

            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "JxtaBidiPipeExample", home.toURI());

            // Start JXTA
            manager.startNetwork();
            // manager.login("principal", "password");

            boolean waitForRendezvous = Boolean.valueOf(System.getProperty("RDVWAIT", "false"));
            if (waitForRendezvous) {
                // wait until a connection to a rendezvous is established
                manager.waitForRendezvousConnection(0);
            }

            PeerGroup netPeerGroup = manager.getNetPeerGroup();
            System.out.println("JXTA Started : " + netPeerGroup);

            // Create PIPE_CONNECTIONS threads to connect to the server pipe.
            for (int i = 1; i <= PIPE_CONNECTIONS; i++) {
                Thread thread = new Thread(new JxtaBidiPipeExample(netPeerGroup), "Connection " + i);
                connections.add(thread);
                thread.start();
            }

            // Wait until all of the threads are done.
            synchronized (connections) {
                while (!connections.isEmpty()) {
                    connections.wait();
                }
            }

            // Stop JXTA
            manager.stopNetwork();
            System.out.println("JXTA Shutdown");
        } catch (Throwable all) {
            LOG.log(Level.SEVERE, "Failure starting bi-directional pipes.", all);
            System.exit(-1);
        }
    }
}