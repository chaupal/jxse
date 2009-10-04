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

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.impl.endpoint.tcp.TcpMessenger;
import net.jxta.impl.util.pipe.reliable.FixedFlowControl;
import net.jxta.impl.util.pipe.reliable.OutgoingMsgrAdaptor;
import net.jxta.impl.util.pipe.reliable.ReliableInputStream;
import net.jxta.impl.util.pipe.reliable.ReliableOutputStream;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import net.jxta.util.JxtaServerPipeNonBlockingAccept;

public class BiDiPipeNonDirect
{
	public static final class NonDirectBiDiPipe extends JxtaBiDiPipe
	{
		@Override
		protected net.jxta.endpoint.Message createOpenMessage(PeerGroup group, PipeAdvertisement pipeAd) throws IOException
		{
			net.jxta.endpoint.Message openMessage = super.createOpenMessage(group, pipeAd);
			openMessage.replaceMessageElement(NonDirectJxtaServerPipeNonBlockingAccept.nameSpace_Accessor,
					new StringMessageElement(NonDirectJxtaServerPipeNonBlockingAccept.directSupportedTag_Accessor, Boolean
							.toString(false), null));
			return openMessage;
		}

		/**
		 * Forces the creation.
		 */
		private void createRLib()
		{
			if (isReliable)
			{
				if (outgoing == null)
				{
					outgoing = new OutgoingMsgrAdaptor(msgr, retryTimeout);
				}
				if (ros == null)
				{
					ros = new ReliableOutputStream(outgoing, new FixedFlowControl(windowSize));
				}
				if (ris == null)
				{
					ris = new ReliableInputStream(outgoing, retryTimeout, this);
				}
			}
		}

		public boolean isDirect()
		{
			return direct;
		}

		public static StructuredDocument getCredDocAccessor(PeerGroup group)
		{
			return JxtaBiDiPipe.getCredDoc(group);
		}

		public void connect(PeerGroup group, PipeAdvertisement pipeAd, int timeout) throws IOException
		{
			connect(group, null, pipeAd, timeout, null);
			if (isDirect())
			{
				createRLib();
				direct = false;
			}
		}
	}

	public static final class NonDirectJxtaServerPipeNonBlockingAccept extends JxtaServerPipeNonBlockingAccept
	{
		public static final String nameSpace_Accessor = nameSpace;
		public static final String directSupportedTag_Accessor = directSupportedTag;

		public NonDirectJxtaServerPipeNonBlockingAccept(PeerGroup netPeerGroup, PipeAdvertisement adv,
				ConnectionListener listener) throws IOException
		{
			super(netPeerGroup, adv, listener);
		}

		@Override
		protected void sendResponseMessage(PeerGroup group, Messenger msgr, PipeAdvertisement pipeAd) throws IOException
		{

			net.jxta.endpoint.Message msg = new net.jxta.endpoint.Message();
			PeerAdvertisement peerAdv = group.getPeerAdvertisement();

			if (myCredentialDoc == null)
			{
				myCredentialDoc = NonDirectBiDiPipe.getCredDocAccessor(group);
			}

			if (myCredentialDoc != null)
			{
				msg.addMessageElement(JxtaServerPipe.nameSpace, new TextDocumentMessageElement(credTag,
						(XMLDocument) myCredentialDoc, null));
			}

			msg.addMessageElement(JxtaServerPipe.nameSpace, new StringMessageElement(JxtaServerPipe.directSupportedTag, Boolean
					.toString(false), null));// HERES THE HACK FORCE TO False

			msg.addMessageElement(JxtaServerPipe.nameSpace, new TextDocumentMessageElement(remPipeTag, (XMLDocument) pipeAd
					.getDocument(MimeMediaType.XMLUTF8), null));

			msg.addMessageElement(nameSpace, new TextDocumentMessageElement(remPeerTag, (XMLDocument) peerAdv
					.getDocument(MimeMediaType.XMLUTF8), null));

			if (msgr instanceof TcpMessenger)
			{
				((TcpMessenger) msgr).sendMessageDirect(msg, null, null, true);
			} else
			{
				msgr.sendMessage(msg);
			}
		}

	}
}
