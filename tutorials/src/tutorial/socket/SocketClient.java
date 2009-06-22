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
package tutorial.socket;

import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;

import java.io.*;
import java.util.Arrays;
import java.text.MessageFormat;

/**
 * This tutorial illustrates the use JxtaSocket. It attempts to bind a
 * JxtaSocket to an instance of JxtaServerSocket bound socket.adv.
 * <p/>
 * Once a connection is established data is exchanged with the server.
 * The client will identify how many ITERATIONS of PAYLOADSIZE buffers will be
 * exchanged with the server and then write and read those buffers.
 */
public class SocketClient {

    /**
     * number of runs to make
     */
    private final static long RUNS = 8;

    /**
     * number of iterations to send the payload
     */
    private final static long ITERATIONS = 1000;

    /**
     * payload size
     */
    private final static int PAYLOADSIZE = 64 * 1024;

    private transient NetworkManager manager = null;

    private transient PeerGroup netPeerGroup = null;
    private transient PipeAdvertisement pipeAdv;
    private transient boolean waitForRendezvous = false;

    public SocketClient(boolean waitForRendezvous) {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "SocketClient",
                    new File(new File(".cache"), "SocketClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        netPeerGroup = manager.getNetPeerGroup();
        pipeAdv = SocketServer.createSocketAdvertisement();
        if (waitForRendezvous) {
            manager.waitForRendezvousConnection(0);
        }
    }

    /**
     * Interact with the server.
     */
    public void run() {
        try {
            if (waitForRendezvous) {
                manager.waitForRendezvousConnection(0);
            }

            long start = System.currentTimeMillis();
            System.out.println("Connecting to the server");
            JxtaSocket socket = new JxtaSocket(netPeerGroup,
                    // no specific peerid
                    null,
                    pipeAdv,
                    // connection timeout: 5 seconds
                    5000,
                    // reliable connection
                    true);

            // get the socket output stream
            OutputStream out = socket.getOutputStream();
            DataOutput dos = new DataOutputStream(out);

            // get the socket input stream
            InputStream in = socket.getInputStream();
            DataInput dis = new DataInputStream(in);

            long total = ITERATIONS * (long) PAYLOADSIZE * 2;
            System.out.println("Sending/Receiving " + total + " bytes.");

            dos.writeLong(ITERATIONS);
            dos.writeInt(PAYLOADSIZE);

            long current = 0;

            while (current < ITERATIONS) {
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

            System.out.println(MessageFormat.format("EOT. Processed {0} bytes in {1} ms. Throughput = {2} KB/sec.", total, elapsed,
                    (total / elapsed) * 1000 / 1024));
            socket.close();
            System.out.println("Socket connection closed");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private void stop() {
        manager.stopNetwork();
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
        try {
            Thread.currentThread().setName(SocketClient.class.getName() + ".main()");
            String value = System.getProperty("RDVWAIT", "false");
            boolean waitForRendezvous = Boolean.valueOf(value);
            SocketClient socEx = new SocketClient(waitForRendezvous);

            for (int i = 1; i <= RUNS; i++) {
                System.out.println("Run #" + i);
                socEx.run();
            }
            socEx.stop();
        } catch (Throwable e) {
            System.out.flush();
            System.err.println("Failed : " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}

