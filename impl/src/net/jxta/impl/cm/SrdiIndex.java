/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.cm;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.impl.xindice.core.data.Key;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;

/**
 * Searchable store of records of all known peers who have broadcast advertisements that have not yet
 * expired.
 * <p>
 * Internally, this is a wrapper around an {@link net.jxta.impl.cm.SrdiIndexBackend} selected using the system property
 * {@link #SRDI_INDEX_BACKEND_SYSPROP}. If no backend is specified through this system property, the default
 * implementation specified by {@link #DEFAULT_SRDI_INDEX_BACKEND} is used.
 */
public class SrdiIndex implements SrdiIndexBackend {
    
	public static final String SRDI_INDEX_BACKEND_SYSPROP = "net.jxta.impl.cm.SrdiIndex.backend.impl";
	public static final String DEFAULT_SRDI_INDEX_BACKEND = "net.jxta.impl.cm.XIndiceSrdiIndexBackend";
	
	public static final long DEFAULT_GC_INTERVAL = 10 * TimeUtils.AMINUTE;
	public static final long NO_AUTO_GC = -1;
	
    private final static transient Logger LOG = Logger.getLogger(SrdiIndex.class.getName());
    
    private SrdiIndexBackend backend;
    private ScheduledFuture<?> gcHandle;
    
    /**
     * Constructor for the SrdiIndex
     *
     * @param group     group
     * @param indexName the index name
     */
    public SrdiIndex(PeerGroup group, String indexName) {
    	this(group, indexName, DEFAULT_GC_INTERVAL);
    }
    
    /**
     * Construct a SrdiIndex and starts a GC thread which runs every "interval"
     * milliseconds
     *
     * @param interval  the interval at which the gc will run in milliseconds
     * @param group     group context
     * @param indexName SrdiIndex name
     */
    
    public SrdiIndex(PeerGroup group, String indexName, long interval) {

        String backendClassName = System.getProperty(SRDI_INDEX_BACKEND_SYSPROP, DEFAULT_SRDI_INDEX_BACKEND);
    	createBackend(backendClassName, group, indexName);

        Logging.logCheckedInfo(LOG, "[" + ((group == null) ? "none" : group.toString()) + "] : Starting SRDI GC Thread for " + indexName);
        
    	startGC(interval);
    	// FIXME?  The scheduledexecutor doesn't give the ability to name threads...

    }

    private void startGC(long interval) {
        if(interval <= 0) {
            // automatic gc disabled
            return;
        }
        
        ScheduledExecutorService executor = TaskManager.getTaskManager().getScheduledExecutorService();
        gcHandle = executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                garbageCollect();
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void createBackend(String backendClassName, PeerGroup group, String indexName) {
        
        try {

            Class<? extends SrdiIndexBackend> backendClass = getBackendClass();
            Constructor<? extends SrdiIndexBackend> constructor = backendClass.getConstructor(PeerGroup.class, String.class);
            backend = (SrdiIndexBackend) constructor.newInstance(group, indexName);

        } catch (Exception e) {

            Logging.logCheckedSevere(LOG, "Unable to construct SRDI Index backend [" + backendClassName + "] specified by system property, constructing default", e);
            backend = new XIndiceSrdiIndexBackend(group, indexName);

        }
    }
    
    public SrdiIndex(SrdiIndexBackend backend, long gcInterval) {
    	this.backend = backend;
    	startGC(gcInterval);
    }
    
    protected String getBackendClassName() {
    	return backend.getClass().getName();
    }
    
    protected SrdiIndexBackend getBackend() {
    	return backend;
    }
    
    /**
     * add an index entry
     *
     * @param primaryKey primary key
     * @param attribute  Attribute String to query on
     * @param value      value of the attribute string
     * @param expiration expiration associated with this entry relative time in
     *                   milliseconds
     * @param pid        peerid reference
     * @throws IOException if an error occurred storing the entry
     */
    public synchronized void add(String primaryKey, String attribute, String value, PeerID pid, long expiration) {
    	
        try {
            backend.add(primaryKey, attribute, value, pid, expiration);
    	} catch(IOException e) {
    	    Logging.logCheckedWarning(LOG, "Failed to write entry to backend", e);
    	}

    }
    
    /**
     * retrieves a record
     *
     * @param pkey  primary key
     * @param skey  secondary key
     * @param value value
     * @return List of Entry objects
     */
    public List<Entry> getRecord(String pkey, String skey, String value) {
    	
        try {

    	    return backend.getRecord(pkey, skey, value);

    	} catch(IOException e) {

    	    Logging.logCheckedWarning(LOG, "Failed to retrieve record from backend", e);
    	    return new LinkedList<Entry>();

    	}
        
    }
    
    /**
     * remove entries pointing to peer id from cache
     *
     * @param pid peer id to remove
     */
    public synchronized void remove(PeerID pid) {
    	
        try {

            backend.remove(pid);

    	} catch(IOException e) {

    	    Logging.logCheckedWarning(LOG, "Failed to remove record from backend", e);

    	}

    }
    
    /**
     * Query SrdiIndex
     *
     * @param attribute Attribute String to query on
     * @param value     value of the attribute string
     * @return an enumeration of canonical paths
     * @param primaryKey primary key
     * @param threshold max number of results
     */
    public synchronized List<PeerID> query(String primaryKey, String attribute, String value, int threshold) {

        try {
            
            return backend.query(primaryKey, attribute, value, threshold);

    	} catch(IOException e) {

    	    Logging.logCheckedWarning(LOG, "Failed to query backend for pk=[" + primaryKey + "], attr=[" + attribute + "], val=[" + value + "], thresh=[" + threshold + "]", e);
  	    return new LinkedList<PeerID>();

    	}

    }
    
    /**
     * Garbage Collect expired entries
     */
    public void garbageCollect() {
    	
        try {

    	    backend.garbageCollect();

    	} catch(IOException e) {

     	    Logging.logCheckedWarning(LOG, "Failed to garbage collect backend", e);
     		
     	}

    }
    
    public void clear() {

        try {
    		
            backend.clear();

    	} catch(IOException e) {

     	    Logging.logCheckedWarning(LOG, "Failed to clear backend", e);
     		
     	}
    }
    
    /**
     * stop the current running thread
     */
    public synchronized void stop() {
        if(gcHandle != null) {
            gcHandle.cancel(false);
        }
        
    	backend.stop();
    }
    
    /**
     * Flushes the Srdi directory for a specified group
     * this method should only be called before initialization of a given group
     * calling this method on a running group would have undefined results
     *
     * @param group group context
     */
    public static void clearSrdi(PeerGroup group) {
        
        Logging.logCheckedInfo(LOG, "Clearing SRDI for " + group.getPeerGroupName());
        
        Class<? extends SrdiIndexBackend> backendClass = getBackendClass();
        
        try {

	    backendClass.getMethod("clearSrdi", PeerGroup.class).invoke(null, group);

        } catch (Exception e) {

            Logging.logCheckedWarning(LOG, "Failed to clear Srdi cache for peer group " + group.getPeerGroupName(), e);

	}

    }
    
    /**
     * Loads and checks the backend class specified by {@link #SRDI_INDEX_BACKEND_SYSPROP} conforms to
     * the specification of SrdiIndex, and provides the appropriate constructors and static methods.
     */
    private static Class<? extends SrdiIndexBackend> getBackendClass() {

        String backendClassName = System.getProperty(SRDI_INDEX_BACKEND_SYSPROP, DEFAULT_SRDI_INDEX_BACKEND);
    	Class<?> backendClass;

    	try {

    	    backendClass = Class.forName(backendClassName);

    	} catch(ClassNotFoundException e) {

    	    Logging.logCheckedSevere(LOG, "Class specified for use as backend could not be found", e);
  	    return getDefaultBackendClass();

    	}
    	
    	Class<? extends SrdiIndexBackend> backendClassChecked;

            try {

                backendClassChecked = backendClass.asSubclass(SrdiIndexBackend.class);

            } catch (ClassCastException e) {

                 Logging.logCheckedSevere(LOG, "Class specified for use as backend does not implement SrdiIndexBackend", e);
                 return getDefaultBackendClass();

            }

            try {

                backendClassChecked.getConstructor(PeerGroup.class, String.class);

            } catch (Exception e) {

                Logging.logCheckedSevere(LOG, "Class specified for use as backend does not provide accessible constructor which takes PeerGroup and String as parameters", e);
                return getDefaultBackendClass();

            }

            try {

                Method method = backendClassChecked.getMethod("clearSrdi", PeerGroup.class);

                if((method.getModifiers() & Modifier.STATIC) == 0) {

                    Logging.logCheckedSevere(LOG, "Class specified for use as backend does not provide method clearSrdi as a static");

                    return getDefaultBackendClass();

                }

            } catch(Exception e) {

                 Logging.logCheckedSevere(LOG, "Class specified for use as backend does not provide accessible method clearSrdi which takes a PeerGroup", e);
                 return getDefaultBackendClass();

            }

            return backendClassChecked;
    }
    
    private static Class<? extends SrdiIndexBackend> getDefaultBackendClass() {
    	
        try {

	    return Class.forName(DEFAULT_SRDI_INDEX_BACKEND).asSubclass(SrdiIndexBackend.class);

	} catch (ClassNotFoundException e) {

	    Logging.logCheckedSevere(LOG, "Could not load default backend for SrdiIndex", e);
	    throw new RuntimeException("Could not load default backend for SrdiIndex", e);

        }

    }
    
    /**
     * An entry in the index tables.
     */
    public final static class Entry {
        
        public final PeerID peerid;
        public final long expiration;
        
        /**
         * Peer Pointer reference
         *
         * @param peerid     PeerID for this entry
         * @param expiration the expiration for this entry
         */
        public Entry(PeerID peerid, long expiration) {
            this.peerid = peerid;
            this.expiration = expiration;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Entry && (peerid.equals(((Entry) obj).peerid));
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return peerid.hashCode();
        }
        
        /**
         *  Return the absolute time in milliseconds at which this entry will
         *  expire.
         *
         *  @return The absolute time in milliseconds at which this entry will
         *  expire.
         */
        public long getExpiration() {
            return expiration;
        }
        
        /**
         *  Return {@code true} if this entry is expired.
         *
         *  @return {@code true} if this entry is expired otherwise {@code false}.
         */
        public boolean isExpired() {
            return TimeUtils.timeNow() > expiration;
        }
    }
    
    
    /**
     * an SrdiIndexRecord wrapper
     */
    public final static class SrdiIndexRecord {
        
        public final Key key;
        public final List<Entry> list;
        
        /**
         * SrdiIndex record
         *
         * @param key  record key
         * @param list record entries
         */
        public SrdiIndexRecord(Key key,List<Entry> list) {
            this.key = key;
            this.list = list;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof SrdiIndexRecord && (key.equals(((SrdiIndexRecord) obj).key));
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}

