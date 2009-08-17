package net.jxta.impl.cm;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.jxta.impl.cm.Srdi.SrdiInterface;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class SrdiPeriodicPushTaskTest extends MockObjectTestCase {

    private static final long PUSH_INTERVAL = 10000L;
    
    private SrdiInterface srdiInterfaceMock;
    private ScheduledExecutorService executorServiceMock;
    private ScheduledFuture<?> runHandleMock;
    
    private SrdiPeriodicPushTask pushTask;
    
    @Override
    protected void setUp() throws Exception {
        srdiInterfaceMock = mock(SrdiInterface.class);
        executorServiceMock = mock(ScheduledExecutorService.class);
        runHandleMock = mock(ScheduledFuture.class);
        
        pushTask = new SrdiPeriodicPushTask("testHandler", srdiInterfaceMock, executorServiceMock, PUSH_INTERVAL);
    }
    
    @Override
    protected void tearDown() throws Exception {
        
    }
    
    public void testExecutesSelfOnStart() {
        checkStartPushTask();
    }

    private void checkStartPushTask() {
        checking(new Expectations() {{
            one(executorServiceMock).scheduleWithFixedDelay(pushTask, 0L, PUSH_INTERVAL, TimeUnit.MILLISECONDS); will(returnValue(runHandleMock));
        }});
        
        pushTask.start();
    }
    
    public void testPushesAllEntriesOnFirstRun() {
        checkStartPushTask();
        
        checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
    }
    
    public void testPushesDeltasAfterPushingAllEntries() {
        checkStartPushTask();
        
        checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
        
        checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(false);
        }});
        
        pushTask.run();
    }
    
    public void testCancelsSelfOnStop() {
        checkStartPushTask();
        checkSelfCancelsOnStop();
    }

    private void checkSelfCancelsOnStop() {
        checking(new Expectations() {{
            one(runHandleMock).cancel(false);
        }});
        pushTask.stop();
    }
    
    public void testPushesAllEntriesAfterRestart() {
        checkStartPushTask();
        checkSelfCancelsOnStop();
        checkStartPushTask();
        
        checking(new Expectations() {{
            one(srdiInterfaceMock).pushEntries(true);
        }});
        
        pushTask.run();
    }
    
    public void testMultipleStartCallsIgnored() {
        checkStartPushTask();
        
        checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        pushTask.start();
    }
    
}
