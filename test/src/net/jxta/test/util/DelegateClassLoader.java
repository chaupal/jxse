
package net.jxta.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility ClassLoader for use in situations where the standard delegation
 * model proves to be inadequate.  Specifically, this class allows the
 * programmer to create a classloading decoupling point to prevent classes
 * being loaded from actually being defined by the ClassLoader containing
 * the class/resource definition, whether it be the system, parent, or
 * child class loader.  This is useful to prevent a class from being loaded
 * too high up the delegation chain and thereby becoming immutable at too
 * high of a level.  Likewise, since the child ClassLoaders are only used to
 * obtain the resource/class definition (as opposed to resolving and defining
 * it), this class can raise the definition scope of the target being
 * loaded.
 * <p/>
 * In general, the delegation model of class loading is both appropriate and
 * desirable.  This class becomes of particular value only in environments
 * where class namespace isolation is desired and in situations where class
 * reloading may occur.
 * <p/>
 * NOTE: Since certain system class are not allowed to be redefined due to
 * security and stability concerns, all "java." and "javax." classes will
 * always be loaded by the system ClassLoader.
 */
public class DelegateClassLoader extends SecureClassLoader {
    /**
     * Log object for this class.
     */
    private static final Logger LOG =
            Logger.getLogger(DelegateClassLoader.class.getName());
    
    /**
     * Initial size of the buffer used to convert a class
     * data stream into a <code>Class</code> instance.
     */
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Incremental size change when the buffer used to
     * convert the data stream into a <code>Class</code>
     * object is not large enough to contain all the
     * data.
     */
    private static final int BUFFER_INCR = 4096;
    
    /**
     * Local reference to the system class loader.
     */
    private static final ClassLoader SYS_LOADER =
            ClassLoader.getSystemClassLoader();
    
    /**
     * Regular expression to select those classes which MUST be loaded using
     * the system ClassLoader.
     */
    private static final Pattern PATTERN_SYS_CLASSES =
            Pattern.compile("^javax?\\..*");
    
    /**
     * List of patterns which will cause a Class to be forcibly redefined by
     * this ClassLoader should the name of the Class match a pattern in this
     * list (and not match any patterns in the neverRedefine list).
     */
    private final Set<Pattern> redefinePatterns =
            new CopyOnWriteArraySet<Pattern>();
    
    /**
     * List of patterns which will prevent a Class from ever being forcibly
     * redefined, even if it matches one of the Patterns in the
     * redefinePatterns list.
     */
    private final Set<Pattern> neverRedefinePatterns =
            new CopyOnWriteArraySet<Pattern>();
    
    /**
     * Stores the order which should be used when
     * looking for a loader.
     */
    private final List<ClassLoader> searchOrder =
            new CopyOnWriteArrayList<ClassLoader>();
    
    /**
     * CodeSource to associate with all loaded classes.
     */
    private final CodeSource codeSource;
    
    /**
     * Creates a class loader instance using the current
     * <code>ClassLoader</code> as the parent loader.
     */
    public DelegateClassLoader() {
        this(null, null);
    }
    
    /**
     * Creates a class loader instance using the specified
     * loader as the parent loader.
     *
     * @param parent parent ClassLoader
     */
    public DelegateClassLoader(ClassLoader parent) {
        this(null, parent);
    }
    
    /**
     * Creates a class loader instance using the specified loader
     * as the parent loader and the specified code source instance for
     * all classes that we load.
     * 
     * @param theCodeSource code source to assign to all loaded classes, or
     *  {@code null} for the default, per-loader code source
     * @param parent parent class loader, or {@code null} to use only the
     *  system class loader
     */
    public DelegateClassLoader(
            final CodeSource theCodeSource, ClassLoader parent) {
        super(parent);
        
        // Always search the system loader first
        searchOrder.add(SYS_LOADER);
        if (parent != null) {
            // Then search the parent loader, if provided
            if (!searchOrder.contains(parent)) {
                searchOrder.add(parent);
            }
        }
        
        // We can never redefine classes which are protected by Java spec
        neverRedefinePatterns.add(PATTERN_SYS_CLASSES);
        
        if (theCodeSource == null) {
            try {
                URL url = new URL("file:/" + getClass().getName() + "/"
                        + toString());
                codeSource = new CodeSource(url, (Certificate[]) null);
            } catch (MalformedURLException malx) {
                throw new IllegalStateException("should never happen", malx);
            }
        } else {
            codeSource = theCodeSource;
        }
    }
    
    /**
     * Adds a regular expression which will be used to test a class name
     * during a class loading attempt to see if the class name should be
     * loaded form the delegated class loader instances and redefined locally
     * by this class.  This creates a local classloading confluence which will
     * keep as many subsequent loading requests as possible coming through
     * this ClassLoader.
     * 
     * @param pattern pattern to add
     */
    public void addClassRedefinePattern(Pattern pattern) {
        redefinePatterns.add(pattern);
    }
    
    /**
     * Adds a regular expression which will be used to test a class name
     * during a class loading attempt to see if the class name should 
     * never be locally redefined by this class.  This protects against
     * reloading attempts which would otherwise be illegal or undesirable,
     * such as reloading the Java APIs loaded by the ssytem class loader.
     * (Note that any class name starting with {@code java.} or {@code javax.}
     * are implicitly in this list, by default).
     * 
     * @param pattern pattern to add
     */
    public void addClassNeverRedefinePattern(Pattern pattern) {
        neverRedefinePatterns.add(pattern);
    }
    
    /**
     * Adds a ClassLoader to the class/resource search path
     * of this class loader.  ClassLoaders are search in order of
     * addition.
     *
     * @param loader child ClassLoader to delegate requests to
     */
    public void addClassLoader(ClassLoader loader) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("addClassLoader(" + loader + ")");
        }
        if (!searchOrder.contains(loader)) {
            searchOrder.add(loader);
        }
    }
    
    //////////////////////////////////////////////////////////////////////
    //  ClassLoader overrides:
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class result = null;
        URL res = null;
        long findStart=0;
        long findTime;
        
        if (LOG.isLoggable(Level.FINE)) {
            findStart = System.currentTimeMillis();
            LOG.fine("findClass(" + name + ")");
        }
        
        // Determine the resource name for this class name
        String resName = name.replace('.', '/').concat(".class");
        boolean redefine = shouldRedefine(name);
        
        // See if we've already loaded this class
        result = this.findLoadedClass(name);
        if (result != null) {
            LOG.finer("Class was previously loaded");
            return result;
        }
        
        // Always use the system loader first.
        try {
            /*
             * Only redefine if flag set and the class is not in the list of
             * classes which must be loaded by the system ClassLoader.
             */
            if (redefine) {
                LOG.finer("Searching for system resource definition");
                res = findResource(resName);
                if (res == null) {
                    LOG.finer("Class resource not found for redefinition");
                } else {
                    LOG.finer("Redefining class resource");
                    result = defineClass(name, res);
                }
            } else {
                LOG.finer("Using system/parent ClassLoader");
                result = super.loadClass(name, resolve);
            }
        } catch (ClassNotFoundException cnfx) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("System/parent ClassLoader could not find class: "
                        + name);
            }
            // Okay.  Try our loader now.
        }
        
        // Get the first definition of this resource name
        if (result == null) {
            LOG.finer("Searching for child resource definition");
            res = findResource(resName);
            if (res != null) {
                LOG.finer("Redefining child data");
                result = defineClass(name, res);
            }
        }
        
        if (LOG.isLoggable(Level.FINEST)) {
            findTime = System.currentTimeMillis() - findStart;
            LOG.finest("findClass(" + name + ") result: " + result);
            LOG.finest("findClass(" + name + ") time: " + findTime);
        }
        
        if (result != null) {
            if (resolve) {
                resolveClass(result);
            }
            return result;
        }
        
        throw new ClassNotFoundException("Class not found: " + name);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("getResource(" + name + ")");
        }
        return findResource(name);
    }
    
    
    /**
     * {@inheritDoc}
     * 
     * @throws IOException on resource location error
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("findResources(" + name + ")");
        }
        return findResources(name, false);
    }
    
    /**
     * {@inheritDoc}
     */
    private Enumeration<URL> findResources(
            final String name, boolean firstOnly)
            throws IOException {        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("findResources(" + name + ", " + firstOnly + ")");
        }
        
        // Remove any leading slashes.
        String normalizedName = name;
        while (normalizedName.charAt(0) == '/') {
            try {
                normalizedName = normalizedName.substring(1);
            } catch (IndexOutOfBoundsException ioobx) {
                normalizedName = "";
            }
        }
        
        // Search through the search path for resource definitions
        ArrayList<URL> found = new ArrayList<URL>();
        for (ClassLoader loader : searchOrder) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("    Searching loader: " + loader);
            }
            
            Enumeration<URL> anEnum = loader.getResources(normalizedName);
            while (anEnum.hasMoreElements()) {
                URL url = anEnum.nextElement();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.finer("    FOUND: " + url);
                }
                found.add(url);
                if (firstOnly) {
                    return Collections.enumeration(found);
                }
            }
        }
        
        return Collections.enumeration(found);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public URL findResource(String name) {
        Enumeration anEnum;
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("findResource(" + name + ")");
        }
        
        try {
            anEnum = findResources(name, true);
            return (URL) anEnum.nextElement();
        } catch (Exception x) {
            return null;
        }
    }
    
    //////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Determines if the class name provided should be forcibly redefined
     * by this ClassLoader.
     * 
     * @param className name of the class to evaluate
     * @return {@code true} if the class should eb forcibly redefined,
     *  {@code false} otherwise
     */
    private boolean shouldRedefine(String className) {
        Iterator<Pattern> iter = redefinePatterns.iterator();
        boolean redefine = false;
        while (iter.hasNext() && !redefine) {
            Pattern pattern = iter.next();
            Matcher matcher = pattern.matcher(className);
            redefine = matcher.matches();
        }
        if (redefine) {
            // Ensure we don't allow redefinition of forbidden classes
            iter = neverRedefinePatterns.iterator();
            while (iter.hasNext() && redefine) {
                Pattern pattern = iter.next();
                Matcher matcher = pattern.matcher(className);
                redefine = !matcher.matches();
            }
        }
        return redefine;
    }
    
    /**
     * Loads a URL resource into a byte array so that it can be
     * directly used in a defineClass() call.
     *
     * @param res resource URL to read
     * @return byte array containing rw definition data
     * @throws IOException on data read failure
     */
    private static byte[] loadResource(URL res)
    throws IOException {
        InputStream input;
        byte[] data, tmp;
        int used;
        int len;
        long start=0;
        long time;
        
        // XXX: Rework this method with ByteArrayOutputStream usage
        
        if (LOG.isLoggable(Level.FINE)) {
            start = System.currentTimeMillis();
            LOG.finer("loadResource(" + res + ")");
        }
        
        // Initialize our buffer
        data = new byte[BUFFER_SIZE];
        used = 0;
        
        // Read in the resource data
        input = res.openStream();
        while (true) {
            len = data.length - used;
            len = input.read(data, used, len);
            if (len < 0) {
                // End of file
                break;
            }
            used += len;
            if (used == data.length) {
                // Increase buffer size
                tmp = new byte[data.length + BUFFER_INCR];
                System.arraycopy(data, 0, tmp, 0, used);
                data = tmp;
            }
        }
        
        // Trim up the buffer
        tmp = new byte[used];
        System.arraycopy(data, 0, tmp, 0, used);
        if (LOG.isLoggable(Level.FINEST)) {
            time = System.currentTimeMillis() - start;
            LOG.finest("    loadResource(" + res + ") time: " + time);
        }
        return tmp;
    }
    
    /**
     * Defines a class using the data found at the other end of the specified
     * URL.
     *
     * @param name class name to define
     * @param classDataURL class data resource URL
     * @return class definition
     */
    private Class defineClass(String name, URL classDataURL) {
        byte[] data;
        Class result = null;
        
        // Load the class data
        try {
            data = loadResource(classDataURL);
            result = defineClass(name, data, 0, data.length, codeSource);
        } catch (Exception x) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Couldn't load resource data\n", x);
            }
        }
        
        return result;
    }
}
