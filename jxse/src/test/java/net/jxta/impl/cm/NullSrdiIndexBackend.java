package net.jxta.impl.cm;

import java.util.List;

import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.peer.PeerID;

/**
 * Null object implementation of SrdiAPI
 */
public class NullSrdiIndexBackend implements SrdiAPI {

	public void add(String primaryKey, String attribute, String value,
			PeerID pid, long expiration) {
		// do nothing
	}

	public void clear() {
		// do nothing
	}

	public void garbageCollect() {
		// do nothing
	}

	public List<Entry> getRecord(String pkey, String skey, String value) {
		return null;
	}

	public List<PeerID> query(String primaryKey, String attribute, String value, int threshold) {
		return null;
	}

	public void remove(PeerID pid) {
		// do nothing
	}

	public void stop() {
		// do nothing
	}

}
