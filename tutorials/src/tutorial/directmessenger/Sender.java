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
import java.io.IOException;
import java.util.Date;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.RouteAdvertisement;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

/**
 * Simple example to illustrate the use of direct messengers.
 */
public class Sender {
    
    private static class ChatAnnounceReceiver implements EndpointListener {
        private final EndpointService endpoint;
        
        public ChatAnnounceReceiver(EndpointService endpoint) {
            this.endpoint = endpoint;
        }
        
        public void processIncomingMessage(Message msg, EndpointAddress source, EndpointAddress destination) {
            MessageElement announce = msg.getMessageElement("ChatAnnounce");
            
            if(null != announce) {
                RouteAdvertisement route;
                
                try {
                    XMLDocument routeDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(announce);
                    
                    route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(routeDoc);
                } catch(Exception bad) {
                    System.err.println("### - Bad Route");
                    bad.printStackTrace(System.err);
                    return;
                }
                
                System.out.println("Announcement from " + route.getDestPeerID() );
                
                for(EndpointAddress anEA : route.getDestEndpointAddresses()) {
                    if("tcp".equals(anEA.getProtocolName())) {
                        System.out.println("Sending response to " + anEA );
                        
                        EndpointAddress destAddress = new EndpointAddress(anEA, "EndpointService:jxta-NetGroup", "chatService/" + route.getDestPeerID().getUniqueValue().toString() );
                        
                        MessageSender tcp = (MessageSender) endpoint.getMessageTransport("tcp");
                        
                        Messenger directMessenger = null;
                        try {
                            directMessenger = tcp.getMessenger(destAddress, null);
                            
                            if(null == directMessenger) {
                                System.err.println("### - getMessenger failed for " + anEA );
                                return;
                            }
                            
                            String chatMessage = "Hello from " + endpoint.getGroup().getPeerID() + " via " + anEA + " @ " + new Date();
                            
                            Message chat = new Message();
                            
                            chat.addMessageElement(new StringMessageElement("Chat", chatMessage, null));
                                                        
                            // FIXME directMessenger.sendMessageB(chat, null, null);
                            directMessenger.sendMessageB(chat, "EndpointService:jxta-NetGroup", "chatService/" + route.getDestPeerID().getUniqueValue().toString());
                        } catch(IOException failed) {
                            failed.printStackTrace(System.err);
                        } finally {
                            if(null != directMessenger) {
                                directMessenger.close();
                            }
                        }                        
                    }
                }
            }
        }
    }
    
    private static boolean stopped = false;
    
    private static String shutDown = new String("shutdown");
    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        try {
            NetworkManager manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "DirectMessengerSender",
                    new File(new File(".cache"), "DirectMessengerSender").toURI());
            
            manager.startNetwork();
            
            PeerGroup npg = manager.getNetPeerGroup();
            
            EndpointService endpoint = npg.getEndpointService();
            
            EndpointListener chatAnnouncelistener = new ChatAnnounceReceiver(endpoint);
            
            endpoint.addIncomingMessageListener(chatAnnouncelistener, "chatAnnounce", null );
            
            synchronized(shutDown) {
                try {
                    while(!stopped) {
                        shutDown.wait(5000);
                    }
                } catch( InterruptedException woken ) {
                    Thread.interrupted();
                }
            }
            
            endpoint.removeIncomingMessageListener( "chatAnnounce", null );
            
            manager.stopNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

