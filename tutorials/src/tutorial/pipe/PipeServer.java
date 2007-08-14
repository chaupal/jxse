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
package tutorial.pipe;


import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.CountingOutputStream;
import net.jxta.util.DevNullOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.Date;


/**
 * This tutorial illustrates the use of JXTA Pipes to exchange messages.
 * <p/>
 * This peer is the pipe "server". It opens the pipe for input and waits for
 * messages to be sent. Whenever a Message is received from a "client" the
 * contents are printed.
 */
public class PipeServer implements PipeMsgListener {

    static PeerGroup netPeerGroup = null;

    /**
     * Network is JXTA platform wrapper used to configure, start, and stop the
     * the JXTA platform
     */
    transient NetworkManager manager;
    private PipeService pipeService;
    private PipeAdvertisement pipeAdv;
    private InputPipe inputPipe = null;

    /**
     * Constructor for the PipeServer object
     */
    public PipeServer() {
        manager = null;
        try {
            manager = new net.jxta.platform.NetworkManager(NetworkManager.ConfigMode.ADHOC, "PipeServer",
                    new File(new File(".cache"), "PipeServer").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Get the NetPeerGroup
        netPeerGroup = manager.getNetPeerGroup();
        // get the pipe service, and discovery
        pipeService = netPeerGroup.getPipeService();
        // create the pipe advertisement
        pipeAdv = PipeClient.getPipeAdvertisement();
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        PipeServer server = new PipeServer();
        server.start();
    }

    /**
     * Dumps the message content to stdout
     *
     * @param msg     the message
     * @param verbose dumps message element content if true
     */
    public static void printMessageStats(Message msg, boolean verbose) {
        try {
            CountingOutputStream cnt;
            ElementIterator it = msg.getMessageElements();

            System.out.println("------------------Begin Message---------------------");
            WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, new MimeMediaType("application/x-jxta-msg"), null);

            System.out.println("Message Size :" + serialed.getByteLength());
            while (it.hasNext()) {
                MessageElement el = it.next();
                String eName = el.getElementName();

                cnt = new CountingOutputStream(new DevNullOutputStream());
                el.sendToStream(cnt);
                long size = cnt.getBytesWritten();

                System.out.println("Element " + eName + " : " + size);
                if (verbose) {
                    System.out.println("[" + el + "]");
                }
            }
            System.out.println("-------------------End Message----------------------");
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * Creates the input pipe with this as the message listener
     */
    public void start() {

        try {
            System.out.println("Creating input pipe");
            // Create the InputPipe and register this for message arrival
            // notification call-back
            inputPipe = pipeService.createInputPipe(pipeAdv, this);
        } catch (IOException io) {
            io.printStackTrace();
            return;
        }
        if (inputPipe == null) {
            System.out.println(" cannot open InputPipe");
            System.exit(-1);
        }
        System.out.println("Waiting for msgs on input pipe");
    }

    /**
     * Closes the output pipe and stops the platform
     */
    public void stop() {
        // Close the input pipe
        inputPipe.close();
        // Stop JXTA
        manager.stopNetwork();
    }

    /**
     * PipeMsgListener interface for asynchronous message arrival notification
     *
     * @param event the message event
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message msg;
        try {
            // Obtain the message from the event
            msg = event.getMessage();
            if (msg == null) {
                System.out.println("Received an empty message");
                return;
            }
            // dump the message content to screen
            printMessageStats(msg, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // get all the message elements
        Message.ElementIterator en = msg.getMessageElements();

        if (!en.hasNext()) {
            return;
        }

        // get the message element in the name space PipeClient.MESSAGE_NAME_SPACE
        MessageElement msgElement = msg.getMessageElement(null, PipeClient.MESSAGE_NAME_SPACE);

        // Get message
        if (msgElement.toString() == null) {
            System.out.println("null msg received");
        } else {
            Date date = new Date(System.currentTimeMillis());
            System.out.println("Message received at :" + date.toString());
            System.out.println("Message  created at :" + msgElement.toString());
        }
    }
}

