package net.jxta.impl.cm;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public abstract class AbstractSrdiIndexBackendConcurrencyTest {
	
    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();
    
	private static final int NUM_INDICES = 8;
	private static final int NUM_GROUPS = 8;
	private static final int OPS_PER_INDEX = 500;
	private static final long TEST_TIMEOUT = 120L;
	
	@Rule
	public TemporaryFolder testFileStore = new TemporaryFolder();
	protected File storeRoot;
	
	private ScheduledExecutorService executorService;
	
	@Before
	public void setUp() throws Exception {
	    storeRoot = testFileStore.getRoot();
	    executorService = new ScheduledThreadPoolExecutor(2);
	}
	
	@After
	public void tearDown() throws Exception {
	    executorService.shutdownNow();
	}
	
	@Test
	public void testSeparateIndexConcurrentSafety() throws Exception {
		PeerGroup group = createGroup(PeerGroupID.defaultNetPeerGroupID, "group1");
		SrdiIndex[] indices = new SrdiIndex[NUM_INDICES];
		
		for(int i=0; i < NUM_INDICES; i++) {
			indices[i] = new SrdiIndex(createBackend(group, "index" + i), SrdiIndex.NO_AUTO_GC, executorService);
		}
		
		randomLoadTest(indices);
	}

	private void randomLoadTest(SrdiIndex[] indices) throws InterruptedException {
		CountDownLatch completionLatch = new CountDownLatch(indices.length);
		IndexRandomLoadTester[] testers = new IndexRandomLoadTester[indices.length];
		
		try {
			for(int i=0; i < indices.length; i++) {
				testers[i] = new IndexRandomLoadTester(indices[i], OPS_PER_INDEX, completionLatch);
				new Thread(testers[i]).start();
			}
			
			Assert.assertTrue("Timed out waiting for thread completion", completionLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS));
			
			for(int i=0; i < indices.length; i++) {
				Assert.assertTrue(testers[i].isSuccessful());
			}
		} finally {
			for(int i=0; i < indices.length; i++) {
				if(indices[i] != null) {
					indices[i].stop();
				}
			}
		}
	}
	
	/* this test is particularly slow running for the in memory srdi index test, which
	 * perhaps isn't optimised for this kind of multi-threaded brutality.
	 * FIXME: revisit this, improve performance of in-memory index so that this can be
	 * run.
	 */
	@Ignore @Test
	public void testSeparateGroupConcurrentSafety() throws Exception {
		SrdiIndex[] indices = new SrdiIndex[NUM_INDICES * NUM_GROUPS];
		for(int groupNum = 0; groupNum < NUM_GROUPS; groupNum++) {
			PeerGroup group = createGroup(IDFactory.newPeerGroupID(), "group" + groupNum);
			for(int indexNum = 0; indexNum < NUM_INDICES; indexNum++) {
				indices[NUM_INDICES * groupNum + indexNum] = new SrdiIndex(createBackend(group, "index" + indexNum), SrdiIndex.NO_AUTO_GC, executorService);
			}
		}
		
		randomLoadTest(indices);
	}
	
	private PeerGroup createGroup(final PeerGroupID groupId, final String name) {
		final PeerGroup group = mockContext.mock(PeerGroup.class, name);
		mockContext.checking(new Expectations() {{
			ignoring(group).getStoreHome(); will(returnValue(storeRoot.toURI()));
			ignoring(group).getPeerGroupName(); will(returnValue(name));
			ignoring(group).getPeerGroupID(); will(returnValue(groupId));
			ignoring(group).getHomeThreadGroup(); will(returnValue(Thread.currentThread().getThreadGroup()));
		}});
		
		return group;
	}

	protected abstract SrdiIndexBackend createBackend(PeerGroup group, String indexName) throws IOException;
}
