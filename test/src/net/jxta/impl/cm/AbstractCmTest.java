/*
 * Copyright (c) 2001-2009 Sun Microsystems, Inc.  All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.cm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.util.FakeSystemClock;
import net.jxta.impl.util.TimeUtils;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.SrdiMessage.Entry;
import net.jxta.test.util.FileSystemTest;

/**
 * Comprehensive suite of unit tests for AdvertisementCache implementations.
 * Simply extend this class and implement {@link #getCacheClassName()} and
 * {@link #createWrappedCache()} to unit test your own implementation.
 */
public abstract class AbstractCmTest extends FileSystemTest {
	
	private static final int NO_THRESHOLD = Integer.MAX_VALUE;
	protected String cacheImplClassName;
	protected AdvertisementCache wrappedCache;
    protected Cm cm;

    protected PeerAdvertisement adv;
    protected PeerGroupID groupId;
	
    protected FakeSystemClock fakeTimer = new FakeSystemClock();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        TimeUtils.setClock(fakeTimer);
        wrappedCache = createWrappedCache("testArea"); 
        cm = new Cm(wrappedCache);
        groupId = IDFactory.newPeerGroupID();
        adv = createPeerAdvert(groupId, "MyPeer100");
    }
    
    @Override
    protected void tearDown() throws Exception {
        cm.stop();
        cm = null;
        wrappedCache = null;
        super.tearDown();
        TimeUtils.resetClock();
    }
    
    /**
     * @return the full class name of the implementation to be tested. Used
     * to ensure the cache can be instantiated from the reflection based approach
     * used in the standard {@link Cm} constructors.
     */
    public abstract String getCacheClassName();
    
    /**
     * @return an instance of the advertisement cache to be tested, instantiated
     * with whatever parameters and options are required for it's normal functioning.
     */
    public abstract AdvertisementCache createWrappedCache(String areaName) throws Exception;

    protected PeerAdvertisement createPeerAdvert(PeerGroupID pgID, String peerName) {
        PeerAdvertisement peerAdv = (PeerAdvertisement)
        AdvertisementFactory.newAdvertisement(PeerAdvertisement.getAdvertisementType());
		
		peerAdv.setPeerGroupID(pgID);
		peerAdv.setPeerID(IDFactory.newPeerID(pgID));
		peerAdv.setName(peerName);
		return peerAdv;
	}
    
    public void testGetRecords_withNullDn_returnsEmptyResultSet() {
    	List<InputStream> records = cm.getRecords(null, 10, null);
    	assertNotNull(records);
    	assertEquals(0, records.size());
    }
    
    public void testGetRecords_withThresholdBeneathNumResults() throws IOException {
    	createTestData();
    	
    	List<InputStream> records = cm.getRecords("a", 3, null);
    	assertEquals(3, records.size());
    	assertTrue("expected peers not found", containsXOf(extractNames(records), 3, "Peer1", "Peer2", "Peer3", "Peer4", "SuperPeerX", "other"));
    }
    
    public void testGetRecords_checkExpirationsMatch() throws Exception {
    	fakeTimer.currentTime = 50000;
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer1"), 120000, 100000);
    	cm.save("a", "c", createPeerAdvert(groupId, "Peer2"), 160000, 160000);
    	
    	// simulate time moving forward by 50000ms
    	fakeTimer.currentTime = 100000;
    	
    	List<Long> expirations = new ArrayList<Long>();
		List<InputStream> records = cm.getRecords("a", 100, expirations);
		
		assertNotNull(records);
		assertEquals(2, records.size());
		ArrayList<String> names = new ArrayList<String>();
		extractNames(records, names);
		checkContains(names, "Peer1", "Peer2");
		
		int index = 0;
		for(String name : names) {
			if(name.equals("Peer1")) {
				assertEquals(70000L, expirations.get(index).longValue());
			} else {
				assertEquals(110000L, expirations.get(index).longValue());
			}
			
			index++;
		}
    }
    
    public void testGetRecords() throws Exception {
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer1"));
    	cm.save("a", "c", createPeerAdvert(groupId, "Peer1"));
    	cm.save("a", "d", createPeerAdvert(groupId, "Peer1"));
    	cm.save("b", "b", createPeerAdvert(groupId, "Peer1"));
    	cm.save("b", "c", createPeerAdvert(groupId, "Peer1"));
    	cm.save("ab", "d", createPeerAdvert(groupId, "Peer1"));
    	cm.save("bc", "d", createPeerAdvert(groupId, "Peer1"));
    	
    	assertEquals(3, cm.getRecords("a", 100, null).size());
    	assertEquals(2, cm.getRecords("b", 100, null).size());
    	// ensure dn strings which are substrings of one another do not affect the outcome
    	assertEquals(1, cm.getRecords("ab", 100, null).size());
    	assertEquals(1, cm.getRecords("bc", 100, null).size());
    }
    
    public void testGetLifetime_withUnknownDnFnPair() throws Exception {
    	assertEquals(-1L, cm.getLifetime("does", "notexist"));
	}
    
    public void testGetLifetime() throws Exception {
    	fakeTimer.currentTime = 0;
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer1"), 50000, 100000);
    	assertEquals(50000L, cm.getLifetime("a", "b"));
    	fakeTimer.currentTime = 20000;
    	assertEquals(30000L, cm.getLifetime("a", "b"));
    	fakeTimer.currentTime = 40000;
    	assertEquals(10000L, cm.getLifetime("a", "b"));
    	fakeTimer.currentTime = 60000;
    	assertEquals(-10000L, cm.getLifetime("a", "b"));
    }
    
    public void testGetExpirationTime_withUnknownDnFnPair() throws Exception {
    	assertEquals(-1, cm.getExpirationtime("does", "notexist"));
    }
    
    public void testGetExpirationTime() throws Exception {
    	fakeTimer.currentTime = 10000;

    	cm.save("a", "b", createPeerAdvert(groupId, "Peer1"), 50000, 30000);
    	
    	// expiration = min(relative(lifetime), expiry), and in this case expiry < relative(lifetime)
    	// Note that we set current time initally to 10000 above so that we guarantee the Min() is the expiration time here, and not
    	// 40000, which is the remaining life.
    	assertEquals(30000L, cm.getExpirationtime("a", "b"));
    	fakeTimer.currentTime = 50000;  // Original life of 10000 means we're now 10000 away from expiration
    	assertEquals(10000L, cm.getExpirationtime("a", "b"));
    	fakeTimer.currentTime = 80000;
    	assertEquals(-1L, cm.getExpirationtime("a", "b"));
    }
    
    public void testGetInputStream_withUnknownDnFnPair() throws Exception {
    	assertNull(cm.getInputStream("does", "notexist"));
    }
    
    public void testGetInputStream() throws IOException {
    	byte[] data = new byte[64];
    	for(int i=0; i < data.length; i++) {
    		data[i] = (byte)i;
    	}
    	
    	cm.save("a", "b", data, 10000L, 20000L);
    	
    	InputStream inputStream = cm.getInputStream("a", "b");
    	
    	for(int i=0; i < data.length; i++) {
    		assertEquals(data[i], inputStream.read());
    	}
    	
    	assertEquals("input stream is not depleted when expected", -1, inputStream.read());
    }
    
    public void testRemove() throws Exception {
    	cm.save("a", "b", new byte[64], 10000L, 20000L);
    	
    	assertNotNull(cm.getInputStream("a", "b"));
    	cm.remove("a", "b");
    	assertNull(cm.getInputStream("a", "b"));
    	assertEquals(-1, cm.getLifetime("a", "b"));
    	assertEquals(-1, cm.getExpirationtime("a", "b"));
    	assertNull(cm.getInputStream("a", "b"));
    }
    
    public void testSaveAdvWithIllegalLifetime() throws Exception {
        try {
            cm.save("test", "test2", adv, -1, 100);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException e) {
            assertEquals("Bad expiration or lifetime.", e.getMessage());
        }
    }

    public void testSaveAdvWithIllegalExpiry() throws Exception {
        try {
            cm.save("test", "test2", adv, 100, -1);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException e) {
            assertEquals("Bad expiration or lifetime.", e.getMessage());
        }
    }

    public void testSaveBytesWithIllegalLifetime() throws Exception {
        try {
            cm.save("test", "test2", new byte[64], -1, 100);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException e) {
            assertEquals("Bad expiration or lifetime.", e.getMessage());
        }
    }

    public void testSaveBytesWithIllegalExpiry() throws Exception {
        try {
            cm.save("test", "test2", new byte[64], 100, -1);
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException e) {
            assertEquals("Bad expiration or lifetime.", e.getMessage());
        }
    }
    
    public void testSaveAdv_overridesLifetimeIfLower() throws Exception {
    	PeerAdvertisement ad = createPeerAdvert(groupId, "Peer1");
    	cm.save("a", "b", ad, 20000L, 30000L);
    	cm.save("a", "b", ad, 10000L, 30000L);
    	
    	assertEquals(20000L, cm.getLifetime("a", "b"));
    }
    
    public void testSaveBytes_overridesLifetimeIfLower() throws Exception {
    	byte[] bytes = new byte[64];
    	cm.save("a", "b", bytes, 20000L, 30000L);
    	cm.save("a", "b", bytes, 10000L, 30000L);
    	
    	assertEquals(20000L, cm.getLifetime("a", "b"));
    }
    
    public void testSearch_exactMatch() throws IOException {
    	cm.save("a", "b", adv, 100000, 200000);
    	List<InputStream> results = cm.search("a", "Name", "MyPeer100", 5, null);
    	assertEquals(1, results.size());
    	assertEquals("MyPeer100", getNameFromResult(results.get(0)));
	}

	protected String getNameFromResult(InputStream stream) throws IOException {
		StructuredDocument<?> doc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, stream);
    	Enumeration<?> children = doc.getChildren("Name");
    	return (String)((Element<?>)children.nextElement()).getValue();
	}
    
    public void testSearch_endsWith() throws IOException {
    	createTestData();
    	
    	List<InputStream> results = cm.search("a", "Name", "*PeerX", 10, null);
    	assertEquals(1, results.size());
    	checkContains(extractNames(results), "SuperPeerX");
    }
    
    public void testSearch_startsWith() throws IOException {
    	createTestData();
    	
    	List<InputStream> results = cm.search("a", "Name", "Peer*", 10, null);
    	assertEquals(4, results.size());
    	checkContains(extractNames(results), "Peer1", "Peer2", "Peer3", "Peer4");    	
    }
    
    public void testSearch_contains() throws IOException {
    	createTestData();
    	
    	List<InputStream> results = cm.search("a", "Name", "*Peer*", 10, null);
    	assertEquals(5, results.size());
    	checkContains(extractNames(results), "Peer1", "Peer2", "Peer3", "Peer4", "SuperPeerX");
    }
    
    public void testSearch_matchAnything() throws IOException {
    	createTestData();
    	
    	List<InputStream> results = cm.search("a", "Name", "*", 10, null);
    	assertEquals(6, results.size());
    	checkContains(extractNames(results), "Peer1", "Peer2", "Peer3", "Peer4", "SuperPeerX", "other");
    }

	private void createTestData() throws IOException {
		cm.save("a", "b", createPeerAdvert(groupId, "Peer1"), 100000, 200000);
    	cm.save("a", "c", createPeerAdvert(groupId, "Peer2"), 150000, 200000);
    	cm.save("a", "d", createPeerAdvert(groupId, "Peer3"), 160000, 200000);
    	cm.save("a", "e", createPeerAdvert(groupId, "Peer4"), 170000, 200000);
    	cm.save("a", "f", createPeerAdvert(groupId, "SuperPeerX"), 100000, 200000);
    	cm.save("a", "g", createPeerAdvert(groupId, "other"), 100000, 200000);
	}
    
    public void testSearch_withThreshold() throws IOException {
    	createTestData();
    	
    	// search could return 4 results with this query, but we only want 2
    	List<InputStream> result = cm.search("a", "Name", "*", 2, null);
    	assertEquals(2, result.size());
    	
    	// cannot predict which 2 of the 4 will be returned
    	assertTrue("Subset of expected peers not found", containsXOf(extractNames(result), 2, "Peer1", "Peer2", "Peer3", "Peer4"));
    	
    	result = cm.search("a", "Name", "*", 3, null);
    	assertEquals(3, result.size());
    	assertTrue(containsXOf(extractNames(result), 3, "Peer1", "Peer2", "Peer3", "Peer4"));
    }
    
    public void testSearch_expiredEntriesNotReturned() throws IOException {
    	createTestData();
    	
    	fakeTimer.currentTime = 150000;
    	List<Long> expirations = new LinkedList<Long>();
    	List<InputStream> result = cm.search("a", "Name", "Peer*", 10, expirations);
    	assertEquals(2, result.size());

    	List<String> names = new ArrayList<String>();
    	extractNames(result, names);
    	HashSet<String> nameSet = new HashSet<String>();
    	nameSet.addAll(names);
    	checkContains(nameSet, "Peer3", "Peer4");

    	assertEquals(2, expirations.size());
    	int index = 0;
    	for(String name : names) {
    		if(name.equals("Peer3")) {
    			assertEquals(10000L, expirations.get(index).longValue());
    		} else if(name.equals("Peer4")) {
    			assertEquals(20000L, expirations.get(index).longValue());
    		}
    		
    		index++;
    	}
    }
    
    public void testGetDeltas_generatedBySave() throws Exception {
    	cm.setTrackDeltas(true);
    	assertNotNull(cm.getDeltas("a"));
    	assertEquals(0, cm.getDeltas("a").size());
    	
    	PeerAdvertisement peer1 = createPeerAdvert(groupId, "Peer1");
    	PeerAdvertisement peer2 = createPeerAdvert(groupId, "Peer2");

    	cm.save("a", "b", peer1, 150000L, 100000L);
		cm.save("a", "c", peer2, 150000L, 100000L);

		// each added peer advertisement generates two deltas - one for each indexable property
		List<Entry> expectedDeltas = new ArrayList<Entry>(4);
		expectedDeltas.add(new Entry("PID", peer1.getPeerID().toString(), 150000L));
		expectedDeltas.add(new Entry("Name", peer1.getName(), 150000L));
		expectedDeltas.add(new Entry("PID", peer2.getPeerID().toString(), 150000L));
		expectedDeltas.add(new Entry("Name", peer2.getName(), 150000L));
    	
    	List<Entry> deltas = cm.getDeltas("a");

    	assertEquals(4, deltas.size());
    	assertTrue(deltas.containsAll(expectedDeltas));
    	HashSet<String> keys = new HashSet<String>();
    	keys.add(deltas.get(0).key);
    	keys.add(deltas.get(1).key);
    	checkContains(keys, "PID", "Name");
    }
    
    public void testGetDeltas_generatedByRemove() throws Exception {
    	cm.setTrackDeltas(false);
    	PeerAdvertisement advert = createPeerAdvert(groupId, "Peer 1");
    	cm.save("a", "b", advert, 100000L, 200000L);
    	
    	cm.setTrackDeltas(true);
    	cm.remove("a", "b");
    	
    	List<Entry> expectedDeltas = new ArrayList<Entry>(4);
    	expectedDeltas.add(new Entry("PID", advert.getPeerID().toString(), 100000L));
    	expectedDeltas.add(new Entry("Name", advert.getName(), 100000L));
    	
    	List<Entry> deltas = cm.getDeltas("a");
    	assertEquals(2, deltas.size());
    	assertTrue(deltas.containsAll(expectedDeltas));
    }
    
    public void testGetDeltas_clearsOnEachConsecutiveCall() throws Exception {
    	cm.setTrackDeltas(true);
    	cm.save("a","b",createPeerAdvert(groupId, "Peer 1"), 100000L, 200000L);
    	assertEquals(2, cm.getDeltas("a").size());
    	assertEquals(0, cm.getDeltas("a").size());
    	
    	cm.remove("a", "b");
    	assertEquals(2, cm.getDeltas("a").size());
    	assertEquals(0, cm.getDeltas("a").size());
    }
    
    public void testSave_deltasNotGeneratedWithZeroExpiration() throws Exception {
    	cm.setTrackDeltas(true);
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer1"), 10000L, 0L);
    	assertEquals(0, cm.getDeltas("a").size());
    }
    
    public void testSave_deltasNotGeneratedWithTrackingOff() throws Exception {
    	cm.setTrackDeltas(false);
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer2"), 100000L, 200000L);
    	assertEquals(0, cm.getDeltas("a").size());
    }
    
    public void testRemove_deltasNotGeneratedWithTrackingOff() throws Exception {
    	cm.save("a", "b", createPeerAdvert(groupId, "Peer 1"), 100000L, 200000L);
    	// clear any deltas generated by save
    	cm.getDeltas("a");
    	cm.setTrackDeltas(false);
    	cm.remove("a", "b");
    	assertEquals(0, cm.getDeltas("a").size());
    }
    
    public void testGetEntries() throws Exception {
        PeerAdvertisement peerAdv = createPeerAdvert(groupId, "Test peer");

        cm.save("a", "b", adv, 100000L, 100000L);
        cm.save("a", "c", peerAdv, 200000L, 200000L);
        
        // this advert won't be included in the returned results, wrong dn
        cm.save("b", "c", createPeerAdvert(groupId, "Test peer 2"));
        
        List<Entry> entries = cm.getEntries("a", false);
        assertNotNull(entries);
        assertEquals(4, entries.size());
        checkContains(entries, new EntryComparator(),
                               new Entry("PID", adv.getPeerID().toString(), 100000L), 
                               new Entry("Name", adv.getName(), 100000L),
                               new Entry("PID", peerAdv.getPeerID().toString(), 200000L),
                               new Entry("Name", peerAdv.getName(), 200000L));
    }

    /**
     * Pseudo-comparator for Entry objects that returns 0 if there is an exact match, -1 otherwise. Used
     * by the tests to make sure entries returned also have the expected expiration, as the equals() method
     * on Entry does not take this into account.
     */
    private class EntryComparator implements Comparator<Entry> {

        public int compare(Entry o1, Entry o2) {
            return o1.key.equals(o2.key) && o1.value.equals(o2.value) && o1.expiration == o2.expiration ? 0 : -1;
        }
    }
    
    public void testGetEntries_flushesDeltasIfRequested() throws IOException {
        cm.setTrackDeltas(true);
        cm.save("a", "b", adv, 100000L, 100000L);
        cm.getEntries("a", true);
        assertEquals(0, cm.getDeltas("a").size());
    }
    
    public void testGetEntries_doesNotFlushDeltasIfNotRequested() throws IOException {
        cm.setTrackDeltas(true);
        cm.save("a", "b", adv, 100000L, 100000L);
        cm.getEntries("a", false);
        List<Entry> deltas = cm.getDeltas("a");
        assertEquals(2, deltas.size());
        checkContains(deltas, new EntryComparator(), 
                              new Entry("PID", adv.getPeerID().toString(), 100000L), 
                              new Entry("Name", adv.getName(), 100000L));
    }
    
    public void testGetEntries_immuneToDeltaClear() throws IOException {
        cm.setTrackDeltas(true);
        cm.save("a", "b", adv, 100000L, 100000L);
        cm.getEntries("a", false);
        
        // will clear deltas for dn=a
        cm.getDeltas("a");
        
        // this should still return all entries
        List<Entry> entries = cm.getEntries("a", false);
        assertEquals(2, entries.size());
        checkContains(entries, new EntryComparator(),
                               new Entry("PID", adv.getPeerID().toString(), 100000L), 
                               new Entry("Name", adv.getName(), 100000L));
    }
    
    public void testGetEntries_returnsExpirationsBasedOnLifetimeOnly() throws IOException {
        cm.save("a", "b", adv, 100000L, 50000L);
        
        List<Entry> entries = cm.getEntries("a", false);
        assertEquals(2, entries.size());
        checkContains(entries, new EntryComparator(),
                new Entry("PID", adv.getPeerID().toString(), 100000L), 
                new Entry("Name", adv.getName(), 100000L));
        
        fakeTimer.currentTime = 40000L;
        
        entries = cm.getEntries("a", false);
        assertEquals(2, entries.size());
        checkContains(entries, new EntryComparator(),
                new Entry("PID", adv.getPeerID().toString(), 60000L), 
                new Entry("Name", adv.getName(), 60000L));
    }
    
    public void testSaveIsolation_differentAreaNames() throws Exception {
        Cm alternateArea = new Cm(createWrappedCache("testArea2"));
        cm.save("a", "b", adv);
        
        assertEquals(1, cm.getRecords("a", NO_THRESHOLD, null).size());
        assertEquals(0, alternateArea.getRecords("a", NO_THRESHOLD, null).size());
        
        assertEquals(2, cm.getEntries("a", false).size());
        assertEquals(0, alternateArea.getEntries("a", false).size());
        
        assertNotNull(cm.getInputStream("a", "b"));
        assertNull(alternateArea.getInputStream("a", "b"));
    }
    
    public void testRemoveIsolation_differentAreaNames() throws Exception {
        Cm alternateArea = new Cm(createWrappedCache("testArea2"));
        cm.save("a", "b", adv);
        alternateArea.remove("a", "b");
        
        // item should still exist
        assertEquals(1, cm.getRecords("a", NO_THRESHOLD, null).size());
    }
    
    public void testConstruct() throws IOException {
    	System.setProperty(Cm.CACHE_IMPL_SYSPROP, getCacheClassName());
        Cm cmFromConstructor = new Cm(testRootDir.toURI(), "testArea");
        assertEquals(getCacheClassName(), cmFromConstructor.getImplClassName());
        cmFromConstructor.stop();
    }
    
    protected <T, U extends Collection<T>> void checkContains(U results, Comparator<T> comparator, T... expectedSet) {
        for (T expected : expectedSet) {
            assertTrue(expected + " not included in set", results.contains(expected));
            if(comparator != null) {
                boolean foundMatch = false;
                for(T item : results) {
                    if(comparator.compare(item, expected) == 0) {
                        foundMatch = true;
                        break;
                    }
                }
                assertTrue("Did not find exact match using comparator for " + expected, foundMatch);
            }
        }
    }
    
    protected <T, U extends Collection<T>> void checkContains(U results, T... expectedSet) {
        checkContains(results, null, expectedSet);
    }
    
    protected boolean containsXOf(HashSet<String> set, int numExpected, String... expectedSet) {
    	int numMatches = 0;
    	for (String expected : expectedSet) {
    		if(set.contains(expected)) {
    			numMatches++;
    		}
		}
    	
    	return numMatches == numExpected;
    }
    
	protected HashSet<String> extractNames(List<InputStream> results)
			throws IOException {
		HashSet<String> names = new HashSet<String>();
		extractNames(results, names);
		return names;
	}
	
	protected void extractNames(List<InputStream> results, Collection<String> output) throws IOException {
		for (InputStream stream : results) {
			output.add(getNameFromResult(stream));
		}
	}
}
