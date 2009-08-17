package net.jxta.impl.cm;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

public abstract class AbstractCmConcurrencyTest extends TestCase {

    private static final int NUM_CACHES = 8;
    private static final int NUM_OPERATIONS = 1000;
    
    public void testConcurrentSafety_randomLoad() throws Exception {
        
        Cm[] caches = new Cm[NUM_CACHES];
        for(int i=0; i < caches.length; i++) {
            caches[i] = new Cm(createWrappedCache("testArea"+i));
            
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

    private void seedCache(Cm cm) throws IOException {
        for(int i=0; i < 1000; i++) {
            cm.save(Double.toString(Math.random()), Double.toString(Math.random()), new byte[1024], Long.MAX_VALUE, Long.MAX_VALUE);
        }
    }

    protected abstract AdvertisementCache createWrappedCache(String string) throws IOException;
    
}
