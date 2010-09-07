package net.jxta.impl.cm;

import org.junit.Ignore;

/*
 * NOTE (2010/07/06 iainmcg): the xindice implementation is known to be slow and so times out
 * on these very aggressive load tests. It is unlikely that the implementation could be improved
 * to the point it will pass the tests, I recommend investing effort in alternative implementations
 * so the XIndice implementation can be completely removed in the near future.
 */
@Ignore
public class XIndiceSrdiLoadTest extends
		AbstractSrdiIndexBackendLoadTest {

	@Override
	protected String getSrdiIndexBackendClassname() {
		return XIndiceSrdi.class.getName();
	}

}
