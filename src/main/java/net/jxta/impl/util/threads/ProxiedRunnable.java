package net.jxta.impl.util.threads;

import java.util.concurrent.ExecutorService;

/**
 * Runnable instance which will execute the wrapped Runnable on the
 * target ExecutorService instance when it executes, thereby forwarding
 * execution to the target ExecutorService.
 */
public class ProxiedRunnable
        implements Runnable {
    
    /**
     * Wrapped executor service instance.
     */
    private final ExecutorService executor;
    
    /**
     * Wrapped runnnable to execute.
     */
    private final Runnable runnable;
    
    /**
     * Creates a new Runnable proxy instance.
     * 
     * @param targetExecutor executor to run runnable on
     * @param targetRunnable runnable to run
     */
    public ProxiedRunnable(
            final ExecutorService targetExecutor,
            final Runnable targetRunnable) {
        executor = targetExecutor;
        runnable = targetRunnable;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling
     * <code>targetExecutor.execute(targetRunnable)</code> when this method
     * is called.
     */
    public void run() {
        executor.execute(runnable);
    }

}
