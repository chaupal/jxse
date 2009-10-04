package net.jxta.osgiimpl;

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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.osgi.platform.NetworkManager;
import net.jxta.osgiimpl.platform.NetworkManagerImpl;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager.ConfigMode;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezVousStatus;

import org.mortbay.util.LoggerLogSink;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Activates and provides a ..osgi.NetworkManager
 * 
 * @author boylejohnr
 * 
 */
public class Activator implements BundleActivator
{

	private static final Logger log = Logger.getLogger(Activator.class.getName());

	/**
	 * This is the reference to the JXTA implementation of Network Manager, not the osgi provided one.
	 */
	private net.jxta.platform.NetworkManager networkManagerJXTA;

	private NetworkManager networkManagerOSGiWrap;

	private ServiceRegistration serviceRegistration;

	private Dictionary<String, String> config;
	private CountDownLatch configLatch = new CountDownLatch(1);

	public Activator()
	{
		setUpHTTPLogging();
	}

	/**
	 * Starts network manager
	 */
	public void start(BundleContext bundleContext) throws Exception
	{
		try
		{
			log.log(Level.INFO, "Starting JXTA in activator");
			symbolicName = bundleContext.getBundle().getSymbolicName();
			bundleContext.registerService(ManagedService.class.getName(), configurationAdmin,
					getManagedServiceConfig(bundleContext));
			while (!configLatch.await(1, TimeUnit.SECONDS))
			{
				System.err.println("Awaiting Config through managed service.");
			}
			System.err.println("Config on start = " + config);
			instanceName = config.get(NetworkManager.JXTA_INSTANCE_NAME);
			networkManagerJXTA = new net.jxta.platform.NetworkManager(getConfigMode(), instanceName);
			networkManagerJXTA.setInstanceHome(new URI(networkManagerJXTA.getInstanceHome() + instanceName + "/"));
			boolean deleteFile = deleteFile(new File(networkManagerJXTA.getInstanceHome()));
			networkManagerOSGiWrap = new NetworkManagerImpl(networkManagerJXTA);
			if (networkManagerJXTA == null)
			{
				throw new IllegalStateException(
						"Can not start JXTA, networkManager could not be constructed on start, check for earlier exceptions in log");
			}
			reconfigure();
			networkManagerJXTA.startNetwork();
			// if (networkManagerJXTA.getNetPeerGroup().isRendezvous())
			// {
			//
			// networkManagerJXTA.getNetPeerGroup().getRendezVousService().addListener(new RendezvousListener()
			// {
			//
			// @Override
			// public void rendezvousEvent(RendezvousEvent arg0)
			// {
			// System.err.println("RDV Event : " + arg0);
			// }
			// });
			// }

			// Thread thread = new Thread(testConnection);
			// thread.setDaemon(true);
			// thread.setName("RDV check " + instanceName);
			// thread.start();
			testConnection.run();
			serviceRegistration = bundleContext.registerService(NetworkManager.class.getName(), networkManagerOSGiWrap,
					new Hashtable()
					{
						{
							put(NetworkManager.JXTA_INSTANCE_NAME, instanceName);
						}
					});
			log.log(Level.INFO, "Started JXTA in activator");
		} catch (Throwable e)
		{
			log.log(Level.SEVERE, "Failed in activator", e);
			throw new Exception(e);
		}
	}

	private final Runnable testConnection = new Runnable()
	{

		public void run()
		{
			RendezVousService rendezVousService = networkManagerJXTA.getNetPeerGroup().getRendezVousService();
			log.info("Awaiting connect to rendezvous connected=" + rendezVousService.isConnectedToRendezVous()
					+ ",isRendevous=" + rendezVousService.isRendezVous());
			RendezVousStatus rendezVousStatus = rendezVousService.getRendezVousStatus();

			while (!rendezVousService.isConnectedToRendezVous() && !rendezVousService.isRendezVous()
					&& getConfigMode() != ConfigMode.RENDEZVOUS_RELAY)
			{
				log.info(instanceName + "Awaiting rendevous connection");
				System.out.print("");
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	};

	public static boolean deleteFile(File path)
	{
		if (path.exists())
		{
			if (path.isDirectory())
			{
				File[] files = path.listFiles();
				for (int i = 0; i < files.length; i++)
				{
					if (files[i].isDirectory())
					{
						deleteFile(files[i]);
					} else
					{
						boolean deleted = files[i].delete();
						if (deleted)
						{
							System.err.println("Deleted " + files[i]);
						} else
						{
							System.err.println("Failed to delete " + files[i]);
						}
					}
				}
			}
		}
		return (path.delete());
	}

	private ConfigMode getConfigMode()
	{
		String configModeString = config.get(NetworkManager.JXTA_CONFIG_MODE);
		System.err.println("-----------------Config Mode " + configModeString);
		ConfigMode configMode = ConfigMode.valueOf(configModeString);
		return configMode;
	}

	private final ManagedService configurationAdmin = new ManagedService()
	{
		public void updated(Dictionary arg0) throws ConfigurationException
		{
			System.err.println("Manged service update for peer with symbolic name " + symbolicName);
			if (arg0 == null)
			{
				return;
			}
			config = arg0;
			for (Enumeration<String> keys = config.keys(); keys.hasMoreElements();)
			{
				String key = keys.nextElement();
				System.err.println(key + "=" + config.get(key));
			}
			configLatch.countDown();
		}
	};

	private String symbolicName;

	private String instanceName;

	private Dictionary getManagedServiceConfig(BundleContext context)
	{
		Dictionary result = new Hashtable();
		result.put(Constants.SERVICE_PID, context.getBundle().getSymbolicName());
		return result;
	}

	private void reconfigure()
	{

		NetworkConfigurator configurator;
		try
		{
			configurator = networkManagerJXTA.getConfigurator();
		} catch (IOException e)
		{
			log.log(Level.SEVERE, e.toString(), e);
			return;
		}
		configurator.setTcpStartPort(-1);
		configurator.setTcpEndPort(-1);
		configurator.setUseMulticast(Boolean.parseBoolean(config.get(NetworkManager.JXTA_MULTICAST_ENABLED)));
		String multicastPortString = config.get(NetworkManager.JXTA_MULTICAST_PORT);
		if (multicastPortString != null)
		{
			configurator.setMulticastPort(Integer.parseInt(multicastPortString));
		}
		configurator.setTcpEnabled(Boolean.parseBoolean(config.get(NetworkManager.JXTA_TCP_ENABLED)));
		String tcpInterfaceAddress = config.get(NetworkManager.JXTA_TCP_INTERFACE_ADDRESS);
		if (tcpInterfaceAddress != null)
		{
			configurator.setTcpInterfaceAddress(tcpInterfaceAddress);
		}
		configurator.setTcpPort(Integer.parseInt(config.get(NetworkManager.JXTA_TCP_PORT)));
		configurator.setTcpIncoming(Boolean.parseBoolean(config.get(NetworkManager.JXTA_TCP_INCOMING)));
		configurator.setHttpEnabled(Boolean.parseBoolean(config.get(NetworkManager.JXTA_HTTP_ENABLED)));
		URI uri;
		try
		{
			String uriString = config.get(NetworkManager.JXTA_RELAY_URI);
			if (uriString != null)
			{
				uri = new URI(uriString);
				configurator.addSeedRelay(uri);
				configurator.addSeedRendezvous(uri);
				configurator.setUseOnlyRelaySeeds(true);
				configurator.setUseOnlyRendezvousSeeds(true);
			}
		} catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println(configurator.getInfrastructureDescriptionStr());
	}

	/**
	 * Stops the network manager
	 */
	public void stop(BundleContext arg0) throws Exception
	{
		log.log(Level.INFO, "Stopping JXTA in activator");
		if (networkManagerJXTA != null)
		{
			networkManagerJXTA.stopNetwork();
		}
	}

	/**
	 * JXTA over rides loggin internally, this gets to it first.
	 */
	private static void setUpHTTPLogging()
	{
		if (System.getProperty("LOG_CLASSES") == null)
		{
			System.setProperty("LOG_CLASSES", "Ignored since already set");
			LoggerLogSink logSink = new LoggerLogSink();
			// Just setting with this logger, issues usi
			Logger jettyLogger = Logger.getLogger(org.mortbay.http.HttpServer.class.getName());

			logSink.setLogger(jettyLogger);
			try
			{
				logSink.start();
				org.mortbay.util.Log.instance().add(logSink);
			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
