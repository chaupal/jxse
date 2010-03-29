package net.jxta.impl.cm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.FileSystemTest;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public abstract class AbstractSrdiIndexBackendLoadTest {
    
    private JUnit4Mockery mockContext = new JUnit4Mockery();
    
	private File storeRoot;
	private String oldSrdiImplName;
	
	@Before
	public void setUp() throws Exception {
		storeRoot = FileSystemTest.createTempDirectory("SrdiIndexBackendConcurrencyTest");
		oldSrdiImplName = System.getProperty(Srdi.SRDI_INDEX_BACKEND_SYSPROP);
		System.setProperty(Srdi.SRDI_INDEX_BACKEND_SYSPROP, getSrdiIndexBackendClassname());
	}
	
	@After
	public void tearDown() throws Exception {
		FileSystemTest.deleteDir(storeRoot);
		if(oldSrdiImplName != null) {
			System.setProperty(Srdi.SRDI_INDEX_BACKEND_SYSPROP, oldSrdiImplName);
		} else {
			System.clearProperty(Srdi.SRDI_INDEX_BACKEND_SYSPROP);
		}
	}
	
	protected abstract String getSrdiIndexBackendClassname();
	
	@Test
	public void testAddPerformance() throws IOException {
		Srdi index = new Srdi(createGroup(PeerGroupID.defaultNetPeerGroupID, "group"), "testIndex");
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
	private void measureAddRemoveTime(Srdi index, FileWriter writer) throws IOException {
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
	private void add10000Records(Srdi index, FileWriter writer) throws IOException {
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
	
}
