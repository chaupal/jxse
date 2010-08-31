package net.jxta.impl.cm;

import java.io.File;
import java.io.IOException;

import net.jxta.test.util.FileSystemTest;
import org.junit.Ignore;

@Ignore("Takes too long to run")
public class XIndiceCmConcurrencyTest extends AbstractCmConcurrencyTest {

    private File storeHome;
    
    @Override
    protected void setUp() throws Exception {
        storeHome = FileSystemTest.createTempDirectory("XIndiceCmConcurrencyTest");
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileSystemTest.deleteDir(storeHome);
    }
    
    @Override
    protected AdvertisementCache createWrappedCache(String areaName) throws IOException {
        return new XIndiceAdvertisementCache(storeHome.toURI(), areaName);
    }
}
