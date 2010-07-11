package net.jxta.impl.cm;

import java.io.IOException;

import net.jxta.impl.util.threads.TaskManager;

public class XIndiceCmConcurrencyTest extends AbstractCmConcurrencyTest {
    
    @Override
    protected AdvertisementCache createWrappedCache(String areaName, TaskManager taskManager) throws IOException {
        return new XIndiceAdvertisementCache(testFileStore.getRoot().toURI(), areaName, taskManager);
    }
}
