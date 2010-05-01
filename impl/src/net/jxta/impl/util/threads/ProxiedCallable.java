package net.jxta.impl.util.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Callable instance which will execute the wrapped Callable on the
 * target ExecutorService instance when it executes, thereby forwarding
 * execution to the target ExecutorService.
 * 
 * @param <V> return type of the callable function
 */
public class ProxiedCallable<V>
        implements Callable<V> {
    
    /**
     * Wrapped executor service instance.
     */
    private final ExecutorService executor;
    
    /**
     * Wrapped callable.
     */
    private final Callable<V> callable;
    
    /**
     * ProxiedScheduleFuture instance to pass our future instance to.  We
     * wait for this to be provided to us before executing our task.
     */
    private ProxiedScheduledFuture<V> futureProxy;
    
    ///////////////////////////////////////////////////////////////////////////
    // Constructor:
    
    /**
     * Creates a new proxied Callable instance.
     * 
     * @param targetExecutor executor to run callable on
     * @param targetCallable callable to execute
     */
    public ProxiedCallable(
            final ExecutorService targetExecutor,
            final Callable<V> targetCallable) {
        executor = targetExecutor;
        callable = targetCallable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Callable interface methods:
    
    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling
     * <code>targetExecutor.submit(targetCallable).get()</code> when this
     * method is called.
     * 
     * @throws Exception if the callable throws an exception during execution
     */
    public V call() throws Exception {
        Future<V> future = executor.submit(callable);
        synchronized(this) {
            while(futureProxy == null) {
                wait();
            }
            futureProxy.setFuture(future);
        }
        
        // This result isn't used anywhere.
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Package methods:
    
    /**
     * Sets the scheduled future proxy that we should contact and provide
     * the actual future instance to when we execute.
     * 
     * @param proxy schedule future instance
     */
    void setProxiedScheduledFuture(final ProxiedScheduledFuture<V> proxy) {
        synchronized(this) {
            if (futureProxy == null) {
                futureProxy = proxy;
            } else {
                throw(new IllegalStateException(
                        "Proxy instance already set"));
            }
            notifyAll();
        }
    }
    
}
