/*
 *  The Sun Project JXTA(TM) Software License
 *
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.content;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import static net.jxta.impl.content.ModuleLifecycleState.*;

/**
 * Tracks the lifecycle state of an individual Module and encapsulates
 * intialization and startup parameters to allow for de-coupled, asynchronous
 * management of subordinate Modules within the ModuleLifecycleManager.
 */
public class ModuleLifecycleTracker<T extends Module> {

    /**
     * The subordinate Module.
     */
    private final T module;

    /**
     * The peer group that this Module will be running in.
     */
    private final PeerGroup peerGroup;

    /**
     * The subordinate Module's assigned ID.
     */
    private final ID assignedID;

    /**
     * The subordinate Module's advertisement.
     */
    private final Advertisement advertisement;

    /**
     * Thhe subordinate Module's startup arguments.
     */
    private final String[] startArgs;

    /**
     * Module's current state.
     */
    private ModuleLifecycleState state = UNINITIALIZED;

    /**
     * List of listeners.
     */
    private List<ModuleLifecycleListener> listeners =
            new CopyOnWriteArrayList<ModuleLifecycleListener>();

    /**
     * Constructor.
     *
     * @param mod subordinate module
     * @param group peer group that this module will run in
     * @param id ID assigned to this module, or null
     * @param adv module's advertisement, or null
     * @param args arguments to pass to <code>startApp</code> when the module
     *  is to be started
     */
    public ModuleLifecycleTracker(
            T mod, PeerGroup group, ID id, Advertisement adv, String[] args) {
        module = mod;
        peerGroup = group;
        assignedID = id;
        advertisement = adv;
        startArgs = args;
    }

    /**
     * Get the subordinate module.
     *
     * @return subordinate module
     */
    public T getModule() {
        return module;
    }

    /**
     * Add the specified ModuleLifecycleListener to our list of
     * listeners to be notified when lifecycle events take place.
     *
     * @param listener listener to add
     */
    public void addModuleLifecycleListener(ModuleLifecycleListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the specified ModuleLifecycleListener from our list of
     * listeners.
     *
     * @param listener listener to remove
     */
    public void removeModuleLifecycleListener(
            ModuleLifecycleListener listener) {
        listeners.remove(listener);
    }

    /**
     * Initialize the subordinate Module with the initialization parameters
     * saved away at construction time, if the Module has not yet been
     * initialized.
     *
     * @return true if intialized successfully, false otherwise
     */
    public boolean init() {
        PeerGroupException toThrow = initImpl();
        if (toThrow == null) {
            return true;
        } else {
            fireUnhandledException(toThrow);
            return false;
        }
    }

    /**
     * Starts the Module with the startup arguments provided at construction
     * time.  If the Module has not yet been initialized, it will be.
     * 
     * @return start result as defined in <code>net.jxta.platform.Module<code>
     */
    public int startApp() {
        ModuleLifecycleState toFire = null;
        int result;

        // Make sure we are initialized
        PeerGroupException toThrow = initImpl();
        if (toThrow != null) {
            fireUnhandledException(toThrow);
            return Module.START_AGAIN_STALLED;
        }

        synchronized(this) {
            if (state == STARTED) {
                return Module.START_OK;
            } else if (state == DISABLED) {
                return Module.START_DISABLED;
            }

            result = module.startApp(startArgs);
            if (result == Module.START_OK) {
                state = STARTED;
                toFire = state;
            } else if (result == Module.START_DISABLED) {
                state = DISABLED;
                toFire = state;
            }
        }

        // Notify our listeners out of sync block
        if (toFire != null) {
            fireStateUpdated(toFire);
        }

        return result;
    }

    /**
     * Stops the Module, if it has been previously started.  If the Module
     * has never been initialized or started, this call will have no effect.
     */
    public void stopApp() {
        ModuleLifecycleState toFire = null;

        synchronized(this) {
            if (state == STARTED) {
                module.stopApp();
                state = STOPPED;
                toFire = state;
            }
        }

        // Notify our listeners out of sync block
        if (toFire != null) {
            fireStateUpdated(toFire);
        }
    }

    /**
     * Returns the current state of the Module.
     *
     * @return current state
     */
    public ModuleLifecycleState getState() {
        synchronized(this) {
            return state;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * The bulk of the init method implementation, pulled out into it's
     * own private method to facilitate code reuse between init() and
     * startApp() without throwing events while synchronized.
     *
     * @return exception to throw, or null if no exception thrown
     */
    private PeerGroupException initImpl() {
        ModuleLifecycleState toFire = null;
        PeerGroupException toThrow = null;

        synchronized(this) {
            if (state == UNINITIALIZED) {
                try {
                    module.init(peerGroup, assignedID, advertisement);
                    state = INITIALIZED;
                    toFire = state;
                } catch (PeerGroupException pgx) {
                    toThrow = pgx;
                }
            }
        }

        // Notify our listeners out of sync block
        if (toFire != null) {
            fireStateUpdated(toFire);
        }

        return toThrow;
    }

    /**
     * Notify all listeners of our change in lifecycle state.
     *
     * @param newState our new state
     */
    private void fireStateUpdated(ModuleLifecycleState newState) {
        for (ModuleLifecycleListener listener : listeners) {
            listener.moduleLifecycleStateUpdated(this, newState);
        }
    }

    /**
     * Notify all listeners of an unhandled exception.
     *
     * @param culprit the culprit exception
     */
    private void fireUnhandledException(PeerGroupException culprit) {
        for (ModuleLifecycleListener listener : listeners) {
            listener.unhandledPeerGroupException(this, culprit);
        }
    }

}
