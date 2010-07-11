
package net.jxta.impl.cm.bdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.impl.cm.AdvertisementCache;
import net.jxta.impl.cm.CacheUtils;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.logging.Logging;
import net.jxta.protocol.SrdiMessage.Entry;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.util.IOExceptionWrapper;

/**
 * Berkeley DB JE based implementation of the {@link AdvertisementCache} interface.
 * Intended as a drop-in replacement for the existing XIndice based implementation,
 * to keep the number of file handles used down to a manageable level.
 * 
 * <p><em>NOTE</em>: Usage of Berkeley DB requires either a commercial license from
 * Oracle or acceptance of Oracle's open source license, which has a GPL-like clause.
 */
public class BerkeleyDbAdvertisementCache implements AdvertisementCache {
	
	/*
	 * IMPLEMENTATION NOTES (2009/06/11): A single primary database is used which maps
	 * keys of the form areaName/dn/fn to AdvertisementDbRecord instances. Two secondary
	 * databases are linked to this: one for searching the database by indexable advertisement
	 * attributes, the other for searching for expired advertisements.
	 */

	private static final Logger LOG = Logger.getLogger(BerkeleyDbAdvertisementCache.class.getName());
	private static final long CLEAN_INTERVAL = 30000L;
	
	private String areaName;

	private Environment dbEnvironment;
	private Database db;
	private SecondaryDatabase attrSearchDb;
	private SecondaryDatabase expiryTimeDb;

	private boolean trackDeltas;

	private Map<String,List<Entry>> deltas = new HashMap<String, List<Entry>>();
	
	private TimerTask cleaner;
	private int expiryCount = 0;
	
    public BerkeleyDbAdvertisementCache(URI storeRoot, String areaName, TaskManager taskManager, long gcinterval, boolean trackDeltas) throws IOException {
    	this(storeRoot, areaName, taskManager);
    	this.trackDeltas = trackDeltas;
    }

	public BerkeleyDbAdvertisementCache(URI storeRoot, String areaName, TaskManager taskManager) throws IOException {
		this(storeRoot, areaName, taskManager, true);
	}
	
	public BerkeleyDbAdvertisementCache(URI storeRoot, String areaName, TaskManager taskManager, boolean enablePeriodicClean) throws IOException {
		if(Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Creating BDB cache within [" + storeRoot.toString() + "], areaName = [" + areaName + "]");
		}
		this.areaName = areaName;

		File dbHome = createStoreRoot(storeRoot);
		
		try {
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			envConfig.setSharedCache(true);
			envConfig.setTransactional(false);
			dbEnvironment = new Environment(dbHome, envConfig);
			
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setDeferredWrite(true);
			db = dbEnvironment.openDatabase(null, "cache", dbConfig);
			
			SecondaryConfig secDbConfig = new SecondaryConfig();
			secDbConfig.setMultiKeyCreator(new AdvertisementIndexableKeyCreator());
			secDbConfig.setAllowCreate(true);
			secDbConfig.setAllowPopulate(true);
			secDbConfig.setSortedDuplicates(true);
			attrSearchDb = dbEnvironment.openSecondaryDatabase(null, "cache_indexables", db, secDbConfig);
			
			SecondaryConfig expiryDbConfig = new SecondaryConfig();
			expiryDbConfig.setKeyCreator(new ExpiryKeyCreator());
			expiryDbConfig.setAllowCreate(true);
			expiryDbConfig.setAllowPopulate(true);
			expiryDbConfig.setSortedDuplicates(true);
			expiryTimeDb = dbEnvironment.openSecondaryDatabase(null, "cache_expiry", db, expiryDbConfig);
			
			garbageCollect();
			
			if(enablePeriodicClean) {
				LOG.fine("Automatic clean-up of cache enabled, starting cleaner task");
				cleaner = new CleanerTask(this);
				taskManager.getScheduledExecutorService().scheduleAtFixedRate(cleaner, CLEAN_INTERVAL, CLEAN_INTERVAL, TimeUnit.MILLISECONDS);
			}
		} catch(Exception e) {
			IOException wrapper = new IOException("Error occurred while initialising Bdb for use");
			wrapper.initCause(e);
			emergencyShutdown();
			throw wrapper;
		}
	}

	private File createStoreRoot(URI storeRoot) throws IOException {
		File storeRootFile = new File(storeRoot);
		if(!storeRootFile.exists()) {
			if(!storeRootFile.mkdirs()) {
				throw new IOException("Failed to create directories for BDB advertisement cache at " + storeRootFile.getAbsolutePath());
			}
		}
		
		if(!storeRootFile.isDirectory()) {
			throw new IOException("Provided store root URI does not point to a directory: " + storeRootFile.getAbsolutePath());
		}
		
		return storeRootFile;
	}

	/**
	 * Will attempt to close all resources and log any exceptions as SEVERE, as we are typically
	 * in an irrecoverable situation at this point
	 */
	private void emergencyShutdown() {
		if(expiryTimeDb != null) {
			try {
				expiryTimeDb.close();
			} catch (DatabaseException e1) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to close expiry time secondary db when recovering from failed construction", e1);
				}
			}
		}
		
		if(attrSearchDb != null) {
			try {
				attrSearchDb.close();
			} catch (DatabaseException e1) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to close attribute index secondary db when recovering from failed construction", e1);
				}
			}
		}
		
		if(db != null) {
			try {
				db.close();
			} catch (DatabaseException e1) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to close main db when recovering from failed construction", e1);
				}
			}
		}
		
		if(dbEnvironment != null) {
			try {
				dbEnvironment.close();
			} catch (DatabaseException e1) {
				if(Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
					LOG.log(Level.SEVERE, "Failed to close environment when recovering from failed construction", e1);
				}
			}
		}
	}
	
    public List<Entry> getDeltas(String dn) {
        List<Entry> currentDeltas = deltas.get(dn);
        if(currentDeltas == null) {
            currentDeltas = new ArrayList<Entry>(0);
        }
        clearDeltas(dn);
        return currentDeltas;
    }

    private void clearDeltas(String dn) {
        deltas.remove(dn);
    }

    public List<Entry> getEntries(String dn, boolean clearDeltas) throws IOException {
        LinkedList<Entry> entries = new LinkedList<Entry>();
        Cursor c = null;
        try {
            c = attrSearchDb.openCursor(null, CursorConfig.READ_UNCOMMITTED);
            
            AttributeSearchKey searchKey = new AttributeSearchKey(areaName, dn);
            DatabaseEntry searchPrefix = searchKey.toDatabaseEntry();
            DatabaseEntry key = searchKey.toDatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            OperationStatus result = c.getSearchKeyRange(key, data, null);
            while(result == OperationStatus.SUCCESS) {
                if(!BerkeleyDbUtil.isPrefixOf(searchPrefix, key)) {
                    break;
                }
                
                AttributeSearchKey matchingKey = AttributeSearchKey.fromDatabaseEntry(key);
                AdvertisementDbRecord record = AdvertisementDbRecord.fromDataEntry(data);
                entries.add(new Entry(matchingKey.getAttributeName(), matchingKey.getValue(), TimeUtils.toRelativeTimeMillis(record.lifetime)));
                
                result = c.getNext(key, data, null);
            }
        } catch (DatabaseException e) {
            throw new IOExceptionWrapper(e);
        } finally {
            closeCursor(c);
        }
        
        if(clearDeltas) {
            clearDeltas(dn);
        }
        
        return entries;
    }

    public long getExpirationtime(String dn, String fn) throws IOException {
    	AdvertisementDbRecord record = getRecord(dn, fn);
    	if(record == null) {
    		return -1;
    	}
    	
    	return record.getExpirationTime();
    }

	public InputStream getInputStream(String dn, String fn) throws IOException {
        AdvertisementDbRecord r = getRecord(dn, fn);
        if(r == null) {
        	return null;
        }
        
    	return new ByteArrayInputStream(r.getData());
    }
    
    private AdvertisementDbRecord getRecord(String dn, String fn) throws IOException {
    	DatabaseEntry result = new DatabaseEntry();
    	try {
    		OperationStatus operationStatus = db.get(null, new AdvertisementKey(areaName, dn, fn).toDatabaseEntry(), result, null);
    		if(operationStatus != OperationStatus.SUCCESS) {
    			return null;
    		}
    		return AdvertisementDbRecord.fromDataEntry(result);
	    } catch (DatabaseException e) {
			IOException wrapper = new IOException("Unable to fetch data for dn=[" + dn + "], fn=[" + fn + "]");
			wrapper.initCause(e);
			throw wrapper;
		}
    }

    public long getLifetime(String dn, String fn) throws IOException {
    	AdvertisementDbRecord record = getRecord(dn, fn);
    	if(record == null) {
    		return -1;
    	}
        
    	// TODO: remove dead record if TTL < 0
    	
    	return TimeUtils.toRelativeTimeMillis(record.lifetime);
    }

    public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations, boolean purge) throws IOException {
    	if(dn == null) {
    		return new ArrayList<InputStream>(0);
    	}
    	
    	Cursor cursor = null;
		try {
			LinkedList<InputStream> results = new LinkedList<InputStream>();
			
			cursor = db.openCursor(null, null);
			AdvertisementKey searchKey = new AdvertisementKey(areaName, dn);
			DatabaseEntry searchKeyEntry = searchKey.toDatabaseEntry();
            DatabaseEntry matchingKey = searchKey.toDatabaseEntry();
			DatabaseEntry matchingData = new DatabaseEntry();
			OperationStatus searchStatus = cursor.getSearchKeyRange(matchingKey, matchingData, null);
			
			while(searchStatus != OperationStatus.NOTFOUND && results.size() < threshold) {
			    if(!BerkeleyDbUtil.isPrefixOf(searchKeyEntry, matchingKey)) {
			        break;
			    }
				
				AdvertisementDbRecord record = AdvertisementDbRecord.fromDataEntry(matchingData);
				results.add(new ByteArrayInputStream(record.getData()));
				if(expirations != null) {
					expirations.add(record.getExpirationTime());
				}
				
				searchStatus = cursor.getNext(matchingKey, matchingData, null);
			}
			
			return results;
		} catch (DatabaseException e) {
			IOException wrapped = new IOException("Error occurred when retrieving all records at dn=[" + dn + "]");
			wrapped.initCause(e);
			throw wrapped;
		} finally {
		    closeCursor(cursor);
		}
    }



	public void remove(String dn, String fn) throws IOException {
        try {
        	if(trackDeltas) {
	        	AdvertisementDbRecord record = getRecord(dn, fn);
	        	if(record != null) {
	        		XMLDocument<?> doc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new ByteArrayInputStream(record.getData()));
	                Advertisement adv = AdvertisementFactory.newAdvertisement(doc);
	                generateDeltas(dn, adv, doc, record.lifetime);
	        	}
        	}
			db.delete(null, new AdvertisementKey(areaName, dn, fn).toDatabaseEntry());
		} catch (DatabaseException e) {
			IOException wrapper = new IOException("Unable to delete data at dn=[" + dn + "], fn=[" + fn + "]");
			wrapper.initCause(e);
			throw wrapper;
		}
    }

    public void save(String dn, String fn, Advertisement adv, long lifetime, long expiration) throws IOException {
    	checkLegalExpirationAndLifetime(lifetime, expiration);
    	AdvertisementDbRecord record = new AdvertisementDbRecord(adv, lifetime, expiration);
    	putRecord(dn, fn, record);
    	generateDeltas(dn, adv, null, expiration);
    }

	private void generateDeltas(String dn, Advertisement adv, StructuredDocument<?> doc, long expiry) {
		if(!trackDeltas || expiry <= 0) {
			return;
		}
		
		if(doc == null) {
			doc = (StructuredDocument<?>)adv.getDocument(MimeMediaType.XMLUTF8);
		}
		
		Map<String, String> indexFields = CacheUtils.getIndexfields(adv.getIndexFields(), doc);
		List<Entry> deltasForDn = deltas.get(dn);
		if(deltasForDn == null) {
		    deltasForDn = new LinkedList<Entry>();
		}
		
		for(String indexField : indexFields.keySet()) {
			deltasForDn.add(new Entry(indexField, indexFields.get(indexField), expiry));
		}
		
		deltas.put(dn, deltasForDn);
	}

    public void save(String dn, String fn, byte[] data, long lifetime, long expiration) throws IOException {
    	checkLegalExpirationAndLifetime(lifetime, expiration);
    	AdvertisementDbRecord record = new AdvertisementDbRecord(data, TimeUtils.toAbsoluteTimeMillis(lifetime), expiration, false);
    	putRecord(dn, fn, record);
    }

	private void putRecord(String dn, String fn, AdvertisementDbRecord record) throws IOException {
		try {
    		AdvertisementDbRecord oldRecord = getRecord(dn, fn);
    		DatabaseEntry key = new AdvertisementKey(areaName, dn, fn).toDatabaseEntry();
			if(oldRecord != null) {
    			// ensure lifetime cannot be replaced with a lower value than it previously had
    			record.lifetime = Math.max(record.lifetime, oldRecord.lifetime);
    			db.delete(null, key);
    		}
			db.put(null, key, record.toDataEntry());
		} catch (DatabaseException e) {
			IOException wrapper = new IOException("failed to write data to BDB: ");
			wrapper.initCause(e);
			throw wrapper;
		}
	}

	private void checkLegalExpirationAndLifetime(long lifetime, long expiration) {
		if(lifetime <= 0 || expiration < 0) {
    		throw new IllegalArgumentException("Bad expiration or lifetime.");
    	}
	}

    public List<InputStream> search(String dn, String attribute, String value, int threshold, List<Long> expirations) throws IOException {
    	LinkedList<InputStream> results = new LinkedList<InputStream>();
    	
    	String regexToMatch = CacheUtils.convertValueQueryToRegex(value);
    	SecondaryCursor cursor = null;
    	try {
			cursor = attrSearchDb.openSecondaryCursor(null, CursorConfig.READ_UNCOMMITTED);
			AttributeSearchKey searchKey = new AttributeSearchKey(areaName, dn, attribute);
			DatabaseEntry matchPrefix = searchKey.toDatabaseEntry();
			DatabaseEntry key = searchKey.toDatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			OperationStatus searchResult = cursor.getSearchKeyRange(key, data, null);
			
			while(searchResult == OperationStatus.SUCCESS && results.size() < threshold) {
			    if(!BerkeleyDbUtil.isPrefixOf(matchPrefix, key)) {
			        break;
			    }
				
			    AttributeSearchKey matchingKey = AttributeSearchKey.fromDatabaseEntry(key);
				String valueForAttribute = matchingKey.getValue();
				if(valueForAttribute.matches(regexToMatch)) {
					AdvertisementDbRecord record = AdvertisementDbRecord.fromDataEntry(data);
					
					// stale advertisements will be removed in the next GC cycle
					if(record.getExpirationTime() > 0) {
						results.add(new ByteArrayInputStream(record.getData()));
						if(expirations != null) {
							expirations.add(record.getExpirationTime());
						}
					}
				}
				
				searchResult = cursor.getNext(key, data, null);
			}
		} catch (DatabaseException e) {
			IOException wrapper = new IOException("Error occurred while searching database for advertisements");
			wrapper.initCause(e);
			throw wrapper;
		} finally {
		    closeCursor(cursor);
		}
        return results;
    }

    

	public void setTrackDeltas(boolean trackDeltas) {
		this.trackDeltas = trackDeltas;
    }

    public void stop() throws IOException {
    	try {
    		if(cleaner != null) {
    			cleaner.cancel();
    		}
    		expiryTimeDb.close();
    		attrSearchDb.close();
    		db.close();
    		dbEnvironment.close();
    	} catch(DatabaseException e) {
    		IOException wrapper = new IOException("Error occurred while trying to stop berkeley DB environment");
    		wrapper.initCause(e);
    		throw wrapper;
    	}
    }

    public void garbageCollect() throws IOException {
    	SecondaryCursor expiryCursor = null;
    	try {
			expiryCursor = expiryTimeDb.openSecondaryCursor(null, CursorConfig.READ_UNCOMMITTED);
			DatabaseEntry timeKey = new DatabaseEntry();
			DatabaseEntry matchingData = new DatabaseEntry();
			OperationStatus searchResult = expiryCursor.getFirst(timeKey, matchingData, null);
			while(searchResult == OperationStatus.SUCCESS) {
				if(BerkeleyDbUtil.getTimeFromExpiryKeyEntry(timeKey) > TimeUtils.timeNow()) {
					break;
				}
				expiryCursor.delete();
				expiryCount++;
				searchResult = expiryCursor.getNext(timeKey, matchingData, null);
			}
			
		} catch (DatabaseException e) {
			IOException wrapper = new IOException("Error occurred while trying to clean out expired entries");
			wrapper.initCause(e);
			throw wrapper;
		} finally {
		    closeCursor(expiryCursor);
		}
    }
    
    /**
     * Return the number of entries that have expired and been flushed from the DB since the last call to this method
     * @return
     */
    protected int getExpiryCount() {
    	int expired = expiryCount;
    	expiryCount = 0;
    	return expired;
    }
    
    private void closeCursor(Cursor c) {
        if(c != null) {
            try {
                c.close();
            } catch(DatabaseException e) {
                if(Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    
                }
            }
        }
    }
}
