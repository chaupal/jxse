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
import java.util.List;
import net.jxta.peer.PeerID;

/**
 * Interface for all storage backends of Srdi. In addition to correctly implementing this interface, all
 * implementations should also provide a constructor with the same signature as {@link net.jxta.impl.cm.Srdi#Srdi(net.jxta.peergroup.PeerGroup, String)} and
 * a static method matching {@link net.jxta.impl.cm.Srdi#clearSrdi(net.jxta.peergroup.PeerGroup)}. Classes not meeting these requirements cannot be used as
 * backends and Srdi will default to using {@link net.jxta.impl.cm.XIndiceSrdiIndexBackend}.
 * <p>
 * All implementations are expected to pass the full test suite specified in {@link net.jxta.impl.cm.AbstractSrdiIndexBackendTest} and pass the concurrency tests
 * in {@link net.jxta.impl.cm.AbstractSrdiIndexBackendLoadTest}. Load testing can be conducted using {@link net.jxta.impl.cm.AbstractSrdiIndexBackendLoadTest},
 * which is useful for comparative benchmarking with other implementations.
 * <p>
 * In order to specify an alternative SrdiAPI from the default, change the system property {@link net.jxta.impl.cm.Srdi#SRDI_INDEX_BACKEND_SYSPROP} to
 * the full class name of the required alternative, or construct Srdi directly using {@link net.jxta.impl.cm.Srdi#Srdi(SrdiAPI)}.
 */
public interface SrdiAPI {
	
	/**
     * Add a peer to the index, such as queries matching the specified primary key, attribute and value will return
     * this peer as part of their result set.
     *
     * @param primaryKey primary key
     * @param attribute  Attribute String to query on. 
     * @param value      value of the attribute string
     * @param pid        peerid reference
     * @param expiration expiration associated with this entry relative time in milliseconds.
	 * @throws IOException if there was a failure writing the entry to the index 
     */
	void add(String primaryKey, String attribute, String value, PeerID pid, long expiration) throws IOException;
	
	/**
     * retrieves all entries exactly matching the provided primary key, secondary key and value.
     *
     * @param pkey  primary key
     * @param skey  secondary key
     * @param value value
     * @return List of Entry objects
     */
	List<Srdi.Entry> getRecord(String pkey, String skey, String value) throws IOException;
	
	/**
	 * Marks all records added to the index for the specified peer ID for garbage collection. If
	 * add is subsequently called before garbage collection occurs, this removal request will be
	 * canceled.
	 */
	void remove(PeerID pid) throws IOException;
	
	/**
	 * Searches the index for all peers matching the specified primary key, attribute and value query
	 * string.
	 * 
	 * @param primaryKey the primary key for the search.
	 * @param attribute the attribute for the search. If this is null, the search will return all peer
	 * IDs who have records under the primary key that have not expired.
	 * @param value the value for the search. This can include a wildcard at the start, end, or middle
	 * of the string. e.g. "*match", "match*", "*match*", "match*match".  If this is null, the search will
	 * return all Peer IDs under the primary key with a matching attribute.
         *
	 * @param threshold the maximum number of results to return.
	 * @return a list of peer IDs who have records matching the specified criteria of this query.
	 */
	List<PeerID> query(String primaryKey, String attribute, String value, int threshold) throws IOException;
	
	/**
     * Empties the index completely.
     */
	void clear() throws IOException;
	
	/**
	 * Triggers a clean-up of the index, removing all records whose expiry has passed.
	 */
	void garbageCollect() throws IOException;
	
	/**
	 * Terminates this index, releasing any associated resources.
	 */
	void stop();
}
