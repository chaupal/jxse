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
package tutorial.discovery;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;

import java.io.File;
import java.util.Enumeration;

/**
 * Illustrates the use of the Discovery Service
 */
public class DiscoveryClient implements DiscoveryListener {

    private transient NetworkManager manager;
    private transient DiscoveryService discovery;

    /**
     * Constructor for the DiscoveryClient
     */
    public DiscoveryClient() {
        try {
            manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC, "DiscoveryClient",
                    new File(new File(".cache"), "DiscoveryClient").toURI());
            manager.startNetwork();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Get the NetPeerGroup
        PeerGroup netPeerGroup = manager.getNetPeerGroup();

        // get the discovery service
        discovery = netPeerGroup.getDiscoveryService();
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        DiscoveryClient disocveryClient = new DiscoveryClient();

        disocveryClient.start();
    }

    /**
     * loop forever attempting to discover advertisements every minute
     */
    public void start() {
        long waittime = 60 * 1000L;

        try {
            // Add ourselves as a DiscoveryListener for DiscoveryResponse events
            discovery.addDiscoveryListener(this);
            discovery.getRemoteAdvertisements(// no specific peer (propagate)
                    null, // Adv type
                    DiscoveryService.ADV, // Attribute = any
                    null, // Value = any
                    null, // one advertisement response is all we are looking for
                    1, // no query specific listener. we are using a global listener
                    null);
            while (true) {
                // wait a bit before sending a discovery message
                try {
                    System.out.println("Sleeping for :" + waittime);
                    Thread.sleep(waittime);
                } catch (Exception e) {
                    // ignored
                }
                System.out.println("Sending a Discovery Message");
                // look for any peer
                discovery.getRemoteAdvertisements(
                        // no specific peer (propagate)
                        null,
                        // Adv type
                        DiscoveryService.ADV,
                        // Attribute = name
                        "Name",
                        // Value = the tutorial
                        "Discovery tutorial",
                        // one advertisement response is all we are looking for
                        1,
                        // no query specific listener. we are using a global listener
                        null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called whenever a discovery response is received, which are
     * either in response to a query we sent, or a remote publish by another node
     *
     * @param ev the discovery event
     */
    public void discoveryEvent(DiscoveryEvent ev) {

        DiscoveryResponseMsg res = ev.getResponse();

        // let's get the responding peer's advertisement
        System.out.println(" [  Got a Discovery Response [" + res.getResponseCount() + " elements]  from peer : " + ev.getSource() + "  ]");

        Advertisement adv;
        Enumeration en = res.getAdvertisements();

        if (en != null) {
            while (en.hasMoreElements()) {
                adv = (Advertisement) en.nextElement();
                System.out.println(adv);
            }
        }
    }

    /**
     * Stops the platform
     */
    public void stop() {
        // Stop JXTA
        manager.stopNetwork();
    }
}

