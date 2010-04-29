/*
 * Copyright (c) 2004-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.endpoint;


import java.io.IOException;
import java.io.InterruptedIOException;

import net.jxta.util.AbstractSimpleSelectable;
import net.jxta.util.SimpleSelectable;


/**
 * An AbstractMessenger is used to implement messengers (for example, by transport modules).
 * It supplies the convenience, bw compatible, obvious, or otherwise rarely changed methods.
 * Many method cannot be overloaded in order to ensure standard behaviour.
 * The rest is left to implementations.
 *
 * @see net.jxta.endpoint.EndpointService
 * @see net.jxta.endpoint.EndpointAddress
 * @see net.jxta.endpoint.Message
 */
public abstract class AbstractMessenger extends AbstractSimpleSelectable implements Messenger {

    /**
     * The default Maximum Transmission Unit.
     */
    protected static final long DEFAULT_MTU = Long.parseLong(System.getProperty("net.jxta.MTU", "65536"));

    /**
     * The destination address of messages sent on this messenger.
     */
    protected final EndpointAddress dstAddress;
    
    private final MessengerStateListenerSet listeners = new MessengerStateListenerSet();
    protected final MessengerStateListener distributingListener = new MessengerStateListener() {
        public boolean messengerStateChanged(int newState) {
        	listeners.notifyNewState(newState);
            return true;
        }
    };

    /**
     * Create a new abstract messenger.
     * @param dest who messages should be addressed to
     */
    public AbstractMessenger(EndpointAddress dest) {
        dstAddress = dest;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A simple implementation for debugging. Do not depend upon the format.
     */
    @Override
    public String toString() {
        return super.toString() + " {" + dstAddress + "}";
    }

    /**
     * {@inheritDoc}
     */
    public final EndpointAddress getDestinationAddress() {
        return dstAddress;
    }

    /**
     * {@inheritDoc}
     * <p/>It is not always enforced. At least this much can always be sent. 
     */
    public long getMTU() {
        return DEFAULT_MTU;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is a minimal implementation. It may not detect closure
     * initiated by the other side unless the messenger was actually used
     * since. A more accurate (but not mandatory implementation) would
     * actually go and check the underlying connection, if relevant...unless
     * breakage initiated by the other side is actually reported asynchronously
     * when it happens. Breakage detection from the other side need not
     * be reported atomically with its occurrence. This not very important
     * since we canonicalize transport messengers and so do not need to
     * aggressively collect closed ones. When not used, messengers die by themselves.
     */
    public boolean isClosed() {
        return (getState() & USABLE) == 0;
    }

    /**
     * {@inheritDoc}
     */
    public final void flush() throws IOException {
        int currentState = 0;

        try {
            currentState = waitState(IDLE, 0);
        } catch (InterruptedException ie) {
            final InterruptedIOException iio = new InterruptedIOException("flush() interrupted");
            iio.initCause(ie);
            throw iio;
        }
        
        if ((currentState & (CLOSED | USABLE)) != 0) {
            return;
        }
        
        throw new IOException("Messenger was unexpectedly closed.");
    }

    /**
     * {@inheritDoc}
     */
    public final boolean sendMessage(Message msg) throws IOException {
        return sendMessage(msg, null, null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public void sendMessage(Message msg, String service, String serviceParam, OutgoingMessageEventListener listener) {
        throw new UnsupportedOperationException("This legacy method is not supported by this messenger.");
    }

    /**
     * {@inheritDoc}
     */
    public final boolean sendMessage(Message msg, String rService, String rServiceParam) throws IOException {

        // We have to retrieve the failure from the message and throw it if its an IOException, this is what the API
        // says that this method does.

        final boolean ret = sendMessageN(msg, rService, rServiceParam);
        final Object failed = msg.getMessageProperty(Messenger.class);

        if (failed == null) {
            // huh ?
            return ret;
        }
        if (failed == OutgoingMessageEvent.SUCCESS) {
            return true;
        }

        Throwable throwable;
        if (failed instanceof Throwable)
            throwable = (Throwable) failed;
        else throwable = ((OutgoingMessageEvent) failed).getFailure();

        if (throwable == null) {
            // Must be saturation, then. (No throw for that).
            return false;
        }

        // Now see how we can manage to throw it.
        if (throwable instanceof IOException) {
            throw (IOException) throwable;
        } else if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        }

        final IOException failure = new IOException("Failure sending message");
        failure.initCause(throwable);
        throw failure;
    }

    /**
     * {@inheritDoc}
     */
    public final int waitState(int wantedStates, long timeout) throws InterruptedException {

        // register the barrier first, in case the state changes concurrently while we do
    	// our initial interrogation
    	final MessengerStateBarrier barrier = new MessengerStateBarrier(wantedStates);
    	addStateListener(barrier);
    	
    	final int currentState = getState();
    	if((currentState & wantedStates) != 0) {
    		barrier.expire();
    		return currentState;
    	}
    	
    	// we are not currently in any of the states we want, so wait until we are
    	// notified that we are
        final int matchingState = barrier.awaitMatch(timeout);
        barrier.expire();
        
        if(matchingState != MessengerStateBarrier.NO_MATCH) {
            return matchingState;
        } else {
        	return getState();
        }
    }
    
    public void addStateListener(MessengerStateListener listener) {
        listeners.addStateListener(listener);
    }
    
    public void removeStateListener(MessengerStateListener listener) {
        listeners.removeStateListener(listener);
    }

    /*
     * SimpleSelectable implementation.
     */

    /**
     * Implements a default for all AbstractMessengers: mirror the event to our selectors. This is what is needed by all the
     * known AbstractMessengers that register themselves somewhere. (That is ChannelMessengers).
     * FIXME - jice@jxta.org 20040413: Not sure that this is the best default.
     *
     * @param changedObject Ignored.
     */
    public void itemChanged(SimpleSelectable changedObject) {
        notifyChange();
    }
}
