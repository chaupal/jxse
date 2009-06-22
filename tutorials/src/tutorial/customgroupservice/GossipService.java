/*
 * Copyright (c) 2004-2007 Sun Microsystems, Inc.  All rights reserved.
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

package tutorial.customgroupservice;

import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.platform.ModuleSpecID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.service.Service;

/**
 *  A very simple Peer Group Service.
 *  <p/>
 *  This service sends JXTA an annoucement message every few seconds via
 *  JXTA endpoint service <tt>propagate()</tt>. It also listens for announcement
 *  messages from other peers and prints a message on the console whenever it
 *  receives one.
 *  <p/>
 *  The protocol for this service consists of JXTA messages sent via Endpoint
 *  propagation. In order for this example to work you <i>must</i> enable at
 *  least one message transport which supports broadcast/multicast.
 *  <p/>
 *  This gossip service implementation uses the <tt>assignedID</tt>
 *  which is initialized in <tt>init()</tt> as the endpoint address for the 
 *  messages it sends and receives. Use of the <tt>assignedID</tt> as the 
 *  <tt>serviceParam</tt> is a common choice because it is gauranteed to be
 *  unique within the PeerGroup and the <tt>assignedID</tt> 
 *  <tt>serviceParam</tt> is informally reserved for the service with that
 *  <tt>assignedID</tt>.
 *  <p/>
 *  The messages exchanged by the gossip service contain two message
 *  elements in the "<tt>gossip</tt>" namespace. "<tt>sender</tt>" contains a
 *  <tt>String</tt> of the peer id of the message sender. "<tt>gossip</tt>"
 *  contains a <tt>String</tt> of the gossip text which is being shared by the
 *  sender.
 */
public class GossipService implements net.jxta.service.Service, net.jxta.endpoint.EndpointListener {

    /**
     * Logger
     */
    private static final transient Logger LOG = Logger.getLogger(GossipService.class.getName());
    /**
     *  The module class ID for Gossip services. All Gossip services regardless
     *  of the protocol used share this same module class id.
     */
    public static final ModuleClassID GOSSIP_SERVICE_MCID = ModuleClassID.create(URI.create("urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30F05"));
    /**
     *  The module spec ID for our Gossip service. The module spec id contains
     *  the {@code GOSSIP_SERVICE_MCID}. All implementations which use the
     *  same messaging protocol as this implementation will share this same
     *  module spec id.
     */
    public static final ModuleSpecID GOSSIP_SERVICE_MSID = ModuleSpecID.create(URI.create("urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30FD0A45F5F0E1A450DA912BB01585AB0FC06"));
    /**
     *  The default gossip text we will send to other peers.
     */
    public static final String DEFAULT_GOSSIP_TEXT = "JXTA is cool. Pass it on!";
    /**
     *  Whether we should show our own gossip text default.
     */
    public static final boolean DEFAULT_SHOW_OWN = false;
    /**
     *  The default interval in milliseconds at which we will send our gossip
     *  text.
     */
    public static final long GOSSIP_INTERVAL_DEFAULT = 10 * 1000L;
    /**
     *  The name of the message namespace for all gossip service messages.
     */
    public static final String GOSSIP_NAMESPACE = "gossip";
    /**
     *  The name of the message element identifying the gossip sender.
     */
    public static final String GOSSIP_SENDER_ELEMENT_NAME = "sender";
    /**
     *  The name of the message element containing the gossip text.
     */
    public static final String GOSSIP_GOSSIP_ELEMENT_NAME = "gossip";
    /**
     *  A Timer shared between all Gossip service instances that we use for
     *  sending our gossip messages.
     */
    public static final Timer SHARED_TIMER = new Timer("Gossip Services Timer", true);
    /**
     *  The peer group in which this instance is running.
     */
    private PeerGroup group;
    /**
     * Our assigned service ID. Usually this is our MCID but may also be our
     * MCID with a role id if there are multiple gossip services within the
     * peer group.
     */
    private ID assignedID;
    /**
     *  The module implementation advertisement for our instance.
     */
    private ModuleImplAdvertisement implAdv;
    /**
     *  The "gossip" message we read from our configuration.
     */
    private String gossip = DEFAULT_GOSSIP_TEXT;
    /**
     *  If {@code true} then we show our own gossip messages;
     */
    private boolean showOwn = DEFAULT_SHOW_OWN;
    /**
     *  The interval in milliseconds at which we will send our gossip message.
     */
    private long gossip_interval = GOSSIP_INTERVAL_DEFAULT;
    /**
     * The endpoint service with which we send our gossips and register our
     * listener.
     */
    private EndpointService endpoint = null;
    /**
     *  The timer task we use to send our gossip messages.
     */
    private TimerTask sendTask = null;

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation doesn't currently use interface objects so it just
     * returns itself. We would use an interface object if we needed to maintain
     * state for each caller of the Gossip Service or wished to attach a
     * security context to the callers of this service. ie. different callers
     * might have different sercurity privleges.
     */
    public Service getInterface() {
        return this;
    }

    /**
     * Return our assigned ID.
     *
     * @return Our assigned ID.
     */
    public ID getAssignedID() {
        return assignedID;
    }

    /**
     * {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        return implAdv;
    }

    /**
     * {@inheritDoc}
     */
    public void init(PeerGroup group, ID assignedID, Advertisement implAdvertisement) throws PeerGroupException {
        this.group = group;
        this.assignedID = assignedID;
        this.implAdv = (ModuleImplAdvertisement) implAdvertisement;

        // Get our configuration parameters.
        GossipServiceConfigAdv gossipConfig = null;
        ConfigParams confAdv = group.getConfigAdvertisement();

        if (confAdv != null) {
            Advertisement adv = null;

            try {
                XMLDocument configDoc = (XMLDocument) confAdv.getServiceParam(getAssignedID());

                if (null != configDoc) {
                    adv = AdvertisementFactory.newAdvertisement(configDoc);
                }
            } catch (NoSuchElementException failed) {
                //ignored
            }

            if (adv instanceof GossipServiceConfigAdv) {
                gossipConfig = (GossipServiceConfigAdv) adv;
            }
        }

        if (null == gossipConfig) {
            // Make a new advertisement for defaults.
            gossipConfig = (GossipServiceConfigAdv) AdvertisementFactory.newAdvertisement(GossipServiceConfigAdv.getAdvertisementType());
        }

        // If the config has a non-null gossip then use that.
        if (null != gossipConfig.getGossip()) {
            gossip = gossipConfig.getGossip();
        }

        // If the config has a non-null showOwn then use that.
        if (null != gossipConfig.getShowOwn()) {
            showOwn = gossipConfig.getShowOwn();
        }

        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            StringBuilder configInfo = new StringBuilder("Configuring Gossip Service : " + assignedID);

            configInfo.append("\n\tImplementation :");
            configInfo.append("\n\t\tModule Spec ID: ").append(implAdv.getModuleSpecID());
            configInfo.append("\n\t\tImpl Description : ").append(implAdv.getDescription());
            configInfo.append("\n\t\tImpl URI : ").append(implAdv.getUri());
            configInfo.append("\n\t\tImpl Code : ").append(implAdv.getCode());

            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : ").append(group);
            configInfo.append("\n\t\tPeer ID : ").append(group.getPeerID());

            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tShow own gossips : ").append(showOwn);
            configInfo.append("\n\t\tGossip : ").append(gossip);

            LOG.config(configInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int startApp(String[] args) {
        /*  We require the peer group Endpoint service. Since the order in which
         *  services are initialized is random the Endpoint might not yet be
         *  initialized when we are first called. If the Endpoint service is not
         *  available then we tell our caller that we can not yet start. The
         *  peer group implementation will continue to start other services and
         *  call our <tt>startApp()</tt> method again.
         */
        endpoint = group.getEndpointService();

        if (null == endpoint) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Stalled until there is an endpoint service");
            }

            return Module.START_AGAIN_STALLED;
        }

        /*  Register our listener for gossip messages. The registered address is
         *  our assigned ID as a String as the <tt>serviceName</tt> and nothing
         *  as the <tt>serviceParam</tt>.
         */
        boolean registered = endpoint.addIncomingMessageListener(this, getAssignedID().toString(), null);

        if (!registered) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Failed to regiser endpoint listener.");
            }
            return -1;
        }

        // Create our timer task which will send our gossip messages.
        sendTask = new TimerTask() {

            /**
             * {@inheritDoc}
             */
            public void run() {
                sendGossip();
            }
        };

        // Register the timer task.
        SHARED_TIMER.schedule(sendTask, gossip_interval, gossip_interval);

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("[" + group + "] Gossip Serivce (" + getAssignedID() + ") started");
        }

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stopApp() {
        /*  We have to assume that <tt>stopApp()</tt> might be called before
         * <tt>startApp()</tt> successfully completes. This means that fields
         * initialized in the <tt>startApp()</tt> method might not be
         * initialized.
         */
        if (null != endpoint) {
            endpoint.removeIncomingMessageListener(getAssignedID().toString(), null);
        }
        endpoint = null;

        // Cancel our sending timer task.
        TimerTask currentTask = sendTask;
        if (null != currentTask) {
            currentTask.cancel();
        }
        sendTask = null;

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("[" + group + "] Gossip Serivce (" + getAssignedID() + ") stopped.");
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        MessageElement sender = message.getMessageElement(GOSSIP_NAMESPACE, GOSSIP_SENDER_ELEMENT_NAME);
        MessageElement text = message.getMessageElement(GOSSIP_NAMESPACE, GOSSIP_GOSSIP_ELEMENT_NAME);

        // Make sure that the message contains the required elements.
        if ((null == sender) || (null == text)) {
            System.err.println("Someone sent us an incomplete message.");
            return;
        }

        // Check if the message is from ourself and should be ignored.
        if (!showOwn && group.getPeerID().toString().equals(sender.toString())) {
            // It's from ourself and we are configured to ignore it.
            return;
        }

        // Print the message's gossip text along with who it's from.
        System.out.println(sender.toString() + " says : " + text.toString());
    }

    /**
     *  Send a gossip message using the endpoint propagate method.
     */
    public void sendGossip() {
        try {
            EndpointService currentEndpoint = endpoint;

            if (null == currentEndpoint) {
                return;
            }
            
            // Create a new message.
            Message gossipMessage = new Message();

            // Add a "sender" element containing our peer id.
            MessageElement sender = new StringMessageElement(GOSSIP_SENDER_ELEMENT_NAME, group.getPeerID().toString(), null);
            gossipMessage.addMessageElement(GOSSIP_NAMESPACE, sender);

            // Add a "gossip" element containing our gossip text.
            MessageElement text = new StringMessageElement(GOSSIP_GOSSIP_ELEMENT_NAME, gossip, null);
            gossipMessage.addMessageElement(GOSSIP_NAMESPACE, text);

            // Send the message to the network using endpoint propagation.
            currentEndpoint.propagate(gossipMessage, getAssignedID().toString(), null);
        } catch (IOException ex) {
            Logger.getLogger(GossipService.class.getName()).log(Level.SEVERE, "Failed sending gossip message.", ex);
        }
    }
}