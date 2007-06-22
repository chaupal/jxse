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


import net.jxta.logging.Logging;
import net.jxta.util.SimpleSelectable;
import net.jxta.util.SimpleSelectable.IdentityReference;
import net.jxta.util.SimpleSelector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The legacy getMessenger asynchronous API never returns any object to the invoker until a messenger could actually be made,
 * allowing the application to supply a listener to be invoked when the operation completes. The legacy Messenger API also
 * provides a method to send messages that calls a listener to report the outcome of the operation.  <p/>
 * <p/>
 * The model has changed, so that an asynchronous messenger is made unresolved and returned immediately to the invoker, which can
 * then request opening or even just send a message to force the opening. Subsequently, the messenger can be used as a control
 * block to monitor progress with {@link Messenger#register} and {@link Messenger#waitState}.<p/>
 * <p/>
 * Likewise, the outcome of sending a message is a property of that message. Messages can be selected to monitor property changes
 * with {@link Message#register} and {@link net.jxta.endpoint.Message#getMessageProperty(Object)} (the outcome property key is
 * <code>Messenger.class</code>).<p/>
 * <p/>
 * This class here provides the legacy listener model on top of the new model for applications that prefer listeners. This class
 * is used internally to emulate the legacy listener behaviour, so that applications do not need to be adapted.<p/>
 * <p/>
 * Note: one instance of this class gets instantiated by each EndpointService interface. However, it does not start using any
 * resources until it first gets used.<p/>
 */
public class ListenerAdaptor implements Runnable {

    // FIXME - jice 20040413: Eventhough it is not as critical as it used to be we should get rid of old, never resolved entries..
    // Attempts are supposed to always fail or succeed rather soon. Here, we trust transports in that matter. Is it safe ?

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(ListenerAdaptor.class.getName());

    /**
     * The in progress messages.
     */
    private final Map<IdentityReference, ListenerContainer> inprogress = new HashMap<IdentityReference, ListenerContainer>(32);

    /**
     * The thread that does the work.
     */
    private Thread bgThread = null;

    /**
     * The selector that we use to watch messengers progress.
     */
    private final SimpleSelector selector = new SimpleSelector();

    /**
     * Are we asked to stop ?
     */
    private volatile boolean stopped = false;

    /**
     * The ThreadGroup in which this adaptor will run.
     */
    private final ThreadGroup threadGroup;

    /**
     * Standard Constructor
     *
     * @param threadGroup The ThreadGroup in which this adaptor will run.
     */
    public ListenerAdaptor(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    private synchronized void stop() {

        stopped = true;

        // Stop the thread if it was ever created.
        if (bgThread != null) {
            bgThread.interrupt();
            bgThread = null;
        }
    }

    /**
     * Cannot be re-started. Do not call once stopped.
     */
    private void init() {

        if (bgThread != null) {
            return;
        }

        bgThread = new Thread(threadGroup, this, "Listener Adaptor");
        bgThread.setDaemon(true);
        bgThread.start();
    }

    public void shutdown() {
        stop();
    }

    /**
     * Stop watching a given selectable.
     *
     * @param ts the selectable
     */
    private void forgetSelectable(SimpleSelectable ts) {
        // Either way, we're done with this one.
        ts.unregister(selector);

        synchronized (this) {
            inprogress.remove(ts.getIdentityReference());
        }
    }

    /**
     * Select the given message and invoke the given listener when the message sending is complete.
     *
     * @param listener The listener to invoke. If null the resolution will take place, but obviously no listener will be invoked.
     * @param m        The message being sent.
     * @return true if the message was registered successfully or the listener is null. If true it is guaranteed that the listener
     *         will be invoked unless null. If false, it is guaranteed that the listener will not be invoked.
     */
    public boolean watchMessage(OutgoingMessageEventListener listener, Message m) {
        synchronized (this) {

            if (stopped) {
                return false;
            }

            if (listener == null) {
                // We're done, then. The invoker does not really care.
                return true;
            }

            // Init if needed.
            init();

            // First we must ensure that if the state changes we'll get to handle it.
            ListenerContainer allListeners = inprogress.get(m.getIdentityReference());

            if (allListeners == null) {
                // Use ArrayList. The code is optimized for that.
                allListeners = new MessageListenerContainer();
                inprogress.put(m.getIdentityReference(), allListeners);
            }
            allListeners.add(listener);
        }

        // When we do that, the selector get notified. Therefore we will always check the initial state automatically. If the
        // selectable is already done with, the listener will be called by the selector's handler.
        m.register(selector);

        return true;
    }

    /**
     * Select the given messenger and invoke the given listener when the messenger is resolved.
     *
     * @param listener The listener to invoke. If null the resolution will take place, but obviously no listener will be invoked.
     * @param m        The messenger being resolved.
     * @return true if the messenger was registered successfully or the listener is null. If true it is guaranteed that the listener
     *         will be invoked unless null. If false, it is guaranteed that the listener will not be invoked.
     */
    public boolean watchMessenger(MessengerEventListener listener, Messenger m) {
        synchronized (this) {

            if (stopped) {
                return false;
            }

            if (listener == null) {
                // We're done, then. The invoker does not really care.
                return true;
            }

            // Init if needed.
            init();

            // First we must ensure that if the state changes we'll get to handle it.
            ListenerContainer allListeners = inprogress.get(m.getIdentityReference());

            if (allListeners == null) {
                // Use ArrayList. The code is optimized for that.
                allListeners = new MessengerListenerContainer();
                inprogress.put(m.getIdentityReference(), allListeners);
            }
            allListeners.add(listener);
        }

        // When we do that, the selector get notified. Therefore we will always check the initial state automatically. If the
        // selectable is already done with, the listener will be called by the selector's handler.
        m.register(selector);

        return true;
    }

    /*
     * Any sort of listener type.
     */
    static abstract class ListenerContainer extends ArrayList<java.util.EventListener> {

        public ListenerContainer() {
            super(1);
        }

        protected abstract void giveUp(SimpleSelectable what, Throwable how);

        protected abstract void process(SimpleSelectable what);
    }


    /**
     * For messages
     */
    class MessageListenerContainer extends ListenerContainer {

        private void messageDone(Message m, OutgoingMessageEvent event) {

            // Note: synchronization is externally provided. When this method is invoked, this
            // object has already been removed from the map, so the list of listener cannot change.

            // Do not throw an iterator in the landfill for such a trivial case: optimize for an array list.
            int i = size();

            if (event == OutgoingMessageEvent.SUCCESS) {
                // Replace it with a msg-specific one.
                event = new OutgoingMessageEvent(m, null);

                while (i-- > 0) {
                    try {
                        ((OutgoingMessageEventListener) get(i)).messageSendSucceeded(event);
                    } catch (Throwable any) {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "Uncaught throwable in listener", any);
                        }
                    }
                }
                return;
            }

            if (event == OutgoingMessageEvent.OVERFLOW) {
                // Replace it with a msg-specific one.
                event = new OutgoingMessageEvent(m, null);
            }

            while (i-- > 0) {
                try {
                    ((OutgoingMessageEventListener) get(i)).messageSendFailed(event);
                } catch (Throwable any) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Uncaught throwable in listener", any);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(SimpleSelectable what) {

            Message m = (Message) what;

            OutgoingMessageEvent event = (OutgoingMessageEvent) m.getMessageProperty(Messenger.class);

            if (event == null) {
                return;
            }

            // Remove this container-selectable binding
            forgetSelectable(what);

            // Invoke app listeners
            messageDone(m, event);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void giveUp(SimpleSelectable what, Throwable how) {
            messageDone((Message) what, new OutgoingMessageEvent((Message) what, how));
        }
    }


    /**
     * For messengers
     */
    class MessengerListenerContainer extends ListenerContainer {

        private void messengerDone(Messenger m) {

            // Note: synchronization is externally provided. When this method is invoked, this
            // object has already been removed from the map, so the list of listener cannot change.

            // Do not throw an iterator in the landfill for such a trivial case: optimize for an array list.
            int i = size();
            MessengerEvent event = new MessengerEvent(ListenerAdaptor.this, m, null);

            while (i-- > 0) {
                try {
                    ((MessengerEventListener) get(i)).messengerReady(event);
                } catch (Throwable any) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Uncaught throwable in listener", any);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void process(SimpleSelectable what) {

            Messenger m = (Messenger) what;

            if ((m.getState() & (Messenger.RESOLVED | Messenger.TERMINAL)) == 0) {
                return;
            }

            // Remove this container-selectable binding
            forgetSelectable(what);

            if ((m.getState() & Messenger.USABLE) == 0) {
                m = null;
            }

            // Invoke app listeners
            messengerDone(m);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void giveUp(SimpleSelectable what, Throwable how) {
            messengerDone(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            while (!stopped) {
                try {
                    Collection<SimpleSelectable> changed = selector.select();

                    for (SimpleSelectable m : changed) {
                        ListenerContainer listeners = null;

                        synchronized (this) {
                            listeners = inprogress.get(m.getIdentityReference());
                        }
                        if (listeners == null) {
                            m.unregister(selector);
                            continue;
                        }
                        listeners.process(m);
                    }
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
            }
        } catch (Throwable anyOther) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Uncaught Throwable in background thread", anyOther);
            }

            // There won't be any other thread. This thing is dead if that
            // happens. And it really shouldn't.
            synchronized (this) {
                stopped = true;
            }
        } finally {
            try {
                // It's only us now. Stopped is true.
                IOException failed = new IOException("Endpoint interface terminated");

                for (Map.Entry<IdentityReference, ListenerContainer> entry : inprogress.entrySet()) {
                    SimpleSelectable m = entry.getKey().getObject();
                    ListenerContainer listeners = entry.getValue();

                    m.unregister(selector);

                    if (listeners != null) {
                        listeners.giveUp(m, failed);
                    }
                }
                inprogress.clear();
            } catch (Throwable anyOther) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable while shutting down background thread", anyOther);
                }
            }
        }
    }
}
