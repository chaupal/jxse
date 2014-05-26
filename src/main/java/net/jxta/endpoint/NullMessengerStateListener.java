package net.jxta.endpoint;

/**
 * Null object implementation of MessengerStateListener. Does nothing in response to
 * state changes, and continues to listen indefinitely (i.e. messengerStateChanged always
 * returns true).
 */
public class NullMessengerStateListener implements MessengerStateListener {

	public boolean messengerStateChanged(int newState) {
		return true;
	}

}
