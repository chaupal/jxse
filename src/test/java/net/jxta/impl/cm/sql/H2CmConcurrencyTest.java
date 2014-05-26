package net.jxta.impl.cm.sql;

import java.io.IOException;

import net.jxta.impl.cm.AbstractCmConcurrencyTest;
import net.jxta.impl.cm.AdvertisementCache;
import net.jxta.impl.util.threads.TaskManager;
import org.junit.Ignore;

@Ignore("Takes way too long for unit test")
public class H2CmConcurrencyTest extends AbstractCmConcurrencyTest {
	
	@Override
    protected AdvertisementCache createWrappedCache(String areaName, TaskManager taskManager) throws IOException {
         return new H2AdvertisementCache(testFileStore.getRoot().toURI(), areaName, taskManager);
    }


}
