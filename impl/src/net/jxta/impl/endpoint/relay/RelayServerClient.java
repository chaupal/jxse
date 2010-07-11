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
package net.jxta.impl.endpoint.relay;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;


/**
 * A client of the Relay Server
 */
class RelayServerClient extends AbstractSelectableChannel implements Runnable {

	/**
	 * Logger
	 */
	private static final Logger LOG = Logger.getLogger(RelayServerClient.class.getName());

	/**
	 * the Relay Server of this client
	 */
	private final RelayServer server;

	/**
	 * the peerId of this client
	 */
	private final PeerID clientPeerId;

	/**
	 * The absolute time at which the lease expires.
	 */
	private long leaseExpireAt = 0;

	/**
	 * The time at which the message queue expires.
	 */
	private long queueStallAt = Long.MAX_VALUE;

	/**
	 * The amount of time in relative milliseconds which we will allow the
	 * client to stall (not poll for messages).
	 */
	private final long stallTimeout;

	/**
	 *  The messenger we are currently using to send messages or {@code null} if
	 *  we have no current way to send messages to the client.
	 */
	private Messenger messenger = null;

	/**
	 *  We allow a 1 message queue of high priority messages.
	 */
	private QueuedMessage outOfBandMessage = null;

	/**
	 * A queue of message for this client
	 */
	private final BlockingQueue<QueuedMessage> messageList;

	/**
	 *  Our current set of valid operations.
	 */
	private volatile int readyOps = 0;

	/**
	 *  A message queued for sending to the client.
	 */
	private static class QueuedMessage {
		final Message message;
		final String destService;
		final String destParam;

		QueuedMessage(Message message, String destService, String destParam) {
			this.message = message;
			this.destService = destService;
			this.destParam = destParam;
		}
	}

	RelayServerClient(RelayServer server, PeerID clientPeerId, long leaseLength, long stallTimeout, int clientQueueSize) {
		super(null);

		try {
			configureBlocking(false);
		} catch(IOException impossible ) {
			throw new Error("Unhandled IOException", impossible);
		}

		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.fine("new Client peerId=" + clientPeerId + " lease=" + leaseLength);
		}

		this.server = server;
		this.clientPeerId = clientPeerId;
		this.stallTimeout = stallTimeout;
		messageList = new ArrayBlockingQueue<QueuedMessage>(clientQueueSize);

		// initialize the lease
		renewLease(leaseLength);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object target) {
		if(target == this) {
			return true;
		}

		if(target instanceof RelayServerClient) {
			return clientPeerId.equals(((RelayServerClient) target).clientPeerId);
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return clientPeerId.hashCode();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p/>Send all of the queued messages to the client.
	 */
	public void run() {
		try {
			if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
				LOG.info("Sending queued messages for " + this);
			}

			int failedInARow = 0;

			// We only last as long as the client channel remains open.
			while(isOpen()) {
				Messenger useMessenger;
				QueuedMessage message;
				boolean wasOOB;

				synchronized (this) {
					// No messenger? Nothing for us to do.
							if( null == messenger) {
								break;
							}

							// If our messenger is unusable, quit.
							if (0 == (messenger.getState() & Messenger.USABLE)) {
								queueStallAt = Math.min(queueStallAt, TimeUtils.toAbsoluteTimeMillis(stallTimeout));
								messenger = null;
								break;
							}

							if (outOfBandMessage != null) {
								message = outOfBandMessage;
								outOfBandMessage = null;
								wasOOB = true;
							} else {
								message = messageList.poll();
								wasOOB = false;
							}

							// No messages? We are now inactive.
									if(null == message) {
										setReadyOps(0);
										break;
									}

									useMessenger = messenger;
				}

				// send the message
				try {
					useMessenger.sendMessageB(message.message, message.destService, message.destParam);

					// A message was sent. Queue is no longer stalled.
					synchronized (this) {
						failedInARow = 0;
						queueStallAt = Long.MAX_VALUE;
					}
				} catch (Exception e) {
					// Check that the exception is not due to the message rather
					// than the messenger, and then drop the message. In this
					// case we give the messenger the benefit of the doubt and
					// keep it open, renewing the lease as above. (this could be
					// the last message). For now the transports do not tell the
					// difference, so we count the number of times we failed in
					// a row. After three times, kill the message rather than
					// the messenger.

					// put the message back
					synchronized (this) {
						if (++failedInARow >= 3) {
							failedInARow = 0;
							queueStallAt = Long.MAX_VALUE;
							continue;
						}

						// Ok, if we cannot push back the message, below, we 
						// should reset failedInARow, since we won't be retrying
						// the same message. But it does not realy matter so 
						// let's keep things simple.
						if (outOfBandMessage == null) {
							outOfBandMessage = message;
						}

						// If we are still holding the same messenger, kill it.
						if(useMessenger == messenger) {
							queueStallAt = Math.min(queueStallAt, TimeUtils.toAbsoluteTimeMillis(stallTimeout));
							messenger = null;
						} else {
							useMessenger = null;
						}
					}

					// If we're here, we decided to close the messenger. We do
					// that out of sync.
					if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
						LOG.log(Level.INFO, "Giving up on unusable messenger : " + useMessenger, e);
					}

					if(null != useMessenger) {
						useMessenger.close();
						useMessenger = null;
					}
				}
			}
		} catch (Throwable all) {
			if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
			}
		} finally {
			if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
				LOG.fine("Stopped sending queued messages for " + this);
			}

			// Re-register with the selector for future messages.
			synchronized(this) {
				if((null != messenger) && isOpen()) {
					try {
						register(server.selector, SelectionKey.OP_WRITE, null);
						server.selector.wakeup();
					} catch(ClosedChannelException betterNotBe) {
						if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
							LOG.log(Level.SEVERE, "Channel unexpectedly closed!", betterNotBe);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return super.toString() + "[" + clientPeerId + ","
		+ getQueueSize() + ","
		+ (messenger == null ? "-m" : "+m") + ","
		+ TimeUtils.toRelativeTimeMillis(queueStallAt) + ","
		+ TimeUtils.toRelativeTimeMillis(leaseExpireAt)
		+ "]";
	}

	/**
	 *  {@inheritDoc}
	 */
	protected void implCloseSelectableChannel() throws IOException {
		if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
			LOG.info( "Closing " + this);
		}

		Messenger messengerToClose = messenger;
		messenger = null;
		if(null != messengerToClose) {
			messengerToClose.close();
		}

		queueStallAt = 0;
		leaseExpireAt = 0;

		messageList.clear();
	}

	/**
	 *  {@inheritDoc}
	 */
	protected void implConfigureBlocking(boolean block) throws IOException {
	}

	/**
	 *  {@inheritDoc}
	 */
	public int validOps() {
		return SelectionKey.OP_WRITE;
	}

	/**
	 *  Return the current Ops state.
	 *
	 *  @return the current ops state, if any.
	 */
	int readyOps() {
		return readyOps;
	}

	/**
	 *  Adjust the current Ops state.
	 *
	 *  @param readyOps the new ops state.
	 */
	private void setReadyOps(int readyOps) {
		this.readyOps = readyOps;
		ClientSelectionKey key = (ClientSelectionKey) keyFor(server.selector);
		if(null != key) {
			key.setReadyOps(readyOps);
		}
	}

	/**
	 *  Creates a new selection key for the specified selector.
	 *
	 *  @param selector The target selector.
	 *  @param ops The initial interest ops.
	 *  @param att The attached object.
	 */
	ClientSelectionKey newSelectionKey(RelayServer.ClientSelector selector, int ops, Object att) {
		return new ClientSelectionKey(selector, ops, att);
	}

	/**
	 *  Returns the number of items we have queued for the client.
	 *
	 *  @return The number of queued messages including the out of band message.
	 */
	private int getQueueSize() {
		return (null != outOfBandMessage) ? 1 : 0 + messageList.size();
	}

	/**
	 * Remove all queued messages. The Out of band message (if any) is retained.
	 */
	void flushQueue() {
		messageList.clear();
	}

	/**
	 *  If {@code true} then:
	 *  <ul>
	 *      <li>This client connection has been closed.</li>
	 *      <li>The client lease has expired.</li>
	 *      <li>The client has failed to poll recently.</li>
	 *  </ul>
	 *
	 * @return {@code true} if this client is no longer usable otherwise
	 * {@code false}.
	 */
	boolean isExpired() {
		long now = TimeUtils.timeNow();
		boolean isExpired = !isOpen() || (now > leaseExpireAt) || (now > queueStallAt);

		if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
			LOG.finer( this + " : isExpired() = " + isExpired );
		}

		return isExpired;
	}

	long getLeaseRemaining() {
		return TimeUtils.toRelativeTimeMillis(leaseExpireAt);
	}

	protected PeerID getClientPeerId() {
		return clientPeerId;
	}

	boolean renewLease(long leaseLength) {
		// It is ok to renew a lease past the expiration time, as long as the
		// client has not been closed yet.

		if (!isOpen()) {
			return false;
		}

		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.fine( this + " : additional lease = " + leaseLength );
		}

		leaseExpireAt = TimeUtils.toAbsoluteTimeMillis(leaseLength);

		return true;
	}

	/**
	 *  Replace the messenger currently in use with a new messenger.
	 *
	 *  @param newMessenger The messenger to be used to send to the client.
	 *  @return If {@code true} then the new messenger has been accepted and
	 *  will be used to send messages to the client. If {@code false} then the
	 *  messenger will not be used.
	 */
	boolean addMessenger(Messenger newMessenger) {
		// make sure we are being passed a valid messenger
		if ((null == newMessenger) || (0 == (newMessenger.getState() & Messenger.USABLE))) {
			if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
				LOG.fine("Ignorning bad messenger (" + newMessenger + ")");
			}

			return false;
		}

		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.fine("New messenger : " + newMessenger );
		}

		// Unless we change our mind, we'll close the new messenger.
		// If we do not keep it, we must close it. Otherwise the client on the
		// other end will never know what happened.
		// Its connection will be left hanging for a long time.
		Messenger messengerToClose = newMessenger;

		synchronized (this) {
			if (isOpen()) {
				// Swap messengers; we'll close the old one if there was one.
				messengerToClose = messenger;
				messenger = newMessenger;

				if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
					LOG.fine("Messenger (" + messenger + ")");
				}

				// If we had no previous messenger then register this channel.
				if(null == messengerToClose) {
					try {
						register(server.selector, SelectionKey.OP_WRITE, null);
						server.selector.wakeup();
					} catch(ClosedChannelException betterNotBe) {
						if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
							LOG.log(Level.SEVERE, "Channel unexpectedly closed!", betterNotBe);
						}
					}
				}

				// If we have waiting messages, select this channel.
				if (getQueueSize() > 0) {
					setReadyOps(SelectionKey.OP_WRITE);
				}
			}
		}

		// Now that we are out of sync, close the unused messenger.
		// In either case, we claim that we kept the new one.
		if (messengerToClose != null) {
			if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
				LOG.fine("Closing messenger : " + messengerToClose );
			}
			messengerToClose.close();
		}

		return true;
	}

	/**
	 * Add a message to the tail of the list
	 *
	 * @param message The message to be enqueued.
	 * @param outOfBand if true, indicates outbound
	 * @return {@code true} if the message was enqueued otherwise {@code false}.
	 */
	private boolean queueMessage(Message message, String destService, String destParam, boolean outOfBand) {
		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.fine("queueMessage for " + this);
		}

		synchronized (this) {
			if (!isOpen()) {
				return false;
			}

			QueuedMessage qm = new QueuedMessage(message, destService, destParam);

			if (outOfBand) {
				// We have a single oob message pending.
				outOfBandMessage = qm;
			} else {
				// We will simply discard the new msg when the queue is full
				// to avoid penalty of dropping earlier reliable message
				if (!messageList.offer(qm)) {
					if (Logging.SHOW_WARNING) {
						LOG.warning("Dropping " + message + " for peer " + clientPeerId);
					}
				}
				else if(Logging.SHOW_INFO && (messageList.size() % 50 == 0) )
					LOG.info("Message queue size for client " + clientPeerId + " now " + messageList.size());
			}

			// Normally, if messenger is null we knew it already:
			// it becomes null only when we detect that it breaks while trying
			// to send. However, let's imagine it's possible that we never had
			// one so far. Be careful that this is not a one-time event; we
			// must not keep renewing the short lease; that would ruin it's
			// purpose.
			if ((null == messenger) || (0 == (messenger.getState() & Messenger.USABLE))) {
				queueStallAt = Math.min(queueStallAt, TimeUtils.toAbsoluteTimeMillis(stallTimeout));
			} else {
				setReadyOps(SelectionKey.OP_WRITE);
			}
		}

		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
			LOG.fine("done queueMessage for " + this );
		}

		return true;
	}

	/**
	 *  {@inheritDoc}
	 */
	Messenger getMessenger(EndpointAddress destAddr, boolean outOfBand) {
		return new RelayMessenger(destAddr, outOfBand);
	}

	private class RelayMessenger extends BlockingMessenger {
		private final boolean outOfBand;

		public RelayMessenger(EndpointAddress destAddress, boolean outOfBand) {
			// We do not use self destruction
			super(RelayServerClient.this.server.group.getPeerGroupID(),
			      destAddress,
			      RelayServerClient.this.server.group.getTaskManager(),
			      false);

			this.outOfBand = outOfBand;
		}

		/*
		 * The cost of just having a finalize routine is high. The finalizer is
		 * a bottleneck and can delay garbage collection all the way to heap
		 * exhaustion. Leave this comment as a reminder to future maintainers.
		 * Below is the reason why finalize is not needed here.
		 *
		 * This is never given to application layers directly. No need
		 * to close-on-finalize.
		 *

        protected void finalize() {
        }

		 */

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isIdleImpl() {
			// We do not use self destruction
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void closeImpl() {
			// Nothing to do. The underlying connection is not affected.
			// The messenger will be marked closed by the state machine once completely down; that's it.
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public EndpointAddress getLogicalDestinationImpl() {
			return new EndpointAddress(RelayServerClient.this.clientPeerId, null, null);
		}

		/**
		 *   {@inheritDoc}
		 *
		 * <p/>Send messages. Messages are queued and then processed when there is a transport messenger.
		 */
		@Override
		public void sendMessageBImpl(Message message, String serviceName, String serviceParam) throws IOException {
			if (!RelayServerClient.this.isOpen()) {
				IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
				if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
					LOG.log(Level.WARNING, failure.getMessage(), failure);
				}
				throw failure;
			}

			// Prepare the final destination address of the message
			EndpointAddress useAddr = getDestAddressToUse(serviceName, serviceParam);

			// simply enqueue the message.
			// We clone it, since we pretend it's been sent synchronously.
			if(!RelayServerClient.this.queueMessage(message.clone(), useAddr.getServiceName(), useAddr.getServiceParameter(), outOfBand)) {
				throw new IOException("Message could not be queued.");
			}
		}
	}

	/**
	 *  Our Selection key
	 */
	class ClientSelectionKey extends AbstractSelectionKey {

		private final RelayServer.ClientSelector selector;
		private volatile int ops;

		private volatile int readyOps = 0;

		ClientSelectionKey(RelayServer.ClientSelector selector, int ops, Object att) {
			this.selector = selector;
			interestOps(ops);
			attach(att);
		}

		/**
		 *  {@inheritDoc}
		 */
		 public RelayServerClient channel() {
			 return RelayServerClient.this;
		 }

		 /**
		  *  {@inheritDoc}
		  */
		 public Selector selector() {
			 return selector;
		 }

		 /**
		  *  {@inheritDoc}
		  */
		 public int interestOps() {
			 return ops;
		 }

		 /**
		  *  {@inheritDoc}
		  */
		 public ClientSelectionKey interestOps(int ops) {
			 if(!isValid())
				 throw new CancelledKeyException();

			 this.ops = ops;

			 return this;
		 }

		 /**
		  *  {@inheritDoc}
		  */
		 public int readyOps() {
			 if(!isValid())
				 throw new CancelledKeyException();

			 return readyOps;
		 }

		 /**
		  *  Set the readyOps for this key.
		  *
		  *  @param readyOps The new ops value.
		  */
		 public void setReadyOps(int readyOps) {
			 if(!isValid())
				 throw new CancelledKeyException();

			 this.readyOps = readyOps;
			 selector.keyChanged(this);
		 }
	}    
}
