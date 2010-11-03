package net.jxta.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

public class MessengerStateBarrierTest {

    @Test(timeout=100)
    public void testAwaitMatch_singleEvent_matchBeforeAwait() throws Exception {
        MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED);
        // barrier should indicate that it is to be unregistered when the right state is matched
        // it does this by returning false
        assertFalse(barrier.messengerStateChanged(Messenger.CLOSED));
        assertEquals(Messenger.CLOSED, barrier.awaitMatch(0));
    }

    @Test
    public void testAwaitMatch_singleEvent_neverMatch() throws Exception {
        MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED);
        assertEquals(MessengerStateBarrier.NO_MATCH, barrier.awaitMatch(10L));
    }

    @Test(timeout=100)
    public void testAwaitMatch_singleEvent_matchesWhileWaiting() throws Exception {
        final MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while sleeping");
                    e.printStackTrace();
                }
                barrier.messengerStateChanged(Messenger.CLOSED);
            }
        }).start();

        assertEquals(Messenger.CLOSED, barrier.awaitMatch(50L));
    }

    @Test
    public void testAwaitMatch_singleEvent_ignoresOthers() throws Exception {
        MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED);
        assertTrue(barrier.messengerStateChanged(Messenger.CLOSING));
        assertEquals(MessengerStateBarrier.NO_MATCH, barrier.awaitMatch(50L));
    }

    @Test
    public void testAwaitMatch_multipleEvents() throws Exception {
        MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED | Messenger.BROKEN);
        assertTrue(barrier.messengerStateChanged(Messenger.CONNECTED));
        assertFalse(barrier.messengerStateChanged(Messenger.BROKEN));
        assertEquals(Messenger.BROKEN, barrier.awaitMatch(0));

        barrier = new MessengerStateBarrier(Messenger.CLOSED | Messenger.BROKEN);
        assertFalse(barrier.messengerStateChanged(Messenger.CLOSED));
        assertEquals(Messenger.CLOSED, barrier.awaitMatch(0));
    }

    @Test
    public void testExpire() {
        MessengerStateBarrier barrier = new MessengerStateBarrier(Messenger.CLOSED);
        barrier.expire();

        // barrier should return false, even if the state does not match it's expectations
        assertFalse(barrier.messengerStateChanged(Messenger.CONNECTED));
    }

}
