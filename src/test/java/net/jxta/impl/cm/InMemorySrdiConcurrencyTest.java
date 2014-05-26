package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;
import org.junit.Ignore;

@Ignore("Should not override default constructor in junit")
public class InMemorySrdiConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) {
		return new InMemorySrdi(group, indexName);
	}

}
