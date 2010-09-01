package net.jxta.impl.cm;

import org.junit.Ignore;

@Ignore("Just takes too long for unit test")
public class XIndiceSrdiLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return XIndiceSrdi.class.getName();
	}

}
