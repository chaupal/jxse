/*
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
package net.jxta.impl.util.ternary.wild;

import java.util.List;


public interface WildcardTernarySearchTree<T> {

    /**
     * Get the value E stored at the node referenced with <code>key</code>
     * If the node does not exist, it is created and the value <code>valueIfCreate</code> is stored
     *
     * @param key A string that indexes the object to be stored.
     * @param valueIfCreate The value to give to the node if is created
     * @return The value at the node
     */
    public T getOrCreate( final String key, final T valueIfCreate );

    /**
     * Stores a new string key and its value in the tree.
     *
     * @param key The string key of the object
     * @param value The value that need to be stored corresponding to the given key.
     * @throws IllegalStateException if there is a conflict.
     */
    public void put( final String key, final T value )
        throws IllegalStateException;

    /**
     * Remove a key and its associated value from the tree.
     *
     * @param key The key of the node that need to be deleted
     * @return true if deleted
     */
    public boolean remove( final String key );

    /**
     * Get a value based on its corresponding key.
     *
     * @param key The key for which to search the tree.
     * @return The value corresponding to the key. null if it can not find the key
     */
    public T get( final String key );

    /**
     * Check if the tree contains any entry corresponding to the given key.
     *
     * @param key The key that need to be searched in the tree.
     * @return return true if the key is present in the tree otherwise false
     */
    public boolean contains( final String key );

    /**
     * Search for all the values for a match against the given search term.
     *
     * @param term The search term can contain one or more wild card character: *.
     *  The wild cards can be placed at the beginning, the end or any point within the search term.
     * @param listener A {@link WildcardTernarySearchTreeMatchListener} which will receive all data as it is found
     *  and can stop the search at any time
     * @return The list of values that match the given search term
     * @throws IllegalArgumentException if the search term does not match the expected format
     */
    public List<T> search( final String term, final WildcardTernarySearchTreeMatchListener<T> listener )
        throws IllegalArgumentException;

    /**
     * Return the size of the tree
     *
     * @return the size of the tree
     */
    public long getSize(  );

    /**
     * Returns a list of all values in the tree that begin with prefix. Only keys for nodes having non-null data are included in the list.
     *
     * @param prefix Each key returned from this method will begin with the characters in prefix.
     * @param listener A {@link WildcardTernarySearchTreeMatchListener} which will receive all data as it is found
     *  and can stop the search at any time
     * @return The list of values that match the given prefix
     */
    public List<T> matchPrefix( final String prefix, final WildcardTernarySearchTreeMatchListener<T> listener );

    /**
     * Allow all heap space used by this tree to be garbage collected
     */
    public void deleteTree(  );

    /**
     * Dumps the prefix and suffix trees to stdout
     */
    public void printTree(  );

    /**
     * Traverse the entire prefix tree.calling the listener for each data node found
     * Used for tree inspection
     * @param listener A {@link WildcardTernarySearchTreeMatchListener} which will receive all data as it is found
     *  and can stop the search at any time
     */
    public void walkPrefixTree( final WildcardTernarySearchTreeMatchListener<T> listener );

    /**
    * Traverse the entire suffix tree.calling the listener for each data node found
    * Used for tree inspection
    * @param listener A {@link WildcardTernarySearchTreeMatchListener} which will receive all data as it is found
    *  and can stop the search at any time
    */
    public void walkSuffixTree( final WildcardTernarySearchTreeMatchListener<T> listener );
}
