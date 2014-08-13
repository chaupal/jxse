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
package net.jxta.impl.cm.srdi.inmemory;

import net.jxta.impl.cm.Srdi.Entry;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree;
import net.jxta.impl.util.ternary.wild.WildcardTernarySearchTreeImpl;
import net.jxta.impl.util.ternary.wild.WildcardTernarySearchTreeMatchListener;

import net.jxta.logging.Logging;

import net.jxta.peer.PeerID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implementation relies on the use of a Ternary Tree (@see {@link TernarySearchTree})
 * to store the PeerIDs for every key/attribute/value combination (hereafter called a {@link SearchKey}).
 */
public class SearchIndex {

    private final static transient Logger LOG = Logger.getLogger( SearchIndex.class.getName(  ) );

    // The Ternary Tree key index that organises Peers Ids per key (Ternary tree Key)
    // An alternative - tested successfully - is to make the Peer ID URI part of the Ternary Tree Key instead of using an HashMap
    // This implementation relies on the fact that the HasMap is likely faster than the Tree for non partial matches on the Peer ID URI
    private WildcardTernarySearchTree<Map<PeerIdKey, Long>> searchIndex = new WildcardTernarySearchTreeImpl<Map<PeerIdKey, Long>>(  );
    private final String indexName;

    // Counter used for statistics only
    private volatile int registrations;

    public SearchIndex( String indexName ) {

        this.indexName = indexName;
    }

    public void clear(  ) {

        synchronized ( this.searchIndex ) {

            this.searchIndex.deleteTree(  );
        }
    }

    /**
     * DEBUG only as it's VERY expensive to run
     * @return A String containing printable statistics about this Index
     */
    public String getStats(  ) {

        int prefixRegistrations = 0;
        int suffixRegistrations = 0;

        WildcardTernarySearchTreeMatchListener<Map<PeerIdKey, Long>> listener = new WildcardTernarySearchTreeMatchListener<Map<PeerIdKey, Long>>(  ) {

                public void resultFound( String key, Map<PeerIdKey, Long> map ) {

                    SearchIndex.this.registrations += map.size(  );
                }

                public boolean continueSearch(  ) {

                    return true;
                }
            };

        synchronized ( searchIndex ) {

            this.registrations = 0;
            this.searchIndex.walkPrefixTree( listener );
            prefixRegistrations = this.registrations;

            this.registrations = 0;
            this.searchIndex.walkSuffixTree( listener );
            suffixRegistrations = this.registrations;
        }

        return "SearchIndex[" + this.indexName + "]: " + this.searchIndex.getSize(  ) + " search terms\t" + prefixRegistrations +
        " prefix map elements\t" + suffixRegistrations + " suffix map elements.";
    }

    public boolean remove( final SearchKey searchKey, final PeerIdKey peerIdKey ) {

        boolean ret = true;

        synchronized ( this.searchIndex ) {

            Map<PeerIdKey, Long> peerIdsMap = this.searchIndex.get( searchKey.getKey(  ) );

            if ( peerIdsMap != null ) {

                // Clean up the Peer Ids to IndexItems Map
                if ( null == peerIdsMap.remove( peerIdKey ) ) {

                    if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                        LOG.log( Level.WARNING,
                            "[" + this.indexName + "] Remove map value using key: " + peerIdKey + " when map contains no previous value!" );
                    }

                    ret = false;
                }

                // If the map is empty, remove the entry
                if ( peerIdsMap.size(  ) == 0 ) {

                    if ( Logging.SHOW_DEBUG && LOG.isLoggable( Level.FINE ) ) {

                        LOG.fine( "[" + this.indexName + "] Keys Index: removing entry '" + searchKey.getKey(  ) + "'" );
                    }

                    if ( false == this.searchIndex.remove( searchKey.getKey(  ) ) ) {

                        if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                            LOG.log( Level.WARNING,
                                "[" + this.indexName + "] Remove value using key: " + searchKey +
                                " failed!" );
                        }

                        ret = false;
                    }
                }
            }
        }

        return ret;
    }

    public List<Entry> getValueList( final SearchKey searchKey ) {

        // The results List
        List<Entry> entries = Collections.synchronizedList( new ArrayList<Entry>(  ) );

        synchronized ( this.searchIndex ) {

            // Only returns non-expired entries
            long now = TimeUtils.timeNow(  );

            Map<PeerIdKey, Long> map = this.searchIndex.get( searchKey.getKey(  ) );

            if ( map != null ) {

                for ( java.util.Map.Entry<PeerIdKey, Long> entry : map.entrySet(  ) ) {

                    PeerIdKey peerIdKey = entry.getKey(  );
                    Long expiration = entry.getValue(  );

                    if ( expiration.longValue(  ) >= now ) {

                        Entry srdiEntry = new Entry( peerIdKey.getPeerID(  ), expiration );

                        entries.add( srdiEntry );
                    }
                }
            }
        }

        if ( Logging.SHOW_DEBUG && LOG.isLoggable( Level.FINE ) ) {

            LOG.fine( "[" + this.indexName + "] getRecord on  '" + searchKey + "' returned " + entries.size(  ) +
                " item(s) from the index" );
        }

        return entries;
    }

    public Long update( final SearchKey searchKey, final PeerIdKey peerIdKey, final Long expiry ) {

        synchronized ( this.searchIndex ) {

            // Get the map at this index or create the node with the default map
            Map<PeerIdKey, Long> map = this.searchIndex.getOrCreate( searchKey.getKey(  ),
                    Collections.synchronizedMap( new HashMap<PeerIdKey, Long>(  ) ) );

            // Update the value
            return map.put( peerIdKey, expiry );
        }
    }

    public List<PeerID> search( final SearchKey searchKey, final int threshold, final boolean prefixOnly ) {

        // Only returns non-expired entries
        final long now = TimeUtils.timeNow(  );

        // The results: a HashMap of PeerIds to avoid potential redundant Peer Ids - convenient but memory intensive
        final Map<PeerIdKey, PeerID> peerIdsMap = Collections.synchronizedMap( new HashMap<PeerIdKey, PeerID>(  ) );

        // A wild-card tree listener that will stop the search when the threshold is reached
        WildcardTernarySearchTreeMatchListener<Map<PeerIdKey, Long>> listener = new WildcardTernarySearchTreeMatchListener<Map<PeerIdKey, Long>>(  ) {

                public void resultFound( String key, Map<PeerIdKey, Long> map ) {

                    for ( java.util.Map.Entry<PeerIdKey, Long> entry : map.entrySet(  ) ) {

                        if ( peerIdsMap.size(  ) == threshold ) {

                            break;
                        }

                        PeerIdKey peerIdKey = entry.getKey(  );
                        Long expiration = entry.getValue(  );

                        if ( expiration.longValue(  ) >= now ) {

                            peerIdsMap.put( peerIdKey, peerIdKey.getPeerID(  ) );
                        }
                    }
                }

                public boolean continueSearch(  ) {

                    return ( ( threshold < 0 ) || ( peerIdsMap.size(  ) < threshold ) );
                }
            };

        if ( prefixOnly ) {

            // Extract items with matching prefix from the main index
            synchronized ( this.searchIndex ) {

                // Match the prefix - results are collected via the listener
                this.searchIndex.matchPrefix( searchKey.getKey(  ), listener );
            }

            LOG.fine( "[" + this.indexName + "] primary key query on  '" + searchKey.getKey(  ) + "' returned " + peerIdsMap.size(  ) +
                " item(s) from the index" );
        } else {

            //////////////////////////////////////////////////
            //
            // Standard Search
            //
            //////////////////////////////////////////////////

            // Extract matching items from the main index
            synchronized ( this.searchIndex ) {

                // Standard search - results are collected via the listener
                this.searchIndex.search( searchKey.getKey(  ), listener );
            }

            LOG.fine( "[" + this.indexName + "] search query on  '" + searchKey.getKey(  ) + "' returned " + peerIdsMap.size(  ) +
                " item(s) from the index" );
        }

        return Collections.synchronizedList( new ArrayList<PeerID>( peerIdsMap.values(  ) ) );
    }
}
