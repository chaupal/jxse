package net.jxta.impl.endpoint.netty;

import net.jxta.endpoint.Message;

/**
 * The interface which is used for dispatching the messages and connection related information
 * out of the Netty channel pipeline to the JXTA environment.
 * 
 * @author Iain McGinniss (iain.mcginniss@onedrum.com)
 */
public interface MessageArrivalListener {

    public void messageArrived(Message m);
    public void connectionDied();
    public void channelSaturated(boolean saturated);
    
}
