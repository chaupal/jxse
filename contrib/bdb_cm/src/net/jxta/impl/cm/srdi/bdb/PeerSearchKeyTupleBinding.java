package net.jxta.impl.cm.srdi.bdb;

import java.net.URI;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class PeerSearchKeyTupleBinding extends TupleBinding<PeerSearchKey> {

	@Override
	public PeerSearchKey entryToObject(TupleInput input) {
		String peerGroupIdStr = input.readString();
		String indexName = input.readString();
		String peerIdStr = input.readString();
		
		PeerGroupID groupId = PeerGroupID.create(URI.create(peerGroupIdStr));
		PeerID peerId = PeerID.create(URI.create(peerIdStr));
		return new PeerSearchKey(groupId, indexName, peerId);
	}

	@Override
	public void objectToEntry(PeerSearchKey object, TupleOutput output) {
		output.writeString(object.getGroupId().toURI().toString());
		output.writeString(object.getIndexName());
		output.writeString(object.getPeerId().toURI().toString());
	}

	
	
}
