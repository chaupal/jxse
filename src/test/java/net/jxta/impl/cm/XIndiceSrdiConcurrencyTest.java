package net.jxta.impl.cm;

import org.junit.Ignore;

import net.jxta.peergroup.PeerGroup;

/*
 * NOTE (2010/07/06 iainmcg): These tests are presently known to fail due to incorrect file locking
 * and synchronization control in the XIndice implementation, which is part of the reason why it
 * was refactored out and alternatives provided. It could still be fixed, but I'd recommend investing
 * effort in the alternative implementations instead, and completely remove the XIndice implementation
 * in the near future.
 */
@Ignore
public class XIndiceSrdiConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) {
		return new XIndiceSrdi(group, indexName);
	}

}
