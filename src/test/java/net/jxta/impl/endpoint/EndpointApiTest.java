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

package net.jxta.impl.endpoint;

import junit.framework.*;

import net.jxta.peergroup.*;
import net.jxta.endpoint.*;
import org.junit.Ignore;

import java.io.IOException;

@Ignore("JXTA Configurator Required")
public class EndpointApiTest extends TestCase implements EndpointListener, MessengerEventListener, OutgoingMessageEventListener {

    static PeerGroup pg;

    boolean hasMessage = false;
    boolean hasMessenger = false;
    boolean msgSent = false;
    int msgrCounter = 0;

    private synchronized boolean waitForMessage() {
        try {
            if (!hasMessage) {
                wait(10000);
            }
        } catch (InterruptedException ie) {}
        if (!hasMessage) {
            return false;
        }
        hasMessage = false;
        return true;
    }

    public void processIncomingMessage(Message message, EndpointAddress src, EndpointAddress dst) {
        synchronized (this) {
            hasMessage = true;
            notify();
        }

    }

    private synchronized void clearMsgrCounter() {
        msgrCounter = 0;
    }

    private synchronized int getMsgrCounter() {
        return msgrCounter;
    }

    private synchronized boolean waitForMessenger() {
        try {
            if (!hasMessenger) {
                wait(10000);
            }
        } catch (InterruptedException ie) {}
        if (!hasMessenger) {
            return false;
        }
        hasMessenger = false;
        return true;
    }

    public boolean messengerReady(MessengerEvent evt) {
        if (evt.getMessenger() == null) {
            return true;
        }
        synchronized (this) {
            hasMessenger = true;
            msgrCounter++;
            notify();
        }
        return true;
    }

    private synchronized boolean waitForMessageSent() {
        try {
            if (!msgSent) {
                wait(10000);
            }
        } catch (InterruptedException ie) {}
        if (!msgSent) {
            return false;
        }
        msgSent = false;
        return true;
    }

    public void messageSendFailed(OutgoingMessageEvent evt) {
        synchronized (this) {
            msgSent = true;
            notify();
        }
    }

    public void messageSendSucceeded(OutgoingMessageEvent evt) {
        synchronized (this) {
            msgSent = true;
            notify();
        }
    }

    public EndpointApiTest(java.lang.String testName) throws net.jxta.exception.PeerGroupException {

        super(testName);
        System.setProperty("net.jxta.tls.password", "password");
        System.setProperty("net.jxta.tls.principal", "password");

        synchronized (EndpointApiTest.class) {
            if (null == pg) {
                pg = null; // PeerGroupFactory.newNetPeerGroup(PeerGroupFactory.newPlatform());
            }
        }
    }

    public void testGetMessenger() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        // Remove listener from previous test.
        endp.removeIncomingMessageListener("EndpointApiTest", "0");

        if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
            fail("Could not add listener");
        }

        // AsyncTest
        EndpointAddress localAddr = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString(), "EndpointApiTest", "0");
        Messenger m = endp.getMessengerImmediate(localAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        try {
            m.sendMessageB(new Message(), null, null);
        } catch (IOException ioe) {
            fail("Cannot send messages to unresolved messenger");
        }

        int n = 0;

        while ((m.getState() & Messenger.RESOLVED) == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {}
            if (n++ > 10) {
                break;
            }
        }
        while ((m.getState() & Messenger.RESOLVED) == 0) {
            fail("could not resolve immediate messenger to local peer");
        }

        if (!waitForMessage()) {
            try {
                m.sendMessageB(new Message(), null, null);
            } catch (IOException ioe) {
                fail("messenger resolved automatically, but cannot send messages");
            }
            if (!waitForMessage()) {
                fail("messenger resolved automatically, but messages get lost after that");
            }
            fail("messenger resolved automatically, but initial message was lost");
        }

        // Leak test
        for (int i = 0; i < 1000000; i++) {
            m = endp.getMessengerImmediate(localAddr, null);
        }
        m = null;

        // Bottleneck test
        for (int i = 0; i < 1000000; i++) {
            EndpointAddress changingSvc = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString()
                    ,
                    "EndpointApiTest", "" + i);

            m = endp.getMessengerImmediate(changingSvc, null);
        }
        m = null;

    }

    public void testGetMessengerListener() {

        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        // Listener legacy api test.
        EndpointAddress localAddr = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString(), "EndpointApiTest", "0");

        clearMsgrCounter();
        endp.getMessenger(this, localAddr, null);
        endp.getMessenger(this, localAddr, null);
        endp.getMessenger(this, localAddr, null);
        while (getMsgrCounter() != 3) {
            if (!waitForMessenger()) {
                fail("could not get messenger via listener. got only " + getMsgrCounter());
            }
        }
    }

    public void testSendMessageListener() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        // Remove listener from previous test.
        endp.removeIncomingMessageListener("EndpointApiTest", "0");

        if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
            fail("Could not add listener");
        }

        // AsyncTest
        EndpointAddress localAddr = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString(), "EndpointApiTest", "0");
        Messenger m = endp.getMessengerImmediate(localAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        m.sendMessage(new Message(), null, null, this);

        int n = 0;

        while ((m.getState() & Messenger.RESOLVED) == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {}
            if (n++ > 10) {
                break;
            }
        }
        while ((m.getState() & Messenger.RESOLVED) == 0) {
            fail("could not resolve immediate messenger to local peer");
        }

        if (!waitForMessageSent()) {
            if (!waitForMessage()) {
                try {
                    m.sendMessageB(new Message(), null, null);
                } catch (IOException ioe) {
                    fail("messenger resolved automatically, but cannot send messages. sendMessage Listener was NOT invoked.");
                }
                if (!waitForMessage()) {
                    fail(
                            "messenger resolved automatically, but messages get lost after that. sendMessage Listener was NOT invoked.");
                }
                fail("messenger resolved automatically, but initial message was lost. sendMessage Listener was NOT invoked.");
            }
            fail("Message was sent but senMessageListener was NOT invoked.");
        }

        if (!waitForMessage()) {
            try {
                m.sendMessageB(new Message(), null, null);
            } catch (IOException ioe) {
                fail("messenger resolved automatically, but cannot send messages. sendMassage Listener was invoked.");
            }
            if (!waitForMessage()) {
                fail("messenger resolved automatically, but messages get lost after that. sendMassage Listener was invoked.");
            }
            fail("messenger resolved automatically, but initial message was lost. sendMassage Listener was invoked.");
        }
    }

    public void testCantGetRawMessenger() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        EndpointAddress badAddr = new EndpointAddress("tcp://1.1.1.1:1/EndpointApiTest/0");
        Messenger m = endp.getMessengerImmediate(badAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        try {
            m.sendMessageB(new Message(), null, null);
        } catch (IOException ioe) {
            fail("Cannot send messages to unresolved messenger");
        }

        // Leak test
        for (int i = 0; i < 1000000; i++) {
            m = endp.getMessengerImmediate(badAddr, null);
        }
        m = null;

        // Bottleneck test
        for (int i = 0; i < 1000000; i++) {
            EndpointAddress changingSvc = new EndpointAddress("tcp://1.1.1.1:1/EndpointApiTest/" + i);

            m = endp.getMessengerImmediate(changingSvc, null);
        }
        m = null;
    }

    public void testCantGetRouterMessenger() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        EndpointAddress badAddr = new EndpointAddress(
                "jxta://uuid-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA03/EndpointApiTest/0");

        Messenger m = endp.getMessengerImmediate(badAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        try {
            m.sendMessageB(new Message(), null, null);
        } catch (IOException ioe) {
            fail("Cannot send messages to unresolved messenger");
        }

        // Leak test
        for (int i = 0; i < 1000000; i++) {
            m = endp.getMessengerImmediate(badAddr, null);
        }
        m = null;

        // Bottleneck test
        for (int i = 0; i < 1000000; i++) {
            EndpointAddress changingSvc = new EndpointAddress("tcp://1.1.1.1:1/EndpointApiTest/" + i);

            m = endp.getMessengerImmediate(changingSvc, null);
        }
        m = null;
    }

    // This will exhaust the heap if channel caching does not work for unresolved messengers.
    public void testMessageQueueHog() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        EndpointAddress badAddr = new EndpointAddress("tcp://1.1.1.1:1/EndpointApiTest/0");

        Messenger m = endp.getMessengerImmediate(badAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        try {
            m.sendMessageB(new Message(), null, null);
        } catch (IOException ioe) {
            fail("Cannot send messages through unresolved messenger");
        }

        // Leak test...should explode the heap, as of now.
        for (int i = 0; i < 1000000; i++) {
            m = endp.getMessengerImmediate(badAddr, null);
            m.sendMessageN(new Message(), null, null);
        }
        m = null;
    }

    // This will exhaust the heap if channel caching does not work for resolved messengers.
    public void testMessageQueueHog2() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        EndpointAddress localAddr = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString(), "EndpointApiTest", "0");

        Messenger m = endp.getMessengerImmediate(localAddr, null);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        try {
            m.sendMessageB(new Message(), null, null);
        } catch (IOException ioe) {
            fail("Cannot send messages through unresolved messenger");
        }

        // Leak test...should explode the heap, as of now.
        for (int i = 0; i < 1000000; i++) {
            m = endp.getMessengerImmediate(localAddr, null);
            m.sendMessageN(new Message(), null, null);
        }
        m = null;
    }

    public void testAddRmListener() {

        try {
            Thread.sleep(5000);
        } catch (Exception e) {}

        EndpointService endp = pg.getEndpointService();

        // A few basic tests.

        // Remove listener from previous test.
        endp.removeIncomingMessageListener("EndpointApiTest", "0");

        if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
            fail("Could not add listener");
        }
        if (endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
            fail("Could add redundant listener");
        }
        Object o = endp.removeIncomingMessageListener("EndpointApiTest", "0");

        if (o != this) {
            fail("Wrong listener removed: " + o + " instead of " + this);
        }
        if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
            fail("Could not add/remove/add listener");
        }
        if (endp.removeIncomingMessageListener("EndpointApiTest", "0") != this) {
            fail("Wrong listener add/remove/add/removed");
        }
        if (endp.removeIncomingMessageListener("EndpointApiTest", "0") != null) {
            fail("Listener removed twice");
        }

        // Leak tests.
        EndpointAddress localAddr = new EndpointAddress("jxta", pg.getPeerID().getUniqueValue().toString(), "EndpointApiTest", "0");
        Messenger m = endp.getMessenger(localAddr);

        if (m == null) {
            fail("could not get tcp messenger to local peer...tcp is on ?");
        }

        // add/remove 500,000 times the same listener
        for (int i = 1; i < 500000; i++) {
            if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "0")) {
                fail("Could not add fixed listener after " + i + " cycles");
            }
            try {
                m.sendMessage(new Message());
            } catch (IOException ioe) {
                fail("Cannot send messages to local fixed listener after " + i + " cycles");
            }
            if (!waitForMessage()) {
                fail("Cannot receive messages through fixed listener after " + i + " cycles");
            }
            if (endp.removeIncomingMessageListener("EndpointApiTest", "0") != this) {
                fail("Wrong fixed listener removed after " + i + " cycles");
            }
        }

        // add/remove 500,000 different listeners
        for (int i = 1; i < 500000; i++) {
            if (!endp.addIncomingMessageListener(this, "EndpointApiTest", "" + i)) {
                fail("Could not add variable listener after " + i + " cycles");
            }
            if (endp.removeIncomingMessageListener("EndpointApiTest", "" + i) != this) {
                fail("Wrong variable listener removed after " + i + " cycles");
            }
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(EndpointApiTest.class);

        return suite;
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
//        pg.unref();
        pg = null;
        System.out.flush();
        System.err.flush();
    }
}
