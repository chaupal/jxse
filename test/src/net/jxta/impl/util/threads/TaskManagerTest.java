package net.jxta.impl.util.threads;

import junit.framework.TestCase;

public class TaskManagerTest extends TestCase {

	private String oldCorePoolSize;
	private String oldScheduledPoolSize;
	
	@Override
	protected void setUp() throws Exception {
		oldCorePoolSize = System.getProperty(TaskManager.CORE_POOL_SIZE_SYSPROP);
		oldScheduledPoolSize = System.getProperty(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP);

		System.clearProperty(TaskManager.CORE_POOL_SIZE_SYSPROP);
		System.clearProperty(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP);
	}
	
	@Override
	protected void tearDown() throws Exception {
		TaskManager.resetTaskManager();
		
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, oldCorePoolSize);
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
