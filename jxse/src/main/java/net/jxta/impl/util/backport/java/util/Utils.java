/*
 * Written by Dawid Kurzyniec, based on code written by Doug Lea with assistance
 * from members of JCP JSR-166 Expert Group. Released to the public domain,
 * as explained at http://creativecommons.org/licenses/publicdomain.
 *
 * Thanks to Craig Mattocks for suggesting to use <code>sun.misc.Perf</code>.
 */

package net.jxta.impl.util.backport.java.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p>
 * This class groups together the functionality of java.util.concurrent that
 * cannot be fully and reliably implemented in backport, but for which some
 * form of emulation is possible.
 * <p>
 * Currently, this class contains methods related to nanosecond-precision
 * timing, particularly via the {@link #nanoTime} method. To measure time
 * accurately, this method by default uses <code>java.sun.Perf</code> on
 * JDK1.4.2 and it falls back to <code>System.currentTimeMillis</code>
 * on earlier JDKs.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public final class Utils {

    private Utils() {}

    public static Object[] collectionToArray(Collection c) {
        // guess the array size; expect to possibly be different
        int len = c.size();
        Object[] arr = new Object[len];
        Iterator itr = c.iterator();
        int idx = 0;
        while (true) {
            while (idx < len && itr.hasNext()) {
                arr[idx++] = itr.next();
            }
            if (!itr.hasNext()) {
                if (idx == len) return arr;
                // otherwise have to trim
                return copyOf(arr, idx, Object[].class);
            }
            // otherwise, have to grow
            int newcap = ((arr.length/2)+1)*3;
            if (newcap < arr.length) {
                // overflow
                if (arr.length < Integer.MAX_VALUE) {
                    newcap = Integer.MAX_VALUE;
                }
                else {
                    throw new OutOfMemoryError("required array size too large");
                }
            }
            arr = copyOf(arr, newcap, Object[].class);
            len = newcap;
        }
    }

    public static Object[] collectionToArray(Collection c, Object[] a) {
        Class aType = a.getClass();
        // guess the array size; expect to possibly be different
        int len = c.size();
        Object[] arr = (a.length >= len ? a :
                        (Object[])Array.newInstance(aType.getComponentType(), len));
        Iterator itr = c.iterator();
        int idx = 0;
        while (true) {
            while (idx < len && itr.hasNext()) {
                arr[idx++] = itr.next();
            }
            if (!itr.hasNext()) {
                if (idx == len) return arr;
                if (arr == a) {
                    // orig array -> null terminate
                    a[idx] = null;
                    return a;
                }
                else {
                    // have to trim
                    return copyOf(arr, idx, aType);
                }
            }
            // otherwise, have to grow
            int newcap = ((arr.length/2)+1)*3;
            if (newcap < arr.length) {
                // overflow
                if (arr.length < Integer.MAX_VALUE) {
                    newcap = Integer.MAX_VALUE;
                }
                else {
                    throw new OutOfMemoryError("required array size too large");
                }
            }
            arr = copyOf(arr, newcap, aType);
            len = newcap;
        }
    }
    
    /**
     * Taken from Arrays in backport-jsr166 3.1.
     */
    public static Object[] copyOf(Object[] original, int newLength, Class newType) {
        Object[] arr = (newType == Object[].class) ? new Object[newLength] :
            (Object[])Array.newInstance(newType.getComponentType(), newLength);
        int len  = (original.length < newLength ? original.length : newLength);
        System.arraycopy(original, 0, arr, 0, len);
        return arr;
    }
}
