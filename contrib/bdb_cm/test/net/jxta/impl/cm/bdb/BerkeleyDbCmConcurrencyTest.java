package net.jxta.impl.cm.bdb;

import java.io.File;
import java.io.IOException;

import net.jxta.impl.cm.AbstractCmConcurrencyTest;
import net.jxta.impl.cm.AdvertisementCache;
import net.jxta.test.util.FileSystemTest;

public class BerkeleyDbCmConcurrencyTest extends AbstractCmConcurrencyTest {

    private File storeHome;
    
    @Override
    protected void setUp() throws Exception {
        storeHome = FileSystemTest.createTempDirectory("BerkeleyDbCmConcurrencyTest");
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileSystemTest.deleteDir(storeHome);
    }
    
    @Override
    protected AdvertisementCache createWrappedCache(String areaName) throws IOException {
        return new BerkeleyDbAdvertisementCache(storeHome.toURI(), areaName);
    }

}
