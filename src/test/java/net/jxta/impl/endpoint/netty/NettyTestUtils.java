package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.DownstreamChannelStateEvent;

public class NettyTestUtils {
    
    public static ByteBuffer convertReadable(ChannelBuffer b) {
        int startIndex = b.readerIndex();
        ByteBuffer converted = ByteBuffer.allocate(b.readableBytes());
        b.readBytes(converted);
        b.readerIndex(startIndex);
        converted.flip();
        return converted;
    }
    
    public static void assertEquals(ChannelBuffer expected, ChannelBuffer actual) {
        if(expected.readableBytes() != actual.readableBytes()) {
            Assert.assertEquals("channel buffers have differing readable sizes", expected.readableBytes(), actual.readableBytes());
        }
        
        int startPositionExpected = expected.readerIndex();
        int startPositionActual = actual.readerIndex();
        int position = 0;
        while(expected.readable()) {
            byte expectedByte = expected.readByte();
            byte actualByte = actual.readByte(); 
            if(expectedByte != actualByte) {
                Assert.assertEquals("channel buffers differ at position " + position, expectedByte, actualByte);
            }
            
            position++;
        }
        
        expected.readerIndex(startPositionExpected);
        actual.readerIndex(startPositionActual);
    }
    
    public static boolean checkEquals(ChannelBuffer expected, ChannelBuffer actual) {
        if(expected.readableBytes() != actual.readableBytes()) {
            return false;
        }
        
        int position = 0;
        while(expected.readable()) {
            byte expectedByte = expected.readByte();
            byte actualByte = actual.readByte(); 
            if(expectedByte != actualByte) {
                return false;
            }
            
            position++;
        }
        
        return true;
    }
    
    public static List<ChannelBuffer> splitIntoChunks(int chunkSize, ChannelBuffer... buffers) {
        LinkedList<ChannelBuffer> chunks = new LinkedList<ChannelBuffer>();
        
        ArrayList<ChannelBuffer> sourceBuffers = new ArrayList<ChannelBuffer>();
        Collections.addAll(sourceBuffers, buffers);
        Iterator<ChannelBuffer> sourceIter = sourceBuffers.iterator();
        ChannelBuffer chunk = ChannelBuffers.buffer(chunkSize);
        while(sourceIter.hasNext()) {
            ChannelBuffer source = sourceIter.next();
            
            int index = source.readerIndex();
            while(source.writerIndex() > index) {
                int fragmentSize = Math.min(source.writerIndex() - index, chunk.writableBytes());
                chunk.writeBytes(source, index, fragmentSize);
                if(!chunk.writable()) {
                    chunks.add(chunk);
                    chunk = ChannelBuffers.buffer(chunkSize);
                }
                index += fragmentSize;
            }
        }
        
        if(chunk.readable()) {
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    public static void checkDownstreamChannelStateEvent(ChannelEvent ev, ChannelState expectedState, Boolean expectedValue) {
        assertTrue(ev instanceof DownstreamChannelStateEvent);
        DownstreamChannelStateEvent stateEv = (DownstreamChannelStateEvent)ev;
        Assert.assertEquals(expectedState, stateEv.getState());
        Assert.assertEquals(expectedValue, stateEv.getValue());
    }
    
}
