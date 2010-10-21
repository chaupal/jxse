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
package net.jxta.document;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleClassAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * ServiceServer side: This is the server side of the JXTA-EX1 example. The
 * server side application advertises the JXTA-EX1 service, starts the
 * service, and receives messages on a service defined pipe
 * endpoint. The service associated module spec and class
 * advertisement are published in the NetPeerGroup. Clients can
 * discover the module advertisements and create output pipeService to
 * connect to the service. The server application creates an input
 * pipe that waits to receive messages. Each message received is
 * printed to the screen. We run the server as a daemon in an infinite
 * loop, waiting to receive client messages.
 */
public class ServiceServer {

    static PeerGroup netPeerGroup = null;
    static PeerGroupAdvertisement groupAdvertisement = null;
    private DiscoveryService discovery;
    private PSEMembershipService membershipService;
    private PipeService pipeService;
    private InputPipe serviceInputPipe;
    private NetworkManager manager;

    /**
     * A pre-baked PipeID string
     */
    public final static String PIPEIDSTR = "urn:jxta:uuid-9CCCDF5AD8154D3D87A391210404E59BE4B888209A2241A4A162A10916074A9504";

    public static void main(String args[]) {
System.setProperty("net.jxta.logging.Logging", "OFF");
System.setProperty("net.jxta.level", "OFF");
        ServiceServer myapp = new ServiceServer();
        System.out.println("Starting Service Peer ....");
        myapp.startJxta();
//        System.out.println("Good Bye ....");
//        System.exit(0);
    }

    private void startJxta() {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "ServiceServer",
                    new File(new File(".cache"), "ServiceServer").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        netPeerGroup = manager.getNetPeerGroup();
        // this is how to obtain the netPeerGroup advertisement
        groupAdvertisement = netPeerGroup.getPeerGroupAdvertisement();

        // get the discovery, and pipe service
        System.out.println("Getting DiscoveryService");
        discovery = netPeerGroup.getDiscoveryService();
        System.out.println("Getting PSEMembershipService");
        membershipService = (PSEMembershipService)netPeerGroup.getMembershipService();
        System.out.println("Getting PipeService");
        pipeService = netPeerGroup.getPipeService();
        startServer();
    }

    /**
     * Creates the pipe advertisement
     * pipe ID
     *
     * @return the pre-defined Pipe Advertisement
     */
    public static PipeAdvertisement createPipeAdvertisement() {
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

    private void startServer() {

        System.out.println("Start the ServiceServer daemon");
        try {

            // First create the Module class advertisement associated with the service
            // We build the module class advertisement using the advertisement
            // Factory class by passing it the type of the advertisement we
            // want to construct. The Module class advertisement is to be used
            // to simply advertise the existence of the service. This is a
            // a very small advertisement that only advertise the existence
            // of service. In order to access the service, a peer will
            // have to discover the associated module spec advertisement.
            ModuleClassAdvertisement mcadv = (ModuleClassAdvertisement)
                    AdvertisementFactory.newAdvertisement(ModuleClassAdvertisement.getAdvertisementType());

            mcadv.setName("JXTAMOD:JXTA-EX1");
            mcadv.setDescription("Tutorial example to use JXTA module advertisement Framework");

            ModuleClassID mcID = IDFactory.newModuleClassID();

            mcadv.setModuleClassID(mcID);
            
            PSECredential credential = (PSECredential)membershipService.getDefaultCredential();

            boolean includePublicKey = true;
            boolean includePeerID = true;
            mcadv.sign(credential, includePublicKey, includePeerID);

            // Ok the Module Class advertisement was created, just publish
            // it in my local cache and to my peergroup. This
            // is the NetPeerGroup
            discovery.publish(mcadv);
            discovery.remotePublish(mcadv);

            // Create the Module Spec advertisement associated with the service
            // We build the module Spec Advertisement using the advertisement
            // Factory class by passing in the type of the advertisement we
            // want to construct. The Module Spec advertisement will contain
            // all the information necessary for a client to contact the service
            // for instance it will contain a pipe advertisement to
            // be used to contact the service

            ModuleSpecAdvertisement mdadv = (ModuleSpecAdvertisement)
                    AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());

            // Setup some of the information field about the servive. In this
            // example, we just set the name, provider and version and a pipe
            // advertisement. The module creates an input pipeService to listen
            // on this pipe endpoint.
            //
            mdadv.setName("JXTASPEC:JXTA-EX1");
            mdadv.setVersion("Version 1.0");
            mdadv.setCreator("sun.com");
            mdadv.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
            mdadv.setSpecURI("http://www.jxta.org/Ex1");

            // Create a pipe advertisement for the Service. The client MUST use
            // the same pipe advertisement to talk to the server. When the client
            // discovers the module advertisement it will extract the pipe
            // advertisement to create its pipe. So, we are reading the pipe
            // advertisement from a default config file to ensure that the
            // service will always advertise the same pipe
            PipeAdvertisement pipeadv = createPipeAdvertisement();

            // Store the pipe advertisement in the spec adv.
            // This information will be retrieved by the client when it will
            // connect to the service
            mdadv.setPipeAdvertisement(pipeadv);

            // display the advertisement as a plain text document.
            StructuredTextDocument doc = (StructuredTextDocument) mdadv.getDocument(MimeMediaType.XMLUTF8);

            StringWriter out = new StringWriter();

            doc.sendToWriter(out);
            System.out.println(out.toString());
            out.close();

            boolean isSigned = mdadv.sign(credential, includePublicKey, includePeerID);
        System.out.println("ModuleSpecAdvertisement is signed = " + isSigned);

//        System.out.println(mdadv);

            // Ok the Module advertisement was created, just publish
            // it in my local cache and into the NetPeerGroup.
            discovery.publish(mdadv);
            discovery.remotePublish(mdadv);

            // we are now ready to start the service create the input pipe endpoint clients will
            // use to connect to the service
            serviceInputPipe = pipeService.createInputPipe(pipeadv);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("ServiceServer: Error publishing the module");
        }

//        // Ok no way to stop this daemon, but that's beyond the point of the example!
//        while (true) {
//            // loop over every input received from clients
//            //System.out.println("Waiting for client messages to arrive");
//            Message msg;
//
//            try {
//                // Listen on the pipe for a client message
//                msg = serviceInputPipe.waitForMessage();
//                if (msg == null)
//                    continue;
//            } catch (Exception e) {
//                serviceInputPipe.close();
//                System.out.println("ServiceServer: Error listening for message");
//                return;
//            }
//
//            // Read the message as a String
//            String ip = null;
//
//            try {
//                // NOTE: The ServiceClient and Service have to agree on the tag names.
//                // this is part of the Service protocol defined  to access the service.
//
//                // get all the message elements
//                Message.ElementIterator en = msg.getMessageElements();
//
//                if (!en.hasNext()) {
//                    return;
//                }
//                // get the message element named SenderMessage
//                MessageElement msgElement = msg.getMessageElement(null, "DataTag");
//
//                // Get message
//                if (msgElement.toString() != null) {
//                    ip = msgElement.toString();
//                }
//
//                if (ip != null) {
//                    // read the data
//                    //System.out.println("ServiceServer: receive message: " + ip);
//                } else {
//                    //System.out.println("ServiceServer: error could not find the tag");
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println("ServiceServer: error receiving message");
//            }
//        }
    }
}
