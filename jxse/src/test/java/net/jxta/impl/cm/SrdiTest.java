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
import org.jmock.integration.junit3.MockObjectTestCase;

public class SrdiTest extends MockObjectTestCase {

    private PeerGroup groupMock;
    private SrdiPushEntriesInterface srdiInterfaceMock;
    private SrdiAPI srdiIndex;
    private RendezVousService rendezvousServiceMock;
    private ScheduledExecutorService executorServiceMock;
    private ScheduledFuture<?> srdiPeriodicPushTaskHandle;
    
    private static final long PUSH_INTERVAL = 10000L;
    private SrdiManager srdiManager;
    
    @Override
    protected void setUp() throws Exception {
        groupMock = mock(PeerGroup.class);
        srdiInterfaceMock = mock(SrdiPushEntriesInterface.class);
        srdiIndex = mock(SrdiAPI.class);
        rendezvousServiceMock = mock(RendezVousService.class);
        executorServiceMock = mock(ScheduledExecutorService.class);
        srdiPeriodicPushTaskHandle = mock(ScheduledFuture.class);
        
        checking(new Expectations() {{
            ignoring(groupMock).getResolverService();
            atLeast(1).of(groupMock).getRendezVousService(); will(returnValue(rendezvousServiceMock));
            one(rendezvousServiceMock).addListener(with(any(SrdiManager.class)));
        }});
        
        srdiManager = new SrdiManager(groupMock, "testHandler", srdiInterfaceMock, srdiIndex);
    }
    
    public void testStartPush() {
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushIgnoredIfPeerIsRdvInGroup() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(true));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushIgnoredIfRdvConnectionNotEstablished() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushIgnoredIfRdvServiceIsInAdHocMode() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testStartPushSchedulesSrdiPeriodicPushTask() {
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
    }
    
    public void testRendezvousConnectEventRestartsPush() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }
    
    public void testRendezvousConnectEventIgnoredIfModeIsAdHoc() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }
    
    public void testRendezvousConnectIgnoredIfPushNotStarted() {
        checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVCONNECT, null));
    }
    
    public void testRendezvousDisconnectEventStopsPush() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVDISCONNECT, null));
    }
    
    public void testBecameRendezvousEventStopsPush() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMERDV, null));
    }
    
    public void testBecameRendezvousEventIgnoredIfNoPushNotStarted() {
        checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMERDV, null));
    }
    
    public void testBecameEdgeEventStartsPush() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    
    public void testBecameEdgeEventIgnoredIfNotConnectedToRendezvous() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(false));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    
    public void testBecameEdgeEventIgnoredIfRendezvousIsInAdHocMode() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(true));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.ADHOC));
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    
    public void testBecameEdgeEventIgnoredIfPushNotStarted() {
        checking(new Expectations() {{
            never(executorServiceMock);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.BECAMEEDGE, null));
    }
    
    public void testRdvFailedEventStopsPush() {
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            one(rendezvousServiceMock).isConnectedToRendezVous(); will(returnValue(true));
            one(rendezvousServiceMock).getRendezVousStatus(); will(returnValue(RendezVousStatus.EDGE));
            one(executorServiceMock).scheduleWithFixedDelay(with(any(SrdiManagerPeriodicPushTask.class)), with(equal(0L)), with(equal(PUSH_INTERVAL)), with(equal(TimeUnit.MILLISECONDS)));
            will(returnValue(srdiPeriodicPushTaskHandle));
        }});
        
        srdiManager.startPush(executorServiceMock, PUSH_INTERVAL);
        
        checking(new Expectations() {{
            one(srdiPeriodicPushTaskHandle).cancel(false);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.RDVFAILED, null));
    }
    
    public void testClientFailedEventRemovesPeerFromSrdiIndexIfCurrentlyARendezvous() throws IOException {
        final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(true));
            one(srdiIndex).remove(peerId);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.CLIENTFAILED, peerId));
    }
    
    public void testClientFailedEventIgnoredIfNotARendezvous() throws IOException {
        final PeerID peerId = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
        
        checking(new Expectations() {{
            one(groupMock).isRendezvous(); will(returnValue(false));
            never(srdiIndex).remove(peerId);
        }});
        
        srdiManager.rendezvousEvent(new RendezvousEvent(new Object(), RendezvousEvent.CLIENTFAILED, peerId));
    }
}
