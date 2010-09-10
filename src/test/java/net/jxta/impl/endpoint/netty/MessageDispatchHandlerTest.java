package net.jxta.impl.endpoint.netty;


import java.net.URI;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.peer.PeerID;
import net.jxta.test.util.JUnitRuleMockery;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

public class MessageDispatchHandlerTest {

    private static final PeerID PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E5047205032503314AAA35D8BB7416D923D8CD14576AE3F03"));
    
    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();
    
    private MessageArrivalListener listener;
    private MessageDispatchHandler handler;
    private NettyChannelRegistry registry;
    private FakeChannel channel;
    
    
    @Before
    public void setUp() throws Exception {
        registry = mockContext.mock(NettyChannelRegistry.class);
        listener = mockContext.mock(MessageArrivalListener.class);
        handler = new MessageDispatchHandler(registry);
        
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("test", handler);
        channel = new FakeChannel(null, null, pipeline, new FakeChannelSink());
    }
    
    @Test
    public void testSendsMessageToListener() {
        final Message expectedMessage = new Message();
        mockContext.checking(new Expectations() {{
            one(listener).messageArrived(expectedMessage);
        }});
        
        handler.setMessageArrivalListener(listener);
        
        Channels.fireMessageReceived(channel, expectedMessage);
    }
    
    @Test
    public void testMessagesAreQueuedUntilListenerAttached() {
        final Sequence seq = mockContext.sequence("messages");
        final Message expectedMessage1 = new Message();
        final Message expectedMessage2 = new Message();
        final Message expectedMessage3 = new Message();
        
        Channels.fireMessageReceived(channel, expectedMessage1);
        Channels.fireMessageReceived(channel, expectedMessage2);
        Channels.fireMessageReceived(channel, expectedMessage3);
        
        mockContext.checking(new Expectations() {{
            one(listener).messageArrived(expectedMessage1); inSequence(seq);
            one(listener).messageArrived(expectedMessage2); inSequence(seq);
            one(listener).messageArrived(expectedMessage3); inSequence(seq);
        }});
        
        handler.setMessageArrivalListener(listener);
    }
    
    @Test
    public void testWelcomeMessageTriggersNewChannelCall() {
        handler.setMessageArrivalListener(listener);
        
        final EndpointAddress expectedLogicalAddress = new EndpointAddress("jxta", PEER_ID.getUniqueValue().toString(), null, null);
        final EndpointAddress directedAtAddress = new EndpointAddress("test://192.168.1.1:12345/blah");
        final WelcomeMessage message = new WelcomeMessage(directedAtAddress, new EndpointAddress("test://192.168.1.2:54321"), PEER_ID, true);
        
        mockContext.checking(new Expectations() {{
            one(registry).newConnection(with(same(channel)), with(equal(directedAtAddress)), with(equal(expectedLogicalAddress)));
        }});
        
        Channels.fireMessageReceived(channel, message);
    }
    
    @Test
    public void testWelcomeAndMessagesInQueue() {
    	final Message message1 = new Message();
    	final Message message2 = new Message();
    	
    	Channels.fireMessageReceived(channel, message1);
    	Channels.fireMessageReceived(channel, message2);
    	
    	final Sequence seq = mockContext.sequence("arrivals");
    	mockContext.checking(new Expectations() {{
    		one(listener).messageArrived(message1); inSequence(seq);
    		one(listener).messageArrived(message2); inSequence(seq);
    	}});
    	
    	handler.setMessageArrivalListener(listener);
    }
}
