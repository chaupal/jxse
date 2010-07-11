package net.jxta.impl.cm.sql;

import net.jxta.impl.cm.AbstractCmTest;
import net.jxta.impl.cm.AdvertisementCache;

public class DerbyAdvertisementCacheTest extends AbstractCmTest {

	@Override
	public AdvertisementCache createWrappedCache(String areaName) throws Exception {
		return new DerbyAdvertisementCache(testRootDir.toURI(), areaName, taskManager);
	}

	@Override
	public String getCacheClassName() {
		return DerbyAdvertisementCache.class.getName();
	}
}
