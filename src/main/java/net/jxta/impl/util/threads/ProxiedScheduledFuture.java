package net.jxta.impl.util.threads;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future implementation which works with the ProxiedCallable to provide
 * offloading to a target executor.  It pretends as if it was
 * 
 * @param <V> return type of the callable function
 */
public class ProxiedScheduledFuture<V>
        implements ScheduledFuture<V> {
    
    /**
     * Future that we were given when we scheduled our proxy task.
     */
    private final ScheduledFuture<V> pFuture;
    
    /**
     * Actual future that we are passed at runtime.
     */
    private Future<V> future;
    
    ///////////////////////////////////////////////////////////////////////////
    // Constructor:
    
    /**
     * Creates a new proxied ProxiedCallableFuture instance.
     * 
     * @param proxyFuture ScheduledFuture obtained by scheduling a proxy task
     *  instance
     */
    public ProxiedScheduledFuture(
            final ScheduledFuture<V> proxyFuture) {
        pFuture = proxyFuture;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ScheduledFuture interface methods:
    
    /**
     * {@inheritDoc}
     * 
     * This instance is equivalent to calling
     * <code>proxyFuture.getDelay()</code>.
     */
    public long getDelay(final TimeUnit unit) {
        return pFuture.getDelay(unit);
    }

    /**
     * {@inheritDoc}
     * 
     * This instance is equivalent to calling
     * <code>proxyFuture.compareTo()</code>.
     */
    public int compareTo(final Delayed o) {
        return pFuture.compareTo(o);
    }

    /**
     * {@inheritDoc}
     * 
     * This instance is equivalent to calling
     * <code>proxyFuture.cancel()</code> before execution is started on
     * the alternate executor service, and is equivalent to
     * <code>future.cancel()</code> thereafter.
     */
    public boolean cancel(final boolean mayInterruptIfRunning) {
        synchronized(this) {
            if (future == null) {
                return pFuture.cancel(mayInterruptIfRunning);
            } else {
                return future.cancel(mayInterruptIfRunning);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This instance is equivalent to calling
     * <code>proxyFuture.isCancelled()</code> before execution is started on
     * the alternate executor service, and is equivalent to
     * <code>future.isCancelled()</code> thereafter.
     */
    public boolean isCancelled() {
        synchronized(this) {
            if (future == null) {
                return pFuture.isCancelled();
            } else {
                return future.isCancelled();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This instance is equivalent to calling
     * <code>proxyFuture.isDone()</code> before execution is started on
     * the alternate executor service, and is equivalent to
     * <code>future.isDone()</code> thereafter.
     */
    public boolean isDone() {
        synchronized(this) {
            if (future == null) {
                return pFuture.isDone();
            } else {
                return future.isDone();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     */
    public V get() throws InterruptedException, ExecutionException {
        synchronized(this) {
            while(future == null) {
                wait();
            }
            return future.get();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws InterruptedException if the current thread was interrupted
     *  while waiting
     */
    public V get(
            final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long entryTime = System.currentTimeMillis();
        synchronized(this) {
            long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
            wait(timeoutMillis);
            if (future == null) {
                throw(new TimeoutException(
                        "Timeout while waiting for future (1)"));
            }
            // Adjust the timeout based on how long we've waited already
            timeoutMillis -= (System.currentTimeMillis() - entryTime);
            if (timeoutMillis < 0) {
                throw(new TimeoutException(
                        "Timeout while waiting for future (2)"));
            }
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Package methods:
    
    /**
     * Called by the ProxiedCallable if/when it executes, providing this class
     * with the actual Future instance.
     * 
     * @param actualFuture actual future instance
     */
    void setFuture(final Future<V> actualFuture) {
        synchronized(this) {
            if (future == null) {
                future = actualFuture;
                if (isCancelled()) {
                    future.cancel(true);
                }
                notifyAll();
            } else {
                throw(new IllegalStateException(
                        "Actual future has already been set"));
            }
        }
    }

}
