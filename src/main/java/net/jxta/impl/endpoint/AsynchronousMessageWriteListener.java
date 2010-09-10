package net.jxta.impl.endpoint;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.jxta.endpoint.Message;

/**
 * Simple implementation of {@link MessageWriteListener} that allows code
 * to wait for the result of a write, either indefinitely or with a fixed
 * timeout. Also takes care of setting the appropriate success or failure
 * flags on the associated Message object
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class AsynchronousMessageWriteListener implements MessageWriteListener {
    
    CountDownLatch completionLatch = new CountDownLatch(1);
    private AtomicBoolean complete = new AtomicBoolean(false);
    private AtomicReference<Throwable> failureCause = new AtomicReference<Throwable>(null);
    private Message message;
    
    public AsynchronousMessageWriteListener(Message message) {
        this.message = message;
    }
    
    /**
     * Awaits the success or failure of this associated message being sent.
     * Once this method returns, {@link #wasSuccessful()} can be called.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void await() throws InterruptedException {
        completionLatch.await();
    }
    
    /**
     * Awaits the success or failure of this associated message being sent.
     * If the method returns true, {@link #wasSuccessful()} can be called.
     * Otherwise, the wait timed out and it is not yet known if the message
     * has been sent successfully.
     * @return whether or not the wait operation timed out.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return completionLatch.await(timeout, unit);
    }
    
    public boolean isDone() {
        return complete.get();
    }
    
    /**
     * @return whether the message was sent successfully.
     * @throws IllegalStateException if the message has not yet been marked as successful or
     * not. One must call {@link #isDone()} or {@link #await()} before calling this method. 
     */
    public boolean wasSuccessful() {
        if(!complete.get()) {
            throw new IllegalStateException("Message has not yet been marked as successful or not");
        }
        return failureCause.get() == null;
    }
    
    /**
     * @return the cause of failure. If the message was sent successfully, this method will
     * return null.
     * @throws IllegalStateException if the message has not yet been marked as successful or
     * not. One must call {@link #isDone()} or {@link #await()} before calling this method. 
     */
    public Throwable getFailureCause() {
        if(!complete.get()) {
            throw new IllegalStateException("Message has not yet been marked as successful or not");
        }
        return failureCause.get();
    }

    /**
     * Used to notify that the message failed to send, for the provided reason.
     */
    public void writeFailure(Throwable cause) {
        failureCause.set(cause);
        TransportUtils.markMessageWithSendFailure(message, cause);
        complete.set(true);
        completionLatch.countDown();
    }

    /**
     * Used to notify that the message successfully sent.
     */
    public void writeSuccess() {
        failureCause.set(null);
        TransportUtils.markMessageWithSendSuccess(message);
        complete.set(true);
        completionLatch.countDown();
    }
}
