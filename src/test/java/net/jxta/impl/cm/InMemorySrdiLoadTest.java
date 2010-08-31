package net.jxta.impl.cm;

import org.junit.Ignore;

// Make sure your don;t use the default JUNIT VM args to run this test or you'll get OOM
// I use -XX:MaxPermSize=256m -Xms256m -Xmx512m
@Ignore("Fails need to investigate")
public class InMemorySrdiLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return InMemorySrdi.class.getName();
	}

}
