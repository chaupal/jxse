package net.jxta.impl.cm.bdb;

import java.io.File;
import java.io.IOException;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.cm.Cm;
import net.jxta.impl.cm.bdb.BerkeleyDbAdvertisementCache;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;

public class BDBStressTest {

	public static void main(String[] args) throws Exception {
		TaskManager taskManager = new TaskManager();
		try {
    		int numRuns = 5;
    		
    		long[] saveTime = new long[numRuns];
    		long[] removeTime = new long[numRuns];
    		long[] searchTime = new long[numRuns];
    		long[] timeResults = new long[numRuns];
    		
    		for(int i=0; i < numRuns; i++) {
    			File storeRoot = File.createTempFile("bdbstress", null);
    			storeRoot.delete();
    			storeRoot.mkdir();
    			
    			System.out.println("Cycle " + i + " start");
    			long startTime = System.currentTimeMillis();
    			Cm cm = new Cm(new BerkeleyDbAdvertisementCache(storeRoot.toURI(), "testArea", taskManager));
    			
    			PeerGroupID groupId = IDFactory.newPeerGroupID();
    			
    			performSaves(saveTime, i, cm, groupId);
    			performSearches(searchTime, i, cm);
    			performRemoves(removeTime, i, cm);
    			
    			cm.stop();
    			timeResults[i] = System.currentTimeMillis() - startTime;
    			deleteDir(storeRoot);
    			
    			System.out.println("Cycle " + i + " complete");
    		}
    		
    		System.out.println("Average save time for 10000 records: " + calculateAverage(saveTime) + "ms");
    		System.out.println("Average search time for 1000000 searches: " + calculateAverage(searchTime) + "ms");
    		System.out.println("Average remove time for 10000 records: " + calculateAverage(removeTime) + "ms");
    		System.out.println("Average run length: " + calculateAverage(timeResults) + "ms");
		} finally {
		    taskManager.shutdown();
		}
	}

	private static void performSearches(long[] searchTime, int i, Cm cm) {
		long searchStartTime = System.currentTimeMillis();
		for(int j=0; j < 1000000; j++) {
			int index = (int)(Math.random() * 10000);
			cm.search("" + j, "Name", "MyPeer" + index, 1, null);
		}
		searchTime[i] = System.currentTimeMillis() - searchStartTime;
	}

	private static void performSaves(long[] saveTime, int i, Cm cm,
			PeerGroupID groupId) throws IOException {
		long saveStartTime = System.currentTimeMillis();
		for(int j=0; j < 10000; j++) {
			PeerAdvertisement adv = createPeerAdvert(groupId, "MyPeer" + j);
			cm.save("" + i, "" + j, adv, 100000L, 100000L);
		}
		saveTime[i] = System.currentTimeMillis() - saveStartTime;
	}

	private static void performRemoves(long[] removeTime, int i, Cm cm)
			throws IOException {
		long removeStartTime = System.currentTimeMillis();
		for(int j=0; j < 10000; j++) {
			cm.remove("" + i, "" + j);
		}
		removeTime[i] = System.currentTimeMillis() - removeStartTime;
	}

	private static long calculateAverage(long[] timeResults) {
		long sum = 0;
		for(int i=0; i < timeResults.length; i++) {
			sum += timeResults[i];
		}
		return (sum / timeResults.length);
	}
	
	public static PeerAdvertisement createPeerAdvert(PeerGroupID pgID, String peerName) {
        PeerAdvertisement peerAdv = (PeerAdvertisement)
        AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
		
		peerAdv.setPeerGroupID(pgID);
		peerAdv.setPeerID(IDFactory.newPeerID(pgID));
		peerAdv.setName(peerName);
		return peerAdv;
	}
	
	public static void deleteDir(File dir) throws IOException {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                File child = new File(dir, children[i]);
                deleteDir(child);
            }
        }

        if (!dir.delete()) {
            throw new IOException("Unable to delete file " + dir.getAbsolutePath());
        }
    }
	
}
