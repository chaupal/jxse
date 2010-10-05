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
package net.jxta.impl.util.ternary.wild;

import net.jxta.impl.util.ternary.TernarySearchTree;
import net.jxta.impl.util.ternary.TernarySearchTreeImpl;
import net.jxta.impl.util.ternary.TernarySearchTreeMatchListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * A Ternary Tree that provides wildcard search capabilities
 *
 * @author Simon Temple (simon.temple@amalto.com)
 * @param <T>
 */
public class WildcardTernarySearchTreeImpl<T> implements WildcardTernarySearchTree<T> {

    private static final Logger LOG = Logger.getLogger( WildcardTernarySearchTreeImpl.class.getName(  ) );
    private TernarySearchTree<T> prefix = new TernarySearchTreeImpl<T>(  );

    // Duplicate of the prefix tree except all keys are ** reversed **
    private TernarySearchTree<T> suffix = new TernarySearchTreeImpl<T>(  );
    private volatile long size = 0;
    private volatile char wildcard;

    /**
     * Build a Wild Card Ternary Search Tree with '*' as wildcard
     */
    public WildcardTernarySearchTreeImpl(  ) {
        this( '*' );
    }

    /**
     * Build a Wild Card Ternary Search Tree
     * @param wildcard The character to use as Wild Card
     */
    public WildcardTernarySearchTreeImpl( char wildcard ) {

        this.wildcard = wildcard;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#contains(java.lang.String)
     */
    public boolean contains( final String key ) {

        synchronized ( prefix ) {

            return ( null != prefix.get( key ) );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#remove(java.lang.String)
     */
    public boolean remove( final String key ) {

        T suffixDeleted = null;
        T prefixDeleted = null;

        // StringBuilder faster than StringBuffer...but not synchronised
        String revKey = new StringBuilder( key ).reverse(  ).toString(  );

        // Synchronize and lock the prefix and suffix trees together
        synchronized ( prefix ) {

            synchronized ( suffix ) {

                prefixDeleted = prefix.remove( key );
                suffixDeleted = suffix.remove( revKey );
            }
        }

        // This is a volatile
        size--;

        if ( LOG.isLoggable( Level.WARNING ) ) {

            if ( null == prefixDeleted ) {

                LOG.warning( "Remove from prefix tree failed! key: " + key );
            }

            if ( null == suffixDeleted ) {

                LOG.warning( "Remove from suffix tree failed! key: " + revKey );
            }
        }

        return ( ( null != prefixDeleted ) && ( null != suffixDeleted ) );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#deleteTree()
     */
    public void deleteTree(  ) {

        // Synchronize and lock the prefix and suffix trees together
        synchronized ( prefix ) {

            synchronized ( suffix ) {

                prefix.deleteTree(  );
                suffix.deleteTree(  );
            }
        }

        // That is a volatile
        size = 0;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#printTree()
     */
    public void printTree(  ) {

        // Synchronize and lock the prefix and suffix trees together
        synchronized ( prefix ) {

            synchronized ( suffix ) {

                System.out.println( "***** P R E F I X   T R E E *****" );
                prefix.printTree(  );
                System.out.println( "***** S U F F I X   T R E E *****" );
                suffix.printTree(  );
            }
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#get(java.lang.String)
     */
    public T get( final String key ) {

        synchronized ( prefix ) {

            return (T) prefix.get( key );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#getSize()
     */
    public long getSize(  ) {

        return size;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#getOrCreate(java.lang.String, java.lang.Object)
     */
    public T getOrCreate( final String key, final T valueIfCreate ) {

        T returnValue = null;

        try {

            String revKey = new StringBuilder( key ).reverse(  ).toString(  );

            // Synchronize and lock the prefix and suffix trees together
            synchronized ( prefix ) {

                synchronized ( suffix ) {

                    returnValue = prefix.getOrCreate( key, valueIfCreate );

                    if ( null == suffix.getOrCreate( revKey, valueIfCreate ) ) {

                        if ( LOG.isLoggable( Level.SEVERE ) ) {

                            LOG.log( Level.SEVERE, "Failed inserting value in suffix wild-tree!  Reversed key: ", revKey );
                        }
                    }
                }
            }

            // Only add one if we really created a new node
            if ( returnValue.equals( valueIfCreate ) ) {

                size++;
            }
        } catch ( Throwable th ) {

            if ( LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "Failed inserting value in tree!", th );
            }
        }

        return returnValue;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#put(java.lang.String, java.lang.Object)
     */
    public void put( final String key, final T value ) {

        try {

            String revKey = new StringBuilder( key ).reverse(  ).toString(  );

            // Synchronize and lock the prefix and suffix trees together
            synchronized ( prefix ) {

                synchronized ( suffix ) {

                    prefix.put( key, value );
                    suffix.put( revKey, value );
                }
            }

            size++;
        } catch ( Throwable th ) {

            if ( LOG.isLoggable( Level.SEVERE ) ) {

                LOG.log( Level.SEVERE, "Failed inserting value in tree!", th );
            }
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#search(java.lang.String)
     */
    public List<T> search( final String term, final WildcardTernarySearchTreeMatchListener<T> listener )
        throws IllegalArgumentException {

        // Safeguard
        if ( term == null ) {

            throw new IllegalArgumentException( "The search term cannot be null" );
        }

        // Who knows....
        if ( ( listener != null ) && !listener.continueSearch(  ) ) {

            return new ArrayList<T>(  );
        }

        // Recover wild-card positions
        ArrayList<Integer> wcPositions = new ArrayList<Integer>(  );
        int pos = -1;

        while ( ( pos = term.indexOf( wildcard, pos + 1 ) ) >= 0 )
            wcPositions.add( pos );

        // No wild-card,perform an exact match
        if ( wcPositions.size(  ) == 0 ) {

            ArrayList<T> result = new ArrayList<T>(  );

            synchronized ( prefix ) {

                String searchTerm = term;

                T item = prefix.get( searchTerm );

                if ( item != null ) {

                    result.add( item );

                    if ( listener != null ) {

                        listener.resultFound( searchTerm, item );
                    }
                }
            }

            return result;
        }

        // Wild Card Search
        // We will do a prefix search or a suffix search then screen the results using a regular expression

        // Extract the prefix of the term (e.g.) the part before the first wild-card
        int firstWCPosition = wcPositions.get( 0 ).intValue(  );
        String termPrefix = ( ( firstWCPosition == 0 ) ? "" : term.substring( 0, firstWCPosition ) );

        // Extract the suffix of the term (e.g.) the part after the last wild-card
        int lastWCPosition = wcPositions.get( wcPositions.size(  ) - 1 ).intValue(  );
        String termSuffix = ( ( lastWCPosition == ( term.length(  ) - 1 ) ) ? "" : term.substring( lastWCPosition + 1, term.length(  ) ) );

        // Build the regular expression
        final Pattern regexp = Pattern.compile( term.replaceAll( "\\" + wildcard, ".*?" ) );

        // The Results
        final ArrayList<T> results = new ArrayList<T>(  );

        // The choice will be to perform a prefix search is the term prefix is longer than the term suffix, a suffix search otherwise
        // There are probably more clever algorithms to minimise the number of returned results

        // The listener that will further screen the data with the regular expression
        TernarySearchTreeMatchListener<T> ttlistener = new TernarySearchTreeMatchListener<T>(  ) {

                public void resultFound( String key, T data ) {

                    // Filter on pattern
                    if ( regexp.matcher( key ).matches(  ) ) {

                        results.add( data );

                        if ( listener != null ) {

                            listener.resultFound( key, data );
                        }
                    }
                }

                public boolean continueSearch(  ) {

                    return ( ( listener != null ) && listener.continueSearch(  ) );
                }
            };

        // If suffix longer --> suffix search 
        if ( termSuffix.length(  ) > termPrefix.length(  ) ) {

            String searchTerm = new StringBuilder( termSuffix ).reverse(  ).toString(  );

            synchronized ( suffix ) {

                List<T> list = suffix.matchPrefix( searchTerm, ttlistener );

                if ( list != null ) {

                    results.addAll( list );
                }
            }
        }
        // Prefix is longer or same size --> prefix search
        else {

            String searchTerm = termPrefix;

            synchronized ( prefix ) {

                List<T> list = prefix.matchPrefix( searchTerm, ttlistener );

                if ( list != null ) {

                    results.addAll( list );
                }
            }
        }

        return results;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#matchPrefix(java.lang.String)
     */
    public List<T> matchPrefix( final String prefixString, final WildcardTernarySearchTreeMatchListener<T> listener ) {

        // Listen to prefix results
        TernarySearchTreeMatchListener<T> ttlistener = null;

        if ( listener != null ) {

            ttlistener = new TernarySearchTreeMatchListener<T>(  ) {

                        public void resultFound( String key, T data ) {

                            listener.resultFound( key, data );
                        }
                        ;
                        public boolean continueSearch(  ) {

                            return listener.continueSearch(  );
                        }
                    };
        }

        synchronized ( prefix ) {

            return prefix.matchPrefix( prefixString, ttlistener );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#walkPrefixTree(net.jxta.impl.util.ternary.wild.WildcardTernarySearchTreeMatchListener)
     */
    public void walkPrefixTree( final WildcardTernarySearchTreeMatchListener<T> listener ) {

        synchronized ( prefix ) {

            walkTree( prefix, listener );
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#walkSuffixTree(net.jxta.impl.util.ternary.wild.WildcardTernarySearchTreeMatchListener)
     */
    public void walkSuffixTree( final WildcardTernarySearchTreeMatchListener<T> listener ) {

        synchronized ( suffix ) {

            walkTree( suffix, listener );
        }
    }

    private void walkTree( final TernarySearchTree<T> tree, final WildcardTernarySearchTreeMatchListener<T> listener ) {

        // Listen to prefix results
        TernarySearchTreeMatchListener<T> ttlistener = null;

        if ( listener != null ) {

            ttlistener = new TernarySearchTreeMatchListener<T>(  ) {

                        public void resultFound( String key, T data ) {

                            listener.resultFound( key, data );
                        }
                        ;
                        public boolean continueSearch(  ) {

                            return listener.continueSearch(  );
                        }
                    };
            tree.walkTree( ttlistener );
        }
    }
}
