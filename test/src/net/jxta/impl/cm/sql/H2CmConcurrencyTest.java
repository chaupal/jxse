package net.jxta.impl.cm.sql;

import java.io.IOException;

import net.jxta.impl.cm.AbstractCmConcurrencyTest;
import net.jxta.impl.cm.AdvertisementCache;

public class H2CmConcurrencyTest extends AbstractCmConcurrencyTest {

	@Override
	protected AdvertisementCache createWrappedCache(String areaName)
			throws IOException {
		return new H2AdvertisementCache(testFileStore.getRoot().toURI(), areaName);
	}

}
