package net.jxta.impl.util.threads;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for periodic, deferred and multi-threaded execution of tasks. The intention
 * of this class is to abstract away the details of how tasks will be executed, and specifics
 * of how many threads are used.
 * <p>
 * <em>NOTE</em>: This is <em>not</em> part of the stable API for JXTA, and should not be used
 * by code which is not a core module of the JXTA-JXSE implementation. We anticipate that in
 * future a similar mechanism to this will be adopted as the standard way of controlling the
 * execution thread pools in JXTA, and may be later exposed to the outside world, but the
 * details have not yet been adequately discussed.
 */
public class TaskManager {
	
    protected static final Logger LOG = Logger.getLogger(TaskManager.class.getName());
    
	static final String CORE_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.corePoolSize";
	static final String MAX_WORKER_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.maxWorkerPoolSize";
	static final String SCHEDULED_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.scheduledPoolSize";
	static final String IDLE_THREAD_TIMEOUT_SYSPROP = "net.jxta.util.threads.TaskManager.idleThreadTimeout";
	
	static final int DEFAULT_CORE_POOL_SIZE =4;
	static final int DEFAULT_MAX_WORKER_POOL_SIZE = Integer.MAX_VALUE;
	static final int DEFAULT_SCHEDULED_POOL_SIZE = 2;
	static final int DEFAULT_IDLE_THREAD_TIMEOUT = 10;
	
	private static TaskManager singleton;
	
	private static SharedThreadPoolExecutor normalExecutor;
	private static SharedScheduledThreadPoolExecutor scheduledExecutor;
	private static ScheduledExecutorService monitoringExecutor;
	
	private static Map<String, ProxiedScheduledExecutorService> proxiedExecutors;
	
	private static boolean started;
	
	static int getScheduledPoolSize() {
		return Math.max(1, Integer.getInteger(SCHEDULED_POOL_SIZE_SYSPROP, DEFAULT_SCHEDULED_POOL_SIZE));
	}

	static int getCorePoolSize() {
		return Math.max(0, Integer.getInteger(CORE_POOL_SIZE_SYSPROP, DEFAULT_CORE_POOL_SIZE));
	}
	
	static int getIdleThreadTimeout() {
	    return Math.max(0, Integer.getInteger(IDLE_THREAD_TIMEOUT_SYSPROP, DEFAULT_IDLE_THREAD_TIMEOUT));
	}
	
	static int getMaxWorkerPoolSize() {
	    // while core pool size is allowed to be zero, max pool size
	    // must be greater than the core pool size AND greater than
	    // 0.
	    int leastUpperBound = Math.max(1, getCorePoolSize());
	    return Math.max(leastUpperBound, Integer.getInteger(MAX_WORKER_POOL_SIZE_SYSPROP, DEFAULT_MAX_WORKER_POOL_SIZE));
	}

	public static TaskManager getTaskManager() {
		if(TaskManager.singleton == null) {
			TaskManager.singleton = new TaskManager();
			monitoringExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("JxtaTaskMonitor"));
			normalExecutor = new SharedThreadPoolExecutor(monitoringExecutor, 
			                                              getCorePoolSize(), 
			                                              getMaxWorkerPoolSize(), 
			                                              getIdleThreadTimeout(),
			                                              TimeUnit.SECONDS, 
			                                              new SynchronousQueue<Runnable>(), 
			                                              new NamedThreadFactory("JxtaWorker"));
			scheduledExecutor = new SharedScheduledThreadPoolExecutor(monitoringExecutor, getScheduledPoolSize(), new NamedThreadFactory("JxtaScheduledWorker"));
			proxiedExecutors = Collections.synchronizedMap(new HashMap<String, ProxiedScheduledExecutorService>());
			started=true;
		}
		return TaskManager.singleton;
	}
	
	/**
	 * FOR TESTING PURPOSES ONLY.
	 * discards any existing TaskManager singleton.
	 */
	public static void resetTaskManager() {
	    if(TaskManager.singleton != null && started) {
	        getTaskManager().shutdown();
	    }
		TaskManager.singleton = null;
	}
	
	/**
	 * Provides a potentially shared executor service.
	 * Note that since this instance could be shared, it is illegal to attempt to shut down the
	 * provided instance (an IllegalStateException will be thrown).
	 */
	public ExecutorService getExecutorService() {
		return normalExecutor;
	}

	/**
	 * Provides a shared scheduled executor service.
	 * Note that since this instance could be shared, it is illegal to attempt to shut down the
	 * provided instance (an IllegalStateException will be thrown).
	 */
	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutor;
	}
	
	/**
	 * Provides a ScheduledExecutorService which the client can safely shut down independently
	 * of any other ScheduledExecutor provided by this class.
	 * 
	 * NOTE: the current implementation of local scheduled executors incurs a cost of an additional
	 * thread per service. Please refrain from this unless you genuinely require your own
	 * executor service.
	 */
	public ScheduledExecutorService getLocalScheduledExecutorService(String serviceName) {
		synchronized(proxiedExecutors) {
			ProxiedScheduledExecutorService service = proxiedExecutors.get(serviceName);
			if(service == null) {
				service = new ProxiedScheduledExecutorService(serviceName, normalExecutor);
				proxiedExecutors.put(serviceName, service);
			}
			return service;
		}
	}
	
	
	public void shutdown() {
		if(!started) {
			throw new IllegalStateException("Task manager is already shut down");
		}
		normalExecutor.shutdownShared();
		scheduledExecutor.shutdownShared();
		monitoringExecutor.shutdownNow();
		
		synchronized (proxiedExecutors) {
			for(String serviceName : proxiedExecutors.keySet()) {
				ProxiedScheduledExecutorService service = proxiedExecutors.get(serviceName);
				if(!service.isShutdown()) {
					LOG.log(Level.WARNING, "Local executor for \"" + serviceName + "\" has not been locally shut down - forcing termination now");
					service.shutdownNow();
				}
			}
			
			proxiedExecutors.clear();
		}
		
		started = false;
	}
}
