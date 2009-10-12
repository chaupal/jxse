package net.jxta.impl.util.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	
	static final String CORE_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.corePoolSize";
	static final String MAX_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.maxPoolSize";
	static final String QUEUE_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.queueSize";
	static final String SCHEDULED_POOL_SIZE_SYSPROP = "net.jxta.util.threads.TaskManager.scheduledPoolSize";
	
	static final int DEFAULT_CORE_POOL_SIZE =4;
	static final int DEFAULT_MAX_POOL_SIZE = 16;
	static final int DEFAULT_QUEUE_SIZE = 1000;
	static final int DEFAULT_SCHEDULED_POOL_SIZE = 2;
	
	private static TaskManager singleton;
	
	private static SharedThreadPoolExecutor normalExecutor;
	private static SharedScheduledThreadPoolExecutor scheduledExecutor;
	
	private static boolean started;
	
	static int getScheduledPoolSize() {
		return Math.max(1, Integer.getInteger(SCHEDULED_POOL_SIZE_SYSPROP, DEFAULT_SCHEDULED_POOL_SIZE));
	}

	static int getQueueSize() {
		return Math.max(1, Integer.getInteger(QUEUE_SIZE_SYSPROP, DEFAULT_QUEUE_SIZE));
	}

	static int getMaxPoolSize() {
		return Math.max(1, Integer.getInteger(MAX_POOL_SIZE_SYSPROP, DEFAULT_MAX_POOL_SIZE));
	}

	static int getCorePoolSize() {
		return Math.max(1, Integer.getInteger(CORE_POOL_SIZE_SYSPROP, DEFAULT_CORE_POOL_SIZE));
	}

	public static TaskManager getTaskManager() {
		if(TaskManager.singleton == null) {
			TaskManager.singleton = new TaskManager();
			BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(getQueueSize());
			normalExecutor = new SharedThreadPoolExecutor(getCorePoolSize(), getMaxPoolSize(), 60000, TimeUnit.SECONDS, workQueue);
			scheduledExecutor = new SharedScheduledThreadPoolExecutor(getScheduledPoolSize());
			started=true;
		}
		return TaskManager.singleton;
	}
	
	/**
	 * discards any existing TaskManager singleton. Intended for testing
	 * purposes only.
	 */
	static void resetTaskManager() {
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
	 */
	public ScheduledExecutorService getLocalScheduledExecutorService() {
		return new ProxiedScheduledExecutorService(normalExecutor);
	}
	
	
	public void shutdown() {
		if(!started) {
			throw new IllegalStateException("Task manager is already shut down");
		}
		normalExecutor.shutdownShared();
		scheduledExecutor.shutdownShared();
		
		started = false;
	}
}
