package net.jxta.impl.cm.srdi.bdb;

import com.sleepycat.je.DatabaseEntry;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

public class SrdiIndexKey {

	private PeerGroupID groupId;
	private String indexName;
	private String primaryKey;
	private String attribute;
	private String value;
	private PeerID peerId;
	
	public SrdiIndexKey(PeerGroupID groupId, String indexName, String primaryKey, String attribute, String value, PeerID peerId) {
		this.groupId = groupId;
		this.indexName = indexName;
		this.primaryKey = primaryKey;
		this.attribute = attribute;
		this.value = value;
		this.peerId = peerId;
	}
	
	public static SrdiIndexKey fromDatabaseEntry(DatabaseEntry entry) {
		return new SrdiIndexKeyTupleBinding().entryToObject(entry);
	}
	
	/**
	 * Convenience constructor, typically used when searching the database for all entries
	 * belonging to a particular group
	 */
	public SrdiIndexKey(PeerGroupID groupId) {
		this(groupId, null, null, null, null, null);
	}
	
	/**
	 * Convenience constructor, typically used when searching the database for all entries
	 * belonging to a particular group and index name
	 */
	public SrdiIndexKey(PeerGroupID groupId, String indexName) {
		this(groupId, indexName, null, null, null, null);
	}
	
	public PeerGroupID getGroupId() {
		return groupId;
	}
	
	public String getIndexName() {
		return indexName;
	}
	
	public String getPrimaryKey() {
		return primaryKey;
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public String getValue() {
		return value;
	}
	
	public PeerID getPeerId() {
		return peerId;
	}
	
	public void setValue(String value) {
        this.value = value;
    }
	
	public DatabaseEntry toDatabaseEntry() {
		DatabaseEntry entry = new DatabaseEntry();
		toDatabaseEntry(entry);
		return entry;
	}
	
	public void toDatabaseEntry(DatabaseEntry entry) {
		SrdiIndexKeyTupleBinding binding = new SrdiIndexKeyTupleBinding();
		binding.objectToEntry(this, entry);
	}
	
	@Override
	public String toString() {
		return "SrdiIndexKey { groupId=" + groupId + ", indexName=" + indexName + ", primaryKey=" + primaryKey + ", attribute=" + attribute + ", value=" + value + ", peerId=" + peerId;
	}

}
