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

package net.jxta.peergroup;

import net.jxta.exception.ConfiguratorException;
import net.jxta.exception.PeerGroupException;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.platform.IJxtaLoader;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.impl.loader.DynamicJxtaLoader;
import net.jxta.impl.peergroup.DefaultConfigurator;
import net.jxta.impl.peergroup.NullConfigurator;

/**
 * A factory for instantiating the World Peer Group. Every peer starts by
 * instantiating the World Peer Group and then other Peer Groups are
 * instantiated as needed. Most applications do not need to use 
 * {@code WorldPeerGroupFactory} but should instead prefer using
 * {@code NetPeerGroupFactory} whenever possible.
 * <p/>
 * The World Peer Group provides the minimum core services needed to find
 * and instantiate other Peer Groups on a peer. The World Peer Group is the
 * primordial peer group upon which all other peer groups are instantiated. The
 * World Peer Group is primarily responsible for management of physical network
 * connections, physical network discovery (generally broadcast) and physical
 * network topology management.
 * <p/>
 * Applications generally do not normally interact directly with the World Peer 
 * Group. The World Peer Group includes only limited endpoint, resolver, 
 * discovery and rendezvous services. 
 * <p/>
 * When the <strong>World Peer Group</strong> starts it may optionally search
 * for <em>The Network Peer Group</em> on the local network and, if found, 
 * instantiate it. Otherwise a default built-in configuration of <em>The Net 
 * Peer Group</em> is instantiated.
 *
 * @since JXTA JSE 2.4
 *
 * @see net.jxta.peergroup.PeerGroup
 * @see net.jxta.peergroup.NetPeerGroupFactory
 */
public final class WorldPeerGroupFactory {

    private final static transient Logger LOG = Logging.getLogger(WorldPeerGroupFactory.class.getName());

    private static final Map<String, PeerGroup> worldPeerGroups = new HashMap<>();

    /**
     * Our strong reference to the World Peer Group.
     */
    private static PeerGroup worldPeerGroup;    

    /**
     * Provided for backwards compatibility, this constructor instantiates the
     * World Peer Group using the PlatformConfig file found in the directory
     * specified by the {@code JXTA_HOME} system property or the "{@code .jxta/}"
     * directory if {@code JXTA_HOME} is not defined.
     * <p/>
     * Though not deprecated this method should be considered as sample
     * code only and the other constructors should be used whenever possible.
     *
     * @throws PeerGroupException Thrown for problems construction the World
     * Peer Group.
     */
    public WorldPeerGroupFactory() throws PeerGroupException {
        // Establish the default store location via long established hackery.
        String jxtaHome = System.getProperty("JXTA_HOME", ".jxta/");

        // ensure that it ends in a seperator.
        if (!jxtaHome.endsWith(File.separator)) {
            jxtaHome += File.separator;
        }

        File jxtaHomeDir = new File(jxtaHome);

        // Ensure the homedir exists.
        if (!jxtaHomeDir.exists()) {
            jxtaHomeDir.mkdirs();
        }

        URI storeHome = jxtaHomeDir.toURI();

        // Instantiate the default configurator. Do not do this in your own code!
        try {
            NullConfigurator configurator = new DefaultConfigurator(storeHome);
            // Get (and possibly generate) the platform configuration.
            ConfigParams configurationParameters = configurator.getConfigParams();

            worldPeerGroup = newWorldPeerGroup(getDefaultWorldPeerGroupClass(), configurationParameters, storeHome);

            //Persist any changes which were made to the platform config by service initialization.
            configurator.setConfigParams(configurationParameters);
            configurator.save();
        } catch (ConfiguratorException configFailure) {
            String exceptionMessage = "Failure while managing World Peer Group configuration";
            LOG.error(exceptionMessage);
            throw new PeerGroupException(exceptionMessage, configFailure);
        }
    }

    /**
     * Constructs the World Peer Group using the specified configuration and
     * using the specified storeHome location for persistence.
     *
     * @param config The configuration to use for the World Peer Group.
     * @param storeHome The optional location that the World Peer Group and its'
     * services should use for storing persistent and transient information.
     * May be <tt>null</tt> if the World Peer Group is not provided a
     * persistent store (though this not currently supported).
     * @throws PeerGroupException Thrown for problems constructing the World
     * Peer Group.
     */
    public WorldPeerGroupFactory(ConfigParams config, URI storeHome) throws PeerGroupException {
        worldPeerGroup = newWorldPeerGroup(getDefaultWorldPeerGroupClass(), config, storeHome);
    }

    /**
     * Constructs the World Peer Group using the specified configuration and
     * using the specified storeHome location for persistence.
     *
     * @param worldPeerGroupClass The class which will be instantiated for the
     * World Peer Group instance.
     * @param config The configuration to use for the World Peer Group.
     * @param storeHome The optional location that the World Peer Group and its'
     * services should use for storing persistent and transient information.
     * May be <tt>null</tt> if the World Peer Group is not provided a
     * persistent store (though this not currently supported).
     * @throws PeerGroupException Thrown for problems constructing the World
     * Peer Group.
     */
    public WorldPeerGroupFactory(Class worldPeerGroupClass, ConfigParams config, URI storeHome) throws PeerGroupException {
        worldPeerGroup = newWorldPeerGroup(worldPeerGroupClass, config, storeHome);
    }

    /**
     * Returns a World Peer.
     *
     * @return An World Peer Group.
     */
    public PeerGroup getWorldPeerGroup() {
        return worldPeerGroup;
    }

//    /**
//     * Returns a weak (non-reference counted) interface object for the World
//     * Peer Group.
//     *
//     * @return A weak (non-reference counted) interface object for the World
//     * Peer Group.
//     * @see PeerGroup#getWeakInterface()
//     */
//    public PeerGroup getWeakInterface() {
//        return world.getWeakInterface();
//    }

    /**
     * Determine the class to use for the World PeeerGroup. 
     *
     * @return The Class which has been configured to be used for
     * World Peer Group instances.
     * @throws PeerGroupException Thrown for problems determining the class to
     * be used for the World Peer Group.
     */
    private static Class getDefaultWorldPeerGroupClass() throws PeerGroupException {
        try {
            IJxtaLoader loader = DynamicJxtaLoader.getInstance();

            ModuleImplAdvertisement worldGroupImplAdv = loader.findModuleImplAdvertisement(IModuleDefinitions.refPlatformSpecID);

            if(null == worldGroupImplAdv) {
                throw new PeerGroupException("Could not locate World PeerGroup Module implementation");
            }

            return Class.forName(worldGroupImplAdv.getCode());
        } catch (RuntimeException | ClassNotFoundException exception) {
            throw new PeerGroupException("Could not load World PeerGroup class.", exception);
        }
    }
    
    public static PeerGroup unregisterWorldPeerGroup(String storeHome) {
        return worldPeerGroups.remove(storeHome);                    
    }

    /**
     * Constructs the World Peer Group instance.
     *
     * @param worldPeerGroupClass The class which will be instantiated for the
     * World Peer Group instance.
     * @param config The configuration to use for the World Peer Group.
     * @param storeHome The optional location the World Peer Group and its'
     * services may use for storing persistent and transient information.
     * May be {@code null} if the World Peer Group is not provided a
     * persistent store (though this not currently supported).
     * @throws PeerGroupException Thrown for problems constructing the World
     * Peer Group.
     * @return the WorldPeerGroup
     */
    private PeerGroup newWorldPeerGroup(Class worldPeerGroupClass, ConfigParams config, URI storeHome) throws PeerGroupException {
        if (!storeHome.isAbsolute()) {
            LOG.error("storeHome must be an absolute URI.");
            throw new PeerGroupException("storeHome must be an absolute URI.");
        }

        if (storeHome.isOpaque()) {
            LOG.error("Opaque storeHome is not currently supported.");
            throw new PeerGroupException("Opaque storeHome is not currently supported.");
        }

        synchronized (worldPeerGroups)
        {
            //A global registry per Peer installation in VM.
            String storeHomeString = storeHome.toString();            
            worldPeerGroup = worldPeerGroups.get(storeHomeString);
            
            if (worldPeerGroup != null) {
                /*StringBuilder exceptionStringBuilder = new StringBuilder();
                exceptionStringBuilder.append("Only a single instance of the World Peer Group may be instantiated at a time with the home path sepcified");
                exceptionStringBuilder.append(System.getProperty("line.separator"));                
                exceptionStringBuilder.append("Home path: ");                
                exceptionStringBuilder.append(storeHomeString);
                throw new PeerGroupException(exceptionStringBuilder.toString());*/
                
                //Return already created WorldPeerGroup object for this store home path
                return worldPeerGroup;
            }            

            try {
                Logging.logCheckedInfo(LOG, "Making a new World Peer Group instance using : ", worldPeerGroupClass.getName());
                
                Constructor<PeerGroup> twoParams = (Constructor<PeerGroup>) worldPeerGroupClass.getConstructor(ConfigParams.class,URI.class);                
                worldPeerGroup = twoParams.newInstance(config, storeHome);               
                worldPeerGroup.init(null, PeerGroupID.WORLD_PEER_GROUP_ID, null);
                worldPeerGroups.put(storeHomeString, worldPeerGroup);
                return worldPeerGroup;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException exception) {                
                LOG.error("World Peer Group could not be instantiated.\n", exception);                                
                throw new PeerGroupException(exception);
            }            
        }
    }
}
