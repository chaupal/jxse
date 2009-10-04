package net.jxta.osgi;

/**
 * 
 * ====================================================================
 * 
 * Copyright (c) 2001 Sun Microsystems, Inc. All rights reserved.
 * 
 * The Sun Project JXTA(TM) Software License
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must include the following acknowledgment: "This
 * product includes software developed by Sun Microsystems, Inc. for JXTA(TM) technology." Alternately, this acknowledgment may
 * appear in the software itself, if and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must not be used to endorse or promote products
 * derived from this software without prior written permission. For written permission, please contact Project JXTA at
 * http://www.jxta.org.
 * 
 * 5. Products derived from this software may not be called "JXTA", nor may "JXTA" appear in their name, without prior written
 * permission of Sun.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN MICROSYSTEMS OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * JXTA is a registered trademark of Sun Microsystems, Inc. in the United States and other countries.
 * 
 * Please see the license information page at : <http://www.jxta.org/project/www/license.html> for instructions on use of the
 * license in source files.
 * 
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on behalf of Project JXTA. For more information on
 * Project JXTA, please see http://www.jxta.org.
 * 
 * This license is based on the BSD license adopted by the Apache Foundation.
 * 
 * @author John Boyle oneDrum.com john@onedrum.com
 */
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.frameworks;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import net.jxta.osgi.platform.NetworkManager;

import org.junit.Assert;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.TimeoutOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public abstract class BasePeerTest
{
	@Inject
	BundleContext bundleContext;
	static final String NET_JXTA_OSGI_IMPL = "net.jxta.osgi.impl";
	private NetworkManager netPeerGroupOne;
	private NetworkManager netPeerGroupTwo;
	private static String hostnameProperty = "test.jxta.hostname";
	private boolean autoStart = true;

	protected static String getHostName() throws UnknownHostException
	{
		return System.getProperty(hostnameProperty, "127.0.0.1" /* InetAddress.getLocalHost().toString() */);
	}

	@Configuration
	public Option[] configure() throws UnknownHostException
	{
		MavenArtifactProvisionOption services = mavenBundle().groupId("com.quolos.eclipse.ext").artifactId(
				"org.eclipse.osgi.services").versionAsInProject().start();
		MavenArtifactProvisionOption useradmin = mavenBundle().groupId("com.quolos.ext").artifactId("useradmin")
				.versionAsInProject().start();
		MavenArtifactProvisionOption preferences = mavenBundle().groupId("com.quolos.ext").artifactId("preferences")
				.versionAsInProject().start();
		MavenArtifactProvisionOption common = mavenBundle().groupId("com.quolos.ext").artifactId("org.eclipse.equinox.common")
				.versionAsInProject().start();
		MavenArtifactProvisionOption configAdmin = mavenBundle().groupId("com.quolos.ext").artifactId("org.eclipse.equinox.cm")
				.versionAsInProject().start();

		MavenArtifactProvisionOption servlet = mavenBundle().groupId("com.quolos.ext").artifactId("javax.servlet")
				.versionAsInProject().start(); // WHY?
		MavenArtifactProvisionOption qcommon = mavenBundle().groupId("net.jxta").artifactId("osgi").versionAsInProject()
				.start();
		MavenArtifactProvisionOption peerForCopy = mavenBundle().groupId("net.jxta").artifactId("osgiimpl")
				.versionAsInProject().noStart();
		// MavenArtifactProvisionOption peerTwo = mavenBundle().groupId("net.jxta").artifactId("test.osgiimpl")
		// .versionAsInProject().noStart();
		// VMOption debugOption = new VMOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9001");
		TimeoutOption timeoutOption = new TimeoutOption(0);
		Option bundles = CoreOptions.provision(services, preferences, common, configAdmin, servlet, qcommon, peerForCopy);

		return options(logProfile(), systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
				frameworks(equinox()), bundles/* , debugOption, timeoutOption */);
	}

	protected void initPeers() throws Error, InterruptedException, InvalidSyntaxException
	{
		ServiceReference configAdminServiceRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		assertNotNull("A configuration admin is required ", configAdminServiceRef);
		ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceRef);
		assertNotNull(bundleContext);

		for (Bundle bundle : bundleContext.getBundles())
		{
			printBundleInfo(bundle);
			try
			{
				if (NET_JXTA_OSGI_IMPL.equals(bundle.getSymbolicName()))
				{
					// configureBundle(configAdmin, bundle, getPeerOneConfig());
					// if (autoStart)
					// {
					// bundle.start();
					// }
					installDuplicatePeers(bundle, configAdmin);
				}
				// else if ("net.jxta.test.osgi.impl".equals(bundle.getSymbolicName()))
				// {
				// configureBundle(configAdmin, bundle, getPeerTwoConfig());
				// // bundle.start();
				// }
			} catch (Exception e)
			{
				throw new Error("Failed to start " + bundle, e);
			}
		}
		// netPeerGroupOne = retrieveNetworkManager(bundleContext, (String) getPeerOneConfig().get(
		// NetworkManager.JXTA_INSTANCE_NAME));
		// assertNotNull(netPeerGroupOne);
		// netPeerGroupTwo = retrieveNetworkManager(bundleContext, (String) getPeerTwoConfig().get(
		// NetworkManager.JXTA_INSTANCE_NAME));
		// assertNotNull(netPeerGroupTwo);
	}

	private void installDuplicatePeers(Bundle bundle, ConfigurationAdmin configAdmin) throws IOException, BundleException
	{
		boolean usedGoldenBundle = false;
		for (Properties config : getPeerConfigurations())
		{
			Bundle duplicateBundle = null;
			String instanceName = config.getProperty(NetworkManager.JXTA_INSTANCE_NAME);
			Assert.assertNotNull("Peer properties must have an instance name " + NetworkManager.JXTA_INSTANCE_NAME,
					instanceName);
			if (usedGoldenBundle)
			{
				duplicateBundle = installDuplicateBundle(bundle, instanceName);
			} else
			{
				duplicateBundle = bundle;
				usedGoldenBundle = true;
			}
			configureBundle(configAdmin, duplicateBundle, config);
			if (autoStart)
			{
				duplicateBundle.start();
			}
		}
	}

	private Bundle installDuplicateBundle(Bundle bundle, String name) throws IOException, BundleException
	{
		String prefix = "initial@reference:file:../";
		String location = bundle.getLocation();
		location = location.substring(prefix.length(), location.length() - 1);
		File source = new File(location);
		File destination = File.createTempFile(bundle.getSymbolicName(), name);
		destination.deleteOnExit();
		BundleUtils.replaceSymbolicName(source, destination, name);
		return bundleContext.installBundle(name, new FileInputStream(destination));
	}

	protected NetworkManager getNetworkManager(String name) throws InterruptedException, InvalidSyntaxException
	{
		return retrieveNetworkManager(bundleContext, name);
	}

	protected abstract List<Properties> getPeerConfigurations();

	private void configureBundle(ConfigurationAdmin configAdmin, Bundle bundle, Dictionary props) throws IOException
	{
		org.osgi.service.cm.Configuration configuration = configAdmin.getConfiguration(bundle.getSymbolicName(), bundle
				.getLocation());
		Dictionary properties = configuration.getProperties();
		Assert.assertNull("There should have been no preconfigured properties.", properties);
		configuration.update(props);
	}

	private void printBundleInfo(Bundle bundle)
	{
		System.out.println(bundle.getSymbolicName() + "{\n\tid=" + bundle.getBundleId() + ",registered = "
				+ Arrays.toString(bundle.getRegisteredServices()) + "\n\tin use = "
				+ Arrays.toString(bundle.getServicesInUse()) + "\n'\texported = " + bundle.getHeaders().keys() + "\n}");
	}

	private NetworkManager retrieveNetworkManager(BundleContext context, String name) throws InterruptedException,
			InvalidSyntaxException
	{
		Filter filter = context.createFilter("(" + NetworkManager.JXTA_INSTANCE_NAME + "=" + name + ")");
		// ServiceTracker tracker = new ServiceTracker(bundleContext, NetworkManager.class.getName(), null);
		ServiceTracker tracker = new ServiceTracker(bundleContext, filter, null);
		tracker.open();
		NetworkManager service = (NetworkManager) tracker.waitForService(1000);
		tracker.close();
		assertNotNull("Failed to get service with name " + name, service);
		return service;
	}

}
