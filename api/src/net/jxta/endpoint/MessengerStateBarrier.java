package net.jxta.endpoint;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MessengerStateBarrier implements MessengerStateListener {

    public static final int NO_MATCH = 0;
    
    private final CountDownLatch latch;
    private final int awaitedState;
    private AtomicInteger matchingState;
    private AtomicBoolean expired;
    
    public MessengerStateBarrier(int awaitedState) {
        this.latch = new CountDownLatch(1);
        this.awaitedState = awaitedState;
        this.matchingState = new AtomicInteger(0);
        this.expired = new AtomicBoolean(false);
    }
    
    public boolean messengerStateChanged(int newState) {
        if(expired.get()) {
            return false;
        }
        
        if((newState & awaitedState) != 0) {
            // ensures that the value is only changed once 
            matchingState.compareAndSet(0, newState);
            expired.set(true);
            latch.countDown();
            return false;
        }
        
        return true;
    }

    /**
     * Waits for the provided number of milliseconds for the current state of the
     * Messenger to match one of the expected states.
     * 
     * @param timeoutInMillis the number of milliseconds to wait for the messenger
     * to be in one of the expected states. A timeout of 0 indicates that the thread
     * should wait indefinitely. A timeout of less than 0 is illegal.
     * @return the matching state of the messenger, if it transitioned to one of
     * the expected states within the specified time interval. If the timeout expires,
     * NO_MATCH is returned.
     * @throws InterruptedException if the thread is interrupted while waiting for
     * a match.
     */
    public int awaitMatch(long timeoutInMillis) throws InterruptedException {
    	if(timeoutInMillis < 0) {
    		throw new IllegalArgumentException(String.format("timeoutInMillis is negative: given %d", timeoutInMillis));
    	} else if(timeoutInMillis == 0) {
            latch.await();
            return matchingState.get();
        } else if(latch.await(timeoutInMillis, TimeUnit.MILLISECONDS)) {
            return matchingState.get();
        } else {
            return NO_MATCH;
        }
    }

    public void expire() {
        expired.set(true);
    }

}
