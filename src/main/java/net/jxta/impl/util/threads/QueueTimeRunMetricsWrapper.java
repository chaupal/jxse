package net.jxta.impl.util.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import net.jxta.logging.Logging;

public class QueueTimeRunMetricsWrapper<T> extends RunMetricsWrapper<T> {

	private long scheduleTime;
	
	public QueueTimeRunMetricsWrapper(ScheduledExecutorService longTaskMonitor, Callable<T> wrapped) {
		super(longTaskMonitor, wrapped);
        this.scheduleTime = System.currentTimeMillis();
	}
	
	public QueueTimeRunMetricsWrapper(ScheduledExecutorService longTaskMonitor, Runnable wrapped) {
		super(longTaskMonitor, wrapped);
		this.scheduleTime = System.currentTimeMillis();
	}
	
	@Override
	public T call() throws Exception {

            long queuedTime = System.currentTimeMillis() - scheduleTime;

            if(queuedTime > 2000 && Logging.SHOW_WARNING && SharedThreadPoolExecutor.LOG.isLoggable(Level.WARNING)) {
            
                SharedThreadPoolExecutor.LOG.log(Level.WARNING, "task of type [" + getWrappedType() + "] queued for " + queuedTime + "ms!");
                
            }
		
            return super.call();
	}

}
