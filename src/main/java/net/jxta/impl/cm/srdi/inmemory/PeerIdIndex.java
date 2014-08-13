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

import net.jxta.logging.Logging;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// The Peer IDs index: this index organises the keys per Peer ID URI
public class PeerIdIndex {

    private final static transient Logger LOG = Logger.getLogger( PeerIdIndex.class.getName(  ) );
    private Map<PeerIdKey, Map<SearchKey, Long>> peerIdsIndex = Collections.synchronizedMap( new HashMap<PeerIdKey, Map<SearchKey, Long>>(  ) );
    private final String indexName;

    public PeerIdIndex( String indexName ) {

        this.indexName = indexName;
    }

    public void clear(  ) {

        synchronized ( this.peerIdsIndex ) {

            peerIdsIndex.clear(  );
        }
    }

    /**
     * DEBUG only as it's VERY expensive to run
     * @return A String containing printable statistics about this Index
     */
    public String getStats(  ) {

        int registrations = 0;
        Collection<Map<SearchKey, Long>> maps = this.peerIdsIndex.values(  );

        for ( Map<SearchKey, Long> map : maps ) {

            registrations += map.size(  );
        }

        return "PeerIdIndex[" + this.indexName + "]: " + this.peerIdsIndex.size(  ) + " peer ids    \t" + registrations + " map elements.";
    }

    public Map<SearchKey, Long> get( final PeerIdKey peerIdKey ) {

        // Find the entry in the Peer Index
        Map<SearchKey, Long> entries = null;

        entries = this.peerIdsIndex.get( peerIdKey );

        return entries;
    }

    public boolean remove( final PeerIdKey peerIdKey, final SearchKey searchKey ) {

        boolean ret = true;

        Map<SearchKey, Long> keysMap = this.peerIdsIndex.get( peerIdKey );

        if ( keysMap != null ) {

            // Clean up the key
            if ( null == keysMap.remove( searchKey ) ) {

                if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                    LOG.log( Level.WARNING,
                        "[" + this.indexName + "] Remove map value using key: " + searchKey + " when map contains no previous value!" );
                }

                ret = false;
            }

            // If the map is empty, remove the entry
            if ( keysMap.size(  ) == 0 ) {

                if ( Logging.SHOW_DEBUG && LOG.isLoggable( Level.FINE ) ) {

                    LOG.fine( "[" + this.indexName + "] Peer Ids index: removing entry '" + peerIdKey + "' from main Index" );
                }

                if ( null == this.peerIdsIndex.remove( peerIdKey ) ) {

                    if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                        LOG.log( Level.WARNING,
                            "[" + this.indexName + "] Remove value using key: " + peerIdKey + " when index contains no previous value!" );
                    }

                    return false;
                }
            }
        }

        return ret;
    }

    public boolean update( final PeerIdKey peerIdKey, final SearchKey searchKey, final Long expiry ) {

        Map<SearchKey, Long> map = null;

        map = this.peerIdsIndex.get( peerIdKey );

        if ( map == null ) {

            map = Collections.synchronizedMap( new HashMap<SearchKey, Long>(  ) );
            this.peerIdsIndex.put( peerIdKey, map );
        }

        map.put( searchKey, expiry );

        return true;
    }
}
