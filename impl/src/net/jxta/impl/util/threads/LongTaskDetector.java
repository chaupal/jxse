/**
 * 
 */
package net.jxta.impl.util.threads;

import java.io.StringWriter;
import java.util.logging.Level;

import net.jxta.logging.Logging;

/**
 * Used to monitor a task which is currently being executed in a thread pool,
 * such that when it is deemed to be taking an excessive amount of time
 * to complete warnings will be logged with it's current stack. Useful for
 * detecting deadlocks or blocking behaviour in pools that expect rapid,
 * non-blocking execution.
 * 
 * @author iain.mcginniss@onedrum.com
 */
class LongTaskDetector implements Runnable {

    private RunMetricsWrapper<?> taskToMonitor;
    
    public LongTaskDetector(RunMetricsWrapper<?> taskToMonitor) {
        this.taskToMonitor = taskToMonitor;
    }
    
    public void run() {
        if(Logging.SHOW_WARNING && TaskManager.LOG.isLoggable(Level.WARNING)) {
            StackTraceElement[] stack = taskToMonitor.getStack();
            StringWriter stackTrace = new StringWriter();
            for(StackTraceElement elem : stack) {
                stackTrace.append(elem.toString());
                stackTrace.append('\n');
            }
            
            TaskManager.LOG.log(Level.WARNING, "task of type [{0}] still running after {1}ms in thread {2}, current stack:\n{3}", 
                    new Object[] { 
                        taskToMonitor.getWrappedType(), 
                        taskToMonitor.getExecutionTime(), 
                        taskToMonitor.getExecutorThreadName(),
                        stackTrace
                    });
        }
    }
}