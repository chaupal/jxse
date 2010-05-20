package net.jxta.impl.cm.srdi.bdb;

import java.io.IOException;

import net.jxta.impl.cm.AbstractSrdiIndexBackendConcurrencyTest;
import net.jxta.impl.cm.SrdiAPI;
import net.jxta.peergroup.PeerGroup;

public class BerkeleyDbSrdiIndexBackendConcurrencyTest extends AbstractSrdiIndexBackendConcurrencyTest {

	@Override
	protected SrdiAPI createBackend(PeerGroup group, String indexName) throws IOException {
		return new BerkeleyDbSrdiIndexBackend(group, indexName);
	}

}
