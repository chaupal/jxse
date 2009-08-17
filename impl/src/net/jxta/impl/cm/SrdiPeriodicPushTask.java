package net.jxta.impl.cm;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.impl.cm.Srdi.SrdiInterface;
import net.jxta.logging.Logging;

public class SrdiPeriodicPushTask implements Runnable {

    private static final Logger LOG = Logger.getLogger(SrdiPeriodicPushTask.class.getName());
    
    private SrdiInterface pushNotifier;
    
    private boolean stopped = true;
    private boolean publishAll = true;
    
    private long pushIntervalInMs;
    
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> selfHandle;

    private String handlerName;

    public SrdiPeriodicPushTask(String handlerName, SrdiInterface pushNotifier, ScheduledExecutorService executorService, long pushIntervalInMs) {
        this.handlerName = handlerName;
        this.pushNotifier = pushNotifier;
        this.executorService = executorService;
        this.pushIntervalInMs = pushIntervalInMs;
    }
    
    public void start() {
        if(!stopped) {
            return;
        }
        
        if(Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, handlerName + ": Periodic Srdi delta push starting, delay = " + pushIntervalInMs + "ms");
        }
        
        stopped = false;
        publishAll = true;
        selfHandle = executorService.scheduleWithFixedDelay(this, 0L, pushIntervalInMs, TimeUnit.MILLISECONDS);
    }
    
    public void stop() {
        if(stopped) {
            return;
        }
        
        if(Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, handlerName + ": Periodic Srdi delta push stopping");
        }
        
        stopped = true;
        selfHandle.cancel(false);
    }
    
    public void run() {
        try {
            if(Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, handlerName + ": Pushing " + (publishAll ? "all entries" : "deltas"));
            }
            pushNotifier.pushEntries(publishAll);
            publishAll = false;
        } catch (Throwable all) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Uncaught Throwable in SrdiPushTask",all);
            }
        }
    }

}
