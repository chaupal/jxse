package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.EndpointService;

/**
 * Basic "null object" implementation of the transport component, intended for extension
 * by more specific transport component types, to cleanly disable the expected functionality
 * within the transport.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public class NullTransportComponent implements TransportComponent {

    public void beginStop() {
        // nothing to do
    }

    public boolean start(EndpointService endpointService) {
        // we always succeed
        return true;
    }

    public void stop() {
        // nothing to do
    }

}
