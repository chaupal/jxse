package net.jxta.impl.cm.bdb;

import java.io.IOException;

import net.jxta.impl.cm.AbstractCmConcurrencyTest;
import net.jxta.impl.cm.AdvertisementCache;

public class BerkeleyDbCmConcurrencyTest extends AbstractCmConcurrencyTest {

    @Override
    protected AdvertisementCache createWrappedCache(String areaName) throws IOException {
        return new BerkeleyDbAdvertisementCache(testFileStore.getRoot().toURI(), areaName);
    }

}
