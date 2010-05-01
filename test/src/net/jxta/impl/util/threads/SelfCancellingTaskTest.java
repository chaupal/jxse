package net.jxta.impl.util.threads;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.jmock.Expectations;


//public class SelfCancellingTaskTest extends MockObjectTestCase {
public class SelfCancellingTaskTest extends TestCase {
	public void testCancel() throws Exception {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		SelfCancellingTaskTester tester = new SelfCancellingTaskTester(5);
		ScheduledFuture<?> handle = executor.scheduleAtFixedRate(tester, 100L, 100L, TimeUnit.MILLISECONDS);
		tester.setHandle(handle);
		
		Thread.sleep(1000L);
		
		executor.shutdown();
		boolean terminated = executor.awaitTermination(100L, TimeUnit.MILLISECONDS);
		assertTrue(terminated);
		assertTrue(handle.isCancelled());
		assertEquals(5, tester.getRunCount());
	}
	
	public void testCancel_whenHandleNotImmediatelySet() throws Exception {
		SelfCancellingTaskTester tester = new SelfCancellingTaskTester(1);
		tester.run();
		tester.run();
		
		assertEquals(1, tester.getRunCount());
		
//		final ScheduledFuture<?> handle = mock(ScheduledFuture.class);
//		checking(new Expectations() {{
//			one(handle).cancel(false);
//		}});
		
//		tester.setHandle(handle);
		tester.run();
	}
	
	public void testCancel_beforeTaskRun() {
		SelfCancellingTaskTester tester = new SelfCancellingTaskTester(1);
		tester.cancel();
		tester.run();
		
		assertEquals(0, tester.getRunCount());
	}
	
	private class SelfCancellingTaskTester extends SelfCancellingTask {
		
		private int cancelCount;

		public SelfCancellingTaskTester(int cancelCount) {
			this.cancelCount = cancelCount;
		}
		
		@Override
		public void execute() {
			if(getRunCount() >= cancelCount) {
				this.cancel();
			}
		}
		
	}
	
}
