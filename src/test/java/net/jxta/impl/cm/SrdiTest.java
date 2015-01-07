package net.jxta.impl.cm;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.jxta.id.IDFactory;
import net.jxta.impl.cm.SrdiManager.SrdiPushEntriesInterface;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezVousStatus;
import net.jxta.rendezvous.RendezvousEvent;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Needs Updating to junit4 and tests revisited")
public class SrdiTest {

    private PeerGroup groupMock;
    private SrdiPushEntriesInterface srdiInterfaceMock;
    private SrdiAPI srdiIndex;
    private RendezVousService rendezvousServiceMock;
    private ScheduledExecutorService executorServiceMock;
    private ScheduledFuture<?> srdiPeriodicPushTaskHandle;
    
    private static final long PUSH_INTERVAL = 10000L;
    private SrdiManager srdiManager;
    private JUnit4Mockery mockery;

    @Before
    protected void setUp() throws Exception {
        mockery = new JUnit4Mockery();
        groupMock = mockery.mock(PeerGroup.class);
        srdiInterfaceMock = mockery.mock(SrdiPushEntriesInterface.class);
        srdiIndex = mockery.mock(SrdiAPI.class);
        rendezvousServiceMock = mockery.mock(RendezVousService.class);
        executorServiceMock = mockery.mock(ScheduledExecutorService.class);
        srdiPeriodicPushTaskHandle = mockery.mock(ScheduledFuture.class);
        
        mockery.checking(new Expectations() {{
            ignoring(groupMock).getResolverService();
            atLeast(1).of(groupMock).getRendezVousService(); will(returnValue(rendezvousServiceMock));
            oneOf(rendezvousServiceMock).addListener(with(any(SrdiManager.class)));
        }});
        
        srdiManager = new SrdiManager(groupMock, "testHandler", srdiInterfaceMock, srdiIndex);
    }
    
    @Test
    public void testStartPush() {
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    @Test
    public void testStartPushIgnoredIfPeerIsRdvInGroup() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(true));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    @Test
    public void testStartPushIgnoredIfRdvConnectionNotEstablished() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushIgnoredIfRdvServiceIsInAdHocMode() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushSchedulesSrdiPeriodicPushTask() {
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }

    @Ignore("Needs updating")
    public void testRendezvousConnectEventRestartsPush() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }

    @Ignore("Needs updating")
    public void testRendezvousConnectEventIgnoredIfModeIsAdHoc() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }

    @Ignore("Needs updating")
    public void testRendezvousConnectIgnoredIfPushNotStarted() {
        mockery.checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }
    @Ignore("Needs updating")
    public void testRendezvousDisconnectEventStopsPush() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVDISCONNECT, null));
    }
    @Ignore("Needs updating")
    public void testBecameRendezvousEventStopsPush() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMERDV, null));
    }
    @Ignore("Needs updating")
    public void testBecameRendezvousEventIgnoredIfNoPushNotStarted() {
        mockery.checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMERDV, null));
    }
    @Ignore("Needs updating")
    public void testBecameEdgeEventStartsPush() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    @Ignore("Needs updating")
    public void testBecameEdgeEventIgnoredIfNotConnectedToRendezvous() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    @Ignore("Needs updating")
    public void testBecameEdgeEventIgnoredIfRendezvousIsInAdHocMode() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    @Ignore("Needs updating")
    public void testBecameEdgeEventIgnoredIfPushNotStarted() {
        mockery.checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    @Ignore("Needs updating")
    public void testRdvFailedEventStopsPush() {
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            oneOf(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            oneOf(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            oneOf(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        mockery.checking(new Expectations() {{
            oneOf(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVFAILED, null));
    }
    @Ignore("Needs updating")
    public void testClientFailedEventRemovesPeerFromSrdiIndexIfCurrentlyARendezvous() throws IOException {
        final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(true));
            oneOf(srdiIndex).remove(peerId);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.CLIENTFAILED, peerId));
    }
    @Ignore("Needs updating")
    public void testClientFailedEventIgnoredIfNotARendezvous() throws IOException {
        final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
        
        mockery.checking(new Expectations() {{
            oneOf(groupMock).isRendezvous(); will(returnValue(false));
            never(srdiIndex).remove(peerId);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.CLIENTFAILED, peerId));
    }
}
