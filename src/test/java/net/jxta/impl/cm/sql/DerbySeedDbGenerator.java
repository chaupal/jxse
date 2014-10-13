package net.jxta.impl.cm.sql;

import java.io.File;
import java.io.IOException;

import net.jxta.impl.util.threads.TaskManager;

public class DerbySeedDbGenerator {

	public static void main(String[] args) throws IOException {
	    TaskManager taskManager = new TaskManager();
		File storeRoot = new File("derby_seed2");
		DerbyAdvertisementCache cache = new DerbyAdvertisementCache(storeRoot.toURI(), "testArea", taskManager);
		
		cache.stop();
		taskManager.shutdown();
	}
}