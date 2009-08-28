package net.jxta.impl.cm.sql;

import java.io.File;
import java.io.IOException;

public class DerbySeedDbGenerator {

	public static void main(String[] args) throws IOException {
		File storeRoot = new File("derby_seed2");
		DerbyAdvertisementCache cache = new DerbyAdvertisementCache(storeRoot.toURI(), "testArea");
		
		cache.stop();
	}
}