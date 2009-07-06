package net.jxta.impl.cm;

import java.io.IOException;
import java.util.List;

import net.jxta.peer.PeerID;

/**
 * Interface for all storage backends of SrdiIndex. In addition to correctly implementing this interface, all
 * implementations should also provide constructors matching the signatures of {@link net.jxta.impl.cm.SrdiIndex#SrdiIndex(net.jxta.peergroup.PeerGroup, String)} and
 * {@link net.jxta.impl.cm.SrdiIndex#SrdiIndex(net.jxta.peergroup.PeerGroup, String, long)} and a static method matching 
 * {@link net.jxta.impl.cm.SrdiIndex#clearSrdi(net.jxta.peergroup.PeerGroup)}. Classes not meeting these requirements cannot be used as
 * backends and SrdiIndex will default to using {@link net.jxta.impl.cm.XIndiceSrdiIndexBackend}.
 * <p>
 * All implementations are expected to pass the full test suite specified in {@link net.jxta.impl.cm.AbstractSrdiIndexBackendTest}.
 * <p>
 * In order to specify an alternative SrdiIndexBackend from the default, change the system property {@link net.jxta.impl.cm.SrdiIndex#SRDI_INDEX_BACKEND_SYSPROP} to
 * the full class name of the required alternative, or construct SrdiIndex directly using {@link net.jxta.impl.cm.SrdiIndex#SrdiIndex(SrdiIndexBackend)}.
 */
public interface SrdiIndexBackend {
	
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
	List<SrdiIndex.Entry> getRecord(String pkey, String skey, String value) throws IOException;
	
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
	 * of the string. e.g. "*match", "match*", "*match*", "match*match".
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
