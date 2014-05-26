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
package net.jxta.impl.util.ternary;

import net.jxta.logging.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * Implementation of ternary search tree. A Ternary Search Tree is a data structure that behaves
 * in a manner that is very similar to a HashMap.
 *
 * Credits:
 *
 * Modified by Bruno Grieder: bruno.grieder@amalto.com
 *      Improvements to the Match Prefix with Threshold recursive algorithms
 *      Minor speed improvements
 *
 * Modified by Simon Temple: simon.temple@amalto.com
 *      Added interface
 *      BUG FIX: NPE on final node removal (left as comment to WEB article; contributor unknown).
 *      BUG FIX: Re-coded delete of nodes with both LO and HI KID relatives
 *      BUG FIX: Assign new parent KID to node following move due to delete
 *      Removed ASCII comparison in favour of using java Character class
 *      Added maxNodeTraversal warnings
 *
 * Modified by Yan Cheng for generic introduction and thread safe matchPrefix.
 *
 * Original Author Wally Flint: wally@wallyflint.com
 *
 * With thanks to Michael Amster of webeasy.com for introducing Wally Flint to
 * the Ternary Search Tree, and providing some starting code.
 *
 */
public class TernarySearchTreeImpl<E> implements TernarySearchTree<E> {

    private final static long MAX_NODE_TRAVERSALS = Long.getLong( TernarySearchTreeImpl.class.getName(  ) + ".maxNodeTraversals", 300 );
    private final static Logger LOG = Logger.getLogger( TernarySearchTreeImpl.class.getName(  ) );
    private volatile static int treesCreated = 0;
    private volatile TSTNode<E> rootNode = null;

    // Convenience variable for getKey method - not synchronised but faster than StringBuffer()
    private StringBuilder getKeyBuffer = new StringBuilder(  );

    // The Tree Name, if any
    private volatile String treeName;

    public TernarySearchTreeImpl(  ) {
        this( null );
    }

    public TernarySearchTreeImpl( String name ) {

        this.treeName = String.valueOf( ++treesCreated ) + "-" + ( ( name == null ) ? String.valueOf( hashCode(  ) ) : name );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#deleteTree()
     */
    @SuppressWarnings( "unchecked" )
    public void deleteTree(  ) {

        if ( rootNode != null ) {

            // Simply detach the lot and let gc deal with it
            rootNode.relatives = new TSTNode[ 4 ];
            rootNode.data = null;
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#getOrCreate(java.lang.String, E)
     */
    public E getOrCreate( final String key, final E valueIfCreate ) {

        TSTNode<E> node = getOrCreateNode( key );

        if ( node.data == null ) {

            node.data = valueIfCreate;
        }

        return node.data;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#put(java.lang.String, E)
     */
    public void put( final String key, final E value ) {

        getOrCreateNode( key ).data = value;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#get(java.lang.String)
     */
    public E get( final String key ) {

        TSTNode<E> node = getNode( key );

        if ( node == null ) {

            return null;
        }

        return node.data;
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#remove(java.lang.String)
     */
    public E remove( final String key ) {

        TSTNode<E> node = getNode( key );
        E nodeData = null;

        if ( null != node ) {

            nodeData = node.data;

            deleteNode( node );
        } else {

            if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                LOG.log( Level.WARNING,
                    ( ( treeName == null ) ? "" : ( "[" + treeName + "] " ) ) + "Failed to find node to remove given key: " + key );
            }
        }

        return nodeData;
    }

    /**
     * Returns the Node indexed by key, or null if that node doesn't exist. Search begins at root node.
     * @param key An index that points to the desired node.
     * @return TSTNode The node object indexed by key. This object is an instance of an inner class
     * named TernarySearchTree.TSTNode.
     */
    public TSTNode<E> getNode( final String key ) {

        return getNode( key, rootNode );
    }

    /**
     * Returns the Node indexed by key, or null if that node doesn't exist. Search begins at root node.
     * @param key An index that points to the desired node.
     * @param startNode The top node defining the subtree to be searched.
     * @return TSTNode The node object indexed by key. This object is an instance of an inner class
     *  named TernarySearchTree.TSTNode.
     */
    protected TSTNode<E> getNode( final String key, final TSTNode<E> startNode ) {

        if ( ( key == null ) || ( startNode == null ) || ( key.length(  ) == 0 ) ) {

            return null;
        }

        TSTNode<E> currentNode = startNode;
        int charIndex = 0;
        int nodesTraversed = 0;

        while ( true ) {

            if ( currentNode == null ) {

                return null;
            }

            int charComp = compareCharsAlphabetically( key.charAt( charIndex ), currentNode.splitchar );

            if ( charComp == 0 ) {

                charIndex++;

                if ( charIndex == key.length(  ) ) {

                    return currentNode;
                }

                currentNode = currentNode.relatives [ TSTNode.EQKID ];
            } else if ( charComp < 0 ) {

                currentNode = currentNode.relatives [ TSTNode.LOKID ];
            } else {

                // charComp must be greater than zero
                currentNode = (TSTNode<E>) currentNode.relatives [ TSTNode.HIKID ];
            }

            if ( ++nodesTraversed > MAX_NODE_TRAVERSALS ) {

                nodesTraversed = 0;

                if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                    LOG.log( Level.WARNING,
                        ( ( treeName == null ) ? "" : ( "[" + treeName + "] " ) ) +
                        "Excessive node traversal detected.  Tree is either broken or very inefficient!" );
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#matchPrefix(java.lang.String)
     */
    public List<E> matchPrefix( final String prefix ) {

        return matchPrefix( prefix, null );
    }

    /* (non-Javadoc)
     * @see net.jxta.impl.util.ternary.TernarySearchTree#matchPrefix(java.lang.String, int)
     */
    public List<E> matchPrefix( final String prefix, final TernarySearchTreeMatchListener<E> listener ) {

        // The returned list
        List<E> sortKeysResult = new ArrayList<E>(  );

        // No one should query with a listener set to stop search, but who knows....
        if ( ( listener != null ) && !listener.continueSearch(  ) ) {

            return sortKeysResult;
        }

        // Find the highest node matching the prefix
        TSTNode<E> startNode = getNode( prefix );

        // No match -> the prefix does not exist in the tree
        if ( startNode == null ) {

            return sortKeysResult;
        }

        // If we have data a this level (e.g. node exactly matching the prefix), collect it
        if ( startNode.data != null ) {

            sortKeysResult.add( startNode.data );

            if ( listener != null ) {

                listener.resultFound( prefix, startNode.data );

                if ( !listener.continueSearch(  ) ) {

                    return sortKeysResult;
                }
            }
        }

        // Start going down the tree to collect additional results
        sortKeysRecursion( sortKeysResult, startNode.relatives [ TSTNode.EQKID ], prefix, listener );

        return sortKeysResult;
    }

    private void sortKeysRecursion( final List<E> sortKeysResult, final TSTNode<E> currentNode, final String currentKey,
        final TernarySearchTreeMatchListener<E> listener ) {

        // We have gone to the left tip of this branch --> move back
        if ( currentNode == null ) {

            return;
        }

        // If we have data available at that node, collect it and keep going left
        if ( currentNode.data != null ) {

            sortKeysResult.add( currentNode.data );

            if ( listener != null ) {

                listener.resultFound( currentKey + currentNode.splitchar, currentNode.data );

                // Check if listener calls for end the search
                if ( !listener.continueSearch(  ) ) {

                    return;
                }
            }
        }

        // Keep going left
        sortKeysRecursion( sortKeysResult, currentNode.relatives [ TSTNode.LOKID ], currentKey, listener );

        // We have done the left branch, start the middle branch
        sortKeysRecursion( sortKeysResult, currentNode.relatives [ TSTNode.EQKID ], currentKey + currentNode.splitchar, listener );

        // Finally, run the right branch
        sortKeysRecursion( sortKeysResult, currentNode.relatives [ TSTNode.HIKID ], currentKey, listener );
    }

    /**
     * Returns the Node indexed by key, creating that node if it doesn't exist, and creating any required.
     * intermediate nodes if they don't exist.
     * @param key A string that indexes the node that is returned.
     * @return TSTNode The node object indexed by key. This object is an instance of an inner class
     * named TernarySearchTree.TSTNode.
     */
    protected TSTNode<E> getOrCreateNode( final String key )
        throws NullPointerException, IllegalArgumentException {

        if ( key == null ) {

            throw new NullPointerException( ( ( treeName == null ) ? "" : ( "[" + treeName + "] " ) ) +
                "Attempt to get or create node with null key" );
        }

        if ( key.length(  ) == 0 ) {

            throw new IllegalArgumentException( ( ( treeName == null ) ? "" : ( "[" + treeName + "] " ) ) +
                "Attempt to get or create node with key of zero length" );
        }

        if ( rootNode == null ) {

            rootNode = new TSTNode<E>( key.charAt( 0 ), null );
        }

        TSTNode<E> currentNode = rootNode;
        int charIndex = 0;
        int nodesTraversed = 0;

        while ( true ) {

            char currentChar = key.charAt( charIndex );

            int charComp = compareCharsAlphabetically( currentChar, currentNode.splitchar );

            if ( charComp == 0 ) {

                charIndex++;

                if ( charIndex == key.length(  ) ) {

                    return currentNode;
                }

                if ( currentNode.relatives [ TSTNode.EQKID ] == null ) {

                    currentNode.relatives [ TSTNode.EQKID ] = new TSTNode<E>( key.charAt( charIndex ), currentNode );
                }

                currentNode = currentNode.relatives [ TSTNode.EQKID ];
            } else if ( charComp < 0 ) {

                if ( currentNode.relatives [ TSTNode.LOKID ] == null ) {

                    currentNode.relatives [ TSTNode.LOKID ] = new TSTNode<E>( currentChar, currentNode );
                }

                currentNode = currentNode.relatives [ TSTNode.LOKID ];
            } else {

                // charComp must be greater than zero
                if ( currentNode.relatives [ TSTNode.HIKID ] == null ) {

                    currentNode.relatives [ TSTNode.HIKID ] = new TSTNode<E>( currentChar, currentNode );
                }

                currentNode = currentNode.relatives [ TSTNode.HIKID ];
            }

            if ( ++nodesTraversed > MAX_NODE_TRAVERSALS ) {

                nodesTraversed = 0;

                if ( Logging.SHOW_WARNING && LOG.isLoggable( Level.WARNING ) ) {

                    LOG.log( Level.WARNING,
                        ( ( treeName == null ) ? "" : ( "[" + treeName + "] " ) ) +
                        "Excessive node traversal detected.  Tree is either broken or very inefficient!" );
                }
            }
        }
    }

    /*
     * Deletes the node passed in as an argument to this method. If this node has non-null data, then both the node and the data will be deleted.
     * Also deletes any other nodes in the tree that are no longer needed after the deletion of the node first passed in as an argument to this method.
     */
    private void deleteNode( final TSTNode<E> nodeToDelete ) {

        if ( nodeToDelete == null ) {

            return;
        }

        TSTNode<E> node = nodeToDelete;

        node.data = null;

        while ( node != null ) {

            if ( Logging.SHOW_FINEST && LOG.isLoggable( Level.FINEST ) ) {

                LOG.log( Level.FINEST, "Deleting tree node: " + node );
            }

            node = deleteNodeRecursion( node );
        }
    }

    private TSTNode<E> deleteNodeRecursion( final TSTNode<E> currentNode ) {

        // To delete a node, first set its data to null, then pass it into this method, then pass the node returned by this method into this method
        // (make sure you don't delete the data of any of the nodes returned from this method!)
        // and continue in this fashion until the node returned by this method is null.
        // The TSTNode instance returned by this method will be next node to be operated on by deleteNodeRecursion.
        // (This emulates recursive method call while avoiding the JVM overhead normally associated with a recursive method.)
        if ( currentNode == null ) {

            return null;
        }

        if ( ( currentNode.relatives [ TSTNode.EQKID ] != null ) || ( currentNode.data != null ) ) {

            return null; // Can't delete this node if it has a non-null eq kid or data
        }

        TSTNode<E> currentParent = currentNode.relatives [ TSTNode.PARENT ];

        // If we've made it this far, then we know the currentNode isn't null, but its data and equal kid are null, so we can delete the current node
        // (before deleting the current node, we'll move any lower nodes higher in the tree)
        boolean lokidNull = currentNode.relatives [ TSTNode.LOKID ] == null;
        boolean hikidNull = currentNode.relatives [ TSTNode.HIKID ] == null;

        // Now find out what kind of child current node is
        int childType;

        if ( currentParent != null ) {

            if ( currentParent.relatives [ TSTNode.LOKID ] == currentNode ) {

                childType = TSTNode.LOKID;
            } else if ( currentParent.relatives [ TSTNode.EQKID ] == currentNode ) {

                childType = TSTNode.EQKID;
            } else if ( currentParent.relatives [ TSTNode.HIKID ] == currentNode ) {

                childType = TSTNode.HIKID;
            } else {

                // If this executes, then current node is root node
                return null;
            }

            if ( lokidNull && hikidNull ) {

                // If we make it to here, all three kids are null and we can just delete this node
                currentParent.relatives [ childType ] = null;

                return currentParent;
            }

            // If we make it this far, we know that EQKID is null, and either HIKID or LOKID is null, or both HIKID and LOKID are NON-null
            if ( lokidNull ) {

                currentParent.relatives [ childType ] = currentNode.relatives [ TSTNode.HIKID ];
                currentNode.relatives [ TSTNode.HIKID ].relatives [ TSTNode.PARENT ] = currentParent;

                return currentParent;
            }

            if ( hikidNull ) {

                currentParent.relatives [ childType ] = currentNode.relatives [ TSTNode.LOKID ];
                currentNode.relatives [ TSTNode.LOKID ].relatives [ TSTNode.PARENT ] = currentParent;

                return currentParent;
            }

            int deltaHi = currentNode.relatives [ TSTNode.HIKID ].splitchar - currentNode.splitchar;
            int deltaLo = currentNode.splitchar - currentNode.relatives [ TSTNode.LOKID ].splitchar;
            int movingKid;
            TSTNode<E> targetNode;

            // If deltaHi is equal to deltaLo, then choose one of them at random, and make it "further away" from the current node's splitchar
            if ( deltaHi == deltaLo ) {

                if ( Math.random(  ) < 0.5 ) {

                    deltaHi++;
                } else {

                    deltaLo++;
                }
            }

            if ( deltaHi > deltaLo ) {

                movingKid = TSTNode.HIKID;
                targetNode = currentNode.relatives [ TSTNode.LOKID ];
            } else {

                movingKid = TSTNode.LOKID;
                targetNode = currentNode.relatives [ TSTNode.HIKID ];
            }

            while ( targetNode.relatives [ movingKid ] != null ) {

                targetNode = targetNode.relatives [ movingKid ];
            }

            // Now targetNode.relatives[movingKid] is null, and we can put the moving kid into it.
            targetNode.relatives [ movingKid ] = currentNode.relatives [ movingKid ];
            // Assign the new parent
            currentNode.relatives [ movingKid ].relatives [ TSTNode.PARENT ] = targetNode;

            // Now we need to put the target node where the current node used to be
            currentParent.relatives [ childType ] = targetNode;

            targetNode.relatives [ TSTNode.PARENT ] = currentParent;

            if ( !lokidNull ) {

                if ( ( movingKid != TSTNode.LOKID ) && ( null != currentNode.relatives [ TSTNode.LOKID ] ) &&
                        ( targetNode != currentNode.relatives [ TSTNode.LOKID ] ) ) {

                    // We must re-graft the LOKID if it exists
                    while ( targetNode.relatives [ TSTNode.LOKID ] != null ) {

                        targetNode = targetNode.relatives [ TSTNode.LOKID ];
                    }

                    // Null the old child reference
                    currentNode.relatives [ TSTNode.LOKID ].relatives [ TSTNode.HIKID ] = null;
                    // and re-graft...
                    targetNode.relatives [ TSTNode.LOKID ] = currentNode.relatives [ TSTNode.LOKID ];
                    currentNode.relatives [ TSTNode.LOKID ].relatives [ TSTNode.PARENT ] = targetNode;
                }

                currentNode.relatives [ TSTNode.LOKID ] = null;
            }

            if ( !hikidNull ) {

                if ( ( movingKid != TSTNode.HIKID ) && ( null != currentNode.relatives [ TSTNode.HIKID ] ) &&
                        ( targetNode != currentNode.relatives [ TSTNode.HIKID ] ) ) {

                    // We must re-graft the HIKID if it exists
                    while ( targetNode.relatives [ TSTNode.HIKID ] != null ) {

                        targetNode = targetNode.relatives [ TSTNode.HIKID ];
                    }

                    // Null the old child reference
                    currentNode.relatives [ TSTNode.HIKID ].relatives [ TSTNode.LOKID ] = null;
                    // and re-graft...
                    targetNode.relatives [ TSTNode.HIKID ] = currentNode.relatives [ TSTNode.HIKID ];
                    currentNode.relatives [ TSTNode.HIKID ].relatives [ TSTNode.PARENT ] = targetNode;
                }

                currentNode.relatives [ TSTNode.HIKID ] = null;
            }

            currentNode.relatives [ TSTNode.PARENT ] = null;
        }

        // Note that the statements above ensure currentNode is completely dereferenced, and so it will be garbage collected
        return currentParent;
    }

    private static int compareCharsAlphabetically( final char cCompare, final Character charRef ) {

        // Use java Character class comparison and not some half baked ASCII char comparison
        Character chr1 = Character.valueOf( cCompare );

        return ( chr1.compareTo( charRef ) );
    }

    // Prints entire tree structure to standard output, beginning with the root node and working down.
    public void printTree(  ) {

        System.out.println( "" );

        if ( rootNode == null ) {

            System.out.println( "Tree is empty!\n" );

            return;
        }

        System.out.println( "Note: keys are delimited by vertical lines: |example key|\n" );
        printNodeRecursion( rootNode );
    }

    // Prints subtree structure to standard output, beginning with startingNode and working down.
    protected void printTree( TSTNode<E> startingNode ) {

        System.out.println( "" );

        if ( rootNode == null ) {

            System.out.println( "Subtree is empty!" );

            return;
        }

        System.out.println( "Note: keys are delimited by vertical lines: |example key|\n" );
        printNodeRecursion( startingNode );
    }

    public void walkTree( final TernarySearchTreeMatchListener<E> listener ) {

        walkNodeRecursion( rootNode, listener );
    }

    private void walkNodeRecursion( TSTNode<E> currentNode, final TernarySearchTreeMatchListener<E> listener ) {

        if ( currentNode == null ) {

            return;
        }

        if ( null != currentNode.data ) {

            listener.resultFound( getKey( currentNode ), currentNode.data );
        }

        walkNodeRecursion( currentNode.relatives [ TSTNode.LOKID ], listener );
        walkNodeRecursion( currentNode.relatives [ TSTNode.EQKID ], listener );
        walkNodeRecursion( currentNode.relatives [ TSTNode.HIKID ], listener );
    }

    // Recursive method used to print out tree or subtree structure.
    private void printNodeRecursion( TSTNode<E> currentNode ) {

        if ( currentNode == null ) {

            return;
        }

        System.out.println( "" );
        System.out.println( "--------------------------------------------------------------------------------" );
        System.out.println( "info for node\t|" + getKey( currentNode ) + "|\tnode data:\t" + currentNode.data + "\n" );

        if ( currentNode.relatives [ TSTNode.PARENT ] == null ) {

            System.out.println( "parent\t\tnull" );
        } else {

            System.out.println( "parent key\t|" + getKey( currentNode.relatives [ TSTNode.PARENT ] ) + "|\tparent data:\t" +
                currentNode.relatives [ TSTNode.PARENT ].data );
        }

        if ( currentNode.relatives [ TSTNode.LOKID ] == null ) {

            System.out.println( "lokid\t\tnull" );
        } else {

            System.out.println( "lokid key\t|" + getKey( currentNode.relatives [ TSTNode.LOKID ] ) + "|\tlo kid data:\t" +
                currentNode.relatives [ TSTNode.LOKID ].data );
        }

        if ( currentNode.relatives [ TSTNode.EQKID ] == null ) {

            System.out.println( "eqkid\t\tnull" );
        } else {

            System.out.println( "eqkid key\t|" + getKey( currentNode.relatives [ TSTNode.EQKID ] ) + "|\tequal kid data:\t" +
                currentNode.relatives [ TSTNode.EQKID ].data );
        }

        if ( currentNode.relatives [ TSTNode.HIKID ] == null ) {

            System.out.println( "hikid\t\tnull" );
        } else {

            System.out.println( "hikid key\t|" + getKey( currentNode.relatives [ TSTNode.HIKID ] ) + "|\thi kid data:\t" +
                currentNode.relatives [ TSTNode.HIKID ].data );
        }

        System.out.println( "--------------------------------------------------------------------------------" );

        printNodeRecursion( currentNode.relatives [ TSTNode.LOKID ] );
        printNodeRecursion( currentNode.relatives [ TSTNode.EQKID ] );
        printNodeRecursion( currentNode.relatives [ TSTNode.HIKID ] );
    }

    /** Returns the key that indexes the node argument.
     *   @param node The node whose index is to be calculated.
     *   @return String The string that indexes the node argument.
     */
    protected String getKey( TSTNode<E> node ) {

        getKeyBuffer.setLength( 0 );
        getKeyBuffer.append( node.splitchar );

        TSTNode<E> currentNode;
        TSTNode<E> lastNode;

        currentNode = node.relatives [ TSTNode.PARENT ];
        lastNode = node;

        while ( currentNode != null ) {

            if ( currentNode.relatives [ TSTNode.EQKID ] == lastNode ) {

                getKeyBuffer.append( currentNode.splitchar );
            }

            lastNode = currentNode;
            currentNode = currentNode.relatives [ TSTNode.PARENT ];
        }

        getKeyBuffer.reverse(  );

        return getKeyBuffer.toString(  );
    }

    /**
     * An inner class of TernarySearchTree that represents a node in the tree.
     */
    private static final class TSTNode<E> {

        // Index values for accessing relatives array
        protected static final int PARENT = 0;
        protected static final int LOKID = 1;
        protected static final int EQKID = 2;
        protected static final int HIKID = 3;

        // Node fields...all volatile
        protected volatile Character splitchar;
        @SuppressWarnings( "unchecked" )
        protected volatile TSTNode<E>[] relatives = new TSTNode[ 4 ];
        protected volatile E data;

        protected TSTNode( char splitchar, TSTNode<E> parent ) {

            this.splitchar = splitchar;
            relatives [ PARENT ] = parent;
        }

        public String toString(  ) {

            return "Node(" + splitchar + "):: Numeric:" + Character.getNumericValue( splitchar );
        }
    }
}
