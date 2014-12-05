package net.jxta.impl.endpoint.netty;

import java.util.Iterator;
import java.util.List;

import net.jxta.endpoint.EndpointAddress;

public interface TransportServerComponent extends TransportComponent {

    /**
     * @return the public addresses for this component - those which will be included in the
     * peer advertisement.
     */
    public Iterator<EndpointAddress> getPublicAddresses();

    /**
     * @return the physically bound addresses for this component, as opposed to those which are
     * broadcasted to external peers.
     */
    public List<EndpointAddress> getBoundAddresses();

}
