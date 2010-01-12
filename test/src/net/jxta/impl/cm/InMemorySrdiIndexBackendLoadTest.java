package net.jxta.impl.cm;

// Make sure your don;t use the default JUNIT VM args to run this test or you'll get OOM
// I use -XX:MaxPermSize=256m -Xms256m -Xmx512m
public class InMemorySrdiIndexBackendLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return InMemorySrdiIndexBackend.class.getName();
	}

}
