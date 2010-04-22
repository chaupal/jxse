package net.jxta.impl.cm.srdi.bdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.impl.cm.CacheUtils;
import net.jxta.impl.cm.SrdiAPI;
import net.jxta.impl.cm.SrdiIndex.Entry;
import net.jxta.impl.cm.bdb.BerkeleyDbUtil;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.bind.tuple.TupleBinding;
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
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.util.IOExceptionWrapper;

public class BerkeleyDbSrdiIndexBackend implements SrdiAPI {

	private static final String DB_NAME = "mainIndex";
	private static final String PEER_SEARCH_DB_NAME = "peerSearch";
	private static final String EXPIRY_SEARCH_DB_NAME = "expirySearch";
	
	public static final long DEFAULT_GC_INTERVAL = TimeUtils.ANHOUR;
	private static final Logger LOG = Logger.getLogger(BerkeleyDbSrdiIndexBackend.class.getName());
	
	private PeerGroupID groupId;
	private String indexName;
	private Environment dbEnvironment;
	private Database db;
	private SecondaryDatabase peerSearchDb;
	private SecondaryDatabase expirySearchDb;
	
	private boolean open;
	
	public BerkeleyDbSrdiIndexBackend(PeerGroup group, String indexName) throws IOException {
		this(group, indexName, DEFAULT_GC_INTERVAL);
	}
	
	public BerkeleyDbSrdiIndexBackend(PeerGroup group, String indexName, long gcInterval) throws IOException {
		this(new File(group.getStoreHome()), group.getPeerGroupID(), indexName, gcInterval);
	}
	
	public BerkeleyDbSrdiIndexBackend(File storeHome, PeerGroupID groupId, String indexName) throws IOException {
		this(storeHome, groupId, indexName, DEFAULT_GC_INTERVAL);
	}
	
	public BerkeleyDbSrdiIndexBackend(File storeHome, PeerGroupID groupId, String indexName, long gcInterval) throws IOException {
		this.groupId = groupId;
		this.indexName = indexName;
		File srdiHome = createStoreDir(storeHome);
		
		try {
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			envConfig.setSharedCache(true);
			envConfig.setTransactional(false);
			dbEnvironment = new Environment(srdiHome, envConfig);
			
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setDeferredWrite(true);
			db = dbEnvironment.openDatabase(null, DB_NAME, dbConfig);
			
			SecondaryConfig peerSearchDbConfig = new SecondaryConfig();
			peerSearchDbConfig.setAllowCreate(true);
			peerSearchDbConfig.setAllowPopulate(true);
			peerSearchDbConfig.setSortedDuplicates(true);
			peerSearchDbConfig.setKeyCreator(new PeerSearchKeyCreator());
			
			peerSearchDb = dbEnvironment.openSecondaryDatabase(null, PEER_SEARCH_DB_NAME, db, peerSearchDbConfig);
			
			SecondaryConfig expirySearchDbConfig = new SecondaryConfig();
			expirySearchDbConfig.setAllowCreate(true);
			expirySearchDbConfig.setAllowPopulate(true);
			expirySearchDbConfig.setSortedDuplicates(true);
			expirySearchDbConfig.setKeyCreator(new ExpirySearchKeyCreator());
			
			expirySearchDb = dbEnvironment.openSecondaryDatabase(null, EXPIRY_SEARCH_DB_NAME, db, expirySearchDbConfig);
			
			garbageCollect();
			
			open = true;
		} catch(Exception e) {
			stop();
			throw new IOExceptionWrapper(e);
		}
	}

	private File createStoreDir(File storeHome) throws IOException {
		File srdiRootFile = new File(storeHome, "bdb_srdi_index");
		if(!srdiRootFile.exists()) {
			if(!srdiRootFile.mkdirs()) {
				throw new IOException("Failed to create directories for BDB SRDI Index at " + srdiRootFile.getAbsolutePath());
			}
		}
		
		if(!srdiRootFile.isDirectory()) {
			throw new IOException("Provided store root URI does not point to a directory: " + srdiRootFile.getAbsolutePath());
		}
		
		return srdiRootFile;
	}

	public static void clearSrdi(PeerGroup group) {
		try {
			BerkeleyDbSrdiIndexBackend backend = new BerkeleyDbSrdiIndexBackend(group, "CLEARALL");
			backend.clearAllIndices(group);
		} catch (IOException e) {
			Logging.logCheckedWarning(LOG, "Error occurred while clearing Srdi indices for group [" + group + "]\n", e);
		}
	}
	
	private void clearAllIndices(PeerGroup group) throws IOException {
		SrdiIndexKey searchKey = new SrdiIndexKey(group.getPeerGroupID());
		SrdiIndexKeyTupleBinding binding = new SrdiIndexKeyTupleBinding();
		processAllMatching(searchKey, db, binding, new DeleteMatchHandler<SrdiIndexKey>());
	}

	public void add(String primaryKey, String attribute, String value, PeerID pid, long expiration) throws IOException {
		DatabaseEntry key = new SrdiIndexKey(groupId, indexName, primaryKey, attribute, value, pid).toDatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		LongBinding.longToEntry(TimeUtils.toAbsoluteTimeMillis(expiration), data);
		try {
			db.put(null, key, data);
		} catch (DatabaseException e) {
			throw new IOExceptionWrapper(e);
		}
	}

	public void clear() throws IOException {
		SrdiIndexKey searchKey = new SrdiIndexKey(groupId, indexName);
		SrdiIndexKeyTupleBinding binding = new SrdiIndexKeyTupleBinding();
		processAllMatching(searchKey, db, binding, new DeleteMatchHandler<SrdiIndexKey>());
	}
	
	private <T> void processAllMatching(T prefix, Database database, TupleBinding<T> binding, MatchHandler<T> matchHandler) throws IOExceptionWrapper {
		Cursor c = null;
		try {
			c = database.openCursor(null, CursorConfig.READ_UNCOMMITTED);
			
			DatabaseEntry searchKey = new DatabaseEntry();
			binding.objectToEntry(prefix, searchKey);
			DatabaseEntry key = new DatabaseEntry();
			binding.objectToEntry(prefix, key);
			DatabaseEntry data = new DatabaseEntry();
			OperationStatus result = c.getSearchKeyRange(key, data, null);
			
			while(result == OperationStatus.SUCCESS) {
				if(!BerkeleyDbUtil.isPrefixOf(searchKey, key)) {
					break;
				}
				
				if(!matchHandler.handleMatch(c, binding.entryToObject(key), data)) {
					break;
				}
				
				result = c.getNext(key, data, null);
			}
		} catch (DatabaseException e) {
			throw new IOExceptionWrapper(e);
		} finally {
			closeCursor(c, "Failed to close cursor while clearing");
		}
	}
	
	private interface MatchHandler<T> {
		public boolean handleMatch(Cursor cursor, T matchingKey, DatabaseEntry matchingData) throws DatabaseException;
	}
	
	private class DeleteMatchHandler<T> implements MatchHandler<T> {

		public boolean handleMatch(Cursor cursor, T matchingKey, DatabaseEntry matchingData) throws DatabaseException {
			cursor.delete();
			return true;
		}
	}
	
	private abstract class QueryMatchHandler<T, U, V extends Collection<U>> implements MatchHandler<T> {
		
		protected V matches;
		
		public QueryMatchHandler(V matches) {
			this.matches = matches;
		}
		
		public abstract boolean handleMatch(Cursor cursor, T matchingKey, DatabaseEntry matchingData) throws DatabaseException;
	}
	
	private class ValueRegexMatchHandler extends QueryMatchHandler<SrdiIndexKey,PeerID, Set<PeerID>> {
		
		private String regex;
		private int threshold;
		
		public ValueRegexMatchHandler(Set<PeerID> matches, String regex, int threshold) {
			super(matches);
			this.regex = regex;
			this.threshold = threshold;
		}
		
		@Override
		public boolean handleMatch(Cursor cursor, SrdiIndexKey matchingKey,
				DatabaseEntry matchingData) throws DatabaseException {
			if(matchingKey.getValue().matches(regex)) {
				matches.add(matchingKey.getPeerId());
			}
			return matches.size() < threshold;
		}
	}
	
	private class AllMatchHandler extends QueryMatchHandler<SrdiIndexKey, PeerID, Set<PeerID>> {
		
		private int threshold;

		public AllMatchHandler(Set<PeerID> results, int threshold) {
			super(results);
			this.threshold = threshold;
		}
		
		public boolean handleMatch(Cursor cursor, SrdiIndexKey matchingKey, DatabaseEntry matchingData) throws DatabaseException {
			matches.add(matchingKey.getPeerId());
			return matches.size() < threshold;
		}
	}

	public void garbageCollect() throws IOException {
		Cursor c = null;
		try {
			c = expirySearchDb.openSecondaryCursor(null, CursorConfig.READ_UNCOMMITTED);
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			OperationStatus result = c.getFirst(key, data, null);
			while(result == OperationStatus.SUCCESS) {
				long expiryTime = LongBinding.entryToLong(key);
				if(expiryTime > TimeUtils.timeNow()) {
					break;
				}
				
				c.delete();
				result = c.getNext(key, data, null);
			}
		} catch (DatabaseException e) {
			throw new IOExceptionWrapper(e);
		} finally {
			closeCursor(c, "Failed to close cursor when garbage collecting");
		}
	}

	public List<Entry> getRecord(String primaryKey, String attribute, String value) throws IOException {
		LinkedList<Entry> results = new LinkedList<Entry>();
		
		Cursor cursor = null;
		try {
			cursor = db.openCursor(null, CursorConfig.READ_UNCOMMITTED);
			DatabaseEntry searchKey = new SrdiIndexKey(groupId, indexName, primaryKey, attribute, value, null).toDatabaseEntry();
			DatabaseEntry key = new SrdiIndexKey(groupId, indexName, primaryKey, attribute, value, null).toDatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();
			
			OperationStatus searchResult = cursor.getSearchKeyRange(key, data, null);
			while(searchResult == OperationStatus.SUCCESS) {
				if(!BerkeleyDbUtil.isPrefixOf(searchKey, key)) {
					break;
				}
				
				SrdiIndexKey matchingKey = SrdiIndexKey.fromDatabaseEntry(key);
				long expiry = LongBinding.entryToLong(data);
				results.add(new Entry(matchingKey.getPeerId(), expiry));
				
				searchResult = cursor.getNext(key, data, null);
			}
		} catch (DatabaseException e) {
			throw new IOExceptionWrapper(e);
		} finally {
			closeCursor(cursor, "Failed to close cursor when retrieving records");
		}
		
		return results;
	}

	public List<PeerID> query(String primaryKey, String attribute, String value, int threshold) throws IOException {
		Set<PeerID> results = new HashSet<PeerID>();
		SrdiIndexKey searchKey = new SrdiIndexKey(groupId, indexName, primaryKey, attribute, null, null);
		QueryMatchHandler<SrdiIndexKey, PeerID, Set<PeerID>> handler;
		if(value == null) {
			handler = new AllMatchHandler(results, threshold);
		} else {
			String regex = CacheUtils.convertValueQueryToRegex(value);
			handler = new ValueRegexMatchHandler(results, regex, threshold);
		}
		
		processAllMatching(searchKey, db, new SrdiIndexKeyTupleBinding(), handler);
		return new ArrayList<PeerID>(results);
	}

	public void remove(PeerID pid) throws IOException {
		PeerSearchKey searchCriteria = new PeerSearchKey(groupId, indexName, pid);
		PeerSearchKeyTupleBinding binding = new PeerSearchKeyTupleBinding();
		processAllMatching(searchCriteria, peerSearchDb, binding, new DeleteMatchHandler<PeerSearchKey>());
	}

	public void stop() {
		if(!open) {
			return;
		}
		
		
		closeDatabase(expirySearchDb, EXPIRY_SEARCH_DB_NAME);
		closeDatabase(peerSearchDb, PEER_SEARCH_DB_NAME);
		closeDatabase(db, DB_NAME);
		
		if(dbEnvironment != null) {
		    try {
		        dbEnvironment.close();
		    } catch(DatabaseException e) {
                        Logging.logCheckedSevere(LOG, "Failed to close SRDI index environment when stopping SRDI index " + indexName, e);
                    }
		}
		
		open = false;
	}

	private void closeCursor(Cursor c, String failMessage) {
		if(c != null) {
			try {
				c.close();
			} catch(DatabaseException e) {
				Logging.logCheckedWarning(LOG, failMessage, e);
			}
		}
	}
	
	private void closeDatabase(Database db, String name) {
		if(db != null) {
			try {
				db.close();
			} catch(DatabaseException e) {
				Logging.logCheckedSevere(LOG, "Failed to close BDB " + name
                                        + " when stopping SRDI index " + indexName, e);
			}
		}
	}

}
