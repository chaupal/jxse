package net.jxta.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a hack to improve thread usage of this class. Used extensively for communication and large numbers of groups, key
 * that efficient.
 * 
 * @author boylejohnr
 * 
 */
public class JxtaServerPipeNonBlockingAccept extends JxtaServerPipe
{
	private static Log log = LogFactory.getLog(JxtaServerPipeNonBlockingAccept.class);
	/**
	 * This was per class, now for all instances and uses a cached pool instead.
	 */
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private final ConnectionListener listener;

	public JxtaServerPipeNonBlockingAccept(PeerGroup netPeerGroup, PipeAdvertisement adv, ConnectionListener listener)
			throws IOException
	{
		super(netPeerGroup, adv);
		this.listener = listener;
		if (listener == null)
		{
			throw new IllegalArgumentException("Must have non null listener");
		}
	}

	private void unsetParentExecutorHack()
	{
		try
		{
			Field field = JxtaServerPipe.class.getDeclaredField("executor");
			field.setAccessible(true);
			// TODO: Need to promote change to the JXTA community to remove this
			// class completely.
			ExecutorService executorLocal = (ExecutorService) field.get(this);
			executorLocal.shutdown();
		} catch (Exception e)
		{
			throw new Error("Unable to clean up thread executor created by JXTAServerPipe", e);
		}
	}

	/**
	 * In order to make none blocking need to override the pipe event, direct lift.
	 */
	@Override
	public void pipeMsgEvent(PipeMsgEvent event)
	{
		Message message = event.getMessage();
		if (message == null)
		{
			return;
		}
		ConnectionProcessor processor = new ConnectionProcessor(message);
		executor.execute(processor);
	}

	/**
	 * Direct lift from parent.
	 * 
	 * @author boylejohnr
	 * 
	 */
	private class ConnectionProcessor implements Runnable
	{

		private Message message;

		ConnectionProcessor(Message message)
		{
			this.message = message;
		}

		public void run()
		{
			JxtaBiDiPipe bidi = processMessage(message);
			// make sure we have a socket returning
			if (bidi != null)
			{
				// Instead of putting message on queue for the accept, will
				// now simply call back there is no other way to construct
				// this class.
				listener.onAccept(bidi);
			}
		}
	}

	@Override
	@Deprecated
	public JxtaBiDiPipe accept()
	{
		throw new UnsupportedOperationException("Accept is not supported on this class, must use listener");
	}

	public interface ConnectionListener
	{
		/**
		 * Expected that the listener deal with this message as quickly as possible or create thread to handle the ongoing work
		 * and accept immediately.
		 * 
		 * @param pipe
		 */
		public void onAccept(JxtaBiDiPipe pipe);
	}

	/**
	 * This is a direct lift from the parent, to enable thread reduction. Log messages needed to be converted and the group
	 * accessed through method.
	 * 
	 * @param msg
	 * @return
	 */
	protected JxtaBiDiPipe processMessage(Message msg)
	{

		PipeAdvertisement outputPipeAdv = null;
		PeerAdvertisement peerAdv = null;
		StructuredDocument credDoc = null;
		try
		{
			MessageElement el = msg.getMessageElement(nameSpace, credTag);

			if (el != null)
			{
				credDoc = StructuredDocumentFactory.newStructuredDocument(el);
			}

			el = msg.getMessageElement(nameSpace, reqPipeTag);
			if (el != null)
			{
				XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(el);
				outputPipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
			}

			el = msg.getMessageElement(nameSpace, remPeerTag);
			if (el != null)
			{
				XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(el);
				peerAdv = (PeerAdvertisement) AdvertisementFactory.newAdvertisement(asDoc);
			}

			el = msg.getMessageElement(nameSpace, reliableTag);
			boolean isReliable = false;
			if (el != null)
			{
				isReliable = Boolean.valueOf((el.toString()));
				if (log.isTraceEnabled())
				{
					log.trace("Connection request [isReliable] :" + isReliable);
				}
			}

			el = msg.getMessageElement(nameSpace, directSupportedTag);
			boolean directSupported = false;
			if (el != null)
			{
				directSupported = Boolean.valueOf((el.toString()));
				if (log.isTraceEnabled())
				{
					log.trace("Connection request [directSupported] :" + directSupported);
				}
			}

			Messenger msgr;
			boolean direct = false;
			if (directSupported)
			{
				msgr = JxtaBiDiPipe.getDirectMessenger(getGroup(), outputPipeAdv, peerAdv);
				if (msgr == null)
				{
					msgr = JxtaBiDiPipe.lightweightOutputPipe(getGroup(), outputPipeAdv, peerAdv);
				} else
				{
					direct = true;
				}
			} else
			{
				msgr = JxtaBiDiPipe.lightweightOutputPipe(getGroup(), outputPipeAdv, peerAdv);
			}

			if (msgr != null)
			{
				if (log.isTraceEnabled())
				{
					log.trace("Reliability set to :" + isReliable);
				}
				PipeAdvertisement newpipe = newInputPipe(getGroup(), outputPipeAdv);
				JxtaBiDiPipe pipe = new JxtaBiDiPipe(getGroup(), msgr, newpipe, credDoc, isReliable, direct);

				pipe.setRemotePeerAdvertisement(peerAdv);
				pipe.setRemotePipeAdvertisement(outputPipeAdv);
				sendResponseMessage(getGroup(), msgr, newpipe);
				return pipe;
			}
		} catch (IOException e)
		{
			// deal with the error
			if (log.isTraceEnabled())
			{
				log.error("IOException occured", e);
			}
		}
		return null;
	}

}
