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

/**
 * ServiceClient Application: This is the client side of the EX1 example that
 * looks for the JXTA-EX1 service and connects to its advertised pipe. The
 * Service advertisement is published in the NetPeerGroup
 * by the server application. The client discovers the service
 * advertisement and create an output pipe to connect to the service input
 * pipe. The server application creates an input pipe that waits to receive
 * messages. Each message receive is displayed to the screen. The client
 * sends an hello message. 
 */
package net.jxta.document;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.platform.NetworkManager;

import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;

import java.io.IOException;
import java.io.StringWriter;
import java.io.File;
import java.util.Enumeration;

/**
 * ServiceClient Side: This is the client side of the JXTA-EX1
 * application. The client application is a simple example on how to
 * start a client, connect to a JXTA enabled service, and invoke the
 * service via a pipe advertised by the service. The
 * client searches for the module specification advertisement
 * associated with the service, extracts the pipe information to
 * connect to the service, creates a new output to connect to the
 * service and sends a message to the service.
 * The client just sends a string to the service no response
 * is expected from the service.
 */
public class ServiceClient {

    static PeerGroup netPeerGroup = null;
    static PeerGroupAdvertisement groupAdvertisement = null;
    private DiscoveryService discovery;
    private PSEMembershipService membershipService;
    private PipeService pipeService;
    private NetworkManager manager;

    public static void main(String args[]) {
System.setProperty("net.jxta.logging.Logging", "OFF");
System.setProperty("net.jxta.level", "OFF");
        ServiceClient myapp = new ServiceClient();
        System.out.println("Starting ServiceClient peer ....");
        myapp.startJxta();
        System.out.println("Good Bye ....");
        System.exit(0);
    }

    private void startJxta() {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "ServiceClient",
                    new File(new File(".cache"), "ServiceClient").toURI());
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
        startClient();
    }

    // start the client
    private void startClient() {

        // Let's initialize the client
        System.out.println("Start the ServiceClient");

        // Let's try to locate the service advertisement we will loop until we find it!
        System.out.println("searching for the JXTA-EX1 Service advertisement");
        Enumeration en;
        while (true) {
            try {
                // let's look first in our local cache to see if we have it! We try to discover an adverisement
                // which as the (Name, JXTA-EX1) tag value
                en = discovery.getLocalAdvertisements(DiscoveryService.ADV, "Name", "JXTASPEC:JXTA-EX1");

                // Ok we got something in our local cache does not need to go further!
                if ((en != null) && en.hasMoreElements()) {
                    break;
                }

                // We could not find anything in our local cache, so let's send a
                // remote discovery request searching for the service advertisement.
                discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", "JXTASPEC:JXTA-EX1", 1, null);

                // The discovery is asynchronous as we do not know how long is going to take
                try {
                    // sleep as much as we want. Yes we should implement asynchronous listener pipe...
                    Thread.sleep(2000);
                } catch (Exception e) {
                    // ignored
                }
            } catch (IOException e) {
                // found nothing!  move on
            }
            System.out.print(".");
        }

        System.out.println("we found the service advertisement");

        // Ok get the service advertisement as a Spec Advertisement
        ModuleSpecAdvertisement mdsadv = (ModuleSpecAdvertisement) en.nextElement();
        //System.out.println(mdsadv);

        PSECredential credential = (PSECredential)membershipService.getDefaultCredential();

        boolean verifyWithKeyStore = true;
        boolean isVerified = mdsadv.verify(credential, verifyWithKeyStore);
        System.out.println("service advertisement verified = " + isVerified);
        System.out.println("service advertisement isAuthenticated = " + mdsadv.isAuthenticated());
        System.out.println("service advertisement isCorrectMembershipKey = " + mdsadv.isCorrectMembershipKey());
        System.out.println("service advertisement isMember = " + mdsadv.isMember());

//        try {
//
//            // let's print the advertisement as a plain text document
//            StructuredTextDocument doc = (StructuredTextDocument) mdsadv.getDocument(MimeMediaType.TEXT_DEFAULTENCODING);
//
//            StringWriter out = new StringWriter();
//
//            doc.sendToWriter(out);
//            //System.out.println(out.toString());
//            out.close();
//
//            // we can find the pipe to connect to the service
//            // in the advertisement.
//            PipeAdvertisement pipeadv = mdsadv.getPipeAdvertisement();
//
//            // Ok we have our pipe advertiseemnt to talk to the service
//            // create the output pipe endpoint to connect
//            // to the server, try 3 times to bind the pipe endpoint to
//            // the listening endpoint pipe of the service
//            OutputPipe outputPipe = pipeService.createOutputPipe(pipeadv, 10000);
//
//            // create the data string to send to the server
//            String data = "Hello my friend!";
//
//            // create the pipe message
//            Message msg = new Message();
//            StringMessageElement sme = new StringMessageElement("DataTag", data, null);
//
//            msg.addMessageElement(null, sme);
//
//            // send the message to the service pipe
//            outputPipe.send(msg);
//            System.out.println("message \"" + data + "\" sent to the ServiceServer");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.out.println("ServiceClient: Error sending message to the service");
//        }
    }
}
