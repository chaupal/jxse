package net.jxta.impl.util.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Decorator for Runnable instances, with the exact same semantics as it's parent (RunMetricsWrapper).
 * 
 * @author iain.mcginniss@onedrum.com
 */
public class RunnableRunMetricsWrapper extends RunMetricsWrapper<Void> implements Runnable {

    private String wrappedType;
    private Runnable wrappedRunnable;
    
    public RunnableRunMetricsWrapper(ScheduledExecutorService longTaskMonitor, final Runnable wrapped) {
        super(longTaskMonitor, new Callable<Void>() {
            public Void call() throws Exception {
                wrapped.run();
                return null;
            }
        });
        this.wrappedType = wrapped.getClass().getName();
    }
    
    public void run() {
        try {
            call();
        } catch(Exception e) {
            // should not occur, due to us passing a wrapped Runnable which cannot
            // throw Exception
            e.printStackTrace();
        }
    }
    
    @Override
    public String getWrappedType() {
        return wrappedType;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof RunnableRunMetricsWrapper) {
            RunnableRunMetricsWrapper runWrapper = (RunnableRunMetricsWrapper)obj;
            return wrappedRunnable.equals(runWrapper.wrappedRunnable);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return wrappedRunnable.hashCode();
    }
}
