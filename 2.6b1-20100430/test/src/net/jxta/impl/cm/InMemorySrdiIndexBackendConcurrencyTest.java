package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;

public class InMemorySrdiIndexBackendConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiIndexBackend createBackend(PeerGroup group, String indexName) {
		return new InMemorySrdiIndexBackend(group, indexName);
	}

}
