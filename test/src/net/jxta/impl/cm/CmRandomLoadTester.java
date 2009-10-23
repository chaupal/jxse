package net.jxta.impl.cm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;

/**
 * Load testing class that randomises it's calls to the index.
 */
public class CmRandomLoadTester implements Runnable {
	
	public static final String[] DIRECTORIES =
	{
		"alfa",
		"bravo",
		"charlie",
		"delta",
		"echo",
		"foxtrot"
	};
	
	public static final String[] PEER_NAMES =
	{
	    "mike",
	    "november",
	    "oscar",
	    "papa",
	    "quebec",
	    "romeo",
	    "sierra"
	};
	
	private final Cm advCache;
	private final int numOps;
	private boolean successful = false;
	
	private CountDownLatch completionLatch;
	
	private Map<String, Integer> expectedCounts;
	private Map<String, Set<String>> expectedFilesInDirectory;
	private Map<String, Map<String, Integer>> expectedPeersInDirectory;
	private Map<String, String> peerNameForDnAndFn;
	
	public CmRandomLoadTester(Cm advCache, int numOps, CountDownLatch completionLatch) {
		this.advCache = advCache;
		this.numOps = numOps;
		this.completionLatch = completionLatch;
		
		expectedCounts = new HashMap<String, Integer>();
		expectedFilesInDirectory = new HashMap<String, Set<String>>();
		expectedPeersInDirectory = new HashMap<String, Map<String, Integer>>();
		peerNameForDnAndFn = new HashMap<String, String>();
		
		for(String dn : DIRECTORIES) {
		    expectedCounts.put(dn, 0);
		    expectedFilesInDirectory.put(dn, new HashSet<String>());
		    HashMap<String, Integer> peerMap = new HashMap<String, Integer>();
            expectedPeersInDirectory.put(dn, peerMap);
            
            for(String peerName : PEER_NAMES) {
                peerMap.put(peerName, 0);
            }
		}
	}
	
	public void run() {
		try {
			for(int i=0; i < numOps; i++) {
				if(Math.random() < 0.8) {
				    // save peer
				    String dn = randomDirectory();
				    String fn = randomFile();
				    String peerName = randomPeerName();
				    
				    add(dn, fn, peerName);
				} else {
				    // remove
				    ArrayList<String> directories = new ArrayList<String>();
				    Collections.addAll(directories, DIRECTORIES);
				    String dn = null;
				    String fn = null;
				    while(fn == null && directories.size() > 0) {
				        dn = randomSelection(directories);
				        fn = randomFileInDirectory(dn);
				    }
				    
				    if(fn != null) {
				        remove(dn, fn);
				    }
				}
				
				/* after a mutation operation, we check the state of the index
				 * still matches what we expect
				 */
				for(String directoryName : DIRECTORIES) {
				    List<InputStream> records = advCache.getRecords(directoryName, Cm.NO_THRESHOLD, null);
                    if(records.size() != expectedCounts.get(directoryName)) {
                        System.err.println("Number of records turned for directory query did not match expected");
				        complete(false);
				        return;
				    }
				    
                    for(String fileName : expectedFilesInDirectory.get(directoryName)) {
                        if(advCache.getInputStream(directoryName, fileName) == null) {
                            System.err.println("Missing file within directory");
                            complete(false);
                            return;
                        }
                    }
				}
				
				String searchDn = randomDirectory();
				String searchPeerName = randomPeerName();
				
				if(advCache.search(searchDn, "Name", searchPeerName, Cm.NO_THRESHOLD, null).size() != getExpectedPeerCount(searchDn, searchPeerName)) {
				    System.err.println("Did not get expected number of results for name query");
				    complete(false);
				    return;
				}
			}
			
			complete(true);
		} catch(Throwable t) {
			System.err.println("Thread died");
			t.printStackTrace();
			complete(false);
		}
	}

    private void add(String dn, String fn, String peerName) throws IOException {
        PeerAdvertisement adv = createPeerAdvert(PeerGroupID.defaultNetPeerGroupID, peerName);
        expectedCounts.put(dn, expectedCounts.get(dn)+1);
        expectedFilesInDirectory.get(dn).add(fn);
        modifyPeerNameCount(dn, peerName, +1);
        peerNameForDnAndFn.put(dn+'/'+fn, peerName);
        advCache.save(dn, fn, adv);
    }

    private void remove(String dn, String fn) throws IOException {
        expectedFilesInDirectory.get(dn).remove(fn);
        expectedCounts.put(dn, expectedCounts.get(dn)-1);
        String peerName = peerNameForDnAndFn.get(dn+'/'+fn);;
        modifyPeerNameCount(dn, peerName, -1);
        advCache.remove(dn, fn);
    }

    private void modifyPeerNameCount(String dn, String peerName, int delta) {
        int oldCount = expectedPeersInDirectory.get(dn).get(peerName);
        expectedPeersInDirectory.get(dn).put(peerName, oldCount + delta);
    }

    private int getExpectedPeerCount(String searchDn, String searchPeerName) {
        return expectedPeersInDirectory.get(searchDn).get(searchPeerName);
    }

    private String randomPeerName() {
        return PEER_NAMES[(int)Math.floor(Math.random() * PEER_NAMES.length)];
    }

    private String randomFile() {
        return Double.toString(Math.random());
    }
    
    private String randomFileInDirectory(String dn) {
        if(expectedFilesInDirectory.get(dn).size() > 0) {
            return expectedFilesInDirectory.get(dn).iterator().next();
        }
        
        return null;
    }

    private String randomDirectory() {
        return DIRECTORIES[(int)Math.floor(Math.random() * DIRECTORIES.length)];
    }
    
    private <T> T randomSelection(List<T> options) {
        int selection = (int)Math.floor(Math.random() * options.size());
        return options.remove(selection);
    }

	private void complete(boolean success) {
		this.successful = success;
		completionLatch.countDown();
	}
	
	public boolean isSuccessful() {
		return successful;
	}
	
	protected PeerAdvertisement createPeerAdvert(PeerGroupID pgID, String peerName) {
        PeerAdvertisement peerAdv = (PeerAdvertisement)
        AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
        
        peerAdv.setPeerGroupID(pgID);
        peerAdv.setPeerID(IDFactory.newPeerID(pgID));
        peerAdv.setName(peerName);
        return peerAdv;
    }
}