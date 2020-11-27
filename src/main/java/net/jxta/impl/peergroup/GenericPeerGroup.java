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

package net.jxta.impl.peergroup;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.access.AccessService;
import net.jxta.content.ContentService;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.loader.DynamicJxtaLoader;
import net.jxta.impl.loader.RefJxtaLoader;
import net.jxta.impl.protocol.PSEConfigAdv;
import net.jxta.impl.protocol.PeerGroupConfigAdv;
import net.jxta.impl.protocol.PeerGroupConfigFlag;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.IJxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.JxtaSocket;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.resolver.ResolverService;
import net.jxta.service.Service;

/**
 * Provides common services for most peer group implementations.
 */
public abstract class GenericPeerGroup implements PeerGroup {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(GenericPeerGroup.class.getName());

    /**
     *  Holder for configuration parameters for groups in the process of being created.
     */
    private final static Map<ID, ConfigParams> group_configs = Collections.synchronizedMap(new HashMap<ID, ConfigParams>());

    /**
     * Default compatibility equater instance.
     */
    private static final CompatibilityEquater COMP_EQ =
    	new CompatibilityEquater() {
        public boolean compatible(Element test) {
            return CompatibilityUtils.isCompatible(test);
        }
    };

    /**
     * Statically scoped JxtaLoader which is used as the root of the
     * JXTA class loader hierarchy.  The world peer group defers to the
     * static loader as it's parent class loader.
     * <p/>
     * This class loader is a concession.  Use the group-scoped loader
     * instead.
     * <p/>
     * XXX 20080817 mcumings - I'd like this to go away entirely, now that
     * each group has it's own JxtaLoader instance.  If the root loader was
     * simply the JxtaLoader used by the WPG things would make more sense.
     * XXX 20140415 cppieters: removed RefJxtaLoader to a static instance in order to improve compatibility with OSGI
     */
    private final static IJxtaLoader staticLoader = DynamicJxtaLoader.getInstance();
            //new RefJxtaLoader(new URL[0], COMP_EQ);

    private static final ThreadGroup JXSE_THREAD_GROUP = new ThreadGroup("JXSE");

    /**
     * The PeerGroup-specific JxtaLoader instance.
     */
    private static IJxtaLoader loader;

    /*
     * Shortcuts to well known services.
     */
    private EndpointService endpoint;
    private ResolverService resolver;
    private DiscoveryService discovery;
    private PipeService pipe;
    private MembershipService membership;
    private RendezVousService rendezvous;
    private PeerInfoService peerinfo;
    private AccessService access;
    private ContentService content;

    /**
     * This peer's advertisement in this group.
     */
    private final PeerAdvertisement peerAdvertisement;

    /**
     * This group's advertisement.
     */
    private PeerGroupAdvertisement peerGroupAdvertisement = null;

    /**
     * This group's implAdvertisement.
     */
    protected JxtaSocket implAdvertisement = null;

    /**
     * This peer's config advertisement.
     */
    protected ConfigParams configAdvertisement = null;

    /**
     * This service implements a group but, being a Service, it runs inside of
     * some group. That's its home group.
     * <p/>
     * Exception: The platform peer group does not have a parent group. It
     * has to be entirely self sufficient.
     */
    protected PeerGroup parentGroup = null;

    /**
     * The location of our store
     */
    protected URI jxtaHome = null;

    /**
     * The services that do the real work of the Peer Group.
     */
    private final Map<ModuleClassID, Service> services = new HashMap<ModuleClassID, Service>();

    /**
     * {@code true} when we have decided to stop this group.
     */
    private volatile boolean stopping = false;

    /**
     * {@code true} when the PG adv has been published.
     */
    private boolean published = false; // assume it hasn't

    /**
     * Counts the number of times an interface to this group has been given out.
     * This is decremented every time an interface object is GCed or its owner
     * calls {@link unref()}.
     * <p/
     * >When it reaches zero, if it is time to tear-down the group instance;
     * nomatter what the GC thinks. There are threads that need to be stopped
     * before the group instance object ever becomes un-reachable.
     */
    private AtomicInteger masterRefCount = new AtomicInteger(0);

    /**
     * Is {@code true} when at least one interface object has been generated
     * AFTER initComplete has become true. If true, the group stops when its ref
     * count reaches zero.
     */
    private boolean stopWhenUnreferenced = false;

    /**
     * Is set to {@code true} when {@code init()} is completed enough that it
     * makes sense to perform ref-counting.
     */
    protected volatile boolean initComplete = false;

    /**
     * {@inheritDoc}
     * <p/>
     * We do not want to count on the invoker to properly unreference the group
     * object that we return; this call is often used in a loop and it is silly
     * to increment and decrement ref-counts for references that are sure to
     * live shorter than the referee.
     * <p/>
     * On the other hand it is dangerous for us to share our reference object to
     * the parent group. That's where weak interface objects come in handy. We
     * can safely make one and give it away.
     */
    public PeerGroup getParentGroup() {
        if (parentGroup == null) {
            return null;
        }
        return parentGroup;
//        return parentGroup.getWeakInterface();
    }

    /**
     * {@inheritDoc}
     */
    public URI getStoreHome() {
        return jxtaHome;
    }

    /**
     * Sets the root location for the store to be used by this peergroup.
     * <p/>
     * This should be set early in the peer group's life and then never changed.
     *
     * @param newHome The new store location.
     */
    protected void setStoreHome(URI newHome) {
        jxtaHome = newHome;
    }

    /**
     * Get a JxtaLoader instance which can be used to load modules
     * irrespective of the PeerGroup.
     *
     * @return JxtaLoader instance
     * @deprecated this statically scoped JxtaLoader instance should be phased
     *  out of use in favor of the group-scoped JxtaLoaders available via the
     *  {@code getLoader()} method.
     */
    @Deprecated
    public static IJxtaLoader getJxtaLoader() {
        return staticLoader;
    }

    public GenericPeerGroup() {
        // Start building our peer adv.
        peerAdvertisement = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object target) {
        if (!(target instanceof PeerGroup)) {
            return false;
        }

        PeerGroup targetAsPeerGroup = (PeerGroup) target;

        // both null or both non-null.
        if ((null == parentGroup) && (null != targetAsPeerGroup.getParentGroup())) {
            return false;
        }

        if ((null != parentGroup) && (null == targetAsPeerGroup.getParentGroup())) {
            return false;
        }

        if ((null != parentGroup) && !parentGroup.equals(targetAsPeerGroup.getParentGroup())) {
            return false;
        }

        // and same peer ids.
        return getPeerGroupID().equals(targetAsPeerGroup.getPeerGroupID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // before init we must fail.
        if ((null == peerAdvertisement) || (null == getPeerGroupID())) {
            throw new IllegalStateException("PeerGroup not sufficiently initialized");
        }

        // XXX 20050907 bondolo including parentGroup would improve the hash.
        return getPeerGroupID().hashCode();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * An implementation suitable for debugging. <b>Don't try to parse this
     * string!</b> All of the information is available from other sources.
     */
    @Override
    public String toString() {
        if (null == getPeerGroupID()) {
            return super.toString();
        }

        StringBuilder result = new StringBuilder();

        result.append(getPeerGroupID().toString());
        String peerGroupName = peerGroupAdvertisement.getName();

        if (null != peerGroupName) {
            result.append(" \"");
            result.append(peerGroupName);
            result.append('\"');
        }

        result.append('[');
        result.append(masterRefCount.get());
        result.append(",");
        result.append(loader.hashCode());
        result.append(']');

        if (parentGroup == null) {
            result.append(" / [");
            result.append(staticLoader.hashCode());
            result.append("]");
        } else {
            result.append(" / ");
            result.append(parentGroup.toString());
        }

        return result.toString();
    }

    /**
     * Discover advertisements.
     *
     * @param discovery The discovery service to use.
     * @param type      the Discovery advertisement type.
     * @param attr      The attribute to search for or {@code null}.
     * @param value     The attribute value to match or {@code null}.
     * @param seconds   The number of seconds to search.
     * @param thisClass The Advertisement class which the advertisement must
     *                  match.
     * @return a Collection of advertisements
     */
    private Collection<Advertisement> discoverSome(DiscoveryService discovery, int type, String attr, String value, int seconds, Class thisClass) {
        long discoverUntil = TimeUtils.toAbsoluteTimeMillis(seconds * TimeUtils.ASECOND);
        long lastRemoteAt = 0; // no previous remote discovery made.

        List<Advertisement> results = new ArrayList<Advertisement>();

        try {
            do {
                Enumeration<Advertisement> res = discovery.getLocalAdvertisements(type, attr, value);

                while (res.hasMoreElements()) {
                    Advertisement a = res.nextElement();

                    if (thisClass.isInstance(a)) {
                        results.add(a);
                    }
                }

                if (!results.isEmpty()) {
                    break;
                }

                if (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), lastRemoteAt) > (30 * TimeUtils.ASECOND)) {
                    discovery.getRemoteAdvertisements(null, type, attr, value, 20);
                    lastRemoteAt = TimeUtils.timeNow();
                }

                // snooze waiting for responses to come in.
                Thread.sleep(1000);

            } while (TimeUtils.timeNow() < discoverUntil);

        } catch (Exception whatever) {

            Logging.logCheckedWarning(LOG, "Failure during discovery\n", whatever);

        }

        return results;
    }

    /**
     * Discover an advertisement within the local peer group.
     *
     * @param type      the Discovery advertisement type.
     * @param attr      The attribute to search for or {@code null}.
     * @param value     The attribute value to match or {@code null}.
     * @param seconds   The number of seconds to search.
     * @param thisClass The Advertisement class which the advertisement must match.
     * @return a Collection of advertisements
     */
    private Advertisement discoverOne(int type, String attr, String value, int seconds, Class thisClass) {
        Iterator<Advertisement> res = discoverSome(discovery, type, attr, value, seconds, thisClass).iterator();

        if (!res.hasNext()) {
            return null;
        }
        return res.next();
    }

    /**
     * Shortcuts to the standard basic services.
     *
     * @param mcid    The Module Class ID of the service.
     * @param service The service instance to set as the shortcut or
     *                {@code null} to clear the shortcut.
     */
    private void setShortCut(ModuleClassID mcid, Service service) {
        if (IModuleDefinitions.endpointClassID.equals(mcid)) {
            endpoint = (EndpointService) service;
            return;
        }
        if (IModuleDefinitions.resolverClassID.equals(mcid)) {
            resolver = (ResolverService) service;
            return;
        }
        if (IModuleDefinitions.discoveryClassID.equals(mcid)) {
            discovery = (DiscoveryService) service;
            return;
        }
        if (IModuleDefinitions.pipeClassID.equals(mcid)) {
            pipe = (PipeService) service;
            return;
        }
        if (IModuleDefinitions.membershipClassID.equals(mcid)) {
            membership = (MembershipService) service;
            return;
        }
        if (IModuleDefinitions.peerinfoClassID.equals(mcid)) {
            peerinfo = (PeerInfoService) service;
            return;
        }
        if (IModuleDefinitions.rendezvousClassID.equals(mcid)) {
            rendezvous = (RendezVousService) service;
            return;
        }
        if (IModuleDefinitions.accessClassID.equals(mcid)) {
            access = (AccessService) service;
        }
        if (IModuleDefinitions.contentClassID.equals(mcid)) {
            content = (ContentService) service;
        }
    }

    /**
     * Add a service to the collection of known services.
     *
     * @param mcid    The Module Class ID of the service.
     * @param service The service instance to set as the shortcut or
     */
    protected synchronized void addService(ModuleClassID mcid, Service service) {
        if (stopping) {
            return;
        }

        if (services.containsKey(mcid)) {
            throw new IllegalStateException("Service" + mcid + " already registered.");
        }

        services.put(mcid, service);

        setShortCut(mcid, service);
    }

    /**
     * {@inheritDoc}
     */
    synchronized public Service lookupService(ID mcid) throws ServiceNotFoundException {

        Service p = services.get(mcid);

        if (p == null) {
            throw new ServiceNotFoundException("Not found: " + mcid.toString());
        }

//        return p.getInterface();
        return p;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Group implementations do not have to support mapping.
     * it would be nice to separate better Interfaces, so that
     * Interface Objects can do things that the real service does
     * not have to implement.
     */
    public Service lookupService(ID mcid, int roleIndex) throws ServiceNotFoundException {

        // If the role number is != 0, it can't be honored: we
        // do not have an explicit map.

        if (roleIndex != 0) {
            throw new ServiceNotFoundException("Not found: " + mcid + "[" + roleIndex + "]");
        }

        return lookupService(mcid);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getRoleMap(ID name) {
        // No translation; use the given name in a singleton.
        return Collections.singletonList(name).iterator();
    }

    /**
     * check that all required core services are registered
     *
     * @throws ServiceNotFoundException If a required service was not found.
     */
    protected void checkServices() throws ServiceNotFoundException {
        Service ignored;

        ignored = lookupService(IModuleDefinitions.endpointClassID);
        ignored = lookupService(IModuleDefinitions.resolverClassID);
        ignored = lookupService(IModuleDefinitions.membershipClassID);
        ignored = lookupService(IModuleDefinitions.accessClassID);

    /**
         * ignored = lookupService(discoveryClassID);
         * ignored = lookupService(pipeClassID);
         * ignored = lookupService(rendezvousClassID);
         * ignored = lookupService(peerinfoClassID);
         */
    }

    /**
     * Ask a group to unregister and unload a service
     *
     * @param mcid The service to be removed.
     * @throws ServiceNotFoundException if service is not found
     */
    protected synchronized void removeService(ModuleClassID mcid) throws ServiceNotFoundException {
        setShortCut(mcid, null);

        Service p = services.remove(mcid);

        if (p == null) {
            throw new ServiceNotFoundException("Not found: " + mcid.toString());
        }

        p.stopApp();

        // service.terminate(); FIXME. We probably need a terminate() method.
        //
        // FIXME 20011013 jice To make sure the service is no-longer referenced
        // we should always return interfaces, and have a way to cut the
        // reference to the real service in the interfaces. One way of doing
        // that would be to have to levels of indirection: we should keep one
        // and return references to it.
        // When we want to cut the service loose, we should clear the reference
        // from the interface that we own before letting it go. We need to study
        // the consequences of doing that before implementing it.
    }

    /**
     * {@inheritDoc}
     */
    public Module loadModule(ID assigned, Advertisement impl) throws ProtocolNotSupportedException, PeerGroupException {
        return loadModule(assigned, (JxtaSocket) impl, false);
    }

    /**
     * Load a Module from a ModuleImplAdv.
     * <p/>
     * Compatibility is checked and load is attempted. If compatible and
     * loaded successfully, the resulting Module is initialized and returned.
     * In most cases the other loadModule() method should be preferred, since
     * unlike this one, it will seek many compatible implementation
     * advertisements and try them all until one works. The home group of the new
     * module (its' parent group if the new Module is a group) will be this group.
     *
     * @param assigned   Id to be assigned to that module (usually its ClassID).
     * @param implAdv    An implementation advertisement for that module.
     * @param privileged If {@code true} then the module is provided the true
     *                   group obj instead of just an interface to the group object. This is
     *                   normally used only for the group's defined services and applications.
     * @return Module the module loaded and initialized.
     * @throws ProtocolNotSupportedException The module is incompatible.
     * @throws PeerGroupException            The module could not be loaded or initialized
     */
    protected Module loadModule(ID assigned, JxtaSocket implAdv, boolean privileged) throws ProtocolNotSupportedException, PeerGroupException {

        Element compat = implAdv.getCompat();

        if (null == compat)
            throw new IllegalArgumentException("No compatibility statement for : " + assigned);

        if (!compatible(compat)) {

            Logging.logCheckedWarning(LOG, "Incompatible Module : ", assigned);
            throw new ProtocolNotSupportedException("Incompatible Module : " + assigned);

        }

        Module newMod = null;

        if ((null != implAdv.getCode()) && (null != implAdv.getUri())) {
            try {
                // Good one. Try it.
                Class<? extends Module> clazz;
                try {
                    clazz = loader.loadClass(implAdv.getModuleSpecID());
                } catch (ClassNotFoundException notLoaded) {
                    clazz = loader.defineClass(implAdv);
                }

                if (null == clazz) {
                    throw new ClassNotFoundException("Cannot load class (" + implAdv.getCode() + ") : " + assigned);
                }

                newMod = clazz.newInstance();
//                newMod.init(privileged ? this : getInterface(), assigned, implAdv);
                newMod.init(this, assigned, implAdv);

                Logging.logCheckedInfo(LOG, "Loaded", (privileged ? " privileged" : ""),
                    " module : ", implAdv.getDescription(), " (", implAdv.getCode(), ")");

            } catch (Exception ex) {

                // What happened?
                Logging.logCheckedSevere(LOG,ex);

                try {
                    newMod.stopApp();
                } catch (Throwable ignored) {
                // If this does not work, nothing needs to be done.
                }
                throw new PeerGroupException("Could not load module for : " + assigned + " (" + implAdv.getDescription() + ")", ex);
            }
        } else {
            String error;

            if (null == implAdv.getCode()) {
                error = "ModuleImpAdvertisement missing Code element";
            } else if (null == implAdv.getUri()) {
                error = "ModuleImpAdvertisement missing URI element";
            } else {
                error = "ModuleImpAdvertisement missing both Code and URI elements";
            }
            throw new PeerGroupException("Can not load module : " + error + " for" + assigned);
        }

        // Publish or renew the lease of this adv since we're using it.
        try {
            if (discovery != null) {
                discovery.publish(implAdv, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
            }
        } catch (Exception ignored) {
            // ignored
        }

        // If we reached this point we're done.
        return newMod;
    }

    /**
     * {@inheritDoc}
     */
    public Module loadModule(ID assigned, ModuleSpecID specID, int where) {
        return loadModule(assigned, specID, where, false);
    }

    /**
     * Load a module from a ModuleSpecID
     * <p/>
     * Advertisement is sought, compatibility is checked on all candidates and
     * load is attempted. The first one that is compatible and loads
     * successfully is initialized and returned.
     *
     * @param assignedID Id to be assigned to that module (usually its ClassID).
     * @param specID     The specID of this module.
     * @param where      May be one of: {@code Here}, {@code FromParent}, or
     *                   {@code Both}, meaning that the implementation advertisement will be
     *                   searched in this group, its parent or both. As a general guideline, the
     *                   implementation advertisements of a group should be searched in its
     *                   prospective parent (that is Here), the implementation advertisements of a
     *                   group standard service should be searched in the same group than where
     *                   this group's advertisement was found (that is, FromParent), while
     *                   applications may be sought more freely (Both).
     * @param privileged If {@code true} then the module is provided the true
     *                   group obj instead of just an interface to the group object. This is
     *                   normally used only for the group's defined services and applications.
     * @return Module the new module, or {@code null} if no usable implementation was found.
     */
    protected Module loadModule(ID assignedID, ModuleSpecID specID, int where, boolean privileged) {

    	List<Advertisement> allModuleImplAdvs = new ArrayList<Advertisement>();

    	JxtaSocket loadedImplAdv = loader.findModuleImplAdvertisement(specID);

    	// We already have a module defined for this spec id.
    	// We test the spec id before deciding to use it
    	if (null != loadedImplAdv && specID.equals(loadedImplAdv.getModuleSpecID())) {
    		allModuleImplAdvs.add(loadedImplAdv);
    	}

    	// A module implementation may have been found, but was not valid and not registered.
    	// If so, we need to broaden the search
    	if (allModuleImplAdvs.isEmpty()){

    		boolean fromHere = (where == Here || where == Both);
    		boolean fromParent = (where == FromParent || where == Both);

    		if (fromHere && (null != discovery)) {
    			Collection<Advertisement> here = discoverSome(discovery, DiscoveryService.ADV,
    					"MSID", specID.toString(), 120, JxtaSocket.class);

    			allModuleImplAdvs.addAll(here);
    		}

    		if (fromParent && (null != getParentGroup()) && (null != parentGroup.getDiscoveryService())) {
    			Collection<Advertisement> parent = discoverSome(parentGroup.getDiscoveryService(), DiscoveryService.ADV,
    					"MSID", specID.toString(), 120, JxtaSocket.class);

    			allModuleImplAdvs.addAll(parent);
    		}
    	}

    	Throwable recentFailure = null;

    	for (Advertisement eachAdv : allModuleImplAdvs) {
            if (!(eachAdv instanceof JxtaSocket)) {
                    continue;
            }

            JxtaSocket foundImpl = (JxtaSocket) eachAdv;

            try {

                // First check that the MSID is really the one we're looking for.
                // It could have appeared somewhere else in the adv than where
                // we're looking, and discovery doesn't know the difference.
                if (!specID.equals(foundImpl.getModuleSpecID())) {
                        continue;
                }

                Module newMod = loadModule(assignedID, foundImpl, privileged);

                // If we reach that point, the module is good.
                return newMod;

            } catch (ProtocolNotSupportedException failed) {

                // Incompatible implementation.
                Logging.logCheckedFine(LOG, "Incompatbile impl adv");

            } catch (PeerGroupException failed) {

                // Initialization failure.
                Logging.logCheckedWarning(LOG, "Initialization failed\n", failed);

            } catch (Throwable e) {

                recentFailure = e;
                Logging.logCheckedWarning(LOG, "Not a usable impl adv: \n", e);

            }
    	}

    	// Throw an exception if there was a recent failure.
    	if (null != recentFailure) {

    		if (recentFailure instanceof Error) {
    			throw (Error) recentFailure;
    		} else if (recentFailure instanceof RuntimeException) {
    			throw (RuntimeException) recentFailure;
    		} else {
    			throw new UndeclaredThrowableException(recentFailure);
    		}
    	}

    	Logging.logCheckedWarning(LOG, "Could not find a loadable implementation for SpecID: ", specID);

    	return null;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigParams getConfigAdvertisement() {
        return configAdvertisement;
    }

    /**
     * Sets the configuration advertisement for this peer group.
     *
     * @param config The configuration advertisement which will be used for
     * this peer group or {@code null} if no configuration advertisement is to
     * be used.
     */
    protected void setConfigAdvertisement(ConfigParams config) {
        configAdvertisement = config;
    }

    /**
     *  Adds configuration parameters for the specified group. The configuration
     *  parameters remain cached until either the specified group is started or
     *  the parameters are replaced.
     *
     *  @param groupid The group for who's params are being provided.
     *  @param params The parameters to be provided to the peer group when it is
     *  created.
     */
    public static void setGroupConfigAdvertisement(ID groupid, ConfigParams params) {
        if (null != params) {
            group_configs.put(groupid, params);
        } else {
            group_configs.remove(groupid);
        }
    }

    /*
     * Now comes the implementation of the public API, including the
     * API mandated by the Service interface.
     */
    /**
     * {@inheritDoc}
     * <p/>
     * It is not recommended to overload this method. Instead, subclassers
     * should overload either or both of
     * {@link #initFirst(PeerGroup,ID,Advertisement)} and {@link #initLast()}.
     * If this method is to be overloaded, the overloading method must
     * invoke <code>super.init</code>.
     * <p/>
     * This method invokes <code>initFirst</code>
     * with identical parameters. <code>initLast</initLast> does not take
     * parameters since the relevant information can be obtained from the
     * group following completion of the <code>initFirst</code> phase.
     * The resulting values may be different from the parameters to
     * <code>initFirst</code> since <code>initFirst</code> may
     * be overLoaded and the overloading method may modify these parameters
     * when calling <code>super.initFirst</code>. (See
     * {@link net.jxta.impl.peergroup.Platform} for an example of such a case).
     * <p/>
     * Upon completion, the group object is marked as completely initialized
     * in all cases. Once a group object is completely initialized, it becomes
     * sensitive to reference counting.
     * <p/>
     * In the future this method may become final.
     */
    public void init(PeerGroup homeGroup, ID assignedID, Advertisement impl) throws PeerGroupException {
        try {
            initFirst(homeGroup, assignedID, impl);
            initLast();
        } finally {
            // This must be done in all cases.
            initComplete = true;
        }
    }

    /**
     * Performs all initialization steps that need to be performed
     * before any subclass initialization is performed.
     * <p/>
     * Classes that override this method should always call
     * <code>super.initFirst()</code> <strong>before</strong> doing
     * any of their own work.
     *
     * @param homeGroup  The group that serves as a parent to this group.
     * @param assignedID The unique ID assigned to this module. For
     *                   group this is the group ID or <code>null</code> if a group ID
     *                   has not yet been assigned. If null is passed, GenericPeerGroup
     *                   will generate a new group ID.
     * @param impl       The ModuleImplAdvertisement which defines this
     *                   group's implementation.
     * @throws PeerGroupException if a group initialization error occurs
     */
    protected void initFirst(PeerGroup homeGroup, ID assignedID, Advertisement impl) throws PeerGroupException {

        this.implAdvertisement = (JxtaSocket) impl;
        this.parentGroup = homeGroup;

        if (null != parentGroup) {
            jxtaHome = parentGroup.getStoreHome();
        }

        // Set the peer configuration before we start.
        if ((null != assignedID) && (null == getConfigAdvertisement())) {
            setConfigAdvertisement(group_configs.remove(assignedID));
        }

        try {
            // FIXME 20030919 bondolo@jxta.org This setup doesnt give us any
            // capability to use seed material or parent group.
            if (null == assignedID) {
                if ("cbid".equals(IDFactory.getDefaultIDFormat())) {
                    throw new IllegalStateException("Cannot generate group id for cbid group");
                } else {
                    assignedID = IDFactory.newPeerGroupID();
                }
            } else {
                if (parentGroup != null) {
                    DiscoveryService disco = parentGroup.getDiscoveryService();
                    if (null != disco) {
                        Enumeration found = disco.getLocalAdvertisements(DiscoveryService.GROUP, "GID", assignedID.toString());
                        if (found.hasMoreElements()) {
                            peerGroupAdvertisement = (PeerGroupAdvertisement) found.nextElement();
                        }
                    }
                }
            }

            if (!(assignedID instanceof PeerGroupID)) {
                throw new PeerGroupException("assignedID must be a peer group ID");
            }

            peerAdvertisement.setPeerGroupID((PeerGroupID) assignedID);

            // // make sure the parent group is the required group
            // if (null != peerAdvertisement.getPeerGroupID().getParentPeerGroupID()) {
            // if (null == parentGroup) {
            // throw new PeerGroupException("Group requires parent group : " + peerAdvertisement.getPeerGroupID().getParentPeerGroupID());
            // } else if (!parentGroup.getPeerGroupID().equals(peerAdvertisement.getPeerGroupID().getParentPeerGroupID())) {
            // throw new PeerGroupException("Group requires parent group : " + peerAdvertisement.getPeerGroupID().getParentPeerGroupID() + ". Provided parent was : " + parentGroup.getPeerGroupID());
            // }
            // }

            // Do our part of the PeerAdv construction.
            if ((configAdvertisement != null) && (configAdvertisement instanceof PlatformConfig)) {
                PlatformConfig platformConfig = (PlatformConfig) configAdvertisement;

                // Normally there will be a peer ID and a peer name in the config.
                PeerID configPID = platformConfig.getPeerID();

                if ((null == configPID) || (ID.nullID == configPID)) {
                    if ("cbid".equals(IDFactory.getDefaultIDFormat())) {
                        // Get our peer-defined parameters in the configAdv
                        XMLElement param = (XMLElement) platformConfig.getServiceParam(IModuleDefinitions.membershipClassID);

                        if (null == param) {
                            throw new IllegalArgumentException(PSEConfigAdv.getAdvertisementType() + " could not be located");
                        }

                        Advertisement paramsAdv = null;
                        try {
                            paramsAdv = AdvertisementFactory.newAdvertisement(param);
                        } catch (NoSuchElementException noadv) {// ignored
                        }
                        if (!(paramsAdv instanceof PSEConfigAdv)) {
                            throw new IllegalArgumentException("Provided Advertisement was not a " + PSEConfigAdv.getAdvertisementType());
                        }

                        PSEConfigAdv config = (PSEConfigAdv) paramsAdv;
                        Certificate clientRoot = config.getCertificate();
                        byte[] pub_der = clientRoot.getPublicKey().getEncoded();

                        platformConfig.setPeerID(IDFactory.newPeerID((PeerGroupID) assignedID, pub_der));
                    } else {
                        platformConfig.setPeerID(IDFactory.newPeerID((PeerGroupID) assignedID));
                    }
                }

                peerAdvertisement.setPeerID(platformConfig.getPeerID());
                peerAdvertisement.setName(platformConfig.getName());
                peerAdvertisement.setDesc(platformConfig.getDesc());
            } else {
                if (null == parentGroup) {
                    // If we did not get a valid peer id, we'll initialize it here.
                    peerAdvertisement.setPeerID(IDFactory.newPeerID((PeerGroupID) assignedID));
                } else {
                    // We're not the world peer group, which is the authoritative source of these values.
                    peerAdvertisement.setPeerID(parentGroup.getPeerAdvertisement().getPeerID());
                    peerAdvertisement.setName(parentGroup.getPeerAdvertisement().getName());
                    peerAdvertisement.setDesc(parentGroup.getPeerAdvertisement().getDesc());
                }
            }

            if (peerGroupAdvertisement == null) {
                // No existing gadv. OK then we're creating the group or we're
                // the platform, it seems. Start a grp adv with the essentials
                // that we know.
                peerGroupAdvertisement = (PeerGroupAdvertisement) AdvertisementFactory.newAdvertisement(PeerGroupAdvertisement.getAdvertisementType());

                peerGroupAdvertisement.setPeerGroupID((PeerGroupID) assignedID);
                peerGroupAdvertisement.setModuleSpecID(implAdvertisement.getModuleSpecID());
            } else {
                published = true;
            }

            // Now that we have our PeerGroupAdvertisement, we can pull out
            // the config to see if we have any PeerGroupConfigAdv params
            if (null == parentGroup) {

                Logging.logCheckedFine(LOG, "Setting up group loader -> static loader");
                loader = new RefJxtaLoader(new URL[0], staticLoader.getClassLoader(), COMP_EQ, this);

            } else {
                IJxtaLoader upLoader = ((GenericPeerGroup) parentGroup).getLoader();
                StructuredDocument cfgDoc =
                        peerGroupAdvertisement.getServiceParam(
                        		IModuleDefinitions.peerGroupClassID);
                PeerGroupConfigAdv pgca;
                if (cfgDoc != null) {
                    pgca = (PeerGroupConfigAdv)
                            AdvertisementFactory.newAdvertisement((XMLElement)
                            peerGroupAdvertisement.getServiceParam(IModuleDefinitions.peerGroupClassID));
                    if (pgca.isFlagSet(PeerGroupConfigFlag.SHUNT_PARENT_CLASSLOADER)) {
                        // We'll shunt to the same class loader that loaded this
                        // class, but not the JXTA form (to prevent Module
                        // definitions).
                        upLoader = (IJxtaLoader) getClass().getClassLoader();
                    }
                }

                Logging.logCheckedFine(LOG, "Setting up group loader -> ", upLoader);
                loader = new RefJxtaLoader(new URL[0], (ClassLoader) upLoader, COMP_EQ, this);

            }

            // If we still do not have a config adv, make one with the parent group, or
            // a minimal one with minimal info in it.
            if (configAdvertisement == null) {

                ConfigParams superConfig = null;

                if ( homeGroup !=null )
                    superConfig = homeGroup.getConfigAdvertisement();

                if ( superConfig == null ) {

                    // We can't rely on the parent group
                    PlatformConfig conf = (PlatformConfig) AdvertisementFactory.newAdvertisement(PlatformConfig.getAdvertisementType());

                    conf.setPeerID(peerAdvertisement.getPeerID());
                    conf.setName(peerAdvertisement.getName());
                    conf.setDesc(peerAdvertisement.getDesc());
                    configAdvertisement = conf;

                } else {

                    // Copying the parent group
                    configAdvertisement = superConfig.clone();

                }

            }

            // Merge service params with those specified by the group (if any). The only
            // policy, right now, is to give peer params the precedence over group params.
            Hashtable grpParams = peerGroupAdvertisement.getServiceParams();
            Enumeration keys = grpParams.keys();

            while (keys.hasMoreElements()) {
                ID key = (ID) keys.nextElement();
                Element e = (Element) grpParams.get(key);

                if (configAdvertisement.getServiceParam(key) == null) {
                    configAdvertisement.putServiceParam(key, e);
                }
            }

            /*
             * Now seems like the right time to attempt to register the group.
             * The only trouble is that it could cause the group to
             * be used before all the services are initialized, but on the
             * other hand, we do not want to let a redundant group go through
             * it's service initialization because that would cause irreparable
             * damage to the legitimate instance. There should be a synchro on
             * on the get<service>() and lookupService() routines.
             */
            if (!getGlobalRegistry().registerInstance((PeerGroupID) assignedID, this)) {
                throw new PeerGroupException("Group already instantiated");
            }

        } catch (Throwable any) {
            Logging.logCheckedSevere(LOG, "Group init failed\n", any);

            if (any instanceof Error) {
                throw (Error) any;
            } else if (any instanceof RuntimeException) {
                throw (RuntimeException) any;
            } else if (any instanceof PeerGroupException) {
                throw (PeerGroupException) any;
            }

            throw new PeerGroupException("Group init failed", any);
        }

    /*
         * The rest of construction and initialization are left to the
         * group subclass, between here and the begining for initLast.
         * That should include instanciating and setting the endpoint, and
         * finally supplying it with endpoint protocols.
         * That also includes instanciating the appropriate services
         * and registering them.
         * For an example, see the StdPeerGroup class.
         */
    }

    /**
     * Perform all initialization steps that need to be performed
     * after any subclass initialization is performed.
     * <p/>
     * Classes that override this method should always call super.initLast
     * <strong>after</strong> doing any of their own work.
     * @throws PeerGroupException if a group initialization error occurs
     */
    protected void initLast() throws PeerGroupException {

        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {

            StringBuilder configInfo = new StringBuilder("Configuring Group : " + getPeerGroupID());

            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: ").append(implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
                configInfo.append("\n\t\tModule Spec ID : ").append(implAdvertisement.getModuleSpecID());
            }

            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tPeer Group ID : ").append(getPeerGroupID());
            configInfo.append("\n\t\tGroup Name : ").append(getPeerGroupName());
            configInfo.append("\n\t\tPeer ID in Group : ").append(getPeerID());
            configInfo.append("\n\tConfiguration :");

            if (null == parentGroup) {
                configInfo.append("\n\t\tHome Group : (none)");
            } else {
                configInfo.append("\n\t\tHome Group : \"").append(parentGroup.getPeerGroupName()).append("\" / ").append(parentGroup.getPeerGroupID());
            }

            configInfo.append("\n\t\tServices :");

            for (Map.Entry<ModuleClassID, Service> anEntry : services.entrySet()) {
                ModuleClassID aMCID = anEntry.getKey();
                JxtaSocket anImplAdv = (JxtaSocket) anEntry.getValue().getImplAdvertisement();

                configInfo.append("\n\t\t\t").append(aMCID).append("\t").append(anImplAdv.getDescription());
            }

            LOG.config(configInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] arg) {
        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * PeerGroupInterface's stopApp() does nothing. Only a real reference to the
     * group object permits to stop it without going through ref counting.
     */
    public void stopApp() {
        stopping = true;

        Collection<ModuleClassID> allServices = new ArrayList<ModuleClassID>(services.keySet());

        // Stop and remove all remaining services.
        for (ModuleClassID aService : allServices) {
            try {
                removeService(aService);
            } catch (Exception failure) {
                LOG.log(Level.WARNING, "Failure shutting down service : " + aService, failure);
            }
        }

        if (!services.isEmpty()) {
            LOG.warning(services.size() + " services could not be shut down during peer group stop.");
        }

        // remove everything (just in case);
        services.clear();

        getGlobalRegistry().unRegisterInstance(peerGroupAdvertisement.getPeerGroupID(), this);

        // Explicitly unreference our parent group in order to allow it
        // to terminate if this group object was itself the last reference
        // to it.
        if (parentGroup != null) {
//            parentGroup.unref();
            parentGroup = null;
        }
        // executors from TaskManager are now shutdown by the NetworkManager
        // No longer initialized.
        initComplete = false;
    }

//    /**
//     * {@inheritDoc}
//     * <p/>
//     * May be called by a module which has a direct reference to the group
//     * object and wants to notify its abandoning it. Has no effect on the real
//     * group object.
//     */
//    public boolean unref() {
//        return true;
//    }

    /**
     * Called every time an interface object that refers to this group
     * goes away, either by being finalized or by its unref() method being
     * invoked explicitly.
     */
    protected void decRefCount() {

        int new_count = masterRefCount.decrementAndGet();

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            Throwable trace = new Throwable("Stack Trace");
            StackTraceElement elements[] = trace.getStackTrace();
            LOG.info("[" + getPeerGroupID() + "] GROUP REF COUNT DECCREMENTED TO: " + new_count + " by\n\t" + elements[2]);
        }

        if (new_count < 0) {
            // Shutdown happens at zero. We must not go lower.
            throw new IllegalStateException("Peer Group reference count has gone negative!");
        }

        if (new_count > 0) {
            // If there are other references then we don't quit.
            return;
        }

        if (!stopWhenUnreferenced) {
            return;
        }

        Logging.logCheckedInfo(LOG, "[", getPeerGroupID(), "] STOPPING UNREFERENCED GROUP");

        stopApp();
    }

//    /*
//     * Implement the Service API so that we can make groups services when we
//     * decide to.
//     */
//    /**
//     * {@inheritDoc}
//     */
//    @Deprecated
//    public PeerGroup getInterface() {
//
//        /*
//         * Interfaces try to solve a problem solved by OSGi. We should rely on OSGi in the
//         * future to solve reference issues.
//         */
//        return this;
//
////        if (stopping) {
////            throw new IllegalStateException("Group has been shutdown. getInterface() is not available");
////        }
////
////        if (initComplete) {
////            // If init is complete the group can become sensitive to its ref
////            // count reaching zero. Before there could be transient references
////            // before there is a chance to give a permanent reference to the
////            // invoker of newGroup.
////            stopWhenUnreferenced = true;
////        }
////
////        int new_count = masterRefCount.incrementAndGet();
////
////        PeerGroupInterface pgInterface = new RefCountPeerGroupInterface(this);
////
////        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
////            Throwable trace = new Throwable("Stack Trace");
////            StackTraceElement elements[] = trace.getStackTrace();
////
////            LOG.info("[" + pgInterface + "] GROUP REF COUNT INCREMENTED TO: " + new_count + " by\n\t" + elements[2]);
////        }
////
////        return pgInterface;
//    }

//    /**
//     * {@inheritDoc}
//     */
//    public PeerGroup getWeakInterface() {
//        return new PeerGroupInterface(this);
//    }

    /**
     * {@inheritDoc}
     */
    public JxtaSocket getImplAdvertisement() {
        return implAdvertisement.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void publishGroup(String name, String description) throws IOException {

        if (published) {
            return;
        }

        peerGroupAdvertisement.setName(name);
        peerGroupAdvertisement.setDescription(description);

        if (parentGroup == null) {
            return;
        }

        DiscoveryService parentDiscovery = parentGroup.getDiscoveryService();

        if (null == parentDiscovery) {
            return;
        }

        parentDiscovery.publish(peerGroupAdvertisement, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
        published = true;
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(Advertisement pgAdv) throws PeerGroupException {
        PeerGroupAdvertisement adv = (PeerGroupAdvertisement) pgAdv;
        PeerGroupID gid = adv.getPeerGroupID();

        if ((gid == null) || ID.nullID.equals(gid)) {
            throw new IllegalArgumentException("Advertisement did not contain a peer group ID");
        }

        PeerGroup theNewGroup = parentGroup.getGlobalRegistry().lookupInstance(gid);

        if (theNewGroup != null) {
            return theNewGroup;
        }

        // We do not know if the grp adv had been previously published or not...  Since it may contain information essential to
        // the configuration of services, we need to make sure it is published localy, rather than letting the group publish
        // itself after the fact.

        // FIXME 20040713 jice The downside is that we're publishing the adv even before making sure that this group
        // can really be instantiated. We're basically using the cm as a means to pass parameters to the module because it is a
        // group. We have the same parameter issue with the config adv. Eventually we need to find a clean way of passing
        // parameters specific to a certain types of module.

        try {

            discovery.publish(adv, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);

        } catch (Exception any) {

            Logging.logCheckedWarning(LOG, "Could not publish the group advertisement: \n", any);

        }

        theNewGroup = (PeerGroup) loadModule(adv.getPeerGroupID(), adv.getModuleSpecID(), Here, false);

        if (theNewGroup == null) {
            throw new PeerGroupException("Could not find group implementation with " + adv.getModuleSpecID());
        }

//        return theNewGroup.getInterface();
        return theNewGroup;
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description) throws PeerGroupException {

        return this.newGroup(gid, impl, name, description, true);

    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description, boolean publish) throws PeerGroupException {

        PeerGroup theNewGroup = null;

        if (null != gid) {
            theNewGroup = parentGroup.getGlobalRegistry().lookupInstance(gid);
        }

        if (theNewGroup != null) return theNewGroup;

        try {

            theNewGroup = (PeerGroup) loadModule(gid, (JxtaSocket) impl, false);

        } catch (Throwable any) {

            Logging.logCheckedSevere(LOG, "Could not load group implementation\n", any);
            throw new PeerGroupException("Could not load group implementation", any);

        }

        if ( publish ) {

            try {

                // The group adv definitely needs to be published.
                theNewGroup.publishGroup(name, description);

            } catch (Exception any) {

                Logging.logCheckedWarning(LOG, "Could not publish group or implementation:\n", any);

            }

        }

        return theNewGroup;

    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup newGroup(PeerGroupID gid) throws PeerGroupException {

        if ((gid == null) || ID.nullID.equals(gid)) {
            throw new IllegalArgumentException("Invalid peer group ID");
        }

        PeerGroup result = parentGroup.getGlobalRegistry().lookupInstance(gid);

        if (result != null) {
            return result;
        }

        PeerGroupAdvertisement adv;

        try {
            adv = (PeerGroupAdvertisement) discoverOne(DiscoveryService.GROUP, "GID", gid.toString(), 120, PeerGroupAdvertisement.class);
        } catch (Throwable any) {
            throw new PeerGroupException("Failed finding group advertisement for " + gid, any);
        }

        if (adv == null) {
            throw new PeerGroupException("Could not find group advertisement for group " + gid);
        }

        return newGroup(adv);
    }

    /**
     * {@inheritDoc}
     */
    public static IJxtaLoader getLoader() {
        return loader;
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerName() {
        // before init we must fail.
        if (null == peerAdvertisement) {
            throw new IllegalStateException("PeerGroup not sufficiently initialized");
        }
        return peerAdvertisement.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getPeerGroupName() {
        // before init we must fail.
        if (null == peerGroupAdvertisement) {
            throw new IllegalStateException("PeerGroup not sufficiently initialized");
        }
        return peerGroupAdvertisement.getName();
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroupID getPeerGroupID() {
        // before init we must fail.
        if (null == peerGroupAdvertisement) {
            throw new IllegalStateException("PeerGroup not sufficiently initialized");
        }

        return peerGroupAdvertisement.getPeerGroupID();
    }

    /**
     * {@inheritDoc}
     */
    public PeerID getPeerID() {
        // before init we must fail.
        if (null == peerAdvertisement) {
            throw new IllegalStateException("PeerGroup not sufficiently initialized");
        }
        return peerAdvertisement.getPeerID();
    }

    /**
     * {@inheritDoc}
     */
    public PeerAdvertisement getPeerAdvertisement() {
        return peerAdvertisement;
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        return peerGroupAdvertisement;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRendezvous() {

        if (rendezvous == null) Logging.logCheckedFine(LOG, "Rendezvous service null");

        return (rendezvous != null) && rendezvous.isRendezVous();

    }

    /*
     * shortcuts to the well-known services, in order to avoid calls to lookup.
     */
    /**
     * {@inheritDoc}
     */
    public EndpointService getEndpointService() {
        if (endpoint == null) {
            return null;
        }
        return endpoint;
    }

    /**
     * {@inheritDoc}
     */
    public ResolverService getResolverService() {
        if (resolver == null) {
            return null;
        }
        return resolver; // .getInterface();
    }

    public GlobalRegistry getGlobalRegistry()
    {
        return parentGroup.getGlobalRegistry();
    }

    /**
     * {@inheritDoc}
     */
    public DiscoveryService getDiscoveryService() {
        if (discovery == null) {
            return null;
        }
        return discovery; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public PeerInfoService getPeerInfoService() {
        if (peerinfo == null) {
            return null;
        }
        return peerinfo; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public MembershipService getMembershipService() {
        if (membership == null) {
            return null;
        }
        return membership; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public PipeService getPipeService() {
        if (pipe == null) {
            return null;
        }
        return pipe; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public RendezVousService getRendezVousService() {
        if (rendezvous == null) {
            return null;
        }
        return rendezvous; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public AccessService getAccessService() {
        if (access == null) {
            return null;
        }
        return access; // .getInterface();
    }

    /**
     * {@inheritDoc}
     */
    public ContentService getContentService() {
        if (content == null) {
            return null;
        }
        return content; // .getInterface();
    }

    public TaskManager getTaskManager() {
        return parentGroup.getTaskManager();
        }

//    @Deprecated
//    public ThreadGroup getHomeThreadGroup() {
//        return JXSE_THREAD_GROUP;
//    }
}
