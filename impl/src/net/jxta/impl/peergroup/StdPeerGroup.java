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

package net.jxta.impl.peergroup;


import net.jxta.discovery.DiscoveryService;
import net.jxta.document.*;
import net.jxta.endpoint.MessageTransport;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.impl.cm.Cm;
import net.jxta.impl.cm.SrdiIndex;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.service.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A subclass of GenericPeerGroup that makes a peer group out of independent
 * plugin services listed in its impl advertisement.
 */
public class StdPeerGroup extends GenericPeerGroup {
    
    private final static transient Logger LOG = Logger.getLogger(StdPeerGroup.class.getName());
    
    // A few things common to all ImplAdv for built-in things.
    public static final XMLDocument STD_COMPAT = mkCS();
    public static final String MODULE_IMPL_STD_URI = "http://www.jxta.org/download/jxta.jar";
    public static final String MODULE_IMPL_STD_PROVIDER = "sun.com";
    
    protected static final String STD_COMPAT_FORMAT = "Efmt";
    
    // FIXME 20061206 bondolo Update this to "JRE1.5" after June 2007 release. 2.4.1 and earlier don't do version comparison correctly.
    
    /**
     * The Specification title and Specification version we require.
     */
    protected static final String STD_COMPAT_FORMAT_VALUE = "JDK1.4.1";
    protected static final String STD_COMPAT_BINDING = "Bind";
    protected static final String STD_COMPAT_BINDING_VALUE = "V2.0 Ref Impl";

    private volatile boolean started = false;
    
    /**
     * The minimum number of Threads our Executor will reserve. Once started
     * these Threads will remain
     */
    private static int COREPOOLSIZE = 5;
    
    /**
     * The maximum number of Threads our Executor will allocate.
     */
    private static int MAXPOOLSIZE = 150;
    
    /**
     * The number of seconds that Threads above {@code COREPOOLSIZE} will
     * remain idle before terminating.
     */
    private static int KEEPALIVETIME = 15;
    
    /**
     * The order in which we started the services.
     */
    private final List<ModuleClassID> moduleStartOrder = new ArrayList<ModuleClassID>();
    
    /**
     * A map of the Message Transports for this group.
     * <p/>
     * <ul>
     * <li>keys are {@link net.jxta.platform.ModuleClassID}</li>
     * <li>values are {@link net.jxta.platform.Module}, but should also be
     * {@link net.jxta.endpoint.MessageTransport}</li>
     * </ul>
     */
    private final Map<ModuleClassID, Object> messageTransports = new HashMap<ModuleClassID, Object>();
    
    /**
     * A map of the applications for this group.
     * <p/>
     * <ul>
     * <li>keys are {@link net.jxta.platform.ModuleClassID}</li>
     * <li>values are {@link net.jxta.platform.Module} or
     * {@link net.jxta.protocol.ModuleImplAdvertisement} or
     * {@link net.jxta.platform.ModuleSpecID}</li>
     * </ul>
     */
    private final Map<ModuleClassID, Object> applications = new HashMap<ModuleClassID, Object>();
    
    /**
     * Cache for this group.
     */
    private Cm cm = null;
    
    /**
     * The PeerGroup ThreadPool
     */
    private ThreadPoolExecutor threadPool;
    
    /**
     * Queue for tasks waiting to be run by our {@code Executor}.
     */
    private final BlockingQueue<Runnable> taskQueue;
    
    private final Set<ModuleClassID> disabledModules = new HashSet<ModuleClassID>();
    
    private ModuleImplAdvertisement allPurposeImplAdv = null;
    
    private static XMLDocument mkCS() {
        XMLDocument doc = (XMLDocument)
                StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Comp");
        
        Element e = doc.createElement(STD_COMPAT_FORMAT, STD_COMPAT_FORMAT_VALUE);

        doc.appendChild(e);
        
        e = doc.createElement(STD_COMPAT_BINDING, STD_COMPAT_BINDING_VALUE);
        doc.appendChild(e);
        return doc;
    }

    /**
     * Our rejected execution handler which has the effect of pausing the
     * caller until the task can be executed or queued.
     */
    private static class CallerBlocksPolicy implements RejectedExecutionHandler {
        
        /**
         *  The target maximum pool size. We will only exceed this amount if we
         *  are failing to make progress.
         */        
        private final int MAXPOOLSIZE;
        
        private CallerBlocksPolicy(int maxPoolSize) {
            MAXPOOLSIZE = maxPoolSize;
        }
        
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            BlockingQueue<Runnable> queue = executor.getQueue();

            while (!executor.isShutdown()) {
                executor.purge();

                try {
                    boolean pushed = queue.offer(runnable, 500, TimeUnit.MILLISECONDS);
                    
                    if (pushed) {
                        break;
                    }
                } catch (InterruptedException woken) {
                    // This is our entire handling of interruption. If the 
                    // interruption signaled a state change of the executor our
                    // while() loop condition will handle termination.
                    Thread.interrupted();
                    continue;
                }

                // Couldn't push? Add a thread!
                synchronized (executor) {
                    int currentMax = executor.getMaximumPoolSize();
                    int newMax = Math.min(currentMax + 1, MAXPOOLSIZE * 2);
                    
                    if (newMax != currentMax) {
                        executor.setMaximumPoolSize(newMax);
                    }
                    
                    // If we are already at the max, increase the core size
                    if (newMax == (MAXPOOLSIZE * 2)) {
                        int currentCore = executor.getCorePoolSize();
                        
                        int newCore = Math.min(currentCore + 1, MAXPOOLSIZE * 2);
                        
                        if (currentCore != newCore) {
                            executor.setCorePoolSize(newCore);
                        } else {
                            // Core size is at the max too. We just have to wait.
                            continue;
                        }
                    }
                }
                
                // Should work now.
                executor.execute(runnable);
                break;
            }
        }
    }

    /**
     * constructor
     */
    public StdPeerGroup() {
        // todo convert these hardcoded settings into group config params
        this.taskQueue = new ArrayBlockingQueue<Runnable>(MAXPOOLSIZE * 2);
        this.threadPool = new ThreadPoolExecutor(COREPOOLSIZE, MAXPOOLSIZE, KEEPALIVETIME, TimeUnit.SECONDS, taskQueue);
        threadPool.setRejectedExecutionHandler(new CallerBlocksPolicy(MAXPOOLSIZE));
    }
    
    /**
     * An internal convenience method essentially for bootstrapping.
     * Make a standard ModuleImplAdv for any service that comes builtin this
     * reference implementation.
     * In most cases there are no params, so we do not take that argument.
     * The invoker may add params afterwards.
     *
     * @param specID spec ID
     * @param code   code uri
     * @param descr  description
     * @return a ModuleImplAdvertisement
     */
    protected static ModuleImplAdvertisement mkImplAdvBuiltin(ModuleSpecID specID, String code, String descr) {
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());
        
        implAdv.setModuleSpecID(specID);
        implAdv.setCompat(STD_COMPAT);
        implAdv.setCode(code);
        implAdv.setUri(MODULE_IMPL_STD_URI);
        implAdv.setProvider(MODULE_IMPL_STD_PROVIDER);
        implAdv.setDescription(descr);
        
        return implAdv;
    }
    
    /**
     * {@inheritDoc}
     */
    // @Override
    public boolean compatible(Element compat) {
        return isCompatible(compat);
    }
    
    /**
     * Evaluates if the given compatibility statement makes the module that
     * bears it is loadable by this group.
     *
     * @param compat compat element
     * @return boolean True if the given statement is compatible.
     */
    static boolean isCompatible(Element compat) {
        boolean formatOk = false;
        boolean bindingOk = false;
        
        try {
            Enumeration hisChildren = compat.getChildren();
            int i = 0;

            while (hisChildren.hasMoreElements()) {
                
                // Stop after 2 elements; there shall not be more.
                if (++i > 2) {
                    return false;
                }
                
                Element e = (Element) hisChildren.nextElement();
                String key = (String) e.getKey();
                String val = (String) e.getValue();

                if (STD_COMPAT_FORMAT.equals(key)) {
                    Package javaLangPackage = Package.getPackage("java.lang");
                    
                    boolean specMatches;
                    String version;
                    
                    if (val.startsWith("JDK") || val.startsWith("JRE")) {
                        specMatches = true;
                        version = val.substring(3).trim(); // allow for spaces.
                    } else if (val.startsWith(javaLangPackage.getSpecificationTitle())) {
                        specMatches = true;
                        version = val.substring(javaLangPackage.getSpecificationTitle().length()).trim(); // allow for spaces.
                    } else {
                        specMatches = false;
                        version = null;
                    }
                    
                    formatOk = specMatches && javaLangPackage.isCompatibleWith(version);
                } else if (STD_COMPAT_BINDING.equals(key) && STD_COMPAT_BINDING_VALUE.equals(val)) {
                    bindingOk = true;
                } else {
                    return false; // Might as well stop right now.
                }
            }
        } catch (Exception any) {
            return false;
        }
        
        return formatOk && bindingOk;
    }
    
    /**
     * Builds a table of modules indexed by their class ID.
     * The values are the loaded modules, the keys are their classId.
     * This routine interprets the parameter list in the advertisement.
     *
     * @param modules    The modules to load
     * @param privileged if true then modules will get a real reference to
     *                   the group loading them, otherwise its an interface object.
     */
    protected void loadAllModules(Map<ModuleClassID, Object> modules, boolean privileged) {
        
        Iterator<Map.Entry<ModuleClassID, Object>> eachModule = modules.entrySet().iterator();
        
        while (eachModule.hasNext()) {
            Map.Entry<ModuleClassID, Object> anEntry = eachModule.next();
            ModuleClassID classID = anEntry.getKey();
            Object value = anEntry.getValue();
            
            // If it is disabled, strip it.
            if (disabledModules.contains(classID)) {
                if (value instanceof ModuleClassID) {
                    if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
                        LOG.config("Module disabled by configuration : " + classID);
                    }
                } else {
                    if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
                        LOG.config("Module disabled by configuration : " + ((ModuleImplAdvertisement) value).getDescription());
                    }
                }
                
                eachModule.remove();
                continue;
            }
            
            // Already loaded.
            if (value instanceof Module) {
                continue;
            }
            
            // Try and load it.
            try {
                Module theModule = null;

                if (value instanceof ModuleImplAdvertisement) {
                    // Load module will republish locally but not in the
                    // parent since that adv does not come from there.
                    theModule = loadModule(classID, (ModuleImplAdvertisement) value, privileged);
                } else if (value instanceof ModuleSpecID) {
                    // loadModule will republish both locally and in the parent
                    // Where the module was fetched.
                    theModule = loadModule(classID, (ModuleSpecID) value, FromParent, privileged);
                } else {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.severe("Skipping: " + classID + " Unsupported module descriptor : " + value.getClass().getName());
                    }
                    eachModule.remove();
                    continue;
                }
                
                if (theModule == null) {
                    throw new PeerGroupException("Could not find a loadable implementation for : " + classID);
                }
                
                anEntry.setValue(theModule);
            } catch (Exception e) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Could not load module for class ID " + classID, e);
                }
                if (value instanceof ModuleClassID) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Will be missing from peer group: " + classID + " (" + e.getMessage() + ").");
                    }
                } else {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING
                                ,
                                "Will be missing from peer group: " + ((ModuleImplAdvertisement) value).getDescription() + " ("
                                + e.getMessage() + ").");
                    }
                }
                eachModule.remove();
            }
        }
    }
    
    /**
     * The group does not care for start args, and does not come-up
     * with args to pass to its main app. So, until we decide on something
     * more useful, the args of the group's startApp are passed-on to the
     * group's main app. NB: both the apps init and startApp methods are
     * invoked.
     *
     * @return int Status.
     */
    @Override
    public int startApp(String[] arg) {
        
        if (!initComplete) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.severe("Group has not been initialized or init failed.");
            }
            return -1;
        }
        
        // FIXME: maybe concurrent callers should be blocked until the
        // end of startApp(). That could mean forever, though.
        if (started) {
            return Module.START_OK;
        }
        
        started = true;
        
        // Normally does nothing, but we have to.
        int res = super.startApp(arg);
        
        if (Module.START_OK != res) {
            return res;
        }
        
        loadAllModules(applications, false); // Apps are non-privileged;
        
        res = 0;
        Iterator<ModuleClassID> appKeys = applications.keySet().iterator();

        while (appKeys.hasNext()) {
            ModuleClassID appKey = appKeys.next();
            Module app = (Module) applications.get(appKey);
            int tmp = app.startApp(arg);

            if (tmp != 0) {
                appKeys.remove();
            } else {
                applications.put(appKey, app);
            }
            res += tmp;
        }
        return res;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stopApp() {
        for (Object o : applications.values()) {
            Module module = null;

            try {
                module = (Module) o;
                module.stopApp();
            } catch (Exception any) {
                if (module != null) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Failed to stop application: " + module.getClass().getName(), any);
                    }
                }
            }
        }
        
        applications.clear();
        // shutdown the threadpool
        threadPool.shutdownNow();
        Collections.reverse(moduleStartOrder);
        
        for (ModuleClassID aModule : moduleStartOrder) {
            try {
                if (messageTransports.containsKey(aModule)) {
                    Module theMessageTransport = (Module) messageTransports.remove(aModule);

                    theMessageTransport.stopApp();
                } else {
                    removeService(aModule);
                }
            } catch (Exception any) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Failed to stop module: " + aModule, any);
                }
            }
        }
        
        moduleStartOrder.clear();
        
        if (!messageTransports.isEmpty()) {
            LOG.warning(messageTransports.size() + " message transports could not be shut down during peer group stop.");
        }
        
        messageTransports.clear();
        
        super.stopApp();
        
        if (cm != null) {
            cm.stop();
            cm = null;
        }
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * This method loads and initializes all modules
     * described in the given implementation advertisement. Then, all modules
     * are placed in a list and the list is processed iteratively. During each
     * iteration, the {@link Module#startApp(String[])} method of each module
     * is invoked once. Iterations continue until no progress is being made or
     * the list is empty.
     * <p/>
     * The status returned by the {@link Module#startApp(String[])} method
     * of each module is considered as follows:
     * <p/>
     * <ul>
     * <li>{@link Module#START_OK}: The module is removed from the list of
     * modules to be started and its {@link Module#startApp(String[])}
     * method will not be invoked again.
     * </li>
     * <p/>
     * <li>{@link Module#START_AGAIN_PROGRESS}: The module remains in the
     * list of modules to be started and its {@link Module#startApp(String[])}
     * method will be invoked during the next iteration, if there is one. </li>
     * <p/>
     * <li>{@link Module#START_AGAIN_STALLED}: The module remains in the list
     * of modules to be started and its {@link Module#startApp(String[])}
     * method will be invoked during the next iteration if there is one. </li>
     * <p/>
     * <li>Any other value: The module failed to initialize. Its
     * {@link Module#startApp(String[])} method will not be invoked again.</li>
     * </ul>
     * <p/>
     * Iterations through the list stop when:
     * <ul>
     * <li>The list is empty: the group initialization proceeds.</li>
     * <p/>
     * <li>A complete iteration was performed and all modules returned
     * {@link Module#START_AGAIN_STALLED}: a {@link PeerGroupException}
     * is thrown.</li>
     * <p/>
     * <li>A number of complete iteration completed without any module
     * returning {@link Module#START_OK}: a {@link PeerGroupException}
     * is thrown. The number of complete iterations before that happens is
     * computed as 1 + the square of the number of modules currently in the
     * list.</li>
     * </ul>
     */
    @Override
    protected synchronized void initFirst(PeerGroup parent, ID assignedID, Advertisement impl) throws PeerGroupException {
        
        if (initComplete) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("You cannot initialize a PeerGroup more than once !");
            }
            return;
        }

        // Set-up the minimal GenericPeerGroup
        super.initFirst(parent, assignedID, impl);
        
        // initialize cm before starting services. Do not refer to assignedID, as it could be
        // null, in which case the group ID has been generated automatically by super.initFirst()
        try {
            cm = new Cm(getHomeThreadGroup(), getStoreHome(), getPeerGroupID().getUniqueValue().toString()
                    ,
                    Cm.DEFAULT_GC_MAX_INTERVAL, false);
        } catch (Exception e) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Error during creation of local store", e);
            }
            throw new PeerGroupException("Error during creation of local store", e);
        }
        
        // flush srdi for this group
        SrdiIndex.clearSrdi(this);
        
        /*
         * Build the list of modules disabled by config.
         */
        ConfigParams conf = getConfigAdvertisement();

        if (conf != null) {
            for (Map.Entry anEntry : conf.getServiceParamsEntrySet()) {
                XMLElement e = (XMLElement) anEntry.getValue();

                if (e.getChildren("isOff").hasMoreElements()) {
                    disabledModules.add((ModuleClassID) anEntry.getKey());
                }
            }
        }
        
        ModuleImplAdvertisement implAdv = (ModuleImplAdvertisement) impl;
        
        /*
         * Load all the modules from the advertisement
         */
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv(implAdv.getParam());
        
        Map<ModuleClassID, Object> initServices = new HashMap<ModuleClassID, Object>(paramAdv.getServices());
        
        initServices.putAll(paramAdv.getProtos());
        
        loadAllModules(initServices, true);
        
        // Applications are shelved until startApp()
        applications.putAll(paramAdv.getApps());
        
        // Make a list of all the things we need to start.
        // There is an a-priori order, but we'll iterate over the
        // list until all where able to complete their start phase
        // or no progress is made. Since we give to modules the opportunity
        // to pretend that they are making progress, we need to have a
        // safeguard: we will not iterate through the list more than N^2 + 1
        // times without at least one module completing; N being the number
        // of modules still in the list. That should cover the worst case
        // scenario and still allow the process to eventually fail if it has
        // no chance of success.
        
        int iterations = 0;
        int maxIterations = initServices.size() * initServices.size() + iterations + 1;
        
        boolean progress = true;
        
        while (!initServices.isEmpty() && (progress || (iterations < maxIterations))) {
            progress = false;
            iterations++;
            
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("Service startApp() round {0} of {1}(max)", iterations, maxIterations));
            }
            
            Iterator<Map.Entry<ModuleClassID, Object>> eachService = initServices.entrySet().iterator();
            
            while (eachService.hasNext()) {
                Map.Entry<ModuleClassID, Object> anEntry = eachService.next();
                ModuleClassID mcid = anEntry.getKey();
                Module aModule = (Module) anEntry.getValue();
                
                int res;

                try {
                    res = aModule.startApp(null);
                } catch (Throwable all) {
                    if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, "Exception in startApp() : " + aModule, all);
                    }
                    res = -1;
                }
                
                switch (res) {
                case Module.START_OK:
                    // One done. Remove from allStart and recompute maxIteration.
                        
                    if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                        LOG.info("Module started : " + aModule);
                    }
                    if (aModule instanceof Service) {
                        addService(mcid, (Service) aModule);
                    } else {
                        messageTransports.put(mcid, aModule);
                    }
                        
                    moduleStartOrder.add(mcid);
                    eachService.remove();
                    progress = true;
                    break;
                        
                case Module.START_AGAIN_PROGRESS:
                    progress = true;
                    break;
                        
                case Module.START_AGAIN_STALLED:
                    break;
                        
                default: // (negative)
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.warning("Service failed to start (" + res + ") : " + aModule);
                    }
                    eachService.remove();
                    progress = true;
                    break;
                }
            }
            
            if (progress) {
                maxIterations = initServices.size() * initServices.size() + iterations + 1;
            }
        }
        
        // Uh-oh. Services co-dependency prevented them from starting.
        if (!initServices.isEmpty()) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                StringBuilder failed = new StringBuilder(
                        "No progress is being made in starting services after " + iterations + " iterations. Giving up.");
                
                failed.append("\nThe following services could not be started : ");
                
                for (Map.Entry<ModuleClassID, Object> aService : initServices.entrySet()) {
                    failed.append("\n\t");
                    failed.append(aService.getKey());
                    failed.append(" : ");
                    failed.append(aService.getValue());
                }
                
                LOG.severe(failed.toString());
            }
            
            throw new PeerGroupException("No progress is being made in starting services. Giving up.");
        }
        
        // Make sure all the required services are loaded.
        try {
            checkServices();
        } catch (ServiceNotFoundException e) {
            LOG.log(Level.SEVERE, "Missing peer group service", e);
            throw new PeerGroupException("Missing peer group service", e);
        }
        
        /*
         * Publish a few things that have not been published in this
         * group yet.
         */
        DiscoveryService discoveryService = getDiscoveryService();

        if (discoveryService != null) {
            // It should work but if it does not we can survive.
            try {
                // Discovery service adv could not be published localy,
                // since at that time there was no local discovery to
                // publish to. FIXME: this is really a cherry on the cake.
                // no-one realy cares
                discoveryService.publish(discoveryService.getImplAdvertisement(), DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
                
                // Try to publish our impl adv within this group. (it was published
                // in the parent automatically when loaded.
                discoveryService.publish(implAdv, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);
            } catch (Exception nevermind) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Failed to publish Impl adv within group.", nevermind);
                }
            }
        }        
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * Nothing special for now, but we might want to move some steps from 
     * initFirst() in the future.
     */
    @Override
    protected synchronized void initLast() throws PeerGroupException {

        super.initLast();
        
        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            StringBuilder configInfo = new StringBuilder("Configuring Group : " + getPeerGroupID());
            
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tCompatibility Statement :\n\t\t\t");
            StringBuilder indent = new StringBuilder(STD_COMPAT.toString().trim());
            int from = indent.length();
            
            while (from > 0) {
                int returnAt = indent.lastIndexOf("\n", from);
                
                from = returnAt - 1;
                if ((returnAt >= 0) && (returnAt != indent.length())) {
                    indent.insert(returnAt + 1, "\t\t\t");
                }
            }
            configInfo.append(indent);
            Iterator eachProto = messageTransports.entrySet().iterator();
            
            if (eachProto.hasNext()) {
                configInfo.append("\n\t\tMessage Transports :");
            }
            while (eachProto.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachProto.next();
                ModuleClassID aMCID = (ModuleClassID) anEntry.getKey();
                Module anMT = (Module) anEntry.getValue();
                
                configInfo.append("\n\t\t\t").append(aMCID).append("\t").append(
                        (anMT instanceof MessageTransport)
                                ? ((MessageTransport) anMT).getProtocolName()
                                : anMT.getClass().getName());
            }
            Iterator eachApp = applications.entrySet().iterator();
            
            if (eachApp.hasNext()) {
                configInfo.append("\n\t\tApplications :");
            }
            while (eachApp.hasNext()) {
                Map.Entry anEntry = (Map.Entry) eachApp.next();
                ModuleClassID aMCID = (ModuleClassID) anEntry.getKey();
                Object anApp = anEntry.getValue();
                
                if (anApp instanceof ModuleImplAdvertisement) {
                    ModuleImplAdvertisement adv = (ModuleImplAdvertisement) anApp;
                    
                    configInfo.append("\n\t\t\t").append(aMCID).append("\t").append(adv.getCode());
                } else {
                    configInfo.append("\n\t\t\t").append(aMCID).append("\t").append(anApp.getClass().getName());
                }
            }
            LOG.config(configInfo.toString());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    // @Override
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
        
        // Build it only the first time; then clone it.
        if (allPurposeImplAdv != null) {
            return allPurposeImplAdv.clone();
        }
        
        // grab an impl adv
        ModuleImplAdvertisement implAdv = mkImplAdvBuiltin(PeerGroup.allPurposePeerGroupSpecID, StdPeerGroup.class.getName()
                ,
                "General Purpose Peer Group Implementation");
        
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();
        ModuleImplAdvertisement moduleAdv;
        
        // set the services
        Hashtable<ModuleClassID, Object> services = new Hashtable<ModuleClassID, Object>();
        
        // core services
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refEndpointSpecID, "net.jxta.impl.endpoint.EndpointServiceImpl"
                ,
                "Reference Implementation of the Endpoint service");
        services.put(PeerGroup.endpointClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refResolverSpecID, "net.jxta.impl.resolver.ResolverServiceImpl"
                ,
                "Reference Implementation of the Resolver service");
        services.put(PeerGroup.resolverClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refMembershipSpecID, "net.jxta.impl.membership.none.NoneMembershipService"
                ,
                "Reference Implementation of the None Membership service");
        services.put(PeerGroup.membershipClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(refAccessSpecID, "net.jxta.impl.access.always.AlwaysAccessService"
                ,
                "Reference Implementation of the Always Access service");
        services.put(PeerGroup.accessClassID, moduleAdv);
        
        // standard services
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refDiscoverySpecID, "net.jxta.impl.discovery.DiscoveryServiceImpl"
                ,
                "Reference Implementation of the Discovery service");
        services.put(PeerGroup.discoveryClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refRendezvousSpecID, "net.jxta.impl.rendezvous.RendezVousServiceImpl"
                ,
                "Reference Implementation of the Rendezvous service");
        services.put(PeerGroup.rendezvousClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refPipeSpecID, "net.jxta.impl.pipe.PipeServiceImpl"
                ,
                "Reference Implementation of the Pipe service");
        services.put(PeerGroup.pipeClassID, moduleAdv);
        
        moduleAdv = mkImplAdvBuiltin(PeerGroup.refPeerinfoSpecID, "net.jxta.impl.peer.PeerInfoServiceImpl"
                ,
                "Reference Implementation of the Peerinfo service");
        services.put(PeerGroup.peerinfoClassID, moduleAdv);
        
        paramAdv.setServices(services);
        
        // NO Transports.
        Hashtable<ModuleClassID, Object> protos = new Hashtable<ModuleClassID, Object>();

        paramAdv.setProtos(protos);
        
        // Main app is the shell Build a ModuleImplAdv for the shell
        ModuleImplAdvertisement newAppAdv = (ModuleImplAdvertisement)
                AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());
        
        // The shell's spec id is a canned one.
        newAppAdv.setModuleSpecID(PeerGroup.refShellSpecID);
        
        // Same compat than the group.
        newAppAdv.setCompat(implAdv.getCompat());
        newAppAdv.setUri(implAdv.getUri());
        newAppAdv.setProvider(implAdv.getProvider());
        
        // Make up a description
        newAppAdv.setDescription("JXTA Shell Reference Implementation");
        
        // Tack in the class name
        newAppAdv.setCode("net.jxta.impl.shell.bin.Shell.Shell");
        
        // Put that in a new table of Apps and replace the entry in paramAdv
        Hashtable<ModuleClassID, Object> newApps = new Hashtable<ModuleClassID, Object>();

        newApps.put(PeerGroup.applicationClassID, newAppAdv);
        paramAdv.setApps(newApps);
        
        // Insert the newParamAdv in implAdv
        XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
        
        implAdv.setParam(paramElement);
        
        allPurposeImplAdv = implAdv;
        
        return implAdv.clone();
    }
    
    /**
     * Returns the cache manager associated with this group.
     *
     * @return the cache manager associated with this group.
     */
    public Cm getCacheManager() {
        return cm;
    }
    
    /**
     * Return a map of the applications for this group.
     * <p/>
     * <ul>
     * <li>keys are {@link net.jxta.platform.ModuleClassID}</li>
     * <li>values are {@link net.jxta.platform.Module} or
     * {@link net.jxta.protocol.ModuleImplAdvertisement}</li>
     * </ul>
     * @return a map of the applications for this group.
     */
    public Map<ModuleClassID, Object> getApplications() {
        return Collections.unmodifiableMap(applications);
    }
    
    /**
     * Returns the executor pool
     *
     * @return the executor pool
     */
    public ThreadPoolExecutor getExecutor() {
        return threadPool;
    }
}
