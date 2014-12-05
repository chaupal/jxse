/*
 * Copyright (c) 2002-2007 Sun Microsystems, Inc.  All rights reserved.
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
package net.jxta.impl.peergroup;

import net.jxta.access.AccessService;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.resolver.ResolverService;
import net.jxta.service.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.content.ContentService;

/**
 * Provides a pure interface object that permits interaction with the actual
 * PeerGroup implementation without giving access to the real object.
 * <p/>
 * This class defines immutable objects. It has no control over the wrapped peer
 * group object's life cycle. It provides a weak PeerGroup interface object.
 */
class PeerGroupInterface implements PeerGroup {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(PeerGroupInterface.class.getName());
    
    /**
     * Ever increasing count of peer group interfaces to assist tracking.
     */
    private final static AtomicInteger interfaceInstanceCount = new AtomicInteger(0);

    /**
     * Tracks the requestor of this interface object.
     */
    protected final Throwable requestor;
    
    /**
     * The instance count of this interface object for tracking purposes.
     */
    protected final int instance;
    
    /**
     * If {@code true} then {@link #unref()} has been called.
     */
    protected final AtomicBoolean unrefed = new AtomicBoolean(false);
    
    /**
     * The peer group instance which backs this interface object.
     */
    protected PeerGroup groupImpl;

    /**
     * Constructs an interface object that front-ends the provided Peer Group.
     * 
     * @param theRealThing the real PeerGroup
     */
    PeerGroupInterface(PeerGroup theRealThing) {
        groupImpl = theRealThing;
        requestor = new Throwable("Requestor Stack Trace : " + theRealThing.getPeerGroupID());
        instance = interfaceInstanceCount.incrementAndGet();
        
        
        if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.INFO, "Peer Group Interface Constructed {" + instance + "}", requestor);
        } else if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Peer Group Interface Constructed {" + instance + "}");
        } 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object target) {
        PeerGroup temp = groupImpl;

        if (null != temp) {
            return temp.equals(target);
        } else {
            return super.equals(target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        PeerGroup temp = groupImpl;

        if (null != temp) {
            return temp.hashCode();
        } else {
            return super.hashCode();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * An implementation suitable for debugging. <b>Do not parse this
     * string!</b> All of the information is available from other sources.
     */
    @Override
    public String toString() {
        PeerGroup temp = groupImpl;

        if (null != temp) {
            return temp.toString();
        } else {
            return super.toString() + "{" + instance + "}";
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is here for class hierarchy reasons. It is normally ignored. By
     * definition, the interface object protects the real object's start/stop
     * methods from being called.
     */
    public void init(PeerGroup pg, ID assignedID, Advertisement impl) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is here for class hierarchy reasons. It is normally ignored. By
     * definition, the interface object protects the real object's start/stop
     * methods from being called.
     */
    public int startApp(String[] args) {
        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Applications assume that they have exclusive access to the peer group
     * object. They call {@code stopApp()} to signify that they are finished 
     * with the peer group. Since peer groups are shared, we use 
     * {@code stopApp()} to {@link #unref()}.
     * <p/>
     * We could also just do nothing and let this interface be GCed but calling
     * {@link #unref()} makes the group go away immediately if it is not shared, 
     * which is what applications calling stopApp() expect.
     */
    public void stopApp() {
        unref();
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup getInterface() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return this;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is already a weak reference (non-counted) to the peer group so we
     * can just return ourself.
     */
    public PeerGroup getWeakInterface() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unref() {
        boolean unref = unrefed.compareAndSet(false, true);

        if (unref) {
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                Throwable unrefer = new Throwable("Unrefer Stack Trace", requestor);

                LOG.log(Level.FINER, "Peer Group Interface Unreference {" + instance + "}", unrefer);
            } else if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Peer Group Interface Unreference {" + instance + "}");
            }

            groupImpl = null;
        } else {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                Throwable unrefer = new Throwable("Unrefer Stack Trace", requestor);

                LOG.log(Level.WARNING, "Duplicate dereference of Peer Group Interface {" + instance + "}", unrefer);
            }
        }

        return unref;
    }

    /**
     * {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getImplAdvertisement();
    }

    /**
     * {@inheritDoc}
     */
    public ThreadGroup getHomeThreadGroup() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getHomeThreadGroup();
    }

    /**
     * {@inheritDoc}
     */
    public URI getStoreHome() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getStoreHome();
    }

    /**
     * {@inheritDoc}
     */
    public JxtaLoader getLoader() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getLoader();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRendezvous() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.isRendezvous();
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerGroupAdvertisement();
    }

    /**
     * {@inheritDoc}
     */
    public PeerAdvertisement getPeerAdvertisement() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerAdvertisement();
    }

    /**
     * {@inheritDoc}
     */
    public Service lookupService(ID name) throws ServiceNotFoundException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.lookupService(name);
    }

    /**
     * {@inheritDoc}
     */
    public Service lookupService(ID name, int roleIndex) throws ServiceNotFoundException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.lookupService(name, roleIndex);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<ID> getRoleMap(ID name) {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getRoleMap(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean compatible(Element compat) {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.compatible(compat);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * FIXME 20031103 jice Ideally, we'd need the groupAPI to offer a means to
     * loadModule() without making a counted reference, so that group services
     * can loadModule() things without preventing group termination. This could
     * be achieved elegantly by making this the only behaviour available through
     * a weak GroupInterface. So it would be enough to obtain a weak interface
     * from one's group and then use its loadmodule method rather than that of
     * the strong group reference.  However, that's a bit too big a change to be
     * decided without more careful consideration.
     */
    public Module loadModule(ID assignedID, Advertisement impl) throws ProtocolNotSupportedException, PeerGroupException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.loadModule(assignedID, impl);
    }

    /**
     * {@inheritDoc}
     */
    public Module loadModule(ID assignedID, ModuleSpecID specID, int where) {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.loadModule(assignedID, specID, where);
    }

    /**
     * {@inheritDoc}
     */
    public void publishGroup(String name, String description) throws IOException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        temp.publishGroup(name, description);
    }

    /*
     * Valuable application helpers: Various methods to instantiate groups.
     */
    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(Advertisement pgAdv) throws PeerGroupException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.newGroup(pgAdv);
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description) throws PeerGroupException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.newGroup(gid, impl, name, description);
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid) throws PeerGroupException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.newGroup(gid);
    }

    /*
     * shortcuts to the well-known services, in order to avoid calls to lookup.
     */
    /**
     * {@inheritDoc}
     */
    public RendezVousService getRendezVousService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getRendezVousService();
    }

    /**
     * {@inheritDoc}
     */
    public EndpointService getEndpointService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getEndpointService();
    }

    /**
     * {@inheritDoc}
     */
    public ResolverService getResolverService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getResolverService();
    }

    /**
     * {@inheritDoc}
     */
    public DiscoveryService getDiscoveryService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getDiscoveryService();
    }

    /**
     * {@inheritDoc}
     */
    public PeerInfoService getPeerInfoService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerInfoService();
    }

    /**
     * {@inheritDoc}
     */
    public MembershipService getMembershipService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getMembershipService();
    }

    /**
     * {@inheritDoc}
     */
    public PipeService getPipeService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPipeService();
    }

    /**
     * {@inheritDoc}
     */
    public AccessService getAccessService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getAccessService();
    }

    /**
     * {@inheritDoc}
     */
    public ContentService getContentService() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getContentService();
    }

    /*
     * A few convenience methods. This information is available from
     * the peer and peergroup advertisement.
     */
    /**
     * {@inheritDoc}
     */
    public PeerGroupID getPeerGroupID() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerGroupID();
    }

    /**
     * {@inheritDoc}
     */
    public PeerID getPeerID() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerID();
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerGroupName() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerGroupName();
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerName() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getPeerName();
    }

    /**
     * {@inheritDoc}
     */
    public ConfigParams getConfigAdvertisement() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        ConfigParams configAdvertisement = temp.getConfigAdvertisement();

        if (configAdvertisement == null) {
            return null;
        }
        return configAdvertisement.clone();
    }

    /**
     * {@inheritDoc}
     */
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() throws Exception {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getAllPurposePeerGroupImplAdvertisement();
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup getParentGroup() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.getParentGroup();
    }
}
