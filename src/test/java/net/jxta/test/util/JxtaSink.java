/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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
package net.jxta.test.util;


import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.ClosedChannelException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;


/**
 *JxtaSink is a utility which provides a jxta data sink. it emulates a 
 * a JXTA peer by conforming with the welcome protocol.
 * it's sole purpose is to drain messages
 */
public class JxtaSink implements Runnable {
    private static final int DEFAULT_SINK_PORT = 9901;
    private InetAddress theAddress = null;
    private int port = DEFAULT_SINK_PORT;
    private Thread thread;
    private WelcomeMessage wm = null;

    public JxtaSink(InetAddress address, WelcomeMessage wm) throws Exception {
        this(address, DEFAULT_SINK_PORT, wm);
    }

    public JxtaSink(InetAddress address, int port, WelcomeMessage wm) throws Exception {
        this.theAddress = address;
        this.port = port;
        this.wm = wm;
        thread = new Thread(this, "JxtaSink on address " + address.getHostAddress() + " port : " + port);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        int count = 0;
        Selector acceptSelector = null;
        ServerSocketChannel serverSoc = null;
        InetSocketAddress address = null;

        try {
            acceptSelector = SelectorProvider.provider().openSelector();
            serverSoc = ServerSocketChannel.open();
            serverSoc.configureBlocking(false);
            address = new InetSocketAddress(theAddress, port);
            System.out.println("Waiting for connections on port : " + port);
            serverSoc.socket().bind(address);
        } catch (IOException io) {
            io.printStackTrace();
        }

        SelectionKey acceptKey = null;

        try {
            acceptKey = serverSoc.register(acceptSelector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException cce) {
            cce.printStackTrace();
        }
        int keysAdded = 0;

        try {
            while ((keysAdded = acceptSelector.select()) > 0) {
                Set readyKeys = acceptSelector.selectedKeys();
                Iterator i = readyKeys.iterator();

                while (i.hasNext()) {
                    SelectionKey sk = (SelectionKey) i.next();

                    i.remove();
                    ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();

                    System.out.println("Accepting connection :" + count++);
                    Socket s = nextReady.accept().socket();

                    wm.sendToStream(s.getOutputStream());
                    // Drain the pipe
                    drain(s.getInputStream());
                    // s.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drain(InputStream in) throws IOException {
        int c;
        byte[] buf = new byte[8192];

        do {
            c = in.read(buf);
            if (c == -1) {
                System.out.println("done draining the pipe");
                return;
            }
        } while (true);
    }
}
