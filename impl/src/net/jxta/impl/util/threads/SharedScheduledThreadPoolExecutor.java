package net.jxta.impl.util.threads;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Simple subclass of ScheduledThreadPoolExecutor which guards against termination
 * through the public API - only classes with appropriate privileges should be allowed
 * to shut down the pool. Additionally, will record some simple metrics on tasks submitted 
 * to the executor to report on tasks which take an excessive amount of time to complete.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class SharedScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private ScheduledExecutorService longTaskMonitorExecutor;

    public SharedScheduledThreadPoolExecutor(ScheduledExecutorService monitorExecutor, int corePoolSize, ThreadFactory threadFactory) {
    	super(corePoolSize, threadFactory);
    	this.longTaskMonitorExecutor = monitorExecutor;
    }
	
	@Override
	public void shutdown() {
		throw new IllegalStateException("shutdown cannot be called on a shared thread pool executor");
	}
	
	public void shutdownShared() {
		super.shutdown();
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException("shutdownNow cannot be called on a shared thread pool executor");
	}
	
	public List<Runnable> shutdownNowShared() {
		return super.shutdownNow();
	}
	
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
	    return super.schedule((Callable<V>)new RunMetricsWrapper<V>(longTaskMonitorExecutor, callable), delay, unit);
	}
	
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
	    return super.schedule((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, command), delay, unit);
	}
	
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
	    return super.scheduleAtFixedRate((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, command), initialDelay, period, unit);
	}
	
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
	    return super.scheduleWithFixedDelay((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, command), initialDelay, delay, unit);
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
	    return super.submit((Callable<T>)new RunMetricsWrapper<T>(longTaskMonitorExecutor, task));
	}
	
	@Override
	public Future<?> submit(Runnable task) {
	    return super.submit((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, task));
	}
	
	public <T extends Object> java.util.concurrent.Future<T> submit(Runnable task, T result) {
	    return super.submit((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, task), result);
	};
	
	@Override
	public void execute(Runnable command) {
	    super.execute((Runnable)new RunMetricsWrapper<Void>(longTaskMonitorExecutor, command));
	}
}
