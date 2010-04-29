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

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Author Wally Flint: wally@wallyflint.com
 *
 * Lifted from the Java World article: http://www.javaworld.com/javaworld/jw-02-2001/jw-0216-ternary.html
 * "With thanks to Michael Amster of webeasy.com for introducing me to the Ternary Search Tree, and providing some starting code."
 * 
 * SimonT: Ensured implementation of Iterator
 */
public class DoublyLinkedList {

    private DLLNode head;
    private DLLNode last;
    private int size = 0;

    public void addFirst( Object data ) {

        DLLNode newNode = new DLLNode();

        newNode.data = data;

        if ( size == 0 ) {

            head = newNode;
            last = head;
        } else {

            newNode.nextNode = head;
            head.previousNode = newNode;
            head = newNode;
        }

        size++;
    }

    public void addLast( Object data ) {

        DLLNode newNode = new DLLNode();

        newNode.data = data;

        if ( size == 0 ) {

            head = newNode;
        } else {

            last.nextNode = newNode;
            newNode.previousNode = last;
        }

        last = newNode;
        size++;
    }

    public void removeFirst() {

        if ( size <= 1 ) {

            head = null;
            last = null;
        } else {

            DLLNode oldHead = head;

            head = oldHead.nextNode;
            oldHead.nextNode = null;
            head.previousNode = null;
        }

        size--;
    }

    public void removeLast() {

        if ( size <= 1 ) {

            head = null;
            last = null;
        } else {

            last = last.previousNode;
            last.nextNode.previousNode = null;
            last.nextNode = null;
        }

        size--;
    }

    public int size() {

        return size;
    }

    public void clear() {

        DLLNode currentNode = last;
        DLLNode tempNode;

        while ( currentNode != null ) {

            tempNode = currentNode.previousNode;
            currentNode.nextNode = null;
            currentNode.previousNode = null;
            currentNode.data = null;
            currentNode = tempNode;
        }

        last = null;
        head = null;
        size = 0;
    }

    public Iterator<Object> iterator() {

        return new DLLIterator();
    }

    protected class DLLNode {

        private DLLNode() {};

        protected DLLNode nextNode;
        protected DLLNode previousNode;
        protected Object data;
        
    }

    public class DLLIterator implements Iterator<Object> {

        private DLLNode currentPreviousNode = null;
        private DLLNode currentNextNode = head;

        public boolean hasNext() {

            if ( currentNextNode == null ) {

                return false;
            } else {

                return ( currentNextNode != null );
            }
        }

        public boolean hasPrevious() {

            if ( currentPreviousNode == null ) {

                return false;
            } else {

                return ( currentPreviousNode != null );
            }
        }

        public Object next() {

            if ( currentNextNode == null ) {

                throw new NoSuchElementException( "Attempt to retrieve next value from " +
                    "DoublyLinkedList after all values have already been retrieved. Verify hasNext method returns true " +
                    "before calling next method." );
            }

            Object data = currentNextNode.data;
            DLLNode tempNode = currentNextNode;

            currentNextNode = currentNextNode.nextNode;
            currentPreviousNode = tempNode;

            return data;
        }

        public Object previous() {

            if ( currentPreviousNode == null ) {

                throw new NoSuchElementException( "Attempt to retrieve previous value from " +
                    "head node of DoublyLinkedList. Verify hasPrevious method returns true " + "before calling previous method." );
            }

            Object data = currentPreviousNode.data;
            DLLNode tempNode = currentPreviousNode;

            currentPreviousNode = currentPreviousNode.previousNode;
            currentNextNode = tempNode;

            return data;
        }

        public void resetToBeginning() {

            currentNextNode = head;
            currentPreviousNode = null;
        }

        public void resetToEnd() {

            currentNextNode = null;
            currentPreviousNode = last;
        }

        public void remove() {
        	// Not implemented
        }
    }
}
