/*
 * Copyright (c) 2002-2007 Sun Microsystems, Inc.  All rights reserved.
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

/* Lifted from:
       http://www.web4j.com/web4j/javadoc/hirondelle/web4j/model/ModelUtil.html

   Copyright (c) 2002-2009, Hirondelle Systems

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of Hirondelle Systems nor the
       names of its contributors may be used to endorse or promote products
       derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY HIRONDELLE SYSTEMS ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL HIRONDELLE SYSTEMS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package net.jxta.impl.cm.srdi.inmemory;

import java.lang.reflect.Array;

public final class IndexKeyUtil {

    // HASH CODE //
    /**
    Initial seed value for a <tt>hashCode</tt>.

    Contributions from individual fields are 'added' to this initial value.
    (Using a non-zero value decreases collisons of <tt>hashCode</tt> values.)
    */
    public static final int HASH_SEED = 23;
    private static final int fODD_PRIME_NUMBER = 37;

    // PRIVATE //
    private IndexKeyUtil(  ) {

        //prevent object construction
    }

    /** Hash code for <tt>boolean</tt> primitives. */
    public static int hash( int aSeed, boolean aBoolean ) {

        return firstTerm( aSeed ) + ( aBoolean ? 1 : 0 );
    }

    /** Hash code for <tt>char</tt> primitives. */
    public static int hash( int aSeed, char aChar ) {

        return firstTerm( aSeed ) + aChar;
    }

    /**
     Hash code for <tt>int</tt> primitives.
     <P>Note that <tt>byte</tt> and <tt>short</tt> are also handled by this method, through implicit conversion.
    */
    public static int hash( int aSeed, int aInt ) {

        return firstTerm( aSeed ) + aInt;
    }

    /** Hash code for <tt>long</tt> primitives.  */
    public static int hash( int aSeed, long aLong ) {

        return firstTerm( aSeed ) + (int) ( aLong ^ ( aLong >>> 32 ) );
    }

    /** Hash code for <tt>float</tt> primitives.  */
    public static int hash( int aSeed, float aFloat ) {

        return hash( aSeed, Float.floatToIntBits( aFloat ) );
    }

    /** Hash code for <tt>double</tt> primitives.  */
    public static int hash( int aSeed, double aDouble ) {

        return hash( aSeed, Double.doubleToLongBits( aDouble ) );
    }

    private static int firstTerm( int aSeed ) {

        return fODD_PRIME_NUMBER * aSeed;
    }

    /**
     Return the hash code in a single step, using all significant fields passed in an {@link Object} sequence parameter.

     <P>(This is the recommended way of implementing <tt>hashCode</tt>.)

     <P>Each element of <tt>aFields</tt> must be an {@link Object}, or an array containing
     possibly-null <tt>Object</tt>s. These items will each contribute to the
     result. (It is not a requirement to use <em>all</em> fields related to an object.)

     <P>If the caller is using a <em>primitive</em> field, then it must be converted to a corresponding
     wrapper object to be included in <tt>aFields</tt>. For example, an <tt>int</tt> field would need
     conversion to an {@link Integer} before being passed to this method.
    */
    public static final int hashCodeFor( Object... aFields ) {

        int result = HASH_SEED;

        for ( Object field : aFields ) {

            result = hash( result, field );
        }

        return result;
    }

    /**
     Hash code for an Object.

     <P><tt>aObject</tt> is a possibly-null object field, and possibly an array.

     If <tt>aObject</tt> is an array, then each element may be a primitive
     or a possibly-null object.
    */
    public static int hash( int aSeed, Object aObject ) {

        int result = aSeed;

        if ( aObject == null ) {

            result = hash( result, 0 );
        } else if ( !isArray( aObject ) ) {

            result = hash( result, aObject.hashCode(  ) );
        } else {

            int length = Array.getLength( aObject );

            for ( int idx = 0; idx < length; ++idx ) {

                Object item = Array.get( aObject, idx );

                //recursive call!
                result = hash( result, item );
            }
        }

        return result;
    }

    // EQUALS //

    /**
     Quick checks for <em>possibly</em> determining equality of two objects.

     <P>This method exists to make <tt>equals</tt> implementations read more legibly,
     and to avoid multiple <tt>return</tt> statements.

     <P><em>It cannot be used by itself to fully implement <tt>equals</tt>. </em>
     It uses <tt>==</tt> and <tt>instanceof</tt> to determine if equality can be
     found cheaply, without the need to examine field values in detail. It is
     <em>always</em> paired with some other method
     (usually {@link #equalsFor(Object[], Object[])}), as in the following example :
     <PRE>
     public boolean equals(Object aThat){
       Boolean result = ModelUtil.quickEquals(this, aThat);
       <b>if ( result == null ){</b>
         //quick checks not sufficient to determine equality,
         //so a full field-by-field check is needed :
         This this = (This) aThat; //will not fail
         result = ModelUtil.equalsFor(this.getSignificantFields(), that.getSignificantFields());
       }
       return result;
     }
     </PRE>

     <P>This method is unusual since it returns a <tt>Boolean</tt> that takes
     <em>3</em> values : <tt>true</tt>, <tt>false</tt>, and <tt>null</tt>. Here,
     <tt>true</tt> and <tt>false</tt> mean that a simple quick check was able to
     determine equality. <span class='highlight'>The <tt>null</tt> case means that the
     quick checks were not able to determine if the objects are equal or not, and that
     further field-by-field examination is necessary. The caller must always perform a
     check-for-null on the return value.</span>
    */
    static public Boolean quickEquals( Object aThis, Object aThat ) {

        Boolean result = null;

        if ( aThis == aThat ) {

            result = Boolean.TRUE;
        } else {

            Class<?> thisClass = aThis.getClass(  );

            if ( !thisClass.isInstance( aThat ) ) {

                result = Boolean.FALSE;
            }
        }

        return result;
    }

    /**
     Return the result of comparing all significant fields.

     <P>Both <tt>Object[]</tt> parameters are the same size. Each includes all fields that have been
     deemed by the caller to contribute to the <tt>equals</tt> method. <em>None of those fields are
     array fields.</em> The order is the same in both arrays, in the sense that the Nth item
     in each array corresponds to the same underlying field. The caller controls the order in which fields are
     compared simply through the iteration order of these two arguments.

     <P>If a primitive field is significant, then it must be converted to a corresponding
     wrapper <tt>Object</tt> by the caller.
    */
    static public boolean equalsFor( Object[] aThisSignificantFields, Object[] aThatSignificantFields ) {

        //(varargs can be used for final arg only)
        if ( aThisSignificantFields.length != aThatSignificantFields.length ) {

            throw new IllegalArgumentException( "Array lengths do not match. 'This' length is " + aThisSignificantFields.length +
                ", while 'That' length is " + aThatSignificantFields.length + "." );
        }

        boolean result = true;

        for ( int idx = 0; idx < aThisSignificantFields.length; ++idx ) {

            if ( !areEqual( aThisSignificantFields [ idx ], aThatSignificantFields [ idx ] ) ) {

                result = false;

                break;
            }
        }

        return result;
    }

    /** Equals for <tt>boolean</tt> fields. */
    static public boolean areEqual( boolean aThis, boolean aThat ) {

        return aThis == aThat;
    }

    /** Equals for <tt>char</tt> fields. */
    static public boolean areEqual( char aThis, char aThat ) {

        return aThis == aThat;
    }

    /**
     Equals for <tt>long</tt> fields.

     <P>Note that <tt>byte</tt>, <tt>short</tt>, and <tt>int</tt> are handled by this method, through
     implicit conversion.
    */
    static public boolean areEqual( long aThis, long aThat ) {

        return aThis == aThat;
    }

    /** Equals for <tt>float</tt> fields. */
    static public boolean areEqual( float aThis, float aThat ) {

        return Float.floatToIntBits( aThis ) == Float.floatToIntBits( aThat );
    }

    /** Equals for <tt>double</tt> fields. */
    static public boolean areEqual( double aThis, double aThat ) {

        return Double.doubleToLongBits( aThis ) == Double.doubleToLongBits( aThat );
    }

    /**
     Equals for possibly-<tt>null</tt> object field.

     <P><em>Does not include arrays</em>. (This restriction will likely be removed in a future version.)
    */
    static public boolean areEqual( Object aThis, Object aThat ) {

        if ( isArray( aThis ) || isArray( aThat ) ) {

            throw new IllegalArgumentException( "This method does not currently support arrays." );
        }

        return ( aThis == null ) ? ( aThat == null ) : aThis.equals( aThat );
    }

    private static boolean isArray( Object aObject ) {

        return ( aObject != null ) && aObject.getClass(  ).isArray(  );
    }
}
