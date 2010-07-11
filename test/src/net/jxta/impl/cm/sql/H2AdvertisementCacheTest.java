package net.jxta.impl.cm.sql;

import net.jxta.impl.cm.AbstractCmTest;
import net.jxta.impl.cm.AdvertisementCache;

public class H2AdvertisementCacheTest extends AbstractCmTest {

	@Override
	public AdvertisementCache createWrappedCache(String areaName) throws Exception {
		return new H2AdvertisementCache(testRootDir.toURI(), areaName, taskManager);
	}

	@Override
	public String getCacheClassName() {
		return H2AdvertisementCache.class.getName();
	}
}
