package net.jxta.impl.endpoint.netty;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import net.jxta.access.AccessService;
import net.jxta.content.ContentService;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.endpoint.EndpointService;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.id.ID;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeService;
import net.jxta.peergroup.core.Module;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.resolver.ResolverService;
import net.jxta.service.Service;

public class FakePeerGroup implements PeerGroup {

    public static final PeerID DEFAULT_PEER_ID = PeerID.create(URI.create("urn:jxta:uuid-59616261646162614E50472050325033857CA397BC2A48DB86FED88696A80AA003"));
    
    public PeerGroup parentGroup = null;
    public PeerGroupID peerGroupId = PeerGroupID.defaultNetPeerGroupID;
    public PeerID peerId = DEFAULT_PEER_ID;
    public String peerGroupName = "FakeGroupForTesting";
    public String peerName = "FakePeerForTesting";
    
    public FakeEndpointService endpointService = new FakeEndpointService(this);

    public ConfigParams configAdv;
    
    public PeerGroup getParentGroup() {
        return parentGroup;
    }
    
    public PeerGroupID getPeerGroupID() {
        return peerGroupId;
    }
    
    public String getPeerGroupName() {
        return peerGroupName;
    }

    public PeerID getPeerID() {
        return peerId;
    }
    
    public String getPeerName() {
        return peerName;
    }
    
    public EndpointService getEndpointService() {
        return endpointService;
    }
    
    public ConfigParams getConfigAdvertisement() {
        return configAdv;
    }
    
    /* UNIMPLEMENTED, IRRELEVANT METHODS BEYOND THIS POINT */
    
    public boolean compatible(Element<?> compat) {
        throw new RuntimeException("not implemented");
    }

    public AccessService getAccessService() {
        throw new RuntimeException("not implemented");
    }

    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() throws Exception {
        throw new RuntimeException("not implemented");
    }

    public DiscoveryService getDiscoveryService() {
        throw new RuntimeException("not implemented");
    }

    public GlobalRegistry getGlobalRegistry()
    {
        throw new UnsupportedOperationException("getGlobalRegistry not implemented");
    }

    public ThreadGroup getHomeThreadGroup() {
        throw new RuntimeException("not implemented");
    }

   public MembershipService getMembershipService() {
        throw new RuntimeException("not implemented");
    }

    public PeerAdvertisement getPeerAdvertisement() {
        throw new RuntimeException("not implemented");
    }

    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        throw new RuntimeException("not implemented");
    }    
    
    public PeerInfoService getPeerInfoService() {
        throw new RuntimeException("not implemented");
    }

    public PipeService getPipeService() {
        throw new RuntimeException("not implemented");
    }

    public RendezVousService getRendezVousService() {
        throw new RuntimeException("not implemented");
    }

    public ResolverService getResolverService() {
        throw new RuntimeException("not implemented");
    }

    public Iterator<ID> getRoleMap(ID name) {
        throw new RuntimeException("not implemented");
    }

    public URI getStoreHome() {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup getWeakInterface() {
        throw new RuntimeException("not implemented");
    }

    public boolean isRendezvous() {
        throw new RuntimeException("not implemented");
    }

    public Module loadModule(ID assignedID, Advertisement impl) throws ProtocolNotSupportedException, PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public Module loadModule(ID assignedID, ModuleSpecID specID, int where) {
        throw new RuntimeException("not implemented");
    }

    public Service lookupService(ID name) throws ServiceNotFoundException {
        throw new RuntimeException("not implemented");
    }

    public Service lookupService(ID name, int roleIndex) throws ServiceNotFoundException {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup newGroup(Advertisement pgAdv) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup newGroup(PeerGroupID gid, Advertisement impl, String name, String description, boolean publish) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup newGroup(PeerGroupID gid) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public void publishGroup(String name, String description) throws IOException {
        throw new RuntimeException("not implemented");
    }

//    public boolean unref() {
//        throw new RuntimeException("not implemented");
//    }

    public Advertisement getImplAdvertisement() {
        throw new RuntimeException("not implemented");
    }

//    public PeerGroup getInterface() {
//        throw new RuntimeException("not implemented");
//    }

    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    public int startApp(String[] args) {
        throw new RuntimeException("not implemented");
    }

    public void stopApp() {
        throw new RuntimeException("not implemented");
    }

    public ContentService getContentService() {
        throw new RuntimeException("not implemented");
    }

    public TaskManager getTaskManager() {
        throw new RuntimeException("not implemented");
    }

}
