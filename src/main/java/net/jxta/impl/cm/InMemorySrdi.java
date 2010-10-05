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

import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.impl.cm.srdi.inmemory.GcIndex;
import net.jxta.impl.cm.srdi.inmemory.GcKey;
import net.jxta.impl.cm.srdi.inmemory.PeerIdIndex;
import net.jxta.impl.cm.srdi.inmemory.PeerIdKey;
import net.jxta.impl.cm.srdi.inmemory.SearchIndex;
import net.jxta.impl.cm.srdi.inmemory.SearchKey;
import net.jxta.impl.util.TimeUtils;

import net.jxta.logging.Logging;

import net.jxta.peer.PeerID;

import net.jxta.peergroup.PeerGroup;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of the Srdi index that is stored in Memory
 *
 * Removing items from the Index is made using a Garbage Collection process.
 *
 * Two additional indexes are maintained along the search index to speed up searches based on {@link PeerID}s
 * and the Garbage Collection run. Those indexes are {@link Map} based.
 *
 * Due to its peculiar design, the calculation of the {@link PeerID} hashcode is extremely expensive;
 * This is why this Implementation does not use the {@link Entry} nor the {@link PeerID} objects directly
 * See comments in {@link PeerIdKey}
 *
 * @author Bruno Grieder (bruno.grieder@amalto.com) & Simon Temple (simon.temple@amalto.com)
 *
 */
public class InMemorySrdi implements SrdiAPI {

    private final static transient Logger LOG = Logger.getLogger( InMemorySrdi.class.getName(  ) );

    // Store of back end objects in use so we can support the static clear functionality
    private static Hashtable<PeerGroup, List<SrdiAPI>> backends = new Hashtable<PeerGroup, List<SrdiAPI>>(  );

    // Three in-memory indexes used to store, search and garbage collect the SRDI
    private GcIndex gcIndex = null;
    private PeerIdIndex peerIdIndex = null;
    private SearchIndex searchIndex = null;

    // Used as a lock to ensure all operations on the above three indexes are thread safe
    private final Object indexLock = new Object(  );

    // Stopped indicator
    private volatile boolean stopped = false;

    // Usage name for this index
    private final String indexName;

    public InMemorySrdi( PeerGroup group, String indexName ) {

        // The index name is only used for logging
        this.indexName = ( ( group == null ) ? "none" : ( ( group.getPeerGroupName(  ) == null ) ? "NPG" : group.getPeerGroupName(  ) ) ) +
            ":" + indexName;

        List<SrdiAPI> idxs = null;

        synchronized ( backends ) {

            if ( null != group ) {

                idxs = backends.get( group );
            }

            if ( null == idxs ) {

                idxs = new ArrayList<SrdiAPI>( 1 );

                if ( null != group ) {

                    backends.put( group, idxs );
                }
            }

            idxs.add( this );
            this.gcIndex = new GcIndex( this.indexName );
            this.peerIdIndex = new PeerIdIndex( this.indexName );
            this.searchIndex = new SearchIndex( this.indexName );
        }

        if ( Logging.SHOW_INFO && LOG.isLoggable( Level.INFO ) ) {

            LOG.info( "[" + ( ( group == null ) ? "none" : group.toString(  ) ) + "] : Initialized " + indexName );
        }
    }

    public static void clearSrdi( PeerGroup group ) {

        if ( Logging.SHOW_INFO && LOG.isLoggable( Level.INFO ) ) {

            LOG.info( "Clearing SRDIs for " + group );
        }

        SrdiAPI[] indexes = null;

        synchronized ( backends ) {

            List<SrdiAPI> idxs = backends.get( group );

            if ( idxs != null ) {

                indexes = idxs.toArray( new SrdiAPI[ idxs.size(  ) ] );
            }
        }

        if ( null != indexes ) {

            for ( SrdiAPI idx : indexes ) {

                try {

                    idx.clear(  );
                } catch ( IOException e ) {

                    if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                        LOG.log( Level.SEVERE, "Failed clearing index for group: " + group.getPeerGroupName(  ), e );
                    }
                }
            }

            synchronized ( backends ) {

                if ( null == backends.remove( group ) ) {

                    if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                        LOG.log( Level.SEVERE, "Failed removing index instance: " + group );
                    }
                }
            }
        }
    }

    private void stoppedCheck(  ) throws IllegalStateException {

        if ( stopped ) {

            throw new IllegalStateException( this.getClass(  ).getName(  ) + " has been stopped!" );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#clear()
     */
    public void clear(  ) throws IOException {

        if ( !stopped ) {

            if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                LOG.warning( "Clearing an index that has not been stopped!" );
            }
        }

        try {

            synchronized ( indexLock ) {

                this.gcIndex.clear(  );
                this.peerIdIndex.clear(  );
                this.searchIndex.clear(  );
            }
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#garbageCollect()
     */
    public void garbageCollect(  ) throws IOException {

        if ( this.stopped ) {

            // Index is stopped... nothing to do
            return;
        }

        try {

            Long now = Long.valueOf( TimeUtils.timeNow(  ) );

            //A small logging counter
            long counter = 0;

            Long[] expirations = this.gcIndex.getAllKeys(  );

            // Now loop over extracted items in a non synchronized (e.g. non blocking) loop
            for ( int i = 0; i < expirations.length; i++ ) {

                Long expiration = expirations [ i ];

                if ( expiration.compareTo( now ) > 0 ) {

                    // This entry is not yet expired, we are done, exit the GC
                    if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

                        LOG.fine( "[" + this.indexName + "] GC: cleared " + counter + " item(s) from the index" );
                    }

                    printStatus(  );

                    return;
                }

                // This entry is expired, process it...
                synchronized ( this.indexLock ) {

                    // Remove and recover the items
                    Set<GcKey> items = gcIndex.remove( expiration );

                    // See if we have any item to process
                    if ( items != null ) {

                        for ( GcKey gcKey : items ) {

                            // Recover the search key
                            SearchKey searchKey = gcKey.getSearchKey(  );

                            if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

                                LOG.fine( "[" + this.indexName + "] GC: using tree key " + searchKey );
                            }

                            // Clean-up the search index
                            this.searchIndex.remove( searchKey, gcKey.getPeerIdKey(  ) );

                            // Clean-up the peers Index
                            this.peerIdIndex.remove( gcKey.getPeerIdKey(  ), searchKey );
                        }

                        // Increment the logging counter
                        counter++;
                    } else {

                        // This *is* possible if the index item was removed via a call to add(), below, after we called getAllKeys() but before we
                        // iterated down to the expiration key to process the entry.  Just log it and carry on...
                        if ( Logging.SHOW_FINER && LOG.isLoggable( Level.FINER ) ) {

                            LOG.finer( "[" + this.indexName + "] GC: Removing GC Index using: " + expiration +
                                " returned a null set.  Assuming it's already been removed via a concurrent add()." );
                        }
                    }
                }
            }

            if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

                LOG.fine( "[" + this.indexName + "] GC: cleared ALL ( e.g." + counter + " ) item(s) from the index" );
            }

            printStatus(  );
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] GC: Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }

    private void printStatus(  ) {

        if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

            StringBuffer sb = new StringBuffer(  );

            sb.append( "\n" );
            sb.append( 
                "------------------------------------------------------------------------------------------------------------------------\n" );
            sb.append( " In Memory SRDI Status: " );
            sb.append( "[" );
            sb.append( this.indexName );
            sb.append( "]\n" );
            sb.append( 
                "------------------------------------------------------------------------------------------------------------------------\n" );

            synchronized ( indexLock ) {

                sb.append( this.gcIndex.getStats(  ) );
                sb.append( "\n\n" );
                sb.append( this.peerIdIndex.getStats(  ) );
                sb.append( "\n" );
                sb.append( this.searchIndex.getStats(  ) );
                sb.append( "\n" );
            }

            sb.append( 
                "------------------------------------------------------------------------------------------------------------------------\n" );

            LOG.fine( sb.toString(  ) );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#getRecord(java.lang.String, java.lang.String, java.lang.String)
     */
    public List<Entry> getRecord( String primaryKey, String attribute, String value )
        throws IOException {

        stoppedCheck(  );

        try {

            return this.searchIndex.getValueList( new SearchKey( primaryKey, attribute, value ) );
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#query(java.lang.String, java.lang.String, java.lang.String, int)
     */
    public List<PeerID> query( final String primaryKey, final String attribute, final String value, final int threshold )
        throws IOException {

        stoppedCheck(  );

        if ( null == primaryKey ) {

            throw new IOException( "[" + indexName + "] Null primary key is not supported in query." );
        }

        try {

            // The key we want to extract from the Ternary Tree
            SearchKey searchKey = new SearchKey( primaryKey, attribute, value );

            //////////////////////////////////////////////////
            //
            // No Attribute 
            // From the JXTA docs: if [attribute] is null, 
            // the search will return all peer IDs who have records 
            // under the primary key that have not expired
            // -->Match Prefix on the shortened key
            //
            //////////////////////////////////////////////////
            if ( null == attribute ) {

                return this.searchIndex.search( searchKey, threshold, true );
            }

            //////////////////////////////////////////////////
            //
            // Standard Search
            //
            //////////////////////////////////////////////////
            return this.searchIndex.search( searchKey, threshold, false );
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#remove(net.jxta.peer.PeerID)
     */
    public void remove( PeerID pid ) throws IOException {

        stoppedCheck(  );

        try {

            // Peer Ids are unique identified by their Unique Value
            PeerIdKey peerIdKey = new PeerIdKey( pid );

            // A simple logging counter
            long counter = 0;

            // Find the entry in the Peer Index
            Map<SearchKey, Long> entries = this.peerIdIndex.get( peerIdKey );

            if ( entries == null ) { // Nothing to do...

                if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

                    LOG.fine( "[" + indexName + "]  Did not Remove:  Peer ID " + peerIdKey + ": is not in the index" );
                }

                return;
            }

            synchronized ( indexLock ) {

                for ( java.util.Map.Entry<SearchKey, Long> entry : entries.entrySet(  ) ) {

                    SearchKey searchKey = entry.getKey(  );
                    Long expiration = entry.getValue(  );

                    // Removal is performed by expiration of the Entry e.g; moving the GC Item to an expiration of -1
                    // Prevent any modification while me manipulated the expiration entry of the item
                    GcKey gcKey = new GcKey( searchKey, peerIdKey );

                    // Re-add to the GC index at an expired position
                    this.gcIndex.add( -1L, gcKey );

                    // Update the search index
                    this.searchIndex.update( searchKey, peerIdKey, -1L );

                    // Remove old gc entry
                    this.gcIndex.remove( expiration, gcKey );

                    // Increment logging counter
                    counter++;
                }
            }

            if ( Logging.SHOW_FINEST && LOG.isLoggable( Level.FINEST ) ) {

                LOG.finest( "[" + indexName + "]  Removing  Peer ID '" + peerIdKey + "' led to the expiration of '" + counter +
                    "' item(s) in the index" );
            }
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#stop()
     */
    public void stop(  ) {

        this.stopped = true;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.cm.SrdiAPI#add(java.lang.String, java.lang.String, java.lang.String, net.jxta.peer.PeerID, long)
     */
    public void add( String primaryKey, String attribute, String value, PeerID pid, long expiry )
        throws IOException {

        stoppedCheck(  );

        try {

            long expiration = TimeUtils.toAbsoluteTimeMillis( expiry );

            SearchKey searchKey = new SearchKey( primaryKey, attribute, value );

            PeerIdKey peerIdKey = new PeerIdKey( pid );

            GcKey gcKey = new GcKey( searchKey, peerIdKey );

            if ( Logging.SHOW_FINEST && LOG.isLoggable( Level.FINEST ) ) {

                LOG.finest( "[" + indexName + "] Adding / Updating " + searchKey + " for " + peerIdKey + " expires in: " +
                    ( expiration - TimeUtils.timeNow(  ) ) + "ms (at: " + expiration + ")" );
            }

            synchronized ( indexLock ) {

                // Add it (back) at the proper location
                this.gcIndex.add( expiration, gcKey );

                // Add/replace it in the peers ID Index
                this.peerIdIndex.update( peerIdKey, searchKey, expiration );

                // Finally, add/replace it in the search index with FULL key

                // Create a default map in case this node does not exist
                Long previousExpiration = searchIndex.update( searchKey, peerIdKey, expiration );

                // Remove the original entry from the GC index if it existed (as long as it's not the same expiration)
                if ( ( previousExpiration != null ) && ( previousExpiration != expiration ) ) {

                    this.gcIndex.remove( previousExpiration, gcKey );
                }
            }
        } catch ( Throwable th ) {

            if ( Logging.SHOW_SEVERE && LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "[" + this.indexName + "] Unexpected exception encountered!", th );
            }

            throw new IOException( th );
        }
    }
}
