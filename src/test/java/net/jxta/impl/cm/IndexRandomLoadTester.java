package net.jxta.impl.cm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import net.jxta.id.IDFactory;
import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

/**
 * Load testing class that randomises it's calls to the index.
 */
public class IndexRandomLoadTester implements Runnable {
	
	public static final String[] ATTRIBUTES =
	{
		"alfa",
		"bravo",
		"charlie",
		"delta",
		"echo",
		"foxtrot"
	};
	
	private final Srdi index;
	private final int numOps;
	private boolean successful = false;
	
	private Map<String,Set<PeerID>> expectedPeersForAttribute;
	private Queue<PeerID> peerIds;
	private CountDownLatch completionLatch;
	
	public IndexRandomLoadTester(Srdi index, int numOps, CountDownLatch completionLatch) {
		this.index = index;
		this.numOps = numOps;
		this.completionLatch = completionLatch;
		expectedPeersForAttribute = new HashMap<String, Set<PeerID>>();
		peerIds = new LinkedList<PeerID>();
		for(String attribute : ATTRIBUTES) {
			expectedPeersForAttribute.put(attribute, new HashSet<PeerID>());
		}
		
	}
	
	public void run() {
		try {
			for(int i=0; i < numOps; i++) {
				double decider = Math.random();
				/* decision on which operation to call here is randomised,
				 * with a 49% chance of an add, 49% chance of a removing a peer,
				 * and 2% chance of a clearing everything
				 */
				if(decider < 0.49) {
					String attribute = ATTRIBUTES[(int)Math.floor(Math.random() * ATTRIBUTES.length)];
					PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
					peerIds.add(peerId);
					expectedPeersForAttribute.get(attribute).add(peerId);
					index.add("x", attribute, "y", peerId, Long.MAX_VALUE);
					
				} else if (decider < 0.98) {
					PeerID peerIdToRemove = peerIds.poll();
					if(peerIdToRemove != null) {
						for(String attr : ATTRIBUTES) {
							removeExpectedPeerForAttribute(attr, peerIdToRemove);
						}
						index.remove(peerIdToRemove);
						index.garbageCollect();
					}
				} else {
					peerIds.clear();
					for(String attribute : ATTRIBUTES) {
						expectedPeersForAttribute.get(attribute).clear();
					}
					index.clear();
				}
				
				/* after a mutation operation, we check the state of the index
				 * still matches what we expect
				 */
				for(String attribute : ATTRIBUTES) {
					List<Entry> results = index.getRecord("x", attribute, "y");
					Set<PeerID> resultPeers = new HashSet<PeerID>();
					for(Entry e : results) {
						resultPeers.add(e.peerid);
					}
					
					Set<PeerID> expectedPeers = expectedPeersForAttribute.get(attribute);
					if(!resultPeers.containsAll(expectedPeers) || resultPeers.size() != expectedPeers.size()) {
						System.err.println("index does not match expected state: " + expectedPeers + " vs " + resultPeers);
						complete(false);
						return;
					}
				}
			}
			
			complete(true);
		} catch(Throwable t) {
			System.err.println("Thread died");
			t.printStackTrace();
			complete(false);
		}
	}

	private void complete(boolean success) {
		this.successful = success;
		completionLatch.countDown();
	}
	
	public boolean isSuccessful() {
		return successful;
	}

	private void removeExpectedPeerForAttribute(String attribute, PeerID peerIdToRemove) {
		expectedPeersForAttribute.get(attribute).remove(peerIdToRemove);
	}
}