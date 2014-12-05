package net.jxta.impl.endpoint.netty;

import java.net.SocketAddress;
import java.util.List;

import net.jxta.endpoint.EndpointAddress;

/**
 * Describes a facility to convert between a SocketAddress and a JXTA EndpointAddress. 
 * Intended to allow flexibility in the Netty transport implementation, should we choose to use
 * a transport which does not use traditional InetSocketAddresses for addressing.
 * 
 * @author iain.mcginniss@onedrum.com
 */
public interface AddressTranslator {

    /**
     * Converts a SocketAddress fetched from a communication channel to the equivalent
     * physical JXTA EndpointAddress.
     */
    public EndpointAddress toEndpointAddress(SocketAddress addr);
    
    /**
     * Converts a JXTA EndpointAddress into the equivalent physical SocketAddress.
     */
    public SocketAddress toSocketAddress(EndpointAddress addr);

    /**
     * Converts a client socket address into an endpoint address, potentially overriding some
     * properties of the client address with those of the server address (i.e. using the server's
     * port rather than the transient client port).
     */
    public EndpointAddress toEndpointAddress(SocketAddress clientAddr, SocketAddress serverAddr);

    /**
     * Takes the provided local bound SocketAddress and returns 0 or more equivalent 
     * addresses which could be used to connect to that bound address externally.
     * 
     * Typically, this is to deal with the case where we have bound to the wildcard address
     * (i.e. [::]:8080 or 0.0.0.0:8080) and wish to convert this into the physical IP addresses
     * for the adapters on our machine (i.e. 192.168.1.45:8080, 172.16.0.52:8080, 10.1.1.12:8080, etc.)
     */
    public List<EndpointAddress> translateToExternalAddresses(SocketAddress bindpoint);

    /**
     * Provides the protocol name which is included in all translated EndpointAddresses.
     */
    public String getProtocolName();
    
}
