package net.jxta.impl.cm.bdb;

import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.DatabaseEntry;

public class BerkeleyDbUtil {

	public static boolean isPrefixOf(DatabaseEntry partial, DatabaseEntry full) {
		if(full.getSize() < partial.getSize()) {
			return false;
		}
		
		byte[] fullData = full.getData();
		byte[] partialData = partial.getData();
		
		for(int i=0; i < partial.getSize(); i++) {
			if(fullData[i] != partialData[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	public static DatabaseEntry createExpiryKeyEntry(DatabaseEntry entry, long time) {
		if(entry == null) {
			entry = new DatabaseEntry();
		}
		
		LongBinding.longToEntry(time, entry);
		return entry;
	}
	
	public static long getTimeFromExpiryKeyEntry(DatabaseEntry entry) {
		return LongBinding.entryToLong(entry);
	}
}
