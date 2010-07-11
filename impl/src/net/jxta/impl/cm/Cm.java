/*
 * Copyright (c) 2001-2009 Sun Microsystems, Inc.  All rights reserved.
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
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.StructuredTextDocument;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.logging.Logging;
import net.jxta.protocol.SrdiMessage.Entry;


/**
 * <p>
 * This class implements a limited document caching mechanism
 * intended to provide cache for services that have a need for cache
 * to search and exchange jxta documents.
 * </p>
 * <p>
 * Only Core Services are intended to use this mechanism.
 * </p>
 *
 * <p>
 * As of 2009-06-03, Cm is now a wrapper around an
 * implementation of the LegacyAdvertisementCache that is selected
 * at runtime. The original Cm implementation is available as
 * {@link XIndiceAdvertisementCache }.
 * </p>
 * 
 * <p>
 * To specify an alternative implementation to be used, you can currently
 * set the system property specified by {@link #CACHE_IMPL_SYSPROP} to
 * the full class name of your implementation, which must implement
 * {@link AdvertisementCache}.
 * </p>
 */
public final class Cm {

    public static final int NO_THRESHOLD = Integer.MAX_VALUE;
	public static final String CACHE_IMPL_SYSPROP = "net.jxta.impl.cm.cache.impl";

	private final static Logger LOG = Logger.getLogger(Cm.class.getName());
	
    /**
     * @deprecated use {@link XIndiceAdvertisementCache#DEFAULT_GC_MAX_INTERVAL } instead
     */
	@Deprecated
    public static final long DEFAULT_GC_MAX_INTERVAL = 1 * TimeUtils.ANHOUR;

    private AdvertisementCache wrappedImpl;

    public Cm(AdvertisementCache wrappedImpl) {
        this.wrappedImpl = wrappedImpl;
    }

    /**
     * Creates a Cm which wraps a {@link XIndiceAdvertisementCache } constructed with the
     * provided parameters.
     * 
	 * XXX I'm not sure how to support an arbitrary executor in this context.
	 * XXX Granted, this is only used in tests right now, but clearly we dont need
	 * XXX yet another pool floating around.  SingleThreadExecutor is used in the default impl.
     * @throws IOException 
     */
    public Cm(URI storeRoot, String areaName, TaskManager taskManager) throws IOException {
    	String cacheImpl = System.getProperty(CACHE_IMPL_SYSPROP);
    	if(cacheImpl == null) {
    		this.wrappedImpl = new XIndiceAdvertisementCache(storeRoot, areaName, taskManager);
    	} else {
    		try {
				Class<?> cacheClass = Class.forName(cacheImpl);
				Class<? extends AdvertisementCache> cacheClassChecked = cacheClass.asSubclass(AdvertisementCache.class);
				Constructor<? extends AdvertisementCache> constructor = cacheClassChecked.getConstructor(URI.class, String.class, TaskManager.class);
				this.wrappedImpl = constructor.newInstance(storeRoot, areaName, taskManager);
			} catch (Exception e) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Unable to construct cache type [" + cacheImpl + "] specified by system property, constructing default", e);
				}
				this.wrappedImpl = new XIndiceAdvertisementCache(storeRoot, areaName, taskManager);
			}
    	}
    }

    /**
     * Creates a Cm which wraps a {@link XIndiceAdvertisementCache } constructed with the
     * provided parameters.
     * @throws IOException 
     */
    
    public Cm(URI storeRoot, String areaName, TaskManager taskManager, long gcinterval, boolean trackDeltas) throws IOException {
    	String cacheImpl = System.getProperty(CACHE_IMPL_SYSPROP);
    	if(cacheImpl == null) {
    		this.wrappedImpl = new XIndiceAdvertisementCache(storeRoot, areaName, taskManager, gcinterval, trackDeltas);
    	} else {
    		try {
				Class<?> cacheClass = Class.forName(cacheImpl);
				Class<? extends AdvertisementCache> cacheClassChecked = cacheClass.asSubclass(AdvertisementCache.class);
				Constructor<? extends AdvertisementCache> constructor = cacheClassChecked.getConstructor(URI.class, String.class, TaskManager.class, long.class, boolean.class);
				this.wrappedImpl = constructor.newInstance(storeRoot, areaName, taskManager, gcinterval, trackDeltas);
			} catch (Exception e) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Unable to construct cache type [" + cacheImpl + "] specified by system property, constructing default", e);
				}
				this.wrappedImpl = new XIndiceAdvertisementCache(storeRoot, areaName, taskManager, gcinterval, trackDeltas);
			}
    	}
    }

    public List<Entry> getDeltas(String dn) {
        return wrappedImpl.getDeltas(dn);
    }

    public List<Entry> getEntries(String dn, boolean clearDeltas) {
        try {
            return wrappedImpl.getEntries(dn, clearDeltas);
        } catch(IOException e) {
            if(Logging.SHOW_WARNING || LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Exception occurred when getting entries for dn=[" + dn + "], clearDeltas=[" + clearDeltas + "]", e);
            }
            return new ArrayList<Entry>(0);
        }
    }

    public long getExpirationtime(String dn, String fn) {
        try {
			return wrappedImpl.getExpirationtime(dn, fn);
		} catch (IOException e) {
			if(Logging.SHOW_WARNING || LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Exception occurred when attempting to determine expiration time of dn=[" + dn + "], fn=[" + fn + "]", e);
			}
			return -1;
		}
    }

    public InputStream getInputStream(String dn, String fn) throws IOException {
        return wrappedImpl.getInputStream(dn, fn);
    }

    public long getLifetime(String dn, String fn) {
        try {
			return wrappedImpl.getLifetime(dn, fn);
		} catch (IOException e) {
			if(Logging.SHOW_WARNING || LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Exception occurred when attempting to determine lifetime of dn=[" + dn + "], fn=[" + fn + "]", e);
			}
			return -1;
		}
    }

    /**
     * Gets the list of all the files into the given folder
     *
     * @param dn          contains the name of the folder
     * @param threshold   the max number of results
     * @param expirations List to contain expirations
     * @return List Strings containing the name of the
     *         files
     */
    public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations) {
        return getRecords(dn, threshold, expirations, false);
    }

    public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations, boolean purge) {
        try {
			return wrappedImpl.getRecords(dn, threshold, expirations, purge);
		} catch (IOException e) {
			if(Logging.SHOW_WARNING || LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Exception occurred when to fetch records at dn=[" + dn + "]", e);
			}
			return new ArrayList<InputStream>(0);
		}
    }

    public void remove(String dn, String fn) throws IOException {
        wrappedImpl.remove(dn, fn);
    }

    public void save(String dn, String fn, Advertisement adv) throws IOException {
        save(dn, fn, adv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.NO_EXPIRATION);
    }

    public void save(String dn, String fn, Advertisement adv, long lifetime, long expiration) throws IOException {
        wrappedImpl.save(dn, fn, adv, lifetime, expiration);
    }

    public void save(String dn, String fn, byte[] data, long lifetime, long expiration) throws IOException {
        wrappedImpl.save(dn, fn, data, lifetime, expiration);
    }

    public List<InputStream> search(String dn, String attribute, String value, int threshold, List<Long> expirations) {
        try {
			return wrappedImpl.search(dn, attribute, value, threshold, expirations);
		} catch (IOException e) {
			return new ArrayList<InputStream>(0);
		}
    }

    public void setTrackDeltas(boolean trackDeltas) {
        wrappedImpl.setTrackDeltas(trackDeltas);
    }

    public void stop() {
        try {
			wrappedImpl.stop();
		} catch (IOException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Error occurred while stopped cache implementation", e);
			}
		}
    }

    public void garbageCollect() {
        try {
			wrappedImpl.garbageCollect();
		} catch (IOException e) {
			if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
				LOG.log(Level.WARNING, "Error occurred while garbage collecting", e);
			}
		}
    }

    /**
     * @deprecated use {@link XIndiceAdvertisementCache#createTmpName(net.jxta.document.StructuredTextDocument)  }
     * directly instead.
     */
    @Deprecated
    public static String createTmpName(StructuredTextDocument<?> doc) {
        return XIndiceAdvertisementCache.createTmpName(doc);
    }

    /**
     * @deprecated use {@link XIndiceAdvertisementCache#getIndexQuery(java.lang.String) }
     * directly instead.
     */
    @Deprecated
    public static IndexQuery getIndexQuery(String value) {
        return XIndiceAdvertisementCache.getIndexQuery(value);
    }

	public String getImplClassName() {
		return wrappedImpl.getClass().getName();
	}
}
