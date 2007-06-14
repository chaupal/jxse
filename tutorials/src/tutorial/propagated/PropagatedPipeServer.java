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

package tutorial.propagated;


import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.impl.endpoint.router.EndpointRouter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Hashtable;
import java.util.logging.Level;


/**
 * Simple example to illustrate the use of propagated pipes
 */

public class PropagatedPipeServer implements PipeMsgListener {

    /**
     * Source PeerID
     */
    public final static String SRCIDTAG = "SRCID";

    /**
     * Source Peer Name
     */
    public final static String SRCNAMETAG = "SRCNAME";

    /**
     * Pong TAG name
     */
    public final static String PONGTAG = "PONG";

    /**
     * Tutorial message name space
     */
    public final static String NAMESPACE = "PROPTUT";
    private PeerGroup netPeerGroup = null;

    /**
     * Common propagated pipe id
     */
    public final static String PIPEIDSTR = "urn:jxta:uuid-59616261646162614E504720503250336FA944D18E8A4131AA74CE6F4BF85DEF04";
    private final static String completeLock = "completeLock";
    private static PipeAdvertisement pipeAdv = null;
    private static PipeService pipeService = null;
    InputPipe inputPipe = null;
    private transient Map<PeerID, OutputPipe> pipeCache = new Hashtable<PeerID, OutputPipe>();
    public static final String ROUTEADV = "ROUTE";
    private RouteControl routeControl = null;
    private MessageElement routeAdvElement = null;

    /**
     * Gets the pipeAdvertisement attribute of the PropagatedPipeServer class
     *
     * @return The pipeAdvertisement value
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
        advertisement.setType(PipeService.PropagateType);
        advertisement.setName("Propagated Pipe Tutorial");
        return advertisement;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message message = event.getMessage();

        if (message == null) {
            return;
        }
        MessageElement sel = message.getMessageElement(NAMESPACE, SRCIDTAG);
        MessageElement nel = message.getMessageElement(NAMESPACE, SRCNAMETAG);

        // check for a route advertisement and train the endpoint router with the new route
        processRoute(message);
        if (sel == null) {
            return;
        }
        System.out.println("Received a Ping from :" + nel.toString());
        System.out.println("Source PeerID :" + sel.toString());
        Message pong = new Message();

        pong.addMessageElement(NAMESPACE, new StringMessageElement(PONGTAG, nel.toString(), null));
        pong.addMessageElement(NAMESPACE, new StringMessageElement(SRCNAMETAG, netPeerGroup.getPeerName(), null));

        OutputPipe outputPipe = null;
        PeerID pid = null;

        try {
            pid = (PeerID) IDFactory.fromURI(new URI(sel.toString()));
            if (pid != null) {
                // Unicast the Message back. One should expect this to be unicast
                // in Rendezvous only propagation mode.
                // create a op pipe to the destination peer
                if (!pipeCache.containsKey(pid)) {
                    // Unicast datagram
                    // create a op pipe to the destination peer
                    outputPipe = pipeService.createOutputPipe(pipeAdv, Collections.singleton(pid), 1);
                    pipeCache.put(pid, outputPipe);
                } else {
                    outputPipe = pipeCache.get(pid);
                }
                outputPipe.send(pong);
            } else {
                // send it to all
                System.out.println("unable to create a peerID from :" + sel.toString());
                outputPipe = pipeService.createOutputPipe(pipeAdv, 1000);
                outputPipe.send(pong);
            }
        } catch (IOException ex) {
            if (pid != null && outputPipe != null) {
                outputPipe.close();
                outputPipe = null;
                pipeCache.remove(pid);
            }
            ex.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Keep running, avoids existing
     */
    private void waitForever() {
        try {
            System.out.println("Waiting for Messages.");
            synchronized (completeLock) {
                completeLock.wait();
            }
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRoute(final Message msg) {
        try {
            final MessageElement routeElement = msg.getMessageElement(NAMESPACE, ROUTEADV);

            if (routeElement != null && routeControl != null) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(routeElement.getMimeType()
                        ,
                        routeElement.getStream());
                final RouteAdvertisement route = (RouteAdvertisement)
                        AdvertisementFactory.newAdvertisement(asDoc);

                routeControl.addRoute(route);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        PropagatedPipeServer server = new PropagatedPipeServer();

        pipeAdv = getPipeAdvertisement();
        NetworkManager manager = null;

        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "PropagatedPipeServer"
                    ,
                    new File(new File(".cache"), "PropagatedPipeServer").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        server.netPeerGroup = manager.getNetPeerGroup();
        pipeService = server.netPeerGroup.getPipeService();

        MessageTransport endpointRouter = (server.netPeerGroup.getEndpointService()).getMessageTransport("jxta");

        if (endpointRouter != null) {
            server.routeControl = (RouteControl) endpointRouter.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);
            RouteAdvertisement route = server.routeControl.getMyLocalRoute();

            if (route != null) {
                server.routeAdvElement = new TextDocumentMessageElement(ROUTEADV
                        ,
                        (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8), null);
            }
        }

        System.out.println("Creating Propagated InputPipe for " + pipeAdv.getPipeID());
        try {
            server.inputPipe = pipeService.createInputPipe(pipeAdv, server);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        server.waitForever();
        server.inputPipe.close();
        manager.stopNetwork();
    }
}

