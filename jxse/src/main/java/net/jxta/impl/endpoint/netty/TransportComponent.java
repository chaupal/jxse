package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.EndpointService;

/**
 * Represents a (potentially optional) component of a transport implementation,
 * which can be started and stopped.
 */
public interface TransportComponent {

    /**
     * Start the component, which may register itself with the provided endpoint service
     * to provide functionality to the wider JXTA stack.
     * @return whether the startup sequence succeeded or not.
     */
    public boolean start(EndpointService endpointService);
    
    /**
     * Instruct the component to begin it's shutdown sequence, releasing external resources
     * as necessary.
     */
    public void beginStop();
    
    /**
     * Complete the shutdown of a transport component, potentially blocking until this is
     * complete.
     */
    public void stop();
}
