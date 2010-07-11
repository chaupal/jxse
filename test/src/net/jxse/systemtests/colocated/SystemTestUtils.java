package net.jxse.systemtests.colocated;

import java.io.IOException;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaServerPipe;
import net.jxta.util.ServerPipeAcceptListener;

public class SystemTestUtils {

    private static final String TEST_NAMESPACE = "SystemTest";
    private static final String STRING_PAYLOAD_ELEMENT = "strPayload";

    public static JxtaServerPipe createServerPipe(NetworkManager manager, ServerPipeAcceptListener listener) throws IOException {
        PipeID pipeId = IDFactory.newPipeID(manager.getNetPeerGroup().getPeerGroupID());
        PipeAdvertisement pipeAd = createUnicastPipeAd(pipeId);
        if(listener == null) {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd);
        } else {
            return new JxtaServerPipe(manager.getNetPeerGroup(), pipeAd, listener);
        }
    }
    
    public static PipeAdvertisement createUnicastPipeAd(PipeID pipeID) {
        PipeAdvertisement advertisement = (PipeAdvertisement)
        AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        
        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.UnicastType);
        
        return advertisement;
    }
    
    public static Message createMessage(String payload) {
        Message msg = new Message();
        msg.addMessageElement(TEST_NAMESPACE, new StringMessageElement(STRING_PAYLOAD_ELEMENT, payload, null));
        return msg;
    }
}
