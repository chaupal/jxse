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
	
	private SharedThreadPoolExecutor normalExecutor;
	private SharedScheduledThreadPoolExecutor scheduledExecutor;
	private ScheduledExecutorService monitoringExecutor;
        private CachedThreadExecutorService cachedExecutor;
	
	private Map<String, ProxiedScheduledExecutorService> proxiedExecutors;
	
	private boolean started;
	
	static int getScheduledPoolSize(Integer scheduledPoolSize) {
		int size = scheduledPoolSize == null ? Integer.getInteger(SCHEDULED_POOL_SIZE_SYSPROP, DEFAULT_SCHEDULED_POOL_SIZE)
		                                     : scheduledPoolSize;
        return Math.max(1, size);
	}

	static int getCorePoolSize(Integer coreWorkerPoolSize) {
	    int size = coreWorkerPoolSize == null ? Integer.getInteger(CORE_POOL_SIZE_SYSPROP, DEFAULT_CORE_POOL_SIZE)
	                                          : coreWorkerPoolSize;
		return Math.max(0, size);
	}
	
	static int getIdleThreadTimeout(Integer idleThreadTimeout) {
	    int timeout = idleThreadTimeout == null ? Integer.getInteger(IDLE_THREAD_TIMEOUT_SYSPROP, DEFAULT_IDLE_THREAD_TIMEOUT)
	                                            : idleThreadTimeout;
        return Math.max(0, timeout);
	}
	
	static int getMaxWorkerPoolSize(int coreWorkerPoolSize, Integer maxWorkerPoolSize) {
	    // while core pool size is allowed to be zero, max pool size
	    // must be greater than the core pool size AND greater than
	    // 0.
	    int leastUpperBound = Math.max(1, coreWorkerPoolSize);
	    Integer size = maxWorkerPoolSize == null ? Integer.getInteger(MAX_WORKER_POOL_SIZE_SYSPROP, DEFAULT_MAX_WORKER_POOL_SIZE)
	                                             : maxWorkerPoolSize;
        return Math.max(leastUpperBound, size);
	}

	/**
	 * Creates a task manager that uses the default or system property specified values for core pool size,
	 * max pool size, idle thread timeout and scheduled pool size.
	 */
	public TaskManager() {
	    this(null, null, null, null);
	}
	
	/**
	 * Allows the construction of a task manager with explicitly specified values for each of core pool size,
	 * max pool size, idle thread timeout and scheduled pool size. If null is passed for any of these parameters,
	 * the system property value, if specified, will be used. Failing that, a sensible default will be applied.
	 * 
	 * @param coreWorkerPoolSize the number of threads that will be maintained in the executor service. 
	 * @param maxWorkerPoolSize the maximum number of threads that will be allowed in the executor service.
	 * @param idleThreadTimeoutSecs the minimum amount of time that additional threads (beyond the core pool size) 
	 * will stay alive before terminating.
	 * @param scheduledPoolSize the number of threads that will be used for the execution of deferred and periodic
	 * tasks.
	 */
	public TaskManager(Integer coreWorkerPoolSize, Integer maxWorkerPoolSize, Integer idleThreadTimeoutSecs, Integer scheduledPoolSize) {
	    NamedThreadFactory NTF = new NamedThreadFactory("JxtaTaskMonitor");
            monitoringExecutor = Executors.newSingleThreadScheduledExecutor(NTF);
            int corePoolSize = getCorePoolSize(coreWorkerPoolSize);
            normalExecutor = new SharedThreadPoolExecutor(monitoringExecutor,
                                                          corePoolSize,
                                                          getMaxWorkerPoolSize(corePoolSize, maxWorkerPoolSize),
                                                          getIdleThreadTimeout(idleThreadTimeoutSecs),
                                                          TimeUnit.SECONDS,
                                                          new SynchronousQueue<Runnable>(),
                                                          new NamedThreadFactory("JxtaWorker"));
            scheduledExecutor = new SharedScheduledThreadPoolExecutor(monitoringExecutor, getScheduledPoolSize(scheduledPoolSize), new NamedThreadFactory("JxtaScheduledWorker"));
            cachedExecutor = new CachedThreadExecutorService(NTF);
            proxiedExecutors = Collections.synchronizedMap(new HashMap<String, ProxiedScheduledExecutorService>());
            started=true;
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
	 * Provides a cached thread executor service.
	 * Note that since this instance could be shared, it is illegal to attempt to shut down the
	 * provided instance (an IllegalStateException will be thrown).
	 */
	public ExecutorService getCachedExecutorService() {
		return cachedExecutor;
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
                cachedExecutor.shutdownShared();
		
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
