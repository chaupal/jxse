package net.jxta.impl.cm;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.FileSystemTest;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public abstract class AbstractSrdiIndexBackendConcurrencyTest extends MockObjectTestCase {
	
	private static final int NUM_INDICES = 8;
	private static final int NUM_GROUPS = 8;
	private static final int OPS_PER_INDEX = 1000;
	private static final long TEST_TIMEOUT = 120L;
	
	private File storeRoot;
	
	@Override
	protected void setUp() throws Exception {
		storeRoot = FileSystemTest.createTempDirectory("SrdiIndexBackendConcurrencyTest");
	}
	
	@Override
	protected void tearDown() throws Exception {
		FileSystemTest.deleteDir(storeRoot);
	}
	
	public void testSeparateIndexConcurrentSafety() throws Exception {
		PeerGroup group = createGroup(PeerGroupID.defaultNetPeerGroupID, "group1");
		SrdiIndex[] indices = new SrdiIndex[NUM_INDICES];
		
		for(int i=0; i < NUM_INDICES; i++) {
			indices[i] = new SrdiIndex(createBackend(group, "index" + i), SrdiIndex.NO_AUTO_GC);
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
			assertTrue("Timed out waiting for thread completion", completionLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS));
			
			for(int i=0; i < indices.length; i++) {
				assertTrue(testers[i].isSuccessful());
			}
		} finally {
			for(int i=0; i < indices.length; i++) {
				if(indices[i] != null) {
					indices[i].stop();
				}
			}
		}
	}
	
	public void testSeparateGroupConcurrentSafety() throws Exception {
		SrdiIndex[] indices = new SrdiIndex[NUM_INDICES * NUM_GROUPS];
		for(int groupNum = 0; groupNum < NUM_GROUPS; groupNum++) {
			PeerGroup group = createGroup(IDFactory.newPeerGroupID(), "group" + groupNum);
			for(int indexNum = 0; indexNum < NUM_INDICES; indexNum++) {
				indices[NUM_INDICES * groupNum + indexNum] = new SrdiIndex(createBackend(group, "index" + indexNum), SrdiIndex.NO_AUTO_GC);
			}
		}
		
		randomLoadTest(indices);
	}
	
	private PeerGroup createGroup(final PeerGroupID groupId, final String name) {
		final PeerGroup group = mock(PeerGroup.class, name);
		checking(new Expectations() {{
			ignoring(group).getStoreHome(); will(returnValue(storeRoot.toURI()));
			ignoring(group).getPeerGroupName(); will(returnValue(name));
			ignoring(group).getPeerGroupID(); will(returnValue(groupId));
			ignoring(group).getHomeThreadGroup(); will(returnValue(Thread.currentThread().getThreadGroup()));
		}});
		
		return group;
	}

	protected abstract SrdiIndexBackend createBackend(PeerGroup group, String indexName) throws IOException;
}
