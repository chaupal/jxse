package net.jxta.impl.cm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import net.jxta.impl.util.threads.TaskManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractCmConcurrencyTest {

    private static final int NUM_CACHES = 8;
    private static final int NUM_OPERATIONS = 1000;

    protected TaskManager taskManager;

    @Rule
    public TemporaryFolder testFileStore = new TemporaryFolder();

    @Before
    public void initTaskManager() {
        taskManager = new TaskManager();
    }

    @After
    public void shutdownTaskManager() {
        taskManager.shutdown();
    }

    @Test
    public void testConcurrentSafety_randomLoad() throws Exception {

        CacheManager[] caches = new CacheManager[NUM_CACHES];
        for(int i=0; i < caches.length; i++) {
            caches[i] = new CacheManager(createWrappedCache("testArea"+i, taskManager));

            System.out.println("Seeding cache " + i);
            seedCache(caches[i]);
        }

        CountDownLatch completionLatch = new CountDownLatch(NUM_CACHES);

        System.out.println("Starting random testers");
        CmRandomLoadTester[] testers = new CmRandomLoadTester[NUM_CACHES];
        for(int i=0; i < testers.length; i++) {
            testers[i] = new CmRandomLoadTester(caches[i], NUM_OPERATIONS, completionLatch);
            new Thread(testers[i]).start();
        }

        System.out.println("Awaiting completion");
        completionLatch.await();

        for(int i=0; i < testers.length; i++) {
            assertTrue("Tester " + i + " failed", testers[i].isSuccessful());
        }

        for(int i=0; i < caches.length; i++) {
            caches[i].stop();
        }

        System.out.println("Complete");
    }

    private void seedCache(CacheManager cm) throws IOException {
        for(int i=0; i < 1000; i++) {
            cm.save(Double.toString(Math.random()), Double.toString(Math.random()), new byte[1024], Long.MAX_VALUE, Long.MAX_VALUE);
        }
    }

    protected abstract AdvertisementCache createWrappedCache(String string, TaskManager taskManager) throws IOException;

}
