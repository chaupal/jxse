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

import net.jxta.impl.util.ternary.DoublyLinkedList;
import net.jxta.impl.util.ternary.TernarySearchTree;
import net.jxta.impl.util.ternary.TernarySearchTreeImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WildcardTernarySearchTreeImpl<T> implements WildcardTernarySearchTree<T> {

    private static final Logger LOG = Logger.getLogger( WildcardTernarySearchTreeImpl.class.getName() );
    private static final String WILDCARD = "*";
    private TernarySearchTree prefix = new TernarySearchTreeImpl();
    private TernarySearchTree suffix = new TernarySearchTreeImpl();
    private long size = 0;

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#contains(java.lang.String)
     */
    public boolean contains( String key ) {

        prefix.setNumReturnValues( 1 );

        return ( null != prefix.get( key ) );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#delete(java.lang.String)
     */
    public boolean delete( String key ) {

        prefix.remove( key );
        suffix.remove( new StringBuffer( key ).reverse().toString() );
        size--;

        return true;
    }

    /* (non-Javadoc)
         * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#deleteTree()
         */
    public void deleteTree() {

        prefix.deleteTree();
        suffix.deleteTree();
        size = 0;
    }

    /* (non-Javadoc)
    * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#find(java.lang.String)
    */
    @SuppressWarnings( "unchecked" )
    public T find( String key ) {

        return (T) prefix.get( key );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#getSize()
     */
    public long getSize() {

        return size;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#insert(java.lang.String, java.lang.Object)
     */
    public void insert( String key, Object value ) throws IllegalStateException {

        prefix.put( key, value );
        suffix.put( new StringBuffer( key ).reverse().toString(), value );
        size++;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernaryTree#search(java.lang.String)
     */
    public List<String> search( String term, int threshold )
        throws IllegalArgumentException {

        if ( ( term.length() - term.replace( WILDCARD, "" ).length() ) != 1 ) {

            throw new IllegalArgumentException( "" );
        }

        if ( term.endsWith( WILDCARD ) ) {

            this.prefix.setNumReturnValues( threshold );

            return toList( prefix.matchPrefix( term.substring( 0, term.length() - 1 ) ), false );
        } else if ( term.startsWith( WILDCARD ) ) {

            String revTerm = new StringBuffer( term.substring( 1 ) ).reverse().toString();

            this.suffix.setNumReturnValues( threshold );

            return toList( suffix.matchPrefix( revTerm ), true );
        } else {

            int idx = term.indexOf( WILDCARD );

            return (List<String>) intersection( search( term.substring( 0, idx + 1 ), threshold ),
                search( term.substring( idx ), threshold ) );
        }
    }

    private List<String> toList( DoublyLinkedList dll, boolean reverse ) {

        if ( LOG.isLoggable( Level.FINEST ) ) {

            LOG.finest( "Building result list... linked list size: " + dll.size() );
        }

        ArrayList<String> al = new ArrayList<String>();
        Iterator<Object> it = dll.iterator();
        String key;

        while ( it.hasNext() ) {

            key = (String) it.next();

            if ( reverse ) {

                key = new StringBuffer( key ).reverse().toString();
            }

            al.add( key );
        }

        return al;
    }

    private Collection<String> intersection( Collection<String> coll1, Collection<String> coll2 ) {

        if ( LOG.isLoggable( Level.FINEST ) ) {

            LOG.finest( "Result intersection. Collection sizes: " + coll1.size() + " " + coll2.size() );
        }

        Set<String> intersection = new HashSet<String>( coll1 );

        intersection.retainAll( coll2 );

        return new ArrayList<String>( intersection );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.wild.WildcardTernarySearchTree#matchPrefix(java.lang.String)
     */
    public List<String> matchPrefix( String prefix, int threshold ) {

        this.prefix.setNumReturnValues( threshold );

        DoublyLinkedList dll = this.prefix.matchPrefix( prefix );

        return toList( dll, false );
    }
}
