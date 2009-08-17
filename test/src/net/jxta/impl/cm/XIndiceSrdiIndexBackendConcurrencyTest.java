package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;

public class XIndiceSrdiIndexBackendConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiIndexBackend createBackend(PeerGroup group, String indexName) {
		return new XIndiceSrdiIndexBackend(group, indexName);
	}

}
