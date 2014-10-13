package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.EndpointAddress;

public interface TransportClientComponent extends TransportComponent {

    /**
     * @return the public address for the client, i.e. the address the client will
     * declare as it's address when connected to remote peers.
     */
    public EndpointAddress getPublicAddress();
    
}
