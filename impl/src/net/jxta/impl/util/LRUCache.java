/*
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.jxta.impl.util;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;


/**
 *  This class implements a Generic LRU Cache
 *
 *@author    Ignacio J. Ortega
 *@author    Mohamed Abdelaziz
 */

public class LRUCache {

    private transient int cacheSize;
    private transient int currentSize;
    private transient CacheNode first;
    private transient CacheNode last;
    private final transient Hashtable nodes;

    /**
     *  Constructor for the LRUCache object
     *
     *@param  size  Description of the Parameter
     */
    public LRUCache(int size) {
        currentSize = 0;
        cacheSize = size;
        nodes = new Hashtable(size);
    }

    /**
     *  clear the cache
     */
    public void clear() {
        first = null;
        last = null;
    }

    /**
     * returns the number of elements currently in cache
     * @return the number of elements in cache
     */
    public int size() {
        return currentSize;
    }

    /**
     *  retrieve an object from cache
     *
     *@param  key  key
     *@return      object
     */
    public Object get(Object key) {
        CacheNode node = (CacheNode) nodes.get(key);

        if (node != null) {
            moveToHead(node);
            return node.value;
        }
        return null;
    }

    public boolean contains(Object key) {
        return nodes.contains(key);
    }

    protected Iterator iterator(int size) {
        List list = new ArrayList();
        Iterator it = nodes.values().iterator();

        while (it.hasNext()) {
            list.add(((CacheNode) it.next()).value);
            if (list.size() >= size) {
                break;
            }
        }
        return list.iterator();
    }

    private void moveToHead(CacheNode node) {
        if (node == first) {
            return;
        }
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        if (last == node) {
            last = node.prev;
        }
        if (first != null) {
            node.next = first;
            first.prev = node;
        }
        first = node;
        node.prev = null;
        if (last == null) {
            last = first;
        }
    }

    /**
     *  puts an object into cache
     *
     *@param  key    key to store value by
     *@param  value  object to insert
     */
    public void put(Object key, Object value) {
        CacheNode node = (CacheNode) nodes.get(key);

        if (node == null) {
            if (currentSize >= cacheSize) {
                if (last != null) {
                    nodes.remove(last.key);
                }
                removeLast();
            } else {
                currentSize++;
            }
            node = new CacheNode();
        }
        node.value = value;
        node.key = key;
        moveToHead(node);
        nodes.put(key, node);
    }

    /**
     *  remove an object from cache
     *
     *@param  key  key
     *@return      Object removed
     */
    public Object remove(Object key) {
        CacheNode node = (CacheNode) nodes.get(key);

        if (node != null) {
            if (node.prev != null) {
                node.prev.next = node.next;
            }
            if (node.next != null) {
                node.next.prev = node.prev;
            }
            if (last == node) {
                last = node.prev;
            }
            if (first == node) {
                first = node.next;
            }
        }
        return node;
    }

    /**
     *  removes the last enry from cache
     */
    private void removeLast() {
        if (last != null) {
            if (last.prev != null) {
                last.prev.next = null;
            } else {
                first = null;
            }
            last = last.prev;
        }
    }

    /**
     *  cache node object wrapper
     */
    protected class CacheNode {
        Object key;
        CacheNode next;

        CacheNode prev;
        Object value;

        /**
         *  Constructor for the CacheNode object
         */
        CacheNode() {}
    }
}

