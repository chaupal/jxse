package net.jxta.impl.cm;

import java.io.IOException;

public class XIndiceCmConcurrencyTest extends AbstractCmConcurrencyTest {

    @Override
    protected AdvertisementCache createWrappedCache(String areaName) throws IOException {
        return new XIndiceAdvertisementCache(testFileStore.getRoot().toURI(), areaName);
    }
}
