package net.jxta.util;

import java.io.OutputStream;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.lib.legacy.ClassImposteriser;

public class CountingOutputStreamTest extends MockObjectTestCase {

	private CountingOutputStream stream;
	
	@Override
	protected void setUp() throws Exception {
		setImposteriser(ClassImposteriser.INSTANCE);
		stream = new CountingOutputStream(new DevNullOutputStream());
		super.setUp();
	}
	
	public void testInitiallyZeroBytesWritten() {
		assertEquals(0L, stream.getBytesWritten());
	}
	
	public void testWriteSingleByte() throws Exception {
		stream.write(0);
		assertEquals(1L, stream.getBytesWritten());
	}
	
	public void testWriteMultipleBytes() throws Exception {
		stream.write(new byte[1024]);
		assertEquals(1024L, stream.getBytesWritten());
	}
	
	public void testMultipleConsecutiveWriteCalls() throws Exception {
		stream.write(0);
		stream.write(new byte[100]);
		stream.write(new byte[1]);
		assertEquals(102, stream.getBytesWritten());
	}
	
	public void testWriteMillionsOfBytes() throws Exception {
		byte[] megabyteArray = new byte[1024 * 1024];
		
		long startTime = System.nanoTime();
		for(int i=0; i < 1024; i++) {
			stream.write(megabyteArray);
		}
		long stopTime = System.nanoTime();
		assertEquals(1024 * 1024 * 1024, stream.getBytesWritten());
		long totalTimeTaken = stopTime - startTime;
		System.out.println("Time taken to write 1GB:" + (totalTimeTaken / 1000000.0) + "ms");
	}
	
	
	
	/**
	 * This test will fail in the pre-patch implementation of CountingOutputStream.
	 * The original implementation was pushing all bytes through
	 * OutputStream.write(int b) instead of the byte array methods where
	 * that was appropriate. This was a significant and unnecessary performance
	 * hit.
	 */
	public void testWriteByteArray_usesWriteByteArrayInWrappedStream() throws Exception {
		final OutputStream mockedStream = mock(OutputStream.class);
		final byte[] bytesToWrite = new byte[1024];
		checking(new Expectations() {{
			oneOf(mockedStream).write(bytesToWrite, 0, 1024);
		}});
		
		stream = new CountingOutputStream(mockedStream);
		stream.write(bytesToWrite);
	}
	
	public void testWriteByteArray_passesCorrectParameters() throws Exception {
		final OutputStream mockedStream = mock(OutputStream.class);
		final byte[] bytesToWrite = new byte[1024];
		checking(new Expectations() {{
			oneOf(mockedStream).write(bytesToWrite, 0, 100);
			oneOf(mockedStream).write(bytesToWrite, 100, 24);
			oneOf(mockedStream).write(bytesToWrite, 124, 900);
		}});
		
		stream = new CountingOutputStream(mockedStream);
		stream.write(bytesToWrite, 0, 100);
		stream.write(bytesToWrite, 100, 24);
		stream.write(bytesToWrite, 124, 900);
	}
}
