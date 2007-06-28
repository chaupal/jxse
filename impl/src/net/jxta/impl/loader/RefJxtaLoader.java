/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.impl.loader;


import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.impl.peergroup.CompatibilityEquater;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * This class is the reference implementation of the JxtaLoader.
 */
public class RefJxtaLoader extends JxtaLoader {

    /**
     * The equator we will use to determine if compatibility statements are
     * compatible with this JXTA implementation.
     */
    private final CompatibilityEquater equator;

    /**
     * <p/><ul>
     * <li>Keys are {@link net.jxta.platform.ModuleSpecID}.</li>
     * <li>Values are {@link java.util.Map}.
     * <ul>
     * <li>Keys are {@link java.lang.String} Compatibility Statements serialized as XML UTF-8</li>
     * <li>Values are {@link java.lang.Class}.</li>
     * </ul>
     * </li>
     * </ul>
     */
    private final Map<ModuleSpecID, Map<String, Class>> classes = new HashMap<ModuleSpecID, Map<String, Class>>();

    /**
     * Classes and ImplAdvs we have known. Weak Map so that classes can be GCed.
     */
    private final Map<Class, ModuleImplAdvertisement> implAdvs = new WeakHashMap<Class, ModuleImplAdvertisement>();

    /**
     * Construct a new loader for the specified URLS with the default parent
     * loader and specified compatibility equator.
     *
     * @param urls    The URLs from which to load classes and resources.
     * @param equator The equator to use in comparing compatibility statements.
     */
    public RefJxtaLoader(URL[] urls, CompatibilityEquater equator) {
        this(urls, RefJxtaLoader.class.getClassLoader(), equator);
    }

    /**
     * Construct a new loader for the specified URLS with the specified parent
     * loader and specified compatibility equator.
     *
     * @param urls    The URLs from which to load classes and resources.
     * @param parent  The parent class loader for delegation.
     * @param equator The equator to use in comparing compatibility statements.
     */
    public RefJxtaLoader(URL[] urls, ClassLoader parent, CompatibilityEquater equator) {
        super(urls, parent);
        this.equator = equator;
    }

    /**
     * Make a stub for a version that uses URL, so that code that load
     * services can be written properly, even if it works only for classes
     * that do not need download.
     *
     * @param name    The class name.
     * @param url     The location of the class.
     * @param resolve If {@code true} then resolve the class.
     * @return the class
     * @throws ClassNotFoundException if class not found
     */
    protected Class loadClass(String name, URL url, boolean resolve) throws ClassNotFoundException {
        try {
            return loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            if (url != null) {
                addURL(url);
                return loadClass(name, resolve);
            } else {
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class<?> newClass = findLoadedClass(name);

        if (newClass == null) { // I'd rather say parent.loadClass() but it is private
            try {
                newClass = super.loadClass(name, false);
            } catch (ClassNotFoundException ignored) {
                // that's ok
                ;
            }
        }

        if (newClass == null) {
            try {
                newClass = findSystemClass(name);
                if (newClass != null) {
                    return newClass;
                }
            } catch (ClassNotFoundException ignored) {
                // that's ok
                ;
            }

            // We need to also check if the Context ClassLoader associated to the
            // the current thread can load the class.
            if (newClass == null) {
                try {
                    newClass = Thread.currentThread().getContextClassLoader().loadClass(name);
                    if (newClass != null) {
                        return newClass;
                    }
                } catch (ClassNotFoundException ignored) {
                    // that's ok
                    ;
                }
            }

            // try {
            // byte[] buf = bytesForClass(name);
            // newClass = defineClass(name, buf, 0, buf.length);
            // } catch (IOException e) {
            // throw new ClassNotFoundException(e.toString());
            // }
        }

        if (resolve) {
            resolveClass(newClass);
        }

        return newClass;
    }

    // /**
    // *  {@inheritDoc}
    // **/
    // protected byte[] bytesForClass(String name)
    // throws IOException, ClassNotFoundException {
    //
    // File file = new File( dir, name.replace('.', File.separatorChar) + ".java");
    // FileInputStream in = new FileInputStream(file);
    // int length = (int) file.length();
    // if (length == 0)
    // throw new ClassNotFoundException(name);
    // byte[] buf = new byte[length];
    // in.read(buf);
    // return buf;
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Class findClass(ModuleSpecID spec) throws ClassNotFoundException {

        Map<String, Class> compats = classes.get(spec);

        if (null == compats) {
            throw new ClassNotFoundException("No matching class for : " + spec);
        }

        for (Map.Entry<String, Class> anEntry : compats.entrySet()) {
            String aCompat = anEntry.getKey();

            StructuredDocument asDoc;

            try {
                asDoc = StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(aCompat));
            } catch (IOException ignored) {
                continue;
            }

            if (equator.compatible(asDoc)) {
                return anEntry.getValue();
            }
        }

        throw new ClassNotFoundException(spec.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class loadClass(ModuleSpecID spec) throws ClassNotFoundException {

        Class found = findClass(spec);

        resolveClass(found);

        return found;
    }

    /**
     * Loads a class
     *
     * @param name class name
     * @param url  url to class
     * @return the Class
     * @throws ClassNotFoundException if class not found
     */
    public Class loadClass(String name, URL url) throws ClassNotFoundException {
        return loadClass(name, url, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Class defineClass(ModuleImplAdvertisement impl) throws ClassFormatError {
        String asString = impl.getCompat().toString();

        Map<String, Class> compats = classes.get(impl.getModuleSpecID());

        if (null == compats) {
            compats = new HashMap<String, Class>();
            classes.put(impl.getModuleSpecID(), compats);
        }

        Class loaded = compats.get(asString);

        if (null == loaded) {
            try {
                loaded = loadClass(impl.getCode(), new URL(impl.getUri()), false);
            } catch (ClassNotFoundException failed) {
                throw new ClassFormatError("Class '" + impl.getCode() + "' could not be loaded from : " + impl.getUri());
            } catch (MalformedURLException failed) {
                throw new ClassFormatError("Cannot load class '" + impl.getCode() + "' from : " + impl.getUri());
            }

            compats.put(asString, loaded);

            implAdvs.put(loaded, impl);
        }

        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModuleImplAdvertisement findModuleImplAdvertisement(Class clazz) {
        ModuleImplAdvertisement result = implAdvs.get(clazz);

        return result;
    }
}
