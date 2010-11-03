package net.jxta.impl.endpoint;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerStateListener;
import net.jxta.endpoint.OutgoingMessageEvent;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.test.util.JUnitRuleMockery;

import org.jmock.Expectations;
import org.jmock.Sequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AsynchronousMessengerTest {

    private static final int QUEUE_SIZE = 10;

    private TestableAsynchronousMessenger messenger;
    private MessengerStateListener mockListener;
    private Message msg;
    private ExecutorService concurrentExecutor;

    @Rule
    public JUnitRuleMockery mockContext = new JUnitRuleMockery();

    @Before
    public void setUp() throws Exception {
        messenger = new TestableAsynchronousMessenger(PeerGroupID.defaultNetPeerGroupID, new EndpointAddress("http", "1.2.3.4", null, null), QUEUE_SIZE);
        mockListener = mockContext.mock(MessengerStateListener.class);
        msg = new Message();
        concurrentExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        concurrentExecutor.shutdownNow();
    }

    @Test
    public void testInitiallyConnected() {
        assertTrue(messenger.getState() == Messenger.CONNECTED);
    }

    @Test
    public void testSendMessageN_returnsTrueOnEnqueue() {
        assertTrue(messenger.sendMessageN(new Message(), "TestService", null));
    }

    @Test
    public void testSendMessageN_enqueuedMessagesArePushed() {
        messenger.sendMessageN(msg, "TestService", null);
        assertEquals(1, messenger.sentMessages.size());
        assertSame(msg, messenger.sentMessages.poll().getMessage());
    }

    @Test
    public void testSendMessageN_returnsFalseWhenSaturated() {
        saturateMessenger();
        messenger.refuseToSend.set(true);

        assertFalse(messenger.sendMessageN(msg, null, null));
        assertEquals(OutgoingMessageEvent.OVERFLOW, msg.getMessageProperty(Messenger.class));
    }

    private void saturateMessenger() {
        assertTrue(messenger.refuseToSend.compareAndSet(false, true));
        while(messenger.sendMessageN(new Message(), null, null));
        messenger.refuseToSend.set(false);
    }

    @Test
    public void testPullNextWrite() {
        enqueueMessages(3);
        messenger.pullMessages();
        assertEquals(3, messenger.sentMessages.size());
    }

    @Test
    public void testSendMessageN_failure() {
        messenger.sendException = new Exception("Bad stuff happened!");
        assertFalse(messenger.sendMessageN(msg, null, null));
        assertTrue(TransportUtils.isMarkedWithFailure(msg));
        assertSame(messenger.sendException, TransportUtils.getFailureCause(msg));
    }

    @Test
    public void testMessagesMarkedOnSuccess() {
        messenger.sendMessageN(msg, null, null);
        messenger.sentMessages.poll().getWriteListener().writeSuccess();
        assertTrue(TransportUtils.isMarkedWithSuccess(msg));
    }

    @Test
    public void testSendMessageB_blocksUntilSent() throws Exception {
        Future<Void> sendTask = concurrentExecutor.submit(new BlockingSender(messenger, msg));

        QueuedMessage sentMessage = messenger.sentMessages.poll(100L, TimeUnit.MILLISECONDS);
        assertNotNull(sentMessage);
        assertSame(msg, sentMessage.getMessage());
        sentMessage.getWriteListener().writeSuccess();
        assertNull(sendTask.get(100L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendMessageB_blocksForSendQueueSpace() throws Exception {
        saturateMessenger();

        Future<Void> sendTask = concurrentExecutor.submit(new BlockingSender(messenger, msg));
        // we expect that, despite the messenger now being able to send messages,
        // the sender will not send anything as it cannot put anything on to the queue.
        try {
            sendTask.get(100L, TimeUnit.MILLISECONDS);
            fail("expected send task to be stalled");
        } catch(TimeoutException e) {
            // timed out as expected
            sendTask.cancel(true);
        }
    }

    @Test
    public void testSendMessageB_canSendMultiple() throws Exception {
        enqueueMessages(3);
        assertEquals(0, messenger.sentMessages.size());
        Future<Void> sendTask = concurrentExecutor.submit(new BlockingSender(messenger, msg));
        markAsSent(4, 100L);
        assertNull(sendTask.get(100L, TimeUnit.MILLISECONDS));
    }

    @Test(timeout=100L)
    public void testSendMessageB_failure() throws Exception {
        messenger.sendException = new Exception("bad stuff happened");
        try {
            messenger.sendMessageB(msg, null, null);
            fail("Expected exception on send");
        } catch(IOException e) {
            // if an InterruptedException is seen here, it typically
            // means JUnit interrupted execution due to the timeout
            // specified in the annotation
            assertEquals("Failed to write message", e.getMessage());
            assertSame(messenger.sendException, e.getCause());
        }
    }

    @Test
    public void testSendMessageB_interrupted() throws Exception {
        final AtomicReference<IOException> result = new AtomicReference<IOException>();
        final CountDownLatch latch = new CountDownLatch(1);
        Thread sendThread = new Thread(new Runnable() {
            public void run() {
                try {
                    messenger.sendMessageB(msg, null, null);
                    result.set(null);
                } catch(IOException e) {
                    result.set(e);
                }
                latch.countDown();
            };
        });

        sendThread.start();
        Thread.sleep(10L);
        sendThread.interrupt();
        assertTrue(latch.await(100L, TimeUnit.MILLISECONDS));
        IOException resultValue = result.get();
        assertTrue(resultValue != null);
        assertTrue(resultValue.getCause() instanceof InterruptedException);
    }

    @Test
    public void testSendStateTransitions() {
        messenger.addStateListener(mockListener);
        final Sequence seq = mockContext.sequence("saturation-events");
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.SENDING); will(returnValue(true)); inSequence(seq);
            one(mockListener).messengerStateChanged(Messenger.SENDINGSATURATED); will(returnValue(true)); inSequence(seq);
        }});

        saturateMessenger();
        mockContext.assertIsSatisfied();

        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.SENDING); will(returnValue(true)); inSequence(seq);
            one(mockListener).messengerStateChanged(Messenger.CONNECTED); will(returnValue(true)); inSequence(seq);
        }});
        messenger.pullMessages();
        mockContext.assertIsSatisfied();
    }

    @Test
    public void testCloseWhenIdle() {
        assertTrue(messenger.getState() == Messenger.CONNECTED);
        messenger.addStateListener(mockListener);
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.CLOSED);
        }});

        messenger.close();
        mockContext.assertIsSatisfied();
        assertTrue(messenger.closeRequested.get());
    }

    @Test
    public void testConnectionFailureWhenClosing() {
        enqueueMessages(1);

        messenger.addStateListener(mockListener);
        final Sequence seq = mockContext.sequence("close-events");
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.CLOSING); will(returnValue(true)); inSequence(seq);
        }});

        messenger.close();
        assertFalse(messenger.closeRequested.get());
        mockContext.assertIsSatisfied();

        // now emulate the connection failing before we have sent all pending messages
        // this should result initially in a state change indicating an attempt to reconnect
        // then an immediate failure (as AsynchMessenger does not yet support reconnecting)

        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.RECONCLOSING); will(returnValue(true)); inSequence(seq);
            one(mockListener).messengerStateChanged(Messenger.BROKEN); will(returnValue(true)); inSequence(seq);
        }});
        messenger.connectionFailed();
        mockContext.assertIsSatisfied();
    }

    @Test
    public void testCloseWithPendingMessages() {
        enqueueMessages(2);

        messenger.addStateListener(mockListener);
        final Sequence seq = mockContext.sequence("close-events");
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.CLOSING); will(returnValue(true)); inSequence(seq);
        }});

        messenger.close();
        assertFalse(messenger.closeRequested.get());
        mockContext.assertIsSatisfied();

        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.CLOSED); will(returnValue(true)); inSequence(seq);
        }});
        messenger.pullMessages();
        mockContext.assertIsSatisfied();

        assertTrue(messenger.closeRequested.get());
        messenger.emulateConnectionGracefulClose();
        assertEquals(2, messenger.sentMessages.size());
    }

    @Test
    public void testSendMessageN_whenClosing() {
        enqueueMessages(1);
        messenger.close();
        assertFalse(messenger.sendMessageN(msg, null, null));
        assertTrue(TransportUtils.isMarkedWithFailure(msg));
        assertEquals("Messenger is closed. It cannot be used to send messages", TransportUtils.getFailureCause(msg).getMessage());
    }

    @Test
    public void testSendMessageB_whenClosing() {
        enqueueMessages(1);
        messenger.close();
        try {
            messenger.sendMessageB(msg, null, null);
            fail("expected IOException to be thrown");
        } catch (IOException e) {
            assertEquals(AsynchronousMessenger.CLOSED_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void testConnectionFailure_whenIdle() {
        messenger.addStateListener(mockListener);
        final Sequence seq = mockContext.sequence("event-seq");
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.DISCONNECTED); will(returnValue(true)); inSequence(seq);
        }});
        messenger.emulateConnectionDeath();
        mockContext.assertIsSatisfied();

        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(true)); inSequence(seq);
            one(mockListener).messengerStateChanged(Messenger.BROKEN); will(returnValue(true)); inSequence(seq);
        }});

        assertFalse(messenger.sendMessageN(msg, null, null));
        mockContext.assertIsSatisfied();
    }

    @Test
    public void testConnectionFailure_whenSending() {
        LinkedList<Message> messages = enqueueMessages(5);
        messenger.addStateListener(mockListener);
        final Sequence seq = mockContext.sequence("event-seq");
        mockContext.checking(new Expectations() {{
            one(mockListener).messengerStateChanged(Messenger.RECONNECTING); will(returnValue(true)); inSequence(seq);
            one(mockListener).messengerStateChanged(Messenger.BROKEN); will(returnValue(true)); inSequence(seq);
        }});
        messenger.emulateConnectionDeath();
        mockContext.assertIsSatisfied();

        for(Message msg : messages) {
            assertTrue(TransportUtils.isMarkedWithFailure(msg));
            assertEquals("Messenger unexpectedly closed", TransportUtils.getFailureCause(msg).getMessage());
        }
    }

    private void markAsSent(int numMessages, long timeoutInMillis) throws InterruptedException {
        int numMarked = 0;
        long startTime = System.currentTimeMillis();
        while(numMarked < numMessages && elapsedTime(startTime) < timeoutInMillis) {
            QueuedMessage message = messenger.sentMessages.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
            if(message != null) {
                message.getWriteListener().writeSuccess();
                numMarked++;
            }
        }

        assertEquals(numMessages, numMarked);
    }

    private LinkedList<Message> enqueueMessages(int numMessages) {
        LinkedList<Message> enqueuedMessages = new LinkedList<Message>();
        assertTrue(messenger.refuseToSend.compareAndSet(false, true));
        for(int i=0; i < numMessages; i++) {
            Message message = new Message();
            enqueuedMessages.add(message);
            assertTrue(messenger.sendMessageN(message, null, null));
        }
        messenger.refuseToSend.set(false);
        return enqueuedMessages;
    }

    private long elapsedTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private class BlockingSender implements Callable<Void> {

        private Messenger messengerToUse;
        private Message message;

        public BlockingSender(Messenger messenger, Message message) {
            this.messengerToUse = messenger;
            this.message = message;
        }

        public Void call() throws Exception {
            messengerToUse.sendMessageB(message, null, null);
            return null;
        }

    }

    private class TestableAsynchronousMessenger extends AsynchronousMessenger {

        public Exception sendException;
        public EndpointAddress localAddress = new EndpointAddress("http", "remote", null, null);
        public EndpointAddress logicalDestAddress = new EndpointAddress("jxta", IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID).getUniqueValue().toString(), null, null);
        public AtomicBoolean refuseToSend = new AtomicBoolean(false);
        public BlockingQueue<QueuedMessage> sentMessages = new LinkedBlockingQueue<QueuedMessage>();
        public AtomicBoolean closeRequested = new AtomicBoolean(false);
        public AtomicBoolean connectionDead = new AtomicBoolean(false);

        public TestableAsynchronousMessenger(PeerGroupID homeGroupID, EndpointAddress dest, int messageQueueSize) {
            super(homeGroupID, dest, messageQueueSize);
        }

        @Override
        public EndpointAddress getLocalAddress() {
            return localAddress;
        }

        @Override
        public EndpointAddress getLogicalDestinationAddress() {
            return logicalDestAddress;
        }

        @Override
        public boolean sendMessageImpl(QueuedMessage message) {
            if(refuseToSend.get()) {
                return false;
            }

            if(connectionDead.get()) {
                message.getWriteListener().writeSubmitted();
                message.getWriteListener().writeFailure(new IOException("Messenger unexpectedly closed"));
                return false;
            }

            if(sendException != null) {
                message.getWriteListener().writeSubmitted();
                message.getWriteListener().writeFailure(sendException);
                return false;
            }
            sentMessages.add(message);
            message.getWriteListener().writeSubmitted();
            return true;
        }

        @Override
        public void requestClose() {
            closeRequested.set(true);
        }

        public void emulateConnectionDeath() {
            connectionDead.set(true);
            connectionFailed();
        }

        public void emulateConnectionGracefulClose() {
            connectionDead.set(true);
            connectionCloseComplete();
        }
    }

}
