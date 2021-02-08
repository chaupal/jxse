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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.jxta.id.IDFactory;
import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Record;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.filer.BTreeCallback;
import net.jxta.impl.xindice.core.filer.BTreeFiler;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.impl.xindice.core.indexer.NameIndexer;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;

/**
 * Srdi
 */
public class XIndiceSrdi implements SrdiAPI {

    /**
     * Logger
     */
    private final static transient Logger LOG = Logger.getLogger(XIndiceSrdi.class.getName());

    private volatile boolean stop = false;
    private final XIndiceIndexer srdiIndexer;
    private final BTreeFiler cacheDB;
    private final Set<PeerID> gcPeerTBL = new HashSet<PeerID>();

    private final String indexName;

    /**
     * Constructor for the Srdi
     *
     * @param group     group
     * @param indexName the index name
     */
    public XIndiceSrdi(PeerGroup group, String indexName) {

        this(getRootDir(group), indexName);

        Logging.logCheckedInfo(LOG, "[", ((group == null) ? "none" : group.toString()), "] : Initialized ", indexName);

    }

    private static File getRootDir(PeerGroup group) {
    	String pgdir = null;
        File storeHome;

        if (group == null) {
            pgdir = "srdi-index";
            storeHome = new File(".jxta");
        } else {
            pgdir = group.getPeerGroupID().getUniqueValue().toString();
            storeHome = new File(group.getStoreHome());
        }

        File rootDir = new File(new File(storeHome, "cm"), pgdir);

        rootDir = new File(rootDir, "srdi");
        if (!rootDir.exists()) {
            // We need to create the directory
            if (!rootDir.mkdirs()) {
                throw new RuntimeException("Cm cannot create directory " + rootDir);
            }
        }

        return rootDir;
	}

	public XIndiceSrdi(File storageDir, String indexName) {
		this.indexName = indexName;
		
		try {
	    	// peerid database
	        // Storage
	        cacheDB = new BTreeFiler();
	        // lazy checkpoint
	        cacheDB.setSync(false);
	        cacheDB.setLocation(storageDir.getCanonicalPath(), indexName);
	
	        if (!cacheDB.open()) {
	            cacheDB.create();
	            // now open it
	            cacheDB.open();
	        }
	
	        // index
	        srdiIndexer = new XIndiceIndexer(false);
	        srdiIndexer.setLocation(storageDir.getCanonicalPath(), indexName);
	        if (!srdiIndexer.open()) {
	            srdiIndexer.create();
	            // now open it
	            srdiIndexer.open();
	        }
		} catch (DBException de) {

	        Logging.logCheckedSevere(LOG, "Unable to Initialize databases\n", de);
	        throw new UndeclaredThrowableException(de, "Unable to Initialize databases");

	    } catch (Throwable e) {

	        Logging.logCheckedSevere(LOG, "Unable to create Cm\n", e);
	
	        if (e instanceof Error) {
	            throw (Error) e;
	        } else if (e instanceof RuntimeException) {
	            throw (RuntimeException) e;
	        } else {
	            throw new UndeclaredThrowableException(e, "Unable to create Cm");
	        }

	    }
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
     */
    public synchronized void add(String primaryKey, String attribute, String value, PeerID pid, long expiration) {

        Logging.logCheckedFine(LOG, "[", indexName, "] Adding ", primaryKey, "/", attribute, " = \'", value, "\' for ", pid);

        try {
            Key key = new Key(primaryKey + attribute + value);
            long expiresin = TimeUtils.toAbsoluteTimeMillis(expiration);

            // update the record if it exists
            synchronized (cacheDB) {
                // FIXME hamada 10/14/04 it is possible a peer re-appears with
                // a different set of indexes since it's been marked for garbage
                // collection.  will address this issue in a subsequent patch
                gcPeerTBL.remove(pid);

                Record record = cacheDB.readRecord(key);
                List<Srdi.Entry> old;

                if (record != null) {
                    old = readRecord(record).list;
                } else {
                    old = new ArrayList<Srdi.Entry>();
                }
                Srdi.Entry entry = new Srdi.Entry(pid, expiresin);

                if (!old.contains(entry)) {
                    old.add(entry);
                } else {
                    // entry exists, replace it (effectively updating expiration)
                    old.remove(old.indexOf(entry));
                    old.add(entry);
                }
                // no sense in keeping expired entries.
                old = removeExpired(old);
                byte[] data = getData(key, old);

                // if (LOG.isLoggable(Level.FINE)) {
                // LOG.fine("Serialized result in : " + (TimeUtils.timeNow() - t0) + "ms.");
                // }
                if (data == null) {
                    Logging.logCheckedSevere(LOG, "Failed to serialize data");
                    return;
                }

                Value recordValue = new Value(data);
                long pos = cacheDB.writeRecord(key, recordValue);
                Map<String, String> indexables = getIndexMap(primaryKey + attribute, value);

                srdiIndexer.addToIndex(indexables, pos);
            }

        } catch (IOException de) {

            Logging.logCheckedWarning(LOG, "Failed to add SRDI\n", de);

        } catch (DBException de) {

            Logging.logCheckedWarning(LOG, "Failed to add SRDI\n", de);

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
    public List<Srdi.Entry> getRecord(String pkey, String skey, String value) {
        Record record = null;

        try {

            Key key = new Key(pkey + skey + value);

            synchronized (cacheDB) {
                record = cacheDB.readRecord(key);
            }

        } catch (DBException de) {

            Logging.logCheckedWarning(LOG, "Failed to retrieve SrdiIndex record\n", de);

        }

        // if record is null, readRecord returns an empty list
        return readRecord(record).list;

    }

    /**
     * inserts a pkey into a map with a value of value
     *
     * @param primaryKey primary key
     * @param value      value
     * @return The Map
     */

    private Map<String, String> getIndexMap(String primaryKey, String value) {
        if (primaryKey == null) {
            return null;
        }
        if (value == null) {
            value = "";
        }
        Map<String, String> map = new HashMap<String, String>(1);

        map.put(primaryKey, value);
        return map;
    }

    /**
     * remove entries pointing to peer id from cache
     *
     * @param pid peer id to remove
     */
    public synchronized void remove(PeerID pid) {

        Logging.logCheckedFine(LOG, " Adding ", pid, " to peer GC table");
        gcPeerTBL.add(pid);

    }

    /**
     * Query Srdi
     *
     * @param attribute Attribute String to query on
     * @param value     value of the attribute string
     * @return an enumeration of canonical paths
     * @param primaryKey primary key
     * @param threshold max number of results
     */
    public synchronized List<PeerID> query(String primaryKey, String attribute, String value, int threshold) {

        Logging.logCheckedFine(LOG, "[", indexName, "] Querying for ", threshold, " ", primaryKey, "/", attribute, " = \'", value, "\'");

        // return nothing
        if (primaryKey == null) return Collections.emptyList();

        List<PeerID> res;

        // a blind query
        if (attribute == null) {
            res = query(primaryKey);
        } else {
            res = new ArrayList<PeerID>();

            IndexQuery iq = XIndiceAdvertisementCache.getIndexQuery(value);

            try {

                srdiIndexer.search(iq, primaryKey + attribute, new SearchCallback(cacheDB, res, threshold, gcPeerTBL));

            } catch (Exception ex) {

                Logging.logCheckedWarning(LOG, "Failure while searching in index\n", ex);

            }
        }

        Logging.logCheckedFine(LOG, "[", indexName, "] Returning ", res.size(), " results for ", primaryKey, "/", attribute, " = \'", value, "\'");

        return res;

    }

    /**
     * Query Srdi
     *
     * @param primaryKey primary key
     * @return A list of Peer IDs.
     */
    protected synchronized List<PeerID> query(String primaryKey) {

        Logging.logCheckedFine(LOG, "[", indexName, "] Querying for ", primaryKey);

        List<PeerID> res = new ArrayList<PeerID>();

        try {

            Map<String, NameIndexer> map = srdiIndexer.getIndexers();

            for (Map.Entry<String, NameIndexer> index : map.entrySet()) {
                String indexName = index.getKey();
                // seperate the index name from attribute
                if (indexName.startsWith(primaryKey)) {
                    NameIndexer idxr = index.getValue();
                    idxr.query(null, new SearchCallback(cacheDB, res, Integer.MAX_VALUE, gcPeerTBL));
                }
            }
        } catch (Exception ex) {

            Logging.logCheckedWarning(LOG, "Exception while searching in index\n", ex);

        }

        Logging.logCheckedFine(LOG, "[", indexName, "] Returning ", res.size(), " results for ", primaryKey);

        return res;
    }

    private static final class SearchCallback implements BTreeCallback {
        private final BTreeFiler cacheDB;
        private final int threshold;
        private final List<PeerID> results;
        private final Set<PeerID> excludeTable;

        SearchCallback(BTreeFiler cacheDB, List<PeerID> results, int threshold, Set<PeerID> excludeTable) {
            this.cacheDB = cacheDB;
            this.threshold = threshold;
            this.results = results;
            this.excludeTable = excludeTable;
        }

        /**
         * @inheritDoc
         */
        public boolean indexInfo(Value val, long pos) {

            if (results.size() >= threshold) {

                Logging.logCheckedFine(LOG, "SearchCallback.indexInfo reached Threshold :", threshold);
                return false;

            }

            Logging.logCheckedFine(LOG, "Found ", val);

            Record record = null;

            try {

                record = cacheDB.readRecord(pos);

            } catch (DBException ex) {

                Logging.logCheckedWarning(LOG, "Exception while reading indexed\n", ex);
                return false;

            }

            if (record != null) {

                long t0 = TimeUtils.timeNow();

                Srdi.SrdiIndexRecord rec = readRecord(record);
                Logging.logCheckedFinest(LOG, "Got result back in : ", (TimeUtils.timeNow() - t0), "ms.");

                copyIntoList(results, rec.list, excludeTable, threshold);

            }

            return results.size() < threshold;
        }
    }

    private static final class GcCallback implements BTreeCallback {
        private final BTreeFiler cacheDB;
        private final List<Long> list;
        private final Set<PeerID> table;

        GcCallback(BTreeFiler cacheDB, XIndiceIndexer idxr, List<Long> list, Set<PeerID> table) {
            this.cacheDB = cacheDB;
            this.list = list;
            this.table = table;
        }

        /**
         * @inheritDoc
         */
        public boolean indexInfo(Value val, long pos) {

            Record record = null;

            synchronized (cacheDB) {

                try {

                    record = cacheDB.readRecord(pos);

                } catch (DBException ex) {

                    Logging.logCheckedWarning(LOG, "Exception while reading indexed\n", ex);
                    return false;

                }

                if (record == null) return true;

                Srdi.SrdiIndexRecord rec = readRecord(record);
                List<Srdi.Entry> res = rec.list;
                boolean changed = false;

                Iterator<Srdi.Entry> eachEntry = res.iterator();
                while(eachEntry.hasNext()) {
                    Srdi.Entry entry = eachEntry.next();

                    if (entry.isExpired() || table.contains(entry.peerid)) {
                        changed = true;
                        eachEntry.remove();
                    }
                }
                if (changed) {

                    if (res.isEmpty()) {

                        try {

                            cacheDB.deleteRecord(rec.key);
                            list.add(pos);

                        } catch (DBException e) {

                            Logging.logCheckedWarning(LOG, "Exception while deleting empty record\n", e);

                        }

                    } else {
                        // write it back
                        byte[] data = getData(rec.key, res);
                        Value recordValue = new Value(data);

                        try {

                            cacheDB.writeRecord(pos, recordValue);

                        } catch (DBException ex) {

                            Logging.logCheckedWarning(LOG, "Exception while writing back record\n", ex);

                        }
                    }
                }
            }
            return true;
        }
    }

    public void clear() {

    	Map<String, NameIndexer> map = srdiIndexer.getIndexers();

        for(NameIndexer idxr : map.values()) {

            synchronized(this) {

                try {

                    ClearCallback callback = new ClearCallback();
                    idxr.query(null, callback);

                } catch (Exception e) {

                    Logging.logCheckedWarning(LOG, "Query to clear index failed\n", e);

                }
            }
        }
    }

    /**
     * Adapted from GcCallback to remove all entries from the cache and purge the srdi index.
     */
    private class ClearCallback implements BTreeCallback {
    	
    	public boolean indexInfo(Value value, long position) {

            Record record = null;

            synchronized (cacheDB) {

                try {

                    record = cacheDB.readRecord(position);

                } catch (DBException ex) {

                    Logging.logCheckedWarning(LOG, "Exception while reading indexed\n", ex);
                    return false;

                }

                if (record == null) return true;

                Srdi.SrdiIndexRecord rec = readRecord(record);

                try {

                    cacheDB.deleteRecord(rec.key);
                    srdiIndexer.purge(position);

                } catch (Exception e) {

                    Logging.logCheckedWarning(LOG, "Exception while deleting empty record\n", e);

                }
            }

            return true;
    	}
    }

    /**
     * copies the content of List into a list. Expired entries are not
     * copied
     *
     * @param to   list to copy into
     * @param from list to copy from
     * @param table table of PeerID's
     * @param threshold maximum number of values permitted in the "to" list
     */
    private static void copyIntoList(List<PeerID> to, List<Srdi.Entry> from, Set<PeerID> table, int threshold) {

        for (Srdi.Entry entry : from) {
            boolean expired = entry.isExpired();

            Logging.logCheckedFiner(LOG, "Entry peerid : ", entry.peerid, (expired ? " EXPIRED " : (" Expires at : " + entry.expiration)));

            if (!to.contains(entry.peerid) && !expired) {
                if (!table.contains(entry.peerid)) {

                    Logging.logCheckedFiner(LOG, "adding Entry :", entry.peerid, " to list");

                    to.add(entry.peerid);
                    if(to.size() >= threshold) return;

                } else {

                    Logging.logCheckedFiner(LOG, "Skipping gc marked entry :", entry.peerid);

                }

            } else {

                Logging.logCheckedFiner(LOG, "Skipping expired Entry :", entry.peerid);

            }
        }
    }

    /**
     * Converts a List of {@link Entry} into a byte[]
     *
     * @param key  record key
     * @param list List to convert
     * @return byte []
     */
    private static byte[] getData(Key key, List<Srdi.Entry> list) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeUTF(key.toString());
            dos.writeInt(list.size());
            for (Srdi.Entry anEntry : list) {
                dos.writeUTF(anEntry.peerid.toString());
                dos.writeLong(anEntry.expiration);
            }
            dos.close();
            return bos.toByteArray();
        } catch (IOException ie) {
            Logging.logCheckedFine(LOG, "Exception while reading Entry\n", ie);
        }
        return null;
    }

    /**
     * Reads the content of a record into List
     *
     * @param record Btree Record
     * @return List of entries
     */
    protected static Srdi.SrdiIndexRecord readRecord(Record record) {
        List<Srdi.Entry> result = new ArrayList<Srdi.Entry>();
        Key key = null;

        if (record == null) {
            return new Srdi.SrdiIndexRecord(null, result);
        }
        if (record.getValue().getLength() <= 0) {
            return new Srdi.SrdiIndexRecord(null, result);
        }
        InputStream is = record.getValue().getInputStream();

        try {
            DataInputStream ois = new DataInputStream(is);

            key = new Key(ois.readUTF());
            int size = ois.readInt();

            for (int i = 0; i < size; i++) {
                try {
                    String idstr = ois.readUTF();
                    PeerID pid = (PeerID) IDFactory.fromURI(new URI(idstr));
                    long exp = ois.readLong();
                    Srdi.Entry entry = new Srdi.Entry(pid, exp);

                    result.add(entry);
                } catch (URISyntaxException badID) {
                    // ignored
                }
            }

            ois.close();

        } catch (EOFException eofe) {

            Logging.logCheckedFine(LOG, "Empty record\n", eofe);

        } catch (IOException ie) {

            Logging.logCheckedWarning(LOG, "Exception while reading Entry", ie);

        }

        return new Srdi.SrdiIndexRecord(key, result);

    }

    /**
     * Garbage Collect expired entries
     */

    public void garbageCollect() {

        try {

            Logging.logCheckedFine(LOG, "Garbage collection started");

            Map<String, NameIndexer> map = srdiIndexer.getIndexers();

            for(NameIndexer idxr : map.values()) {
                List<Long> list = new ArrayList<Long>();

                if(stop) {
                    break;
                }

                synchronized(this) {
                    idxr.query(null, new GcCallback(cacheDB, srdiIndexer, list, gcPeerTBL));
                    srdiIndexer.purge(list);
                }
            }

            gcPeerTBL.clear();

        } catch (Exception ex) {

            Logging.logCheckedWarning(LOG, "Failure during SRDI Garbage Collect\n", ex);

        }

        Logging.logCheckedFine(LOG, "Garbage collection completed");

    }

    /**
     * Remove expired entries from a List
     *
     * @param list A list of entries.
     * @return The same list with the expired entries removed.
     */
    private static List<Srdi.Entry> removeExpired(List<Srdi.Entry> list) {
        Iterator<Srdi.Entry> eachEntry = list.iterator();

        while(eachEntry.hasNext()) {

            Srdi.Entry entry = eachEntry.next();

            if (entry.isExpired()) {
                eachEntry.remove();
                Logging.logCheckedFine(LOG, "Removing expired Entry peerid :", entry.peerid, " Expires at :", entry.expiration);
            }

        }

        return list;
    }

    /**
     * stop the current running thread
     */
    public synchronized void stop() {
        if(stop) {
            return;
        }

        stop = true;

        // Stop the database

        try {

            srdiIndexer.close();
            cacheDB.close();
            gcPeerTBL.clear();

        } catch (Exception ex) {

            Logging.logCheckedSevere(LOG, "Unable to stop the Srdi Indexer\n", ex);

        }
    }

    /**
     * Flushes the Srdi directory for a specified group
     * this method should only be called before initialization of a given group
     * calling this method on a running group would have undefined results
     *
     * @param group group context
     */
    public static void clearSrdi(PeerGroup group) {

        String pgdir = null;
        String pgname = "<unknown>";

        if (group == null) {
            pgdir = "srdi-index";
        } else {
            pgdir = group.getPeerGroupID().getUniqueValue().toString();
            pgname = group.getPeerGroupName();
        }

        Logging.logCheckedInfo(LOG, "Clearing SRDI for ", pgname);

        try {

            File rootDir = null;

            if (group != null) rootDir = new File(new File(new File(group.getStoreHome()), "cm"), pgdir);

            rootDir = new File(rootDir, "srdi");
            if (rootDir.exists()) {

                // remove it along with it's content
                String[] list = rootDir.list();

                for (String aList : list) {

                    Logging.logCheckedFine(LOG, "Removing : ", aList);

                    File file = new File(rootDir, aList);

                    if (!file.delete()) {
                        Logging.logCheckedWarning(LOG, "Unable to delete the file");
                    }

                }

                rootDir.delete();
            }

        } catch (Throwable t) {

            Logging.logCheckedWarning(LOG, "Unable to clear Srdi\n", t);

        }
    }
}

