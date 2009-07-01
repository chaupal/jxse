package net.jxta.impl.util.threads;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple subclass of ThreadPoolExecutor which guards against termination
 * through the public API - only classes with appropriate privileges should be allowed
 * to shut down the pool.
 * 
 * @author iainmcgin
 */
public class SharedThreadPoolExecutor extends ThreadPoolExecutor {

	public SharedThreadPoolExecutor(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
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
		super.shutdown();
	}
}
