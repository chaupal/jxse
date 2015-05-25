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
import net.jxta.platform.JxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
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
    public PeerGroupID peerGroupId = PeerGroupID.NET_PEER_GROUP_ID;
    public PeerID peerId = DEFAULT_PEER_ID;
    public String peerGroupName = "FakeGroupForTesting";
    public String peerName = "FakePeerForTesting";
    
    public FakeEndpointService endpointService = new FakeEndpointService(this);

    public ConfigParams configAdv;
    
    @Override
    public PeerGroup getParentGroup() {
        return parentGroup;
    }
    
    @Override
    public PeerGroupID getPeerGroupID() {
        return peerGroupId;
    }
    
    @Override
    public String getPeerGroupName() {
        return peerGroupName;
    }

    @Override
    public PeerID getPeerID() {
        return peerId;
    }
    
    @Override
    public String getPeerName() {
        return peerName;
    }
    
    @Override
    public EndpointService getEndpointService() {
        return endpointService;
    }
    
    @Override
    public ConfigParams getConfigAdvertisement() {
        return configAdv;
    }
    
    /* UNIMPLEMENTED, IRRELEVANT METHODS BEYOND THIS POINT */
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean compatible(Element compat) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public AccessService getAccessService() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ModuleImplAdvertisement getAllPurposePeerGroupImplAdvertisement() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public DiscoveryService getDiscoveryService() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GlobalRegistry getGlobalRegistry()
    {
        throw new UnsupportedOperationException("getGlobalRegistry not implemented");
    }

    public ThreadGroup getHomeThreadGroup() {
        throw new RuntimeException("not implemented");
    }

    public JxtaLoader getLoader() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public MembershipService getMembershipService() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PeerAdvertisement getPeerAdvertisement() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PeerGroupAdvertisement getPeerGroupAdvertisement() {
        throw new RuntimeException("not implemented");
    }    
    
    @Override
    public PeerInfoService getPeerInfoService() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PipeService getPipeService() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public RendezVousService getRendezVousService() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ResolverService getResolverService() {
        throw new RuntimeException("not implemented");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator getRoleMap(ID name) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public URI getStoreHome() {
        throw new RuntimeException("not implemented");
    }

    public PeerGroup getWeakInterface() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isRendezvous() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Module loadModule(ID assignedID, Advertisement impl) throws ProtocolNotSupportedException, PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Module loadModule(ID assignedID, ModuleSpecID specID, int where) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Service lookupService(ID name) throws ServiceNotFoundException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Service lookupService(ID name, int roleIndex) throws ServiceNotFoundException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PeerGroup newGroup(PeerGroupAdvertisement peerGroupAdvertisement) throws PeerGroupException {
        throw new RuntimeException("Not implemented");
    }

    @Deprecated
    @Override
    public PeerGroup newGroup(PeerGroupID gid, ModuleImplAdvertisement moduleImplementationAdvertisement, String name, String description) throws PeerGroupException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public PeerGroup newGroup(PeerGroupID gid, ModuleImplAdvertisement moduleImplementationAdvertisement, String name, String description, boolean publish) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PeerGroup newGroup(PeerGroupID peerGroupId) throws PeerGroupException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void publishGroup(String name, String description) throws IOException {
        throw new RuntimeException("not implemented");
    }

//    public boolean unref() {
//        throw new RuntimeException("not implemented");
//    }

    @Override
    public Advertisement getImplAdvertisement() {
        throw new RuntimeException("not implemented");
    }

//    public PeerGroup getInterface() {
//        throw new RuntimeException("not implemented");
//    }

    @Override
    public void init(PeerGroup group, ID assignedID, Advertisement implAdv) throws PeerGroupException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int startApp(String[] args) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void stopApp() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ContentService getContentService() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public TaskManager getTaskManager() {
        throw new RuntimeException("not implemented");
    }
}
