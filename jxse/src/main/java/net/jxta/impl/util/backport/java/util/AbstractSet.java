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
@SuppressWarnings("unchecked")
public abstract class AbstractSet extends java.util.AbstractSet {

    /**
     * Sole constructor. (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractSet() { super(); }

    public Object[] toArray() {
        return Utils.collectionToArray(this);
    }

    public Object[] toArray(Object[] a) {
        return Utils.collectionToArray(this, a);
    }
}
