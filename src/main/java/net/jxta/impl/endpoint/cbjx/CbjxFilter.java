
package net.jxta.impl.endpoint.cbjx;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageFilterListener;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.impl.endpoint.EndpointServiceImpl;

/**
 *
 */
public class CbjxFilter implements MessageFilterListener
{

    @Override
    public Message filterMessage(Message paramMsg, EndpointAddress paramSrcAddr, EndpointAddress paramDstAddr)
    {
        if(WireFormatMessageFactory.CBJX_DISABLE)
        {
            return paramMsg;
        }
        boolean tempLoopback = (Boolean)paramMsg.getMessageProperty(EndpointServiceImpl.MESSAGE_LOOPBACK);
        if(tempLoopback)
        {
            return paramMsg;
        }

        Set<EndpointAddress> tempSetEA = (Set)paramMsg.getMessageProperty(EndpointServiceImpl.VERIFIED_ADDRESS_SET);
        if(tempSetEA.contains(paramSrcAddr))
        {
            return paramMsg;
        }
        else
        {
            Logger.getLogger(CbjxFilter.class.getName()).log(Level.SEVERE, "Address spoofing: wrong address="+paramSrcAddr);
            Logger.getLogger(CbjxFilter.class.getName()).log(Level.SEVERE, "                  verified set ="+tempSetEA);
            return null;
        }
    }

}
