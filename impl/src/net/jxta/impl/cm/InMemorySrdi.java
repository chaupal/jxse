/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.backport.java.util.NavigableSet;
import net.jxta.impl.util.backport.java.util.TreeMap;
import net.jxta.impl.util.ternary.TernarySearchTreeImpl;
import net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree;
import net.jxta.impl.util.ternary.wild.WildcardTernarySearchTreeImpl;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;

public class InMemorySrdi implements SrdiAPI {

    private final static transient Logger LOG = Logger.getLogger( InMemorySrdi.class.getName() );
    private static final String WILDCARD = "*";
    private static final String REGEX_WILDCARD = ".*";

    // Store of back end objects in use so we can support the static clear functionality
    private static Hashtable<PeerGroup, List<SrdiAPI>> backends = new Hashtable<PeerGroup, List<SrdiAPI>>();

    // Dummy object used in HashMaps
    private static final Object OBJ = new Object();

    // Value counter - used to index ItemIndex objects 
    static long counter = 0;
    
    // Wild card ternary tree of values to peerID lists
    WildcardTernarySearchTree<List<PeerIDItem>> peeridValueIndex = new WildcardTernarySearchTreeImpl<List<PeerIDItem>>();
    
    // The GC Index
    TreeMap expiryIndex = new TreeMap();

    // Used by removal to complete expire time for gc cleanup
    TernarySearchTreeImpl peerRemovalIndex = new TernarySearchTreeImpl();

    // Stopped indicator
    private boolean stopped = false;

    // Usage name for this index
    private final String indexName;

    public InMemorySrdi( PeerGroup group, String indexName ) {

        this.indexName = indexName;

        List<SrdiAPI> idxs = backends.get( group );

        if ( null == idxs ) {

            idxs = new ArrayList<SrdiAPI>( 1 );
            backends.put( group, idxs );
        }

        idxs.add( this );

        peerRemovalIndex.setNumReturnValues( -1 );

        Logging.logCheckedInfo(LOG, "[", ( ( group == null ) ? "none" : group.toString() ), "] : Initialized ", indexName);

    }

    public static void clearSrdi( PeerGroup group ) {

        Logging.logCheckedInfo(LOG, "Clearing SRDIs for ", group );

        List<SrdiAPI> idxs = backends.get( group );

        if ( null != idxs ) {

            for ( SrdiAPI idx : idxs ) {

                try {
                    idx.clear();
                } catch ( IOException e ) {
                    Logging.logCheckedSevere(LOG, "Failed clearing index for group: ", group.getPeerGroupName(), e );
                }

            }

            backends.remove( group );
        }
    }

    private void stoppedCheck() throws IllegalStateException {

        if ( stopped ) {

            throw new IllegalStateException( this.getClass().getName() + " has been stopped!" );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#clear()
     */
    public synchronized void clear() throws IOException {

        if ( !stopped ) {
            Logging.logCheckedWarning(LOG, "Clearing an index that has not been stopped!" );
        }

        this.expiryIndex.clear();
        this.peerRemovalIndex.deleteTree();
        this.peeridValueIndex.deleteTree();

    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#garbageCollect()
     */
    public synchronized void garbageCollect() throws IOException {

        if ( this.stopped ) {

            // Index is stopped... nothing to do
            return;
        }

        Logging.logCheckedInfo(LOG, "gc... " );
        
        Long now = Long.valueOf( TimeUtils.timeNow() );

        NavigableSet exps = this.expiryIndex.navigableKeySet();

        // If we have some work to do...
        if ( !exps.isEmpty() ) {

            if ( ((Long)exps.first()).compareTo( now ) < 0 ) {

                Iterator it = exps.iterator();
                Long exp;

                while ( it.hasNext() ) {

                    exp = (Long)it.next();

                    if ( exp.compareTo( now ) > 0 ) {

                        // We've reached the end of this gc scan
                        break;
                    }

                    Logging.logCheckedFine(LOG, "Expired: ", exp, " is less than:", now );
                    
                    HashMap items = (HashMap)this.expiryIndex.get( exp );

                    ArrayList<IndexItem> removalKeys = new ArrayList<IndexItem>();

                    if( null != items ){
                    	
	                    for ( Object itemObj : items.values() ) {

                                IndexItem item = (IndexItem) itemObj;
	                        List<PeerIDItem> pids = this.peeridValueIndex.find( item.getTreeKey() );
	
	                        if ( !this.peeridValueIndex.delete( item.getTreeKey() ) ) {
	
	                            Logging.logCheckedSevere(LOG, "Failed deleting from PeerId Value Index using key: ", item.getTreeKey() );
	                            
	                        }
	
	                        if ( null != pids && pids.size() > 1 ) {
	
	                            // Other peer IDs sharing the same key
	                            for ( PeerIDItem pid : pids ) {
	
	                                if ( pid.getPeerid().getUniqueValue().toString()
	                                            .equals( item.getIpid().getPeerid().getUniqueValue().toString() ) ) {
	
	                                    pids.remove( pid );
	
	                                    break;
	                                }
	                            }
	
	                            this.peeridValueIndex.insert( item.getTreeKey(), pids );
	                        }
	
	                        Logging.logCheckedFine(LOG, "TST size: ", this.peeridValueIndex.getSize());
	                        
                                removalKeys.add( item );

	                    }
	
	                    items = null;
                    }
                    
                    // Must delete via the iterator
                    it.remove();

                    for ( IndexItem item : removalKeys ) {

                        // Remove from current position in the expire index
                        removeGcItem( item );
                    }

                    removalKeys = null;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#getRecord(java.lang.String, java.lang.String, java.lang.String)
     */
    public synchronized List<Entry> getRecord( String pkey, String skey, String value )
        throws IOException {

        stoppedCheck();

        ArrayList<Entry> entries = new ArrayList<Entry>();

        String treeKey = pkey + "\u0800" + skey + "\u0801" + value;

        List<PeerIDItem> ipids;

        if ( null != ( ipids = this.peeridValueIndex.find( treeKey ) ) ) {

            for ( PeerIDItem ipid : ipids ) {

                if ( ipid.getExpiry() > TimeUtils.timeNow() ) {

                    entries.add( ipid.toEntry() );
                }
            }
        }

        return entries;
    }

    private void processKeyList( List<String> keys, HashMap<PeerID, Object> results, int threshold ) {

        processKeyList( keys, results, threshold, null );
    }

    private void processKeyList( List<String> keys, HashMap<PeerID, Object> results, int threshold, String regex ) {

        int resultCount = 0;

        if( null != keys ){
        	
	        for ( String key : keys ) {
	
	            if ( null != regex ) {
	
	                if ( !key.matches( regex ) ) {
	
	                    continue;
	                }
	            }
	
	            List<PeerIDItem> pids = this.peeridValueIndex.find( key );
	
	            if ( null != pids ) {
	
	                for ( PeerIDItem pid : pids ) {
	
	                    if ( !results.containsKey( pid.getPeerid() ) && ( pid.getExpiry() >= TimeUtils.timeNow() ) ) {
	
	                        results.put( pid.getPeerid(), OBJ );
	                        resultCount++;
	
	                        if ( resultCount == threshold ) {
	
	                            return;
	                        }
	                    }
	                }
	            }
	        }
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#query(java.lang.String, java.lang.String, java.lang.String, int)
     */
    public synchronized List<PeerID> query( String pkey, String skey, String value, int threshold )
        throws IOException {

        stoppedCheck();

        HashMap<PeerID, Object> results = new HashMap<PeerID, Object>();

        if ( null == skey ) {

            // All peer IDs who have records under the primary key that have not expired
            String treeKey = pkey + "\u0800";
            List<String> keys = this.peeridValueIndex.matchPrefix( treeKey, -1 );

            processKeyList( keys, results, threshold );
        } else {

            String treeKey = pkey + "\u0800" + skey + "\u0801" + value;

            if ( value.contains( WILDCARD ) ) {

                // Support for top and tail wild cards, not supported by WildcardTernaryTree
                if ( value.startsWith( WILDCARD ) && value.endsWith( WILDCARD ) ) {

                    // Perform partial match and use string pattern matching for the rest
                    treeKey = pkey + "\u0800" + skey + "\u0801";

                    List<String> keys = this.peeridValueIndex.matchPrefix( treeKey, -1 );

                    processKeyList( keys, results, threshold, value.replace( WILDCARD, REGEX_WILDCARD ) );
                } else {

                    List<String> keys = this.peeridValueIndex.search( treeKey, -1 );

                    processKeyList( keys, results, threshold );
                }
            } else {

                List<PeerIDItem> pids = this.peeridValueIndex.find( treeKey );
                int resultCount = 0;

                if ( null != pids ) {

                    for ( PeerIDItem pid : pids ) {

                        if ( !results.containsKey( pid.getPeerid() ) && ( pid.getExpiry() >= TimeUtils.timeNow() ) ) {

                            results.put( pid.getPeerid(), OBJ );
                            resultCount++;

                            if ( resultCount == threshold ) {

                                break;
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<PeerID>( results.keySet() );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#remove(net.jxta.peer.PeerID)
     */
    @SuppressWarnings( "unchecked" )
    public synchronized void remove( PeerID pid ) throws IOException {

        stoppedCheck();

        HashMap<Long, IndexItem> items = (HashMap<Long, IndexItem>) this.peerRemovalIndex.get( pid.getUniqueValue().toString() );
        if(items == null)  // Nothing to do...
        	return;
        Iterator<Long> it = items.keySet().iterator();
        IndexItem iitem = null;
        ArrayList<IndexItem> removalKeys = new ArrayList<IndexItem>();

        while ( it.hasNext() ) {

            iitem = items.get( it.next() );

            removalKeys.add( iitem );

            // Expire this entry
            iitem.getIpid().setExpiry( -1 );

            // Re-add at new position in gc index
            addGcItem( iitem );
        }

        for ( IndexItem item : removalKeys ) {

            // Remove from current position in the expire index
            removeGcItem( item );
            
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#stop()
     */
    public synchronized void stop() {
        this.stopped = true;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#add(java.lang.String, java.lang.String, java.lang.String, net.jxta.peer.PeerID, long)
     */
    @SuppressWarnings( "unchecked" )
    public synchronized void add( String pkey, String skey, String value, PeerID pid, long expiry ) {

        stoppedCheck();

        Logging.logCheckedFine(LOG, "[", indexName, "] Adding ", pkey, "/", skey, " = \'", value, "\' for ", pid );

        String treeKey = pkey + "\u0800" + skey + "\u0801" + value;

        List<PeerIDItem> pids;
        String pidString = pid.getUniqueValue().toString();

        if ( null != ( pids = this.peeridValueIndex.find( treeKey ) ) ) {

            // Check for a PeerID entry already added
            for ( PeerIDItem item : pids ) {

                String peerIDString = item.getPeerid().getUniqueValue().toString();

                if ( peerIDString.equals( pidString ) ) {

                    removeGcItem( item, peerIDString );
                    pids.remove( item );

                    break;
                }
            }
        } else {

            pids = new ArrayList<PeerIDItem>( 1 );
            this.peeridValueIndex.insert( treeKey, pids );
        }

        PeerIDItem ipid = new PeerIDItem( pid, expiry );

        pids.add( ipid );

        IndexItem idxItem = new IndexItem( ipid, treeKey );

        // Prune out entries that are not for this PeerID
        HashMap<Long, IndexItem> pitems = addGcItem( idxItem );

        // Update or add the peerid removal index entry
        pitems = (HashMap<Long, IndexItem>) this.peerRemovalIndex.get( pidString );

        if ( null != pitems ) {

            this.peerRemovalIndex.remove( pidString );
        } else {

            pitems = new HashMap<Long, IndexItem>( 1 );
        }

        pitems.put( idxItem.getId(), idxItem );
        this.peerRemovalIndex.put( pidString, pitems );
    }

    // Less efficient Gc item removal using enumeration of gcItems
    private boolean removeGcItem( PeerIDItem piitem, String peerIDString ) {

        boolean ret = false;
        Long oldExpiry = Long.valueOf( piitem.getExpiry() );

        // Remove from current position in the expire index
        HashMap gcItems = (HashMap) this.expiryIndex.get( oldExpiry );

        if( null != gcItems ){
        	
            Long id = null;

	        for ( Object iitemObj : gcItems.values() ) {
	        	IndexItem iitem = (IndexItem) iitemObj;
	            if ( iitem.getIpid().getPeerid().getUniqueValue().toString().equals( peerIDString ) ) {
	
	                id = iitem.getId();
	
	                break;
	            }
	        }
	        
	        if ( null != id ) {
	
	            if ( null != gcItems.remove( id ) ) {
	
	                ret = true;
	            }
	        }
	
	        if ( gcItems.isEmpty() ) {
	
	            this.expiryIndex.remove( oldExpiry );
	        }
        }
        
        return ret;
    }

    private boolean removeGcItem( IndexItem iitem ) {

        boolean ret = false;
        Long oldExpiry = Long.valueOf( iitem.getIpid().getExpiry() );

        // Remove from current position in the expire index
        HashMap gcItems = (HashMap) this.expiryIndex.get( oldExpiry );

        if ( null != gcItems ) {

            gcItems.remove( iitem.getId() );
            ret = true;

            if ( gcItems.isEmpty() ) {

                this.expiryIndex.remove( oldExpiry );
            }
        }

        return ret;
    }

    private HashMap<Long, IndexItem> addGcItem( IndexItem iitem ) {

        Long expiryKey = Long.valueOf( iitem.getIpid().getExpiry() );
        HashMap gcItems = (HashMap) this.expiryIndex.get( expiryKey );

        if ( null == gcItems ) {

            gcItems = new HashMap( 1 );
            this.expiryIndex.put( expiryKey, gcItems );
        }

        gcItems.put( iitem.getId(), iitem );

        return gcItems;
    }

    private static class PeerIDItem {

        private PeerID peerid;
        private long expiry;

        PeerIDItem( PeerID peerid, long ttl ) {

            this.peerid = peerid;
            this.expiry = TimeUtils.toAbsoluteTimeMillis( ttl );
        }

        public PeerID getPeerid() {

            return peerid;
        }

        public long getExpiry() {

            return expiry;
        }

        public void setExpiry( long expiry ) {

            this.expiry = expiry;
        }

        Entry toEntry() {

            return new Entry( this.peerid, this.expiry );
        }
    }

    private static class IndexItem {

        private Long id;
        private String treeKey;
        PeerIDItem ipid;

        public IndexItem( PeerIDItem ipid, String treeKey ) {

            this.ipid = ipid;
            this.id = InMemorySrdi.counter++;
            this.treeKey = treeKey;
        }

        public String getTreeKey() {

            return treeKey;
        }

        public Long getId() {

            return id;
        }

        public PeerIDItem getIpid() {

            return ipid;

        }

    }

}
