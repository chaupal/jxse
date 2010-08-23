package net.jxta.endpoint;

public interface MessengerStateListener {

    /**
     * This method is invoked whenever the Messenger changes state.
     * 
     * @param newState the state that the Messenger has changed to.
     * @return whether or not this listener is interested in any subsequent
     * state changes. If false is returned, the messenger will be unsubscribed
     * from the messenger.
     */
    boolean messengerStateChanged(int newState);
    
}
