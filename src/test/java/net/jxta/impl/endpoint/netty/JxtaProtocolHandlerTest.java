package net.jxta.impl.endpoint.netty;

import static net.jxta.impl.endpoint.netty.NettyTestUtils.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.peer.PeerID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamChannelStateEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.timeout.TimeoutException;
import org.junit.Before;
import org.junit.Test;

public class JxtaProtocolHandlerTest {

    public static final String TEST_PROTO_NAME = "htun";
    
    private static final InetAddress SERVER_BIND_ADDR = createAddress(new byte[] { 0, 0, 0, 0 });
    private static final InetAddress LOCAL_ADDR = createAddress(new byte[] { 10, 1, 1, 1 });
    private static final SocketAddress PARENT_SOCK_ADDR = new InetSocketAddress(SERVER_BIND_ADDR, 12345);
    
    private static final SocketAddress LOCAL_SOCK_ADDR = new InetSocketAddress(LOCAL_ADDR, 57043);
    private static final EndpointAddress LOCAL_ENDPOINT_ADDR = new EndpointAddress(TEST_PROTO_NAME, "10.1.1.1:12345", null, null);

    private static final SocketAddress REMOTE_SOCK_ADDR = InetSocketAddress.createUnresolved("remoteaddr", 54321);
    private static final EndpointAddress REMOTE_ENDPOINT_ADDR = new EndpointAddress(TEST_PROTO_NAME, "remoteaddr:54321", null, null);
    private static final EndpointAddress REMOTE_ENDPOINT_ADDR_WITH_PARAMS = new EndpointAddress(TEST_PROTO_NAME, "remoteAddr:54321", "TestService", "testparams");
    
    private static final PeerID LOCAL_PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503304F8E1DEBB4942C0BF16DD923DEC949803"));
    private static final PeerID REMOTE_PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E50472050325033E7E1335996F44E38BD66B16349BB1F1E03"));


    private static InetAddress createAddress(byte[] b) {
        try {
            return InetAddress.getByAddress(b);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Bad address in test");
        }
    }

    private FakeChannel channel;
    private JxtaProtocolHandler handler;
    private FakeTimer timeoutTimer;
    private FakeChannelSink downstreamCatcher;
    private UpstreamEventCatcher upstreamCatcher;
    private Message testMessage;
    
    @Before
    public void setUp() throws Exception {
        timeoutTimer = new FakeTimer();
        handler = new JxtaProtocolHandler(new InetSocketAddressTranslator(TEST_PROTO_NAME), LOCAL_PEER_ID, timeoutTimer, REMOTE_ENDPOINT_ADDR_WITH_PARAMS, LOCAL_ENDPOINT_ADDR);
        downstreamCatcher = new FakeChannelSink();
        ChannelPipeline pipeline = Channels.pipeline();
        upstreamCatcher = new UpstreamEventCatcher();
        pipeline.addLast(JxtaProtocolHandler.NAME, handler);
        pipeline.addLast(UpstreamEventCatcher.NAME, upstreamCatcher);
        FakeChannel parent = new FakeChannel(null, null, Channels.pipeline(), new FakeChannelSink());
        parent.localAddress = PARENT_SOCK_ADDR;
        parent.bound = true;
        parent.connected = false;
        channel = new FakeChannel(parent, null, pipeline, downstreamCatcher);
        channel.localAddress = LOCAL_SOCK_ADDR;
        channel.remoteAddress = REMOTE_SOCK_ADDR;
        channel.bound = true;
        channel.connected = true;
        
        testMessage = createTestMessage();
    }

    @Test
    public void testSendsWelcomeMessageImmediately() throws Exception {
        final WelcomeMessage expectedWelcomeMessage = new WelcomeMessage(REMOTE_ENDPOINT_ADDR_WITH_PARAMS, LOCAL_ENDPOINT_ADDR, LOCAL_PEER_ID, false);
        
        Channels.fireChannelConnected(channel, REMOTE_SOCK_ADDR);
        assertEquals(1, downstreamCatcher.events.size());
        ChannelEvent event = downstreamCatcher.events.poll();
        
        assertTrue(event instanceof DownstreamMessageEvent);
        DownstreamMessageEvent msgEv = (DownstreamMessageEvent)event;
        
        assertTrue(msgEv.getMessage() instanceof ChannelBuffer);
        ChannelBuffer sentData = (ChannelBuffer)msgEv.getMessage();
        ByteBuffer data = convertReadable(sentData);
        
        WelcomeMessage sent = new WelcomeMessage();
        assertTrue(sent.read(data));
        assertEquals(expectedWelcomeMessage.getWelcomeString(), sent.getWelcomeString());
    }
    
    @Test
    public void testSignalsConnectedOnceReceiveWelcomeMessage() throws Exception {
        emulateConnect();
        ChannelBuffer welcomeBytes = createRemoteWelcomeMessageBuffer();
        Channels.fireMessageReceived(channel, welcomeBytes);
        
        assertEquals(2, upstreamCatcher.events.size());
        checkIsWelcomeMessage(upstreamCatcher.events.poll());
        checkUpstreamChannelStateEvent(upstreamCatcher.events.poll(), ChannelState.CONNECTED, REMOTE_SOCK_ADDR);
    }
    
    private void checkIsWelcomeMessage(ChannelEvent ev) {
        assertTrue(ev instanceof UpstreamMessageEvent);
        assertTrue(((UpstreamMessageEvent)ev).getMessage() instanceof WelcomeMessage);
    }

    @Test
    public void testReceiveWelcomeMessageInChunks() throws Exception {
        emulateConnect();
        final WelcomeMessage receivedWelcomeMessage = new WelcomeMessage(LOCAL_ENDPOINT_ADDR, REMOTE_ENDPOINT_ADDR, REMOTE_PEER_ID, false);
        
        ByteBuffer allBytes = receivedWelcomeMessage.getByteBuffer();
        List<ChannelBuffer> parts = splitIntoChunks(4, ChannelBuffers.wrappedBuffer(allBytes));
        
        for(ChannelBuffer part : parts.subList(0, parts.size() - 1)) {
            Channels.fireMessageReceived(channel, part);
            checkQueuesEmpty();
        }
        
        Channels.fireMessageReceived(channel, parts.get(parts.size() - 1));
        assertEquals(2, upstreamCatcher.events.size());
        checkIsWelcomeMessage(upstreamCatcher.events.poll());
        checkUpstreamChannelStateEvent(upstreamCatcher.events.poll(), ChannelState.CONNECTED, REMOTE_SOCK_ADDR);
    }
    
    @Test
    public void testWelcomeMessageTimeout() throws Exception {
        Channels.fireChannelConnected(channel, REMOTE_SOCK_ADDR);
        assertEquals(1, timeoutTimer.numRegisteredTimeouts());
        
        // we don't care about anything sent up or downstream at this point
        clearQueues();
        Channels.fireExceptionCaught(channel, new TimeoutException());
        assertEquals(1, downstreamCatcher.events.size());
        ChannelEvent ev = downstreamCatcher.events.poll();
        checkDownstreamChannelStateEvent(ev, ChannelState.OPEN, Boolean.FALSE);
    }
    
    @Test
    public void testTimerRemovedAfterWelcomeReceived() throws Exception {
        emulateEstablished();
        assertEquals(0, timeoutTimer.numRegisteredTimeouts());
    }
    
    @Test
    public void testReceiveEmptyFramedMessage() throws IOException {
        emulateEstablished();
        
        MessagePackageHeader header = createHeader(ChannelBuffers.buffer(0)); 
        ChannelBuffer headerBuffer = ChannelBuffers.wrappedBuffer(header.getByteBuffer());
        Channels.fireMessageReceived(channel, headerBuffer);
        
        assertEquals(1, upstreamCatcher.events.size());
        ChannelEvent event = upstreamCatcher.events.poll();
        ChannelBuffer unwrappedMessage = checkIsUpstreamMessageEventContainingSerializedMessage(event, WireFormatMessageFactory.DEFAULT_WIRE_MIME);
        assertEquals(0, unwrappedMessage.readableBytes());
    }
    
    @Test
    public void testReceiveFramedMessage() throws IOException {
        emulateEstablished();
        
        ChannelBuffer messageContents = serializeMessage(testMessage);
        
        MessagePackageHeader header = createHeader(messageContents);
        ChannelBuffer messageBuffer = ChannelBuffers.wrappedBuffer(ChannelBuffers.wrappedBuffer(header.getByteBuffer()), messageContents);
        Channels.fireMessageReceived(channel, messageBuffer);
        
        assertEquals(1, upstreamCatcher.events.size());
        ChannelEvent event = upstreamCatcher.events.poll();
        ChannelBuffer unwrappedMessage = checkIsUpstreamMessageEventContainingSerializedMessage(event, WireFormatMessageFactory.DEFAULT_WIRE_MIME);
        assertEquals(messageContents, unwrappedMessage);
    }
    
    @Test
    public void testReceiveFramedMessageInChunks() throws IOException {
        emulateEstablished();
        
        ChannelBuffer messageContents = serializeMessage(testMessage);
        
        MessagePackageHeader header = createHeader(messageContents); 
        ChannelBuffer headerBuffer = ChannelBuffers.wrappedBuffer(header.getByteBuffer());
        
        List<ChannelBuffer> parts = splitIntoChunks(5, headerBuffer, messageContents);
        
        for(ChannelBuffer part : parts.subList(0, parts.size()-1)) {
            Channels.fireMessageReceived(channel, part);
            checkQueuesEmpty();
        }
        
        Channels.fireMessageReceived(channel, parts.get(parts.size() - 1));
        assertEquals(1, upstreamCatcher.events.size());
        ChannelEvent event = upstreamCatcher.events.poll();
        ChannelBuffer unwrappedMessage = checkIsUpstreamMessageEventContainingSerializedMessage(event, WireFormatMessageFactory.DEFAULT_WIRE_MIME);
        assertEquals(messageContents, unwrappedMessage);
    }
    
    @Test
    public void receiveWelcomeAndMessagesInOverlappingChunks() throws IOException {
        emulateConnect();
        
        ChannelBuffer welcomeBuffer = createRemoteWelcomeMessageBuffer();
        ChannelBuffer serializedMessage = serializeMessage(testMessage);
        ChannelBuffer messageBuffer = createFramedMessage(serializedMessage);
        ChannelBuffer messageBuffer2 = createFramedMessage(serializedMessage);
        
        List<ChannelBuffer> parts = splitIntoChunks(7, welcomeBuffer, messageBuffer, messageBuffer2);
        
        for(ChannelBuffer part : parts) {
            Channels.fireMessageReceived(channel, part);
        }
        
        assertEquals(4, upstreamCatcher.events.size());
        checkIsWelcomeMessage(upstreamCatcher.events.poll());
        checkUpstreamChannelStateEvent(upstreamCatcher.events.poll(), ChannelState.CONNECTED, REMOTE_SOCK_ADDR);
        assertEquals(serializedMessage, checkIsUpstreamMessageEventContainingSerializedMessage(upstreamCatcher.events.poll(), WireFormatMessageFactory.DEFAULT_WIRE_MIME));
        assertEquals(serializedMessage, checkIsUpstreamMessageEventContainingSerializedMessage(upstreamCatcher.events.poll(), WireFormatMessageFactory.DEFAULT_WIRE_MIME));
    }
    
    @Test
    public void receiveWelcomeAndMessagesInSingleChunk() throws IOException {
        emulateConnect();
        
        ChannelBuffer welcomeBuffer = createRemoteWelcomeMessageBuffer();
        ChannelBuffer serializedMessage = serializeMessage(testMessage);
        ChannelBuffer messageBuffer = createFramedMessage(serializedMessage);
        ChannelBuffer messageBuffer2 = createFramedMessage(serializedMessage);
        
        ChannelBuffer combined = ChannelBuffers.wrappedBuffer(welcomeBuffer, messageBuffer, messageBuffer2);
        Channels.fireMessageReceived(channel, combined);
        
        assertEquals(4, upstreamCatcher.events.size());
        checkIsWelcomeMessage(upstreamCatcher.events.poll());
        checkUpstreamChannelStateEvent(upstreamCatcher.events.poll(), ChannelState.CONNECTED, REMOTE_SOCK_ADDR);
        assertEquals(serializedMessage, checkIsUpstreamMessageEventContainingSerializedMessage(upstreamCatcher.events.poll(), WireFormatMessageFactory.DEFAULT_WIRE_MIME));
        assertEquals(serializedMessage, checkIsUpstreamMessageEventContainingSerializedMessage(upstreamCatcher.events.poll(), WireFormatMessageFactory.DEFAULT_WIRE_MIME));
    }
    
    @Test
    public void testSendMessageWrapsWithFrame() throws IOException {
        emulateEstablished();
        
        
        
        ChannelBuffer messageContents = serializeMessage(testMessage);
        MessagePackageHeader header = createHeader(messageContents);
        SerializedMessage message = new SerializedMessage(header, messageContents);
        
        ChannelBuffer headerBuffer = ChannelBuffers.wrappedBuffer(header.getByteBuffer());
        ChannelBuffer fullFrame = ChannelBuffers.wrappedBuffer(headerBuffer, messageContents);
        
        Channels.write(channel, message);
        
        assertEquals(1, downstreamCatcher.events.size());
        ChannelEvent event = downstreamCatcher.events.poll();
        assertTrue(event instanceof DownstreamMessageEvent);
        assertEquals(fullFrame, checkIsMessageEventContainingBuffer(event));
    }
    
    @Test
    public void testSendIllegallyLargeWelcomeMessage() throws Exception {
        emulateConnect();
        
        ChannelBuffer fakeTooLongWelcomeMessage = ChannelBuffers.buffer(JxtaProtocolHandler.MAX_WELCOME_MESSAGE_SIZE + 1);
        fakeTooLongWelcomeMessage.writeBytes("JXTAHELLO ".getBytes("UTF-8"));
        fakeTooLongWelcomeMessage.writerIndex(fakeTooLongWelcomeMessage.capacity());
        Channels.fireMessageReceived(channel, fakeTooLongWelcomeMessage);
        
        assertEquals(1, downstreamCatcher.events.size());
        checkDownstreamChannelStateEvent(downstreamCatcher.events.poll(), ChannelState.OPEN, Boolean.FALSE);
    }
    
    @Test
    public void testSendInvalidWelcomeMessage() throws Exception {
        emulateConnect();
        
        ChannelBuffer fakeTooLongWelcomeMessage = ChannelBuffers.buffer(JxtaProtocolHandler.MAX_WELCOME_MESSAGE_SIZE + 1);
        fakeTooLongWelcomeMessage.writeBytes("JXTAHELLO\r\n".getBytes("UTF-8"));
        fakeTooLongWelcomeMessage.writerIndex(fakeTooLongWelcomeMessage.capacity());
        Channels.fireMessageReceived(channel, fakeTooLongWelcomeMessage);
        
        assertEquals(1, downstreamCatcher.events.size());
        checkDownstreamChannelStateEvent(downstreamCatcher.events.poll(), ChannelState.OPEN, Boolean.FALSE);
    }

    private ChannelBuffer createFramedMessage(ChannelBuffer serializedMessage) {
        ChannelBuffer messageContents = serializedMessage;
        MessagePackageHeader header = createHeader(messageContents);
        ChannelBuffer headerBuffer = ChannelBuffers.wrappedBuffer(header.getByteBuffer());
        return ChannelBuffers.wrappedBuffer(headerBuffer, messageContents);
    }
    
    private MessagePackageHeader createHeader(ChannelBuffer messageContents) {
        MessagePackageHeader header = new MessagePackageHeader();
        header.setContentLengthHeader(messageContents.readableBytes());
        header.setContentTypeHeader(WireFormatMessageFactory.DEFAULT_WIRE_MIME);
        return header;
    }

    private ChannelBuffer serializeMessage(Message testMessage) {
        WireFormatMessage serializedMessage = WireFormatMessageFactory.toWire(testMessage, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null);
        ByteBuffer[] messageBody = serializedMessage.getByteBuffers();
        
        return ChannelBuffers.wrappedBuffer(messageBody);
    }

    private Message createTestMessage() {
        Message testMessage = new Message();
        testMessage.addMessageElement(new StringMessageElement("a", "b", null));
        testMessage.addMessageElement(new StringMessageElement("c", "d", null));
        testMessage.addMessageElement(new StringMessageElement("e", "f", null));
        return testMessage;
    }

    private ChannelBuffer checkIsUpstreamMessageEventContainingSerializedMessage(ChannelEvent event, MimeMediaType expectedMime) {
        assertTrue(event instanceof UpstreamMessageEvent);
        UpstreamMessageEvent messageEv = (UpstreamMessageEvent)event;
        assertTrue(messageEv.getMessage() instanceof SerializedMessage);
        SerializedMessage message = (SerializedMessage) messageEv.getMessage();
        assertEquals(expectedMime, message.getMessageHeader().getContentTypeHeader());
        return message.getMessageContents();
    }
    
    private ChannelBuffer checkIsMessageEventContainingBuffer(ChannelEvent event) {
        assertTrue(event instanceof MessageEvent);
        MessageEvent messageEv = (MessageEvent)event;
        assertTrue(messageEv.getMessage() instanceof ChannelBuffer);
        ChannelBuffer unwrappedMessage = (ChannelBuffer) messageEv.getMessage();
        return unwrappedMessage;
    }
    
    private ChannelBuffer createRemoteWelcomeMessageBuffer() throws IOException {
        final WelcomeMessage receivedWelcomeMessage = new WelcomeMessage(LOCAL_ENDPOINT_ADDR, REMOTE_ENDPOINT_ADDR, REMOTE_PEER_ID, false);
        
        ByteBuffer byteBuffer = receivedWelcomeMessage.getByteBuffer();
        ChannelBuffer welcomeBytes = ChannelBuffers.wrappedBuffer(byteBuffer);
        return welcomeBytes;
    }
    
    private void emulateConnect() {
        Channels.fireChannelConnected(channel, REMOTE_SOCK_ADDR);
        clearQueues();
    }
    
    private void emulateEstablished() throws IOException {
        emulateConnect();
        Channels.fireMessageReceived(channel, createRemoteWelcomeMessageBuffer());
        clearQueues();
    }

    private void checkUpstreamChannelStateEvent(ChannelEvent event, ChannelState expectedState, SocketAddress expectedRemoteAddr) {
        assertTrue(event instanceof UpstreamChannelStateEvent);
        UpstreamChannelStateEvent stateEv = (UpstreamChannelStateEvent) event;
        assertEquals(expectedState, stateEv.getState());
        assertEquals(expectedRemoteAddr, stateEv.getValue());
    }
    
    private void checkQueuesEmpty() {
        assertTrue(upstreamCatcher.events.isEmpty());
        assertTrue(downstreamCatcher.events.isEmpty());
    }

    

    private void clearQueues() {
        upstreamCatcher.events.clear();
        downstreamCatcher.events.clear();
    }
}
