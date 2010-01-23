package net.jxta.impl.cm.srdi.bdb;

import com.sleepycat.je.DatabaseEntry;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

/**
 * Peer removal must be isolated to the particular group and index name on which the removal was requested.
 * As such, the search key includes these attributes.
 */
public class PeerSearchKey {

	private PeerGroupID groupId;
	private String indexName;
	private PeerID peerId;
	
	public PeerSearchKey(PeerGroupID groupId, String indexName, PeerID peerId) {
		this.groupId = groupId;
		this.indexName = indexName;
		this.peerId = peerId;
	}
	
	public PeerGroupID getGroupId() {
		return groupId;
	}
	
	public String getIndexName() {
		return indexName;
	}
	
	public PeerID getPeerId() {
		return peerId;
	}

	public void toDatabaseEntry(DatabaseEntry entry) {
		new PeerSearchKeyTupleBinding().objectToEntry(this, entry);
	}
	
	public static PeerSearchKey fromDatabaseEntry(DatabaseEntry entry) {
		return new PeerSearchKeyTupleBinding().entryToObject(entry);
	}

	public DatabaseEntry toDatabaseEntry() {
		DatabaseEntry entry = new DatabaseEntry();
		toDatabaseEntry(entry);
		return entry;
	}
}
