package net.jxta.impl.util.threads;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Simple extensions to the standard thread pool executor - guards against termination through
 * the standard shutdown() and shutdownNow() methods, to allow for distribution to untrusted
 * code (typically from a refactor of timer usage or code which created it's own threads).
 * Additionally, will record some simple metrics on tasks submitted to the executor to report
 * on tasks which take an excessive amount of time to complete.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class SharedThreadPoolExecutor extends ThreadPoolExecutor {
    
    static final Logger LOG = Logger.getLogger(SharedThreadPoolExecutor.class.getName());
    
    ScheduledExecutorService longTaskMonitorService;

	public SharedThreadPoolExecutor(ScheduledExecutorService monitoringExecutor,
                        	        int corePoolSize,
                                    int maximumPoolSize,
                                    long keepAliveTime,
                                    TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue,
                                    ThreadFactory threadFactory) {
	    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.longTaskMonitorService = monitoringExecutor;
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
		throw new IllegalStateException("shutdown cannot be called on a shared thread pool executor");
	}
	
	public void shutdownNowShared() {
		super.shutdownNow();
	}
	
	@Override
	public void execute(Runnable command) {
	    RunnableRunMetricsWrapper wrapper = new RunnableRunMetricsWrapper(longTaskMonitorService, command);
	    super.execute(wrapper);
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
	    return super.submit(new RunMetricsWrapper<T>(longTaskMonitorService, task));
	}
	
	@Override
	public Future<?> submit(Runnable task) {
	    return super.submit((Runnable)new RunnableRunMetricsWrapper(longTaskMonitorService, task));
	}
	
	public <T extends Object> java.util.concurrent.Future<T> submit(Runnable task, T result) {
	    return super.submit(new RunnableRunMetricsWrapper(longTaskMonitorService, task), result);
	};
}
