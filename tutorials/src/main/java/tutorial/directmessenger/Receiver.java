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
package tutorial.directmessenger;

import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.platform.NetworkManager;
import java.io.File;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.endpoint.router.RouteController;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.RouteAdvertisement;

/**
 * Simple example to illustrate the use of direct messengers.
 */
public class Receiver {

    private static class ChatUnicastReceiver implements EndpointListener {
        public void processIncomingMessage(Message msg, EndpointAddress source, EndpointAddress destination) {
            MessageElement chat = msg.getMessageElement("Chat");

            if(null != chat) {
                System.out.println(chat.toString());
            }
        }
    }

    private static boolean stopped = false;

    private static String shutDown = "shutdown";
    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        try {
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "DirectMessengerReceiver",
                    new File(new File(".cache"), "DirectMessengerReceiver").toURI());

            manager.startNetwork();

            PeerGroup npg = manager.getNetPeerGroup();

            EndpointService endpoint = npg.getEndpointService();

            EndpointListener chatlistener = new ChatUnicastReceiver();

            endpoint.addIncomingMessageListener(chatlistener, "chatService", npg.getPeerID().getUniqueValue().toString() );

            synchronized(shutDown) {
                try {
                    while(!stopped) {
                        shutDown.wait(5000);

                        System.out.println("Proudly announcing the existance of " + npg.getPeerID());

                        RouteController routeControl = endpoint.getEndpointRouter().getRouteController();

                        RouteAdvertisement myRoute = routeControl.getLocalPeerRoute();

                        Message announce = new Message();

                        XMLDocument routeDoc = (XMLDocument) myRoute.getDocument(MimeMediaType.XMLUTF8);

                        announce.addMessageElement(new TextDocumentMessageElement("ChatAnnounce", routeDoc, null));

                        endpoint.propagate(announce, "chatAnnounce", null);
                    }
                } catch( InterruptedException woken ) {
                    Thread.interrupted();
                }
            }

            endpoint.removeIncomingMessageListener( "chatService", npg.getPeerID().getUniqueValue().toString() );

            manager.stopNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

