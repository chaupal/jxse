package net.jxta.endpoint;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MessengerStateListenerSet {

    /**
     * List of all registered state change listeners.
     */
    private final Set<MessengerStateListener> stateChangeListeners;
    
    public MessengerStateListenerSet() {
        stateChangeListeners = new HashSet<MessengerStateListener>();
    }
    
    public synchronized void notifyNewState(int newState) {
        final Iterator<MessengerStateListener> listenerIter = stateChangeListeners.iterator();
        while(listenerIter.hasNext()) {
            final MessengerStateListener listener = listenerIter.next();
            if(!listener.messengerStateChanged(newState)) {
                listenerIter.remove();
            }
        }
    }
    
    public synchronized void addStateListener(MessengerStateListener listener) {
        stateChangeListeners.add(listener);
    }
    
    public synchronized void removeStateListener(MessengerStateListener listener) {
        stateChangeListeners.remove(listener);
    }
}
