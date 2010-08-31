package net.jxta.impl.cm;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.jxta.impl.cm.SrdiManager.SrdiPushEntriesInterface;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SrdiManagerPeriodicPushTaskTest {

    private static final long PUSH_INTERVAL = 10000L;
    
    private SrdiPushEntriesInterface srdiInterfaceMock;
    private ScheduledExecutorService executorServiceMock;
    private ScheduledFuture<?> runHandleMock;
    
    private SrdiManagerPeriodicPushTask pushTask;
    private JUnit4Mockery mockery;

    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery();
        srdiInterfaceMock = mockery.mock(SrdiPushEntriesInterface.class);
        executorServiceMock = mockery.mock(ScheduledExecutorService.class);
        runHandleMock = mockery.mock(ScheduledFuture.class);
        
        pushTask = new SrdiManagerPeriodicPushTask("testHandler", srdiInterfaceMock, executorServiceMock, PUSH_INTERVAL);
    }
    
    @After
    public void tearDown() throws Exception {
        
    }
    @Test
    public void testExecutesSelfOnStart() {
        checkStartPushTask();
    }

    private void checkStartPushTask() {
        mockery.checking(new Expectations() {{
            one(executorServiceMock).scheduleWithFixedDelay(pushTask, 0L, PUSH_INTERVAL, TimeUnit.MILLISECONDS); will(returnValue(runHandleMock));
        }});
        
        pushTask.start();
    }
    @Test
    public void testPushesAllEntriesOnFirstRun() {
        checkStartPushTask();
        
        mockery.checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
    }
    @Test
    public void testPushesDeltasAfterPushingAllEntries() {
        checkStartPushTask();
        
        mockery.checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
        
        mockery.checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(false);
        }});
        
        pushTask.run();
    }
    
    @Test
        public void testCancelsSelfOnStop() {
        checkStartPushTask();
        checkSelfCancelsOnStop();
    }

    private void checkSelfCancelsOnStop() {
        mockery.checking(new Expectations() {{
            one(runHandleMock).cancel(false);
        }});
        pushTask.stop();
    }

    @Test
    public void testPushesAllEntriesAfterRestart() {
        checkStartPushTask();
        checkSelfCancelsOnStop();
        checkStartPushTask();
        
        mockery.checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
    }
    
    @Test
    public void testMultipleStartCallsIgnored() {
        checkStartPushTask();
        
        mockery.checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        pushTask.start();
    }
    
}
