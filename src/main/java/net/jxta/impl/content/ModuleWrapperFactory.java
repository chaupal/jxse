/*
 *  The Sun Project JXTA(TM) Software License
 *
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.content;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.jxta.platform.Module;
import net.jxta.service.Service;

/**
 * Factory for producing proxy Module objects which ignore the Module
 * lifecycle method calls but pass everything else through to the
 * underlying Module instance.
 */
public class ModuleWrapperFactory {

    /**
     * Invocation handler which we will use to ignore Module
     * lifecycle methods.
     */
    private static class Handler implements InvocationHandler {
        /**
         * The Module instance backing this proxy.
         */
        private final Object instance;

        /**
         * Constructs a new InvocationHandler with the provided
         * backing Module instance.
         * 
         * @param target module instance
         */
        public Handler(Module target) {
            instance = target;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(
                Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass().equals(Module.class)) {
                // Return something that works when ignoring the call
                if ("startApp".equals(method.getName())) {
                    return Module.START_OK;
                } else {
                    return null;
                }
            } else {
                return method.invoke(instance, args);
            }
        }
    }

    /**
     * Convenience method for creating proxy objects which implement
     * the Module interface and use the provided Module as the backing
     * object.
     * 
     * @param module Module instance
     * @return new proxy object using module as a backing instance
     */
    public static Module newWrapper(Module module) {
        return newWrapper(
                new Class[] { Module.class },
                module);
    }

    /**
     * Convenience method for creating proxy objects which implement
     * the Service interface and use the provided Service as the backing
     * object.
     * 
     * @param service Service instance
     * @return new proxy object using module as a backing instance
     */
    public static Service newWrapper(Service service) {
        return (Service) newWrapper(
                new Class[] { Service.class },
                service);
    }

    /**
     * Generic form for creating proxy objects which implement
     * the specified list of interfaces and use the provided Module
     * as the backing object.
     * 
     * @param interfaces list of interfaces to implement
     * @param module Module instance
     * @return new proxy object using target as a backing instance
     */
    public static Module newWrapper(
            Class<?>[] interfaces, Module module) {
        Class<?>[] allInterfaces = interfaces;
        boolean notFound = true;

        for (int i=0; i<interfaces.length && notFound; i++) {
            if (interfaces[i].equals(Module.class)) {
                notFound = false;
            }
        }
        if (notFound) {
            // Add the Module interface to the list, as it is required
            allInterfaces = new Class[interfaces.length + 1];
            System.arraycopy(
                    interfaces, 0, allInterfaces, 0, interfaces.length);
            allInterfaces[interfaces.length] = Module.class;
        }

        return (Module) Proxy.newProxyInstance(
                module.getClass().getClassLoader(), allInterfaces,
                new Handler(module));
    }
}
