package net.jxta.osgiimpl.pipe;

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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.osgi.pipe.BiDiPipe;
import net.jxta.osgi.pipe.BiDiServerPipe;
import net.jxta.osgi.pipe.InputPipe;
import net.jxta.osgi.pipe.Message;
import net.jxta.osgi.pipe.OutputPipe;
import net.jxta.osgi.pipe.PipeMsgListener;
import net.jxta.osgi.pipe.PipeService;
import net.jxta.osgi.pipe.ServerPipeConnectionListener;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;

public class PipeServiceImpl implements PipeService
{
	private final net.jxta.pipe.PipeService pipeService;
	private final PeerGroupID peerGroupID;
	private final net.jxta.peergroup.PeerGroup peerGroup;

	public PipeServiceImpl(net.jxta.peergroup.PeerGroup peerGroup, net.jxta.pipe.PipeService pipeService)
	{
		this.peerGroup = peerGroup;
		this.peerGroupID = peerGroup.getPeerGroupID();
		this.pipeService = pipeService;
	}

	public Message createMessage()
	{
		return new MessageImpl(new net.jxta.endpoint.Message());
	}

	public InputPipe createPropagatedInputPipe(String name, PipeMsgListener listener) throws IOException
	{
		PipeAdvertisement adv = getMulicastSocketAdvertisement(name);
		InputPipeImpl inputPipeImpl = new InputPipeImpl(pipeService.createInputPipe(adv, new PipeMsgListenerImpl(listener)));
		publish(adv);
		return inputPipeImpl;
	}

	public OutputPipe createPropagatedOuputPipe(String name) throws IOException
	{
		PipeAdvertisement adv = getMulicastSocketAdvertisement(name);
		OutputPipeImpl outputPipeImpl = new OutputPipeImpl(pipeService.createOutputPipe(adv, 50000));
		publish(adv);
		return outputPipeImpl;
	}

	@Override
	public BiDiPipe createBiDiPipe(net.jxta.osgi.pipe.PipeAdvertisement adv, PipeMsgListener listener,
			net.jxta.osgi.pipe.PipeStateListener stateListener) throws IOException
	{
		net.jxta.osgiimpl.pipe.PipeAdvertisementImpl adv2 = (net.jxta.osgiimpl.pipe.PipeAdvertisementImpl) adv;
		BiDiPipeNonDirect.NonDirectBiDiPipe jxtaBiDiPipe = new BiDiPipeNonDirect.NonDirectBiDiPipe();
		jxtaBiDiPipe.setReliable(true);
		jxtaBiDiPipe.setWindowSize(10);
		jxtaBiDiPipe.setPipeStateListener(new PipeStateListenerImpl(stateListener));
		while (!jxtaBiDiPipe.isBound())
		{
			System.err.println("CONNECTING ********************");
			jxtaBiDiPipe.connect(peerGroup, adv2.getNativePipeAdv(), 1000);
			// jxtaBiDiPipe.connect(peerGroup, peerGroup.getPeerID(), adv2.getNativePipeAdv(), 10000, new PipeMsgListenerImpl(
			// listener), true);
		}
		jxtaBiDiPipe.setMessageListener(new PipeMsgListenerImpl(listener));
		System.err.println("IS DIRECT " + jxtaBiDiPipe.isDirect());
		// new GetValueLoop(jxtaBiDiPipe).start();
		return new BiDiPipeImpl(jxtaBiDiPipe);
	}

	public static String printMessage(String name, net.jxta.endpoint.Message msg)
	{
		StringBuffer sb = new StringBuffer("Message content " + name + "{" + msg.getMessageNumber() + "}" + "[");
		for (Iterator<MessageElement> i = msg.getMessageElements(); i.hasNext();)
		{
			MessageElement element = i.next();
			sb.append("\n").append(element.getElementName() + ":" + element.toString());
		}
		sb.append("]");
		return sb.toString();
	}

	public net.jxta.osgi.pipe.PipeAdvertisement createPipeAdvertisement(InputStream is) throws IOException
	{
		return new PipeAdvertisementImpl((PipeAdvertisement) AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is));
	}

	@Override
	public BiDiServerPipe createBiDiServerPipe(String name, ServerPipeConnectionListener connectionListener) throws IOException
	{
		PipeAdvertisement adv = getSocketAdvertisement(name, null);
		BiDiServerPipeImpl biDiServerPipeImpl = new BiDiServerPipeImpl(
				new BiDiPipeNonDirect.NonDirectJxtaServerPipeNonBlockingAccept(peerGroup, adv,
						new ServerPipeConnectionListenerImpl(connectionListener)));
		publish(adv);
		return biDiServerPipeImpl;
	}

	private PipeAdvertisement getMulicastSocketAdvertisement(String name)
	{
		PipeID socketID = null;
		socketID = (PipeID) IDFactory.newPipeID(peerGroupID, hash(name));

		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement
				.getAdvertisementType());
		advertisement.setPipeID(socketID);
		// set to type to propagate
		advertisement.setType(net.jxta.pipe.PipeService.PropagateType);
		advertisement.setName(name);
		return advertisement;
	}

	private PipeAdvertisement getSocketAdvertisement(String name, String pipeIDStr)
	{
		PipeID socketID = null;
		if (pipeIDStr != null)
		{
			try
			{
				socketID = (PipeID) IDFactory.fromURI(new URI(pipeIDStr));
			} catch (URISyntaxException use)
			{
				use.printStackTrace();
			}
		} else
		{
			socketID = IDFactory.newPipeID(peerGroupID); // (PipeID)
		}
		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement
				.getAdvertisementType());
		advertisement.setPipeID(socketID);
		// set to type to propagate
		advertisement.setType(net.jxta.pipe.PipeService.UnicastType);
		advertisement.setName(name);
		return advertisement;
	}

	private static byte[] hash(final String expression)
	{
		byte[] result;
		MessageDigest digest;

		if (expression == null)
		{
			throw new IllegalArgumentException("Invalid null expression");
		}

		try
		{
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException failed)
		{
			failed.printStackTrace(System.err);
			RuntimeException failure = new IllegalStateException("Could not get SHA-1 Message");
			failure.initCause(failed);
			throw failure;
		}

		try
		{
			byte[] expressionBytes = expression.getBytes("UTF-8");
			result = digest.digest(expressionBytes);
		} catch (UnsupportedEncodingException impossible)
		{
			RuntimeException failure = new IllegalStateException("Could not encode expression as UTF8");

			failure.initCause(impossible);
			throw failure;
		}
		return result;
	}

	private void publish(Advertisement adv) throws IOException
	{
		DiscoveryService disco = peerGroup.getDiscoveryService();
		disco.publish(adv);
		disco.remotePublish(adv);
	}

}
