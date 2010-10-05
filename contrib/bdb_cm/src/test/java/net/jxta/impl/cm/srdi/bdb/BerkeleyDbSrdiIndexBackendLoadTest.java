package net.jxta.impl.cm.srdi.bdb;

import net.jxta.impl.cm.AbstractSrdiIndexBackendLoadTest;

public class BerkeleyDbSrdiIndexBackendLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return BerkeleyDbSrdiIndexBackend.class.getName();
	}
	
}
