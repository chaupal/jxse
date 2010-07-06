package net.jxta.impl.cm;

import static org.junit.Assert.*;
import net.jxta.impl.cm.SrdiIndex.Entry;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;

import org.jmock.Expectations;
import org.junit.Test;

import java.util.List;


public class InMemorySrdiIndexBackendTest extends AbstractSrdiIndexBackendTest {
    
    @Override
    public Expectations createExpectationsForConstruction_withPeerGroup_IndexName( final PeerGroup mockGroup, final PeerGroupID groupId,
        String groupName ) {

        return new Expectations() {{
            ignoring(mockGroup).getPeerGroupName(); will(returnValue("testGroup"));
        }};
    }

    @Override
    public String getBackendClassname(  ) {
        return InMemorySrdiIndexBackend.class.getName(  );
    }

    @Override
    protected SrdiIndexBackend createBackend( PeerGroup group, String indexName ) {
        return new InMemorySrdiIndexBackend( group, indexName );
    }

    /**
     * Checks that expired entries recorded under the same primary key, attribute and
     * value combination are removed on a call to add.
     * <p>
     * It may not make sense for all implementations to remove expired entries on add -
     * it is done in the XIndice implementation simply because it is convenient. It
     * should be possible to copy this test to the test class of an alternate implementation
     * if it too should remove expired entries.
     */
    @Test
    public void testAdd_removesExpiredEntries(  ) throws Exception {

        srdiIndex.add( "a", "b", "c", PEER_ID, 10000L );
        srdiIndex.add( "a", "b", "c", PEER_ID_2, 5000L );

        // this entry should not be deleted automatically as it is under a different
        // (pkey, attr, value) combination.
        srdiIndex.add( "a", "d", "x", PEER_ID_2, 5000L );

        clock.currentTime = 8000L;
        srdiIndex.add( "a", "b", "c", PEER_ID_3, 12000L );

        List<Entry> record = srdiIndex.getRecord( "a", "b", "c" );

        assertNotNull( record );
        assertEquals( 2, record.size(  ) );
        assertContains( record, new Entry( PEER_ID, 10000L ), new Entry( PEER_ID_3, 20000L ) );
    }

    // Never will survive a restart
    @Test
    public void testDataSurvivesRestart(  ) throws Exception {

        assertTrue( true );
    }

    // Index content is inexplicably linked to the in-memory index object.  Creating a new in-memory model
    // does not clone the data model from another index.  Therefore this test is modified to prove isolation
    // without the need to implement data model cloning
    @Test
    public void testClearViaStatic_groupsWithSameStoreAreIsolated(  ) {

        srdiIndex.add( "a", "b", "c", PEER_ID, 1000L );
        srdiIndexForGroup2.add( "a", "b", "c", PEER_ID, 1000L );

        srdiIndex.stop(  );
        //srdiIndexForGroup2.stop();
        SrdiIndex.clearSrdi( group1 );

        SrdiIndex group1IndexRestarted = new SrdiIndex( group1, "testIndex" );

        //SrdiIndex group2IndexRestarted = new SrdiIndex(group2, "testIndex");
        assertTrue( group1IndexRestarted.query( "a", "b", "c", -1 ).isEmpty(  ) );
        // assertContains(group2IndexRestarted.query("a", "b", "c", NO_THRESHOLD), PEER_ID);
        assertContains( srdiIndexForGroup2.query( "a", "b", "c", -1 ), PEER_ID );
    }
}
