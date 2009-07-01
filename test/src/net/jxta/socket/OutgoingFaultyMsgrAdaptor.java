/*
 * Copyright (c) 2003-2007 Sun Microsystems, Inc.  All rights reserved.
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
package net.jxta.socket;


import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.impl.util.TimeUtils;

import java.util.logging.Level;
import net.jxta.impl.util.pipe.reliable.*;
import net.jxta.logging.Logging;
import java.util.logging.Logger;


/**
 *  An Outgoing Messenger adpator that implements synthetic faults. The faults
 *  include dropping messages and delaying messages.
 */
public class OutgoingFaultyMsgrAdaptor implements Outgoing {    
    private final static transient Logger LOG = Logger.getLogger(OutgoingMsgrAdaptor.class.getName());
    
    private final double MESSAGE_LOSS_PROBABILITY;
    
    private final double MESSAGE_DELAY_RATIO;
    
    private final Timer delayMessageTimer;
    
    private final Random rand = new Random();
    
    private final Messenger msgr;
    private int timeout;
    private long lastAccessed = 0;
    private boolean closed = false;
    
    /**
     *  Constructor for the OutgoingMsgrAdaptor object
     *
     *  @param  msgr     the messenger used to send messages
     *  @param  timeout  timeout in milliseconds
     *  @param  lose  The probability between 0 and 1.0 of losing a message.
     *  @param  delayRatio Messages are delayed with a gaussian distribution up
     *  to {@code delayRatio * timeout}.
     */
    public OutgoingFaultyMsgrAdaptor(Messenger msgr, int timeout, double lose, double delayRatio) {
        if (msgr == null) {
            throw new IllegalArgumentException("messenger cannot be null");
        }
        this.msgr = msgr;
        this.timeout = timeout;
        this.MESSAGE_LOSS_PROBABILITY = lose;
        this.MESSAGE_DELAY_RATIO = delayRatio;
        
        delayMessageTimer = new Timer("Faulty message delayer for " + msgr, true);
        
        // initialize to some reasonable value
        lastAccessed = TimeUtils.timeNow();
    }
    
    /**
     *  {@inheritDoc}
     */
    protected void finalizer() throws Exception {
        delayMessageTimer.cancel();
    }
    
    /**
     *  returns last accessed time as a string
     *
     *@return    last accessed time as a string
     */
    @Override
    public String toString() {
        return " lastAccessed=" + Long.toString(lastAccessed);
    }
    
    /**
     *  Sets the Timeout attribute. A timeout of 0 blocks forever
     *
     * @param  timeout The new soTimeout value
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    /**
     *  close the messenger (does not close the messenger)
     */
    public void close() {
        closed = true;
    }
    
    /**
     *  Gets the minIdleReconnectTime of the OutgoingMsgrAdaptor
     *  (obsolete).
     *@return    The minIdleReconnectTime value
     */
    public long getMinIdleReconnectTime() {
        return timeout;
    }
    
    /**
     *  Gets the idleTimeout of the OutgoingMsgrAdaptor. The adaptor never times out.
     *@return    <code>Long.MAX_VALUE</code>
     */
    public long getIdleTimeout() {
        return Long.MAX_VALUE;
    }
    
    /**
     *  Gets the maxRetryAge attribute of the OutgoingMsgrAdaptor
     *
     *@return    The maxRetryAge value
     */
    public long getMaxRetryAge() {
        return timeout == 0 ? Long.MAX_VALUE : timeout;
    }
    
    /**
     *  Gets the lastAccessed time of OutgoingMsgrAdaptor
     *
     *@return    The lastAccessed in milliseconds
     */
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    /**
     *  Sets the lastAccessed of OutgoingMsgrAdaptor
     *
     *@param  time  The new lastAccessed in milliseconds
     */
    public void setLastAccessed(long time) {
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting lastAccessed to :" + lastAccessed);
        }
        lastAccessed = time;
    }
    
    /**
     *  Sends a message
     *
     *@param  msg              message to send
     *@return                  true if message send is successfull
     *@exception  IOException  if an io error occurs
     */
    public synchronized boolean send(Message msg) throws IOException {
        if (closed) {
            throw new IOException("broken connection");
        }
        
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Sending a Message");
        }
        
        if (rand.nextDouble() < MESSAGE_LOSS_PROBABILITY) {
            // we sent it. Really! we sent it!....
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Losing " + msg);
            }
            return true;
        }
        
        long delay = (long) (Math.abs(rand.nextGaussian()) * MESSAGE_DELAY_RATIO * timeout);
        
        if (delay > 50) {
            delayMessageTimer.schedule(new DelayTask(msg), delay);
            
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Delaying " + msg + " for " + delay + "ms.");
            }
            
            return true;
        } else {        
            return msgr.sendMessage(msg, null, null);
        }
    }
    
    private class DelayTask extends TimerTask {        
        private final Message msg;
        
        public DelayTask(Message msg) {
            this.msg = msg;
        }
        
        public void run() {
            try {
                msgr.sendMessageB(msg, null, null);
            } catch (IOException ignored) {}
        }
    }
}

