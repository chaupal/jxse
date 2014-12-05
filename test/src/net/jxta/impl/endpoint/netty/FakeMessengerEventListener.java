package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;

public class FakeMessengerEventListener implements MessengerEventListener {

    public FakeEndpointService owner;
    
    public FakeMessengerEventListener(FakeEndpointService owner) {
        this.owner = owner;
    }

    public boolean messengerReady(MessengerEvent event) {
        owner.messengers.add(event.getMessenger());
        return true;
    }
}
