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

import net.jxta.document.AdvertisementFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.text.MessageFormat;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jxta.credential.Credential;
import net.jxta.protocol.PeerAdvertisement;

/**
 * This tutorial illustrates the use JxtaServerSocket It creates a
 * JxtaServerSocket with a back log of 10. it also blocks indefinitely, until a
 * connection is established.
 * <p/>
 * Once a connection is established data is exchanged with the initiator.
 * The initiator will provide an iteration count and buffer size. The peers will
 * then read and write buffers. (or write and read for the initiator).
 */
public class SocketServer extends TestCase {
    public final static PipeID SOCKET_ID = PipeID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503393B5C2F6CA7A41FBB0F890173088E79404"));

    private static transient NetworkManager manager = null;

    private static transient PeerGroup netPeerGroup = null;

    private static transient JxtaServerSocket serverSocket = null;

    private static boolean closed = false;

    public SocketServer() throws IOException, PeerGroupException {
        synchronized (SocketServer.class) {
            try {
                if(null == manager) {
                    manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "SocketServer", new File(new File(".cache"), "SocketServer").toURI());

                    manager.startNetwork();

                    netPeerGroup = manager.getNetPeerGroup();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public static PipeAdvertisement getSocketAdvertisement() {
        PipeAdvertisement advertisement = (PipeAdvertisement)
        AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

        advertisement.setPipeID(SOCKET_ID);
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName("Socket tutorial");
        return advertisement;
    }

    /**
     * wait for connections
     */
    public void acceptHandler() {
    }

    private class ConnectionHandler implements Runnable {
        Socket socket = null;

        ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Sends data over socket
         *
         * @param socket Description of the Parameter
         */
        private void sendAndReceiveData(Socket socket) {
            try {
                long start = System.currentTimeMillis();

                // get the socket output stream
                OutputStream out = socket.getOutputStream();
                // get the socket input stream
                InputStream in = socket.getInputStream();

                DataInput dis = new DataInputStream(in);

                long iterations = dis.readLong();
                int size = dis.readInt();

                if(0 == iterations) {
                    closed = true;
                    serverSocket.close();
                }

                long total = iterations * (long) size * 2;

                System.out.println(MessageFormat.format("Sending/Receiving {0} bytes.", total));

                long current = 0;

                while (current < iterations) {
                    byte[] buf = new byte[size];

                    dis.readFully(buf);
                    out.write(buf);
                    out.flush();
                    current++;
                }

                out.close();
                in.close();

                long finish = System.currentTimeMillis();
                long elapsed = finish - start;

                System.out.println(
                        MessageFormat.format("EOT. Received {0} bytes in {1} ms. Throughput = {2} KB/sec.",
                        total, elapsed, (total / elapsed) * 1000 / 1024));
                socket.close();
                System.out.println("Connection closed");
            } catch (Exception ie) {
                ie.printStackTrace();
            }
        }

        public void run() {
            sendAndReceiveData(socket);
        }
    }

    public void testServerSocket() {
        try {
            SocketServer socEx = new SocketServer();

            System.out.println("Starting ServerSocket");

            FaultyJxtaServerSocket.loss = 0.05;
            FaultyJxtaServerSocket.delay = 0.5;

            serverSocket = new FaultyJxtaServerSocket(netPeerGroup, getSocketAdvertisement(), 10);
            serverSocket.setSoTimeout(0);

            while (!closed) {
                try {
                    System.out.println("Waiting for connections");
                    Socket socket = serverSocket.accept();

                    // set reliable
                    if (socket != null) {
                        System.out.println("socket created");
                        Thread thread = new Thread(new ConnectionHandler(socket), "Connection Handler Thread");
                        thread.setDaemon(true);
                        thread.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            System.err.println("Failed : " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    /**
     *  Just like a JxtaServerSocket, but faulty.
     */
    private static class FaultyJxtaServerSocket extends JxtaServerSocket {

        static volatile double loss = 0.0;

        static volatile double delay = 0.0;

        FaultyJxtaServerSocket(PeerGroup group, PipeAdvertisement pipeAdv, int backlog) throws IOException {
            super(group, pipeAdv, backlog);
        }

        protected JxtaSocket makeEphemeralSocket(PeerGroup group, PipeAdvertisement pipeAdv, PipeAdvertisement itsEphemeralPipeAdv, PeerAdvertisement itsPeerAdv, Credential myCredential, Credential credential, boolean isReliable) throws IOException {
            synchronized (SocketClient.class) {
                SocketClient.FaultyJxtaSocket.loss = loss;
                SocketClient.FaultyJxtaSocket.delay = delay;

                return new SocketClient.FaultyJxtaSocket(group, pipeAdv, itsEphemeralPipeAdv, itsPeerAdv, myCredential, credential, isReliable);
            }
        }
    }

    @Override
    protected void finalize() {
        synchronized (SocketServer.class) {
            if (null != manager) {
                manager.stopNetwork();
                manager = null;
            }
        }
    }

    public static void main(java.lang.String[] args) {
        Thread.currentThread().setName(SocketServer.class.getName() + ".main()");

        try {
            junit.textui.TestRunner.run(suite());
        } finally {
            synchronized (SocketServer.class) {
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
        TestSuite suite = new TestSuite(SocketServer.class);

        return suite;
    }
}
