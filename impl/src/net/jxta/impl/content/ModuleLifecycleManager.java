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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import static net.jxta.impl.content.ModuleLifecycleState.*;

/**
 * Manages the lifecycle of a set of subordinate Modules such that a goal
 * state can be set and the subordinate Modules will make their
 * way toward the goal state using the semantics defined by the return
 * value of <code>Module.startApp()</code>.  Although the API presented by
 * this class has the ability to manage Modules spanning more than one
 * PeerGroup, the intent is for a manager instance to be created per peer
 * group, allowing the entire set of Modules in the PeerGroup to be centrally
 * managed by a single control point.
 */
public class ModuleLifecycleManager<T extends Module> {
    // TODO 20070911 mcumings: Exponential backoff to retry stalled modules?

    /**
     * The maximum number of consecutive times a tracker state check can
     * stall before terminal failure.
     */
    private static final int MAX_STALLS =
            Integer.getInteger(ModuleLifecycleManager.class.getName()
            + ".maxStalls", 10);

    /**
     * Current state of the manager.  This acts as the target state for all
     * subordinate Module.
     */
    private ModuleLifecycleState state = UNINITIALIZED;

    /**
     * List of all Module instances that we are going to manage.
     */
    private Set<ModuleLifecycleTracker<T>> trackers =
            new CopyOnWriteArraySet<ModuleLifecycleTracker<T>>();

    /**
     * List of Module instances that have successfully started.  This is
     * mainly used to allow us to stop the Modules in the reverse order.
     */
    private List<ModuleLifecycleTracker<T>> started =
            new CopyOnWriteArrayList<ModuleLifecycleTracker<T>>();

    /**
     * List of our ModuleLifecycleListeners.
     */
    private List<ModuleLifecycleListener> mlListeners =
            new CopyOnWriteArrayList<ModuleLifecycleListener>();

    /**
     * List of our ModuleLifecycleManagerListeners.
     */
    private List<ModuleLifecycleManagerListener> mlmListeners =
            new CopyOnWriteArrayList<ModuleLifecycleManagerListener>();

    /**
     * Flag indicating that a check is currently running.  This allows the
     * critical section to be protected without requiring synchronization
     * during processing.
     */
    private boolean checkRunning;

    /**
     * Proxy listener implementation to prevent exposing these methods
     * as publicly accessible methods of the manager class.
     */
    private ModuleLifecycleListener proxy = new ModuleLifecycleListener() {
        /**
         * {@inheritDoc}
         */
        public void unhandledPeerGroupException(
                ModuleLifecycleTracker subject, PeerGroupException mlcx) {
            for (ModuleLifecycleListener listener : mlListeners) {
                listener.unhandledPeerGroupException(subject, mlcx);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void moduleLifecycleStateUpdated(
                ModuleLifecycleTracker subject, ModuleLifecycleState newState) {
            for (ModuleLifecycleListener listener : mlListeners) {
                listener.moduleLifecycleStateUpdated(subject, newState);
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Creates a new instance of ModuleLifecycleManager.
     */
    public ModuleLifecycleManager() {
        // Empty
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods:

    /**
     * Add the specified ModuleLifecycleListener to our list of
     * mlListeners to be notified when lifecycle events take place.
     *
     * @param listener listener to add
     */
    public void addModuleLifecycleListener(ModuleLifecycleListener listener) {
        mlListeners.add(listener);
    }

    /**
     * Remove the specified ModuleLifecycleListener from our list of
     * mlListeners.
     *
     * @param listener listener to remove
     */
    public void removeModuleLifecycleListener(
            ModuleLifecycleListener listener) {
        mlListeners.remove(listener);
    }

    /**
     * Add the specified ModuleLifecycleManagerListener to our list of
     * listeners to be notified when lifecycle events take place.
     *
     * @param listener listener to add
     */
    public void addModuleLifecycleManagerListener(
            ModuleLifecycleManagerListener listener) {
        mlmListeners.add(listener);
    }

    /**
     * Remove the specified ModuleLifecycleManagerListener from our list of
     * listeners.
     *
     * @param listener listener to remove
     */
    public void removeModuleLifecycleListener(
            ModuleLifecycleManagerListener listener) {
        mlmListeners.remove(listener);
    }

    /**
     * Adds the Module provided.  Once it has been added, it
     * will start progressing toward the current master state.
     *
     * @param subordinate the Module to be managed
     * @param peerGroup the PeerGroup within which the master Module
     *  is operating.
     * @param assignedID the ID assigned to this Module
     * @param advertisement the Module's advertisement
     * @param args arguments to pass to the tracker when it is started
     */
    public void addModule(T subordinate, PeerGroup peerGroup,
            ID assignedID, Advertisement advertisement, String[] args) {
        ModuleLifecycleTracker<T> tracker =
                new ModuleLifecycleTracker<T>(
                subordinate, peerGroup, assignedID, advertisement, args);
        tracker.addModuleLifecycleListener(proxy);
        if (trackers.add(tracker)) {
            if (tracker.getState() == STARTED) {
                started.add(tracker);
            }
            checkTrackers();
        }
    }

    /**
     * Removes the specified Module from the auspices of the manager.
     *
     * @param subordinate the previously added Module which should be removed
     *  from the tracked Module list
     * @param stopFirst set to true if the Module should always be returned to
     *  a stopped state, false to leave the Module in it's current state
     */
    public void removeModule(Module subordinate, boolean stopFirst) {
        for (ModuleLifecycleTracker tracker : trackers) {
            Module module = tracker.getModule();
            if (subordinate.equals(module)) {
                tracker.removeModuleLifecycleListener(proxy);

                // Stop on request
                if (stopFirst) {
                    tracker.stopApp();
                }

                // Make sure we no longer track this Module as started
                started.remove(tracker);
                break;
            }
        }
    }

    /**
     * Returns the current list of ModuleLifecycleTrackers.
     *
     * @return list of ModuleLifecycleTrackers
     */
    public Set<ModuleLifecycleTracker<T>> getModuleLifecycleTrackers() {
        return trackers;
    }

    /**
     * Set the goal state to initialize the subordinate modules.
     */
    public void init() {
        setState(INITIALIZED);
    }

    /**
     * Set the goal state to start the subordinate modules.
     */
    public void start() {
        setState(STARTED);
    }

    /**
     * Set the goal state to stop the subordinate modules.
     */
    public void stop() {
        setState(STOPPED);
    }

    /**
     * Convenience method to obtain the total number of Modules being tracked.
     *
     * @return number of tracked Modules
     */
    public int getModuleCount() {
        return trackers.size();
    }

    /**
     * Returns the number of subordinate modules that are currently at
     * the goal state.  This method knows to check the ModuleLifecycle
     * state's <code>isInitialized</code> and <code>isStarted</code> methods
     * to match the intent of the goal state.
     *
     * @return number of subordinate Modules in the goal state
     */
    public int getModuleCountInGoalState() {
        ModuleLifecycleState goalState;

        synchronized(this) {
            goalState = state;
        }

        int result = 0;
        for (ModuleLifecycleTracker tracker : trackers) {
            ModuleLifecycleState tState = tracker.getState();
            switch(goalState) {
                case INITIALIZED:
                    if (tState != UNINITIALIZED) {
                        result++;
                    }
                    break;
                case STOPPED:
                    if (tState != STARTED) {
                        result++;
                    }
                    break;
                default:
                    // All other states are assumed to require direct equality
                    if (tState == goalState) {
                        result++;
                    }
                    break;
            }
        }
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Notify all manager listeners of a stalled module.
     *
     * @param tracker the tracker containing the stalled Module
     */
    private void fireModuleStalled(ModuleLifecycleTracker tracker) {
        for (ModuleLifecycleManagerListener listener : mlmListeners) {
            listener.moduleStalled(this, tracker);
        }
    }

    /**
     * Notify all manager listeners of adisabled module.
     *
     * @param tracker the tracker containing the disabled Module
     */
    private void fireModuleDisabled(ModuleLifecycleTracker tracker) {
        for (ModuleLifecycleManagerListener listener : mlmListeners) {
            listener.moduleDisabled(this, tracker);
        }
    }

    /**
     * Set the goal state to the specified state.
     *
     * @param newState new goal state
     */
    private void setState(ModuleLifecycleState newState) {
        synchronized(this) {
            if (newState == state) {
                // Nothing to do
                return;
            }
            state = newState;
        }
        checkTrackers();
    }

    /**
     * Check the trackers and see if any can be progressed toward the goalState
     * state.
     */
    private void checkTrackers() {
        ModuleLifecycleState goalState;

        // Gain exclusive processing access
        synchronized(this) {
            while(checkRunning) {
                try {
                    wait();
                } catch (InterruptedException intx) {
                    Thread.interrupted();
                }
            }
            checkRunning = true;
            goalState = state;
        }

        /*
         * Each state has it's own way of being checked.
         */
        switch (goalState) {
            case UNINITIALIZED:
                // Ignore this goalState
                break;
            case INITIALIZED:
                checkTrackersInit();
                break;
            case STARTED:
                checkTrackersStart();
                break;
            case STOPPED:
                checkTrackersStop();
                break;
            default:
                throw(new IllegalStateException(
                        "Unsupported goal state: " + goalState));
        }

        // Release processing access
        synchronized(this) {
            checkRunning = false;
            notifyAll();
        }
    }

    /**
     * Perform tracker initialization.
     */
    private void checkTrackersInit() {
        for (ModuleLifecycleTracker tracker : trackers) {
            tracker.init();
        }
    }

    /**
     * Perform tracker startup.  This is the only goal state which can
     * have different results requiring retries.
     */
    private void checkTrackersStart() {
        List<ModuleLifecycleTracker> stalled =
                new ArrayList<ModuleLifecycleTracker>();
        boolean progress;
        int stalls = 0;
        int maxIterations = trackers.size() * trackers.size();
        int iteration = 0;

        do {
            List<ModuleLifecycleTracker> disabled = null;
            progress = false;
            stalled.clear();
            for (ModuleLifecycleTracker<T> tracker : trackers) {
                if (tracker.getState() == STARTED) {
                    // Ignore modules which are already started
                    continue;
                }

                int result = tracker.startApp();
                if (result == Module.START_OK) {
                    started.add(tracker);
                    progress = true;
                } else if (result == Module.START_AGAIN_PROGRESS) {
                    progress = true;
                    stalled.add(tracker);
                } else if (result == Module.START_DISABLED) {
                    if (disabled == null) {
                        // Lazy init
                        disabled = new ArrayList<ModuleLifecycleTracker>();
                    }
                    disabled.add(tracker);
                    progress = true;
                } else {
                    // some other error, including START_AGAIN_STALLED
                    stalled.add(tracker);
                }
            }
            
            // Check for and dereference disabled modules
            if (disabled != null) {
                for (ModuleLifecycleTracker tracker : disabled) {
                    fireModuleDisabled(tracker);
                    tracker.removeModuleLifecycleListener(proxy);
                    trackers.remove(tracker);
                }
            }

            // Check to see if we're done
            if (stalled.size() == 0) {
                break;
            }

            // Too many attempts?
            if (++iteration >= maxIterations) {
                for (ModuleLifecycleTracker tracker : stalled) {
                    fireModuleStalled(tracker);
                }
                break;
            }

            // If we made progress, retry immediately
            if (progress) {
                stalls = 0;
                continue;
            }

            // Too many consecutive stalls?
            if (++stalls >= MAX_STALLS) {
                for (ModuleLifecycleTracker tracker : stalled) {
                    fireModuleStalled(tracker);
                }
                break;
            }
        } while (true);
    }

    /**
     * Perform tracker shutdown.
     */
    private void checkTrackersStop() {
        Collections.reverse(started);
        for (ModuleLifecycleTracker tracker : started) {
            tracker.stopApp();
        }
        started.clear();
    }

}
