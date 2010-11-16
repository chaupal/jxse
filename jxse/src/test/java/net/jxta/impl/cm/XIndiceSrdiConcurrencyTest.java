package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;

public class XIndiceSrdiConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) {
		return new XIndiceSrdi(group, indexName);
	}

}
