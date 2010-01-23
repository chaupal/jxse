package net.jxta.impl.cm.srdi.bdb;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

public class PeerSearchKeyCreator implements SecondaryKeyCreator {

	public boolean createSecondaryKey(SecondaryDatabase secondary,
			DatabaseEntry key, DatabaseEntry data, DatabaseEntry result)
			throws DatabaseException {
		SrdiIndexKey keyDecoded = SrdiIndexKey.fromDatabaseEntry(key);
		PeerSearchKey searchKey = new PeerSearchKey(keyDecoded.getGroupId(), keyDecoded.getIndexName(), keyDecoded.getPeerId());
		searchKey.toDatabaseEntry(result);
		
		return true;
	}
}
