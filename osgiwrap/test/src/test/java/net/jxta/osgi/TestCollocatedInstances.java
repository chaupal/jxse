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
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import net.jxta.osgi.pipe.BiDiPipe;
import net.jxta.osgi.pipe.BiDiServerPipe;
import net.jxta.osgi.pipe.InputPipe;
import net.jxta.osgi.pipe.Message;
import net.jxta.osgi.pipe.OutputPipe;
import net.jxta.osgi.pipe.Pipe;
import net.jxta.osgi.pipe.PipeAdvertisement;
import net.jxta.osgi.pipe.PipeMsgEvent;
import net.jxta.osgi.pipe.PipeMsgListener;
import net.jxta.osgi.pipe.PipeService;
import net.jxta.osgi.pipe.PipeStateListener;
import net.jxta.osgi.pipe.ServerPipeConnectionListener;
import net.jxta.osgi.platform.NetworkManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class TestCollocatedInstances extends BasePeerTest
{
	private static final String PEER_TWO = "peerTwo";
	private static final String PEER_ONE = "peerOne";
	private static final String RELAY = "relay";
	private static String hostname;
	private List<Properties> peerConfigs;

	@Before
	public void setUp() throws UnknownHostException
	{
		hostname = getHostName();
		peerConfigs = new LinkedList<Properties>()
		{
			{
				add(new Properties()
				{
					{
						put(NetworkManager.JXTA_INSTANCE_NAME, RELAY);
						put(NetworkManager.JXTA_TCP_PORT, String.valueOf(7001));
						put(NetworkManager.JXTA_TCP_ENABLED, String.valueOf(true));
						put(NetworkManager.JXTA_CONFIG_MODE, "RENDEZVOUS_RELAY");
						put(NetworkManager.JXTA_MULTICAST_ENABLED, String.valueOf(false));
						// put(NetworkManager.JXTA_MULTICAST_PORT, String.valueOf(6789));
						put(NetworkManager.JXTA_TCP_INTERFACE_ADDRESS, hostname);
						put(NetworkManager.JXTA_HTTP_ENABLED, String.valueOf(false));
						put(NetworkManager.JXTA_TCP_INCOMING, String.valueOf(true));

					}
				});
				add(new Properties()
				{
					{
						put(NetworkManager.JXTA_INSTANCE_NAME, PEER_ONE);
						put(NetworkManager.JXTA_TCP_PORT, String.valueOf(7003)); // Why is this port configured..?
						put(NetworkManager.JXTA_TCP_ENABLED, String.valueOf(true));
						put(NetworkManager.JXTA_CONFIG_MODE, "EDGE");
						put(NetworkManager.JXTA_MULTICAST_ENABLED, String.valueOf(false));
						put(NetworkManager.JXTA_HTTP_ENABLED, String.valueOf(false));
						put(NetworkManager.JXTA_TCP_INCOMING, String.valueOf(true));
						put(NetworkManager.JXTA_RELAY_URI, "tcp://" + hostname + ":7001");
					}
				});
				add(new Properties()
				{
					{
						put(NetworkManager.JXTA_INSTANCE_NAME, PEER_TWO);
						put(NetworkManager.JXTA_TCP_PORT, String.valueOf(7002));
						put(NetworkManager.JXTA_TCP_ENABLED, String.valueOf(true));
						put(NetworkManager.JXTA_CONFIG_MODE, "EDGE");
						put(NetworkManager.JXTA_MULTICAST_ENABLED, String.valueOf(false));
						put(NetworkManager.JXTA_HTTP_ENABLED, String.valueOf(false));
						put(NetworkManager.JXTA_TCP_INCOMING, String.valueOf(false));
						put(NetworkManager.JXTA_RELAY_URI, "tcp://" + hostname + ":7001");
					}
				});
			}
		};

	}

	@Test
	// @Ignore("Not rolled out yet")
	public void testCollocatedPropagatedInstance() throws Exception
	{
		List<Pipe> pipesToClose = new LinkedList<Pipe>();
		try
		{
			initPeers();
			PipeService pipeServiceOne = getNetworkManager(PEER_ONE).getNetPeerGroup().getPipeService();
			PipeService pipeServiceTwo = getNetworkManager(PEER_TWO).getNetPeerGroup().getPipeService();

			String propagatedPipeName = "test";
			OutputPipe propagatedOuputPipeOne = pipeServiceOne.createPropagatedOuputPipe(propagatedPipeName);
			pipesToClose.add(propagatedOuputPipeOne);
			final String elementname = "testStringElement";
			final LatchMessageReceived messageReceived = new LatchMessageReceived("PeerTwoMessageReceived");

			InputPipe createPropagatedInputPipe = pipeServiceTwo.createPropagatedInputPipe(propagatedPipeName, messageReceived);
			pipesToClose.add(createPropagatedInputPipe);
			Message message = pipeServiceOne.createMessage();

			message.addStringElement(elementname, "This is a test string ");
			System.err.println("Sending message " + message.getStringElement(elementname));
			boolean success = propagatedOuputPipeOne.send(message);
			Assert.assertTrue("Failed to send message ", success);

			Assert.assertTrue("Failed to receive message", messageReceived.await(5000, TimeUnit.SECONDS));
		} finally
		{
			closePipes(pipesToClose);
		}
	}

	private void closePipes(List<Pipe> pipesToClose)
	{
		for (Pipe toClose : pipesToClose)
		{
			try
			{
				toClose.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Test
	// @Ignore("Not working on this at present.")
	public void testCollocatedBiDiInstance() throws Exception
	{
		List<Pipe> pipesToClose = new LinkedList<Pipe>();
		try
		{
			initPeers();
			final PipeService pipeServiceOne = getNetworkManager(PEER_ONE).getNetPeerGroup().getPipeService();
			PipeService pipeServiceTwo = getNetworkManager(PEER_TWO).getNetPeerGroup().getPipeService();

			String bidiPipeName = "bidiPipe";
			final CountDownLatch connectedLatch = new CountDownLatch(1);

			final BiDiPipe[] bidiHolder = new BiDiPipe[1];
			final LatchMessageReceived serverMessageReceived = new LatchMessageReceived("ServerMessage");
			final LatchMessageReceived senderMessageReceived = new LatchMessageReceived("SenderMessage");
			final String serverTestElement = "ServerTestElement";
			final LatchedPipeStates serverPipeState = new LatchedPipeStates("Server");
			BiDiServerPipe propagatedServerPipeOne = pipeServiceOne.createBiDiServerPipe(bidiPipeName,
					new ServerPipeConnectionListener()
					{
						public void onAccept(BiDiPipe pipe)
						{
							bidiHolder[0] = pipe;
							pipe.setMessageListener(serverMessageReceived);
							pipe.setPipeStateListener(serverPipeState);
							System.err.println("IS BOUND? =" + pipe.isBound());// Bound is tosh means not a lot.
							Message message = pipeServiceOne.createMessage();

							message.addStringElement(serverTestElement, "This is a test string from server");
							System.err.println("Sending message " + message.getStringElement(serverTestElement));
							boolean success;
							try
							{
								success = pipe.send(message);
								Assert.assertTrue("Failed to send message ", success);
							} catch (IOException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							connectedLatch.countDown();
						}
					});
			pipesToClose.add(propagatedServerPipeOne);
			final String elementname = "testStringElement";
			PipeAdvertisement advertisement = pipeServiceTwo.createPipeAdvertisement(propagatedServerPipeOne.getAdv()
					.getInputStream());
			final LatchedPipeStates sendPipeStates = new LatchedPipeStates("Sender");
			BiDiPipe sendingPipe = pipeServiceTwo.createBiDiPipe(advertisement, senderMessageReceived, sendPipeStates);
			pipesToClose.add(sendingPipe);

			Assert.assertTrue("Failed to getConnected", connectedLatch.await(5, TimeUnit.SECONDS));
			// Assert.assertTrue("Failed to open on server", serverPipeState.opened.await(5, TimeUnit.SECONDS));
			Assert.assertTrue("Failed to get Opened Connection", sendPipeStates.opened.await(5, TimeUnit.SECONDS));
			// Will get this first, since occurs on connection accept.
			Assert.assertTrue("Failed to receive message", senderMessageReceived.await(5, TimeUnit.SECONDS));

			BiDiPipe receivingPipe = bidiHolder[0];
			pipesToClose.add(receivingPipe);

			Message message = pipeServiceTwo.createMessage();
			message.addStringElement(elementname, "This is a test string ");
			System.err.println("Sending message " + message.getStringElement(elementname));
			boolean success = sendingPipe.send(message);
			Assert.assertTrue("Failed to send message ", success);

			Assert.assertTrue("Failed to receive message", serverMessageReceived.await(5, TimeUnit.SECONDS));
		} finally
		{
			closePipes(pipesToClose);
		}
	}

	protected List<Properties> getPeerConfigurations()
	{
		return peerConfigs;
	}

	private static class LatchMessageReceived implements PipeMsgListener
	{
		private final CountDownLatch countDownLatch;
		private final String name;

		public LatchMessageReceived(String name)
		{
			this.name = name;
			countDownLatch = new CountDownLatch(1);
		}

		public long getRemaining()
		{
			return countDownLatch.getCount();
		}

		public LatchMessageReceived(String name, int numberMessages)
		{
			this.name = name;
			countDownLatch = new CountDownLatch(numberMessages);
		}

		public void pipeMsgEvent(PipeMsgEvent pipeMsgEvent)
		{
			System.err.println(name + " GOT A MESSAGE latch=" + countDownLatch.getCount());
			countDownLatch.countDown();
		}

		public boolean await(long timeout, TimeUnit unit)
		{
			try
			{
				return countDownLatch.await(timeout, unit);
			} catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}

	private static class LatchedPipeStates implements PipeStateListener
	{
		public CountDownLatch closed = new CountDownLatch(1);
		public CountDownLatch opened = new CountDownLatch(1);
		public CountDownLatch failed = new CountDownLatch(1);
		private final String name;

		public LatchedPipeStates(String name)
		{
			this.name = name;

		}

		public void onClosed()
		{
			System.err.println(name + " Closed");
			closed.countDown();
		}

		public void onFailed()
		{
			System.err.println(name + " Failed");
			failed.countDown();
		}

		public void onOpened()
		{
			System.err.println(name + " Opened");
			opened.countDown();
		}
	}
}
