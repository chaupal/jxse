package net.jxta.impl.endpoint.netty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jxta.endpoint.EndpointAddress;

/**
 * Basic "null object" implementation of the TransportServerComponent interface, intended to be used
 * to cleanly disable the server functionality in a transport.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class NullTransportServerComponent extends NullTransportComponent implements TransportServerComponent {

    public List<EndpointAddress> getBoundAddresses() {
        return new ArrayList<EndpointAddress>(0);
    }

    public Iterator<EndpointAddress> getPublicAddresses() {
        return new ArrayList<EndpointAddress>(0).iterator();
    }

}
