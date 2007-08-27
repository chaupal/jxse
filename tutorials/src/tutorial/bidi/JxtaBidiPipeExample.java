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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This example illustrates how to utilize a JxtaBiDiPipe to establish a
 * bidirectional connection, and also illustrates use of asynchronous messaging
 * interface.
 * The example will attempt to establish a connection to it's counterpart
 * (JxtaServerPipeExample), then wait until all the messages are receieved
 * asynchronously
 */
public class JxtaBidiPipeExample {
    private final static Logger LOG = Logger.getLogger(JxtaBidiPipeExample.class.getName());
    private transient NetworkManager manager = null;
    private final transient File home = new File(new File(".cache"), "client");
    private final static String SenderMessage = "pipe_tutorial";
    private final static String completeLock = "completeLock";

    public JxtaBidiPipeExample(boolean waitForRendezvous) {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "JxtaBidiPipeExample", home.toURI());
            manager.startNetwork();
            if (waitForRendezvous) {
                // wait until a connection to a rendezvous is established
                manager.waitForRendezvousConnection(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // manager.login("principal", "password");
        PeerGroup netPeerGroup = manager.getNetPeerGroup();
        for (int i= 0; i<10; i++) {
            Thread thread = new Thread(new Connection(netPeerGroup), "Connection Thread "+i);
            thread.start();
        }
    }

    private void waitUntilCompleted() {
        try {
            System.out.println("Waiting for Messages.");
            synchronized (completeLock) {
                completeLock.wait();
            }
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.OFF.toString());

        String value = System.getProperty("RDVWAIT", "false");
        boolean waitForRendezvous = Boolean.valueOf(value);
        new JxtaBidiPipeExample(waitForRendezvous);
    }


    private class Connection implements Runnable, PipeMsgListener {
        PeerGroup peerGroup;
        JxtaBiDiPipe pipe = null;
        private int count = 0;        
        Connection(PeerGroup peerGroup) {
            this.peerGroup = peerGroup;
        }

    /**
     * This is the PipeListener interface. Expect a call to this method
     * When a message is received.
     * when we get a message, print out the message on the console
     *
     * @param event message event
     */
    public void pipeMsgEvent(PipeMsgEvent event) {
            Message msg;
            Message response;
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
                // Get the message element named SenderMessage
                MessageElement msgElement = msg.getMessageElement(SenderMessage, SenderMessage);

                // Get message
                if (msgElement.toString() == null) {
                    System.out.println("null msg received");
                }
                count++;
                response = msg.clone();
                //System.out.println("Sending response to " + msgElement.toString()+" Count:"+count);
                pipe.sendMessage(response);
                // If JxtaServerPipeExample.ITERATIONS # of messages received, it is
                // no longer needed to wait. notify main to exit gracefully
                if (count >= JxtaServerPipeExample.ITERATIONS) {
                    System.out.println("Received all messages");
                    synchronized (completeLock) {
                        completeLock.notify();
                    }
                } else {
                   // System.out.println("Received "+count+" out of "+JxtaServerPipeExample.ITERATIONS);
                }
            } catch (Exception e) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine(e.toString());
                }
            }
        }


        /**
         * Main processing method for the ConnectionHandler object
         */
        public void run() {
            try {
                pipe = new JxtaBiDiPipe();
                pipe.setReliable(true);
                System.out.println("Attempting to establish a connection");
                pipe.connect(peerGroup,
                             // any listening node will do
                             null,
                             JxtaServerPipeExample.getPipeAdvertisement(),
                             10000,
                             // register as a message listener
                             this);
                // at this point we need to keep references around until data xchange
                // is complete
                System.out.println("JxtaBiDiPipe pipe created");
            } catch (IOException e) {
                System.out.println(Thread.currentThread().getName()+" failed to bind the JxtaBiDiPipe due to the following exception");
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}

