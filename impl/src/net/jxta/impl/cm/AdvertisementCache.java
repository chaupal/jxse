package net.jxta.impl.cm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.jxta.document.Advertisement;
import net.jxta.protocol.SrdiMessage;

/**
 * The interface that advertisement caches must implement so that they can be plugged into
 * the {@link Cm} cache wrapper.
 * 
 * <p>All AdvertisementCache implementations must also provide two constructors, with the
 * same signatures as {@link Cm#Cm(java.net.URI, String)} and 
 * {@link Cm#Cm(ThreadGroup, java.net.URI, String, long, boolean)}. This allows them
 * to be used by specifying their full class name in the 
 * {@link Cm#CACHE_IMPL_SYSPROP system property defined in Cm}.
 */
public interface AdvertisementCache {

    /**
     * returns all entries that are added since this method was last called
     *
     * @param dn the relative dir name
     * @return SrdiMessage.Entries
     */
    List<SrdiMessage.Entry> getDeltas(String dn);

    /**
     * returns all entries that are cached
     *
     * @param dn          the relative dir name
     * @param clearDeltas if true clears the delta cache
     * @return SrdiMessage.Entries
     */
    List<SrdiMessage.Entry> getEntries(String dn, boolean clearDeltas);

    /**
     * Returns the maximum duration in milliseconds for which this
     * document should cached by those other than the publisher. This
     * value is either the cache lifetime or the remaining lifetime
     * of the document, whichever is less.
     *
     * @param dn contains the name of the folder
     * @param fn contains the name of the file
     * @return number of milliseconds until the file expires or -1 if the
     * file is not recognized or already expired.
     * @throws IOException if an I/O error occurs
     */
    long getExpirationtime(String dn, String fn) throws IOException;

    /**
     * Returns the inputStream of a specified file, in a specified dir
     *
     * @param dn directory name
     * @param fn file name
     * @return The inputStream value
     * @throws IOException if an I/O error occurs
     */
    InputStream getInputStream(String dn, String fn) throws IOException;

    /**
     * Returns the relative time in milliseconds at which the file
     * will expire. Implementations should remove an expired record
     * if appropriate.
     *
     * @param dn contains the name of the folder
     * @param fn contains the name of the file
     * @return the absolute time in milliseconds at which this
     * document will expire. -1 is returned if the file is not
     * recognized or already expired.
     * @throws IOException if an I/O error occurs
     */
    long getLifetime(String dn, String fn) throws IOException;

    /**
     * Returns the relative time in milliseconds at which the file
     * will expire.
     *
     * @param dn contains the name of the folder
     * @param fn contains the name of the file
     * @return the absolute time in milliseconds at which this
     * document will expire. -1 is returned if the file is not
     * recognized or already expired.
     * @throws IOException if an I/O error occurs
     */
    List<InputStream> getRecords(String dn, int threshold, List<Long> expirations, boolean purge) throws IOException;

    /**
     * Remove a file
     *
     * @param dn directory name
     * @param fn file name
     * @throws IOException if an I/O error occurs
     */
    void remove(String dn, String fn) throws IOException;

    /**
     * Restore an advetisement into a byte array.
     *
     * @param dn directory name
     * @param fn file name
     * @return byte [] containing the file
     * @throws IOException if an I/O error occurs
     */
    byte[] restoreBytes(String dn, String fn) throws IOException;
    
    
    /**
     * Stores a StructuredDocument in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param dn         directory name
     * @param fn         file name
     * @param adv        Advertisement to save
     * @param lifetime   Document (local) lifetime in relative ms
     * @param expiration Document (global) expiration time in relative ms
     * @throws IOException Thrown if there is a problem saving the document.
     */
    void save(String dn, String fn, Advertisement adv, long lifetime, long expiration) throws IOException;

    /**
     * Store some bytes in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param dn         directory name
     * @param fn         file name
     * @param data       byte array to save
     * @param lifetime   Document (local) lifetime in relative ms
     * @param expiration Document (global) expiration time in relative ms
     * @throws IOException Thrown if there is a problem saving the document.
     */
    void save(String dn, String fn, byte[] data, long lifetime, long expiration) throws IOException;

    /**
     * Search and recovers documents that contains at least
     * a matching pair of tag/value.
     *
     * @param dn          contains the name of the folder on which to
     * perform the search
     * @param value       contains the value to search on.
     * @param attribute   attribute to search on
     * @param threshold   threshold
     * @param expirations List to contain expirations
     * @return Enumeration containing of all the documents names
     * @throws IOException when an I/O error occurs
     */
    List<InputStream> search(String dn, String attribute, String value, int threshold, List<Long> expirations) throws IOException;

    /**
     * Set whether or not changes to the cache should be tracked, stored and later returned by {@link #getDeltas(String)}.
     * @param trackDeltas when true, changes will be tracked.
     */
    void setTrackDeltas(boolean trackDeltas);

    public void stop() throws IOException;

    public void garbageCollect() throws IOException;
}
