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

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements a Generic LRU Cache.
 *
 * @author Ignacio J. Ortega
 * @author Mohamed Abdelaziz
 */

public class LRUCache<K, V> {

    private final transient int cacheSize;
    private transient int currentSize;
    private transient CacheNode first;
    private transient CacheNode last;
    private final transient Map<K, CacheNode> nodes;

    /**
     * Constructor for the LRUCache object
     *
     * @param size Description of the Parameter
     */
    public LRUCache(int size) {
        currentSize = 0;
        cacheSize = size;
        nodes = new HashMap<K, CacheNode>(size);
    }

    /**
     * clear the cache
     */
    public synchronized void clear() {
        first = null;
        last = null;
        nodes.clear();
        currentSize = 0;
    }

    /**
     * returns the number of elements currently in cache
     *
     * @return the number of elements in cache
     */
    public synchronized int size() {
        return currentSize;
    }

    /**
     * retrieve an object from cache
     *
     * @param key key
     * @return object
     */
    public synchronized V get(K key) {
        CacheNode node = nodes.get(key);

        if (node != null) {
            moveToHead(node);
            return node.value;
        }
        return null;
    }

    public synchronized boolean contains(K key) {
        return nodes.keySet().contains(key);
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
     * puts an object into cache
     *
     * @param key   key to store value by
     * @param value object to insert
     */
    public synchronized void put(K key, V value) {
        CacheNode node = nodes.get(key);

        if (node == null) {
            if (currentSize >= cacheSize) {
                if (last != null) {
                    nodes.remove(last.key);
                }
                removeLast();
            } else {
                currentSize++;
            }
            node = new CacheNode(key, value);
        }
        node.value = value;
        moveToHead(node);
        nodes.put(key, node);
    }

    /**
     * remove an object from cache
     *
     * @param key key
     * @return Object removed
     */
    public synchronized V remove(K key) {
        CacheNode node = nodes.get(key);

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
        if (node != null) {
            return node.value;
        } else {
            return null;
        }
    }

    /**
     * removes the last entry from cache
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
     * cache node object wrapper
     */
    protected class CacheNode {
        final K key;
        CacheNode next;

        CacheNode prev;
        V value;

        /**
         * Constructor for the CacheNode object
         *
         * @param key   key
         * @param value value
         */
        CacheNode(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}

