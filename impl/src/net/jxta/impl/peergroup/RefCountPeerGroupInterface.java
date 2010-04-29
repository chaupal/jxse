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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.service.Service;
import net.jxta.exception.ServiceNotFoundException;

/**
 * Provides a very-strong (counted) reference to a peergroup.  When the peer 
 * group reference count reaches zero, the peergroup terminates itself. We don't 
 * depend upon GC for managing peergroup life-cycle as there are too many strong
 * references from the various services that prevent the peer group from ever 
 * being finalized. The alternative, to give out only weak references to the 
 * peer group seems impractical.
 */
class RefCountPeerGroupInterface extends PeerGroupInterface {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(RefCountPeerGroupInterface.class.getName());

    /**
     *  Map for resolving multiple instances of a single MCID by role #.
     */
    private final Map<ID, ID[]> roleMap;
    
    /**
     *  Constructs an interface object that front-ends the provided PeerGroup.
     *
     * @param theRealThing the peer group
     */
    RefCountPeerGroupInterface(GenericPeerGroup theRealThing) {
        this(theRealThing, null);
    }

    /**
     * Constructs an interface object that front-ends the provided PeerGroup.
     * 
     * @param theRealThing the peer group
     * @param roleMap  Map of MCIDs for groups with multiple role ids of a 
     * single service.
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

                Logging.logCheckedSevere(LOG, "[", getPeerGroupID(), "] Referenced Group has been GCed. This is an application error. Please call stopApp() before releasing Peer Group references. {", instance, "}");
                unref();

            }

        } finally {
            super.finalize();
        }

    }

    /**
     * {@inheritDoc}
     * <p/>
     * Normally the interface object protects the real object's start and stop 
     * methods from being called. Unlike the weak peer group interface objects, 
     * we do call the real {@link PeerGroup#startApp(String[])} as even the
     * creator of a group does not have access to the real object. So we must
     * invoke the peer group {@code startApp()} which is responsible for
     * ensuring that it is executed only once (if needed).
     */
    @Override
    public int startApp(String[] args) {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return temp.startApp(args);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since THIS is already an interface object it could return itself.
     * However, the group wants to know about the number of interfaces objects
     * floating around so we have the group make a new one. This way, 
     * applications which want to use {@link #unref()} on interfaces can avoid 
     * sharing interface objects by using {@link #getInterface()} as a sort of 
     * clone with the additional ref-counting semantics.
     */
    @Override
    public PeerGroup getInterface() {
        PeerGroup temp = groupImpl;

        if (unrefed.get() || (null == temp)) {
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
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
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
        }

        return new PeerGroupInterface(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unref() {
        if (!unrefed.get()) {
            ((GenericPeerGroup) groupImpl).decRefCount();
        }
        
        return super.unref();
    }

    /**
     * Service-specific role mapping is implemented here.
     */

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
            throw new IllegalStateException("This Peer Group interface object has been unreferenced and can no longer be used. {" + instance + "}");
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
