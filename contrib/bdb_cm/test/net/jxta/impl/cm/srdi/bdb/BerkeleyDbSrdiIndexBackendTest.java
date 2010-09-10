package net.jxta.impl.cm.srdi.bdb;

import net.jxta.impl.cm.AbstractSrdiIndexBackendTest;
import net.jxta.impl.cm.SrdiAPI;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import org.jmock.Expectations;

public class BerkeleyDbSrdiIndexBackendTest extends AbstractSrdiIndexBackendTest {
	
	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) throws Exception {
		return new BerkeleyDbSrdiIndexBackend(group, indexName);
	}

	@Override
	protected Expectations createExpectationsForConstruction_withPeerGroup_IndexName(final PeerGroup mockGroup, final PeerGroupID groupId, final String groupName) {
		return new Expectations() {{
			ignoring(mockGroup).getPeerGroupName(); will(returnValue(groupName));
			ignoring(mockGroup).getPeerGroupID(); will(returnValue(groupId));
			atLeast(1).of(mockGroup).getStoreHome(); will(returnValue(testFileStore.getRoot().toURI()));
		}};
	}

	@Override
	public String getBackendClassname() {
		return BerkeleyDbSrdiIndexBackend.class.getName();
	}

}
