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
package tutorial.propagated;


import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.document.XMLDocument;
import net.jxta.document.MimeMediaType;

import java.io.File;
import java.io.IOException;


/**
 * Simple example to illustrate the use of propagated pipes
 */
public class PropagatedPipeClient implements PipeMsgListener {
    private InputPipe inputPipe;
    private MessageElement routeAdvElement = null;
    private RouteControl routeControl = null;
    public static final String ROUTEADV = "ROUTE";

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message message = event.getMessage();

        if (message == null) {
            return;
        }
        MessageElement sel = message.getMessageElement(PropagatedPipeServer.NAMESPACE, PropagatedPipeServer.PONGTAG);
        MessageElement nel = message.getMessageElement(PropagatedPipeServer.NAMESPACE, PropagatedPipeServer.SRCNAMETAG);

        if (sel == null) {
            return;
        }
        // Since propagation relies on ip multicast whenever possible, it is to 
        // to be expected that a unicasted message can be intercepted through ip
        // multicast
        System.out.println("Received a pong from :" + nel.toString() + " " + sel.toString());
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        PropagatedPipeClient client = new PropagatedPipeClient();
        NetworkManager manager = null;

        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "PropagatedPipeClient",
                    new File(new File(".cache"), "PropagatedPipeClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PeerGroup netPeerGroup = manager.getNetPeerGroup();
        PipeAdvertisement pipeAdv = PropagatedPipeServer.getPipeAdvertisement();
        PipeService pipeService = netPeerGroup.getPipeService();

        System.out.println("Creating Propagated InputPipe for " + pipeAdv.getPipeID());
        try {
            client.inputPipe = pipeService.createInputPipe(pipeAdv, client);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        MessageTransport endpointRouter = (netPeerGroup.getEndpointService()).getMessageTransport("jxta");

        if (endpointRouter != null) {
            client.routeControl = (RouteControl) endpointRouter.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);
            RouteAdvertisement route = client.routeControl.getMyLocalRoute();

            if (route != null) {
                client.routeAdvElement = new TextDocumentMessageElement(ROUTEADV, (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8), null);
            }
        }

        System.out.println("Creating Propagated OutputPipe for " + pipeAdv.getPipeID());
        OutputPipe output = null;

        try {
            output = pipeService.createOutputPipe(pipeAdv, 1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        int i = 0;

        try {
            while (i < 10000000) {
                Message ping = new Message();
                ping.addMessageElement(PropagatedPipeServer.NAMESPACE,
                        new StringMessageElement(PropagatedPipeServer.SRCIDTAG, netPeerGroup.getPeerID().toString(), null));
                ping.addMessageElement(PropagatedPipeServer.NAMESPACE,
                        new StringMessageElement(PropagatedPipeServer.SRCNAMETAG, netPeerGroup.getPeerName() + " #" + i++, null));
                if (client.routeAdvElement != null && client.routeControl != null) {
                    ping.addMessageElement(PropagatedPipeServer.NAMESPACE, client.routeAdvElement);
                }

                System.out.println("Sending message :" + (i - 1));
                boolean sucess = output.send(ping);
                System.out.println("Send oing message status :"+sucess);
            }
            Thread.sleep(3000);
            manager.stopNetwork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

