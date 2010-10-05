package net.jxta.impl.cm.srdi.bdb;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/**
 * Expiry search keys do not include the group ID or index name, as such garbage collections
 * on a shared BDB environment will remove expired keys across indices and groups. While
 * this is not normal behaviour, it is harmless.
 */
public class ExpirySearchKeyCreator implements SecondaryKeyCreator {

	public boolean createSecondaryKey(SecondaryDatabase secondary,
			DatabaseEntry key, DatabaseEntry data, DatabaseEntry result)
			throws DatabaseException {
		long expiry = LongBinding.entryToLong(data);
		LongBinding.longToEntry(expiry, result);
		
		return true;
	}

	public static DatabaseEntry createExpirySearchKey(long expirationTime) {
		DatabaseEntry entry = new DatabaseEntry();
		LongBinding.longToEntry(expirationTime, entry);
		return entry;
	}
}