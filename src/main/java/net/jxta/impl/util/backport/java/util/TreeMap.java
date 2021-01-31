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
public class TreeMap<V extends Object> extends AbstractMap<V>
                     implements NavigableMap<Object,V>, Serializable {

    private static final long serialVersionUID = 919286545866124006L;

    private final Comparator<Object> comparator;

    private transient Entry<Object,V> root;

    private transient int size = 0;
    private transient int modCount = 0;

    private transient EntrySet entrySet;
    private transient KeySet navigableKeySet;
    private transient NavigableMap<Object,V> descendingMap;
    private transient Comparator<Object> reverseComparator;

    public TreeMap() {
        this.comparator = null;
    }

    public TreeMap(Comparator<Object> comparator) {
        this.comparator = comparator;
    }

    public TreeMap(SortedMap<Object,V> map) {
        this.comparator = map.comparator();
        this.buildFromSorted(map.entrySet().iterator(), map.size());
    }

    public TreeMap(Map<Object,V> map) {
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
        TreeMap<V> clone;
        try { clone = (TreeMap<V>)super.clone(); }
        catch (CloneNotSupportedException e) { throw new InternalError(); }
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        if (!isEmpty()) {
            clone.buildFromSorted(this.entrySet().iterator(), this.size);
        }
        return clone;
    }

    public V put(Object key, V value) {
        if (root == null) {
            root = new Entry<Object,V>(key, value);
            size++;
            modCount++;
            return null;
        }
        else {
            Entry<Object,V> t = root;
            for (;;) {
                int diff = compare(key, t.getKey(), comparator);
                if (diff == 0) return t.setValue(value);
                else if (diff <= 0) {
                    if (t.left != null) t = t.left;
                    else {
                        size++;
                        modCount++;
                        Entry<Object,V> e = new Entry<Object,V>(key, value);
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
                        Entry<Object,V> e = new Entry<Object,V>(key, value);
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
        Entry<Object,V> entry = getEntry(key);
        return (entry == null) ? null : entry.getValue();
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    public Set<Map.Entry<Object, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    public static class Entry<K extends Object, V extends Object>
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
    private static <V extends Object> Entry<Object,V> successor(Entry<Object,V> e) {
        if (e.right != null) {
            for (e = e.right; e.left != null; e = e.left) {}
            return e;
        } else {
            Entry<Object,V> p = e.parent;
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
    private static <V extends Object> Entry<Object,V> predecessor(Entry<Object,V> e) {
        if (e.left != null) {
            for (e = e.left; e.right != null; e = e.right) {}
            return e;
        }
        else {
            Entry<Object,V> p = e.parent;
            while (p != null && e == p.left) {
                e = p;
                p = p.parent;
            }
            return p;
        }
    }

    private Entry<Object,V> getEntry(Object key) {
        Entry<Object,V> t = root;
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

    private Entry<Object,V> getHigherEntry(Object key) {
        Entry<Object,V> t = root;
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
                    Entry<Object,V> parent = t.parent;
                    while (parent != null && t == parent.right) {
                        t = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
    }

    private Entry<Object,V> getFirstEntry() {
        Entry<Object,V> e = root;
        if (e == null) return null;
        while (e.left != null) e = e.left;
        return e;
    }

    private Entry<Object,V> getLastEntry() {
        Entry<Object,V> e = root;
        if (e == null) return null;
        while (e.right != null) e = e.right;
        return e;
    }

    private Entry<Object,V> getCeilingEntry(Object key) {
        Entry<Object,V> e = root;
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
                    Entry<Object,V> p = e.parent;
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

    private Entry<Object,V> getLowerEntry(Object key) {
        Entry<Object,V> e = root;
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
                    Entry<Object,V> p = e.parent;
                    while (p != null && e == p.left) {
                        e = p;
                        p = p.parent;
                    }
                    return p;
                }
            }
        }
    }

    private Entry<Object,V> getFloorEntry(Object key) {
        Entry<Object,V> e = root;
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
                    Entry<Object,V> p = e.parent;
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

    void buildFromSorted(Iterator<Map.Entry<Object,V>> itr, int size) {
        modCount++;
        this.size = size;
        // nodes at the bottom (unbalanced) level must be red
        int bottom = 0;
        for (int ssize = 1; ssize-1 < size; ssize <<= 1) bottom++;
        this.root = createFromSorted(itr, size, 0, bottom);
    }

    private static <V extends Object> Entry<Object,V> createFromSorted(Iterator<Map.Entry<Object,V>> itr, int size,
                                          int level, int bottom) {
        level++;
        if (size == 0) return null;
        int leftSize = (size-1) >> 1;
        int rightSize = size-1-leftSize;
        Entry<Object,V> left = createFromSorted(itr, leftSize, level, bottom);
        Map.Entry<Object,V> orig = (Map.Entry<Object,V>)itr.next();
        Entry<Object,V> right = createFromSorted(itr, rightSize, level, bottom);
        Entry<Object,V> e = new Entry<Object,V>(orig.getKey(), orig.getValue());
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
    private void delete(Entry<Object,V> e) {

        // handle case where we are only node
        if (e.left == null && e.right == null && e.parent == null) {
            root = null;
            size = 0;
            modCount++;
            return;
        }
        // if strictly internal, swap places with a successor
        if (e.left != null && e.right != null) {
            Entry<Object,V> s = successor(e);
            e.key = s.key;
            e.element = s.element;
            e = s;
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
            Entry<Object,V> replacement = e.left;
            if (replacement == null)
                replacement = e.right;

            // link replacement to parent
            replacement.parent = e.parent;

            if (e.parent == null)
                root = replacement;
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
    static <V extends Object> boolean colorOf(Entry<Object,V> p) {
        return (p == null) ? Entry.BLACK : p.color;
    }

    /**
     * return parent of node p, or null if p is null
     */
    static <V extends Object> Entry<Object,V> parentOf(Entry<Object,V> p) {
        return (p == null) ? null : p.parent;
    }

    /**
     * Set the color of node p, or do nothing if p is null
     */
    private static <V extends Object> void setColor(Entry<Object,V> p, boolean c) {
        if (p != null) p.color = c;
    }

    /**
     * return left child of node p, or null if p is null
     */
    private static <V extends Object> Entry<Object,V> leftOf(Entry<Object,V> p) {
        return (p == null) ? null : p.left;
    }

    /**
     * return right child of node p, or null if p is null
     */
    private static <V extends Object> Entry<Object,V> rightOf(Entry<Object,V> p) {
        return (p == null) ? null : p.right;
    }

    /** From CLR */
    private final void rotateLeft(Entry<Object,V> e) {
        Entry<Object,V> r = e.right;
        e.right = r.left;
        if (r.left != null)
            r.left.parent = e;
        r.parent = e.parent;
        if (e.parent == null) root = r;
        else if (e.parent.left == e)
            e.parent.left = r;
        else
            e.parent.right = r;
        r.left = e;
        e.parent = r;
    }

    /** From CLR */
    private final void rotateRight(Entry<Object,V> e) {
        Entry<Object,V> l = e.left;
        e.left = l.right;
        if (l.right != null)
            l.right.parent = e;
        l.parent = e.parent;
        if (e.parent == null) root = l;
        else if (e.parent.right == e)
            e.parent.right = l;
        else
            e.parent.left = l;
        l.right = e;
        e.parent = l;
    }

    /** From CLR */
    private final void fixAfterInsertion(Entry<Object,V> e) {
        e.color = Entry.RED;
        Entry<Object,V> x = e;

        while (x != null && x != root && x.parent.color == Entry.RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<Object,V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == Entry.RED) {
                    setColor(parentOf(x), Entry.BLACK);
                    setColor(y, Entry.BLACK);
                    setColor(parentOf(parentOf(x)), Entry.RED);
                    x = parentOf(parentOf(x));
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
                Entry<Object,V> y = leftOf(parentOf(parentOf(x)));
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
    private final Entry<Object,V> fixAfterDeletion(Entry<Object,V> e) {
        Entry<Object,V> x = e;
        while (x != root && colorOf(x) == Entry.BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<Object,V> sib = rightOf(parentOf(x));
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
                        rotateRight(sib);
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
                Entry<Object,V> sib = leftOf(parentOf(x));
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
                        rotateLeft(sib);
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
        Entry<Object,V> cursor;
        Entry<Object,V> lastRet;
        int expectedModCount;
        BaseEntryIterator(Entry<Object,V> cursor) {
            this.cursor = cursor;
            this.expectedModCount = modCount;
        }
        public boolean hasNext() {
            return (cursor != null);
        }
        Entry<Object,V> nextEntry() {
            Entry<Object,V> curr = cursor;
            if (curr == null) throw new NoSuchElementException();
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            cursor = successor(curr);
            lastRet = curr;
            return curr;
        }
        Entry<Object,V> prevEntry() {
            Entry<Object,V> curr = cursor;
            if (curr == null) throw new NoSuchElementException();
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            cursor = predecessor(curr);
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

    class EntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<Object, V>> {
        EntryIterator(Entry<Object,V> cursor) { super(cursor); }
        public Entry<Object,V> next() { return nextEntry(); }
    }

    class KeyIterator extends BaseEntryIterator implements Iterator<Object> {
        KeyIterator(Entry<Object, V> cursor) { super(cursor); }
        public Object next() { return nextEntry().key; }
    }

    class ValueIterator extends BaseEntryIterator implements Iterator<V> {
        ValueIterator(Entry<Object, V> cursor) { super(cursor); }
        public V next() { return nextEntry().element; }
    }

    class DescendingEntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<Object, V>> {
        DescendingEntryIterator(Entry<Object, V> cursor) { super(cursor); }
        public Entry<Object,V> next() { return prevEntry(); }
    }

    class DescendingKeyIterator extends BaseEntryIterator implements Iterator<Object> {
        DescendingKeyIterator(Entry<Object, V> cursor) { super(cursor); }
        public Object next() { return prevEntry().key; }
    }

    class DescendingValueIterator extends BaseEntryIterator implements Iterator<V> {
        DescendingValueIterator(Entry<Object, V> cursor) { super(cursor); }
        public V next() { return prevEntry().element; }
    }

    private Entry<Object, V> getMatchingEntry(Object o) {
        if (!(o instanceof Map.Entry)) return null;
        Map.Entry<Object, V> e = (Map.Entry<Object, V>)o;
        Entry<Object, V> found = TreeMap.this.getEntry(e.getKey());
        return (found != null && eq(found.getValue(), e.getValue())) ? found : null;
    }

    class EntrySet extends AbstractSet<Map.Entry<Object, V>> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public Iterator<Map.Entry<Object, V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        public boolean contains(Object o) {
            return getMatchingEntry(o) != null;
        }

        public boolean remove(Object o) {
            Entry<Object,V> e = getMatchingEntry(o);
            if (e == null) return false;
            delete(e);
            return true;
        }
    }

    class DescendingEntrySet extends EntrySet {
        public Iterator<Map.Entry<Object, V>> iterator() {
            return new DescendingEntryIterator(getLastEntry());
        }
    }

    class ValueSet extends AbstractSet<V> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public boolean contains(Object o) {
            for (Entry<Object, V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (eq(o, e.element)) return true;
            }
            return false;
        }

        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        public boolean remove(Object o) {
            for (Entry<Object, V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (eq(o, e.element)) {
                    delete(e);
                    return true;
                }
            }
            return false;
        }
    }

    abstract class KeySet extends AbstractSet<Object> implements NavigableSet<Object> {
        public int size() { return TreeMap.this.size(); }
        public boolean isEmpty() { return TreeMap.this.isEmpty(); }
        public void clear() { TreeMap.this.clear(); }

        public boolean contains(Object o) {
            return getEntry(o) != null;
        }

        public boolean remove(Object o) {
            Entry<Object, V> found = getEntry(o);
            if (found == null) return false;
            delete(found);
            return true;
        }
        public SortedSet<Object> subSet(Object fromElement, Object toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public SortedSet<Object> headSet(Object toElement) {
            return headSet(toElement, false);
        }
        public SortedSet<Object> tailSet(Object fromElement) {
            return tailSet(fromElement, true);
        }
    }

    class AscendingKeySet extends KeySet {

        public Iterator<Object> iterator() {
            return new KeyIterator(getFirstEntry());
        }

        public Iterator<Object> descendingIterator() {
            return new DescendingKeyIterator(getFirstEntry());
        }

        public Object lower(Object e)   { return lowerKey(e); }
        public Object floor(Object e)   { return floorKey(e); }
        public Object ceiling(Object e) { return ceilingKey(e); }
        public Object higher(Object e)  { return higherKey(e); }
        public Object first()           { return firstKey(); }
        public Object last()            { return lastKey(); }
        public Comparator<Object> comparator()  { return TreeMap.this.comparator(); }

        public Object pollFirst() {
            Map.Entry<Object,V> e = pollFirstEntry();
            return e == null? null : e.getKey();
        }
        public Object pollLast() {
            Map.Entry<Object,V> e = pollLastEntry();
            return e == null? null : e.getKey();
        }

        public NavigableSet<Object> subSet(Object fromElement, boolean fromInclusive,
                                   Object toElement,   boolean toInclusive) {
            return (NavigableSet<Object>)(subMap(fromElement, fromInclusive,
                                         toElement,   toInclusive)).keySet();
        }
        public NavigableSet<Object> headSet(Object toElement, boolean inclusive) {
            return (NavigableSet<Object>)(headMap(toElement, inclusive)).keySet();
        }
        public NavigableSet<Object> tailSet(Object fromElement, boolean inclusive) {
            return (NavigableSet<Object>)(tailMap(fromElement, inclusive)).keySet();
        }
        public NavigableSet<Object> descendingSet() {
            return (NavigableSet<Object>)descendingMap().keySet();
        }
    }

    class DescendingKeySet extends KeySet {

        public Iterator<Object> iterator() {
            return new DescendingKeyIterator(getLastEntry());
        }

        public Iterator<Object> descendingIterator() {
            return new KeyIterator(getFirstEntry());
        }

        public Object lower(Object e)   { return higherKey(e); }
        public Object floor(Object e)   { return ceilingKey(e); }
        public Object ceiling(Object e) { return floorKey(e); }
        public Object higher(Object e)  { return lowerKey(e); }
        public Object first()           { return lastKey(); }
        public Object last()            { return firstKey(); }
        public Comparator<Object> comparator()  { return descendingMap().comparator(); }

        public Object pollFirst() {
            Map.Entry<Object, V> e = pollLastEntry();
            return e == null? null : e.getKey();
        }
        public Object pollLast() {
            Map.Entry<Object, V> e = pollFirstEntry();
            return e == null? null : e.getKey();
        }

        public NavigableSet<Object> subSet(Object fromElement, boolean fromInclusive,
                                   Object toElement,   boolean toInclusive) {
            return (NavigableSet<Object>)(descendingMap().subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive)).keySet();
        }
        public NavigableSet<Object> headSet(Object toElement, boolean inclusive) {
            return (NavigableSet<Object>)(descendingMap().headMap(toElement, inclusive)).keySet();
        }
        public NavigableSet<Object> tailSet(Object fromElement, boolean inclusive) {
            return (NavigableSet<Object>)(descendingMap().tailMap(fromElement, inclusive)).keySet();
        }
        public NavigableSet<Object> descendingSet() {
            return (NavigableSet<Object>)keySet();
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
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> lowerEntry(Object key) {
        Map.Entry<Object,V> e = getLowerEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    public Object lowerKey(Object key) {
        Map.Entry<Object,V> e = getLowerEntry(key);
        return (e == null) ? null : e.getKey();
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> floorEntry(Object key) {
        Entry<Object,V> e = getFloorEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    public Object floorKey(Object key) {
        Entry<Object,V> e = getFloorEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> ceilingEntry(Object key) {
        Entry<Object,V> e = getCeilingEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    public Object ceilingKey(Object key) {
        Entry<Object,V> e = getCeilingEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> higherEntry(Object key) {
        Entry<Object,V> e = getHigherEntry(key);
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    public Object higherKey(Object key) {
        Entry<Object,V> e = getHigherEntry(key);
        return (e == null) ? null : e.key;
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> firstEntry() {
        Entry<Object,V> e = getFirstEntry();
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> lastEntry() {
        Entry<Object,V> e = getLastEntry();
        return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> pollFirstEntry() {
        Entry<Object,V> e = getFirstEntry();
        if (e == null) return null;
        Map.Entry<Object,V> res = new AbstractMap.SimpleImmutableEntry(e);
        delete(e);
        return res;
    }

    /**
     * @since 1.6
     */
    @SuppressWarnings("rawtypes")
	public Map.Entry<Object,V> pollLastEntry() {
        Entry<Object,V> e = getLastEntry();
        if (e == null) return null;
        Map.Entry<Object,V> res = new AbstractMap.SimpleImmutableEntry(e);
        delete(e);
        return res;
    }

    /**
     * @since 1.6
     */
    public NavigableMap<Object,V> descendingMap() {
        NavigableMap<Object,V> map = descendingMap;
        if (map == null) {
            descendingMap = map = new DescendingSubMap(true, null, true,
                                                       true, null, true);
        }
        return map;
    }

    public NavigableSet<Object> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    public SortedMap<Object,V> subMap(Object fromKey, Object toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    public SortedMap<Object,V> headMap(Object toKey) {
        return headMap(toKey, false);
    }

    public SortedMap<Object,V> tailMap(Object fromKey) {
        return tailMap(fromKey, true);
    }

    public NavigableMap<Object,V> subMap(Object fromKey, boolean fromInclusive,
                               Object toKey,   boolean toInclusive) {
        return new AscendingSubMap(false, fromKey, fromInclusive,
                                   false, toKey, toInclusive);
    }

    public NavigableMap<Object,V> headMap(Object toKey, boolean toInclusive) {
        return new AscendingSubMap(true,  null,  true,
                                   false, toKey, toInclusive);
    }

    public NavigableMap<Object,V> tailMap(Object fromKey, boolean fromInclusive) {
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

    public Object firstKey() {
        Entry<Object,V> e = getFirstEntry();
        if (e == null) throw new NoSuchElementException();
        return e.key;
    }

    public Object lastKey() {
        Entry<Object,V> e = getLastEntry();
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

    private static <V extends Object> boolean containsNull(Entry<Object,V> e) {
        if (e.element == null) return true;
        if (e.left != null && containsNull(e.left)) return true;
        if (e.right != null && containsNull(e.right)) return true;
        return false;
    }

    private static <V extends Object> boolean containsValue(Entry<Object,V> e, Object val) {
        if (val.equals(e.element)) return true;
        if (e.left != null && containsValue(e.left, val)) return true;
        if (e.right != null && containsValue(e.right, val)) return true;
        return false;
    }

    public V remove(Object key) {
        Entry<Object,V> e = getEntry(key);
        if (e == null) return null;
        V old = e.getValue();
        delete(e);
        return old;
    }

    public void putAll(Map<? extends Object,? extends V > map) {
        if (map instanceof SortedMap) {
            SortedMap<Object,V> smap = (SortedMap<Object,V>)map;
            if (eq(this.comparator, smap.comparator())) {
                this.buildFromSorted(smap.entrySet().iterator(), map.size());
                return;
            }
        }
        // not a sorted map, or comparator mismatch
        super.putAll(map);
    }

    public Set<Object> keySet() {
        return navigableKeySet();
    }

    public NavigableSet<Object> navigableKeySet() {
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
    private abstract class NavigableSubMap extends AbstractMap<V>
                                           implements NavigableMap<Object,V>, Serializable {

        private static final long serialVersionUID = -6520786458950516097L;

        final Object fromKey, toKey;
        final boolean fromStart, toEnd;
        final boolean fromInclusive, toInclusive;
        transient int cachedSize = -1, cacheVersion;
        transient SubEntrySet entrySet;
        transient NavigableMap<Object,V> descendingMap;
        transient NavigableSet<Object> navigableKeySet;

        NavigableSubMap(boolean fromStart, Object fromKey, boolean fromInclusive,
                        boolean toEnd,     Object toKey,   boolean toInclusive) {
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

        final TreeMap.Entry<Object,V> checkLoRange(TreeMap.Entry<Object,V> e) {
            return (e == null || absTooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<Object,V> checkHiRange(TreeMap.Entry<Object,V> e) {
            return (e == null || absTooHigh(e.key)) ? null : e;
        }

        final boolean inRange(Object key) {
            return !absTooLow(key) && !absTooHigh(key);
        }

        final boolean inRangeExclusive(Object key) {
            return (fromStart || compare(key, fromKey, comparator) >= 0)
                && (toEnd     || compare(toKey, key, comparator) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inRangeExclusive(key);
        }

        private boolean absTooHigh(Object key) {
            if (toEnd) return false;
            int c = compare(key, toKey, comparator);
            return (c > 0 || (c == 0 && !toInclusive));
        }

        private boolean absTooLow(Object key) {
            if (fromStart) return false;
            int c = compare(key, fromKey, comparator);
            return (c < 0 || (c == 0 && !fromInclusive));
        }

        protected abstract TreeMap.Entry<Object,V> first();
        protected abstract TreeMap.Entry<Object,V> last();
        protected abstract TreeMap.Entry<Object,V> lower(Object key);
        protected abstract TreeMap.Entry<Object,V> floor(Object key);
        protected abstract TreeMap.Entry<Object,V> ceiling(Object key);
        protected abstract TreeMap.Entry<Object,V> higher(Object key);
        protected abstract TreeMap.Entry<Object,V> uncheckedHigher(TreeMap.Entry<Object,V> e);

        // absolute comparisons, for use by subclasses

        final TreeMap.Entry<Object,V> absLowest() {
            return checkHiRange((fromStart) ? getFirstEntry() :
                fromInclusive ? getCeilingEntry(fromKey) : getHigherEntry(fromKey));
        }

        final TreeMap.Entry<Object,V> absHighest() {
            return checkLoRange((toEnd) ? getLastEntry() :
                toInclusive ? getFloorEntry(toKey) : getLowerEntry(toKey));
        }

        final TreeMap.Entry<Object,V> absLower(Object key) {
            return absTooHigh(key) ? absHighest() : checkLoRange(getLowerEntry(key));
        }

        final TreeMap.Entry<Object,V> absFloor(Object key) {
            return absTooHigh(key) ? absHighest() : checkLoRange(getFloorEntry(key));
        }

        final TreeMap.Entry<Object,V> absCeiling(Object key) {
            return absTooLow(key) ? absLowest() : checkHiRange(getCeilingEntry(key));
        }

        final TreeMap.Entry<Object,V> absHigher(Object key) {
            return absTooLow(key) ? absLowest() : checkHiRange(getHigherEntry(key));
        }

        // navigable implementations, using subclass-defined comparisons

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> firstEntry() {
            TreeMap.Entry<Object,V> e = first();
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object firstKey() {
            TreeMap.Entry<Object,V> e = first();
            if (e == null) throw new NoSuchElementException();
            return e.key;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> lastEntry() {
            TreeMap.Entry<Object,V> e = last();
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object lastKey() {
            TreeMap.Entry<Object,V> e = last();
            if (e == null) throw new NoSuchElementException();
            return e.key;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> pollFirstEntry() {
            TreeMap.Entry<Object,V> e = first();
            if (e == null) return null;
            Map.Entry<Object,V> result = new SimpleImmutableEntry(e);
            delete(e);
            return result;
        }

        @SuppressWarnings("rawtypes")
		public java.util.Map.Entry<Object,V> pollLastEntry() {
            TreeMap.Entry<Object,V> e = last();
            if (e == null) return null;
            Map.Entry result = new SimpleImmutableEntry(e);
            delete(e);
            return result;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> lowerEntry(Object key) {
            TreeMap.Entry<Object,V> e = lower(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object lowerKey(Object key) {
            TreeMap.Entry<Object,V> e = lower(key);
            return (e == null) ? null : e.key;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> floorEntry(Object key) {
            TreeMap.Entry<Object,V> e = floor(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object floorKey(Object key) {
            TreeMap.Entry<Object,V> e = floor(key);
            return (e == null) ? null : e.key;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> ceilingEntry(Object key) {
            TreeMap.Entry<Object,V> e = ceiling(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object ceilingKey(Object key) {
            TreeMap.Entry<Object,V> e = ceiling(key);
            return (e == null) ? null : e.key;
        }

        @SuppressWarnings("rawtypes")
		public Map.Entry<Object,V> higherEntry(Object key) {
            TreeMap.Entry<Object,V> e = higher(key);
            return (e == null) ? null : new AbstractMap.SimpleImmutableEntry(e);
        }

        public Object higherKey(Object key) {
            TreeMap.Entry<Object,V> e = higher(key);
            return (e == null) ? null : e.key;
        }

        public NavigableSet<Object> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public SortedMap<Object,V> subMap(Object fromKey, Object toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<Object,V> headMap(Object toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<Object,V> tailMap(Object fromKey) {
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
            TreeMap.Entry<Object,V> terminator = absHighest();
            Object terminalKey = terminator != null ? terminator.key : null;

            int size = 0;
            for (TreeMap.Entry<Object,V> e = absLowest(); e != null;
                 e = (e.key == terminalKey) ? null : successor(e)) {
                size++;
            }
            return size;
        }

        public boolean isEmpty() {
            return absLowest() == null;
        }

        public boolean containsKey(Object key) {
            return (inRange(key) && TreeMap.this.containsKey(key));
        }

        public V get(Object key) {
            if (!inRange(key)) return null;
            else return TreeMap.this.get(key);
        }

        public V put(Object key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("Key out of range");
            return TreeMap.this.put(key, value);
        }

        public V remove(Object key) {
            if (!inRange(key)) return null;
            return TreeMap.this.remove(key);
        }

        public Set<Map.Entry<Object, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new SubEntrySet();
            }
            return entrySet;
        }

        public Set<Object> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<Object> navigableKeySet() {
            if (navigableKeySet == null) {
                navigableKeySet = new SubKeySet();
            }
            return navigableKeySet;
        }

        private TreeMap.Entry<Object,V> getMatchingSubEntry(Object o) {
            if (!(o instanceof Map.Entry)) return null;
            Map.Entry<Object,V> e = (Map.Entry<Object,V>)o;
            Object key = e.getKey();
            if (!inRange(key)) return null;
            TreeMap.Entry<Object,V> found = getEntry(key);
            return (found != null && eq(found.getValue(), e.getValue())) ? found : null;
        }

        class SubEntrySet extends AbstractSet<Map.Entry<Object,V>> {
            public int size() { return NavigableSubMap.this.size(); }
            public boolean isEmpty() { return NavigableSubMap.this.isEmpty(); }

            public boolean contains(Object o) {
                return getMatchingSubEntry(o) != null;
            }

            public boolean remove(Object o) {
                TreeMap.Entry<Object,V> e = getMatchingSubEntry(o);
                if (e == null) return false;
                delete(e);
                return true;
            }

            public Iterator<Map.Entry<Object,V>> iterator() {
                return new SubEntryIterator();
            }
        }

        class SubKeySet extends AbstractSet<Object> implements NavigableSet<Object> {
            public int size() { return NavigableSubMap.this.size(); }
            public boolean isEmpty() { return NavigableSubMap.this.isEmpty(); }
            public void clear() { NavigableSubMap.this.clear(); }

            public boolean contains(Object o) {
                return getEntry(o) != null;
            }

            public boolean remove(Object o) {
                if (!inRange(o)) return false;
                TreeMap.Entry<Object,V> found = getEntry(o);
                if (found == null) return false;
                delete(found);
                return true;
            }
            public SortedSet<Object> subSet(Object fromElement, Object toElement) {
                return subSet(fromElement, true, toElement, false);
            }
            public SortedSet<Object> headSet(Object toElement) {
                return headSet(toElement, false);
            }
            public SortedSet<Object> tailSet(Object fromElement) {
                return tailSet(fromElement, true);
            }

            public Iterator<Object> iterator() {
                return new SubKeyIterator(NavigableSubMap.this.entrySet().iterator());
            }

            public Iterator<Object> descendingIterator() {
                return new SubKeyIterator(NavigableSubMap.this.descendingMap().entrySet().iterator());
            }

            public Object lower(Object e)   { return NavigableSubMap.this.lowerKey(e); }
            public Object floor(Object e)   { return NavigableSubMap.this.floorKey(e); }
            public Object ceiling(Object e) { return NavigableSubMap.this.ceilingKey(e); }
            public Object higher(Object e)  { return NavigableSubMap.this.higherKey(e); }
            public Object first()           { return NavigableSubMap.this.firstKey(); }
            public Object last()            { return NavigableSubMap.this.lastKey(); }
            public Comparator<Object> comparator()  { return NavigableSubMap.this.comparator(); }

            public Object pollFirst() {
                Map.Entry<Object,V> e = NavigableSubMap.this.pollFirstEntry();
                return e == null? null : e.getKey();
            }
            public Object pollLast() {
                Map.Entry<Object,V> e = NavigableSubMap.this.pollLastEntry();
                return e == null? null : e.getKey();
            }

            public NavigableSet<Object> subSet(Object fromElement, boolean fromInclusive,
                                       Object toElement,   boolean toInclusive) {
                return (NavigableSet<Object>)(NavigableSubMap.this.subMap(fromElement, fromInclusive,
                                             toElement,   toInclusive)).keySet();
            }
            public NavigableSet<Object> headSet(Object toElement, boolean inclusive) {
                return (NavigableSet<Object>)(NavigableSubMap.this.headMap(toElement, inclusive)).keySet();
            }
            public NavigableSet<Object> tailSet(Object fromElement, boolean inclusive) {
                return (NavigableSet<Object>)(NavigableSubMap.this.tailMap(fromElement, inclusive)).keySet();
            }
            public NavigableSet<Object> descendingSet() {
                return (NavigableSet<Object>)NavigableSubMap.this.descendingMap().keySet();
            }
        }

        class SubEntryIterator extends BaseEntryIterator implements Iterator<Map.Entry<Object, V>> {
            final Object terminalKey;
            SubEntryIterator() {
                super(first());
                TreeMap.Entry<Object,V> terminator = last();
                this.terminalKey = terminator == null ? null : terminator.key;
            }
            public boolean hasNext() {
                return cursor != null;
            }
            public Map.Entry<Object, V> next() {
                TreeMap.Entry<Object, V> curr = cursor;
                if (curr == null) throw new NoSuchElementException();
                if (expectedModCount != modCount)
                    throw new ConcurrentModificationException();
                cursor = (curr.key == terminalKey) ? null : uncheckedHigher(curr);
                lastRet = curr;
                return curr;
            }
        }

        class SubKeyIterator implements Iterator<Object> {
            final Iterator<Map.Entry<Object, V>> itr;
            SubKeyIterator(Iterator<Map.Entry<Object, V>> itr) { this.itr = itr; }
            public boolean hasNext()     { return itr.hasNext(); }
            public Object next()         { return ((Map.Entry<Object,V>)itr.next()).getKey(); }
            public void remove()         { itr.remove(); }
        }
    }

    class AscendingSubMap extends NavigableSubMap {
		private static final long serialVersionUID = 1L;

		AscendingSubMap(boolean fromStart, Object fromKey, boolean fromInclusive,
                        boolean toEnd,     Object toKey,   boolean toInclusive) {
            super(fromStart, fromKey, fromInclusive, toEnd, toKey, toInclusive);
        }

        public Comparator<Object> comparator() {
            return comparator;
        }

        protected TreeMap.Entry<Object, V> first()             { return absLowest(); }
        protected TreeMap.Entry<Object, V> last()              { return absHighest(); }
        protected TreeMap.Entry<Object, V> lower(Object key)   { return absLower(key); }
        protected TreeMap.Entry<Object, V> floor(Object key)   { return absFloor(key); }
        protected TreeMap.Entry<Object, V> ceiling(Object key) { return absCeiling(key); }
        protected TreeMap.Entry<Object, V> higher(Object key)  { return absHigher(key); }

        protected TreeMap.Entry<Object, V> uncheckedHigher(TreeMap.Entry<Object, V> e) {
            return successor(e);
        }

        public NavigableMap<Object, V> subMap(Object fromKey, boolean fromInclusive,
                                   Object toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(false, fromKey, fromInclusive,
                                       false, toKey, toInclusive);
        }

        public NavigableMap<Object, V> headMap(Object toKey, boolean toInclusive) {
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap(fromStart, fromKey, fromInclusive,
                                       false, toKey, toInclusive);
        }

        public NavigableMap<Object, V> tailMap(Object fromKey, boolean fromInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new AscendingSubMap(false, fromKey, fromInclusive,
                                       toEnd, toKey, toInclusive);
        }

        public NavigableMap<Object, V> descendingMap() {
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

		DescendingSubMap(boolean fromStart, Object fromKey, boolean fromInclusive,
                         boolean toEnd,     Object toKey,   boolean toInclusive) {
            super(fromStart, fromKey, fromInclusive, toEnd, toKey, toInclusive);
        }

        public Comparator<Object> comparator() { return TreeMap.this.reverseComparator(); }

        protected TreeMap.Entry<Object, V> first()             { return absHighest(); }
        protected TreeMap.Entry<Object, V> last()              { return absLowest(); }
        protected TreeMap.Entry<Object, V> lower(Object key)   { return absHigher(key); }
        protected TreeMap.Entry<Object, V> floor(Object key)   { return absCeiling(key); }
        protected TreeMap.Entry<Object, V> ceiling(Object key) { return absFloor(key); }
        protected TreeMap.Entry<Object, V> higher(Object key)  { return absLower(key); }

        protected TreeMap.Entry<Object, V> uncheckedHigher(TreeMap.Entry<Object, V> e) {
            return predecessor(e);
        }

        public NavigableMap<Object, V> subMap(Object fromKey, boolean fromInclusive,
                                   Object toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(false, toKey, toInclusive,
                                        false, fromKey, fromInclusive);
        }

        public NavigableMap<Object, V> headMap(Object toKey, boolean toInclusive) {
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap(false, toKey, toInclusive,
                                        this.toEnd, this.toKey, this.toInclusive);
        }

        public NavigableMap<Object, V> tailMap(Object fromKey, boolean fromInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new DescendingSubMap(this.fromStart, this.fromKey, this.fromInclusive,
                                        false, fromKey, fromInclusive);
        }

        public NavigableMap<Object, V> descendingMap() {
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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		IteratorNoClassException(ClassNotFoundException e) {
            super(e);
        }
        ClassNotFoundException getException() {
            return (ClassNotFoundException)getCause();
        }
    }

    static class IOIterator <V extends Object> implements Iterator<Map.Entry<Object, V>> {
        final java.io.ObjectInputStream ois;
        int remaining;
        IOIterator(java.io.ObjectInputStream ois, int remaining) {
            this.ois = ois;
            this.remaining = remaining;
        }
        public boolean hasNext() {
            return remaining > 0;
        }
        @SuppressWarnings("rawtypes")
		public Map.Entry<Object, V> next() {
            if (remaining <= 0) throw new NoSuchElementException();
            remaining--;
            try {
                return new AbstractMap.SimpleImmutableEntry(ois.readObject(),
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
        for (Entry<Object, V> e = getFirstEntry(); e != null; e = successor(e)) {
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
            buildFromSorted(new IOIterator<V>(in, size), size);
        }
        catch (IteratorIOException e) {
            throw e.getException();
        }
        catch (IteratorNoClassException e) {
            throw e.getException();
        }
    }

    protected class SubMap extends AbstractMap<V> implements Serializable, NavigableMap<Object, V> {

        private static final long serialVersionUID = -6520786458950516097L;

        final Object fromKey, toKey;

        SubMap() { fromKey = toKey = null; }

        private Object readResolve() {
            return new AscendingSubMap(fromKey == null, fromKey, true,
                                       toKey == null, toKey, false);
        }

        public Map.Entry<Object, V> lowerEntry(Object key)   { throw new Error(); }
        public Object lowerKey(Object key)        { throw new Error(); }
        public Map.Entry<Object, V> floorEntry(Object key)   { throw new Error(); }
        public Object floorKey(Object key)        { throw new Error(); }
        public Map.Entry<Object, V> ceilingEntry(Object key) { throw new Error(); }
        public Object ceilingKey(Object key)      { throw new Error(); }
        public Map.Entry<Object, V> higherEntry(Object key)  { throw new Error(); }
        public Object higherKey(Object key)       { throw new Error(); }
        public Map.Entry<Object, V> firstEntry()             { throw new Error(); }
        public Map.Entry<Object, V> lastEntry()              { throw new Error(); }
        public Map.Entry<Object, V> pollFirstEntry()         { throw new Error(); }
        public Map.Entry<Object, V> pollLastEntry()          { throw new Error(); }
        public NavigableMap<Object, V> descendingMap()       { throw new Error(); }
        public NavigableSet<Object> navigableKeySet()     { throw new Error(); }
        public NavigableSet<Object> descendingKeySet()    { throw new Error(); }
        public Set<Map.Entry<Object, V>> entrySet()                     { throw new Error(); }

        public NavigableMap<Object, V> subMap(Object fromKey, boolean fromInclusive,
                                   Object toKey, boolean toInclusive) {
            throw new Error();
        }

        public NavigableMap<Object, V> headMap(Object toKey, boolean inclusive) {
            throw new Error();
        }

        public NavigableMap<Object, V> tailMap(Object fromKey, boolean inclusive) {
            throw new Error();
        }

        public SortedMap<Object, V> subMap(Object fromKey, Object toKey) {
            throw new Error();
        }

        public SortedMap<Object, V> headMap(Object toKey)     { throw new Error(); }
        public SortedMap<Object, V> tailMap(Object fromKey)   { throw new Error(); }
        public Comparator<Object> comparator()             { throw new Error(); }
        public Object firstKey()                   { throw new Error(); }
        public Object lastKey()                    { throw new Error(); }
    }
}
