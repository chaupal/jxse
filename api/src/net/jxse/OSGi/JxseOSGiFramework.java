/*
 * ====================================================================
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxse.OSGi;

import java.io.IOException;
import java.util.logging.Logger;
import net.jxse.JxseInstantiator;
import net.jxta.configuration.JxtaConfiguration;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

/**
 * API access to the OSGi Framework.
 *
 * <p>This object handles the call to the {@code JxseOSGiFramework}
 * implementation object using reflexion, as specified in the [@code OSGi.properties}
 * file.
 * <p>The {@code INSTANCE} public static attribute is initialized with the OSGi framework
 * object.
 * <p>Typically, the OSGi framework should be accessed as following in a standalone application:
 * <pre>
 *     public static void main(String[] args) {
 *         ...
 *         try {
 *
 *             // Starting the framework
 *             JxseOSGiFramework.INSTANCE.start();
 *             ...
 *             // Accessing the framework
 *             ServiceReference[] Services = JxseOSGiFramework.INSTANCE.getRegisteredServices();
 *             ...
 *             // Stopping the framewok
 *             JxseOSGiFramework.INSTANCE.stop();
 * 
 *             // Waiting for the stop (0 = wait indefinitely)
 *             FrameworkEvent StopEvent = JxseOSGiFramework.INSTANCE.waitForStop(0);
 *             ...
 *             System.exit(0);
 *             ...
 *
 *     }
 * </pre>
 */
public class JxseOSGiFramework {

    /**
     *  Logger
     */
    private final static Logger LOG = Logger.getLogger(JxseOSGiFramework.class.getName());
    
    /**
     *  OSGi configuration and properties
     */
    private static final JxtaConfiguration Configuration = new JxtaConfiguration();

    // Loading OSGi configuration
    static {
        try {
            Configuration.load(JxseOSGiFramework.class.getResourceAsStream("JxseOSGi.properties"));
        } catch (IOException ex) {
            LOG.severe("Cannot load JxseOSGi.properties :\n" + ex.toString());
        }
    }

    /**
     * OSGI Framework instance
     */
    public static final Framework INSTANCE;

    // Creating the framework instance
    static {

        final String FrameworkClassName = Configuration.getProperty("FRAMEWORK_LAUNCHER");

        if (FrameworkClassName==null) {

            LOG.severe("Null 'FRAMEWORK_LAUNCHER' property, cannot create OSGi framework");
            INSTANCE = null;

        } else {

            // Retrieving OSGi framework launcher class
            final Class FrameworkLauncherClass = JxseInstantiator.forName(Configuration.getProperty("FRAMEWORK_LAUNCHER"));

            // Creating framework instance launcher
            final JxseOSGiFrameworkLauncher TheLauncher = (JxseOSGiFrameworkLauncher) JxseInstantiator.instantiateWithNoParameterConstructor(FrameworkLauncherClass);

            // Initializing OSGi framework object
            INSTANCE = TheLauncher.getOsgiFrameworkInstance();

        }

    }

    /**
     * Provides a {@code ServiceTracker} for a corresponding service API class registered within the
     * OSGi framework. These are very useful to access registered OSGi services, since these may
     * (potentially) come and go at any time. {@code ServiceTracker}s help tracking their
     * availability.
     *
     * <p>A typical use of {@code ServiceTracker}s is the following:
     * <pre>
     *    ...
     *    // Creating a service tracker for the NetworkManager OSGi service
     *    ServiceTracker NetworkManagerST = JxseOSGiFramework.getServiceTracker(JxseOSGiNetworkManagerService.class);
     *
     *    // Starting the service tracker
     *    NetworkManagerST.open();
     *    ...
     *
     *    // Trying to retrieve the service for 10 seconds
     *    JxseOSGiNetworkManagerService TheNMS = NetworkManagerST.waitForService(10000);
     *
     *    if (TheNMS!=null) {
     *        // The service is available...
     *    } else {
     *        // The service is not available...
     *    }
     *    ...
     *
     *    // Stopping the service tracker
     *    NetworkManagerST.close();
     *    ...
     *
     * </pre>
     *
     * @param serviceAPIClass The service API Class
     * @return An OSGi service tracker
     */
    public static ServiceTracker getServiceTracker(Class serviceAPIClass) {

        return new ServiceTracker(INSTANCE.getBundleContext(), serviceAPIClass.getName(), null);

    }

}
