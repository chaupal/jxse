/*
 * Written by Dawid Kurzyniec, on the basis of public specifications and
 * public domain sources from JSR 166 and the Doug Lea's collections package,
 * and released to the public domain,
 * as explained at http://creativecommons.org/licenses/publicdomain.
 */

package net.jxta.impl.util.backport.java.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.AbstractSet;
import java.util.SortedSet;
import java.util.Set;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Sorted map implementation based on a red-black tree and implementing
 * all the methods from the NavigableMap interface.
 *
 * @author Dawid Kurzyniec
 */
@SuppressWarnings("unchecked")
public class TreeMap<K extends Object, V extends Object> extends AbstractMap<K,V>
                     implements NavigableMap<K,V>, Serializable {

    private static final long serialVersionUID = 919286545866124006L;

    private final Comparator<Object> comparator;

    private transient Entry<K,V> root;

    private transient int size = 0;
    private transient int modCount = 0;

    private transient EntrySet entrySet;
    private transient KeySet<K> navigableKeySet;
    private transient NavigableMap<K,V> descendingMap;
    private transient Comparator<Object> reverseComparator;

    public TreeMap() {
        this.comparator = null;
    }

    public TreeMap(Comparator<Object> comparator) {
        this.comparator = comparator;
    }

    public TreeMap(SortedMap<K,V> map) {
        this.comparator = (Comparator<Object>) map.comparator();
        this.buildFromSorted(map.entrySet().iterator(), map.size());
    }

    public TreeMap(Map<K,V> map) {
        this.comparator = null;
        putAll(map);
    }

    public int size() { return size; }

    public void clear() {
        root = null;
        size = 0;
        modCount++;
    }

    public Object clone() {
        TreeMap<K,V> clone;
        try { clone = (TreeMap<K,V>)super.clone(); }
        catch (CloneNotSupportedException e) { throw new InternalError(); }
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        if (!isEmpty()) {
            clone.buildFromSorted(this.entrySet().iterator(), this.size);
        }
        return clone;
    }

    public V put(K key, V value) {
        if (root == null) {
            root = new Entry<K,V>(key, value);
            size++;
            modCount++;
            return null;
        }
        else {
            Entry<K,V> t = root;
            for (;;) {
                int diff = compare(key, t.getKey(),  comparator);
                if (diff == 0) return t.setValue(value);
                else if (diff <= 0) {
                    if (t.left != null) t = t.left;
                    else {
                        size++;
                        modCount++;
                        Entry<K,V> e = new Entry<K,V>(key, value);
                        e.parent = t;
                        t.left = e;
                        fixAfterInsertion(e);
                        return null;
                    }
                }
                else {
                    if (t.right != null) t = t.right;
                    else {
                        size++;
                        modCount++;
                        Entry<K,V> e = new Entry<K,V>(key, value);
                        e.parent = t;
                        t.right = e;
                        fixAfterInsertion(e);
                        return null;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) {
        Entry<K,V> entry = getEntry(key);
        return (V) ((entry == null) ? null : entry.getValue());
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    public static class Entry<K extends Object,V extends Object>
        implements Map.Entry<K,V>, Cloneable, java.io.Serializable {
    	private static final long serialVersionUID = 1L;
		
    	private static final boolean RED = false;
        private static final boolean BLACK = true;

        private K key;
        private V element;

        /**
         * The node color (RED, BLACK)
         */
        private boolean color;

        /**
         * Pointer to left child
         */
        private Entry<K,V> left;

        /**
         * Pointer to right child
         */
        private Entry<K,V> right;

        /**
         * Pointer to parent (null if root)
         */
        private Entry<K,V> parent;

        /**
         * Make a new node with given element, null links, and BLACK color.
         * Normally only called to establish a new root.
         */
        public Entry(K key, V element) {
            this.key = key;
            this.element = element;
            this.color = BLACK;
        }

        /**
         * Return a new Entry with same element and color as self,
         * but with null links. (Since it is never OK to have
         * multiple identical links in a RB tree.)
         */
        protected Object clone() throws CloneNotSupportedException {
            Entry<K,V> t = new Entry<K,V>(key, element);
            t.color = color;
            return t;
        }

        public final K getKey() {
            return key;
        }

        /**
         * return the element value
         */
        public final V getValue() {
            return element;
        }

        /**
         * set the element value
         */
        public final V setValue(V v) {
            V old = element;
            element = v;
            return old;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            return eq(key, e.getKey()) && eq(element, e.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^
                   (element == null ? 0 : element.hashCode());
        }

        public String toString() {
            return key + "=" + element;
        }
    }

    /**
     * Return the inorder successor, or null if no such
     */
    private static Entry<?,?> successor(Entry<?,?> e) {
        if (e.right != null) {
            for (e = e.right; e.left != null; e = e.left) {}
            return e;
        } else {
            Entry<?,?> p = e.parent;
            while (p != null && e == p.right) {
                e = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Return the inorder predecessor, or null if no such
     */
    private static Entry<?,?> predecessor(Entry<?,?> e) {
        if (e.left != null) {
            for (e = e.left; e.right != null; e = e.right) {}
            return e;
        }
        else {
            Entry<?,?> p = e.parent;
            while (p != null && e == p.left) {
                e = p;
                p = p.parent;
            }
            return p;
        }
    }

    private Entry<K,V> getEntry(Object key) {
        Entry<K,V> t = root;
        if (comparator != null) {
            for (;;) {
                if (t == null) return null;
                int diff = comparator.compare(key, t.key);
                if (diff == 0) return t;
                t = (diff < 0) ? t.left : t.right;
            }
        }
        else {
            Comparable<Object> c = (Comparable<Object>)key;
            for (;;) {
                if (t == null) return null;
                int diff = c.compareTo(t.key);
                if (diff == 0) return t;
                t = (diff < 0) ? t.left : t.right;
            }
        }
    }

    private Entry<K,V> getHigherEntry(Object key) {
        Entry<K,V> t = root;
        if (t == null) return null;
        for (;;) {
            int diff = compare(key, t.key, comparator);
            if (diff < 0) {
                if (t.left != null) t = t.left; else return t;
            }
            else {
                if (t.right != null) {
                    t = t.right;
                }
                else {
                    Entry<K,V> parent = t.parent;
                    while (parent != null && t == parent.right) {
                        t = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
    }

    private Entry<K,V> getFirstEntry() {
        Entry<K,V> e = root;
        if (e == null) return null;
        while (e.left != null) e = e.left;
        return e;
    }

    private Entry<K,V> getLastEntry() {
        Entry<K,V> e = root;
        if (e == null) return null;
        while (e.right != null) e = e.right;
        return e;
    }

    private Entry<K,V> getCeilingEntry(Object key) {
        Entry<K,V> e = root;
        if (e == null) return null;
        for (;;) {
            int diff = compare(key, e.key, comparator);
            if (diff < 0) {
                if (e.left != null) e = e.left; else return e;
            }
            else if (diff > 0) {
                if (e.right != null) {
                    e = e.right;
                }
                else {
                    Entry<K,V> p = e.parent;
                    while (p != null && e == p.right) {
                        e = p;
                        p = p.parent;
                    }
                    return p;
                }
            }
            else return e;
        }
    }

    private Entry<K,V> getLowerEntry(Object key) {
        Entry<K,V> e = root;
        if (e == null) return null;
        for (;;) {
            int diff = compare(key, e.key, comparator);
            if (diff > 0) {
                if (e.right != null) e = e.right; else return e;
            }
            else {
                if (e.left != null) {
                    e = e.left;
                }
                else {
                    Entry<K,V> p = e.parent;
                    while (p != null && e == p.left) {
                        e = p;
                        p = p.parent;
                    }
                    return p;
                }
            }
        }
    }

    private Entry<K,V> getFloorEntry(Object key) {
        Entry<K,V> e = root;
        if (e == null) return null;
        for (;;) {
            int diff = compare(key, e.key, comparator);
            if (diff > 0) {
                if (e.right != null) e = e.right; else return e;
            }
            else if (diff < 0) {
                if (e.left != null) {
                    e = e.left;
                }
                else {
                    Entry<K,V> p = e.parent;
                    while (p != null && e == p.left) {
                        e = p;
                        p = p.parent;
                    }
                    return p;
                }
            }
            else return e;
        }
    }

    void buildFromSorted(Iterator<Map.Entry<K,V>> itr, int size) {
        modCount++;
        this.size = size;
        // nodes at the bottom (unbalanced) level must be red
        int bottom = 0;
        for (int ssize = 1; ssize-1 < size; ssize <<= 1) bottom++;
        this.root = (Entry<K, V>) createFromSorted(itr, size, 0, bottom);
    }

    private static Entry<Object,Object> createFromSorted(Iterator<?> itr, int size,
                                          int level, int bottom) {
        level++;
        if (size == 0) return null;
        int leftSize = (size-1) >> 1;
        int rightSize = size-1-leftSize;
        Entry<Object,Object> left = createFromSorted(itr, leftSize, level, bottom);
        Map.Entry<?,?> orig = (Map.Entry<?,?>)itr.next();
        Entry<Object,Object> right = createFromSorted(itr, rightSize, level, bottom);
        Entry<Object,Object> e = new Entry<Object, Object>(orig.getKey(), orig.getValue());
        if (left != null) {
            e.left = left;
            left.parent = e;
        }
        if (right != null) {
            e.right = right;
            right.parent = e;
        }
        if (level == bottom) e.color = Entry.RED;
        return e;
    }

    /**
     * Delete the current node, and then rebalance the tree it is in
     * @param root the root of the current tree
     * @return the new root of the current tree. (Rebalancing
     * can change the root!)
     */
    private void delete(Entry<K,V> e) {

        // handle case where we are only node
        if (e.left == null && e.right == null && e.parent == null) {
            root = null;
            size = 0;
            modCount++;
            return;
        }
        // if strictly internal, swap places with a successor
        if (e.left != null && e.right != null) {
            Entry<?,?> s = successor(e);
            e.key = (K) s.key;
            e.element = (V) s.element;
            e = (Entry<K, V>) s;
        }

        // Start fixup at replacement node (normally a child).
        // But if no children, fake it by using self

        if (e.left == null && e.right == null) {

            if (e.color == Entry.BLACK)
                fixAfterDeletion(e);

            // Unlink  (Couldn't before since fixAfterDeletion needs parent ptr)

            if (e.parent != null) {
                if (e == e.parent.left)
                    e.parent.left = null;
                else if (e == e.parent.right)
                    e.parent.right = null;
                e.parent = null;
            }

        }
        else {
            Entry<K,V> replacement = e.left;
            if (replacement == null)
                replacement = e.right;

            // link replacement to parent
            replacement.parent = e.parent;

            if (e.parent == null)
                root = (Entry<K, V>) replacement;
            else if (e == e.parent.left)
                e.parent.left = replacement;
            else
                e.parent.right = replacement;

            e.left = null;
            e.right = null;
            e.parent = null;

            // fix replacement
            if (e.color == Entry.BLACK)
                fixAfterDeletion(replacement);

        }

        size--;
        modCount++;
    }

    /**
     * Return color of node p, or BLACK if p is null
     * (In the CLR version, they use
     * a special dummy `nil' node for such purposes, but that doesn't
     * work well here, since it could lead to creating one such special
     * node per real node.)
     *
     */
    static boolean colorOf(Entry<?,?> p) {
        return (p == null) ? Entry.BLACK : p.color;
    }

    /**
     * return parent of node p, or null if p is null
     */
    static Entry parentOf(Entry p) {
        return (p == null) ? null : p.parent;
    }

    /**
     * Set the color of node p, or do nothing if p is null
     */
    private static void setColor(Entry<?,?> p, boolean c) {
        if (p != null) p.color = c;
    }

    /**
     * return left child of node p, or null if p is null
     */
    private static Entry leftOf(Entry p) {
        return (p == null) ? null : p.left;
    }

    /**
     * return right child of node p, or null if p is null
     */
    private static Entry rightOf(Entry p) {
        return (p == null) ? null : p.right;
    }

    /** From CLR */
    private final void rotateLeft(Entry<K,V> e) {
        Entry<K,V> r = e.right;
        e.right = r.left;
        if (r.left != null)
            r.left.parent = e;
        r.parent = e.parent;
        if (e.parent == null) root = (Entry<K, V>) r;
        else if (e.parent.left == e)
            e.parent.left = r;
        else
            e.parent.right = r;
        r.left = e;
        e.parent = r;
    }

    /** From CLR */
    private final void rotateRight(Entry<K,V> e) {
        Entry<K,V> l = e.left;
        e.left = l.right;
        if (l.right != null)
            l.right.parent = e;
        l.parent = e.parent;
        if (e.parent == null) root = (Entry<K, V>) l;
        else if (e.parent.right == e)
            e.parent.right = l;
        else
            e.parent.left = l;
        l.right = e;
        e.parent = l;
    }

    /** From CLR */
    private final void fixAfterInsertion(Entry<K,V> e) {
        e.color = Entry.RED;
        Entry<K, V> x = e;

        while (x != null && x != root && x.parent.color == Entry.RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == Entry.RED) {
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(y, Entry.BLACK);
                    setColor(parentOf(parentOf(x)), Entry.RED);
                    x = (Entry<K, V>) parentOf(parentOf(x));
                }
                else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(parentOf(parentOf(x)), Entry.RED);
                    if (parentOf(parentOf(x)) != null)
                        rotateRight(parentOf(parentOf(x)));
                }
            }
            else {
                Entry<Object, Object> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == Entry.RED) {
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(y, Entry.BLACK);
                    setColor(parentOf(parentOf(x)), Entry.RED);
                    x = parentOf(parentOf(x));
                }
                else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(parentOf(parentOf(x)), Entry.RED);
                    if (parentOf(parentOf(x)) != null)
                        rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = Entry.BLACK;
    }

    /** From CLR */
    private final Entry<K,V> fixAfterDeletion(Entry<K,V> e) {
        Entry<K,V> x = e;
        while (x != root && colorOf(x) == Entry.BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<Object, Object> sib = rightOf(parentOf(x));
                if (colorOf(sib) == Entry.RED) {
                    setColor(sib, Entry.BLACK);
                    setColor(parentOf(x), Entry.RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                if (colorOf(leftOf(sib)) == Entry.BLACK &&
                    colorOf(rightOf(sib)) == Entry.BLACK) {
                    setColor(sib, Entry.RED);
                    x = parentOf(x);
                }
                else {
                    if (colorOf(rightOf(sib)) == Entry.BLACK) {
                        setColor(leftOf(sib), Entry.BLACK);
                        setColor(sib, Entry.RED);
                        rotateRight((Entry<K, V>) sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(rightOf(sib), Entry.BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            }
            else {
                Entry<Object, Object> sib = leftOf(parentOf(x));
                if (colorOf(sib) == Entry.RED) {
                    setColor(sib, Entry.BLACK);
                    setColor(parentOf(x), Entry.RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                if (colorOf(rightOf(sib)) == Entry.BLACK &&
                    colorOf(leftOf(sib)) == Entry.BLACK) {
                    setColor(sib, Entry.RED);
                    x = parentOf(x);
                }
                else {
                    if (colorOf(leftOf(sib)) == Entry.BLACK) {
                        setColor(rightOf(sib), Entry.BLACK);
                        setColor(sib, Entry.RED);
                        rotateLeft((Entry<K, V>) sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(leftOf(sib), Entry.BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        setColor(x, Entry.BLACK);
        return root;
    }

    private class BaseEntryIterator {
        Entry<K, V> cursor;
        Entry<K, V> lastRet;
        int expectedModCount;
        BaseEntryIterator(Entry<K, V> cursor) {
            this.cursor = cursor;
            this.expectedModCount = modCount;
        }
        public boolean hasNext() {
            return (cursor != null);
        }
        Entry<K,V> nextEntry() {
            Entry<K,V> curr = cursor;
            if (curr == null) throw new NoSuchElementException();
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            cursor = (Entry<K, V>) successor(curr);
            lastRet = curr;
            return curr;
        }
        Entry<K,V> prevEntry() {
            Entry<K,V> curr = cursor;
            if (curr == null) throw new NoSuchElementException();
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            cursor = (Entry<K, V>) predecessor(curr);
            lastRet = curr;
            return curr;
        }
        public void remove() {
            if (lastRet == null) throw new IllegalStateException();
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            // if removal strictly internal, it swaps places with a successor
            if (lastRet.left != null && lastRet.right != null && cursor != null) cursor = lastRet;
            delete(lastRet);
            lastRet = null;
            expectedModCount++;
        }
    }

    class EntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<K,V>> {
        EntryIterator(Entry<K,V> cursor) { super(cursor); }
        public Map.Entry<K,V> next() { return nextEntry(); }
    }

    class KeyIterator extends BaseEntryIterator implements Iterator<K> {
        KeyIterator(Entry<K,V> cursor) { super(cursor); }
        public K next() { return nextEntry().key; }
    }

    class ValueIterator extends BaseEntryIterator implements Iterator<V> {
        ValueIterator(Entry<K,V> cursor) { super(cursor); }
        public V next() { return nextEntry().element; }
    }

    class DescendingEntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<K,V>> {
        DescendingEntryIterator(Entry<K,V> cursor) { super(cursor); }
        public Map.Entry<K,V> next() { return prevEntry(); }
    }

    class DescendingKeyIterator extends BaseEntryIterator implements Iterator<K> {
        DescendingKeyIterator(Entry<K,V> cursor) { super(cursor); }
        public K next() { return prevEntry().key; }
    }

    class DescendingValueIterator extends BaseEntryIterator implements Iterator<V> {
        DescendingValueIterator(Entry<K,V> cursor) { super(cursor); }
        public V next() { return prevEntry().element; }
    }

    private Entry<K,V> getMatchingEntry(Object o) {
        if (!(o instanceof Map.Entry)) return null;
        Map.Entry<K,V> e = (Map.Entry<K,V>)o;
        Entry<K,V> found = TreeMap.this.getEntry(e.getKey());
        return (found != null && eq(found.getValue(), e.getValue())) ? found : null;
    }

    class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator( getFirstEntry());
        }

        public boolean contains(Object o) {
            return getMatchingEntry(o) != null;
        }

        public boolean remove(Object o) {
            Entry<K,V> e = getMatchingEntry(o);
            if (e == null) return false;
            delete(e);
            return true;
        }
    }

    class DescendingEntrySet extends EntrySet {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new DescendingEntryIterator(getLastEntry());
        }
    }

    class ValueSet extends AbstractSet<V> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public boolean contains(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = (Entry<K, V>) successor(e)) {
                if (eq(o, e.element)) return true;
            }
            return false;
        }

        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        public boolean remove(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = (Entry<K, V>) successor(e)) {
                if (eq(o, e.element)) {
                    delete(e);
                    return true;
                }
            }
            return false;
        }
    }

    abstract class KeySet<T extends Object> extends AbstractSet<T> implements NavigableSet<T> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public boolean contains(Object o) {
            return getEntry(o) != null;
        }

        public boolean remove(Object o) {
            Entry<K,V> found = getEntry(o);
            if (found == null) return false;
            delete(found);
            return true;
        }
        public SortedSet<T> subSet( T fromElement, T toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public SortedSet<T> headSet( T toElement) {
            return headSet(toElement, false);
        }
        public SortedSet<T> tailSet( T fromElement) {
            return tailSet(fromElement, true);
        }
    }

    class AscendingKeySet extends KeySet<K> {

        public Iterator<K> iterator() {
            return new KeyIterator(getFirstEntry());
        }

        public Iterator<K> descendingIterator() {
            return new DescendingKeyIterator( getFirstEntry());
        }

        public Object lower(Object e)   { return lowerKey(e); }
        public Object floor(Object e)   { return floorKey(e); }
        public Object ceiling(Object e) { return ceilingKey(e); }
        public Object higher(Object e)  { return higherKey(e); }
        public K first()           { return (K) firstKey(); }
        public K last()            { return (K) lastKey(); }
        public Comparator<Object> comparator()  { return TreeMap.this.comparator(); }

        public K pollFirst() {
            Map.Entry<K,V> e = pollFirstEntry();
            return e == null? null : e.getKey();
        }
        public K pollLast() {
            Map.Entry<K,V> e = pollLastEntry();
            return e == null? null : e.getKey();
        }

        public NavigableSet<K> subSet( K fromElement, boolean fromInclusive,
                                   K toElement,   boolean toInclusive) {
            return (NavigableSet<K>)(subMap(fromElement, fromInclusive,
                                         toElement,   toInclusive)).keySet();
        }
        public NavigableSet<K> headSet( K toElement, boolean inclusive) {
            return (NavigableSet<K>)(headMap(toElement, inclusive)).keySet();
        }
        public NavigableSet<K> tailSet( K fromElement, boolean inclusive) {
            return (NavigableSet<K>)(tailMap(fromElement, inclusive)).keySet();
        }
        public NavigableSet<K> descendingSet() {
            return (NavigableSet<K>)descendingMap().keySet();
        }
    }

    class DescendingKeySet extends KeySet<K> {

        public Iterator<K> iterator() {
            return new DescendingKeyIterator( getLastEntry());
        }

        public Iterator<K> descendingIterator() {
            return new KeyIterator( getFirstEntry());
        }

        public Object lower(Object e)   { return higherKey(e); }
        public Object floor(Object e)   { return ceilingKey(e); }
        public Object ceiling(Object e) { return floorKey(e); }
        public Object higher(Object e)  { return lowerKey(e); }
        public K first()           { return (K) lastKey(); }
        public K last()            { return (K) firstKey(); }
        public Comparator<K> comparator()  { return (Comparator<K>) descendingMap().comparator(); }

        public K pollFirst() {
            Map.Entry<K,V> e = pollLastEntry();
            return e == null? null : e.getKey();
        }
        public K pollLast() {
            Map.Entry<K,V> e = pollFirstEntry();
            return e == null? null : e.getKey();
        }

        public NavigableSet<K> subSet( K fromElement, boolean fromInclusive,
                                   K toElement,   boolean toInclusive) {
            return (NavigableSet<K>)(descendingMap().subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive)).keySet();
        }
        public NavigableSet<K> headSet( K toElement, boolean inclusive) {
            return (NavigableSet<K>)(descendingMap().headMap(toElement, inclusive)).keySet();
        }
        public NavigableSet<K> tailSet( K fromElement, boolean inclusive) {
            return (NavigableSet<K>)(descendingMap().tailMap(fromElement, inclusive)).keySet();
        }
        public NavigableSet<K> descendingSet() {
            return (NavigableSet<K>)keySet();
        }
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    private static int compare(Object o1, Object o2, Comparator<Object> cmp) {
        return (cmp == null)
            ? ((Comparable<Object>)o1).compareTo(o2)
            : cmp.compare(o1, o2);
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> lowerEntry(Object key) {
        Map.Entry<K,V> e = getLowerEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Object lowerKey(Object key) {
        Map.Entry<K,V> e = getLowerEntry(key);
        return (e == null) ? null : e.getKey();
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> floorEntry(Object key) {
        Entry<K,V> e = getFloorEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Object floorKey(Object key) {
        Entry<K,V> e = getFloorEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> ceilingEntry(Object key) {
        Entry<K,V> e = getCeilingEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Object ceilingKey(Object key) {
        Entry<K,V> e = getCeilingEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> higherEntry(Object key) {
        Entry<K,V> e = getHigherEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Object higherKey(Object key) {
        Entry<K,V> e = getHigherEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> firstEntry() {
        Entry<K,V> e = getFirstEntry();
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> lastEntry() {
        Entry<K,V> e = getLastEntry();
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> pollFirstEntry() {
        Entry<K,V> e = getFirstEntry();
        if (e == null) return null;
        Map.Entry<K,V> res = new AbstractMap.SimpleImmutableEntry<K,V>(e);
        delete( e);
        return res;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> pollLastEntry() {
        Entry<K,V> e = getLastEntry();
        if (e == null) return null;
        Map.Entry<K,V> res = new AbstractMap.SimpleImmutableEntry<K,V>(e);
        delete( e);
        return res;
    }

    /**
     * @since 1.6
     */
    public NavigableMap<K,V> descendingMap() {
        NavigableMap<K,V> map = descendingMap;
        if (map == null) {
            descendingMap = map = (NavigableMap<K, V>) new DescendingSubMap(true, null, true,
                                                       true, null, true);
        }
        return map;
    }

    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    public SortedMap<K,V> subMap( K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    public SortedMap<K,V> headMap( K toKey) {
        return headMap(toKey, false);
    }

    public SortedMap<K,V> tailMap( K fromKey) {
        return tailMap(fromKey, true);
    }

    public NavigableMap<K,V> subMap( K fromKey, boolean fromInclusive,
                               K toKey,   boolean toInclusive) {
        return new AscendingSubMap(false, fromKey, fromInclusive,
                                   false, toKey, toInclusive);
    }

    public NavigableMap<K,V> headMap( K toKey, boolean toInclusive) {
        return new AscendingSubMap(true,  null,  true,
                                   false, toKey, toInclusive);
    }

    public NavigableMap<K,V> tailMap( K fromKey, boolean fromInclusive) {
        return new AscendingSubMap(false, fromKey, fromInclusive,
                                   true,  null,    true);
    }

    public Comparator<Object> comparator() {
        return comparator;
    }

    final Comparator<Object> reverseComparator() {
        if (reverseComparator == null) {
            reverseComparator = Collections.reverseOrder(comparator);
        }
        return reverseComparator;
    }

    public K firstKey() {
        Entry<K,V> e = getFirstEntry();
        if (e == null) throw new NoSuchElementException();
        return e.key;
    }

    public K lastKey() {
        Entry<K,V> e = getLastEntry();
        if (e == null) throw new NoSuchElementException();
        return e.key;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsValue(Object value) {
        if (root == null) return false;
        return (value == null) ? containsNull(root) : containsValue(root, value);
    }

    private static boolean containsNull(Entry<?,?> e) {
        if (e.element == null) return true;
        if (e.left != null && containsNull(e.left)) return true;
        if (e.right != null && containsNull(e.right)) return true;
        return false;
    }

    private static boolean containsValue(Entry<?,?> e, Object val) {
        if (val.equals(e.element)) return true;
        if (e.left != null && containsValue(e.left, val)) return true;
        if (e.right != null && containsValue(e.right, val)) return true;
        return false;
    }

    public V remove(Object key) {
        Entry<K,V> e = getEntry(key);
        if (e == null) return null;
        V old = (V) e.getValue();
        delete( e);
        return old;
    }

    public void putAll(Map<? extends K,? extends V> map) {
        if (map instanceof SortedMap) {
            SortedMap<K,V> smap = (SortedMap<K,V>)map;
            if (eq(this.comparator, smap.comparator())) {
                this.buildFromSorted(smap.entrySet().iterator(), map.size());
                return;
            }
        }
        // not a sorted map, or comparator mismatch
        super.putAll(map);
    }

    public Set<K> keySet() {
        return navigableKeySet();
    }

    public NavigableSet<K> navigableKeySet() {
        if (navigableKeySet == null) {
            navigableKeySet = new AscendingKeySet();
        }
        return navigableKeySet;
    }

//    public Collection values() {
//        if (valueSet == null) {
//            valueSet = new ValueSet();
//        }
//        return valueSet;
//    }
//
    private abstract class NavigableSubMap extends AbstractMap<K,V>
                                           implements NavigableMap<K,V>, Serializable {

        private static final long serialVersionUID = -6520786458950516097L;

        final K fromKey, toKey;
        final boolean fromStart, toEnd;
        final boolean fromInclusive, toInclusive;
        transient int cachedSize = -1, cacheVersion;
        transient Set<Map.Entry<K,V>> entrySet;
        transient NavigableMap<K,V> descendingMap;
        transient NavigableSet<K> navigableKeySet;

        NavigableSubMap(boolean fromStart, K fromKey, boolean fromInclusive,
                        boolean toEnd,     K toKey,   boolean toInclusive) {
            if (!fromStart && !toEnd) {
                if (compare(fromKey, toKey, comparator) > 0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            }
            else {
                if (!fromStart) compare(fromKey, fromKey, comparator);
                if (!toEnd) compare(toKey, toKey, comparator);
            }
            this.fromStart = fromStart;
            this.toEnd = toEnd;
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.fromInclusive = fromInclusive;
            this.toInclusive = toInclusive;
        }

        final TreeMap.Entry<K,V> checkLoRange(TreeMap.Entry<K,V> e) {
            return (e == null || absTooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> checkHiRange(TreeMap.Entry<K,V> e) {
            return (e == null || absTooHigh(e.key)) ? null : e;
        }

        final boolean inRange( K key) {
            return !absTooLow(key) && !absTooHigh(key);
        }

        final boolean inRangeExclusive( K key) {
            return (fromStart || compare(key, fromKey, comparator) >= 0)
                && (toEnd     || compare(toKey, key, comparator) >= 0);
        }

        final boolean inRange( K key, boolean inclusive) {
            return inclusive ? inRange(key) : inRangeExclusive(key);
        }

        private boolean absTooHigh( K key) {
            if (toEnd) return false;
            int c = compare(key, toKey, comparator);
            return (c > 0 || (c == 0 && !toInclusive));
        }

        private boolean absTooLow( K key) {
            if (fromStart) return false;
            int c = compare(key, fromKey, comparator);
            return (c < 0 || (c == 0 && !fromInclusive));
        }

        protected abstract TreeMap.Entry<K,V> first();
        protected abstract TreeMap.Entry<K,V> last();
        protected abstract TreeMap.Entry<K,V> lower( K key);
        protected abstract TreeMap.Entry<K,V> floor( K key);
        protected abstract TreeMap.Entry<K,V> ceiling( K key);
        protected abstract TreeMap.Entry<K,V> higher( K key);
        protected abstract TreeMap.Entry<K,V> uncheckedHigher(TreeMap.Entry<K,V> e);

        // absolute comparisons, for use by subclasses

        final TreeMap.Entry<K,V> absLowest() {
            return checkHiRange((fromStart) ? getFirstEntry() :
                fromInclusive ? getCeilingEntry(fromKey) : getHigherEntry(fromKey));
        }

        final TreeMap.Entry<K,V> absHighest() {
            return checkLoRange((toEnd) ? getLastEntry() :
                toInclusive ? getFloorEntry(toKey) : getLowerEntry(toKey));
        }

        final TreeMap.Entry<K,V> absLower( K key) {
            return absTooHigh(key) ? absHighest() : checkLoRange(getLowerEntry(key));
        }

        final TreeMap.Entry<K,V> absFloor( K key) {
            return absTooHigh(key) ? absHighest() : checkLoRange(getFloorEntry(key));
        }

        final TreeMap.Entry<K,V> absCeiling( K key) {
            return absTooLow(key) ? absLowest() : checkHiRange(getCeilingEntry(key));
        }

        final TreeMap.Entry<K,V> absHigher( K key) {
            return absTooLow(key) ? absLowest() : checkHiRange(getHigherEntry(key));
        }

        // navigable implementations, using subclass-defined comparisons

        public Map.Entry<K,V> firstEntry() {
            TreeMap.Entry<K,V> e = first();
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K firstKey() {
            TreeMap.Entry<K,V> e = first();
            if (e == null) throw new NoSuchElementException();
            return (K) e.key;
        }

        public Map.Entry<K,V> lastEntry() {
            TreeMap.Entry<K,V> e = last();
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K lastKey() {
            TreeMap.Entry<K,V> e = last();
            if (e == null) throw new NoSuchElementException();
            return (K) e.key;
        }

        public Map.Entry<K,V> pollFirstEntry() {
            TreeMap.Entry<K,V> e = first();
            if (e == null) return null;
            Map.Entry<K,V> result = new SimpleImmutableEntry<K,V>(e);
            delete( e);
            return result;
        }

        public java.util.Map.Entry<K,V> pollLastEntry() {
            TreeMap.Entry<K,V> e = last();
            if (e == null) return null;
            Map.Entry<K,V> result = new SimpleImmutableEntry<K,V>(e);
            delete(e);
            return result;
        }

        public Map.Entry<K,V> lowerEntry( K key) {
            TreeMap.Entry<K,V> e = lower(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K lowerKey( K key) {
            TreeMap.Entry<K,V> e = lower(key);
            return (e == null) ? null : e.key;
        }

        public Map.Entry<K,V> floorEntry( K key) {
            TreeMap.Entry<K,V> e = floor(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K floorKey( K key) {
            TreeMap.Entry<K,V> e = floor(key);
            return (e == null) ? null : e.key;
        }

        public Map.Entry<K,V> ceilingEntry( K key) {
            TreeMap.Entry<K,V> e = ceiling(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K ceilingKey( K key) {
            TreeMap.Entry<K,V> e = ceiling(key);
            return (e == null) ? null : e.key;
        }

        public Map.Entry<K,V> higherEntry( K key) {
            TreeMap.Entry<K,V> e = higher(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry<K,V>(e);
        }

        public K higherKey( K key) {
            TreeMap.Entry<K,V> e = higher(key);
            return (e == null) ? null : e.key;
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public SortedMap<K,V> subMap( K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<K,V> headMap( K toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<K,V> tailMap( K fromKey) {
            return tailMap(fromKey, true);
        }

        public int size() {
            if (cachedSize < 0 || cacheVersion != modCount) {
                cachedSize = recalculateSize();
                cacheVersion = modCount;
            }
            return cachedSize;
        }

        private int recalculateSize() {
            TreeMap.Entry<K,V> terminator = absHighest();
            Object terminalKey = terminator != null ? terminator.key : null;

            int size = 0;
            for (TreeMap.Entry<K,V> e = absLowest(); e != null;
                 e = (net.jxta.impl.util.backport.java.util.TreeMap.Entry<K, V>) ((e.key == terminalKey) ? null : successor(e))) {
                size++;
            }
            return size;
        }

        public boolean isEmpty() {
            return absLowest() == null;
        }

        public boolean containsKey( Object key) {
            return (inRange((K) key) && TreeMap.this.containsKey(key));
        }

        public V get(Object key) {
            if (!inRange((K) key)) return null;
            else return TreeMap.this.get(key);
        }

        public V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("Key out of range");
            return TreeMap.this.put(key, value);
        }

        public V remove(Object key) {
            if (!inRange((K) key)) return null;
            return TreeMap.this.remove(key);
        }

        public Set<Map.Entry<K,V>> entrySet() {
            if (entrySet == null) {
                entrySet = new SubEntrySet();
            }
            return entrySet;
        }

        public Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> navigableKeySet() {
            if (navigableKeySet == null) {
                navigableKeySet = new SubKeySet();
            }
            return navigableKeySet;
        }

        private TreeMap.Entry<K,V> getMatchingSubEntry(Object o) {
            if (!(o instanceof Map.Entry)) return null;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            K key = e.getKey();
            if (!inRange(key)) return null;
            TreeMap.Entry<K,V> found = getEntry(key);
            return (found != null && eq(found.getValue(), e.getValue())) ? found : null;
        }

        class SubEntrySet extends AbstractSet<Map.Entry<K, V>> {
            public int size() { return NavigableSubMap.this.size(); }
            public boolean isEmpty() { return NavigableSubMap.this.isEmpty(); }

            public boolean contains(Object o) {
                return getMatchingSubEntry(o) != null;
            }

            public boolean remove(Object o) {
                TreeMap.Entry<K,V> e = getMatchingSubEntry(o);
                if (e == null) return false;
                delete( e);
                return true;
            }

            public Iterator<Map.Entry<K, V>> iterator() {
                return new SubEntryIterator();
            }
        }

        class SubKeySet extends AbstractSet<K> implements NavigableSet<K> {
            public int size() { return NavigableSubMap.this.size(); }
            public boolean isEmpty() { return NavigableSubMap.this.isEmpty(); }
            public void clear() { NavigableSubMap.this.clear(); }

            public boolean contains(Object o) {
                return getEntry(o) != null;
            }

            public boolean remove(Object o) {
                if (!inRange((K) o)) return false;
                TreeMap.Entry<K,V> found = getEntry(o);
                if (found == null) return false;
                delete(found);
                return true;
            }
            public SortedSet<K> subSet( K fromElement, K toElement) {
                return subSet(fromElement, true, toElement, false);
            }
            public SortedSet<K> headSet( K toElement) {
                return headSet(toElement, false);
            }
            public SortedSet<K> tailSet( K fromElement) {
                return tailSet(fromElement, true);
            }

            public Iterator<K> iterator() {
                return new SubKeyIterator(NavigableSubMap.this.entrySet().iterator());
            }

            public Iterator<K> descendingIterator() {
                return new SubKeyIterator(NavigableSubMap.this.descendingMap().entrySet().iterator());
            }

            public K lower( K e)   { return NavigableSubMap.this.lowerKey(e); }
            public K floor( K e)   { return NavigableSubMap.this.floorKey(e); }
            public K ceiling( K e) { return NavigableSubMap.this.ceilingKey(e); }
            public K higher( K e)  { return NavigableSubMap.this.higherKey(e); }
            public K first()           		{ return NavigableSubMap.this.firstKey(); }
            public K last()					{ return NavigableSubMap.this.lastKey(); }
            public Comparator<K> comparator()  { return (Comparator<K>) NavigableSubMap.this.comparator(); }

            public K pollFirst() {
                Map.Entry<K,V> e = NavigableSubMap.this.pollFirstEntry();
                return e == null? null : e.getKey();
            }
            public K pollLast() {
                Map.Entry<K,V> e = NavigableSubMap.this.pollLastEntry();
                return e == null? null : e.getKey();
            }

            public NavigableSet<K> subSet( K fromElement, boolean fromInclusive,
                                       K toElement,   boolean toInclusive) {
                return (NavigableSet<K>)(NavigableSubMap.this.subMap(fromElement, fromInclusive,
                                             toElement,   toInclusive)).keySet();
            }
            public NavigableSet<K> headSet( K toElement, boolean inclusive) {
                return (NavigableSet<K>)(NavigableSubMap.this.headMap(toElement, inclusive)).keySet();
            }
            public NavigableSet<K> tailSet( K fromElement, boolean inclusive) {
                return (NavigableSet<K>)(NavigableSubMap.this.tailMap(fromElement, inclusive)).keySet();
            }
            public NavigableSet<K> descendingSet() {
                return (NavigableSet<K>)NavigableSubMap.this.descendingMap().keySet();
            }
        }

        class SubEntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<K,V>> {
            final Object terminalKey;
            SubEntryIterator() {
                super(first());
                TreeMap.Entry<K,V> terminator = last();
                this.terminalKey = terminator == null ? null : terminator.key;
            }
            public boolean hasNext() {
                return cursor != null;
            }
            public TreeMap.Entry<K,V> next() {
                TreeMap.Entry<K,V> curr = (net.jxta.impl.util.backport.java.util.TreeMap.Entry<K, V>) cursor;
                if (curr == null) throw new NoSuchElementException();
                if (expectedModCount != modCount)
                    throw new ConcurrentModificationException();
                cursor = (curr.key == terminalKey) ? null : uncheckedHigher(curr);
                lastRet = curr;
                return curr;
            }
        }

        class SubKeyIterator implements Iterator<K> {
            final Iterator<Map.Entry<K,V>> itr;
            SubKeyIterator(Iterator<Map.Entry<K,V>> itr) { this.itr = itr; }
            public boolean hasNext()     { return itr.hasNext(); }
            public K next() { return itr.next().getKey(); }
            public void remove()         { itr.remove(); }
        }
    }

    class AscendingSubMap extends NavigableSubMap {
		private static final long serialVersionUID = 1L;

		AscendingSubMap(boolean fromStart, K fromKey, boolean fromInclusive,
                        boolean toEnd,     K toKey,   boolean toInclusive) {
            super(fromStart, fromKey, fromInclusive, toEnd, toKey, toInclusive);
        }

        public Comparator<Object> comparator() {
            return comparator;
        }

        protected TreeMap.Entry<K,V> first()             { return absLowest(); }
        protected TreeMap.Entry<K,V> last()              { return absHighest(); }
        protected TreeMap.Entry<K,V> lower( K key)   { return absLower(key); }
        protected TreeMap.Entry<K,V> floor( K key)   { return absFloor(key); }
        protected TreeMap.Entry<K,V> ceiling( K key) { return absCeiling(key); }
        protected TreeMap.Entry<K,V> higher( K key)  { return absHigher(key); }

        protected TreeMap.Entry<K,V> uncheckedHigher(TreeMap.Entry<K,V> e) {
            return (net.jxta.impl.util.backport.java.util.TreeMap.Entry<K, V>) successor(e);
        }

        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                   K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(false, fromKey, fromInclusive,
                                       false, toKey, toInclusive);
        }

        public NavigableMap<K,V> headMap(K toKey, boolean toInclusive) {
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(fromStart, fromKey, fromInclusive,
                                       false, toKey, toInclusive);
        }

        public NavigableMap<K,V> tailMap(K fromKey, boolean fromInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new AscendingSubMap(false, fromKey, fromInclusive,
                                       toEnd, toKey, toInclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            if (descendingMap == null) {
                descendingMap =
                    new DescendingSubMap(fromStart, fromKey, fromInclusive,
                                         toEnd,     toKey,   toInclusive);
            }
            return descendingMap;
        }
    }

    class DescendingSubMap extends NavigableSubMap {
		private static final long serialVersionUID = 1L;

		DescendingSubMap(boolean fromStart, K fromKey, boolean fromInclusive,
                         boolean toEnd,     K toKey,   boolean toInclusive) {
            super(fromStart, fromKey, fromInclusive, toEnd, toKey, toInclusive);
        }

        public Comparator<Object> comparator() { return TreeMap.this.reverseComparator(); }

        protected TreeMap.Entry<K,V> first()             { return absHighest(); }
        protected TreeMap.Entry<K,V> last()              { return absLowest(); }
        protected TreeMap.Entry<K,V> lower( K key)   { return absHigher(key); }
        protected TreeMap.Entry<K,V> floor( K key)   { return absCeiling(key); }
        protected TreeMap.Entry<K,V> ceiling( K key) { return absFloor(key); }
        protected TreeMap.Entry<K,V> higher( K key)  { return absLower(key); }

        protected TreeMap.Entry<K,V> uncheckedHigher(TreeMap.Entry<K,V> e) {
            return (net.jxta.impl.util.backport.java.util.TreeMap.Entry<K, V>) predecessor(e);
        }

        public NavigableMap<K,V> subMap( K fromKey, boolean fromInclusive,
                                         K toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(false, toKey, toInclusive,
                                        false, fromKey, fromInclusive);
        }

        public NavigableMap<K,V> headMap( K toKey, boolean toInclusive) {
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(false, toKey, toInclusive,
                                        this.toEnd, this.toKey, this.toInclusive);
        }

        public NavigableMap<K,V> tailMap( K fromKey, boolean fromInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new DescendingSubMap(this.fromStart, this.fromKey, this.fromInclusive,
                                        false, fromKey, fromInclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            if (descendingMap == null) {
                descendingMap =
                    new AscendingSubMap(fromStart, fromKey, fromInclusive,
                                        toEnd,     toKey,   toInclusive);
            }
            return descendingMap;
        }
    }

    // serialization

    static class IteratorIOException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		IteratorIOException(java.io.IOException e) {
            super(e);
        }
        java.io.IOException getException() {
            return (java.io.IOException)getCause();
        }
    }

    static class IteratorNoClassException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		IteratorNoClassException(ClassNotFoundException e) {
            super(e);
        }
        ClassNotFoundException getException() {
            return (ClassNotFoundException)getCause();
        }
    }

    static class IOIterator implements Iterator<Map.Entry<?,?>> {
        final java.io.ObjectInputStream ois;
        int remaining;
        IOIterator(java.io.ObjectInputStream ois, int remaining) {
            this.ois = ois;
            this.remaining = remaining;
        }
        public boolean hasNext() {
            return remaining > 0;
        }
        public SimpleImmutableEntry<?, ?> next() {
            if (remaining <= 0) throw new NoSuchElementException();
            remaining--;
            try {
                return new AbstractMap.SimpleImmutableEntry<Object, Object>(ois.readObject(),
                                                            ois.readObject());
            }
            catch (java.io.IOException e) { throw new IteratorIOException(e); }
            catch (ClassNotFoundException e) { throw new IteratorNoClassException(e); }
        }
        public void remove() { throw new UnsupportedOperationException(); }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size);
        for (Entry<K,V> e = getFirstEntry(); e != null; e = (Entry<K, V>) successor(e)) {
            out.writeObject(e.key);
            out.writeObject(e.element);
        }
    }

    private void readObject(ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        int size = in.readInt();
        try {
        	Iterator itr = new IOIterator(in, size);
        	buildFromSorted(itr, size);
        }
        catch (IteratorIOException e) {
            throw e.getException();
        }
        catch (IteratorNoClassException e) {
            throw e.getException();
        }
    }

    private class SubMap extends AbstractMap<K,V> implements Serializable, NavigableMap<K,V> {

        private static final long serialVersionUID = -6520786458950516097L;

        final K fromKey, toKey;

        SubMap() { fromKey = toKey = null; }

        private Object readResolve() {
            return new AscendingSubMap(fromKey == null, fromKey, true,
                                       toKey == null, toKey, false);
        }

        public Map.Entry<K,V> lowerEntry(Object key)   { throw new Error(); }
        public Object lowerKey(Object key)        { throw new Error(); }
        public Map.Entry<K,V> floorEntry(Object key)   { throw new Error(); }
        public Object floorKey(Object key)        { throw new Error(); }
        public Map.Entry<K,V> ceilingEntry(Object key) { throw new Error(); }
        public Object ceilingKey(Object key)      { throw new Error(); }
        public Map.Entry<K,V> higherEntry(Object key)  { throw new Error(); }
        public Object higherKey(Object key)       { throw new Error(); }
        public Map.Entry<K,V> firstEntry()             { throw new Error(); }
        public Map.Entry<K,V> lastEntry()              { throw new Error(); }
        public Map.Entry<K,V> pollFirstEntry()         { throw new Error(); }
        public Map.Entry<K,V> pollLastEntry()          { throw new Error(); }
        public NavigableMap<K,V> descendingMap()       { throw new Error(); }
        public NavigableSet<K> navigableKeySet()     { throw new Error(); }
        public NavigableSet<K> descendingKeySet()    { throw new Error(); }
        public Set<Map.Entry<K,V>> entrySet()                     { throw new Error(); }

        public NavigableMap<K,V> subMap(Object fromKey, boolean fromInclusive,
                                   Object toKey, boolean toInclusive) {
            throw new Error();
        }

        public NavigableMap<K,V> headMap(Object toKey, boolean inclusive) {
            throw new Error();
        }

        public NavigableMap<K,V> tailMap(Object fromKey, boolean inclusive) {
            throw new Error();
        }

        public SortedMap<K,V> subMap(Object fromKey, Object toKey) {
            throw new Error();
        }

        public SortedMap<K,V> headMap(Object toKey)     	{ throw new Error(); }
        public SortedMap<K,V> tailMap(Object fromKey)   	{ throw new Error(); }
        public Comparator<Object> comparator()             	{ throw new Error(); }
        public K firstKey()                   		{ throw new Error(); }
        public K lastKey()                    		{ throw new Error(); }
    }
}
