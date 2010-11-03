/*
 * Copyright (c) 2003-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.util.pipe.reliable;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import org.junit.Ignore;

@Ignore("JXTA Configurator required")
public class ReliableTest extends TestCase implements
        RendezvousListener, DiscoveryListener, PipeMsgListener, OutputPipeListener {

    private static int MIN_LOAD = 1024;
    private static int MAX_LOAD = 65536;

    private static final String MESSAGE_TAG = "reliable.message";
    private static final String SENT_AT_TAG = "reliable.sent.at";
    private static final String PAYLOAD_TAG = "reliable.payload";

    private static final MimeMediaType MIME_BINARY = MimeMediaType.AOS;

    private static String MSG_PIPE_NAME = "ReliableTestMsgPipe";
    private static String ACK_PIPE_NAME = "ReliableTestAckPipe";

    private static boolean DEBUG = false;
    private static boolean ADAPTIVE = false;
    private static boolean IS_QUIET = false;
    private static boolean IS_SENDER = false;
    private static boolean IS_SERVER = false;
    private static boolean waitRdv = false;
    private static String PRINCIPAL = "password";
    private static String PASSWORD = "password";
    private static int DROP_MSG = Integer.MAX_VALUE;
    private static int BW_LIMIT = Integer.MAX_VALUE;
    private static int PIPE_LEN = 327680; // 20 packets of 16K
    private static int LATENCY = 0;
    private static int DELAY = 200;
    private static int ITERATIONS = 1000;

    private Object rdvConnectLock = new Object();
    private Random random = new Random(System.currentTimeMillis());

    private int nextMessageId = 0;
    private ArrayList loadElements = null;

    private int dropMsgCount = 0;

    private ScheduledExecutorService scheduledExecutor;
    private ScheduledFuture<?> reliableTestTaskHandle = null;

    private PeerGroup netPeerGroup = null;
    private RendezVousService rendezvousService = null;
    private DiscoveryService discoverySvc = null;
    private PipeService pipeSvc = null;

    PipeAdvertisement msgPipeAdv = null;
    PipeAdvertisement ackPipeAdv = null;

    OutputPipe outputPipe = null;
    InputPipe inputPipe = null;

    OutgoingPipeAdaptorSync outgoing = null;
    IncomingPipeAdaptor incoming = null;

    ReliableOutputStream ros = null;
    ReliableInputStream ris = null;

    BlockingQueue<Message> bwQueue = new LinkedBlockingQueue<Message>(Integer.MAX_VALUE);
    long bwQueueSz = 0;
    long nextInjectTime = 0;
    long delayAdj = 0;
    long roundingLoss = 0;

    long lostToCongestion = 0;

    class TimedMsg implements Runnable {

        long delivDate;

        public TimedMsg(long date) {
            delivDate = date;
        }

        public void run() {
            Message msg;

            synchronized (bwQueue) {

                msg = (Message) bwQueue.poll();
                long msgLen = msg.getByteLength();

                bwQueueSz -= msgLen;

                delayAdj = delivDate - System.currentTimeMillis();
                if (ros != null) {
                    ros.recv(msg);
                } else if (ris != null) {
                    ris.recv(msg);
                }
            }
        }
    }

    private void bwQueueMsg(Message msg) {
        synchronized (bwQueue) {
            long len = msg.getByteLength();

            if (bwQueueSz + len > PIPE_LEN) {
                lostToCongestion++;
                if (!IS_QUIET) {
                    System.out.println("\nSimulating congestion");
                }
                return;
            }
            bwQueue.offer(msg);
            bwQueueSz += len;
        }

        // Schedule delivery of the message based on bw, layency, and
        // current messages in the pipe.
        long now = System.currentTimeMillis();

        // The injection or extraction time depends on length and bandwidth
        long bitsToClock = msg.getByteLength() * 8000 + roundingLoss;
        long delay = bitsToClock / (BW_LIMIT * 1024);
        long roundingLoss = bitsToClock % (BW_LIMIT * 1024);

        // We can inject a message if/after the last byte of the previous one
        // is done injecting. 
        nextInjectTime = Math.max(nextInjectTime, now) + delay;

        // At the new nextInjectTime, we have injected the last byte of the
        // new message. The message is delivered when this last byte arrives.
        long delivDate = nextInjectTime + LATENCY;
        long delivDelay = delivDate - now;

        if (delayAdj >= 10) {
            delivDelay += 10;
            delayAdj -= 10;
        }
        if (delayAdj <= -10) {
            delivDelay -= 10;
            delayAdj += 10;
        }

        // A carefully chosen combination of unrealistic parameters can
        // lead to an attempt at delivering messages in the past.
        if (delivDelay <= 0) {
            delivDelay = 1;
        }

        // Because we strictly serialize messages.
        // Their delivery order is the same than their queuing order. So the
        // timer task only needs to pickup the next message and deliver it.
        reliableTestTaskHandle = scheduledExecutor.scheduleAtFixedRate(new TimedMsg(delivDate), delivDelay, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public ReliableTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ReliableTest.class);

        return suite;
    }

    @Override
    protected void setUp() {
        scheduledExecutor = new ScheduledThreadPoolExecutor(2);
        loadElements = new ArrayList();
        for (int size = MIN_LOAD; size <= MAX_LOAD; size = size << 1) {
            byte[] le = new byte[size];

            random.nextBytes(le);
            loadElements.add(le);
        }

        System.setProperty("net.jxta.tls.password", PASSWORD);
        System.setProperty("net.jxta.tls.principal", PRINCIPAL);

        try {
            netPeerGroup = PeerGroupFactory.newNetPeerGroup(PeerGroupFactory.newPlatform());
            discoverySvc = netPeerGroup.getDiscoveryService();
            pipeSvc = netPeerGroup.getPipeService();
            rendezvousService = netPeerGroup.getRendezVousService();
            rendezvousService.addListener(this);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("failed to start jxta");
        }

        if (waitRdv) {
            System.out.print("connecting to rendezvous...");
            System.out.flush();
            synchronized (rdvConnectLock) {
                while (!rendezvousService.isConnectedToRendezVous()) {
                    System.out.print(".");
                    System.out.flush();
                    try {
                        rdvConnectLock.wait(10 * DELAY);
                    } catch (InterruptedException ignore) {}
                }
            }
            System.out.println(" connected");
        }
    }

    @Override
    public void tearDown() {
            if (reliableTestTaskHandle != null) {
                reliableTestTaskHandle.cancel(false);
                reliableTestTaskHandle = null;
            }

        scheduledExecutor.shutdownNow();
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        parse(args);
        TestRunner.run(suite());
        System.err.flush();
        System.out.flush();
    }

    public static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-quiet")) {
                IS_QUIET = true;
            } else if (args[i].equals("-sender")) {
                IS_SENDER = true;
            } else if (args[i].equals("-receiver")) {
                IS_SENDER = false;
            } else if (args[i].equals("-server")) {
                IS_SENDER = false;
                IS_SERVER = true;
            } else if (args[i].equals("-waitrdv")) {
                waitRdv = true;
            } else if (args[i].equals("-delay") && i + 1 < args.length) {
                String delayStr = args[++i];

                try {
                    DELAY = Integer.parseInt(delayStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid delay: " + delayStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-iterations") && i + 1 < args.length) {
                String iterStr = args[++i];

                try {
                    ITERATIONS = Integer.parseInt(iterStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid iterations: " + iterStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-drop") && i + 1 < args.length) {
                String dropStr = args[++i];

                try {
                    DROP_MSG = Integer.parseInt(dropStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid drop message: " + dropStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-bw") && i + 1 < args.length) {
                String bwStr = args[++i];

                try {
                    BW_LIMIT = Integer.parseInt(bwStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid bw: " + bwStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-pl") && i + 1 < args.length) {
                String plStr = args[++i];

                try {
                    PIPE_LEN = Integer.parseInt(plStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid pl: " + plStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-lat") && i + 1 < args.length) {
                String latStr = args[++i];

                try {
                    LATENCY = Integer.parseInt(latStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid lat: " + latStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-minload") && i + 1 < args.length) {
                String minlStr = args[++i];

                try {
                    MIN_LOAD = Integer.parseInt(minlStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid minload: " + minlStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-maxload") && i + 1 < args.length) {
                String maxlStr = args[++i];

                try {
                    MAX_LOAD = Integer.parseInt(maxlStr);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid maxload: " + maxlStr + USAGE);
                    return;
                }
            } else if (args[i].equals("-password") && i + 1 < args.length) {
                PASSWORD = args[++i];
            } else if (args[i].equals("-principal") && i + 1 < args.length) {
                PRINCIPAL = args[++i];
            } else if (args[i].equals("-name") && i + 1 < args.length) {
                MSG_PIPE_NAME = args[++i] + "MsgPipe";
                ACK_PIPE_NAME = args[i] + "AckPipe";
            } else if (args[i].equals("-debug")) {
                DEBUG = true;
            } else if (args[i].equals("-adapt")) {
                ADAPTIVE = true;
            } else if (args[i].equals("-help")) {
                System.err.println(USAGE);
                System.err.println(HELP);
                return;
            }
        }
        System.out.println(
                (IS_SENDER ? "Sender" : "Receiver") + "\n--------" + "\n quiet:      " + IS_QUIET + "\n delay:      " + DELAY
                + "\n iterations: " + ITERATIONS + "\n drop:       " + DROP_MSG + "\n bw:         " + BW_LIMIT + "\n pl:         "
                + PIPE_LEN + "\n latency:    " + LATENCY + "\n min load:   " + MIN_LOAD + "\n max load:   " + MAX_LOAD
                + "\n adaptive:   " + ADAPTIVE + "\n debug:      " + DEBUG);
    }

    public void rendezvousEvent(RendezvousEvent event) {
        synchronized (rdvConnectLock) {
            rdvConnectLock.notifyAll();
        }
    }

    public void test() {
        if (IS_SENDER) {
            doSender();
            longPause();
            longPause();
            doSender();
        } else {
            do {
                doReceiver();
            } while (IS_SERVER);
        }
    }

    private PipeAdvertisement createPipeAdv(String pipeName) {
        PipeAdvertisement padv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());

        String pipeType = PipeService.UnicastType;
        String composite = pipeName + pipeType;
        byte[] seed = Integer.toHexString(composite.hashCode()).getBytes();
        PeerGroupID pgID = netPeerGroup.getPeerGroupID();
        PipeID pipeId = IDFactory.newPipeID(pgID, seed);

        padv.setName(pipeName);
        padv.setPipeID(pipeId);
        padv.setType(pipeType);

        return padv;
    }

    private void pause() {
        synchronized (this) {
            try {
                wait(DELAY);
            } catch (InterruptedException e) {}
        }
    }

    private void longPause() {
        try {
            synchronized (this) {
                wait(10 * DELAY);
            }
        } catch (InterruptedException e) {}
    }

    private void doSender() {

        try {
            int sequence = 0;

            // Create the ros and the outgoing adaptor to go with it before
            // the outputpipe exists. The pipe will be set when ready. In the
            // meantime, send() would just fail or block. However, in the sender
            // we do not start before the pipes are created; we do not need to.
            // The same trick is more usefull to the receiver.

            outgoing = new OutgoingPipeAdaptorSync(null);
            ros = ADAPTIVE
                    ? new ReliableOutputStream(outgoing, new AdaptiveFlowControl(), scheduledExecutor)
                    : new ReliableOutputStream(outgoing, new FixedFlowControl(40), scheduledExecutor);

            for (int i = 0; i < ITERATIONS; i++) {

                // if we do not already have it resolved, retry
                // to open the output pipe every so often
                if (outputPipe == null) {
                    pause();
                    if ((i % 11) == 0) {
                        PipeAdvertisement padv = IS_SENDER ? msgPipeAdv : ackPipeAdv;

                        if (padv == null) {
                            discoverySvc.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", MSG_PIPE_NAME, 10, this);

                            discoverySvc.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", ACK_PIPE_NAME, 10, this);
                            if (DEBUG) {
                                System.out.println("launched remote discovery for " + MSG_PIPE_NAME + " and " + ACK_PIPE_NAME);
                            }
                            // wait for discovery response to come in
                            continue;
                        }
                        if (DEBUG) {
                            System.out.println("re-resolving output pipe " + padv.getName());
                        }
                        try {
                            pipeSvc.createOutputPipe(padv, this);
                        } catch (IOException ex) {
                            System.err.println(ex.getMessage());
                            return;
                        }
                    }
                }

                // No need to start before we could supply the pipes; messages
                // would go nowhere and/or the other side would not be able to
                // resolve the ack pipe.

                if (outputPipe == null || inputPipe == null) {
                    continue;
                }

            }

            System.out.print("Sending...");
            System.out.flush();

            for (int i = 0; i <= ITERATIONS; i++) {

                Message msg = new Message();

                if (i == ITERATIONS) {
                    msg.addMessageElement(new StringMessageElement(MESSAGE_TAG, "mclose", null));
                } else {
                    String messageId = "m" + Integer.toString(nextMessageId++);

                    msg.addMessageElement(new StringMessageElement(MESSAGE_TAG, messageId, null));
                    // add a random load element
                    int index = random.nextInt(loadElements.size());
                    byte[] le = (byte[]) loadElements.get(index);
                    MessageElement elm = new ByteArrayMessageElement(PAYLOAD_TAG, MIME_BINARY, le, null);

                    msg.addMessageElement(elm);
                }

                msg.addMessageElement(new StringMessageElement(SENT_AT_TAG, Long.toString(System.currentTimeMillis()), null));

                try {
                    sequence = ros.send(msg);
                    // System.out.println(messageId + " " + 
                    // msg.getByteLength() + "b " +
                    // sequence + "seq " +
                    // ros.getMaxAck() + "ack");
                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }
            }

            System.out.print("closing...");
            System.out.flush();
            while (ros.getMaxAck() != sequence) {
                pause();
            }

            ros.close();
            inputPipe.close();
            outputPipe.close();

            msgPipeAdv = null;
            ackPipeAdv = null;

            outputPipe = null;
            inputPipe = null;

            outgoing = null;
            incoming = null;

            ros = null;

            System.out.println("Done");

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void discoveryEvent(DiscoveryEvent event) {
        Enumeration ae = event.getResponse().getResponses();

        while (ae.hasMoreElements()) {
            String str = (String) ae.nextElement();
            // create Advertisement from response
            Advertisement adv = null;
            XMLDocument advDocument = null;

            try {
                advDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument( MimeMediaType.XMLUTF8, new StringReader(str) );
            	
                adv = AdvertisementFactory.newAdvertisement(advDocument);
            } catch (IOException ex) {
                System.err.println("error parsing discovery response");
                System.err.println(ex.getMessage());
                continue;
            }
            if (adv instanceof PipeAdvertisement) {
                PipeAdvertisement pipeAdv = (PipeAdvertisement) adv;
                String pipeName = pipeAdv.getName();

                if (MSG_PIPE_NAME.equals(pipeName)) {
                    msgPipeAdv = pipeAdv;
                    if (DEBUG) {
                        System.out.println("discovered msg pipe: " + pipeName);
                    }
                    try {
                        pipeSvc.createOutputPipe(msgPipeAdv, this);
                        if (DEBUG) {
                            System.out.println("opened msg pipe for output");
                        }
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                } else if (ACK_PIPE_NAME.equals(pipeName)) {
                    ackPipeAdv = pipeAdv;
                    if (DEBUG) {
                        System.out.println("discovered ack pipe: " + pipeName);
                    }
                    try {
                        inputPipe = pipeSvc.createInputPipe(ackPipeAdv, this);
                        if (DEBUG) {
                            System.out.println("opened ack pipe for input");
                        }
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            }
        }
    }

    private void doReceiver() {

        try {
            if (msgPipeAdv == null) {
                msgPipeAdv = createPipeAdv(MSG_PIPE_NAME);
                discoverySvc.publish(msgPipeAdv);
                discoverySvc.remotePublish(msgPipeAdv);
                inputPipe = pipeSvc.createInputPipe(msgPipeAdv, this);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        if (DEBUG) {
            System.out.println("published msg pipe: " + msgPipeAdv.getName());
            System.out.println("opened msg pipe for input");
        }

        // We need to give to our input a reference to our output, but,
        // obviously, we need to create our input before resolving the output
        // pipe, otherwise we do not known when to start trying to resolve.
        // In addition, our output must not loose messages while the pipe is
        // being resolved: these are acks. All the first packets would remain
        // un-acked, which would make for a very slow start.
        // Therefore we need a form of output that can be created before
        // the pipe. This used to be solved by interposing queues and threads.
        // Now, we create the outgoing adaptor without a pipe.
        // The pipe will be set whenever it is ready. In the meantime, the
        // reliable input stream will block or return false if it is used.

        outgoing = new OutgoingPipeAdaptorSync(null);
        ris = new ReliableInputStream(outgoing, 0);

        try {
            if (ackPipeAdv == null) {
                ackPipeAdv = createPipeAdv(ACK_PIPE_NAME);
                discoverySvc.publish(ackPipeAdv);
                discoverySvc.remotePublish(ackPipeAdv);
            }

            pipeSvc.createOutputPipe(ackPipeAdv, this);

        } catch (IOException ex) {
            fail(ex.getMessage());
        }

        if (DEBUG) {
            System.out.println("published ack pipe: " + ackPipeAdv.getName());
            System.out.println("opened ack pipe for output");
        }

        System.out.print("Waiting for sender...");
        System.out.flush();

        while (!ris.hasNextMessage()) {
            pause();
        }

        System.out.println("Receiving");

        for (int i = 0; outputPipe == null;) {

            pause();
            if ((i++ % 11) == 0) {
                if (DEBUG) {
                    System.out.println("re-resolving output pipe " + ackPipeAdv.getName());
                }
                try {
                    pipeSvc.createOutputPipe(ackPipeAdv, this);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }

        long startedAt = 0;
        long bytesTransferred = 0;
        long nbMsgs = 0;

        while (true) {
            Message msg = null;

            try {
                msg = ris.nextMessage(true);
            } catch (IOException ioe) {
                System.err.println("Failed to obtain next message");
                ioe.printStackTrace(); 
            }
            ++nbMsgs;
            printMessageForDebug("doReceiver", msg);

            String msgId = (msg.getMessageElement(MESSAGE_TAG)).toString();
            String sentAt = (msg.getMessageElement(SENT_AT_TAG)).toString();
            long size = msg.getByteLength();

            long now = System.currentTimeMillis();
            long sent = now;

            try {
                sent = Long.parseLong(sentAt);
            } catch (NumberFormatException nex) {
                System.err.println("could not parse msg send time: " + sentAt);
                continue;
            }

            // We start computing averages only after the first message has
            // been received.

            long throughput = 0;

            if (nbMsgs > (2 * ITERATIONS / 3)) {
                if (startedAt == 0) {
                    startedAt = System.currentTimeMillis();
                } else {
                    long totalTime = now - startedAt;

                    if (totalTime <= 0) {
                        // that can't be. Some time has passed.
                        totalTime = 1;
                    }
                    bytesTransferred += size;
                    throughput = (((1000 * 8 * bytesTransferred) / totalTime) / 1024);
                }
            }

            long delay = now - sent;

            if (msgId.equals("mclose")) {
                System.out.println(
                        "\nResults" + "\n-------" + "\ntransferred:       " + bytesTransferred + " bytes"
                        + "\nthroughput:        " + throughput + " kbps" + "\ncongestion events: " + lostToCongestion);
                try {
                    ris.close();
                } catch (IOException ioe1) {}
                ris = null;
                outgoing.close();
                outgoing = null;
                outputPipe = null;
                longPause();
                break;
            }

            if (!IS_QUIET) {
                System.out.print(msgId + " " + size + "b " + delay + "ms " + throughput + "kbps avg\r");
                System.out.flush();
            }

        }
    }

    private void printMessageForDebug(String header, Message msg) {
        if (DEBUG) {
            ElementIterator iter = msg.getMessageElements();

            System.out.print(header + " " + IS_SENDER + " (");
            while (iter.hasNext()) {
                MessageElement el = iter.next();

                System.out.print(el.getElementName());
                if (iter.hasNext()) {
                    System.out.print(", ");
                }
            }
            System.out.println(")");
        }
    }

    public void pipeMsgEvent(PipeMsgEvent inputPipeEvent) {
        Message msg = inputPipeEvent.getMessage();

        if (msg == null) {
            return;
        }

        printMessageForDebug("pipeMsgEvent", msg);

        if (dropMessage()) {

            if (DEBUG) {
                System.out.println("dropped incoming:" + (IS_SENDER ? "ack" : "msg"));
            }
            return;
        }

        if (BW_LIMIT < Integer.MAX_VALUE) {
            bwQueueMsg(msg);
            return;
        }

        // We're redirecting either to the ros or to the ris; depending
        // on whether we're on one side or the other of the reliable stream.
        // The other stream obj is null.

        if (ros != null) {
            ros.recv(msg);
        } else if (ris != null) {
            ris.recv(msg);
        }
    }

    public void outputPipeEvent(OutputPipeEvent outputPipeEvent) {
        String pid = outputPipeEvent.getPipeID();

        // this will happen in the sender
        if (outputPipe == null && pid.equals(msgPipeAdv.getPipeID().toString())) {
            outputPipe = outputPipeEvent.getOutputPipe();
            outgoing.setPipe(outputPipe);

            // the next line is not needed in this case,
            // since here we register 'this' as an
            // PipeMsgListener (Input Pipe Listener) and
            // 'manually' redirect the ack messages to
            // ros.recv() from pipeMsgEvent

            // incoming = new IncomingPipeAdaptor(inputPipe, ros);
            if (DEBUG) {
                System.out.println("resolved msg output pipe " + outputPipe.getName());
            }
        }

        // this will happen in the receiver
        if (outputPipe == null && pid.equals(ackPipeAdv.getPipeID().toString())) {
            outputPipe = outputPipeEvent.getOutputPipe();
            outgoing.setPipe(outputPipe);

            // the next line is not needed in this case,
            // since here we register 'this' as an
            // PipeMsgListener (Input Pipe Listener) and
            // 'manually' redirect the ack messages to
            // ros.recv() from pipeMsgEvent

            // incoming = new IncomingPipeAdaptor(inputPipe, ris);
            if (DEBUG) {
                System.out.println("resolved ack output pipe " + outputPipe.getName());
            }
        }
    }

    synchronized boolean dropMessage() {

        return ((++dropMsgCount) % DROP_MSG) == 0;
    }

    private static final String USAGE = "\nUsage: ReliableTest <options>" + "\n" + "options:\n"
            + "  -help       outputs some usefull advice" + "  -quiet      only output a summary (" + IS_QUIET + ")\n"
            + "  -sender     whether to run as sender of messages (" + IS_SENDER + ")\n"
            + "  -receiver   whether to run as a receiver of messages (" + !IS_SENDER + ")\n"
            + "  -server     whether to run as a permanent receiver of messages (" + IS_SERVER + ")\n"
            + "  -waitrdv    wait for a rendezvous connection before starting (not)\n"
            + "  -drop       drop every Nth messages (on arrival) (" + DROP_MSG + ")\n"
            + "  -bw         simulated bw cap in Kbit/s (on arrival) (" + BW_LIMIT + ")\n"
            + "  -pl         simulated pipe length in bytes (only with bw) (" + PIPE_LEN + ")\n"
            + "  -lat        simulated latency in ms (only with bw) (" + LATENCY + ")\n"
            + "  -minload    smallest of the random payload sizes in bytes (" + MIN_LOAD + ")\n"
            + "  -maxload    largest of the random payload sizes in bytes (" + MAX_LOAD + ")\n"
            + "  -name       base name for the pipes (ReliableTest)\n"
            + "  -debug      whether to turn on debugging in the peer (" + DEBUG + ")\n"
            + "  -adapt      Use adaptive flow control (do not)\n" + "  -iterations number of times to send a message ("
            + ITERATIONS + ")\n" + "  -delay      Basic delay unit (" + DELAY + ")\n"
            + "  -principal  net.jxta.tls.principal property (" + PRINCIPAL + ")\n"
            + "  -password   net.jxta.tls.password property (" + PASSWORD + ")\n";

    private static final String HELP = "Some options serve to simulate particular network conditions.\n"
            + "These conditions are simulated on the destination side of a\n"
            + "link. As a result, the options given to the receiver will\n"
            + "control the behaviour of the data channel, while the options\n"
            + "to the sender will control the behaviour of the ack channel.\n" + "\n" + "These options are the following:\n"
            + "-bw, as in \"bandwidth\":\n" + "\tcontrols the time it takes for the slowest segment of the path\n"
            + "\tto go from begining the transmition of one bit to being ready\n"
            + "\tto transmit the next one. It is expressed in Kbit per second.\n" + "-lat, as in \"latency\":\n"
            + "\tcontrols the time it takes for a bit to traverse the network in\n"
            + "\taddition to the time caused by the bandwidth. In real life, the\n"
            + "\tprincipal contributors to this time would be signal travel time\n"
            + "\tper the laws of physics, and packet processing time at each\n"
            + "\tnode along the path. The essential characteristics of latency,\n"
            + "\tis that over one given packet's latency time, a number of other\n" + "\tpackets can begin traveling as well.\n"
            + "-pl, as in \"path length\":\n" + "\tcontrols the actual amount of data that can be traveling along\n"
            + "\tthe network path (in bytes). In real life, this is the\n"
            + "\tproduct bandwidth * latency + some buffering, but the three\n"
            + "\tvalues can be chosen differently in order to simulate extreme\n"
            + "\tconditions. For example, most often pl would be larger than\n"
            + "\tthe bandwidth*latency product to reflect buffering capacity at\n"
            + "\tvarious nodes along the path. Under non-congested conditions\n"
            + "\tthese buffers are used to store at most one pending packet\n"
            + "\ton each node, which ensures back-to-back link utilization\n"
            + "\tdespite statistical variations. In that case, buffering has\n"
            + "\tno noticeable effect on latency. However, buffering space\n"
            + "\tcan retard congestion when the capacity of a node is exceeded\n"
            + "\t(and the apparent latency will increase).\n" + "\tIn practice, under simulated conditions on a single host, it\n"
            + "\tis impossible to keep the network path fully utilized if\n"
            + "\tpath length is less than twice bandwidth*latency. Any\n"
            + "\trealistic network has at least one extra buffer at each node.\n"
            + "\tSetting a path length smaller than the bandwidth*latency\n"
            + "\tproduct is somewhat artifical. It roughly corresponds to some\n"
            + "\tnode along the path delaying packets with no buffer to store\n"
            + "\tthem while they wait. In effect it results in the bandwidth\n"
            + "\tlowering to match, but makes congestions harder to predict. So\n"
            + "\tit is usefull for testing congestion recovery mechanisms.\n";
}
