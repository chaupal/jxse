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

package net.jxta.socket;


import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.text.MessageFormat;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.credential.Credential;
import net.jxta.endpoint.Messenger;
import net.jxta.impl.util.pipe.reliable.Outgoing;
import net.jxta.peer.PeerID;
import net.jxta.protocol.PeerAdvertisement;


/**
 * This tutorial illustrates the use JxtaSocket. It attempts to bind a
 * JxtaSocket to an instance of JxtaServerSocket bound socket.adv.
 * <p/>
 * Once a connection is established data is exchanged with the server.
 * The client will identify how many ITERATIONS of PAYLOADSIZE buffers will be
 * exchanged with the server and then write and read those buffers.
 */
public class SocketClient extends TestCase {
    /**
     *  The maximum ratio of messages we will test losing.
     */
    private final static double MAX_MESSAGE_LOSS = 0.5;
    
    /**
     *  The increment of message loss we will test.
     */
    private final static double MESSAGE_LOSS_INCREMENT = 0.05;
    
    /**
     *  The maximum ratio of messages we will allow to delay.
     */
    private final static double MAX_MESSAGE_DELAY = 0.75;
    
    /**
     *  The increment of message delay we will test.
     */
    private final static double MESSAGE_DELAY_INCREMENT = 0.05;
    
    /**
     *  The number of runs we will attempt.
     */
    private final static int RUNS = 20;
    
    // number of iterations to send the payload
    private final static long ITERATIONS = 100;
    
    // payload size
    private final static int PAYLOADSIZE = 64 * 1024;
    
    private static transient NetworkManager manager = null;
    
    private static transient PeerGroup netPeerGroup = null;
    
    private transient PipeAdvertisement pipeAdv;
    
    public SocketClient() {
        synchronized (SocketClient.class) {
            try {
                if(null == manager) {
                    manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "SocketClient",
                            new File(new File(".cache"), "SocketClient").toURI());
                    manager.startNetwork();
                    
                    netPeerGroup = manager.getNetPeerGroup();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        
        pipeAdv = SocketServer.getSocketAdvertisement();
    }
    
    /**
     * Interact with the server.
     * @param run
     * @param iterations
     * @param loss
     * @param delayRatio 
     */
    public void singleTest(int run, long iterations, double loss, double delayRatio) {
        try {
            
            long start = System.currentTimeMillis();
            
            JxtaSocket socket = null;
            
            synchronized (FaultyJxtaSocket.class) {
                int attempt = 0;
                
                FaultyJxtaSocket.loss = loss;
                FaultyJxtaSocket.delay = delayRatio;
                
                while ((null == socket) && (attempt < 3)) {
                    attempt++;
                    try {
                        System.out.println("Connecting : LOSS = " + loss + " DELAYS = " + delayRatio + " RUN = " + run + " ATTEMPT = " + attempt);
                        
                        socket = new FaultyJxtaSocket(netPeerGroup,
                                null, // no specific peerid
                                pipeAdv,
                                60000, // general TO: 60 seconds
                                true); // reliable connection
                    } catch (SocketTimeoutException noConnect) {
                        continue;
                    }
                }
            }
            
            if (null == socket) {
                fail("Could not open socket.");
            }
            
            // get the socket output stream
            OutputStream out = socket.getOutputStream();
            DataOutput dos = new DataOutputStream(out);
            
            // get the socket input stream
            InputStream in = socket.getInputStream();
            DataInput dis = new DataInputStream(in);
            long total = iterations * (long) PAYLOADSIZE * 2;
            
            System.out.println("Sending/Receiving " + total + " bytes.");
            
            dos.writeLong(iterations);
            dos.writeInt(PAYLOADSIZE);
            
            long current = 0;
            
            while (current < iterations) {
                byte[] out_buf = new byte[PAYLOADSIZE];
                byte[] in_buf = new byte[PAYLOADSIZE];
                
                Arrays.fill(out_buf, (byte) current);
                out.write(out_buf);
                out.flush();
                dis.readFully(in_buf);
                assert Arrays.equals(in_buf, out_buf);
                current++;
            }
            out.close();
            in.close();
            
            long finish = System.currentTimeMillis();
            long elapsed = finish - start;
            
            System.out.println(MessageFormat.format("EOT. Processed {0} bytes in {1} ms. Throughput = {2} KB/sec.",
                    total, elapsed, (total / elapsed) * 1000 / 1024));
            
            socket.close();
            System.out.println("Completed: Connecting : LOSS = " + loss + " DELAYS = " + delayRatio + " RUN = " + run);
        } catch (IOException io) {
            io.printStackTrace(System.err);
            fail("Failed : " + io);
        }
    }
    
    /**
     */
    public void testFailingSocket() {
        
        try {
            for (double loss = 0.0; loss < MAX_MESSAGE_LOSS; loss += MESSAGE_LOSS_INCREMENT) {
                for (double delays = 0.0; delays < MAX_MESSAGE_DELAY; delays += MESSAGE_DELAY_INCREMENT) {
                    for (int i = 1; i <= RUNS; i++) {
                        singleTest(i, ITERATIONS, loss, delays);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            fail("Failed : " + e);
        }
    }
    
    /**
     */
    public void testMessageLoss() {
        
        try {
            for (double loss = 0.0; loss < MAX_MESSAGE_LOSS; loss += MESSAGE_LOSS_INCREMENT) {
                for (int i = 1; i <= RUNS; i++) {
                    singleTest(i, ITERATIONS, loss, 0.0);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            fail("Failed : " + e);
        }
    }
    
    /**
     */
    public void testMessageDelay() {
        
        try {
            for (double delays = 0.0; delays < MAX_MESSAGE_DELAY; delays += MESSAGE_DELAY_INCREMENT) {
                for (int i = 1; i <= RUNS; i++) {
                    singleTest(i, ITERATIONS, 0.0, delays);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            fail("Failed : " + e);
        }
    }
     
    /**
     */
    public void testDefault() {
        
        try {
            for (int i = 1; i <= RUNS; i++) {
                singleTest(i, ITERATIONS, 0.0, 0.0);
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            fail("Failed : " + e);
        }
    }
   
    /**
     *  Just like a JxtaSocket, but with built in faultiness! (not meant to be
     *  used in real applications).
     */
    static class FaultyJxtaSocket extends net.jxta.socket.JxtaSocket {
        
        static volatile double loss = 0.0;
        
        static volatile double delay = 0.0;
        
        public FaultyJxtaSocket(PeerGroup group, PeerID peerid, PipeAdvertisement pipeAdv, int timeout, boolean reliable) throws IOException {
            super(group, peerid, pipeAdv, timeout, reliable);
        }
        
        protected FaultyJxtaSocket(PeerGroup group, PipeAdvertisement pipeAdv, PipeAdvertisement itsEphemeralPipeAdv, PeerAdvertisement itsPeerAdv, Credential myCredential, Credential itsCredential, boolean isReliable) throws IOException {
            super(group, pipeAdv, itsEphemeralPipeAdv, itsPeerAdv, myCredential, itsCredential, isReliable);
        }
        
        /**
         *  {@inheritDoc}
         */
        @Override
        protected Outgoing makeOutgoing(Messenger msgr, long timeout) {
            return new OutgoingFaultyMsgrAdaptor(msgr, (int) timeout, loss, delay);
        }
    }
    
    @Override
    protected void finalize() {
        
        synchronized (SocketClient.class) {
            if (null != manager) {
                manager.stopNetwork();
                manager = null;
            }
        }
    }
    
    public static void main(java.lang.String[] args) {
        Thread.currentThread().setName(SocketClient.class.getName() + ".main()");
        
        try {
            junit.textui.TestRunner.run(suite());
        } finally {
            synchronized (SocketClient.class) {
                if (null != manager) {
                    manager.stopNetwork();
                    manager = null;
                }
            }
            
            System.err.flush();
            System.out.flush();
        }
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SocketClient.class);
        
        return suite;
    }
}

