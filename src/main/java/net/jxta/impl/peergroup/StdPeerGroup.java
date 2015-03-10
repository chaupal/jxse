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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.exception.PeerGroupAuthenticationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.impl.access.pse.PSEAccessService;
import net.jxta.impl.cm.CacheManager;
import net.jxta.impl.cm.Srdi;
import net.jxta.impl.content.ContentServiceImpl;
import net.jxta.impl.membership.pse.DialogAuthenticator;
import net.jxta.impl.membership.pse.EngineAuthenticator;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.IJxtaLoader;
import net.jxta.platform.JxtaApplication;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.service.Service;

/**
 * A subclass of GenericPeerGroup that makes a peer group out of independent
 * plugin services listed in its impl advertisement.
 */
public class StdPeerGroup extends GenericPeerGroup {

    private final static transient Logger LOG = Logging.getLogger(StdPeerGroup.class.getName());

//    /**
//     * This field is for backwards compatibility with broken code and will
//     * be removed in the near future.  The correct way to obtain a compatibility
//     * statement is to obtain it from a peer group's implementation
//     * advertisement.
//     *
//     * @deprecated will be removed in 2.8
//     */
//    @Deprecated
//    public static final XMLDocument STD_COMPAT = CompatibilityUtils.createDefaultCompatStatement();

//    /**
//     * This field is for backwards compatibility with broken code and will
//     * be removed in the near future.  The correct way to obtain this
//     * information is to obtain it from a peer group's implementation
//     * advertisement.
//     *
//     * @deprecated will be removed in 2.8
//     */
//    @Deprecated
//    public static final String MODULE_IMPL_STD_URI =
//            CompatibilityUtils.getDefaultPackageURI();

//    /**
//     * This field is for backwards compatibility with broken code and will
//     * be removed in the near future.  The correct way to obtain this
//     * information is to obtain it from a peer group's implementation
//     * advertisement.
//     * 
//     * @deprecated will be removed in 2.8
//     */
//    @Deprecated
//    public static final String MODULE_IMPL_STD_PROVIDER =
//            CompatibilityUtils.getDefaultProvider();

    /**
     * Static initializer.
     */
    static {
        // XXX Force redefinition of StdPeerGroup moduleImplementationAdvertisement.
        getJxtaLoader().defineClass(getDefaultModuleImplAdvertisement());
    }

    /**
     * If {@code true} then the PeerGroup has been started.
     */
    private volatile boolean started = false;

    /**
     * The order in which we started the services.
     */
    private final List<ModuleClassID> moduleStartOrder = new ArrayList<>();

    /**
     * A map of the Message Transports for this group.
     * <p/>
     * <ul>
     * <li>keys are {@link net.jxta.platform.ModuleClassID}</li>
     * <li>values are {@link net.jxta.platform.Module}, but should also be
     * {@link net.jxta.endpoint.MessageTransport}</li>
     * </ul>
     */
    private final Map<ModuleClassID, Object> messageTransports = new HashMap<>();

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
    private final Map<ModuleClassID, Object> applications = new HashMap<>();

    /**
     * Cache for this group.
     */
    private CacheManager cacheManager = null;

    /**
     *  Create and populate the default module impl Advertisement for this class.
     *
     *  @return The default module impl advertisement for this class.
     */
    public static ModuleImplAdvertisement getDefaultModuleImplAdvertisement() {
        ModuleImplAdvertisement implAdv = CompatibilityUtils.createModuleImplAdvertisement(IModuleDefinitions.allPurposePeerGroupSpecID, StdPeerGroup.class.getName(), "General Purpose Peer Group Implementation");

        // Create the service list for the group.
        StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

        // Set core services        
        IJxtaLoader loader = getJxtaLoader();

        paramAdv.addService(IModuleDefinitions.endpointClassID, IModuleDefinitions.refEndpointSpecID);
        paramAdv.addService(IModuleDefinitions.resolverClassID, IModuleDefinitions.refResolverSpecID);
        paramAdv.addService(IModuleDefinitions.membershipClassID, PSEMembershipService.pseMembershipSpecID);
        paramAdv.addService(IModuleDefinitions.accessClassID, PSEAccessService.PSE_ACCESS_SPEC_ID);

        // Set standard services
        paramAdv.addService(IModuleDefinitions.discoveryClassID, IModuleDefinitions.refDiscoverySpecID);
        paramAdv.addService(IModuleDefinitions.rendezvousClassID, IModuleDefinitions.refRendezvousSpecID);
        paramAdv.addService(IModuleDefinitions.pipeClassID, IModuleDefinitions.refPipeSpecID);
        paramAdv.addService(IModuleDefinitions.peerinfoClassID, IModuleDefinitions.refPeerinfoSpecID);
        paramAdv.addService(IModuleDefinitions.contentClassID, ContentServiceImpl.MODULE_SPEC_ID);

//        // Applications
//        ModuleImplAdvertisement moduleAdv = loader.findModuleImplAdvertisement(PeerGroup.refShellSpecID);
//        if (null != moduleAdv) {
//            peerGroupParametersAdvertisement.addApp(PeerGroup.applicationClassID, PeerGroup.refShellSpecID);
//        }

        // Insert the newParamAdv in implAdv
        XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);

        implAdv.setParam(paramElement);

        return implAdv;
    }

    /**
     * constructor
     */
    public StdPeerGroup() {
        // Empty
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    @Override
    public boolean compatible(Element compat) {
        return CompatibilityUtils.isCompatible(compat);
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

            // Already loaded.
            if (value instanceof Module) {
                continue;
            }

            // Try and load it.
            try {
                Module module = null;

                if (value instanceof ModuleImplAdvertisement) {
                    // Load module will republish locally but not in the
                    // parent since that adv does not come from there.
                    module = loadModule(classID, (ModuleImplAdvertisement) value, privileged);

                } else if (value instanceof ModuleSpecID) {

                    // loadModule will republish both locally and in the parent
                    // Where the module was fetched.
                    module = loadModule(classID, (ModuleSpecID) value, FromParent, privileged);

                } else {
                    Logging.logCheckedError(LOG, "Skipping: ", classID, " Unsupported module descriptor : ", value.getClass().getName());
                    eachModule.remove();
                    continue;
                }

                if (module == null) {
                    throw new PeerGroupException("Could not find a loadable implementation for : " + classID);
                }
                
                anEntry.setValue(module);

            } catch (ProtocolNotSupportedException | PeerGroupException e) {

                Logging.logCheckedWarning(LOG, "Could not load module for class ID : ", classID, "\n", e);

                if (value instanceof ModuleImplAdvertisement) {
                    Logging.logCheckedWarning(LOG, "Will be missing from peer group : " + ((ModuleImplAdvertisement) value).getDescription());
                } else {
                    Logging.logCheckedWarning(LOG, "Will be missing from peer group: ", value);
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
     * @param arg
     * @return int Status.
     */
    @Override
    public int startApp(String[] arg) {

        if (!initComplete) {

            Logging.logCheckedError(LOG, "Group has not been initialized or init failed.");
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

        res = startModules((Map) applications);

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopApp() {
        // Shut down the group services and message transports.
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

                Logging.logCheckedWarning(LOG, "Failed to stop module: ", aModule, "\n", any);

            }
        }

        moduleStartOrder.clear();

        if (!messageTransports.isEmpty()) {
            LOG.warn(messageTransports.size() + " message transports could not be shut down during peer group stop.");
        }

        messageTransports.clear();

        super.stopApp();

        if (cacheManager != null) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    /**
     * Given a list of all the modules we need to start attempt to start them.
     * There is an a-priori order, but we'll iterate over the list until all
     * where able to complete their start phase or no progress is made. Since we
     * give modules the opportunity to pretend that they are making progress, we
     * need to have a safeguard: we will not iterate through the list more than
     * N^2 + 1 times without at least one module completing;  N being the number
     * of modules still in the list. This should cover the worst case scenario
     * and still allow the process to eventually fail if it has no chance of
     * success.
     *
     * @param services The services to start.
     */
    private int startModules(Map<ModuleClassID,Module> services) {
        int iterations = 0;
        int maxIterations = services.size() * services.size() + iterations + 1;

        boolean progress = true;

        while (!services.isEmpty() && (progress || (iterations < maxIterations))) {

            progress = false;
            iterations++;

            Logging.logCheckedDebug(LOG, MessageFormat.format("Service startApp() round {0} of {1}(max)", iterations, maxIterations));

            Iterator<Map.Entry<ModuleClassID, Module>> eachService = services.entrySet().iterator();

            while (eachService.hasNext()) {
                Map.Entry<ModuleClassID, Module> anEntry = eachService.next();
                ModuleClassID mcid = anEntry.getKey();
                Module aModule = anEntry.getValue();

                int res;

                try {

                    res = aModule.startApp(null);

                } catch (Throwable all) {

                    Logging.logCheckedWarning(LOG, "Exception in startApp() : ", aModule, "\n", all);
                    res = -1;

                }

                switch (res) {

                    case Module.START_OK:

                        Logging.logCheckedDebug(LOG, "Module started : ", aModule);

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

                    	// LOGGING: was Finer
                        Logging.logCheckedDebug(LOG, "Service made progress during start : ", aModule);
                        progress = true;
                        break;

                    case Module.START_AGAIN_STALLED:

                    	// LOGGING: was Finer
                        Logging.logCheckedDebug(LOG, "Service stalled during start : ", aModule);
                        break;

                    case Module.START_DISABLED:

                        Logging.logCheckedDebug(LOG, "Service declined to start : ", aModule);
                        eachService.remove();
                        progress = true;
                        break;

                    default: // (negative)

                        Logging.logCheckedWarning(LOG, "Service failed to start (", res, ") : ", aModule);
                        eachService.remove();
                        progress = true;
                        break;

                }
            }

            if (progress) {
                maxIterations = services.size() * services.size() + iterations + 1;
            }
        }

        // Services co-dependency prevented them from starting.
        if (!services.isEmpty()) {

            if (Logging.SHOW_ERROR && LOG.isErrorEnabled()) {
                StringBuilder failed = new StringBuilder( "No progress is being made in starting services after "
                        + iterations + " iterations. Giving up.");

                failed.append("\nThe following services could not be started : ");

                for (Map.Entry<ModuleClassID, Module> aService : services.entrySet()) {
                    failed.append("\n\t");
                    failed.append(aService.getKey());
                    failed.append(" : ");
                    failed.append(aService.getValue());
                }

                LOG.error(failed.toString());
            }

            return -1;
        }

        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method loads and initializes all of the peer group modules
     * described in the provided implementation advertisement. Then, all modules
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
    protected synchronized void initFirst(PeerGroup parent, ID moduleClassId, Advertisement impl) throws PeerGroupException {

        if (initComplete) {
            Logging.logCheckedWarning(LOG, "You cannot initialize a PeerGroup more than once !");
            return;
        }

        // Set-up the minimal GenericPeerGroup
        super.initFirst(parent, moduleClassId, impl);                

        // moduleClassId might have been null. It is now the peer group ID.
        moduleClassId = getPeerGroupID();

        // Initialize cacheManager before starting services.
        try {
            cacheManager = new CacheManager(getStoreHome(), moduleClassId.getUniqueValue().toString(), getTaskManager(), 0L, false);
        } catch (IOException e) {            
            Logging.logCheckedError(LOG, "Failure instantiating local store\n", e);
            throw new PeerGroupException("Failure instantiating local store", e);
        }

        // Flush srdi for this group
        Srdi.clearSrdi(this);

        // Load the list of peer group services from the module implementation advertisement parameters.
        StdPeerGroupParamAdv peerGroupParametersAdvertisement = new StdPeerGroupParamAdv(moduleImplementationAdvertisement.getParam());        
        
        Map<ModuleClassID, Object> services = new HashMap<> (peerGroupParametersAdvertisement.getServices());
        services.putAll(peerGroupParametersAdvertisement.getProtos());

        // Remove the modules disabled in the configuration file.        
        if(configurationParametersAdvertisement != null) {
            Iterator<ModuleClassID> eachModule = services.keySet().iterator();

            while(eachModule.hasNext()) {
                ModuleClassID aModule = eachModule.next();

                if(!configurationParametersAdvertisement.isSvcEnabled(aModule)) {

                    // Remove disabled module
                    Logging.logCheckedDebug(LOG, "Module disabled in configuration : ", aModule);
                    eachModule.remove();

                }
            }
        }
        
        //The membership service is mandatory from now on (Jan. 20, 2008). It will be loaded first
        //and logged in. That will make sure the subsequent publishing will be signed.
        //The objective of this section is to establish the peer's default credential for this group.
        Object membershipServiceSpecification = services.remove(IModuleDefinitions.membershipClassID);
        
        if(membershipServiceSpecification == null) {
            throw new PeerGroupException("Membership service is mandatory. It is not found for this group : " + this.getPeerGroupName());
        } else {
            Map<ModuleClassID, Object> membershipServiceModuleParameters = new HashMap<>();
            membershipServiceModuleParameters.put(IModuleDefinitions.membershipClassID, membershipServiceSpecification);
            //Load peer group membership service
            loadAllModules(membershipServiceModuleParameters, true);
            int startModulesResult = startModules((Map)membershipServiceModuleParameters);                        
            
            if(startModulesResult == Module.START_OK) {                
                //Automatic authentication only for World Peer Group and Net Peer Group
                ID peerGroupModuleSpecId =  getPeerGroupAdvertisement().getModuleSpecID();
            
                if (peerGroupModuleSpecId.equals(IModuleDefinitions.refPlatformSpecID) || peerGroupModuleSpecId.equals(IModuleDefinitions.refNetPeerGroupSpecID)) {
                    MembershipService membershipService = this.getMembershipService();
                    Credential defaultCredentials = null;                                

                    NetworkManager networkManager = JxtaApplication.findNetworkManager(getStoreHome());
                    assert networkManager != null;

                    String membershipAuthenticationType = "";
                    String membershipPassword = "";

                    try {
                        NetworkConfigurator networkConfigurator = networkManager.getConfigurator();
                        membershipAuthenticationType = networkConfigurator.getAuthenticationType();
                        membershipPassword = networkConfigurator.getPassword();
                        
                        defaultCredentials = membershipService.getDefaultCredential();
                        
                        if (defaultCredentials == null && membershipAuthenticationType != null) {                        
                            AuthenticationStrategy authenticationStrategy;
                            
                            switch (membershipAuthenticationType) {
                                case "StringAuthentication" :
                                    authenticationStrategy = new StringAuthenticationStrategy(this, membershipPassword, this.getPeerID().toString(), membershipPassword);                                                                    
                                    break;
                                case "EngineAuthentication":
                                    authenticationStrategy = new EngineAuthenticationStrategy(this);                                                                    
                                    break;                                                                
                                default:
                                    authenticationStrategy = new StringAuthenticationStrategy(this, membershipPassword, this.getPeerID().toString(), membershipPassword);
                                    break;
                            }                            
                            authenticationStrategy.authenticate();
                            
                            //The default credentials for nep peer group should be established.
                            //From now on, all the advertisements will be signed by this credential.
                        }                        
                    } catch (IOException ex) {                    
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to retrieve network manager");
                        stringBuilder.append(ex.getLocalizedMessage());
                        LOG.error(stringBuilder.toString());                                       
                    }  
                }                
                //The credential has already been established, perhaps done during the module startup.                
            } else {
                throw new PeerGroupException("Failed to start peer group membership service for this group: " + this.getPeerGroupName() + ". Error = " + startModulesResult);
            }
        }
        
        // Applications are shelved until startApp()
        applications.putAll(peerGroupParametersAdvertisement.getApps());

        if(null != configurationParametersAdvertisement) {
            Iterator<ModuleClassID> eachModule = applications.keySet().iterator();

            while(eachModule.hasNext()) {
                ModuleClassID aModule = eachModule.next();

                if(!configurationParametersAdvertisement.isSvcEnabled(aModule)) {
                    // Remove disabled modules
                    Logging.logCheckedDebug(LOG, "Application disabled in configuration : ", aModule);
                    eachModule.remove();
                }
            }
        }

        loadAllModules(services, true);

        int result = startModules((Map) services);

        if(result != Module.START_OK) {
            throw new PeerGroupException("Failed to start peer group services. Result : " + result);
        }

        // Make sure all the required services are loaded.
        try {
            checkServices();
        } catch (ServiceNotFoundException e) {
            LOG.error("Missing peer group service", e);
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
                // publish to.
                discoveryService.publish(discoveryService.getImplAdvertisement(), DEFAULT_LIFETIME, DEFAULT_EXPIRATION);

                // Try to publish our impl adv within this group. (it was published
                // in the parent automatically when loaded.
                discoveryService.publish(moduleImplementationAdvertisement, DEFAULT_LIFETIME, DEFAULT_EXPIRATION);

            } catch (Exception nevermind) {
                Logging.logCheckedWarning(LOG, "Failed to publish Impl adv within group.", nevermind);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Nothing special for now, but we might want to move some steps from
     * initFirst() in the future.
     * @throws net.jxta.exception.PeerGroupException
     */
    @Override
    protected synchronized void initLast() throws PeerGroupException {

        super.initLast();

        if (Logging.SHOW_CONFIG && LOG.isConfigEnabled()) {

            StringBuilder configInfo = new StringBuilder("Configuring Group : " + getPeerGroupID());

            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tCompatibility Statement :\n\t\t\t");

            StringBuilder indent = new StringBuilder(
                    CompatibilityUtils.createDefaultCompatStatement().toString().trim());
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
            Iterator<Map.Entry<ModuleClassID, Object>> eachApp = applications.entrySet().iterator();

            if (eachApp.hasNext()) {
                configInfo.append("\n\t\tApplications :");
            }
            while (eachApp.hasNext()) {
                Map.Entry<ModuleClassID, Object> anEntry = eachApp.next();
                ModuleClassID aMCID = anEntry.getKey();
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
    @Override
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
        IJxtaLoader loader = getLoader();

        // Find all purpose implementation advertisement by its specification ID
        ModuleImplAdvertisement implAdv = loader.findModuleImplAdvertisement(IModuleDefinitions.allPurposePeerGroupSpecID);
        return implAdv;
    }

    /**
     * Returns the cache manager associated with this group.
     *
     * @return the cache manager associated with this group.
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * Return a map of the applications for this group.
     * <p/>
     * <ul>
     * <li>keys are {@link net.jxta.platform.ModuleClassID}</li>
     * <li>values are {@link net.jxta.platform.Module} or
     * {@link net.jxta.protocol.ModuleImplAdvertisement}</li>
     * </ul>
     *
     * @return a map of the applications for this group.
     */
    public Map<ModuleClassID, Object> getApplications() {
        return Collections.unmodifiableMap(applications);
    }    
}

