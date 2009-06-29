package net.jxta.impl.util.threads;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Simple subclass of ScheduledThreadPoolExecutor which guards against termination
 * through the public API - only classes with appropriate privileges should be allowed
 * to shut down the pool.
 * 
 * @author iainmcgin
 */
public class SharedScheduledThreadPoolExecutor extends
		ScheduledThreadPoolExecutor {

    public SharedScheduledThreadPoolExecutor(int corePoolSize) {
    	super(corePoolSize);
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
}
