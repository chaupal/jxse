package net.jxta.impl.cm;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.jxta.id.IDFactory;
import net.jxta.impl.cm.SrdiIndex.Entry;
import net.jxta.impl.util.FakeSystemClock;
import net.jxta.impl.util.JavaSystemClock;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractSrdiIndexBackendTest {
	
	public static final PeerID PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E504720503250335D5E0326CF3E4271A498E9D5CB98C7C703"));
	public static final PeerID PEER_ID_2 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E50472050325033212AC0685A254A879825EC23B36214EE03"));
	public static final PeerID PEER_ID_3 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503364652E32BCBC4C8596D3CFE9613AE68903"));
	public static final PeerID PEER_ID_4 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E504720503250337B1043A089C6481B85A9CE0D4586662A03"));
	public static final PeerID PEER_ID_5 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E50472050325033624C1724F1CF4038BACF9C81719672A003"));
	public static final PeerID PEER_ID_6 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503383D5217E1EBD4A97AA38C5DC3A32130903"));
	public static final PeerID PEER_ID_7 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E504720503250337B083C30A3F74643884195C51FD4894E03"));
	public static final PeerID PEER_ID_8 = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E504720503250332B22AC234DCD40A3902FB7073613E9E403"));
	
	public static final PeerGroupID GROUP_ID_1 = PeerGroupID.create(URI.create("urn:jxta:uuid-7B96885D59E6498CB1E4C380479967CE02"));
	public static final PeerGroupID GROUP_ID_2 = PeerGroupID.create(URI.create("urn:jxta:uuid-631A2779A3E548748586A011B837A38302"));
	
	private static final int NO_THRESHOLD = Integer.MAX_VALUE;
	
	public abstract String getBackendClassname();
	
	private String oldBackendValue;
	protected SrdiIndex srdiIndex;
	protected FakeSystemClock clock;
	
	protected EntryComparator comparator;
	protected PeerGroup group1;
	protected PeerGroup group2;
	protected SrdiIndex srdiIndexForGroup2;
	private SrdiIndex alternativeIndexForGroup1;
	
	@Rule
	public JUnitRuleMockery mockContext = new JUnitRuleMockery();
	
	@Rule
	public TemporaryFolder testFileStore = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {
        TaskManager.resetTaskManager();
	    
	    oldBackendValue = System.getProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, getBackendClassname());
		
		group1 = mockContext.mock(PeerGroup.class, "group1");
		group2 = mockContext.mock(PeerGroup.class, "group2");
		
		mockContext.checking(createExpectationsForConstruction_withPeerGroup_IndexName(group1, GROUP_ID_1, "group1"));
		mockContext.checking(createExpectationsForConstruction_withPeerGroup_IndexName(group2, GROUP_ID_2, "group2"));
		
		srdiIndex = new SrdiIndex(createBackend(group1, "testIndex"), SrdiIndex.NO_AUTO_GC);
		srdiIndexForGroup2 = new SrdiIndex(createBackend(group2, "testIndex"), SrdiIndex.NO_AUTO_GC);
		alternativeIndexForGroup1 = new SrdiIndex(createBackend(group1, "testIndex2"), SrdiIndex.NO_AUTO_GC);
		clock = new FakeSystemClock();
		comparator = new EntryComparator();
		TimeUtils.setClock(clock);
		
		
	}
	
	protected abstract SrdiIndexBackend createBackend(PeerGroup group, String indexName) throws Exception;

	@After
	public void tearDown() throws Exception {
		srdiIndex.stop();
		TimeUtils.resetClock();
		if(oldBackendValue == null) {
			System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		} else {
			System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, oldBackendValue);
		}
	}
	
	@Test
	public void testAdd() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 10000L);
		List<Entry> record = srdiIndex.getRecord("a", "b", "c");
		assertNotNull(record);
		assertEquals(1, record.size());
		assertEquals(10000L, record.get(0).expiration);
		assertEquals(PEER_ID, record.get(0).peerid);
	}
	
	@Test
	public void testAdd_twiceShouldUpdateExpiry() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 10000L);
		srdiIndex.add("a", "b", "c", PEER_ID, 15000L);
		
		List<Entry> results = srdiIndex.getRecord("a", "b", "c");
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(15000L, results.get(0).expiration);
		assertEquals(PEER_ID, results.get(0).peerid);
	}
	
	@Test
	public void testAdd_calculatesAbsoluteExpiry() throws Exception {
		clock.currentTime = 30000L;
		srdiIndex.add("a", "b", "c", PEER_ID, 5000L);
		srdiIndex.add("a", "b", "c", PEER_ID_2, 6000L);
		
		List<Entry> results = srdiIndex.getRecord("a", "b", "c");
		assertNotNull(results);
		assertEquals(2, results.size());
		assertContains(results, comparator, new Entry(PEER_ID, 35000L), new Entry(PEER_ID_2, 36000L));
	}
	
	@Test
	public void testRemove() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "c", "d", PEER_ID, 1000L);
		
		// this record should not be removed, uses a different peer id
		srdiIndex.add("a", "c", "d", PEER_ID_2, 1000L);
		
		srdiIndex.remove(PEER_ID);
		// remove does not necessarily take effect until the next index GC
		srdiIndex.garbageCollect();
		
		assertEquals(0, srdiIndex.getRecord("a", "b", "c").size());
		List<Entry> results = srdiIndex.getRecord("a", "c", "d");
		assertEquals(1, results.size());
		assertContains(results, comparator, new Entry(PEER_ID_2, 1000L));
	}
	
	@Test
	public void testBulkAddAndRemove() throws Exception {
		Queue<PeerID> peers = new LinkedList<PeerID>();
		for(int i=0; i < 100; i++) {
			PeerID peer = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
			peers.add(peer);
			srdiIndex.add("a", "b", "c", peer, 1000L);
		}
		
		while(peers.size() > 0) {
			assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), peers.toArray(new PeerID[0]));
			srdiIndex.remove(peers.remove());
		}
	}
	
	@Test
	public void testGetRecord_forMultipleMatches() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "c", PEER_ID_2, 2000L);
		srdiIndex.add("a", "b", "c", PEER_ID_3, 3000L);
		
		// these entries should not be returned
		srdiIndex.add("a", "b", "d", PEER_ID_4, 4000L); // wrong value
		srdiIndex.add("b", "b", "c", PEER_ID_5, 1000L); // wrong primary key
		srdiIndex.add("a", "c", "c", PEER_ID_6, 1000L); // wrong attribute
		
		
		List<Entry> results = srdiIndex.getRecord("a", "b", "c");
		assertNotNull(results);
		assertEquals(3, results.size());
		assertContains(results, comparator, new Entry(PEER_ID, 1000L), new Entry(PEER_ID_2, 2000L), new Entry(PEER_ID_3, 3000L));
	}
	
	@Test
	public void testQuery_exactMatch() throws Exception {
		srdiIndex.add("a", "b", "test", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "test", PEER_ID_2, 1000L);
		
		// these entries should not be returned
		srdiIndex.add("a", "b", "testing", PEER_ID_3, 1000L); // is not exactly "test"
		srdiIndex.add("a", "b", "tEsT", PEER_ID_4, 1000L); // wrong case
		srdiIndex.add("a", "c", "test", PEER_ID_5, 1000L); // wrong attribute
		srdiIndex.add("b", "b", "test", PEER_ID_6, 1000L); // wrong primary key
		
		List<PeerID> matches = srdiIndex.query("a", "b", "test", NO_THRESHOLD);
		assertNotNull(matches);
		assertEquals(2, matches.size());
		assertContains(matches, PEER_ID, PEER_ID_2);
	}
	
	@Test
	public void testQuery_startsWith() throws Exception {
		srdiIndex.add("a", "b", "test", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "testing", PEER_ID_2, 1000L);
		
		// these entries should not be returned
		srdiIndex.add("a", "b", "alsotesting", PEER_ID_3, 1000L); // does not start with "test"
		srdiIndex.add("a", "c", "test123", PEER_ID_4, 1000L); // wrong attribute
		srdiIndex.add("a", "b", "tEst", PEER_ID_5, 1000L); // wrong case
		srdiIndex.add("b", "b", "testing", PEER_ID_6, 1000L); // wrong primary key
		
		List<PeerID> results = srdiIndex.query("a", "b", "test*", NO_THRESHOLD);
		assertEquals(2, results.size());
		assertContains(results, PEER_ID, PEER_ID_2);
	}
	
	@Test
	public void testQuery_endsWith() throws Exception {
		srdiIndex.add("a", "b", "alpha", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "delta", PEER_ID_2, 1000L);
		srdiIndex.add("a", "b", "a", PEER_ID_3, 1000L);
		
		// these entries should not be returned
		srdiIndex.add("a", "b", "charlie", PEER_ID_4, 1000L); // does not end in "a"
		srdiIndex.add("a", "c", "alpha", PEER_ID_5, 1000L); // wrong attribute
		srdiIndex.add("a", "b", "alphA", PEER_ID_6, 1000L); // wrong case
		srdiIndex.add("b", "b", "alpha", PEER_ID_7, 1000L); // wrong primary key
		
		List<PeerID> results = srdiIndex.query("a", "b", "*a", NO_THRESHOLD);
		assertEquals(3, results.size());
		assertContains(results, PEER_ID, PEER_ID_2, PEER_ID_3);
	}
	
	@Test
	public void testQuery_contains() throws Exception {
		srdiIndex.add("a", "b", "elf", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "golfer", PEER_ID_2, 1000L);
		srdiIndex.add("a", "b", "lfx", PEER_ID_3, 1000L);
		srdiIndex.add("a", "b", "lf", PEER_ID_4, 1000L);
		
		// these entries should not be returned
		srdiIndex.add("a", "b", "planet", PEER_ID_5, 1000L); // does not contain "lf"
		srdiIndex.add("a", "c", "selfish", PEER_ID_6, 1000L); // wrong attribute
		srdiIndex.add("a", "b", "lFoo", PEER_ID_7, 1000L); // wrong case
		srdiIndex.add("b", "b", "golfer", PEER_ID_8, 1000L); // wrong primary key
		
		List<PeerID> results = srdiIndex.query("a", "b", "*lf*", NO_THRESHOLD);
		assertEquals(4, results.size());
		assertContains(results, PEER_ID, PEER_ID_2, PEER_ID_3, PEER_ID_4);
	}
	
	@Test
	public void testQuery_withThreshold() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "c", PEER_ID_2, 1000L);
		srdiIndex.add("a", "b", "c", PEER_ID_3, 1000L);
		srdiIndex.add("a", "b", "c", PEER_ID_4, 1000L);
		
		List<PeerID> results = srdiIndex.query("a", "b", "c", 2);
		assertEquals(2, results.size());
		assertTrue(containsXOf(results, 2, PEER_ID, PEER_ID_2, PEER_ID_3, PEER_ID_4));
	}
	
	@Test
	public void testQuery_withLargeResultSet() throws Exception {
		PeerID[] ids = new PeerID[500];
		PeerID[] nonExpired = new PeerID[250];
		for(int i=0; i < 250; i++) {
			ids[i] = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
			srdiIndex.add("a", "b", "c", ids[i], 1000L * i);
		}
		
		for(int i=0; i < 250; i++) {
			ids[i+250] = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
			nonExpired[i] = ids[i+250];
			srdiIndex.add("a", "b", "c", ids[i+250], 1000L * (i+250));
		}
		
		assertEquals(500, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		assertTrue(containsXOf(srdiIndex.query("a", "b", "c", 100), 100, ids));
		
		clock.currentTime = 250000;
		
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), nonExpired);
	}
	
	@Test
	public void testQuery_primaryKeyOnly() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "c", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "e", "d", PEER_ID_2, 1000L);
		srdiIndex.add("b", "x", "y", PEER_ID, 1000L);
		
		List<PeerID> results = srdiIndex.query("a", null, null, NO_THRESHOLD);
		assertEquals(2, results.size());
		assertContains(results, PEER_ID, PEER_ID_2);
	}
	
	@Test
	public void testGarbageCollect() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.add("a", "b", "c", PEER_ID_2, 2000L);
		srdiIndex.add("a", "b", "c", PEER_ID_3, 3000L);
		srdiIndex.add("a", "b", "c", PEER_ID_4, 4000L);
		
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID, PEER_ID_2, PEER_ID_3, PEER_ID_4);
		
		clock.currentTime = 1500L;
		srdiIndex.garbageCollect();
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID_2, PEER_ID_3, PEER_ID_4);
		
		clock.currentTime = 2500L;
		srdiIndex.garbageCollect();
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID_3, PEER_ID_4);
		
		clock.currentTime = 3500L;
		srdiIndex.garbageCollect();
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID_4);
		
		clock.currentTime = 4500L;
		srdiIndex.garbageCollect();
		assertEquals(0, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
	}
	
	@Test
	public void testGarbageCollect_automatic() throws Exception {
	    TimeUtils.setClock(new JavaSystemClock());
	    SrdiIndex srdiIndexWithAutoGC = new SrdiIndex(createBackend(group1, "gcIndex"), 500L);
	    srdiIndexWithAutoGC.add("a", "b", "c", PEER_ID, 500L);
	    assertEquals(1, srdiIndexWithAutoGC.query("a", "b", "c", NO_THRESHOLD).size());
	    
	    Thread.sleep(1000L);
	    
	    assertEquals(0, srdiIndexWithAutoGC.query("a", "b", "c", NO_THRESHOLD).size());
	}
	
	@Test
	public void testClear() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
		
		srdiIndex.clear();
		assertEquals(0, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		
		srdiIndex.add("a", "b", "c", PEER_ID_2, 1000L);
		assertEquals(1, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		assertEquals(PEER_ID_2, srdiIndex.query("a", "b", "c", NO_THRESHOLD).get(0));
	}
	
	@Test
	public void testDataSurvivesRestart() throws Exception {
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndex.stop();
		
		SrdiIndex restarted = new SrdiIndex(createBackend(group1, "testIndex"), SrdiIndex.NO_AUTO_GC);
		
		assertEquals(1, restarted.query("a", "b", "c", NO_THRESHOLD).size());
		assertEquals(PEER_ID, restarted.query("a", "b", "c", NO_THRESHOLD).get(0));
	}
	
	@Test
	public void testClearViaStatic() throws Exception {
		SrdiIndex index = srdiIndex;
		index.add("a", "b", "c", PEER_ID, 1000L);
		index.stop();
		
		SrdiIndex index2 = alternativeIndexForGroup1;
		index2.add("a", "b", "c", PEER_ID_2, 1000L);
		index2.stop();
		
		SrdiIndex.clearSrdi(group1);
		
		SrdiIndex restarted = new SrdiIndex(group1, "testIndex");
		assertEquals(0, restarted.query("a", "b", "c", NO_THRESHOLD).size());
		restarted.stop();
		
		SrdiIndex restarted2 = new SrdiIndex(group1, "testIndex2");
		assertEquals(0, restarted2.query("a", "b", "c", NO_THRESHOLD).size());
		restarted.stop();
	}
	
	@Test
	public void testClearViaStatic_groupsWithSameStoreAreIsolated() {
		
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		srdiIndexForGroup2.add("a", "b", "c", PEER_ID, 1000L);
		
		srdiIndex.stop();
		srdiIndexForGroup2.stop();
		
		SrdiIndex.clearSrdi(group1);
		
		SrdiIndex group1IndexRestarted = new SrdiIndex(group1, "testIndex");
		SrdiIndex group2IndexRestarted = new SrdiIndex(group2, "testIndex");
		
		assertTrue(group1IndexRestarted.query("a", "b", "c", NO_THRESHOLD).isEmpty());
		assertContains(group2IndexRestarted.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
	}
	
	@Test
	public void testAdd_GroupIsolation_withinSameStore() {
		checkAddIsolation(srdiIndex, srdiIndexForGroup2);
	}
	
	protected void checkAddIsolation(SrdiIndex a, SrdiIndex b) {
		// sanity check: there should be no results on a newly created index
		assertEquals(0, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		assertEquals(0, srdiIndexForGroup2.query("a", "b", "c", NO_THRESHOLD).size());
		
		srdiIndex.add("a", "b", "c", PEER_ID, 1000L);
		
		// we should not see the result in index2
		assertEquals(1, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		assertTrue(srdiIndexForGroup2.query("a", "b", "c", NO_THRESHOLD).isEmpty());
		
		srdiIndexForGroup2.add("a", "b", "c", PEER_ID_2, 1000L);
		
		// each index should have a different peer id
		assertEquals(1, srdiIndex.query("a", "b", "c", NO_THRESHOLD).size());
		assertContains(srdiIndex.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
		assertEquals(1, srdiIndexForGroup2.query("a", "b", "c", NO_THRESHOLD).size());
		assertContains(srdiIndexForGroup2.query("a", "b", "c", NO_THRESHOLD), PEER_ID_2);
	}
	
	@Test
	public void testClear_GroupIsolation_withinSameStore() {
		checkClearIsolation(srdiIndex, srdiIndexForGroup2);
	}
	
	protected void checkClearIsolation(SrdiIndex a, SrdiIndex b) {
		a.add("a", "b", "c", PEER_ID, 1000L);
		b.add("a", "b", "c", PEER_ID, 1000L);
		a.clear();
		
		// clear should only have affected the first index
		assertTrue(a.query("a", "b", "c", NO_THRESHOLD).isEmpty());
		assertContains(b.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
	}
	
	@Test
	public void testRemove_GroupIsolation_withinSameStore() {
		checkRemoveIsolation(srdiIndex, srdiIndexForGroup2);
	}
	
	protected void checkRemoveIsolation(SrdiIndex a, SrdiIndex b) {
		a.add("a", "b", "c", PEER_ID, 1000L);
		b.add("a", "b", "c", PEER_ID, 1000L);
		
		a.remove(PEER_ID);
		
		assertTrue(a.query("a", "b", "c", NO_THRESHOLD).isEmpty());
		assertContains(b.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
	}
	
	@Test
	public void testAdd_IndexIsolation_withinSameGroup() {
		checkAddIsolation(srdiIndex, alternativeIndexForGroup1);
	}
	
	@Test
	public void testClear_IndexIsolation_withinSameGroup() {
		checkClearIsolation(srdiIndex, alternativeIndexForGroup1);
	}
	
	@Test
	public void testRemove_IndexIsolation_withinSameGroup() {
		checkRemoveIsolation(srdiIndex, srdiIndexForGroup2);
	}

	@Test
	public void testConstruction_withGroup_IndexName() {
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, getBackendClassname());
		final PeerGroup group = mockContext.mock(PeerGroup.class);
		
		mockContext.checking(createExpectationsForConstruction_withPeerGroup_IndexName(group, GROUP_ID_1, "testGroup"));
		
		SrdiIndex srdiIndex = new SrdiIndex(group, "testIndex");
		assertEquals(getBackendClassname(), srdiIndex.getBackendClassName());
		srdiIndex.stop();
	}
	
	protected abstract Expectations createExpectationsForConstruction_withPeerGroup_IndexName(final PeerGroup mockGroup, final PeerGroupID groupId, String groupName);
	
	protected <T> void assertContains(List<T> entries, T... expected) {
		assertContains(entries, null, expected);
	}
	
	protected <T> void assertContains(List<T> entries, Comparator<T> comparator, T... expected) {
		HashMap<T, T> toFind = new HashMap<T, T>();
		for(T e : expected) {
			toFind.put(e, e);
		}
		
		for(T e : entries) {
			if(toFind.containsKey(e)) {
				T match = toFind.get(e);
				assertEquals(match, e);
				if(comparator != null) {
					assertEquals(0, comparator.compare(match, e));
				}
				toFind.remove(e);
			}
		}
		
		assertTrue("Expected entries not found: " + toFind.keySet(), toFind.isEmpty());
	}

	private class EntryComparator implements Comparator<Entry> {

		public int compare(Entry a, Entry b) {
			if(a.peerid.equals(b.peerid) && a.expiration == b.expiration) {
				return 0;
			} else if(a.expiration < b.expiration) {
				return -1;
			}
			
			return 1;
		}
		
	}
	
	protected <T> boolean containsXOf(Collection<T> set, int numExpected, T... expectedSet) {
    	int numMatches = 0;
    	for (T expected : expectedSet) {
    		if(set.contains(expected)) {
    			numMatches++;
    		}
		}
    	
    	return numMatches == numExpected;
    }
}
