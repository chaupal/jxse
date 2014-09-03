package net.jxta.impl.endpoint.servlethttp;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointService;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.peergroup.PeerGroup;

/**
 * An interface that the ServletHttpTransportImpl provides to HttpMessageReceiver
 * and HttpMessageSender. The methods in here were formerly package-private, but
 * in order to extract this interface (which exists so that ServletHttpTransport
 * may be mocked out in tests), these methods have had to be made public.
 *
 * This interface is effectively package-private.
 */
interface ServletHttpTransport {

	/**
	 * Get the peer group from which this Module can obtain services.
	 * @return the peer group.
	 */
    PeerGroup getPeerGroup();

    /**
     * Get the Identity of the Module within group.
     * @return the module ID.
     */
    ID getAssignedID();

    /**
     * Get the name that's been configured for the HTTP protocol, which is
     * 'http' by default, but can be overridden by configuration.
     * @return the HTTP protocol name.
     */
    String getConfiguredHttpProtocolName();
    
    /**
     * Get the EndpointService instance we attach to.
     * 
     * @return EndpointService instance
     */
    EndpointService getEndpointService();

    /**
     * Get an appropriate transport binding meter, if transport metering is in
     * effect.
     * 
     * @param peerIDString the peer ID, can be null to get a meter for
     * an unknown peer 
     * @param destinationAddress the destination address
     * @return the transport binding meter, or null if transport metering is
     * not in effect.
     */
    TransportBindingMeter getTransportBindingMeter(String peerIDString, EndpointAddress destinationAddress);

}
