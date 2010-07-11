package net.jxta.impl.cm.bdb;

import java.io.File;
import java.io.IOException;

import net.jxta.impl.util.threads.TaskManager;

/**
 * Can be used to check how many file handles are in use by multiple instances of a BDB based cache.
 * In Unix, use lsof while the program is running. Expected behaviour is that with multiple instances
 * using the same store root URI, file handle usage should be constant. If a different root is used
 * for each cache, file handle usage will increase linearly with the number of instances.
 */
public class BDBMultiInstanceTest {

	public static void main(String[] args) throws IOException {
		int numInstances = 10000;
		
		TaskManager taskManager = new TaskManager();
		try {
    		File testRootDir = File.createTempFile("multi_instance", null);
    		testRootDir.delete();
    		testRootDir.mkdir();
    		
    		BerkeleyDbAdvertisementCache[] caches = new BerkeleyDbAdvertisementCache[numInstances];
    		for(int i=0; i < numInstances; i++) {
    			System.out.println("creating instance " + i);
    			caches[i] = new BerkeleyDbAdvertisementCache(testRootDir.toURI(), "createMany" + i, taskManager, false);
    			caches[i].save("a", "b", new byte[1024], 10000L, 10000L);
    		}
    		
    		System.out.println("tearing down instances");
    		
    		for(int i=0; i < numInstances; i++) {
    			caches[i].stop();
    		}
    		
    		System.out.println("Deleting temporary store");
    		deleteDir(testRootDir);
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
