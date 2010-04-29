
package net.jxta.util;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CountingOutputStreamTest {

    private FakeOutputStream underlying;
	private CountingOutputStream stream;
	
	@Before
	public void setUp() throws Exception {
	    underlying = new FakeOutputStream();
		stream = new CountingOutputStream(underlying);
	}
	
	@Test
	public void testInitiallyZeroBytesWritten() {
	    assertEquals(0L, stream.getBytesWritten());
	}
	
	@Test
	public void testWriteSingleByte() throws Exception {
		stream.write(0);
		assertEquals(1L, stream.getBytesWritten());
	}
	
	@Test
	public void testWriteMultipleBytes() throws Exception {
		stream.write(new byte[1024]);
		assertEquals(1024L, stream.getBytesWritten());
	}
	
	@Test
	public void testMultipleConsecutiveWriteCalls() throws Exception {
		stream.write(0);
		stream.write(new byte[100]);
		stream.write(new byte[1]);
		assertEquals(102, stream.getBytesWritten());
	}
	
	@Test
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
	@Test
	public void testWriteByteArray_usesWriteByteArrayInWrappedStream() throws Exception {
		final byte[] bytesToWrite = new byte[1024];
		stream.write(bytesToWrite);
		
		// there should be a single byte array, of size 1024
		assertEquals(1, underlying.writtenBytes.size());
		assertEquals(1024, underlying.writtenBytes.get(0).length);
	}
	
	private class FakeOutputStream extends OutputStream {

	    private List<byte[]> writtenBytes = new LinkedList<byte[]>();
	    
        @Override
        public void write(int b) throws IOException {
            writtenBytes.add(new byte[] { (byte)b });
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            writtenBytes.add(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if(off == 0 && len == b.length) {
                write(b);
            } else {
                byte[] written = new byte[len];
                System.arraycopy(b, off, written, 0, len);
                writtenBytes.add(written);
            }
        }
	}
}
