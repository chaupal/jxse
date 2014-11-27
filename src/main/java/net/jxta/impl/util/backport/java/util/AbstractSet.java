/*
 * Written by Dawid Kurzyniec, based on public domain code written by Doug Lea
 * and publictly available documentation, and released to the public domain, as
 * explained at http://creativecommons.org/licenses/publicdomain
 */

package net.jxta.impl.util.backport.java.util;

/**
 * Overrides toArray() and toArray(Object[]) in AbstractCollection to provide
 * implementations valid for concurrent sets.
 *
 * @author Doug Lea
 * @author Dawid Kurzyniec
 */
public abstract class AbstractSet<T extends Object> extends java.util.AbstractSet<T> {

    /**
     * Sole constructor. (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractSet() { super(); }

    public Object[] toArray() {
        return Utils.collectionToArray(this);
    }

    
    
    @SuppressWarnings("unchecked")
	public T[] toArray(Object[] a) {
        return (T[]) Utils.collectionToArray(this, a);
    }
}
