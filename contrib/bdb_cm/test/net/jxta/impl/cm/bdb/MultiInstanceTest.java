package net.jxta.impl.cm.bdb;

import java.io.File;
import java.io.IOException;

import net.jxta.impl.cm.Cm;
import net.jxta.impl.cm.bdb.BerkeleyDbAdvertisementCache;
import net.jxta.impl.util.threads.TaskManager;

public class MultiInstanceTest {

	public static void main(String[] args) throws Exception {
	    TaskManager taskManager = new TaskManager();
	    try {
    		int numInstances = 10;
    		
    		File[] storeRoots = new File[numInstances];
    		Cm[] instances = new Cm[numInstances];
    		
    		for(int i=0; i < instances.length; i++) {
    			storeRoots[i] = File.createTempFile("multiinstance", null);
    			storeRoots[i].delete();
    			storeRoots[i].mkdir();
    			
    			System.out.println("Cycle " + i);
    			long startTime = System.currentTimeMillis();
    			instances[i] = new Cm(new BerkeleyDbAdvertisementCache(storeRoots[i].toURI(), "testArea", taskManager));
    			
    			for(int j=0; j < 100000; j++) {
    				instances[i].save("dn", "fn" + j, new byte[1024], 1000000L, 1000000L);
    			}
    			System.out.println("time for cycle:" + (System.currentTimeMillis() - startTime));
    		}
    		
    		for(int i=0; i < instances.length; i++) {
    			instances[i].stop();
    			deleteDir(storeRoots[i]);
    		}
    		
    		System.out.println("Done");
	    } finally {
	        taskManager.shutdown();
	    }
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
