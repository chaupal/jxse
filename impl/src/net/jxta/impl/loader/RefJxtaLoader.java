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

import net.jxta.content.*;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.content.TransferAggregator;
import net.jxta.impl.peergroup.CompatibilityEquater;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is the reference implementation of the JxtaLoader.
 */
public class RefJxtaLoader extends JxtaLoader {

    /**
     * Logger
     */
    private final static transient Logger LOG =
            Logger.getLogger(RefJxtaLoader.class.getName());
    
    /**
     * Maximum amount of time which will be allotted to the retrieval of
     * remoge package Content items.
     */
    private static final long MAX_XFER_TIME =
            Long.getLong(RefJxtaLoader.class.getName() + ".maxTransferTime",
            60 * 1000L);
    
    /**
     * The equator we will use to determine if compatibility statements are
     * compatible with this JXTA implementation.
     */
    private final CompatibilityEquater equator;
    
    /**
     * The PeerGroup we are loading modules for, or null if unknown.
     */
    private final PeerGroup group;

    /**
     * <ul>
     *     <li>Keys are {@link net.jxta.platform.ModuleSpecID}.</li>
     *     <li>Values are {@link java.util.Map}.
     *         <ul>
     *             <li>Keys are {@link java.lang.String} Compatibility Statements serialized as XML UTF-8</li>
     *             <li>Values are {@link java.lang.Class}<? extends Module>.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private final Map<ModuleSpecID, Map<String, Class<? extends Module>>> classes =
            new HashMap<ModuleSpecID, Map<String, Class<? extends Module>>>();

    /**
     * Classes and ImplAdvs we have known. Weak Map so that classes can be GCed.
     */
    private final Map<Class<? extends Module>, ModuleImplAdvertisement> implAdvs =
            new WeakHashMap<Class<? extends Module>, ModuleImplAdvertisement>();

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
        this(urls, parent, equator, null);
    }
    
    /**
     * Construct a new loader for the specified URLS with the specified parent
     * loader and specified compatibility equator and the specified peer
     * group.  The addition of the PeerGroup allows the loader to support
     * loading of package URIs specified as ContentIDs.
     *
     * @param urls    The URLs from which to load classes and resources.
     * @param parent  The parent class loader for delegation.
     * @param equator The equator to use in comparing compatibility statements.
     * @param pGroup  The peer group which this loader is loading modules for
     */
    public RefJxtaLoader(URL[] urls, ClassLoader parent, CompatibilityEquater equator, PeerGroup pGroup) {
        super(urls, parent);
        this.equator = equator;
        group = pGroup;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // General ClassLoader extensions:

    /**
     * Loads a class, falling back to attempting to load the class from the
     * URL provided, if available.
     *
     * @param name class name
     * @param uri  URI to class (e.g., jar URL, ContentID of jar)
     * @return the Class
     * @throws ClassNotFoundException if class not found
     */
    protected Class loadClass(String name, URI uri) throws ClassNotFoundException {
        return loadClass(name, uri, false);
    }

    /**
     * Make a stub for a version that uses URL, so that code that load
     * services can be written properly, even if it works only for classes
     * that do not need download.
     *
     * @param name    The class name.
     * @param uri     The location of the class.
     * @param resolve If {@code true} then resolve the class.
     * @return the class
     * @throws ClassNotFoundException if class not found
     */
    protected Class loadClass(String name, URI uri, boolean resolve) throws ClassNotFoundException {
        try {
            // First, make sure we don't already have it loaded/loadable
            return loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            if (uri == null) {
                // Nothing more we can do
                throw(e);
            }
            
            /*
             * If the package URI is a JXTA ContentID we will need to retrieve
             * the Content and then redefine the URL we use later on, pointing
             * the URL at the newly downloaded Content which we assume is a
             * jar file.
             */
            try {
                ID id = IDFactory.fromURI(uri);
                if (!(id instanceof ContentID)) {
                    throw(new ClassNotFoundException(
                            "Of all JXTA IDs, only ContentIDs are supported package URIs"));
                }
                
                URI contentURI = retrieveContent((ContentID) id);
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(hashHex() + ": Using Content URI: " + contentURI);
                }
                
                // Switch to the Content URI and fall through
                uri = contentURI;
            } catch (URISyntaxException urisx) {
                // Not a JXTA ID.  Fall through and try as URL.
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(hashHex() + ": Not a JXTA ID: " + uri);
                }
            }

            /*
             * Fall back to the standard jar loading mechanism.  We just
             * turn it into a URL and add the URL to what the URLClassLoader
             * handles, then try loading the class again.
             */
            try {
                URL url = uri.toURL();
                addURL(url);
                return loadClass(name, resolve);
            } catch (MalformedURLException mux) {
                throw(new ClassNotFoundException(
                        "Could not load class from URL: " + uri));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to the default ClassLoader
     * implementation with one exception - it attempts to load classes
     * via the thread's context class loader if all the normal sources
     * fail to load the requested class.
     */
    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        /*
         * First, we ask the super-class.  This will consult the local
         * classes already laoded, the parent loader (if set), and the
         * system loader.  Failing those it will call the local
         * findClass(String) method.
         */
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException cnfx) {
            // Fall through
        }
        
        /*
         * The only thing left to try is the context loader.
         */
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        Class newClass = contextLoader.loadClass(name);
        if (resolve) {
            resolveClass(newClass);
        }
        return newClass;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // JxtaLoader implementation:

    /**
     * {@inheritDoc}
     * 
     * @throws ClassNotFoundException if module cannot be loaded or if the
     *  class which is loaded is not a Module implementation
     */
    @Override
    public synchronized Class<? extends Module> findClass(ModuleSpecID spec) throws ClassNotFoundException {
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": findClass(MSID=" + spec + ")");
        }
        
        // search through already existing compats for something that works
        // if found, return it
        Class<? extends Module> result = searchCompats(spec);
        if (result != null) {
            return result;
        }
        
        // search for more compats via Jar SPI
        // results contain MSID, Class name, and description
        // if MSID matches, generate/discover MIA, then define the class.
        locateModuleImplementations(spec);
        
        // search through compats again.
        // if found, return it
        // throw CNFE
        result = searchCompats(spec);
        if (result != null) {
            return result;
        }
        
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + "    No class found for MSID");
        }
        throw new ClassNotFoundException(spec.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ClassNotFoundException if class cannot be found or if class
     *  is not a Module implementation
     */
    @Override
    public Class<? extends Module> loadClass(ModuleSpecID spec) throws ClassNotFoundException {
        /*
         * Here we replicate the logic of the standard
         * ClassLoader.loadClass(String,boolean) method, but this time we
         * do so for Modules.  The only main difference is that we only
         * defer to our parent loader (since it is the only JxtaLoader).
         */
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": loadClass(MSID=" + spec + ")");
        }
        
        // Try the parent JxtaLoader, if present
        try {
            ClassLoader parentLoader = getParent();
            if (parentLoader instanceof JxtaLoader) {
                JxtaLoader jxtaLoader = (JxtaLoader) parentLoader;
                Class<? extends Module> result = jxtaLoader.loadClass(spec);
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(hashHex() + ": Parent found: " + result);
                }
                return result;
            } else {
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(hashHex() + ": No parent loader to try.");
                }
            }
        } catch (ClassNotFoundException cnfx) {
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, hashHex() + ": Parent could not load MSID: " + spec);
            }
            // Fall through
        }
        
        // Now try locally
        try {
            Class found = findClass(spec);
            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.finest(hashHex() + ": Self loaded: " + found);
            }
            return verifyAndCast(found);
        } catch (ClassNotFoundException cnfx) {
            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.finest(hashHex() + ": Self loader threw: "
                        + cnfx.getClass() + ": " + cnfx.getMessage());
            }
            throw(cnfx);
        }
    }

    /**
     * {@inheritDoc}
     * @throws ClassFormatError if class cannot be found or if class is not
     *  a Module implementation
     */
    @Override
    public synchronized Class<? extends Module> defineClass(ModuleImplAdvertisement impl) throws ClassFormatError {
        String asString = impl.getCompat().toString();

        // See if we have any classes defined for this ModuleSpecID.
        // Note that there may be multiple definitions with different compatibility statements.
        Map<String, Class<? extends Module>> compats = classes.get(impl.getModuleSpecID());

        if (null == compats) {
            compats = new HashMap<String, Class<? extends Module>>();
            classes.put(impl.getModuleSpecID(), compats);
        }

        // See if there is a class defined which matches the compatibility statement of the implAdv.
        Class<? extends Module> loaded = compats.get(asString);

        if (null == loaded) {
            try {
                URI uri = URI.create(impl.getUri());
                Class<?> clazz = loadClass(impl.getCode(), uri, false);
                loaded = verifyAndCast(clazz);
            } catch (IllegalArgumentException iax) {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, hashHex() + ": Caught exception", iax);
                }
                throw new ClassFormatError("Class '" + impl.getCode()
                        + "' could not be loaded from : " + impl.getUri());
            } catch (ClassNotFoundException failed) {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, hashHex() + ": Caught exception", failed);
                }
                throw new ClassFormatError("Class '" + impl.getCode()
                        + "' could not be loaded from : " + impl.getUri());
            }

            // Remember the class along with the matching compatibility statement.
            compats.put(asString, loaded);
        }

        // Force update of impl advertisement. This is done because the class will frequently redefine itself.
        implAdvs.put(loaded, impl);

        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModuleImplAdvertisement findModuleImplAdvertisement(Class clazz) {
        Class<? extends Module> modClass;
        try {
            modClass = verifyAndCast(clazz);
        } catch (ClassNotFoundException cnfx) {
            // Not a Module class
            return null;
        }
        
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": findModuleImplAdv(" + clazz + ")");
        }
        ClassLoader parentLoader = getParent();
        if (parentLoader instanceof JxtaLoader) {
            JxtaLoader jxtaLoader = (JxtaLoader) parentLoader;
            ModuleImplAdvertisement result = jxtaLoader.findModuleImplAdvertisement(modClass);
            if (result != null) {
                return result;
            }
        }
        
        ModuleImplAdvertisement result = implAdvs.get(modClass);
        if (result == null) {
            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.finest(hashHex() + ":    MIA for class not found");
            }
            return null;
        } else {
            return result.clone();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModuleImplAdvertisement findModuleImplAdvertisement(ModuleSpecID msid) {
        Class<? extends Module> moduleClass;

        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": findModuleImplAdvertisement(MSID=" + msid + ")");
        }
        try {
            moduleClass = loadClass(msid);
        } catch (ClassNotFoundException failed) {
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, hashHex() + ": Failed to find class for " + msid, failed);
            }
            return null;
        }

        return findModuleImplAdvertisement(moduleClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(super.toString());
        result.append(" - Classes: ");
        for (Map.Entry<ModuleSpecID, Map<String, Class<? extends Module>>> eachMCID : classes.entrySet()) {
            ModuleSpecID mcid = eachMCID.getKey();
            result.append("\n\t").append(mcid).append(" :");
            for (Map.Entry<String, Class<? extends Module>> eachClass : eachMCID.getValue().entrySet()) {
                result.append("\n\t\t").append(eachClass.getValue().toString());
            }
        }

        return result.toString();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Searches through the already discovered compatibility statements of
     * module implementations looking for something already loaded which
     * will work.
     * 
     * @param msid ModuleSpecID to search for
     * @return Class instance which (compatibly) implements the module spec,
     *  or null if no class is know about which satisfies the module spec
     *  compatibly
     */
    private Class<? extends Module> searchCompats(ModuleSpecID msid) {
        Map<String, Class<? extends Module>> compats = classes.get(msid);

        if (null == compats) {
            return null;
        }

        for (Map.Entry<String, Class<? extends Module>> anEntry : compats.entrySet()) {
            String aCompat = anEntry.getKey();

            StructuredDocument asDoc;

            try {
                asDoc = StructuredDocumentFactory.newStructuredDocument(
                        MimeMediaType.XMLUTF8, new StringReader(aCompat));
            } catch (IOException iox) {
                if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, hashHex() + ": Caught exception", iox);
                }
                continue;
            }

            if (equator.compatible(asDoc)) {
                return anEntry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Attempt to locate implementations of the specified ModuleSpecID.
     * We'll use a process similar to the standard Jar SPI mechanism to
     * perform the discovery.  We do this in an on-demand basis to allow
     * for natural dependency resolution between Modules.
     * 
     * @param msid ModuleSpecID to search for
     */
    private void locateModuleImplementations(ModuleSpecID msid) {
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": discoverModuleImplementations(MSID=" + msid + ")");
        }

        List<ModuleImplAdvertisement> locatedAdvs = null;
        try {
            Enumeration<URL> allProviderLists =
                    getResources("META-INF/services/net.jxta.platform.Module");
            for (URL providers : Collections.list(allProviderLists)) {
                List<ModuleImplAdvertisement> located =
                        locateModuleImplementations(msid, providers);
                if (located != null) {
                    if (locatedAdvs == null) {
                        locatedAdvs = new ArrayList<ModuleImplAdvertisement>();
                    }
                    locatedAdvs.addAll(located);
                }
            }
        } catch (IOException ex) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Failed to locate provider lists", ex);
            }
        }
        
        if (locatedAdvs == null) {
            // Early out.
            return;
        }
        
        for (ModuleImplAdvertisement mAdv : locatedAdvs) {
            defineClass(mAdv);
        }
    }

    /**
     * Register instance classes given a URL to a file containing modules which
     * must be found on the current class path. Each line of the file contains a 
     * module spec ID, the class name and the Module description. The fields are 
     * separated by whitespace. Comments are marked with a '#', the pound sign. 
     * Any text following # on any line in the file is ignored.
     *
     * @param specID ModuleSpecID that we are seeking implementations of
     * @param providers URL to a resource containing a list of providers.
     * @return list of discovered ModuleImplAdvertisements for the specified
     *  ModuleSpecID, or null if no results were found.
     */
    private List<ModuleImplAdvertisement> locateModuleImplementations(
            ModuleSpecID specID, URL providers) {
        List<ModuleImplAdvertisement> result = null;
        InputStream urlStream = null;
        
        if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
            LOG.finest(hashHex() + ": discoverModuleImplementations(MSID="+ specID + ", URL=" + providers + ")");
        }
        try {
            urlStream = providers.openStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(urlStream, "UTF-8"));
            
            String provider;
            while ((provider = reader.readLine()) != null) {
                int comment = provider.indexOf('#');
                
                if (comment != -1) {
                    provider = provider.substring(0, comment);
                }
                
                provider = provider.trim();
                
                if (0 == provider.length()) {
                    continue;
                }
                
                try {
                    ModuleImplAdvertisement mAdv = null;
                    String[] parts = provider.split("\\s", 3);
                    if (parts.length == 1) {
                        // Standard Jar SPI format:  Class name
                        mAdv = locateModuleImplAdvertisement(parts[0]);
                    } else if (parts.length == 3) {
                        // Current non-standard format: MSID, Class name, Description
                        ModuleSpecID msid = ModuleSpecID.create(URI.create(parts[0]));
                        String code = parts[1];
                        String description = parts[2];
                        
                        if (!msid.equals(specID)) {
                            // Early-out here to prevent unnecessary work
                            continue;
                        }
                        
                        mAdv = locateModuleImplAdvertisement(code);
                        if (mAdv == null) {
                            // Create one
                            mAdv = CompatibilityUtils.createModuleImplAdvertisement(msid, code, description);
                        }
                    } else {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, hashHex() + ": Failed to register \'" + provider + "\'");
                        }
                    }
                    
                    if (mAdv != null) {
                        if (result == null) {
                            result = new ArrayList<ModuleImplAdvertisement>();
                        }
                        result.add(mAdv);
                    }
                } catch (Exception allElse) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, hashHex() + ": Failed to register \'" + provider + "\'", allElse);
                    }
                }
            }
        } catch (IOException ex) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, hashHex() + ": Failed to read provider list " + providers, ex);
            }
        } finally {
            if (null != urlStream) {
                try {
                    urlStream.close();
                } catch (IOException ignored) {
                    
                }
            }
        }
        
        return result;
    }
    
    /**
     * Attempts to locate the ModuleImplAdvertisement of a module by
     * the use of reflection.
     * 
     * @param className class name to examine
     * @return ModuleImplAdvertisement found by introspection, or null if
     *  the ModuleImplAdvertisement could not be discovered in this manner
     */
    private ModuleImplAdvertisement locateModuleImplAdvertisement(String className) {
        try {
            Class<?> moduleClass = (Class<?>) Class.forName(className);
            Class<? extends Module> modClass = verifyAndCast(moduleClass);
            Method getImplAdvMethod = modClass.getMethod("getDefaultModuleImplAdvertisement");
            return (ModuleImplAdvertisement) getImplAdvMethod.invoke(null);
        } catch(Exception ex) {
            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, hashHex() + ": Could not introspect Module for MIA: " + className, ex);
            }
        }
        return null;
    }
    
    /**
     * Checks that a class is a Module.  If not, it raises a an exception.
     * If it is, it casts the generic class to the subtype.
     * 
     * @param clazz generic class to verify
     * @return Module subtype class
     * @throws ClassNotFoundException if class was not of the proper type
     */
    private Class<? extends Module> verifyAndCast(Class<?> clazz)
    throws ClassNotFoundException {
        try {
            return clazz.asSubclass(Module.class);
        } catch (ClassCastException ccx) {
            throw(new ClassNotFoundException(
                    "Class found but was not a Module class: " + clazz));
        }
    }
    
    /**
     * Determines the location that we should use to store the retrieved
     * Content.  This location follows the current standards for Module
     * information persistance in the store home.
     * 
     * @param service ContentService instance 
     */
    private File getContentFile(ContentID forContentID) {
        ModuleSpecID groupMSID =
                group.getPeerGroupAdvertisement().getModuleSpecID();
        URI storeHomeURI = group.getStoreHome();
        File storeHome = new File(storeHomeURI);
        File grpHome = new File(storeHome,
                group.getPeerGroupID().getUniqueValue().toString());
        File modHome = new File(grpHome, groupMSID.getUniqueValue().toString());
        File svcHome = new File(modHome, getClass().getSimpleName());
        if (!svcHome.isDirectory() && !svcHome.mkdirs()) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, hashHex() 
                        + ": Could not create Content dir: " + svcHome);
            }
        }
        File result =
                new File(svcHome, forContentID.getUniqueValue().toString());
        
        return result;
    }
    
    /**
     * Attempts to retrieve the remote Content which is assumed to be a
     * jar.  The Content will be stored in the PeerGroup's store home, ensuring
     * that it's our own private copy.  If the Content cannot be retrieved in
     * a reasonable amount of time this method will throw a ClassNotFound
     * exception and attempt to carry on.
     * 
     * @param contentID ID of the content to retrieve
     * @return URI to the local Content, once it has been retrieved.  Never
     *  returns {@code null}.
     * @throws ClassNotFoundException if the Content cannot be retrieved in a
     *  reasonable amount of time
     */
    private URI retrieveContent(ContentID contentID)
            throws ClassNotFoundException {
        if (group == null) {
            throw(new ClassNotFoundException(
                    "Loading of ContentID is only possible when JxtaLoader "
                    + "is constructed with a PeerGroup reference"));
        }
        
        // Get the storage location for this Content
        File file = getContentFile(contentID);
        if (file.exists()) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(hashHex()
                        + ": Using previously retrieved package Content: "
                        + file);
            }
            return file.toURI();
        }
        
        // Obtain and check content service instances from local and parent
        ContentService groupService = group.getContentService();
        PeerGroup parentGroup = group.getParentGroup();
        ContentService parentService;
        if (parentGroup == null) {
            parentService = null;
        } else {
            parentService = parentGroup.getContentService();
        }
        if (groupService == null && parentService == null) {
            throw(new ClassNotFoundException(
                    "No ContentService instance found in either the local "
                    + "group or the parent group"));
        }
        
        // Commence the transfer, creating an aggregation as necessary
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine(hashHex()
                    + ": Starting retrieval of Module package Content: "
                    + contentID);
        }
        ContentTransfer xfer;
        if (groupService == null) {
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.fine(hashHex() + ": Using only the parent ContentService");
            }
            xfer = parentService.retrieveContent(contentID);
        } else if (parentService == null) {
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.fine(hashHex() + ": Using only the local ContentService");
            }
            xfer = groupService.retrieveContent(contentID);
        } else {
            /*
             * We have both group and parent ContentServices.  Aggregate them
             * but prefer the local group over the parent.  We use an
             * aggregation to allow the ContentProviders in either of the
             * groups to respond immediately if the Content is being shared
             * by the local peer in either of the groups.
             */
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.fine(hashHex()
                        + ": Using both the local and parent ContentService");
            }
            
            List<ContentTransfer> toAgg = new ArrayList<ContentTransfer>();
            xfer = groupService.retrieveContent(contentID);
            if (xfer != null) {
                toAgg.add(xfer);
            }
            
            xfer = parentService.retrieveContent(contentID);
            if (xfer != null) {
                toAgg.add(xfer);
            }
            
            if (toAgg.size() == 0) {
                throw(new ClassNotFoundException(
                        "No Content providers were able to load content ID: "
                        + contentID));
            } else if (toAgg.size() == 1) {
                // No reason to use an aggregation.
                xfer = toAgg.get(0);
            } else {
                // Use an aggregation
                xfer = new TransferAggregator(null, toAgg);
            }
        }
        attachDebugListeners(xfer);
        
        Content content = null;
        try {
            xfer.startTransfer(file);
            xfer.waitFor(MAX_XFER_TIME);
            if (!xfer.getTransferState().isFinished()) {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.finer(hashHex() + ": Transfer did not complete in "
                            + "maximum allotted time");
                }
                xfer.cancel();
            }
            content = xfer.getContent();
        } catch (InterruptedException intx) {
            xfer.cancel();
            file.delete();
            throw(new ClassNotFoundException(
                    "Thread was interrupted during transfer attempt", intx));
        } catch (TransferException xferx) {
            xfer.cancel();
            file.delete();
            throw(new ClassNotFoundException(
                    "Package Content transfer failed", xferx));
        }
        
        // If the Content is not stored in the file, persist it
        if (!file.exists()) {
            // In-memory content.  Persist it to disk.
            try {
                FileOutputStream fileOut = new FileOutputStream(file);
                content.getDocument().sendToStream(fileOut);
                fileOut.close();
            } catch (IOException iox) {
                file.delete();
                throw(new ClassNotFoundException(
                        "Could not persist Content", iox));
            }
        }
        
        return file.toURI();
    }
    
    /**
     * Attaches listeners to the provider transfer for the purpose of
     * debug logging.
     * 
     * @param xfer transfer to attach listeners to
     */
    private void attachDebugListeners(ContentTransfer xfer) {
        if (!(Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST))) {
            // Early out.
            return;
        }
        
        final String prefix = hashHex();

        final ContentTransferListener xListener = new ContentTransferListener() {
            public void contentLocationStateUpdated(ContentTransferEvent event) {
                LOG.finest(prefix + ": Received event: " + event);
            }

            public void contentTransferStateUpdated(ContentTransferEvent event) {
                LOG.finest(prefix + ": Received event: " + event);
            }

            public void contentTransferProgress(ContentTransferEvent event) {
                LOG.finest(prefix + ": Received event: " + event);
            }
        };

        final ContentTransferAggregatorListener xaListener =
                new ContentTransferAggregatorListener() {
            public void selectedContentTransfer(ContentTransferAggregatorEvent event) {
                LOG.finest(prefix + ": Received event: " + event);
            }

            public void updatedContentTransferList(ContentTransferAggregatorEvent event) {
                LOG.finest(prefix + ": Received event: " + event);
            }
        };

        LOG.finest(hashHex() + ": Attaching ContentTransferListener to: " + xfer);
        xfer.addContentTransferListener(xListener);
        if (xfer instanceof ContentTransferAggregator) {
            ContentTransferAggregator xferAgg = (ContentTransferAggregator) xfer;
            LOG.finest(hashHex() + ": Attaching ContentTransferAggregatorListener to: " + xfer);
            xferAgg.addContentTransferAggregatorListener(xaListener);
            
            // Recurse...
            for (ContentTransfer child : xferAgg.getContentTransferList()) {
                attachDebugListeners(child);
            }
        }
    }
    
    /**
     * Returns the hashCode value in hex.
     * 
     * @return hex hashCode value
     */
    private String hashHex() {
        return Integer.toString(hashCode(), 16);
    }
    
}
