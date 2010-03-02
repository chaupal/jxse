/**
 * 
 */
package net.jxta.impl.util.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.jxta.logging.Logging;

/**
 * Decorator for Callable instances which will monitor how long it takes before the callable
 * is executed (the queue time), and how long it takes to execute. Some monitoring is done
 * in parallel with the task also, using a {@link LongTaskDetector}, to log warnings when
 * the task is taking an excessive amount of time to complete.
 * 
 * @author iain.mcginniss@onedrum.com
 */
class RunMetricsWrapper<T> implements Callable<T>, Runnable {
	
    private Callable<T> wrappedRunnable;
    Thread executorThread;
    
    private long startTime;
    private ScheduledExecutorService longTaskMonitor;
    private String wrappedType;
    
    public RunMetricsWrapper(ScheduledExecutorService longTaskMonitor, Callable<T> wrapped) {
        this.wrappedRunnable = wrapped;
        this.longTaskMonitor = longTaskMonitor;
        this.wrappedType = wrapped.getClass().getName();
    }
    
    public RunMetricsWrapper(ScheduledExecutorService longTaskMonitor, Runnable wrapped) {
    	this(longTaskMonitor, new RunnableAsCallableWrapper<T>(wrapped));
    	this.wrappedType = wrapped.getClass().getName();
    }
    
    public T call() throws Exception {
        executorThread = Thread.currentThread();
        ScheduledFuture<?> future = longTaskMonitor.scheduleAtFixedRate(new LongTaskDetector(this), 1L, 5L, TimeUnit.SECONDS);

        startTime = System.currentTimeMillis();
        T returnVal = wrappedRunnable.call();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        future.cancel(true);
        
        if(elapsedTime > 200 && Logging.SHOW_WARNING && SharedThreadPoolExecutor.LOG.isLoggable(Level.WARNING)) {
            SharedThreadPoolExecutor.LOG.log(Level.WARNING, "task of type [{0}] took {1}ms to complete in the shared executor", new Object[] { getWrappedType(), elapsedTime });
        }
        
        return returnVal;
    }
    
    public Object getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getWrappedType() {
        return wrappedType;
    }

    public Object getExecutorThreadName() {
        return executorThread.getName();
    }

    public StackTraceElement[] getStack() {
        return executorThread.getStackTrace();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RunMetricsWrapper<?>) {
            wrappedRunnable.equals(((RunMetricsWrapper<?>)obj).wrappedRunnable);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return wrappedRunnable.hashCode();
    }
    
    public void run() {
    	try {
			call();
		} catch (Exception e) {
			
		}
    }
}