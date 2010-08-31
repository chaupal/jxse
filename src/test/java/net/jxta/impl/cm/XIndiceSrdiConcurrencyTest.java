package net.jxta.impl.cm;

import net.jxta.peergroup.PeerGroup;
import org.junit.Ignore;

@Ignore("Investigate")
public class XIndiceSrdiConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) {
		return new XIndiceSrdi(group, indexName);
	}

}
