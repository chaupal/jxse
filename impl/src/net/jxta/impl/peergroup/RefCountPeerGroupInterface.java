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

import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.service.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.logging.Logging;

/**
 * A PeerGroupInterface object that also serves as a very-strong reference to 
 * the peergroup. When the last such goes away, the peergroup terminates itself
 * despite the existence of aeternal strong references from the various 
 * service's threads that would prevent it from ever being finalized. The 
 * alternative, to give only weak references to threads, seems impractical.
 */
class RefCountPeerGroupInterface extends PeerGroupInterface {

    /**
     * Logger
     */
    private final static Logger LOG = Logger.getLogger(RefCountPeerGroupInterface.class.getName());

    /**
     *  Map for resolving multiple instances of a single MCID by role #.
     */
    private final Map<ID, ID[]> roleMap;

    /**
     * Constructs an interface object that front-ends a given
     * PeerGroup object.
     *
     * @param theRealThing the peer group
     */
    RefCountPeerGroupInterface(GenericPeerGroup theRealThing) {
        super(theRealThing);
        roleMap = null;
    }

    /**
     * 
     * @param theRealThing the peer group
     * @param roleMap Map of MCIDs for groups with multiple role ids of a single
     *  service.
     */
    RefCountPeerGroupInterface(GenericPeerGroup theRealThing, Map<ID, ID[]> roleMap) {
        super(theRealThing);
        this.roleMap = roleMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!unrefed.get()) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("[" + getPeerGroupID() + "] Referenced Group has been GCed. This is an application error. Please call stopApp() before releasing Peer Group references.");
                }
            }

            unref();
        } finally {
            super.finalize();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Normally it is ignored. By definition, the interface object protects the 
     * real object's start/stop methods from being called. Unlike the weak
     * peer group interface objects, we do call the real startApp() method as 
     * even the creator of a group does not have access to the real object. So 
     * the interface has to forward startApp() to the peer group, which is 
     * responsible for ensuring that it is executed only once (if needed).
     *
     * @param arg A table of strings arguments.
     * @return int status indication.
     */
    @Override
    public int startApp(String[] args) {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used.");
        }

        return temp.startApp(args);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since THIS is already an interface object it could return itself.
     * However, the group wants to know about the number of interfaces objects
     * floating around so we have the group make a new one. This way, 
     * applications which want to use unref() on interfaces can avoid sharing
     * interface objects by using getInterface() as a sort of clone with the 
     * additional ref-counting semantics.
     */
    @Override
    public PeerGroup getInterface() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used.");
        }

        return temp.getInterface();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Returns a weak interface object that refers to this interface object
     * rather than to the group directly. The reason for that is that we want
     * the owner of this interface object to be able to invalidate all weak 
     * interface objects made out of this interface object, without them keeping 
     * a reference to the group object, and without necessarily having to 
     * terminate the group.
     */
    @Override
    public PeerGroup getWeakInterface() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used.");
        }

        return new PeerGroupInterface(this);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * We can't call super() because we have to ensure that the unreferencing
     * happens only once.
     */
    @Override
    public void unref() {
        if (unrefed.compareAndSet(false, true)) {
            try {
                ((GenericPeerGroup) groupImpl).decRefCount();
            } finally {
                groupImpl = null;
            }
        }
    }

    /**
     * Service-specific role mapping is implemented here.
     **/

    /**
     * {@inheritDoc}
     */
    @Override
    public Service lookupService(ID name) throws ServiceNotFoundException {
        return lookupService(name, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Service lookupService(ID name, int roleIndex) throws ServiceNotFoundException {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used.");
        }

        if (roleMap != null) {
            ID[] map = roleMap.get(name);

            // If there is a map, remap; else, identity is the default for
            // role 0 only; the default mapping has only index 0.

            if (map != null) {
                if (roleIndex < 0 || roleIndex >= map.length) {
                    throw new ServiceNotFoundException(name + "[" + roleIndex + "]");
                }

                // We have a translation; look it up directly
                return temp.lookupService(map[roleIndex]);
            }
        }

        // No translation; use the name as-is, provided roleIndex is 0.
        // Do not call groupImpl.lookupService(name, id); group impls should not
        // have to implement it at all.

        if (roleIndex != 0) {
            throw new ServiceNotFoundException(name + "[" + roleIndex + "]");
        }

        return temp.lookupService(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ID> getRoleMap(ID name) {
        List<ID> roles = Collections.singletonList(name);

        if (roleMap != null) {
            ID[] map = roleMap.get(name);

            // If there is a map, remap; else, identity is the default for
            // role 0 only; the default mapping has only index 0.

            if (map != null) {
                roles = Arrays.asList(map);
            }
        }

        return Collections.unmodifiableList(roles).iterator();
    }
}
