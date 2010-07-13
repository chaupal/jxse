package net.jxta.impl.cm;

public class XIndiceSrdiLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return XIndiceSrdi.class.getName();
	}

}
