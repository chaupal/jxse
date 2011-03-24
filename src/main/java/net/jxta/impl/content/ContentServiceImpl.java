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

import net.jxta.content.*;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.logging.Logging;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ContentShareAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reference implementation of the ContentService.  This implementation
 * manages the listener list, tracks active shares, and uses the Jar
 * service provider interface to locate transfer provider implementations
 * which will perform the real work.
 */
public class ContentServiceImpl implements ContentService {

    /**
     * Well known service spec identifier: reference implementation of the
     * ContentService.
     */
    public final static ModuleSpecID MODULE_SPEC_ID =
            ModuleSpecID.create(URI.create(
            "urn:jxta:uuid-DDC5CA55578E4AB99A0AA81D2DC6EF3F"
            + "3F7E9F18B5D84DD58D21CE9E37E19E6C06"));

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(
            ContentServiceImpl.class.getName());
    
    /**
     * List of all currently registered providers, used only for providing
     * programmatic access to the API user (hence the use of the
     * ContentProvider super-interface versus the SPI interface).
     */
    private final List<ContentProvider> providers = 
            new CopyOnWriteArrayList<ContentProvider>();
    
    /**
     * List of providers which are started and ready for use.
     */
    private final List<ContentProviderSPI> active = 
            new CopyOnWriteArrayList<ContentProviderSPI>();
    
    /**
     * List of our listeners.
     */
    private final List<ContentProviderListener> listeners =
            new CopyOnWriteArrayList<ContentProviderListener>();
    
    /**
     * Lifecycle manager responsible for getting provider instances
     * into the correct operational state.
     */
    private final ModuleLifecycleManager<ContentProviderSPI> manager =
            new ModuleLifecycleManager<ContentProviderSPI>();
    
    /**
     * List of providers which are registered and waiting for the
     * service to be initialized before being added to the lifecycle
     * manager.  After initialization, this list is nulled.
     */
    private List<ContentProviderSPI> waitingForInit = locateProviders();
    
    /**
     * Implementation adv given to us via init().
     */
    private ModuleImplAdvertisement implAdv = null;

    /**
     * Peer group given to us via init().
     */
    private PeerGroup group;
    
    /**
     * Object to lock against when accessing member vars.
     */
    private final Object lock = new Object();
    
    /**
     * Flag indicatin that this instancce has been initialized.
     */
    private boolean initialized = false;
    
    /**
     * Flag indicating that this instance has been started.
     */
    private volatile boolean started = false;

    /**
     * Default constructor.
     */
    public ContentServiceImpl() {
        // Track available providers indirectly via the lifecycle manager
        manager.addModuleLifecycleListener(
                new ModuleLifecycleListener() {

            /**
             * {@inheritDoc}
             */
            public void unhandledPeerGroupException(ModuleLifecycleTracker subject, PeerGroupException mlcx) {

                Logging.logCheckedWarning(LOG, "Uncaught exception", mlcx);
                
            }

            /**
             * {@inheritDoc}
             */
            public void moduleLifecycleStateUpdated(
                    ModuleLifecycleTracker subject,
                    ModuleLifecycleState newState) {
                ContentProviderSPI provider =
                        (ContentProviderSPI) subject.getModule();
                LOG.fine("Content provider lifecycle state update: "
                        + provider + " --> " + newState);
                if (newState == ModuleLifecycleState.STARTED) {
                    active.add(provider);
                } else {
                    active.remove(provider);
                }
            }
            
        });
    }

    //////////////////////////////////////////////////////////////////////////
    // Module interface methods:

    /**
     * {@inheritDoc}
     */
    public void init(
            PeerGroup group, ID assignedID, Advertisement adv) {
        List<ContentProviderSPI> toAdd;
        synchronized(lock) {
            if (initialized) {
                return;
            }
            initialized = true;
            this.group = group;
            this.implAdv = (ModuleImplAdvertisement) adv;
            toAdd = waitingForInit;
            waitingForInit = null;
        }


        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {

            StringBuilder configInfo = new StringBuilder();

            configInfo.append("Configuring Content Service : ").append(assignedID);

            configInfo.append( "\n\tImplementation :" );

            if (implAdv != null) {
                configInfo.append("\n\t\tModule Spec ID: ").append(implAdv.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : ").append(implAdv.getDescription());
                configInfo.append("\n\t\tImpl URI : ").append(implAdv.getUri());
                configInfo.append("\n\t\tImpl Code : ").append(implAdv.getCode());
            }

            configInfo.append( "\n\tGroup Params :" );
            configInfo.append("\n\t\tGroup : ").append(group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : ").append(group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : ").append(group.getPeerID());

            configInfo.append( "\n\tProviders: ");

            for (ContentProviderSPI provider : toAdd) {
                configInfo.append("\n\t\tProvider: ").append(provider);
            }
            
            LOG.config( configInfo.toString() );

        }
        
        // Provider initialization
        for (ContentProviderSPI provider : toAdd) {
            addContentProvider(provider);
        }

        manager.init();
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String args[]) {
        synchronized(lock) {
            if (started) {
                return START_OK;
            }
            started = true;
        }


        return START_OK;
    }

    /**
     * {@inheritDoc}
     */
    public void stopApp() {
        synchronized(lock) {
            if (!started) {
                return;
            }
            started = false;
        }

        manager.stop();


    }

    //////////////////////////////////////////////////////////////////////////
    // Service interface methods:

    /**
     * {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        synchronized(lock) {
            return implAdv;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentService getInterface() {
        return  (ContentService) ModuleWrapperFactory.newWrapper(
                new Class[] { ContentService.class },
                this);
    }

    //////////////////////////////////////////////////////////////////////////
    // ContentService interface methods:

    /**
     * {@inheritDoc}
     */
    public void addContentProvider(ContentProviderSPI provider) {
        boolean addToManager = false;
        providers.add(provider);
        synchronized(lock) {
            if (initialized) {
                // Add to manager and let the manager event add to list
                addToManager = true;
            } else {
                // Add to pending list
                waitingForInit.add(provider);
            }
        }
        if (addToManager) {
            // We try to be as correct and complete as possible here...
            Advertisement adv = provider.getImplAdvertisement();
            ID asgnID;
            if (adv instanceof ModuleSpecAdvertisement) {
                ModuleSpecAdvertisement specAdv =
                        (ModuleSpecAdvertisement) adv;
                asgnID = specAdv.getModuleSpecID();
            } else if (adv instanceof ModuleImplAdvertisement) {
                ModuleImplAdvertisement mimpAdv =
                        (ModuleImplAdvertisement) adv;
                asgnID = mimpAdv.getModuleSpecID();
            } else {
                asgnID = adv.getID();
            }
            manager.addModule(provider, group, asgnID, adv, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentProvider(ContentProvider provider) {

        if (!(provider instanceof ContentProviderSPI)) {

            /*
             * Can't cast so we can't use.  Note that the add/remove
             * asymmetry is intentional since getContentProviders()
             * returns the ContentProvider sub-interface to prevent
             * user access to SPI methods.
             */


            return;

        }

        providers.remove(provider);
        ContentProviderSPI spi = (ContentProviderSPI) provider;
        boolean removeFromManager = false;
        synchronized(lock) {
            if (initialized) {
                // List is maintained via manager
                removeFromManager = true;
            } else {
                // Remove from pending list
                waitingForInit.remove(provider);
            }
        }
        if (removeFromManager) {
            manager.removeModule(spi, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ContentProvider> getContentProviders() {
        return Collections.unmodifiableList(providers);
    }

    /**
     * {@inheritDoc}
     */
    public List<ContentProvider> getActiveContentProviders() {
        checkStart();
        /*
         * NOTE mcumings 20061120:  Note that this could also be implemented
         * using Collections.unmodifiableList(), but having the returned list
         * run the potential of effectively changing over time (it would be
         * read-through) led me to select a full copy instead.
         */
        List<ContentProvider> result =
                new ArrayList<ContentProvider>(active);
        return result;
    }

    //////////////////////////////////////////////////////////////////////////
    // ContentProvider interface methods:

    /**
     * {@inheritDoc}
     */
    public void addContentProviderListener(
            ContentProviderListener listener) {
        checkStart();
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeContentProviderListener(
            ContentProviderListener listener) {
        checkStart();
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public ContentTransfer retrieveContent(ContentID contentID) {

        checkStart();
        
        try {
            return new TransferAggregator(this, active, contentID);
        } catch (TransferException transx) {


            return null;
        }

    }

    /**
     * {@inheritDoc}
     */
    public ContentTransfer retrieveContent(ContentShareAdvertisement adv) {
        
        checkStart();

        try {
            return new TransferAggregator(this, active, adv);
        } catch (TransferException transx) {


            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ContentShare> shareContent(Content content) {
        checkStart();
        
        List<ContentShare> result = null;
        List<ContentShare> subShares;
        for (ContentProvider provider : active) {

            try {

                subShares = provider.shareContent(content);

                if (subShares == null) continue;


                if (result == null) result = new ArrayList<ContentShare>();

                result.addAll(subShares);

            } catch (UnsupportedOperationException uox) {


            }
            
        }

        if (result != null) {
            fireContentShared(result);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean unshareContent(ContentID contentID) {
        checkStart();
        
        boolean unshared = false;
        for (ContentProvider provider : active) {
            unshared |= provider.unshareContent(contentID);
        }

        if (unshared) {
            fireContentUnshared(contentID);
        }
        return unshared;
    }

    /**
     * {@inheritDoc}
     */
    public void findContentShares(
            int maxNum, ContentProviderListener listener) {
        checkStart();
        
        List<ContentProviderListener> findListeners =
                new ArrayList<ContentProviderListener>();
        findListeners.add(listener);

        EventAggregator aggregator =
                new EventAggregator(findListeners, active);
        aggregator.dispatchFindRequest(maxNum);
    }


    //////////////////////////////////////////////////////////////////////////
    // Private methods:
    
    /**
     * Check to see if the ContentService has been started.  If so, make
     * sure that the provider implementations are started.  If not, throw
     * an illegal state exception.  This method should be used in places
     * where providers are about to be used, allowing their intialization to
     * be deferred until the time of use.
     */
    private void checkStart() {
        synchronized(lock) {
            if (!started) {
                throw(new IllegalStateException(
                        "Service instance has not yet been started"));
            }
        }
        manager.start();
    }

    /**
     * Notify listeners of a new Content share.
     *
     * @param shares list of shares for the Content
     */
    private void fireContentShared(List<ContentShare> shares) {
        ContentProviderEvent event = null;

        for (ContentProviderListener listener : listeners) {
            try {

                if (event == null) {
                    event = new ContentProviderEvent.Builder(this, shares)
                            .build();
                }

                listener.contentShared(event);

            } catch (Throwable thr) {

                Logging.logCheckedWarning(LOG, "Uncaught throwable from listener\n", thr);
                
            }
        }
    }

    /**
     * Notify listeners of a new Content share.
     *
     * @param id Content ID
     */
    private void fireContentUnshared(ContentID id) {
        ContentProviderEvent event = null;

        for (ContentProviderListener listener : listeners) {
            try {

                if (event == null) {
                    event = new ContentProviderEvent.Builder(this, id)
                            .build();
                }

                listener.contentUnshared(event);

            } catch (Throwable thr) {

                Logging.logCheckedWarning(LOG, "Uncaught throwable from listener\n", thr);
                
            }
        }
    }

    /**
     * Locate all implementations of the ContentServiceProviderSPI interface
     * using the Jar service provider interface mechanism.
     *
     * @return list of content provider implementations
     */
    private List<ContentProviderSPI> locateProviders() {
        
        ContentProviderSPI provider;

        List<ContentProviderSPI> result = new CopyOnWriteArrayList<ContentProviderSPI>();

        ClassLoader loader = getClass().getClassLoader();


        Enumeration resources;
        try {
            resources = loader.getResources(
                    "META-INF/services/" + ContentProviderSPI.class.getName());
        
        } catch (IOException iox) {

            Logging.logCheckedWarning(LOG, "Unable to enumerate ContentProviders\n", iox);
            
            // Early-out.
            return result;

        }
        
        // Create a Set of all unique class names
        Set<String> provClassNames = new HashSet<String>();
        while (resources.hasMoreElements()) {

            URL resURL = (URL) resources.nextElement();


            try {

                InputStreamReader inReader = new InputStreamReader(resURL.openStream());
                BufferedReader reader = new BufferedReader(inReader);
                String str;

                while ((str = reader.readLine()) != null) {
                    int idx = str.indexOf('#');
                    if (idx >= 0) {
                        str = str.substring(0, idx);
                    }
                    str = str.trim();
                    if (str.length() == 0) {
                        // Probably a commented line
                        continue;
                    }

                    provClassNames.add(str);

                }

            } catch (IOException iox) {

                Logging.logCheckedWarning(LOG, "Could not parse ContentProvider services from: ",
                    resURL, iox);

            }
        }
         
        // Now attempt to instantiate all the providers we've found
        for (String str : provClassNames) {

            try {

                Class cl = loader.loadClass(str);
                provider = (ContentProviderSPI) cl.newInstance();
                result.add(provider);


            } catch (ClassNotFoundException cnfx) {

                Logging.logCheckedSevere(LOG, "Could not load service provider\n", cnfx);
                // Continue to next provider class name

            } catch (InstantiationException instx) {

                Logging.logCheckedSevere(LOG, "Could not load service provider\n", instx);
                // Continue to next provider class name

            } catch (IllegalAccessException iaccx) {

                Logging.logCheckedSevere(LOG, "Could not load service provider\n", iaccx);
                // Continue to next provider class name

            }
        }            

        return result;
    }
}
