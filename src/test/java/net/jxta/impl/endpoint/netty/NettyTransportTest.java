package net.jxta.impl.endpoint.netty;

import static org.junit.Assert.*;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.document.DOMXMLDocument;
import net.jxta.impl.document.DOMXMLElement;
import net.jxta.impl.protocol.GroupConfig;
import net.jxta.impl.protocol.ModuleImplAdv;
import net.jxta.impl.protocol.TCPAdv;
import net.jxta.peergroup.IModuleDefinitions;
import net.jxta.peergroup.core.Module;
import net.jxta.protocol.TransportAdvertisement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NettyTransportTest {
    
    private static final String[] NO_PARAMS = new String[0];
    
    private NettyTransport transport;
    private FakePeerGroup group;
    private ID assignedID;
    private ModuleImplAdv standardImplAdv;

    private GroupConfig groupConfig;
    
    @Before
    public void setUp() throws Exception {
        transport = new NettyTransport();
        group = new FakePeerGroup();
        groupConfig = (GroupConfig) new GroupConfig.Instantiator().newInstance();
        group.configAdv = groupConfig;
        
        TCPAdv instanceConfig = (TCPAdv) new TCPAdv.Instantiator().newInstance();
        // instanceConfig.setMulticastState(false);
        instanceConfig.setClientEnabled(true);
        instanceConfig.setServerEnabled(true);
        instanceConfig.setStartPort(7901);
        instanceConfig.setEndPort(7909);
        instanceConfig.setPort(7901);
        addInstanceConfigurationToGroupConfig(instanceConfig);
        
        assignedID = IModuleDefinitions.tcpProtoClassID;
        standardImplAdv = createImplAdvertisement(null);
    }
    
    @After
    public void tearDown() {
        transport.stopApp();
    }

    private ModuleImplAdv createImplAdvertisement(String protocolName) {
        ModuleImplAdv implAdv = (ModuleImplAdv) new ModuleImplAdv.Instantiator().newInstance();
        implAdv.setModuleSpecID(IModuleDefinitions.refTcpProtoSpecID);
        implAdv.setDescription("Reference Implementation of the JXSE 2.6+ HTTP Message Transport");
        implAdv.setCode(NettyTransport.class.getName());
        implAdv.setUri("http://jxta-jxse.dev.java.net/download/jxta.jar");
        implAdv.setProvider("sun.com");
        
        
        DOMXMLDocument staticConfiguration = (DOMXMLDocument) DOMXMLDocument.INSTANTIATOR.newInstance(MimeMediaType.XMLUTF8, "Param");
        if(protocolName != null) {
            DOMXMLElement protoElem = staticConfiguration.createElement("Proto", protocolName);
            staticConfiguration.appendChild(protoElem);
        }
        
        implAdv.setParam(staticConfiguration);
        
        return implAdv;
    }

    private XMLDocument<?> createCloneableAdv(TransportAdvertisement instanceConfig) {
        XMLDocument<?> instanceConfigDoc = (XMLDocument<?>) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
        StructuredDocumentUtils.copyElements(instanceConfigDoc, instanceConfigDoc, (XMLDocument<?>) instanceConfig.getDocument(MimeMediaType.XMLUTF8));
        return instanceConfigDoc;
    }
    
    @Test
    public void testGetProtocolName_changedThroughStaticConfig() throws PeerGroupException {
        transport.init(group, assignedID, createImplAdvertisement("testproto"));
        assertEquals("testproto", transport.getProtocolName());
    }
    
    @Test
    public void testStartApp_returnsStalledIfEndpointServiceNotAvailable() throws PeerGroupException {
        group.endpointService = null;
        transport.init(group, assignedID, standardImplAdv);
        assertEquals(Module.START_AGAIN_STALLED, transport.startApp(NO_PARAMS));
        group.endpointService = new FakeEndpointService(group);
    }
    
    @Test
    public void testStartApp_returnsErrorIfEndpointServiceRefusesRegistration() throws PeerGroupException {
        transport.init(group, assignedID, standardImplAdv);
        group.endpointService.refuseRegistration();
        assertEquals(-1, transport.startApp(NO_PARAMS));
    }
    
    // TODO: move to client and server tests
//    @Test
//    public void testStartApp_returnsDisabledIfClientAndServerDisabled() throws PeerGroupException {
//        HTTPAdv instanceConfig = (HTTPAdv) new HTTPAdv.Instantiator().newInstance();
//        instanceConfig.setServerEnabled(false);
//        instanceConfig.setClientEnabled(false);
//        
//        addInstanceConfigurationToGroupConfig(instanceConfig);
//        
//        final EndpointService endpointServiceMock = mockContext.mock(EndpointService.class);
//        final MessengerEventListener listenerMock = mockContext.mock(MessengerEventListener.class);
//        mockContext.checking(new Expectations() {{
//            one(group).getEndpointService(); will(returnValue(endpointServiceMock));
//            one(endpointServiceMock).addMessageTransport(transport); will(returnValue(listenerMock));
//        }});
//        
//        transport.init(group, assignedID, standardImplAdv);
//        assertEquals(Module.START_DISABLED, transport.startApp(NO_PARAMS));
//    }

    private void addInstanceConfigurationToGroupConfig(TCPAdv instanceConfig) {
        XMLDocument<?> instanceConfigDoc = createCloneableAdv(instanceConfig);
        groupConfig.putServiceParam(IModuleDefinitions.tcpProtoClassID, instanceConfigDoc);
    }
    
    // TODO: move to client and server tests
//    @Test
//    public void testStopApp_deregistersFromEndpointService() throws PeerGroupException {
//        transport.init(group, assignedID, standardImplAdv);
//        
//        final EndpointService endpointServiceMock = mockContext.mock(EndpointService.class);
//        mockContext.checking(new Expectations() {{
//            atLeast(1).of(group).getEndpointService(); will(returnValue(endpointServiceMock));
//            one(endpointServiceMock).addMessageTransport(transport);
//        }});
//        
//        transport.startApp(NO_PARAMS);
//        
//        mockContext.checking(new Expectations() {{
//            one(endpointServiceMock).removeMessageTransport(transport);
//        }});
//        transport.stopApp();
//    }
    
    // TODO: move to client and server tests
//    @Test
//    public void testStopApp_onlyDeregistersOnce() throws PeerGroupException {
//        transport.init(group, assignedID, standardImplAdv);
//        
//        final EndpointService endpointServiceMock = mockContext.mock(EndpointService.class);
//        mockContext.checking(new Expectations() {{
//            atLeast(1).of(group).getEndpointService(); will(returnValue(endpointServiceMock));
//            one(endpointServiceMock).addMessageTransport(transport);
//        }});
//        
//        transport.startApp(NO_PARAMS);
//        
//        mockContext.checking(new Expectations() {{
//            one(endpointServiceMock).removeMessageTransport(transport);
//        }});
//        transport.stopApp();
//        
//        // this second call should have no effect
//        transport.stopApp();
//    }
    
    // TODO: move to client and server tests
//    @Test
//    public void testTransportIsListedInModules() {
//        // TODO: check that we are listed in "/META-INF/services/net.jxta.platform.Module"
//        fail();
//    }
    
}
