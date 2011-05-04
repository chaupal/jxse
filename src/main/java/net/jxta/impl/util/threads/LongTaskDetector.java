/**
 * 
 */
package net.jxta.impl.util.threads;

import net.jxta.logging.Logging;

import java.util.logging.Level;

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
        try
        {
            if (Logging.SHOW_WARNING && TaskManager.LOG.isLoggable(Level.WARNING))
            {
                StackTraceElement[] stack = taskToMonitor.getStack();
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("task of type [");
                logMessage.append(taskToMonitor.getWrappedType());
                logMessage.append("] still running after ");
                logMessage.append(taskToMonitor.getExecutionTime());
                logMessage.append("ms in thread {");
                logMessage.append(taskToMonitor.getExecutorThreadName());
                logMessage.append("}, current stack:\n");

                for (StackTraceElement elem : stack)
                {
                    logMessage.append(elem.toString());
                    logMessage.append('\n');
                }

                TaskManager.LOG.log(Level.WARNING, logMessage.toString());
            }
        }
        catch (Throwable t)
        {
            TaskManager.LOG.log(Level.WARNING, "Unable to report long running task exception occurred " + t);
        }
    }
}