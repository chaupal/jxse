package net.jxta.endpoint;


import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MessengerStateListenerListTest {

    private MessengerStateListenerSet list;
    
    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();
    
    
    @Before
    public void setUp() throws Exception {
        list = new MessengerStateListenerSet();
    }
    
    @Test
    public void testNotifiesAllListeners() {
        final MessengerStateListener listener1 = mockContext.mock(MessengerStateListener.class, "listener1");
        final MessengerStateListener listener2 = mockContext.mock(MessengerStateListener.class, "listener2");
        final MessengerStateListener listener3 = mockContext.mock(MessengerStateListener.class, "listener3");
        list.addStateListener(listener1);
        list.addStateListener(listener2);
        list.addStateListener(listener3);
        
        mockContext.checking(new Expectations() {{
            one(listener1).messengerStateChanged(Messenger.CLOSED);
            one(listener2).messengerStateChanged(Messenger.CLOSED);
            one(listener3).messengerStateChanged(Messenger.CLOSED);
        }});
        
        list.notifyNewState(Messenger.CLOSED);
    }
    
    @Test
    public void testListenerAutomaticRemoval() {
        final MessengerStateListener listener1 = mockContext.mock(MessengerStateListener.class, "listener1");
        final MessengerStateListener listener2 = mockContext.mock(MessengerStateListener.class, "listener2");
        final MessengerStateListener listener3 = mockContext.mock(MessengerStateListener.class, "listener3");
        list.addStateListener(listener1);
        list.addStateListener(listener2);
        list.addStateListener(listener3);
        
        final Sequence expectedOrder = mockContext.sequence("listener1seq"); 
        
        mockContext.checking(new Expectations() {{
            one(listener1).messengerStateChanged(Messenger.RECONNECTING); inSequence(expectedOrder); will(returnValue(true));
            one(listener2).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(false));
            one(listener3).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(false));
            
            one(listener1).messengerStateChanged(Messenger.CONNECTED); inSequence(expectedOrder); will(returnValue(false));
        }});
        
        list.notifyNewState(Messenger.RECONNECTING);
        list.notifyNewState(Messenger.CONNECTED);
        
        // no one should receive the idle state
        list.notifyNewState(Messenger.IDLE);
    }
    
    @Test
    public void testRemoveStateListener() {
        final MessengerStateListener listener1 = mockContext.mock(MessengerStateListener.class, "listener1");
        final MessengerStateListener listener2 = mockContext.mock(MessengerStateListener.class, "listener2");
        final MessengerStateListener listener3 = mockContext.mock(MessengerStateListener.class, "listener3");
        list.addStateListener(listener1);
        list.addStateListener(listener2);
        list.addStateListener(listener3);
        
        list.removeStateListener(listener1);
        
        mockContext.checking(new Expectations() {{
            never(listener1);
            one(listener2).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(false));
            one(listener3).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(false));
        }});
        
        list.notifyNewState(Messenger.RECONNECTING);
    }
    
    @Test
    public void testAddListenerTwice() {
        final MessengerStateListener listener = mockContext.mock(MessengerStateListener.class, "listener1");
        list.addStateListener(listener);
        
        // the second attempt to add should have no effect
        list.addStateListener(listener);
        
        mockContext.checking(new Expectations() {{
            one(listener).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(true));
        }});
        
        list.notifyNewState(Messenger.RECONNECTING);
    }

}
