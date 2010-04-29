/*
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * <p>The purpose of this class is to provide a mechanism to load {@code Class}
 * object instances from their fully qualified name, for later instantiation using
 * reflexion. 
 * 
 * <p>It can be used to instantiate classes within API code without referring to
 * implementation code.</p>
 */
public final class JxseInstantiator {

    /**
     * Logger for this class
     */
    private final static Logger LOG = Logger.getLogger(JxseInstantiator.class.getName());

    /**
     * This method provides a {@code Class} object instance from the fully qualified name
     * of a class. 
     *
     * @param fullyQualifiedClassName The fully qualified name of the {@code Class}
     * @return a {@code Class} instance or {@code null} if not available
     */
    public static Class<?> forName(String fullyQualifiedClassName) {

        Class<?> Result;

        try {

            Result = Class.forName(fullyQualifiedClassName);

            if (null == Result) {
                throw new ClassNotFoundException("forName() result is null");
            }

        } catch (ClassNotFoundException notThere) {

            final String Tmp = "Could not find class named : " + fullyQualifiedClassName
                    + "\n" + notThere.toString();

            LOG.severe(Tmp);
            return null;

        } catch (NoClassDefFoundError notThere) {

            final String Tmp = "Could not find class named : " + fullyQualifiedClassName
                    + "\n" + notThere.toString();

            LOG.severe(Tmp);
            return null;

        }

        return Result;

    }

    /**
     * This method instantiates an object from a {@code Class}. The
     * constructor is fetched using provided array of parameter types and the
     * class is instantiated with the provided array of parameter values.
     *
     * <p>The method logs a SEVERE record and returns {@code null} if an exception
     * is encountered.
     *
     * @param inClass The {@code Class} to instantiate
     * @param inParamsTypes An array of parameter types
     * @param inParamsValues An array of parameter values
     * @return an {@code Object} class or {@code null}
     */
    public static Object instantiate(Class inClass, Class[] inParamTypes, Object[] inParamValues) {

        // Checking parameter
        if (inParamTypes == null) {
            inParamTypes = new Class[0];
        }

        if (inParamValues == null) {
            inParamValues = new Object[0];
        }

        // Preparing result
        Object Result = null;
        Constructor<?> Constr = null;

        try {

            // Retrieving the constructor
            Constr = inClass.getConstructor(inParamTypes);

        } catch (NoSuchMethodException ex) {

            LOG.severe("Cannot find constructor for: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        } catch (SecurityException ex) {

            LOG.severe("Security exception encountered while retrieving constructor for: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        }

        try {

            // Creating the instance
            Result = Constr.newInstance(inParamValues);

        } catch (InstantiationException ex) {

            LOG.severe("Instantion exception encountered while instantiating: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        } catch (IllegalAccessException ex) {

            LOG.severe("Illegal access exception encountered while instantiating: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        } catch (IllegalArgumentException ex) {

            LOG.severe("Illegal argument exception encountered while instantiating: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        } catch (InvocationTargetException ex) {

            LOG.severe("Invalid target exception encountered while instantiating: " + inClass.getName()
                    + "\n" + ex.toString());

            return null;

        }

        return Result;

    }

    /**
     * This method instantiates an object from a {@code Class}. The
     * constructor is fetched using provided parameters values.
     *
     * <p>The method logs a SEVERE record and returns {@code null} if an exception
     * is encountered.
     *
     * @param inClass The {@code Class} to instantiate
     * @param inParamsValues Any number of parameter values for the constructor
     * @return an {@code Object} class or {@code null}
     */
    public static Object instantiate(Class inClass, Object... inParametersValues) {

        Class[] TheTypes = new Class[inParametersValues.length];
        Object[] TheValues = new Object[inParametersValues.length];

        for (int i=0;i<inParametersValues.length;i++) {

            TheTypes[i] = inParametersValues[i].getClass();
            TheValues[i] = inParametersValues[i];

        }

        return instantiate(inClass, TheTypes, TheValues);

    }

    /**
     * This method instantiates an object from a {@code Class} using its no
     * parameter constructor. The latter is fetched using reflexion.
     *
     * <p>The method logs a SEVERE record and returns {@code null} if an exception is
     * encountered.
     *
     * @param inClass The {@code Class} to instantiate
     * @return an {@code Object} class or {@code null}
     */
    public static Object instantiateWithNoParameterConstructor(Class inClass) {

        return instantiate(inClass, new Class[0], new Object[0]);
        
    }

    /**
     * Default constructor
     */
    private JxseInstantiator() {
    }

}
