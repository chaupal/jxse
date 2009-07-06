package net.jxta.impl.cm;

import java.io.File;
import java.net.URI;
import java.util.List;

import net.jxta.impl.cm.SrdiIndex.Entry;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.FileSystemTest;

import org.jmock.Expectations;

public class XIndiceSrdiIndexBackendTest extends AbstractSrdiIndexBackendTest {

	private File storeHome;
	
	@Override
	protected void setUp() throws Exception {
		storeHome = FileSystemTest.createTempDirectory("DefaultSrdiIndexBackendTest");
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		FileSystemTest.deleteDir(storeHome);
		super.tearDown();
	}
	
	@Override
	public String getBackendClassname() {
		return XIndiceSrdiIndexBackend.class.getName();
	}
	
	@Override
	protected SrdiIndexBackend createBackend(PeerGroup group, String indexName) {
		return new XIndiceSrdiIndexBackend(group, indexName);
	}
	
	/**
	 * Checks that expired entries recorded under the same primary key, attribute and
	 * value combination are removed on a call to add.
	 * <p>
	 * It may not make sense for all implementations to remove expired entries on add -
	 * it is done in the XIndice implementation simply because it is convenient. It
	 * should be possible to copy this test to the test class of an alternate implementation
	 * if it too should remove expired entries.
	 */
	public void testAdd_removesExpiredEntries() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 10000L);
		srdiIndex.add("a", "b", "c", PEER_ID_2, 5000L);
		
		// this entry should not be deleted automatically as it is under a different
		// (pkey, attr, value) combination.
		srdiIndex.add("a", "d", "x", PEER_ID_2, 5000L);
		
		clock.currentTime = 8000L;
		srdiIndex.add("a", "b", "c", PEER_ID_3, 12000L);
		
		List<Entry> record = srdiIndex.getRecord("a", "b", "c");
		assertNotNull(record);
		assertEquals(2, record.size());
		assertContains(record, new Entry(PEER_ID, 10000L), new Entry(PEER_ID_3, 20000L));
	}
	
	/**
	 * Check that if the entries for a peer ID are marked for removal, that
	 * the remove is cancelled if another add for that peer occurs before
	 * the garbage collect.
	 * <p>
	 * This behaviour is commented as "FIXME" in XIndiceSrdiIndexBackend, so it
	 * may not be desirable for all implementations to follow these semantics.
	 */
	public void testRemove_isCancelledBySubsequentAdd() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.remove(PEER_ID);
		srdiIndex.add("a", "d", "e", PEER_ID, 1000L);
		
		srdiIndex.garbageCollect();
		
		List<Entry> results = srdiIndex.getRecord("a", "b", "c");
		assertEquals(1, results.size());
		assertContains(results, new Entry(PEER_ID, 1000L));
	}
	
	public void testConstruction_withNoBackendOverride() {
		String oldBackend = System.getProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		try {
			System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
			final PeerGroup group = mock(PeerGroup.class);
			
			checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
			
			SrdiIndex srdiIndex = new SrdiIndex(group, "testIndex");
			assertEquals("net.jxta.impl.cm.XIndiceSrdiIndexBackend", srdiIndex.getBackendClassName());
			srdiIndex.stop();
		} finally {
			if(oldBackend != null) {
				System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, oldBackend);
			} else {
				System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
			}
		}
	}
	
	public void testConstruction_withNoBackendOverride_GCIntervalSpecified() {
		String oldBackend = System.getProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		try {
			System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
			final PeerGroup group = mock(PeerGroup.class);
			
			checking(createExpectationsForConstruction_withPeerGroup_IndexName_Interval(group));
			
			SrdiIndex srdiIndex = new SrdiIndex(group, "testIndex", 20000L);
			assertEquals("net.jxta.impl.cm.XIndiceSrdiIndexBackend", srdiIndex.getBackendClassName());
			srdiIndex.stop();
		} finally {
			if(oldBackend != null) {
				System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, oldBackend);
			} else {
				System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
			}
		}
	}
	
	@Override
	public Expectations createExpectationsForConstruction_withPeerGroup_IndexName(final PeerGroup mockGroup, final PeerGroupID groupId, String groupName) {
		final URI storeHomeURI = storeHome.toURI();
		return new Expectations() {{
			ignoring(mockGroup).getPeerGroupName(); will(returnValue("testGroup"));
			atLeast(1).of(mockGroup).getPeerGroupID(); will(returnValue(groupId));
			atLeast(1).of(mockGroup).getStoreHome(); will(returnValue(storeHomeURI));
			ignoring(mockGroup).getHomeThreadGroup(); will(returnValue(Thread.currentThread().getThreadGroup()));
		}};
	}
	
	@Override
	public Expectations createExpectationsForConstruction_withPeerGroup_IndexName_Interval(final PeerGroup mockGroup) {
		final URI storeHomeURI = storeHome.toURI();
		return new Expectations() {{
			ignoring(mockGroup).getPeerGroupName(); will(returnValue("testGroup"));
			atLeast(1).of(mockGroup).getPeerGroupID(); will(returnValue(PeerGroupID.defaultNetPeerGroupID));
			atLeast(1).of(mockGroup).getStoreHome(); will(returnValue(storeHomeURI));
			ignoring(mockGroup).getHomeThreadGroup(); will(returnValue(Thread.currentThread().getThreadGroup()));
		}};
	}
	
	public void testConstruction_withSrdiIndexBackendMissingConstructor_PeerGroup_IndexName() {
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName.class.getName());
		final PeerGroup group = mock(PeerGroup.class);
		checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
		
		SrdiIndex index = new SrdiIndex(group, "testIndex");
		
		// should fall back to default
		assertEquals(index.getBackendClassName(), SrdiIndex.DEFAULT_SRDI_INDEX_BACKEND);
	}
	
	public void testConstruction_withSrdiIndexBackendMissingConstructor_PeerGroup_IndexName_Interval() {
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName_Interval.class.getName());
		final PeerGroup group = mock(PeerGroup.class);
		checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
		
		SrdiIndex index = new SrdiIndex(group, "testIndex");
		
		// should fall back to default
		assertEquals(index.getBackendClassName(), SrdiIndex.DEFAULT_SRDI_INDEX_BACKEND);
	}
	
	public void testConstruction_withSrdiIndexBackendMissingStaticMethod_clearSrdi() {
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, SrdiIndexBackendWithoutStaticMethod_clearSrdi.class.getName());
		final PeerGroup group = mock(PeerGroup.class);
		checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
		
		SrdiIndex index = new SrdiIndex(group, "testIndex");
		
		// should fall back to default
		assertEquals(index.getBackendClassName(), SrdiIndex.DEFAULT_SRDI_INDEX_BACKEND);
	}
	
	public void testConstruction_withBackendThatDoesNotImplementSrdiIndexBackend() {
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, BackendThatDoesNotImplementSrdiIndexBackend.class.getName());
		final PeerGroup group = mock(PeerGroup.class);
		checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
		
		SrdiIndex index = new SrdiIndex(group, "testIndex");
		
		// should fall back to default
		assertEquals(index.getBackendClassName(), SrdiIndex.DEFAULT_SRDI_INDEX_BACKEND);
	}
	
	/**
	 * has everything needed except for the constructor that takes a peer group and string as parameters.
	 */
	public static class SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName extends NullSrdiIndexBackend {
		public SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName(PeerGroup group, String indexName, long interval) {}
		public static void clearSrdi(PeerGroup group) {}
	}
	
	/**
	 * has everything needed except for the constructor that takes a peer group, index name and GC interval as parameters.
	 */
	public static class SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName_Interval extends NullSrdiIndexBackend {
		
		public SrdiIndexBackendWithoutConstructor_PeerGroup_IndexName_Interval(PeerGroup group, String indexName) {}
		public static void clearSrdi(PeerGroup group) {}
	}
	
	/**
	 * has everything needed except for the static method to clear all indices for a given group
	 */
	public static class SrdiIndexBackendWithoutStaticMethod_clearSrdi extends NullSrdiIndexBackend {
		
		public SrdiIndexBackendWithoutStaticMethod_clearSrdi(PeerGroup group, String indexName) {}
		public SrdiIndexBackendWithoutStaticMethod_clearSrdi(PeerGroup group, String indexName, long interval) {}
	}
	
	/**
	 * Has all the required constructors and static methods but does not implement SrdiIndexBackend
	 */
	public static class BackendThatDoesNotImplementSrdiIndexBackend {
		public BackendThatDoesNotImplementSrdiIndexBackend(PeerGroup group, String indexName) {}
		public BackendThatDoesNotImplementSrdiIndexBackend(PeerGroup group, String indexName, long interval) {}
		public static void clearSrdi(PeerGroup group) {}
	}
}
