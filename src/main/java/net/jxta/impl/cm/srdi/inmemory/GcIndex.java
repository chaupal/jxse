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

import net.jxta.impl.util.TimeUtils;

import net.jxta.logging.Logging;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


// The GC Index: this index organises the Entries by expiration time and is used for Garbage Collection 
public class GcIndex {

    private final static transient Logger LOG = Logger.getLogger( GcIndex.class.getName(  ) );
    private Map<Long, Set<GcKey>> gcIndex = Collections.synchronizedMap( new TreeMap<Long, Set<GcKey>>(  ) );
    private final String indexName;

    public GcIndex( final String indexName ) {

        this.indexName = indexName;
    }

    public void clear(  ) {

        this.gcIndex.clear(  );
    }

    /**
     * DEBUG only as it's VERY expensive to run
     * @return A String containing printable statistics about this Index
     */
    public String getStats(  ) {

        int registrations = 0;
        Set<Long> expList = this.gcIndex.keySet(  );
        Collection<Set<GcKey>> sets = this.gcIndex.values(  );

        for ( Set<GcKey> set : sets ) {

            registrations += set.size(  );
        }

        long now = TimeUtils.timeNow(  );
        long hour = now + TimeUtils.ANHOUR;
        long day = now + TimeUtils.ADAY;
        long week = now + TimeUtils.AWEEK;
        long month = now + ( TimeUtils.AWEEK * 4 );
        long year = now + TimeUtils.AYEAR;

        int negative = 0;

        int greaterThanOrEqualToZeroAndLessThanAnHour = 0;
        int greaterThanOrEqualToAnHourAndLessThanADay = 0;
        int greaterThanOrEqualToADayAndLessThanAWeek = 0;
        int greaterThatOrEqualToAWeekAndLessThanAMonth = 0;
        int greaterThanOrEqualToAMonthAndLessThanAYear = 0;
        int greaterThanOrEqualToAYear = 0;

        for ( Long exp : expList ) {

            if ( exp >= year ) {

                greaterThanOrEqualToAYear++;
            } else {

                if ( exp >= month ) {

                    greaterThanOrEqualToAMonthAndLessThanAYear++;
                } else {

                    if ( exp >= week ) {

                        greaterThatOrEqualToAWeekAndLessThanAMonth++;
                    } else {

                        if ( exp >= day ) {

                            greaterThanOrEqualToADayAndLessThanAWeek++;
                        } else {

                            if ( exp >= hour ) {

                                greaterThanOrEqualToAnHourAndLessThanADay++;
                            } else {

                                if ( exp >= 0 ) {

                                    greaterThanOrEqualToZeroAndLessThanAnHour++;
                                } else {

                                    negative++;
                                }
                            }
                        }
                    }
                }
            }
        }

        return "GcIndex    [" + this.indexName + "]: " + expList.size(  ) + " expiry times\t" + registrations + " map elements.\n" +
        "           <0: " + negative + ", <hour: " + greaterThanOrEqualToZeroAndLessThanAnHour + ", <day: " +
        greaterThanOrEqualToAnHourAndLessThanADay + ", <week: " + greaterThanOrEqualToADayAndLessThanAWeek +
        ", <month: " + greaterThatOrEqualToAWeekAndLessThanAMonth + ", <year: " +
        greaterThanOrEqualToAMonthAndLessThanAYear + ", >=year: " + greaterThanOrEqualToAYear;
    }

    public Long[] getAllKeys(  ) {

        Long[] expirations = null;

        Set<Long> expList = this.gcIndex.keySet(  );

        expirations = expList.toArray( new Long[ expList.size(  ) ] );

        return expirations;
    }

    public Set<GcKey> remove( final Long expiry ) {

        return this.gcIndex.remove( expiry );
    }

    public boolean remove( final Long expiry, final GcKey gcKey ) {

        Set<GcKey> gcKeys = this.gcIndex.get( expiry );

        if ( gcKeys != null ) {

            if ( false == gcKeys.remove( gcKey ) ) {

                if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                    LOG.log( Level.WARNING,
                        "[" + this.indexName + "] Remove set value using key: " + gcKey + " when set contains no previous value!" );
                }
            }

            if ( gcKeys.size(  ) == 0 ) {

                // No more entry to expire... remove entry form the GC Index
                if ( Logging.SHOW_FINE && LOG.isLoggable( Level.FINE ) ) {

                    LOG.fine( "[" + this.indexName + "] GC Index: removing entry '" + expiry + "'" );
                }

                if ( null == remove( expiry ) ) {

                    if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                        LOG.log( Level.WARNING,
                            "[" + this.indexName + "] Remove value using key: " + expiry + " when index contains no previous value!" );
                    }
                }
            }
        }

        return true;
    }

    /**
     * Adds an IndexItem to the main GC expiration Index
     *
     * Items are "removed" by setting their expiration time to -1 and
     * updating their entry in this index so that they can be GCed on
     * the next Garbage Collector run
     *
     * @param value The {@link GcKey} to add to the Garbage Collector
     * @param expiry The absolute time at which this item will expire
     */
    public boolean add( final Long expiry, final GcKey value ) {

        Set<GcKey> gcKeys;

        gcKeys = this.gcIndex.get( expiry );

        if ( null == gcKeys ) {

            gcKeys = Collections.synchronizedSet( new HashSet<GcKey>(  ) );

            this.gcIndex.put( expiry, gcKeys );
        }

        gcKeys.add( value );

        return true;
    }
}
