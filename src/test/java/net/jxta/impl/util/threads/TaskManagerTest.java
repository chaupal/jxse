package net.jxta.impl.util.threads;

import java.util.concurrent.ExecutorService;
import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TaskManagerTest {

    private Properties oldProperties;
	
	@Before
	public void setUp() throws Exception {
		oldProperties = System.getProperties();
	    
		System.clearProperty(TaskManager.CORE_POOL_SIZE_SYSPROP);
		System.clearProperty(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP);
		System.clearProperty(TaskManager.IDLE_THREAD_TIMEOUT_SYSPROP);
		System.clearProperty(TaskManager.MAX_WORKER_POOL_SIZE_SYSPROP);
	}
	
	@After
	public void tearDown() throws Exception {
	    System.setProperties(oldProperties);
	}
	
	private void setSysProp(String property, String value) {
		if(value != null) {
			System.setProperty(property, value);
		} else {
			System.clearProperty(property);
		}
	}

	@Test
	public void testCorePoolSize_hasDefault() {
		assertEquals(TaskManager.DEFAULT_CORE_POOL_SIZE, TaskManager.getCorePoolSize(null));
	}
	
	@Test
	public void testCorePoolSize_usesSysProp() {
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getCorePoolSize(null));
	}
	
	@Test
	public void testCorePoolSize_specifiedAsParam() {
	    assertEquals(5, TaskManager.getCorePoolSize(5));
	}
	
	@Test
	public void testCorePoolSize_specifiedAsParam_minimumEnforced() {
	    assertEquals(0, TaskManager.getCorePoolSize(-2));
	}

	@Test
	public void testCorePoolSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.CORE_POOL_SIZE_SYSPROP, "-4");
		assertEquals(0, TaskManager.getCorePoolSize(null));
	}
	
	@Test
	public void testScheduledPoolSize_usesSysProp() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "100");
		assertEquals(100, TaskManager.getScheduledPoolSize(null));
	}
	
	@Test
	public void testScheduledPoolSize_hasMinimum_handlesNegativeNumbers() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "-4");
		assertEquals(1, TaskManager.getScheduledPoolSize(null));
	}
	
	@Test
	public void testScheduledPoolSize_hasMinimum_handlesZero() {
		setSysProp(TaskManager.SCHEDULED_POOL_SIZE_SYSPROP, "0");
		assertEquals(1, TaskManager.getScheduledPoolSize(null));
	}
	
	@Test
	public void testScheduledPoolSize_specifiedAsParameter() {
	    assertEquals(13, TaskManager.getScheduledPoolSize(13));
	}
	
	@Test
	public void testScheduledPoolSize_specifiedAsParameter_hasMinimum() {
	    assertEquals(1, TaskManager.getScheduledPoolSize(0));
	}
	
	@Test
	public void testIdleThreadTimeout_hasDefault() {
	    assertEquals(TaskManager.DEFAULT_IDLE_THREAD_TIMEOUT, TaskManager.getIdleThreadTimeout(null));
	}
	
	@Test
	public void testIdleThreadTimeout_usesSysProp() {
	    setSysProp(TaskManager.IDLE_THREAD_TIMEOUT_SYSPROP, "100");
	    assertEquals(100, TaskManager.getIdleThreadTimeout(null));
	}
	
	@Test
	public void testIdleThreadTimeout_hasMinimum() {
	    setSysProp(TaskManager.IDLE_THREAD_TIMEOUT_SYSPROP, "-1");
	    assertEquals(0, TaskManager.getIdleThreadTimeout(null));
	}
	
	@Test
	public void testIdleThreadTimeout_specifiedAsParameter() {
	    assertEquals(5, TaskManager.getIdleThreadTimeout(5));
	}
	
	@Test
	public void testIdleThreadTimeout_specifiedAsParameter_minimumEnforced() {
	    assertEquals(0, TaskManager.getIdleThreadTimeout(-1));
	}
	
	@Test
	public void testMaxWorkerPoolSize_hasDefault() {
        assertEquals(TaskManager.DEFAULT_MAX_WORKER_POOL_SIZE, TaskManager.getMaxWorkerPoolSize(0, null));
    }
    
	@Test
    public void testMaxWorkerPoolSize_usesSysProp() {
        setSysProp(TaskManager.MAX_WORKER_POOL_SIZE_SYSPROP, "100");
        assertEquals(100, TaskManager.getMaxWorkerPoolSize(5, null));
    }
    
	@Test
    public void testMaxWorkerPoolSize_hasMinimum() {
        setSysProp(TaskManager.MAX_WORKER_POOL_SIZE_SYSPROP, "0");
        assertEquals(1, TaskManager.getMaxWorkerPoolSize(0, null));
    }
    
	@Test
    public void testMaxWorkerPoolSize_specifiedAsParameter() {
        assertEquals(5, TaskManager.getMaxWorkerPoolSize(0, 5));
    }
    
	@Test
    public void testMaxWorkerPoolSize_specifiedAsParameter_minimumEnforced() {
        assertEquals(1, TaskManager.getMaxWorkerPoolSize(0, 0));
    }
    
	@Test
    public void testMaxWorkerPoolSize_specifiedAsParameter_greaterThanCorePoolSize() {
        assertEquals(5, TaskManager.getMaxWorkerPoolSize(5, 3));
    }
    
    @Test(expected=IllegalStateException.class)
    public void testShutdownTwiceThrowsException () {
        TaskManager t = new TaskManager();
        t.shutdown();
        t.shutdown();
    }

    @Test
    public void testCachedExecutorServiceInstance() {

        TaskManager t = new TaskManager();
        assertNotNull("Unexpected null TaskManager", t);

        ExecutorService item = t.getCachedExecutorService();
        assertNotNull("Unexpected null CachedExecutorService", item);

    }

}
