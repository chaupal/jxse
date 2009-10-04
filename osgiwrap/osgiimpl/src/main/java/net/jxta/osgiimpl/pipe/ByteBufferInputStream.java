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
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Does not use byte buffer, however is symmetrical with the bytebuffer output for out purposes. And will directly use byte
 * array events for the stream
 * 
 * @author boylejohnr
 * 
 */
public class ByteBufferInputStream extends InputStream
{
	static Log log = LogFactory.getLog(ByteBufferInputStream.class);
	private static final BufferState WAITING_BYTES = new WaitingBytes(), HAS_BYTES = new HasBytes();
	private static BufferState currentState = WAITING_BYTES;
	private byte[] currentBuffer;
	private ReentrantLock stateLock = new ReentrantLock();
	private Condition waitingForBytes = stateLock.newCondition();
	private Condition waitingToInjectBytes = stateLock.newCondition();

	public ByteBufferInputStream()
	{
	}

	private int bufferPostion = 0;

	/**
	 * Expect that we will need a state model of sorts to reliably support the read state
	 */
	public void injectBytes(byte[] bytes)
	{
		stateLock.lock();
		try
		{
			while (currentState == HAS_BYTES)
			{
				try
				{
					waitingToInjectBytes.await();
				} catch (InterruptedException e)
				{
					log.error(e, e);
				}
			}
			currentState.injectBytes(this, bytes);
		} finally
		{
			stateLock.unlock();
		}
	}

	/**
	 * I think this is a blocking read add will block indefinitely.
	 */
	@Override
	public int read() throws IOException
	{
		stateLock.lock();
		try
		{
			while (currentState == WAITING_BYTES)
			{
				try
				{
					if (!waitingForBytes.await(5000, TimeUnit.MILLISECONDS))
					{
						log.debug("Failed to read in 5000 ms");
						throw new SocketTimeoutException("Failed to read within timeout of 5000");
					}
				} catch (InterruptedException e)
				{
					log.error(e, e);
				}
			}
			return currentState.read(this);
		} finally
		{
			stateLock.unlock();
		}
	}

	// @Override
	// public void close() throws IOException
	// {
	// TODO This should be injecting int -1 not byte
	// injectBytes(new byte[] { -1 });
	// super.close();
	// }

	private interface BufferState
	{
		public int read(ByteBufferInputStream impl);

		public void injectBytes(ByteBufferInputStream impl, byte[] bytes);
	}

	private void switchToWaitingBytes()
	{
		currentState = WAITING_BYTES;
		currentBuffer = null;
		bufferPostion = 0;
		// Signal thread waiting to inject.
		waitingToInjectBytes.signalAll();

	}

	private void switchToHasBytes(byte[] bytes)
	{
		currentBuffer = bytes;
		currentState = HAS_BYTES;
		// Signal thread that has been waiting to read.
		waitingForBytes.signalAll();

	}

	private static final class HasBytes implements BufferState
	{

		@Override
		public int read(ByteBufferInputStream impl)
		{
			// Just move along the buffer
			byte nextByte = impl.currentBuffer[impl.bufferPostion];
			int nextInt = nextByte & 0xff;
			impl.bufferPostion++;
			if (impl.bufferPostion == impl.currentBuffer.length)
			{
				impl.switchToWaitingBytes();
			}
			return nextInt;
		}

		@Override
		public void injectBytes(ByteBufferInputStream impl, byte[] bytes)
		{
			// Going to throw since we should not be able write bytes when
			// current buffer not read!

			throw new IllegalStateException("Should not be trying to write bytes when in state:" + currentState);
		}

	}

	private static final class WaitingBytes implements BufferState
	{

		@Override
		public int read(ByteBufferInputStream impl)
		{
			throw new IllegalStateException("Should not be trying to read bytes when in state:" + currentState);
		}

		@Override
		public void injectBytes(ByteBufferInputStream impl, byte[] bytes)
		{
			log.debug("writing bytes " + bytes.length);
			if (bytes.length != 0)
			{
				impl.switchToHasBytes(bytes);
			}
		}
	}

}
