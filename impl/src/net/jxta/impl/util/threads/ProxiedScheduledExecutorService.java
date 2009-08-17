package net.jxta.impl.util.threads;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ScheduledExecutorService implementation which performs the scheduling of
 * the tasks but then uses another ExecutorService implementation to actually
 * execute tasks.  This allows for small, purpose-driven scheduled executors
 * to be created which then delegate to the more common application-level
 * thread pools.
 * 
 * NOTE: This class is dynamically patched at build time by ant to support the 
 * java 1.6 semantics for invokeAny and invokeAll if 1.6 is found.  See the discussion
 * beginning at https://jxta.dev.java.net/servlets/ReadMsg?list=dev&msgNo=981
 * 
 */
public class ProxiedScheduledExecutorService
        implements ScheduledExecutorService {
    
    /**
     * ScheduledExecutorService instance used for task scheduling.
     */
    private final ScheduledExecutorService schedExec;
    
    /**
     * ExecutorService used when we actually need to execute a task.
     */
    private final ExecutorService targetExec;
    
    /**
     * Flag indicating whether or not shutdown commands should be forwarded
     * on to the wrapped ScheduledExecutorService instance.
     */
    private final boolean forwardShutdown;

    ///////////////////////////////////////////////////////////////////////////
    // Constructors:
    
    /**
     * Creates a scheduled executor service which executes all tasks on the
     * specified executor service.  This form creates an internal single
     * thread scheduled executor for scheduling purposes.
     * 
     * @param targetExecutor executor service instance to use for task
     *  execution
     */
    public ProxiedScheduledExecutorService(
            final ExecutorService targetExecutor) {
        this(Executors.newSingleThreadScheduledExecutor(),
                targetExecutor, true);
    }
    
    /**
     * Creates a scheduled executor service which executes all tasks on the
     * specified executor service.  This form creates uses the provided
     * scheduled executor service instance for scheduling operations but
     * prevents the shutdown commands from impacting the underlying
     * scheduled executor service instance.
     * 
     * @param schedExecutor scheduled executor service instance to use for
     *  scheduling tasks
     * @param targetExecutor executor service instance to use for task
     *  execution
     */
    public ProxiedScheduledExecutorService(
            final ScheduledExecutorService schedExecutor,
            final ExecutorService targetExecutor) {
        this(schedExecutor, targetExecutor, false);
    }
    
    /**
     * Creates a scheduled executor service which executes all tasks on the
     * specified executor service.  This form creates uses the provided
     * scheduled executor service instance for scheduling operations but
     * prevents the shutdown commands from impacting the underlying
     * scheduled executor service instance.
     * 
     * @param schedExecutor scheduled executor service instance to use for
     *  scheduling tasks
     * @param targetExecutor executor service instance to use for task
     *  execution
     * @param forwardShutdownCommands flag indicating whether or not the
     *  shutdown commands should impact the underlying scheduled executor
     *  service instance.  <code>true</code> if a call to
     *  <code>shutdown()</code> or <code>shutdownNow()</code> should result
     *  in the corresponding methods of the wrapped scheduled executor service
     *  instance being called, <code>false</code> otherwise.
     */
    public ProxiedScheduledExecutorService(
            final ScheduledExecutorService schedExecutor,
            final ExecutorService targetExecutor,
            final boolean forwardShutdownCommands) {
        schedExec = schedExecutor;
        targetExec = targetExecutor;
        forwardShutdown = forwardShutdownCommands;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // ScheduledExecutorService interface methods:
    
    /**
     * {@inheritDoc}
     * 
     * This implementation wraps the provided Runnable in a proxy object which
     * then forwards the execution on to the target executor service instance
     * for execution.
     */
    public ScheduledFuture<?> schedule(
            final Runnable command,
            final long delay,
            final TimeUnit unit) {
        ProxiedRunnable proxy = new ProxiedRunnable(targetExec, command);
        return schedExec.schedule(proxy, delay, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation wraps the provided Callable in a proxy object which
     * then forwards the execution on to the target executor service instance
     * for execution.
     * 
     * @param <V> return type of the function
     */
    public <V> ScheduledFuture<V> schedule(
            final Callable<V> callable,
            final long delay,
            final TimeUnit unit) {
        ProxiedCallable<V> proxy = new ProxiedCallable<V>(targetExec, callable);
        ScheduledFuture<V> schedFuture = schedExec.schedule(proxy, delay, unit);
        ProxiedScheduledFuture<V> proxyFuture =
                new ProxiedScheduledFuture<V>(schedFuture);
        proxy.setProxiedScheduledFuture(proxyFuture);
        return proxyFuture;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation wraps the provided Runnable in a proxy object which
     * then forwards the execution on to the target executor service instance
     * for execution.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(
            final Runnable command,
            final long initialDelay,
            final long period,
            final TimeUnit unit) {
        ProxiedRunnable proxy = new ProxiedRunnable(targetExec, command);
        return schedExec.scheduleAtFixedRate(
                proxy, initialDelay, period, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation wraps the provided Runnable in a proxy object which
     * then forwards the execution on to the target executor service instance
     * for execution.
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(
            final Runnable command,
            final long initialDelay,
            final long delay,
            final TimeUnit unit) {
        ProxiedRunnable proxy = new ProxiedRunnable(targetExec, command);
        return schedExec.scheduleWithFixedDelay(
                proxy, initialDelay, delay, unit);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ExecutorService interface methods:
    
    /**
     * {@inheritDoc}
     * 
     * This implementation will call <code>shutdown()</code> on the wrapped
     * ScheduledExecutorService if requested during construction.  Otherwise,
     * it does nothing.
     */
    public void shutdown() {
        if (forwardShutdown) {
            schedExec.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will call <code>shutdownNow()</code> on the wrapped
     * ScheduledExecutorService if requested during construction.  Otherwise,
     * it always do nothing and return an empty list.
     */
    public List<Runnable> shutdownNow() {
        if (forwardShutdown) {
            return schedExec.shutdownNow();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling <code>isShutdown()</code>
     * on the wrapped ScheduledExecutorService.
     */
    public boolean isShutdown() {
        return schedExec.isShutdown();
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling <code>isShutdown()</code>
     * on the wrapped ScheduledExecutorService.
     */
    public boolean isTerminated() {
        return schedExec.isTerminated();
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling <code>isShutdown()</code>
     * on the wrapped ScheduledExecutorService.
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(
            final long timeout,
            final TimeUnit unit)
            throws InterruptedException {
        return schedExec.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling the equivalent
     * <code>submit()</code> method on the wrapped ExecutorService instance.
     * 
     * @param <T> return type of the function
     */
    public <T> Future<T> submit(final Callable<T> task) {
        return targetExec.submit(task);
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling the equivalent
     * <code>submit()</code> method on the wrapped ExecutorService instance.
     * 
     * @param <T> return type of the function
     */
    public <T> Future<T> submit(final Runnable task, final T result) {
        return targetExec.submit(task, result);
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling the equivalent
     * <code>submit()</code> method on the wrapped ExecutorService instance.
     */
    public Future<?> submit(final Runnable task) {
        return targetExec.submit(task);
    }








    /**
     * {@inheritDoc}
     */
    public void execute(final Runnable command) {
        targetExec.execute(command);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods:
    
    /**
     * {@inheritDoc}
     * 
     * This implementation calls <code>shutdownNow()</code>.
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation is equivalent to calling the equivalent
     * <code>invokeAll()</code> method on the wrapped ExecutorService instance.
     * 
     * @param <T> return type of the function(s)
     * @throws InterruptedException if interrupted while waiting, in which
     *  case unfinished tasks are cancelled 
     */

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return targetExec.invokeAll(tasks);
	}

	  /**
	  * {@inheritDoc}
	  * 
	  * This implementation is equivalent to calling the equivalent
	  * <code>invokeAll()</code> method on the wrapped ExecutorService instance.
	  * 
	  * @param <T> return type of the function(s)
	  * @throws InterruptedException if interrupted while waiting, in which
	  *  case unfinished tasks are cancelled 
	  */
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, 
			long timeout, TimeUnit unit) throws InterruptedException {
		return targetExec.invokeAll(tasks, timeout, unit);
	}


	  /**
	  * {@inheritDoc}
	  * 
	  * This implementation is equivalent to calling the equivalent
	  * <code>invokeAny()</code> method on the wrapped ExecutorService instance.
	  * 
	  * @param <T> return type of the function(s)
	  * @throws InterruptedException if interrupted while waiting, in which
	  *  case unfinished tasks are cancelled 
	  */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return targetExec.invokeAny(tasks);
	}



	  /**
	  * {@inheritDoc}
	  * 
	  * This implementation is equivalent to calling the equivalent
	  * <code>invokeAny()</code> method on the wrapped ExecutorService instance.
	  * 
	  * @param <T> return type of the function(s)
	  * @throws InterruptedException if interrupted while waiting 
	  */
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return targetExec.invokeAny(tasks, timeout, unit);
	}
    
}
