package net.jxta.impl.cm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.jxta.id.IDFactory;
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

public abstract class AbstractSrdiIndexBackendLoadTest {
    
    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();
    
    @Rule
    public TemporaryFolder testFileStore = new TemporaryFolder();
	private File storeRoot;
	private String oldSrdiImplName;
	
	@Before
	public void setUp() throws Exception {
		storeRoot = testFileStore.getRoot();
		assertNotNull(storeRoot);
		oldSrdiImplName = System.getProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, getSrdiIndexBackendClassname());
	}
	
	@After
	public void tearDown() throws Exception {
		if(oldSrdiImplName != null) {
			System.setProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP, oldSrdiImplName);
		} else {
			System.clearProperty(SrdiIndex.SRDI_INDEX_BACKEND_SYSPROP);
		}
	}
	
	protected abstract String getSrdiIndexBackendClassname();
	
	@Test
	public void testAddPerformance() throws IOException {
		SrdiIndex index = new SrdiIndex(createGroup(PeerGroupID.defaultNetPeerGroupID, "group"), "testIndex");
		File resultsFile = File.createTempFile("perftest_" + index.getBackendClassName(), ".csv", new File("."));
		FileWriter writer = null;
		try {
			writer = new FileWriter(resultsFile);
			add10000Records(index, writer);

			measureAddRemoveTime(index, writer);
		} finally {
			if(writer != null) {
				writer.close();
			}
		}
		
		index.stop();
	}

	/**
	 * Adds 100 records for a new random peer ID, then removes that peer ID, recording the time taken per
	 * operation. Repeats this for 1000 peers.
	 */
	private void measureAddRemoveTime(SrdiIndex index, FileWriter writer) throws IOException {
		List<Long> addTimes = new LinkedList<Long>();
		List<Long> removeTimes = new LinkedList<Long>();
		StatsTracker addTracker = new StatsTracker();
		StatsTracker removeTracker = new StatsTracker();
		
		for(int peerNum=0; peerNum < 100; peerNum++) {
			PeerID pid = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
			
			for(int opNum=0; opNum < 100; opNum++) {
				long addStart = System.nanoTime();
				index.add(Double.toString(Math.random()), Double.toString(Math.random()), Double.toString(Math.random()), pid, Long.MAX_VALUE);
				long addEnd = System.nanoTime();
				addTimes.add(addEnd - addStart);
				addTracker.addResult(addEnd - addStart);
			}
			
			long removeStart = System.nanoTime();
			index.remove(pid);
			long removeEnd = System.nanoTime();
			removeTimes.add(removeEnd - removeStart);
			removeTracker.addResult(removeEnd - removeStart);
		}
		
		writer.write("add time,");
		for(long addTime : addTimes) {
			writer.write(Long.toString(addTime));
			writer.write(',');
		}
		
		writer.write("\r\n,remove time,");
		for(long removeTime : removeTimes) {
			writer.write(Long.toString(removeTime));
			writer.write(',');
		}
		
		writer.write("\r\n");
		
		System.out.println("Add stats: mean=" + (addTracker.getMean() / 1000000.0) + "ms, std dev=" + (addTracker.getStdDev() / 1000000.0) + "ms");
		System.out.println("Remove stats: mean=" + (removeTracker.getMean() / 1000000.0) + "ms, std dev=" + (removeTracker.getStdDev() / 1000000.0) + "ms");
	}

	/**
	 * Adds 10000 entirely random records to the index to warm it up, recording the overall
	 * time taken, though this is not likely to be a conclusive statistic to rely on.
	 */
	private void add10000Records(SrdiIndex index, FileWriter writer) throws IOException {
		long phase1Start = System.nanoTime();
		for(int pk=0; pk < 10; pk++) {
			for(int attr=0; attr < 10; attr++) {
				for(int value=0; value < 10; value++) {
					for(int pid = 0; pid < 10; pid++) {
						index.add(Integer.toString(pk), Integer.toString(attr), Integer.toString(value), IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID), Long.MAX_VALUE);						
					}
				}
			}
		}
		
		long phase1End = System.nanoTime();
		double timeInMs = (phase1End - phase1Start) / 1000000.0;
		
		writer.write("setup time," + timeInMs);
		writer.write("\r\n");
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
	
	@Test(timeout=30000)
	/*
	 * This is an important performance test, as real world usage of the SRDI index often contains many values for
	 * the same key and attribute. If these queries are not fast, overall system performance is hugely degraded.
	 */
	public void testQuery_manyValuesForSameKeyAndAttribute() throws IOException {
	    SrdiIndex index = new SrdiIndex(createGroup(PeerGroupID.defaultNetPeerGroupID, "group"), "duplicatesTestIndex");
	    String primaryKey = "pk";
	    String attribute = "attr";
	    

	    int numAlreadyAdded = 0;
	    // add entries with the same primary key and attribute, but with different values and peer IDs
	    for(int stage=1; stage <= 3; stage++) {
	        int numEntries = (int) Math.pow(10, stage+2);
	        System.out.println("Testing with " + numEntries);
	        StatsTracker queryTimeTracker = new StatsTracker();
	        
    	    System.out.println("Adding test entries");
            for(int i=numAlreadyAdded; i < numEntries; i++) {
    	        index.add(primaryKey, attribute, "value" + i, IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID), Long.MAX_VALUE);
    	    }
    	    System.out.println("Finished adding test entries");
    	    
    	    System.out.println("Performing queries");
    	    Random r = new Random();
    	    // perform random queries to determine the average query time
    	    for(int i=0; i < 1000; i++) {
    	        long startTime = System.nanoTime();
    	        List<PeerID> result = index.query(primaryKey, attribute, "value" + r.nextInt(numEntries), 1);
    	        assertEquals(1, result.size());
    	        long endTime = System.nanoTime();
    	        queryTimeTracker.addResult(endTime - startTime);
    	    }
    	    
    	    System.out.printf("Query times - mean: %.1f ns, min: %.1f, max: %.1f, stdev: %.3f\n" , queryTimeTracker.getMean(), queryTimeTracker.getMin(), queryTimeTracker.getMax(), queryTimeTracker.getStdDev());
	    }
	}
	
}
