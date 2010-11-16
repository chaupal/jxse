package net.jxta.impl.cm.bdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.jxta.impl.cm.AbstractCmTest;
import net.jxta.impl.cm.AdvertisementCache;
import net.jxta.impl.cm.bdb.BerkeleyDbAdvertisementCache;

public class BerkeleyDbAdvertisementCacheTest extends AbstractCmTest {

	@Override
	public AdvertisementCache createWrappedCache(String areaName) throws Exception {
		return new BerkeleyDbAdvertisementCache(testRootDir.toURI(), areaName, false);
	}
	
	@Override
	public String getCacheClassName() {
		return BerkeleyDbAdvertisementCache.class.getName();
	}
	
	public void testGarbageCollection() throws Exception {
		fakeTimer.currentTime = 0;
		cm.save("a", "b", adv, 10000L, 10000L);
		cm.garbageCollect();
		assertEquals(0, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		fakeTimer.currentTime = 20000L;
		cm.garbageCollect();
		assertEquals(1, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		// should return to 0 after each call
		assertEquals(0, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		assertNull(cm.getInputStream("a", "b"));
		assertEquals(-1, cm.getLifetime("a", "b"));
		assertEquals(-1, cm.getExpirationtime("a", "b"));
		
		cm.save("a", "1", createPeerAdvert(groupId, "PeerA"), 15000L, 15000L); // expires @ 35000
		cm.save("a", "2", createPeerAdvert(groupId, "PeerB"), 12000L, 12000L); // expires @ 32000
		cm.save("a", "3", createPeerAdvert(groupId, "PeerC"), 12000L, 12000L); // expires @ 32000
		cm.save("a", "4", createPeerAdvert(groupId, "PeerD"), 18000L, 18000L); // expires @ 38000
		
		fakeTimer.currentTime = 34000L;
		cm.garbageCollect();
		assertEquals(2, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		List<InputStream> search = cm.search("a", "Name", "Peer*", 100, null);
		checkContains(extractNames(search), "PeerA", "PeerD");
		
		fakeTimer.currentTime = 35500L;
		cm.garbageCollect();
		
		assertEquals(1, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		search = cm.search("a", "Name", "Peer*", 100, null);
		checkContains(extractNames(search), "PeerD");
		
		fakeTimer.currentTime = 40000L;
		cm.garbageCollect();
		
		assertEquals(1, ((BerkeleyDbAdvertisementCache)wrappedCache).getExpiryCount());
		search = cm.search("a", "Name", "Peer*", 100, null);
		assertEquals(0, search.size());
	}
	
	public void testCreation_withStoreRootThatIsAFile() throws Exception {
		File testRoot = File.createTempFile("bdbtest", null);
		
		try {
			new BerkeleyDbAdvertisementCache(testRoot.toURI(), "test");
			fail("IOException expected");
		} catch(IOException e) {
			assertTrue("Error message does not contain expected prefix: " + e.getMessage(), e.getMessage().startsWith("Provided store root URI does not point to a directory: "));
		}
	}
	
}
