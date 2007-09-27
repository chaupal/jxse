/*
 * BlabberMouthService.java
 *
 * Created on 26-Sep-2007, 6:19:40 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *  This service sends JXTA an annoucement message every few seconds via the
 *  JXTA endpoint. It also listens for announcement messages from other peers
 *  and prints a message on the console whenever it receives one.
 *
 * @author mike
 */
public class GossipService implements net.jxta.service.Service, net.jxta.endpoint.EndpointListener {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(GossipService.class.getName());
    /**
     *  The module class ID for Gossip services. All Gossip services share this
     *  same module class id.
     */
    public final static ModuleClassID GOSSIP_SERVICE_MCID = ModuleClassID.create(URI.create("urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30F05"));
    /**
     *  The module spec ID for our Gossip services. The module spec id contains
     *  within it {@code GOSSIP_SERVICE_MCID}. All implementations which use the
     *  same protocol as this implementation will share this same module spec id.
     */
    public final static ModuleSpecID GOSSIP_SERVICE_MSID = ModuleSpecID.create(URI.create("urn:jxta:uuid-4CD1574ABA614A5FA242B613D8BAA30FD0A45F5F0E1A450DA912BB01585AB0FC06"));
    
    /**
     *  The default gossip text we will send to other peers.
     */
    public final static String DEFAULT_GOSSIP_TEXT = "JXTA is cool. Pass it on!";
    
    /**
     *  Whether we should show our own gossip text default.
     */
    public final static boolean DEFAULT_SHOW_OWN = false;
    
    /**
     *  The default interval in milliseconds at which we will send our gossip
     *  text.
     */
    public final static long GOSSIP_INTERVAL_DEFAULT = 10 * 1000L;

    /**
     *  The name of the message namespace for all gossip service messages.
     */
    public final static String GOSSIP_NAMESPACE = "gossip";
    
    /**
     *  The name of the message element identifying the gossip sender.
     */
    public final static String GOSSIP_SENDER_ELEMENT_NAME = "sender";
        
    /**
     *  The name of the message element containing the gossip text.
     */
    public final static String GOSSIP_GOSSIP_ELEMENT_NAME = "gossip";
    
    /**
     *  A Timer shared between all Gossip service instances that we use for
     *  sending our gossip messages.
     */
    public final static Timer SHARED_TIMER = new Timer("Gossip Services Timer", true);
    
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
     * returns itself.
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
        if(null != gossipConfig.getGossip()) {
            gossip = gossipConfig.getGossip();
        }

        // If the config has a non-null showOwn then use that.
        if(null != gossipConfig.getShowOwn()) {
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
        endpoint = group.getEndpointService();

        if (null == endpoint) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Stalled until there is an endpoint service");
            }

            return START_AGAIN_STALLED;
        }
        
        boolean registered = endpoint.addIncomingMessageListener(this, getAssignedID().toString(), null);
        
        if(!registered) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Failed to regiser endpoint listener.");
            }
            return -1;
        }
        
        sendTask = new TimerTask() {

            /**
             * {@inheritDoc}
             */
            public void run() {
                sendGossip();
            }            
        };
        
        SHARED_TIMER.schedule(sendTask, gossip_interval, gossip_interval);

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info( "[" + group + "] Gossip Serivce (" + getAssignedID() + ") started");
        }

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stopApp() {
        if(null != endpoint) {
            endpoint.removeIncomingMessageListener(getAssignedID().toString(), null);
        }

        endpoint = null;
        
        TimerTask currentTask = sendTask;
        if(null != currentTask) {
            currentTask.cancel();
        }
        sendTask = null;
        
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info( "[" + group + "] Gossip Serivce (" + getAssignedID() + ") stopped.");
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        MessageElement sender = message.getMessageElement(GOSSIP_NAMESPACE, GOSSIP_SENDER_ELEMENT_NAME);
        MessageElement text = message.getMessageElement(GOSSIP_NAMESPACE, GOSSIP_GOSSIP_ELEMENT_NAME);
        
        if((null == sender) || (null == text)) {
            System.err.println("Someone sent us an incomplete message.");
            return;
        }
        
        if(!showOwn && group.getPeerID().toString().equals(sender.toString())) {
            // It's from ourself and we are configured to ignore it.
            return;
        }
        
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

            Message gossipMessage = new Message();

            MessageElement sender = new StringMessageElement(GOSSIP_SENDER_ELEMENT_NAME, group.getPeerID().toString(), null);
            gossipMessage.addMessageElement(GOSSIP_NAMESPACE, sender);

            MessageElement text = new StringMessageElement(GOSSIP_GOSSIP_ELEMENT_NAME, gossip, null);
            gossipMessage.addMessageElement(GOSSIP_NAMESPACE, text);

            currentEndpoint.propagate(gossipMessage, getAssignedID().toString(), null);
        } catch (IOException ex) {
            Logger.getLogger(GossipService.class.getName()).log(Level.SEVERE, "Failed sending gossip message.", ex);
        }
    }
}