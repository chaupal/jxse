package net.jxta.impl.cm.bdb;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

public class ExpiryKeyCreator implements SecondaryKeyCreator {

	AdvertisementDbRecordTupleBinding binding = new AdvertisementDbRecordTupleBinding();

	public boolean createSecondaryKey(SecondaryDatabase secondary,
			DatabaseEntry key, DatabaseEntry data, DatabaseEntry result)
			throws DatabaseException {
		
		AdvertisementDbRecord entryToObject = binding.entryToObject(data);
		BerkeleyDbUtil.createExpiryKeyEntry(result, entryToObject.lifetime);
		
		return true;
	}

}
