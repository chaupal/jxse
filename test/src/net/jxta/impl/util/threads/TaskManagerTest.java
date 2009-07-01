package net.jxta.impl.util.threads;

import junit.framework.TestCase;

public class TaskManagerTest extends TestCase {

	private String oldCorePoolSize;
	private String oldMaxPoolSize;
	private String oldQueueSize;
	private String oldScheduledPoolSize;
	
	@Override
	protected void setUp() throws Exception {
		oldCorePoolSize = System.getProperty(TaskManager.CORE_POOL_SIZE_SYSPROP);
		oldMaxPoolSize = System.getProperty(TaskManager.MAX_POOL_SIZE_SYSPROP);
		oldQueueSize = System.getProperty(TaskManager.QUEUE_SIZE_SYSPROP);
		oldScheduledPoolSize = System.getProperty(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP);

		System.clearProperty(TaskManager.CORE_POOL_SIZE_SYSPROP);
		System.clearProperty(TaskManager.MAX_POOL_SIZE_SYSPROP);
		System.clearProperty(TaskManager.QUEUE_SIZE_SYSPROP);
		System.clearProperty(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP);
	}
	
	@Override
	protected void tearDown() throws Exception {
		TaskManager.resetTaskManager();
		
		
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, oldCorePoolSize);
		setSysProp(TaskManager.MAX_POOL_SIZE_SYSPROP, oldMaxPoolSize);
		setSysProp(TaskManager.QUEUE_SIZE_SYSPROP, oldQueueSize);
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, oldScheduledPoolSize);
	}
	
	private void setSysProp(String property, String value) {
		if(value != null) {
			System.setProperty(property, value);
		} else {
			System.clearProperty(property);
		}
	}

	public void testDefaultCorePoolSize_forDefaultWorkType() {
		assertEquals(TaskManager.DEFAULT_CORE_POOL_SIZE, TaskManager.getCorePoolSize());
	}
	
	public void testCorePoolSize_usesSysProp() {
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getCorePoolSize());
	}

	public void testCorePoolSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, "-4");
		assertEquals(1, TaskManager.getCorePoolSize());
	}
	
	public void testCorePoolSize_hasMinimum_handlesZero() {
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, "0");
		assertEquals(1, TaskManager.getCorePoolSize());
	}
	
	public void testMaxPoolSize_usesSysProp() {
		setSysProp(TaskManager.MAX_POOL_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getMaxPoolSize());
	}
	
	public void testMaxPoolSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.MAX_POOL_SIZE_SYSPROP, "-4");
		assertEquals(1, TaskManager.getMaxPoolSize());
	}
	
	public void testMaxPoolSize_hasMinimum_handlesZero() {
		setSysProp(TaskManager.MAX_POOL_SIZE_SYSPROP, "0");
		assertEquals(1, TaskManager.getMaxPoolSize());
	}
	
	public void testQueueSize_usesSysProp() {
		setSysProp(TaskManager.QUEUE_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getQueueSize());
	}
	
	public void testQueueSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.QUEUE_SIZE_SYSPROP, "-4");
		assertEquals(1, TaskManager.getQueueSize());
	}
	
	public void testQueueSize_hasMinimum_handlesZero() {
		setSysProp(TaskManager.QUEUE_SIZE_SYSPROP, "0");
		assertEquals(1, TaskManager.getQueueSize());
	}
	
	public void testScheduledPoolSize_usesSysProp() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getScheduledPoolSize());
	}
	
	public void testScheduledPoolSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "-4");
		assertEquals(1, TaskManager.getScheduledPoolSize());
	}
	
	public void testScheduledPoolSize_hasMinimum_handlesZero() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "0");
		assertEquals(1, TaskManager.getScheduledPoolSize());
	}
}
