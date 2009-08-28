package net.jxta.impl.cm.sql;

import java.io.File;
import java.io.IOException;

import net.jxta.impl.cm.AbstractCmConcurrencyTest;
import net.jxta.impl.cm.AdvertisementCache;
import net.jxta.test.util.FileSystemTest;

public class H2CmConcurrencyTest extends AbstractCmConcurrencyTest {

	File storeHome;
	
	@Override
	protected AdvertisementCache createWrappedCache(String areaName)
			throws IOException {
		storeHome = FileSystemTest.createTempDirectory("BerkeleyDbCmConcurrencyTest");
		return new H2AdvertisementCache(storeHome.toURI(), areaName);
	}

}
