/*
 * Written by Dawid Kurzyniec, based on public domain code written by Doug Lea
 * and publictly available documentation, and released to the public domain, as
 * explained at http://creativecommons.org/licenses/publicdomain
 */

package net.jxta.impl.util.backport.java.util;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * Convenience base class for map implementations that provides helper classes
 * representing simple map entries, both mutable and immutable.
 *
 * @author Doug Lea
 * @author Dawid Kurzyniec
 */
public abstract class AbstractMap<V extends Object> extends java.util.AbstractMap<Object,V> {

    transient Set<Object> keySet;

    /**
     * Sole constructor. (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractMap() {}

    /**
     * {@inheritDoc}
     */
    public Set<Object> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet() { // from e.e.m.b. (overrides toArray)
                public int size() { return AbstractMap.this.size(); }
                public boolean contains(Object e) { return AbstractMap.this.containsKey(e); }
                public Iterator<Object> iterator() {
                    return new Iterator<Object>() {
                        final Iterator<Map.Entry<Object, V>> itr = AbstractMap.this.entrySet().iterator();
                        public boolean hasNext() { return itr.hasNext(); }
                        public Object next() { return ((Entry<Object,V>)itr.next()).getKey(); }
                        public void remove() { itr.remove(); }
                    };
                }
            };
        }
        return keySet;
    }

    /**
     * An Entry maintaining a key and a value.  The value may be
     * changed using the <tt>setValue</tt> method.  This class
     * facilitates the process of building custom map
     * implementations. For example, it may be convenient to return
     * arrays of <tt>SimpleEntry</tt> instances in method
     * <tt>Map.entrySet().toArray</tt>
     *
     * @since 1.6
     */
    public static class SimpleEntry<V extends Object> implements Entry<Object,V> {
        private final Object key;
        private V value;

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleEntry(Object key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         *
         * @param entry the entry to copy
         */
        public SimpleEntry(Entry<Object,V> entry) {
            this.key   = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        public Object getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value.
         *
         * @param value new value to be stored in this entry
         * @return the old value corresponding to the entry
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @SuppressWarnings("unchecked")
		public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<Object,V> e = (Map.Entry<Object,V>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        public int hashCode() {
            return ((key   == null) ? 0 :   key.hashCode()) ^
                   ((value == null) ? 0 : value.hashCode());
        }

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         *
         * @return a String representation of this map entry
         */
        public String toString() {
            return key + "=" + value;
        }

    }

    /**
     * An Entry maintaining an immutable key and value, This class
     * does not support method <tt>setValue</tt>.  This class may be
     * convenient in methods that return thread-safe snapshots of
     * key-value mappings.
     *
     * @since 1.6
     */
    public static class SimpleImmutableEntry<V extends Object> implements Entry<Object,V> {
        private final Object key;
        private final V value;

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
        public SimpleImmutableEntry(Object key, V value) {
            this.key   = key;
            this.value = value;
        }

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         *
         * @param entry the entry to copy
         */
        public SimpleImmutableEntry(Entry<Object,V> entry) {
            this.key   = entry.getKey();
            this.value = entry.getValue();
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        public Object getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation).  This implementation simply throws
         * <tt>UnsupportedOperationException</tt>, as this class implements
         * an <i>immutable</i> map entry.
         *
         * @param value new value to be stored in this entry
         * @return (Does not return)
         * @throws UnsupportedOperationException always
         */
        public V setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
		public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<Object,V> e = (Map.Entry<Object,V>)o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        public int hashCode() {
            return ((key   == null) ? 0 :   key.hashCode()) ^
                   ((value == null) ? 0 : value.hashCode());
        }

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         *
         * @return a String representation of this map entry
         */
        public String toString() {
            return key + "=" + value;
        }
    }

    private static boolean eq(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }
}
