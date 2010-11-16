package net.jxta.impl.cm.srdi.bdb;

import java.net.URI;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class SrdiIndexKeyTupleBinding extends TupleBinding<SrdiIndexKey> {

	@Override
	public SrdiIndexKey entryToObject(TupleInput input) {
		String groupIdStr = input.readString();
		String indexName = input.readString();
		String primaryKey = input.readString();
		String attribute = input.readString();
		String value = input.readString();
		String peerIdStr = input.readString();
		
		PeerGroupID groupId = PeerGroupID.create(URI.create(groupIdStr));
		PeerID peerId = PeerID.create(URI.create(peerIdStr));
		return new SrdiIndexKey(groupId, indexName, primaryKey, attribute, value, peerId);
	}

	@Override
	public void objectToEntry(SrdiIndexKey object, TupleOutput output) {
		output.writeString(object.getGroupId().toURI().toString());
		
		if(object.getIndexName() == null) return;
		output.writeString(object.getIndexName());
		
		if(object.getPrimaryKey() == null) return;
		output.writeString(object.getPrimaryKey());
		
		if(object.getAttribute() == null) return;
		output.writeString(object.getAttribute());
		
		if(object.getValue() == null) return;
		output.writeString(object.getValue());
		
		if(object.getPeerId() == null) return;
		output.writeString(object.getPeerId().toURI().toString());
	}

}
