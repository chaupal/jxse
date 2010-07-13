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

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * This tutorial illustrates the use of JXTA Pipes to exchange messages.
 * <p/>
 * This peer is the pipe "client". It opens the pipe for output and when it
 * resolves (finds a listening peer) it sends a message to the "server".
 */
public class PipeClient implements OutputPipeListener {

    /**
     * The tutorial message name space
     */
    public final static String MESSAGE_NAME_SPACE = "PipeTutorial";
    private boolean waitForRendezvous = false;
    private PipeService pipeService;
    private PipeAdvertisement pipeAdv;
    private OutputPipe outputPipe;
    private final Object lock = new Object();

    /**
     * Network is JXTA platform wrapper used to configure, start, and stop the
     * the JXTA platform
     */
    private NetworkManager manager;

    /**
     * A pre-baked PipeID string
     */
    public final static String PIPEIDSTR = "urn:jxta:uuid-59616261646162614E50472050325033C0C1DE89719B456691A596B983BA0E1004";

    /**
     * Create this instance and starts the JXTA platform
     *
     * @param waitForRendezvous indicates whether to wait for a rendezvous connection
     */
    public PipeClient(boolean waitForRendezvous) {
        this.waitForRendezvous = waitForRendezvous;
        try {
            manager = new net.jxta.platform.NetworkManager(NetworkManager.ConfigMode.ADHOC, "PipeClient",
                    new File(new File(".cache"), "PipeClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // get the pipe service, and discovery
        pipeService = manager.getNetPeerGroup().getPipeService();
        // create the pipe advertisement
        pipeAdv = getPipeAdvertisement();
    }

    /**
     * main
     *
     * @param args command line arguments
     */
    public static void main(String args[]) {
        // by setting this property it will trigger a wait for a rendezvous
        // connection prior to attempting to resolve the pipe
        String value = System.getProperty("RDVWAIT", "false");
        boolean waitForRendezvous = Boolean.valueOf(value);
        PipeClient client = new PipeClient(waitForRendezvous);

        client.start();
    }

    /**
     * Creates the pipe advertisement
     * pipe ID
     *
     * @return the pre-defined Pipe Advertisement
     */
    public static PipeAdvertisement getPipeAdvertisement() {
        PipeID pipeID = null;

        try {
            pipeID = (PipeID) IDFactory.fromURI(new URI(PIPEIDSTR));
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        PipeAdvertisement advertisement = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName("Pipe tutorial");
        return advertisement;
    }

    /**
     * the thread which creates (resolves) the output pipe
     * and sends a message once it's resolved
     */
    public synchronized void start() {
        try {
            if (waitForRendezvous) {
                System.out.println("Waiting for Rendezvous Connection");
                // wait indefinitely until connected to a rendezvous
                manager.waitForRendezvousConnection(0);
                System.out.println("Connected to Rendezvous, attempting to create a OutputPipe");
            }
            // issue a pipe resolution asynchronously. outputPipeEvent() is called
            // once the pipe has resolved
            pipeService.createOutputPipe(pipeAdv, this);
            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted");
            }
        } catch (IOException e) {
            System.out.println("OutputPipe creation failure");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * by implementing OutputPipeListener we must define this method which
     * is called when the output pipe is created
     *
     * @param event event object from which to get output pipe object
     */
    public void outputPipeEvent(OutputPipeEvent event) {

        System.out.println("Received the output pipe resolution event");
        // get the output pipe object
        outputPipe = event.getOutputPipe();

        Message msg;

        try {
            System.out.println("Sending message");
            // create the message
            msg = new Message();
            Date date = new Date(System.currentTimeMillis());
            // add a string message element with the current date
            StringMessageElement sme = new StringMessageElement(MESSAGE_NAME_SPACE, date.toString(), null);

            msg.addMessageElement(null, sme);
            // send the message
            outputPipe.send(msg);
            System.out.println("message sent");
        } catch (IOException e) {
            System.out.println("failed to send message");
            e.printStackTrace();
            System.exit(-1);
        }
        stop();
    }

    /**
     * Closes the output pipe and stops the platform
     */
    public void stop() {
        // Close the output pipe
        outputPipe.close();
        // Stop JXTA
        manager.stopNetwork();
        synchronized (lock) {
            // done.
            lock.notify();
        }
    }
}
