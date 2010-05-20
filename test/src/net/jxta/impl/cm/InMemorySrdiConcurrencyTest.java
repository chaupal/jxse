package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;

public class InMemorySrdiConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) {
		return new InMemorySrdi(group, indexName);
	}

}
